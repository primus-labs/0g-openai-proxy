const path = require('path');
const { fork } = require('child_process');
const {
  buildPoolUnavailableError,
  buildQueueSaturatedError,
  buildTaskTimeoutError,
  buildWorkerExitedError,
} = require('./errors');
const { buildAttestMessage, isTypedMessage } = require('./protocol');

let nextWorkerId = 1;
let nextRequestId = 1;

class AttestationWorkerPool {
  constructor({
    minWorkers,
    maxWorkers,
    maxQueueSize,
    taskTimeoutMs,
    idleShrinkMs,
    workerPath,
  }) {
    this.minWorkers = minWorkers;
    this.maxWorkers = maxWorkers;
    this.maxQueueSize = maxQueueSize;
    this.taskTimeoutMs = taskTimeoutMs;
    this.idleShrinkMs = idleShrinkMs;
    this.workerPath = workerPath;
    this.workers = new Set();
    this.idleWorkers = [];
    this.pendingQueue = [];
    this.spawningWorkers = 0;
    this.started = false;
    this.startPromise = null;
  }

  async start() {
    if (this.started) {
      return;
    }
    if (!this.startPromise) {
      this.startPromise = Promise.all(
        Array.from({ length: this.minWorkers }, () => this.spawnWorker({ isBaseline: true })),
      ).then(() => {
        this.started = true;
      }).catch((error) => {
        this.startPromise = null;
        throw error;
      });
    }
    return this.startPromise;
  }

  async runTask(payload) {
    await this.start();
    return new Promise((resolve, reject) => {
      const task = {
        requestId: `att_${nextRequestId++}`,
        payload,
        resolve,
        reject,
      };

      const idleWorker = this.idleWorkers.shift();
      if (idleWorker) {
        this.clearWorkerShrinkTimer(idleWorker);
        this.dispatchToWorker(idleWorker, task);
        return;
      }

      if (this.getTotalWorkerSlots() < this.maxWorkers) {
        this.spawnWorker({ isBaseline: false })
          .then((worker) => {
            this.removeIdleWorker(worker);
            this.clearWorkerShrinkTimer(worker);
            this.dispatchToWorker(worker, task);
          })
          .catch((error) => {
            reject(error);
          });
        return;
      }

      if (this.pendingQueue.length >= this.maxQueueSize) {
        reject(buildQueueSaturatedError(this.pendingQueue.length));
        return;
      }

      this.pendingQueue.push(task);
      console.warn(
        `[${new Date().toISOString()}] [attestation-pool] queued task depth=${this.pendingQueue.length}`,
      );
    });
  }

  getStats() {
    let busyWorkers = 0;
    let readyWorkers = 0;
    let runningWorkers = 0;

    for (const worker of this.workers) {
      runningWorkers += 1;
      if (worker.ready) {
        readyWorkers += 1;
      }
      if (worker.currentTask) {
        busyWorkers += 1;
      }
    }

    return {
      minWorkers: this.minWorkers,
      maxWorkers: this.maxWorkers,
      runningWorkers,
      readyWorkers,
      idleWorkers: this.idleWorkers.length,
      busyWorkers,
      spawningWorkers: this.spawningWorkers,
      queueDepth: this.pendingQueue.length,
      maxQueueSize: this.maxQueueSize,
      started: this.started,
      startPending: Boolean(this.startPromise) && !this.started,
      taskTimeoutMs: this.taskTimeoutMs,
      idleShrinkMs: this.idleShrinkMs,
    };
  }

  async spawnWorker({ isBaseline }) {
    const id = nextWorkerId++;
    this.spawningWorkers += 1;
    const child = fork(this.workerPath, {
      cwd: path.join(__dirname, '..', '..'),
      env: process.env,
      stdio: ['inherit', 'inherit', 'inherit', 'ipc'],
    });

    const worker = {
      id,
      child,
      currentTask: null,
      ready: false,
      isBaseline,
      shrinkTimer: null,
    };

    const readyPromise = new Promise((resolve, reject) => {
      let startupSettled = false;
      const finishStartup = () => {
        if (startupSettled) {
          return false;
        }
        startupSettled = true;
        this.spawningWorkers -= 1;
        return true;
      };

      const onMessage = (message) => {
        if (isTypedMessage(message, 'ready')) {
          child.off('message', onMessage);
          child.off('exit', onExitBeforeReady);
          worker.ready = true;
          this.workers.add(worker);
          this.markWorkerIdle(worker);
          finishStartup();
          console.log(
            `[${new Date().toISOString()}] [attestation-pool] worker ready id=${worker.id} baseline=${worker.isBaseline}`,
          );
          resolve(worker);
          return;
        }
        if (isTypedMessage(message, 'init_error')) {
          child.off('message', onMessage);
          child.off('exit', onExitBeforeReady);
          const err = new Error(
            message.payload && message.payload.message
              ? message.payload.message
              : 'Attestation worker init failed',
          );
          finishStartup();
          reject(err);
        }
      };

      const onExitBeforeReady = (code, signal) => {
        child.off('message', onMessage);
        finishStartup();
        reject(buildWorkerExitedError(code, signal));
      };

      child.on('message', onMessage);
      child.once('exit', onExitBeforeReady);
    });

    child.on('message', (message) => {
      if (!worker.ready) {
        return;
      }
      this.handleWorkerMessage(worker, message);
    });

    child.on('exit', (code, signal) => {
      this.handleWorkerExit(worker, code, signal);
    });

    console.log(
      `[${new Date().toISOString()}] [attestation-pool] worker starting id=${worker.id} pid=${child.pid} baseline=${worker.isBaseline}`,
    );

    return readyPromise;
  }

