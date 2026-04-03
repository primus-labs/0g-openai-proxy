# OpenAI Proxy Client

Node.js OpenAI API forwarding service with a proxy interface fully compatible with the OpenAI protocol.

## Features

- Fully compatible with the OpenAI API protocol
- Streaming responses (`text/event-stream`)
- Forwards all OpenAI API endpoints
- CORS enabled
- Forwards to DeepSeek or other OpenAI-compatible backends
- Transparent model name rewriting (clients may use any model name)
- zkTLS attestation attached to JSON and SSE responses
- Elastic multi-process zkTLS worker pool for concurrent server-side attestations

## Setup

### 1. Install dependencies

```bash
npm install
```

### 2. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` and set your upstream API key:

```env
PORT=3000
OPENAI_API_KEY=sk-your-deepseek-api-key-here
OPENAI_API_BASE=https://api.deepseek.com/v1
UPSTREAM_MODEL=deepseek-chat
ZKTLS_APP_ID=your_primus_app_id
ZKTLS_APP_SECRET=your_primus_app_secret
ZKTLS_MIN_WORKERS=2
ZKTLS_MAX_WORKERS=4
```

## Usage

### Start the server

```bash
npm start
```

### Run tests

```bash
npm test
```

### Run concurrent request test

Start the server first, then run:

```bash
npm run test:concurrency
```

Optional overrides:

```bash
CONCURRENCY=4 REQUESTS=8 npm run test:concurrency
```

Higher-load preset:

```bash
npm run test:concurrency:high
```

This test sends multiple non-streaming `/chat/completions` requests in parallel and reports:

- total success count
- attestation presence count
- per-request latency statistics
- HTTP status distribution

It is intended as a smoke test for the zkTLS worker pool. It does not prove that the SDK scales on every machine or under production traffic.

Recommended progression:

1. Run `npm test`
2. Run `npm run test:concurrency`
3. Run `npm run test:concurrency:high`

### API examples

#### Non-streaming chat

```bash
curl -X POST http://localhost:3000/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4-turbo",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

#### Streaming chat

```bash
curl -X POST http://localhost:3000/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4-turbo",
    "messages": [{"role": "user", "content": "Hello!"}],
    "stream": true
  }'
```

## Model name handling

The client may send any `model` name (e.g. `gpt-4-turbo`). The proxy will:

1. Replace `model` with `UPSTREAM_MODEL` from config (e.g. `deepseek-chat`) when calling the upstream API.
2. Rewrite the `model` field in upstream responses back to the client's original name.

Clients can keep arbitrary model names without knowing the upstream's actual model id.

## Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `3000` |
| `OPENAI_API_KEY` | Upstream API key (Bearer) | Required |
| `OPENAI_API_BASE` | Upstream base URL | `https://api.deepseek.com/v1` |
| `UPSTREAM_MODEL` | Model sent to upstream | `deepseek-chat` |
| `ZKTLS_APP_ID` | Primus application id | Required (or `PRIMUS_APP_ID`) |
| `ZKTLS_APP_SECRET` | Primus app secret (EVM private key) | Required (or `PRIMUS_APP_SECRET`) |
| `ZKTLS_USER_ADDRESS` | Optional recipient address for attestations | `0x0000...0000` |
| `ZKTLS_INIT_MODE` | Primus backend: `auto`, `native`, or `wasm` | `auto` |
| `ZKTLS_MIN_WORKERS` | Number of pre-warmed zkTLS worker processes started at boot | `2` |
| `ZKTLS_MAX_WORKERS` | Maximum zkTLS worker processes allowed after elastic scale-out | Falls back to `ZKTLS_MIN_WORKERS` |
| `ZKTLS_TASK_TIMEOUT_MS` | Timeout for one attestation task in the main process | `600000` |
| `ZKTLS_MAX_QUEUE_SIZE` | Maximum queued attestation tasks before returning `503` | `100` |
| `ZKTLS_IDLE_SHRINK_MS` | How long an extra scaled-out worker may stay idle before being removed | `30000` |

## Concurrency model

`@primuslabs/zktls-core-sdk` attestation execution is treated as process-scoped. To avoid serializing all user requests through one SDK instance, this project now uses an elastic child-process worker pool:

- the main HTTP server keeps request parsing, response formatting, and OpenAI compatibility
- `ZKTLS_MIN_WORKERS` workers are pre-warmed at startup
- each worker process owns one `PrimusCoreTLS` instance and initializes it with `ZKTLS_INIT_MODE`
- one worker handles one attestation task at a time
- if all current workers are busy and total workers are still below `ZKTLS_MAX_WORKERS`, the pool creates another worker
- once `ZKTLS_MAX_WORKERS` is reached, requests are queued up to `ZKTLS_MAX_QUEUE_SIZE`
- extra non-baseline workers are removed after `ZKTLS_IDLE_SHRINK_MS` of idle time
- if a task times out or a worker crashes, that worker is replaced automatically

Design details are documented in [ZKTLS_PROCESS_POOL_DESIGN.md](./ZKTLS_PROCESS_POOL_DESIGN.md).

## Runtime inspection

The server exposes current worker-pool state in:

- `GET /health`
- `GET /debug/attestation-pool`

The response includes:

- minimum worker count
- maximum worker count
- running worker count
- ready worker count
- idle worker count
- busy worker count
- spawning worker count
- queue depth
- max queue size
- idle shrink timeout
- task timeout

## Docker

### Build

```bash
docker build -t openai-proxy-js .
```

### Run

```bash
docker run -d \
  -p 3000:3000 \
  -e OPENAI_API_KEY=your_api_key_here \
  -e OPENAI_API_BASE=https://api.deepseek.com/v1 \
  -e UPSTREAM_MODEL=deepseek-chat \
  -e ZKTLS_APP_ID=your_primus_app_id \
  -e ZKTLS_APP_SECRET=your_primus_app_secret \
  -e ZKTLS_MIN_WORKERS=2 \
  -e ZKTLS_MAX_WORKERS=4 \
  openai-proxy-js
```

### Docker Compose

1. Create `.env`:

```env
OPENAI_API_KEY=your_api_key_here
OPENAI_API_BASE=https://api.deepseek.com/v1
UPSTREAM_MODEL=deepseek-chat
PORT=3000
ZKTLS_APP_ID=your_primus_app_id
ZKTLS_APP_SECRET=your_primus_app_secret
ZKTLS_MIN_WORKERS=2
ZKTLS_MAX_WORKERS=4
```

2. Start:

```bash
docker-compose up -d
```

3. Logs:

```bash
docker-compose logs -f
```

4. Stop:

```bash
docker-compose down
```

## Project layout

```
openai-proxy-js/
├── package.json       # Project config and dependencies
├── src/
│   ├── server.js      # Main HTTP server
│   └── attestation/   # zkTLS worker-pool implementation
├── test/
│   ├── test.js        # Basic HTTP integration tests
│   └── concurrency.js # Concurrent request smoke test
├── Dockerfile         # Docker image
├── build.sh           # Docker build helper
├── .env.example       # Example env vars
├── ZKTLS_PROCESS_POOL_DESIGN.md # Technical design
└── README.md          # This file
```

## Notes

- zkTLS still uses non-streaming upstream calls internally. Streaming responses to clients are synthesized from the final upstream result plus attestation.
- The concurrent test requires a running server, valid zkTLS credentials, and upstream network connectivity.
- If all workers are busy and the queue is full, the server returns HTTP `503` with code `attestation_queue_full`.
- If load exceeds the baseline pool, the service may scale out up to `ZKTLS_MAX_WORKERS`, then shrink extra workers after they stay idle.
