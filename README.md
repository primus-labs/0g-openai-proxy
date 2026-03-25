# 0g-openai-proxy

This repository now contains:

- [`openai-proxy-client`](/Users/xuda/workspace/github/primuslabs/0g-openai-proxy/openai-proxy-client): OpenAI-compatible proxy service
- [`openai-proxy-attestor`](/Users/xuda/workspace/github/primuslabs/0g-openai-proxy/openai-proxy-attestor): attestation service migrated from the two Pado projects

## Deploy

The deployment entry is [`deploy/docker-compose.yaml`](/Users/xuda/workspace/github/primuslabs/0g-openai-proxy/deploy/docker-compose.yaml). It now includes `openai-proxy-attestor`.

Example:

```bash
cd deploy
export KEYSTORE_CONTENT_BASE64="$(base64 -i ./attestor.json)"
export ATTESTOR_KEYSTORE_PASS=your_keystore_password
docker compose up -d --build
```

Optional attestor environment variables:

- `ATTESTOR_CALL_URL`: remote callback URL; if empty, local signing mode is used
- `ATTESTOR_PADO_CALLBACK_URL`: compatibility alias for callback URL
- `ATTESTOR_PROVIDER`: attestation provider, default `SEPOLIA`
- `KEYSTORE_CONTENT_BASE64`: base64-encoded keystore content, written by `config-init`
- `ATTESTOR_KEYSTORE_PATH`: container path to keystore, default `/keystore/keystore.json`
- `ATTESTOR_KEYSTORE_PASS`: keystore password
