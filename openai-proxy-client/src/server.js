/**
 * OpenAI-compatible proxy: forwards requests to an upstream LLM via Primus zkTLS
 * (non-streaming upstream) and returns JSON or SSE to clients. Request and response
 * bodies are forwarded without rewriting the `model` field.
 */
const path = require('path');
const express = require('express');
const cors = require('cors');
const { PrimusCoreTLS } = require('@primuslabs/zktls-core-sdk');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const PORT = process.env.PORT || 3000;
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const OPENAI_API_BASE = process.env.OPENAI_API_BASE || 'https://api.deepseek.com/v1';
const ZKTLS_APP_ID = process.env.ZKTLS_APP_ID || process.env.PRIMUS_APP_ID;
const ZKTLS_APP_SECRET = process.env.ZKTLS_APP_SECRET || process.env.PRIMUS_APP_SECRET;
const ZKTLS_USER_ADDRESS =
  process.env.ZKTLS_USER_ADDRESS || '0x0000000000000000000000000000000000000000';
/** Primus init backend: auto | native | wasm (default auto). Server/Linux often falls back to wasm. */
const ZKTLS_INIT_MODE = process.env.ZKTLS_INIT_MODE || 'auto';
const PRIMUS_MPC_URL = process.env.PRIMUS_MPC_URL || 'ws://api-dev.padolabs.org:38110';
const PRIMUS_PROXY_URL = process.env.PRIMUS_PROXY_URL || 'ws://api-dev.padolabs.org:38111';
const PROXY_URL = process.env.PROXY_URL || 'ws://api-dev.padolabs.org:38112';

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

let primusTlsInstance = null;
let primusInitPromise = null;

async function getPrimusTls() {
  if (primusTlsInstance) {
    return primusTlsInstance;
  }
  if (!primusInitPromise) {
    primusInitPromise = (async () => {
      const zk = new PrimusCoreTLS();
      await zk.init(ZKTLS_APP_ID, ZKTLS_APP_SECRET, ZKTLS_INIT_MODE);
      primusTlsInstance = zk;
      console.log(
        `[${new Date().toISOString()}] Primus zkTLS ready (mode=${ZKTLS_INIT_MODE})`,
      );
      return zk;
    })();
  }
  return primusInitPromise;
}

let attestationChain = Promise.resolve();

function queueAttestation(fn) {
  const p = attestationChain.then(() => fn());
  attestationChain = p.catch(() => { });
  return p;
}

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
  res.write(`data: ${JSON.stringify(chunk2)}\n\n`);

  if (completionJson.usage) {
    const usageChunk = {
      ...base,
      choices: [],
      usage: completionJson.usage,
    };
    res.write(`data: ${JSON.stringify(usageChunk)}\n\n`);
  }

  // zkTLS proof as an OpenAI-shaped chunk (clients that only handle chat.completion.chunk).
  // `usage` stays {} per wire shape; attestation is a sibling field (empty usage cannot carry it).
  const proofChunk = {
    id: completionJson.id,
    object: 'chat.completion.chunk',
    created: completionJson.created,
    model: completionJson.model,
    choices: [],
    usage: {},
    attestation: serializeAttestation(attestation),
  };
  res.write(`data: ${JSON.stringify(proofChunk)}\n\n`);

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

    const zk = await getPrimusTls();
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

    const attRequest = zk.generateRequestParams(
      networkRequest,
      responseResolves,
      ZKTLS_USER_ADDRESS,
    );
    attRequest.setAttMode({
      algorithmType: 'proxytls',
      resultType: 'plain',
    });

    const urls = {
      primusMpcUrl: PRIMUS_MPC_URL,
      primusProxyUrl: PRIMUS_PROXY_URL,
      proxyUrl: PROXY_URL,
    };
    const attestation = await queueAttestation(() =>
      zk.startAttestation(attRequest, 600000, urls),
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
      const choiceCount = parsed.choices.length;
      const ms = Date.now() - startedAt;
      console.log(
        `[${new Date().toISOString()}] [proxy] ok chat.completion ${req.path} stream=${wantStream} choices=${choiceCount} model=${String(parsed.model || '')} durationMs=${ms}`,
      );
      if (wantStream) {
        writeChatCompletionAsSse(res, parsed, attestation);
      } else {
        const body = attachAttestation({ ...parsed }, attestation);
        res.json(body);
      }
      return;
    }

    const ms = Date.now() - startedAt;
    const obj = parsed && typeof parsed === 'object' ? parsed.object : typeof parsed;
    console.log(
      `[${new Date().toISOString()}] [proxy] ok ${req.path} stream=${wantStream} object=${String(obj)} durationMs=${ms}`,
    );
    if (wantStream) {
      writeJsonAsSse(res, parsed, attestation);
    } else {
      res.json(attachAttestation(parsed, attestation));
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
    );

    if (error.code === 'ECONNABORTED') {
      res.status(504).json({
        error: {
          message: 'Request timeout',
          type: 'timeout_error',
          code: 'timeout',
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
  console.log(`Health: http://localhost:${PORT}/health`);
  console.log('='.repeat(50));
});
