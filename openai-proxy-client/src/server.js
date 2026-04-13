/**
 * OpenAI-compatible proxy: forwards requests to an upstream LLM via Primus zkTLS
 * (non-streaming upstream) and returns JSON or SSE to clients. The client-facing
 * model id can be rewritten to a configured upstream model and restored in responses.
 */
const path = require('path');
const express = require('express');
const cors = require('cors');
const { createAttestationWorkerPool } = require('./attestation/pool');
const {
  buildAttestationTaskPayload,
  parseAttestedField,
  serializeAttestation,
} = require('./attestation/shared');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const PORT = process.env.PORT || 3000;
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const OPENAI_API_BASE = process.env.OPENAI_API_BASE || 'https://api.deepseek.com/v1';
const UPSTREAM_MODEL = process.env.UPSTREAM_MODEL || 'deepseek-chat';
const ZKTLS_APP_ID = process.env.ZKTLS_APP_ID || process.env.PRIMUS_APP_ID;
const ZKTLS_APP_SECRET = process.env.ZKTLS_APP_SECRET || process.env.PRIMUS_APP_SECRET;
const ZKTLS_USER_ADDRESS =
  process.env.ZKTLS_USER_ADDRESS || '0x0000000000000000000000000000000000000000';
/** Primus init backend: auto | native | wasm (default auto). Server/Linux often falls back to wasm. */
const ZKTLS_INIT_MODE = process.env.ZKTLS_INIT_MODE || 'auto';
const PRIMUS_MPC_URL = process.env.PRIMUS_MPC_URL || 'ws://api-dev.padolabs.org:38110';
const PRIMUS_PROXY_URL = process.env.PRIMUS_PROXY_URL || 'ws://api-dev.padolabs.org:38111';
const PROXY_URL = process.env.PROXY_URL || 'ws://api-dev.padolabs.org:38112';
const ZKTLS_TASK_TIMEOUT_MS = Number(process.env.ZKTLS_TASK_TIMEOUT_MS || 600000);
const ZKTLS_MIN_WORKERS = Number(
  process.env.ZKTLS_MIN_WORKERS || process.env.ZKTLS_WORKER_COUNT || 2,
);
const ZKTLS_MAX_WORKERS = Number(process.env.ZKTLS_MAX_WORKERS || ZKTLS_MIN_WORKERS);
const ZKTLS_MAX_QUEUE_SIZE = Number(process.env.ZKTLS_MAX_QUEUE_SIZE || 100);
const ZKTLS_IDLE_SHRINK_MS = Number(process.env.ZKTLS_IDLE_SHRINK_MS || 30000);

/** JSONPath for the full upstream JSON body (Primus response resolve). */
const UPSTREAM_RESPONSE_PARSE_PATH = '$';
const UPSTREAM_RESPONSE_KEY_NAME = 'response';

if (!OPENAI_API_KEY) {
  console.error('Error: OPENAI_API_KEY is required');
  process.exit(1);
}

if (!ZKTLS_APP_ID || !ZKTLS_APP_SECRET) {
  console.error('Error: ZKTLS_APP_ID and ZKTLS_APP_SECRET (or PRIMUS_APP_*) are required');
  process.exit(1);
}

const app = express();

app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

/** Log URL origin + pathname only; omit query (may contain tokens). */
function safeTargetForLog(url) {
  try {
    const u = new URL(url);
    return `${u.origin}${u.pathname}`;
  } catch {
    return '[invalid-url]';
  }
}

/** Request shape for logs — no message content, keys, or secrets. */
function summarizeProxyRequest(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return {};
  }
  const summary = {
    stream: body.stream === true,
  };
  if (typeof body.model === 'string') {
    summary.model = body.model;
  }
  if (Array.isArray(body.messages)) {
    summary.messageCount = body.messages.length;
  }
  return summary;
}

function joinBaseAndPath(base, pathname) {
  const b = base.replace(/\/$/, '');
  const p = pathname.startsWith('/') ? pathname : `/${pathname}`;
  return `${b}${p}`;
}

function buildTargetUrl(base, pathname, query) {
  const pathUrl = joinBaseAndPath(base, pathname);
  const keys = Object.keys(query || {});
  if (keys.length === 0) {
    return pathUrl;
  }
  const sp = new URLSearchParams();
  for (const k of keys) {
    const v = query[k];
    if (Array.isArray(v)) {
      v.forEach((item) => {
        if (item != null) sp.append(k, String(item));
      });
    } else if (v != null) {
      sp.append(k, String(v));
    }
  }
  const q = sp.toString();
  return q ? `${pathUrl}?${q}` : pathUrl;
}

