# OpenAI Proxy Client

Node.js OpenAI API forwarding service with a proxy interface fully compatible with the OpenAI protocol.

## Features

- Fully compatible with the OpenAI API protocol
- Streaming responses (`text/event-stream`)
- Forwards all OpenAI API endpoints
- CORS enabled
- Forwards to DeepSeek or other OpenAI-compatible backends
- Transparent model name rewriting (clients may use any model name)

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
  openai-proxy-js
```

### Docker Compose

1. Create `.env`:

```env
OPENAI_API_KEY=your_api_key_here
OPENAI_API_BASE=https://api.deepseek.com/v1
UPSTREAM_MODEL=deepseek-chat
PORT=3000
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
│   └── server.js      # Main server
├── test/
│   └── test.js        # HTTP integration tests
├── Dockerfile         # Docker image
├── docker-compose.yml # Compose config
├── build.sh           # Docker build helper
├── .env.example       # Example env vars
└── README.md          # This file
```
