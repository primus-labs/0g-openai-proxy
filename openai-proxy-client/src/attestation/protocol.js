function buildReadyMessage() {
  return { type: 'ready' };
}

function buildResultMessage(requestId, attestation) {
  return {
    type: 'result',
    requestId,
    payload: { attestation },
  };
}

function buildErrorMessage(requestId, error) {
  return {
    type: 'error',
    requestId,
    payload: error,
  };
}

function buildInitErrorMessage(error) {
  return {
    type: 'init_error',
    payload: error,
  };
}

function buildAttestMessage(requestId, payload) {
  return {
    type: 'attest',
    requestId,
    payload,
  };
}

function isTypedMessage(message, type) {
  return Boolean(message) && typeof message === 'object' && message.type === type;
}

module.exports = {
  buildAttestMessage,
  buildErrorMessage,
  buildInitErrorMessage,
  buildReadyMessage,
  buildResultMessage,
  isTypedMessage,
};