const attestationPool = createAttestationWorkerPool({
  minWorkers: ZKTLS_MIN_WORKERS,
  maxWorkers: ZKTLS_MAX_WORKERS,
  maxQueueSize: ZKTLS_MAX_QUEUE_SIZE,
  taskTimeoutMs: ZKTLS_TASK_TIMEOUT_MS,
  idleShrinkMs: ZKTLS_IDLE_SHRINK_MS,
});

function attachAttestation(body, attestation) {
  const att = serializeAttestation(attestation);
  if (body !== null && typeof body === 'object' && !Array.isArray(body)) {
    return { ...body, attestation: att };
  }
  return { result: body, attestation: att };
}

/**
 * zkTLS uses non-streaming upstream calls. OpenAI-compatible APIs reject
 * stream_options unless stream === true, so strip every variant of that field.
 */
function sanitizeUpstreamOpenAiBody(body) {
  const o = body && typeof body === 'object' && !Array.isArray(body) ? { ...body } : {};
  o.stream = false;
  for (const key of Object.keys(o)) {
    const norm = key.toLowerCase().replace(/_/g, '');
    if (norm === 'streamoptions') {
      delete o[key];
    }
  }
  delete o.stream_options;
  delete o.streamOptions;
  return o;
}

function rewriteModelForUpstream(body) {
  const originalModel =
    body && typeof body === 'object' && !Array.isArray(body) && typeof body.model === 'string'
      ? body.model
      : null;

  if (!UPSTREAM_MODEL) {
    return { body, originalModel };
  }

  const nextBody =
    body && typeof body === 'object' && !Array.isArray(body)
      ? { ...body, model: UPSTREAM_MODEL }
      : body;
  return { body: nextBody, originalModel };
}

function rewriteResponseModel(payload, clientModel) {
  if (
    !clientModel ||
    !payload ||
    typeof payload !== 'object' ||
    Array.isArray(payload) ||
    typeof payload.model !== 'string'
  ) {
    return payload;
  }
  return { ...payload, model: clientModel };
}

function clientRequestedEventStream(req) {
  return req.body && req.body.stream === true;
}

function writeSseHeaders(res) {
  res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
  res.setHeader('Cache-Control', 'no-cache, no-transform');
  res.setHeader('Connection', 'keep-alive');
  res.removeHeader('Content-Length');
}

function writeChatCompletionAsSse(res, completionJson, attestation) {
  const choice0 = completionJson.choices && completionJson.choices[0];
  const content =
    choice0 && choice0.message && typeof choice0.message.content === 'string'
      ? choice0.message.content
      : '';
  const finishReason = (choice0 && choice0.finish_reason) || 'stop';
  const serializedAttestation = serializeAttestation(attestation);
  const base = {
    id: completionJson.id,
    object: 'chat.completion.chunk',
    created: completionJson.created,
    model: completionJson.model,
  };

  writeSseHeaders(res);
  res.status(200);
  if (typeof res.flushHeaders === 'function') {
    res.flushHeaders();
  }

  const chunk1 = {
    ...base,
    choices: [
      {
        index: 0,
        delta: { role: 'assistant', content },
        finish_reason: null,
      },
    ],
  };
  res.write(`data: ${JSON.stringify(chunk1)}\n\n`);

  const chunk2 = {
    ...base,
    choices: [{ index: 0, delta: {}, finish_reason: finishReason }],
  };

  if (completionJson.usage) {
    res.write(`data: ${JSON.stringify(chunk2)}\n\n`);

    const usageChunk = {
      ...base,
      choices: [],
      usage: completionJson.usage,
      attestation: serializedAttestation,
    };
    res.write(`data: ${JSON.stringify(usageChunk)}\n\n`);
  } else {
    const finalChunk = {
      ...chunk2,
      attestation: serializedAttestation,
    };
    res.write(`data: ${JSON.stringify(finalChunk)}\n\n`);
  }

  res.write('data: [DONE]\n\n');
  res.end();
}

function writeJsonAsSse(res, payload, attestation) {
  const envelope = attachAttestation(payload, attestation);
  writeSseHeaders(res);
  res.status(200);
  if (typeof res.flushHeaders === 'function') {
    res.flushHeaders();
  }
  res.write(`data: ${JSON.stringify(envelope)}\n\n`);
  res.write('data: [DONE]\n\n');
  res.end();
}

