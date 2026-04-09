function parseAttestedField(attestation, keyName) {
  let data = attestation.data;
  if (typeof data === 'string') {
    try {
      data = JSON.parse(data);
    } catch {
      return null;
    }
  }
  if (!data || typeof data !== 'object') {
    return null;
  }
  const raw = data[keyName];
  if (raw === undefined) {
    return null;
  }
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw);
    } catch {
      return raw;
    }
  }
  return raw;
}

function serializeAttestation(attestation) {
  if (attestation == null) {
    return null;
  }
  try {
    return JSON.parse(JSON.stringify(attestation));
  } catch {
    return attestation;
  }
}

function buildAttestationTaskPayload({
  networkRequest,
  responseResolves,
  userAddress,
  urls,
  timeoutMs,
  attMode,
}) {
  return {
    networkRequest,
    responseResolves,
    userAddress,
    urls,
    timeoutMs,
    attMode,
  };
}

module.exports = {
  buildAttestationTaskPayload,
  parseAttestedField,
  serializeAttestation,
};