  handleWorkerMessage(worker, message) {
    const task = worker.currentTask;
    if (!task) {
      return;
    }

    if (isTypedMessage(message, 'result')) {
      if (message.requestId !== task.requestId) {
        return;
      }
      clearTimeout(task.timer);
      worker.currentTask = null;
      task.resolve(message.payload.attestation);
      this.markWorkerIdle(worker);
      return;
    }

    if (isTypedMessage(message, 'error')) {
      if (message.requestId !== task.requestId) {
        return;
      }
      clearTimeout(task.timer);
      worker.currentTask = null;
      const err = new Error(
        message.payload && message.payload.message
          ? message.payload.message
          : 'Attestation worker returned an error',
      );
      if (message.payload && message.payload.code) {
        err.code = message.payload.code;
      }
      if (message.payload && message.payload.data !== undefined) {
        err.data = message.payload.data;
      }
      task.reject(err);
      this.markWorkerIdle(worker);
    }
  }

  handleWorkerExit(worker, code, signal) {
    this.clearWorkerShrinkTimer(worker);
    this.removeIdleWorker(worker);
    this.workers.delete(worker);

    if (worker.currentTask) {
      clearTimeout(worker.currentTask.timer);
      const task = worker.currentTask;
      worker.currentTask = null;
      task.reject(buildWorkerExitedError(code, signal));
    }

    console.error(
      `[${new Date().toISOString()}] [attestation-pool] worker exited id=${worker.id} pid=${worker.child.pid} baseline=${worker.isBaseline} code=${String(code)} signal=${String(signal)}`,
    );

    if (this.started && worker.isBaseline) {
      setTimeout(() => {
        this.spawnWorker({ isBaseline: true }).catch((error) => {
          console.error(
            `[${new Date().toISOString()}] [attestation-pool] worker restart failed`,
            error.message,
          );
        });
      }, 1000);
    }
  }

  dispatchToWorker(worker, task) {
    if (!worker.ready) {
      task.reject(buildPoolUnavailableError());
      return;
    }

    worker.currentTask = task;
    task.timer = setTimeout(() => {
      if (worker.currentTask !== task) {
        return;
      }
      worker.currentTask = null;
      task.reject(buildTaskTimeoutError(this.taskTimeoutMs));
      worker.child.kill('SIGKILL');
    }, this.taskTimeoutMs);

    console.log(
      `[${new Date().toISOString()}] [attestation-pool] dispatch worker=${worker.id} queueDepth=${this.pendingQueue.length}`,
    );
    try {
      worker.child.send(buildAttestMessage(task.requestId, task.payload));
    } catch (error) {
      clearTimeout(task.timer);
      worker.currentTask = null;
      task.reject(error);
      worker.child.kill('SIGKILL');
    }
  }

  markWorkerIdle(worker) {
    const nextTask = this.pendingQueue.shift();
    if (nextTask) {
      this.clearWorkerShrinkTimer(worker);
      this.dispatchToWorker(worker, nextTask);
      return;
    }
    this.idleWorkers.push(worker);
    this.scheduleWorkerShrink(worker);
  }

  removeIdleWorker(worker) {
    const index = this.idleWorkers.indexOf(worker);
    if (index >= 0) {
      this.idleWorkers.splice(index, 1);
    }
  }

  clearWorkerShrinkTimer(worker) {
    if (worker.shrinkTimer) {
      clearTimeout(worker.shrinkTimer);
      worker.shrinkTimer = null;
    }
  }

  scheduleWorkerShrink(worker) {
    this.clearWorkerShrinkTimer(worker);
    if (worker.isBaseline) {
      return;
    }
    if (this.idleShrinkMs <= 0) {
      return;
    }
    worker.shrinkTimer = setTimeout(() => {
      worker.shrinkTimer = null;
      if (worker.currentTask) {
        return;
      }
      if (worker.isBaseline) {
        return;
      }
      if (this.getTotalWorkerSlots() <= this.minWorkers) {
        return;
      }
      this.removeIdleWorker(worker);
      console.log(
        `[${new Date().toISOString()}] [attestation-pool] shrinking worker id=${worker.id} pid=${worker.child.pid}`,
      );
      worker.child.kill('SIGTERM');
    }, this.idleShrinkMs);
  }

  getTotalWorkerSlots() {
    return this.workers.size + this.spawningWorkers;
  }
}

function createAttestationWorkerPool(options = {}) {
  const minWorkers = Math.max(
    1,
    Number(options.minWorkers || process.env.ZKTLS_MIN_WORKERS || process.env.ZKTLS_WORKER_COUNT || 2),
  );
  const maxWorkers = Math.max(
    minWorkers,
    Number(options.maxWorkers || process.env.ZKTLS_MAX_WORKERS || minWorkers),
  );
  const maxQueueSize = Math.max(
    0,
    Number(options.maxQueueSize || process.env.ZKTLS_MAX_QUEUE_SIZE || 100),
  );
  const taskTimeoutMs = Math.max(
    1,
    Number(options.taskTimeoutMs || process.env.ZKTLS_TASK_TIMEOUT_MS || 600000),
  );
  const idleShrinkMs = Math.max(
    0,
    Number(options.idleShrinkMs || process.env.ZKTLS_IDLE_SHRINK_MS || 30000),
  );

  return new AttestationWorkerPool({
    minWorkers,
    maxWorkers,
    maxQueueSize,
    taskTimeoutMs,
    idleShrinkMs,
    workerPath: options.workerPath || path.join(__dirname, 'worker.js'),
  });
}

module.exports = {
  AttestationWorkerPool,
  createAttestationWorkerPool,
};