async function proxyToUpstream(req, res) {
  const startedAt = Date.now();
  try {
    const wantStream = clientRequestedEventStream(req);
    let upstreamBody =
      req.body && typeof req.body === 'object' ? { ...req.body } : {};

    upstreamBody = sanitizeUpstreamOpenAiBody(upstreamBody);
    const { body: rewrittenBody, originalModel } = rewriteModelForUpstream(upstreamBody);
    upstreamBody = rewrittenBody;

    const targetUrl = buildTargetUrl(OPENAI_API_BASE, req.path, req.query);
    const method = (req.method || 'GET').toUpperCase();

    const reqSummary = summarizeProxyRequest(req.body);
    console.log(
      `[${new Date().toISOString()}] [proxy] ${method} ${req.path} -> ${safeTargetForLog(targetUrl)}`,
      JSON.stringify(reqSummary),
    );

    const header = {
      Authorization: `Bearer ${OPENAI_API_KEY}`,
      'Content-Type': 'application/json',
    };

    const bodyString =
      method === 'GET' || method === 'HEAD' ? '' : JSON.stringify(upstreamBody);

    const networkRequest = {
      url: targetUrl,
      method,
      header,
      body: bodyString,
    };

    const responseResolves = [
      {
        keyName: UPSTREAM_RESPONSE_KEY_NAME,
        parseType: 'json',
        parsePath: UPSTREAM_RESPONSE_PARSE_PATH,
      },
    ];

    const urls = {
      primusMpcUrl: PRIMUS_MPC_URL,
      primusProxyUrl: PRIMUS_PROXY_URL,
      proxyUrl: PROXY_URL,
    };
    const attestation = await attestationPool.runTask(
      buildAttestationTaskPayload({
        networkRequest,
        responseResolves,
        userAddress: ZKTLS_USER_ADDRESS,
        urls,
        timeoutMs: ZKTLS_TASK_TIMEOUT_MS,
        attMode: {
          algorithmType: 'proxytls',
          resultType: 'plain',
        },
      }),
    );
    const parsed = parseAttestedField(attestation, UPSTREAM_RESPONSE_KEY_NAME);

    if (parsed === null || parsed === undefined) {
      console.warn(
        `[${new Date().toISOString()}] [proxy] bad attestation payload ${req.path} durationMs=${Date.now() - startedAt}`,
      );
      res.status(502).json({
        error: {
          message: 'Upstream response could not be parsed from attestation',
          type: 'attestation_error',
          code: 'bad_attestation_payload',
        },
      });
      return;
    }

    if (parsed && typeof parsed === 'object' && parsed.error) {
      const code = parsed.error && parsed.error.code;
      let status = 502;
      if (code === 'invalid_api_key' || code === 'authentication_error') {
        status = 401;
      } else if (code === 'rate_limit_exceeded') {
        status = 429;
      } else if (code === 'insufficient_quota') {
        status = 402;
      } else if (typeof code === 'number' && code >= 400 && code < 600) {
        status = code;
      }
      const errBody = attachAttestation(parsed, attestation);
      const errCode = parsed.error && parsed.error.code;
      const errType = parsed.error && parsed.error.type;
      console.warn(
        `[${new Date().toISOString()}] [proxy] upstream error ${req.path} http=${status} code=${String(errCode)} type=${String(errType)} durationMs=${Date.now() - startedAt}`,
      );
      if (wantStream) {
        res.status(status);
        writeSseHeaders(res);
        if (typeof res.flushHeaders === 'function') {
          res.flushHeaders();
        }
        res.write(`data: ${JSON.stringify(errBody)}\n\n`);
        res.write('data: [DONE]\n\n');
        res.end();
      } else {
        res.status(status).json(errBody);
      }
      return;
    }

    if (
      parsed &&
      typeof parsed === 'object' &&
      parsed.object === 'chat.completion' &&
      Array.isArray(parsed.choices)
    ) {
      const responseBody = rewriteResponseModel(parsed, originalModel);
      const choiceCount = parsed.choices.length;
      const ms = Date.now() - startedAt;
      console.log(
        `[${new Date().toISOString()}] [proxy] ok chat.completion ${req.path} stream=${wantStream} choices=${choiceCount} clientModel=${String(originalModel || parsed.model || '')} upstreamModel=${String(parsed.model || '')} durationMs=${ms}`,
      );
      if (wantStream) {
        writeChatCompletionAsSse(res, responseBody, attestation);
      } else {
        const body = attachAttestation(responseBody, attestation);
        res.json(body);
      }
      return;
    }

    const responseBody = rewriteResponseModel(parsed, originalModel);
    const ms = Date.now() - startedAt;
    const obj = parsed && typeof parsed === 'object' ? parsed.object : typeof parsed;
    console.log(
      `[${new Date().toISOString()}] [proxy] ok ${req.path} stream=${wantStream} object=${String(obj)} durationMs=${ms}`,
    );
    if (wantStream) {
      writeJsonAsSse(res, responseBody, attestation);
    } else {
      res.json(attachAttestation(responseBody, attestation));
    }
  } catch (error) {
    const zkCode = error && error.code;
    let dataLen = 0;
    try {
      if (error && error.data !== undefined && error.data !== null) {
        dataLen =
          typeof error.data === 'string'
            ? error.data.length
            : JSON.stringify(error.data).length;
      }
    } catch {
      // ignore
    }
    console.error(
      `[${new Date().toISOString()}] [proxy error] ${req.path} durationMs=${Date.now() - startedAt}`,
      error.message,
      zkCode ? `zktls_code=${zkCode}` : '',
      dataLen ? `error.data_len=${dataLen}` : '',
        error ? `error=${error}` : '',
    );

    if (error.code === 'ECONNABORTED' || error.code === 'attestation_timeout') {
      res.status(504).json({
        error: {
          message: 'Request timeout',
          type: 'timeout_error',
          code: 'timeout',
        },
      });
    } else if (error.code === 'attestation_queue_full') {
      res.status(503).json({
        error: {
          message: 'Attestation capacity is saturated',
          type: 'server_overloaded',
          code: 'attestation_queue_full',
        },
      });
    } else if (
      error.code === 'attestation_pool_unavailable' ||
      error.code === 'attestation_worker_exit'
    ) {
      res.status(error.httpStatus || 503).json({
        error: {
          message: error.message || 'Attestation worker pool is unavailable',
          type: 'server_error',
          code: error.code || 'attestation_pool_unavailable',
        },
      });
    } else if (zkCode && typeof zkCode === 'string') {
      const hint =
        zkCode === '30001' || String(error.message || '').includes('400')
          ? 'Upstream HTTP error (often invalid body: remove stream_options when stream is false, or bad API key).'
          : zkCode === '00104' || zkCode === '00002'
            ? 'Primus attestation failed or timed out; retry or check quota/network.'
            : undefined;
      res.status(502).json({
        error: {
          message: error.message || 'Attestation failed',
          type: 'zktls_error',
          code: zkCode,
          ...(hint ? { hint } : {}),
        },
      });
    } else {
      const msg = error.message || 'Internal server error';
      const upstreamHint =
        /400|Response error|status code error/i.test(msg) &&
        !/timeout/i.test(msg)
          ? 'If this only fails on the server, confirm the image includes stream_options fix (upstream stream:false), env vars match local, and try ZKTLS_INIT_MODE=wasm.'
          : undefined;
      res.status(500).json({
        error: {
          message: msg,
          type: 'server_error',
          code: 'internal_error',
          ...(upstreamHint ? { hint: upstreamHint } : {}),
        },
      });
    }
  }
}

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    upstream: OPENAI_API_BASE,
    upstreamModel: UPSTREAM_MODEL,
    attestationPool: attestationPool.getStats(),
  });
});

