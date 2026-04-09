const path = require('path');
const { PrimusCoreTLS } = require('@primuslabs/zktls-core-sdk');
const { serializeError } = require('./errors');
const {
  buildErrorMessage,
  buildInitErrorMessage,
  buildReadyMessage,
  buildResultMessage,
  isTypedMessage,
} = require('./protocol');

require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env') });

const ZKTLS_APP_ID = process.env.ZKTLS_APP_ID || process.env.PRIMUS_APP_ID;
const ZKTLS_APP_SECRET = process.env.ZKTLS_APP_SECRET || process.env.PRIMUS_APP_SECRET;
const ZKTLS_INIT_MODE = process.env.ZKTLS_INIT_MODE || 'auto';

let zk = null;
let busy = false;

async function init() {
  if (!ZKTLS_APP_ID || !ZKTLS_APP_SECRET) {
    throw new Error('ZKTLS_APP_ID and ZKTLS_APP_SECRET (or PRIMUS_APP_*) are required');
  }
  zk = new PrimusCoreTLS();
  await zk.init(ZKTLS_APP_ID, ZKTLS_APP_SECRET, ZKTLS_INIT_MODE);
}

async function handleAttestation(message) {
  if (busy) {
    throw new Error('Worker received a task while still busy');
  }
  busy = true;
  try {
    const {
      networkRequest,
      responseResolves,
      userAddress,
      urls,
      timeoutMs,
      attMode,
    } = message.payload || {};

    const attRequest = zk.generateRequestParams(
      networkRequest,
      responseResolves,
      userAddress,
    );
    attRequest.setAttMode(attMode || {
      algorithmType: 'proxytls',
      resultType: 'plain',
    });
    const attestation = await zk.startAttestation(attRequest, timeoutMs, urls);
    process.send(buildResultMessage(message.requestId, attestation));
  } catch (error) {
    process.send(buildErrorMessage(message.requestId, serializeError(error)));
  } finally {
    busy = false;
  }
}

async function main() {
  try {
    await init();
    process.send(buildReadyMessage());
  } catch (error) {
    process.send(buildInitErrorMessage(serializeError(error)));
    process.exit(1);
    return;
  }

  process.on('message', async (message) => {
    if (!isTypedMessage(message, 'attest')) {
      return;
    }
    await handleAttestation(message);
  });
}

main().catch((error) => {
  process.send(buildInitErrorMessage(serializeError(error)));
  process.exit(1);
});
