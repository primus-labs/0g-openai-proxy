function serializeError(error) {
  if (!error || typeof error !== 'object') {
    return {
      message: String(error || 'Unknown attestation error'),
      type: 'attestation_error',
      code: null,
      stack: null,
      data: null,
    };
  }

  let data = null;
  if (error.data !== undefined) {
    try {
      data = JSON.parse(JSON.stringify(error.data));
    } catch {
      data = String(error.data);
    }
  }

  return {
    message: error.message || 'Unknown attestation error',
    type: error.type || 'attestation_error',
    code: error.code || null,
    stack: error.stack || null,
    data,
  };
}

function buildQueueSaturatedError(queueSize) {
  const error = new Error('Attestation capacity is saturated');
  error.httpStatus = 503;
  error.code = 'attestation_queue_full';
  error.queueSize = queueSize;
  return error;
}

function buildPoolUnavailableError() {
  const error = new Error('Attestation worker pool is unavailable');
  error.httpStatus = 503;
  error.code = 'attestation_pool_unavailable';
  return error;
}

function buildTaskTimeoutError(timeoutMs) {
  const error = new Error(`Attestation timed out after ${timeoutMs}ms`);
  error.httpStatus = 504;
  error.code = 'attestation_timeout';
  return error;
}

function buildWorkerExitedError(exitCode, signal) {
  const error = new Error(
    `Attestation worker exited unexpectedly (code=${String(exitCode)}, signal=${String(signal)})`,
  );
  error.httpStatus = 502;
  error.code = 'attestation_worker_exit';
  error.exitCode = exitCode;
  error.signal = signal;
  return error;
}

module.exports = {
  buildPoolUnavailableError,
  buildQueueSaturatedError,
  buildTaskTimeoutError,
  buildWorkerExitedError,
  serializeError,
};