app.get('/debug/attestation-pool', (_req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    attestationPool: attestationPool.getStats(),
  });
});

// Browsers request /favicon.ico for any origin; do not forward to the LLM API.
app.get('/favicon.ico', (_req, res) => {
  res.status(204).end();
});

app.post('/chat/completions', proxyToUpstream);
app.all('*', proxyToUpstream);

app.listen(PORT, () => {
  console.log('='.repeat(50));
  console.log('OpenAI Proxy Service Started');
  console.log('='.repeat(50));
  console.log(`Port: ${PORT}`);
  console.log(`Upstream: ${OPENAI_API_BASE}`);
  console.log(`Upstream model: ${UPSTREAM_MODEL}`);
  console.log(`Health: http://localhost:${PORT}/health`);
  console.log(`zkTLS init mode: ${ZKTLS_INIT_MODE}`);
  console.log(`zkTLS min workers: ${ZKTLS_MIN_WORKERS}`);
  console.log(`zkTLS max workers: ${ZKTLS_MAX_WORKERS}`);
  console.log(`zkTLS max queue size: ${ZKTLS_MAX_QUEUE_SIZE}`);
  console.log(`zkTLS idle shrink ms: ${ZKTLS_IDLE_SHRINK_MS}`);
  console.log(`zkTLS task timeout ms: ${ZKTLS_TASK_TIMEOUT_MS}`);
  console.log('='.repeat(50));
});

attestationPool.start().catch((error) => {
  console.error(
    `[${new Date().toISOString()}] [attestation-pool] startup failed`,
    error.message,
  );
});
