/**
 * Concurrent HTTP integration test against a running server.
 *
 * Usage:
 *   npm run test:concurrency
 *   CONCURRENCY=4 REQUESTS=8 npm run test:concurrency
 */
const path = require('path');
const http = require('http');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || '127.0.0.1';
const CONCURRENCY = Math.max(1, Number(process.env.CONCURRENCY || 4));
const REQUESTS = Math.max(CONCURRENCY, Number(process.env.REQUESTS || CONCURRENCY * 2));
const PATHNAME = process.env.TEST_PATH || '/chat/completions';
const HAS_API_KEY = process.env.OPENAI_API_KEY && process.env.OPENAI_API_KEY !== 'your_api_key_here';

function makeRequest(body, requestIndex) {
  return new Promise((resolve) => {
    const startedAt = Date.now();
    const req = http.request(
      {
        hostname: HOST,
        port: PORT,
        path: PATHNAME,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      },
      (res) => {
        let responseBody = '';
        res.on('data', (chunk) => {
          responseBody += chunk;
        });
        res.on('end', () => {
          const durationMs = Date.now() - startedAt;
          let parsedBody = null;
          try {
            parsedBody = JSON.parse(responseBody);
          } catch {
            parsedBody = null;
          }
          resolve({
            requestIndex,
            statusCode: res.statusCode,
            durationMs,
            ok: res.statusCode >= 200 && res.statusCode < 300,
            hasAttestation: Boolean(
              parsedBody &&
              typeof parsedBody === 'object' &&
              !Array.isArray(parsedBody) &&
              parsedBody.attestation &&
              typeof parsedBody.attestation === 'object',
            ),
            errorCode:
              parsedBody &&
              parsedBody.error &&
              typeof parsedBody.error === 'object' &&
              parsedBody.error.code
                ? parsedBody.error.code
                : null,
          });
        });
      },
    );

    req.on('error', (error) => {
      resolve({
        requestIndex,
        statusCode: 0,
        durationMs: Date.now() - startedAt,
        ok: false,
        hasAttestation: false,
        errorCode: error.code || error.message || 'request_error',
      });
    });

    req.write(JSON.stringify(body));
    req.end();
  });
}

async function runWithConcurrency(tasks, limit) {
  const results = new Array(tasks.length);
  let cursor = 0;

  async function worker() {
    while (true) {
      const current = cursor;
      cursor += 1;
      if (current >= tasks.length) {
        return;
      }
      results[current] = await tasks[current]();
    }
  }

  await Promise.all(Array.from({ length: limit }, () => worker()));
  return results;
}

function buildRequestBody(index) {
  return {
    model: `concurrency-test-${index}`,
    messages: [
      {
        role: 'user',
        content: `Return a short response for concurrency test request ${index}.`,
      },
    ],
    stream: false,
  };
}

function printSummary(results, suiteDurationMs) {
  const statusCounts = new Map();
  let okCount = 0;
  let attestationCount = 0;
  let minDuration = Number.POSITIVE_INFINITY;
  let maxDuration = 0;
  let sumDuration = 0;

  for (const result of results) {
    statusCounts.set(result.statusCode, (statusCounts.get(result.statusCode) || 0) + 1);
    if (result.ok) {
      okCount += 1;
    }
    if (result.hasAttestation) {
      attestationCount += 1;
    }
    minDuration = Math.min(minDuration, result.durationMs);
    maxDuration = Math.max(maxDuration, result.durationMs);
    sumDuration += result.durationMs;
  }

  console.log('='.repeat(60));
  console.log('Concurrent Request Test Summary');
  console.log('='.repeat(60));
  console.log(`Host: ${HOST}:${PORT}`);
  console.log(`Requests: ${REQUESTS}`);
  console.log(`Concurrency: ${CONCURRENCY}`);
  console.log(`Suite duration: ${suiteDurationMs} ms`);
  console.log(`Successful responses: ${okCount}/${results.length}`);
  console.log(`Responses with attestation: ${attestationCount}/${results.length}`);
  console.log(`Latency min/avg/max: ${minDuration} / ${Math.round(sumDuration / results.length)} / ${maxDuration} ms`);
  console.log('Status counts:', Object.fromEntries(statusCounts));

  const failures = results.filter((result) => !result.ok);
  if (failures.length > 0) {
    console.log('Failures:');
    for (const failure of failures) {
      console.log(
        `  #${failure.requestIndex}: status=${failure.statusCode} duration=${failure.durationMs}ms error=${String(failure.errorCode)}`,
      );
    }
  }
}

async function main() {
  if (!HAS_API_KEY) {
    console.log('OPENAI_API_KEY is not configured. Skipping concurrent request test.');
    process.exit(0);
    return;
  }

  const tasks = Array.from({ length: REQUESTS }, (_, index) => () =>
    makeRequest(buildRequestBody(index + 1), index + 1),
  );

  console.log('='.repeat(60));
  console.log('Running concurrent request test');
  console.log('='.repeat(60));

  const startedAt = Date.now();
  const results = await runWithConcurrency(tasks, CONCURRENCY);
  const suiteDurationMs = Date.now() - startedAt;
  printSummary(results, suiteDurationMs);

  const allPassed = results.every((result) => result.ok);
  process.exit(allPassed ? 0 : 1);
}

main().catch((error) => {
  console.error('Concurrent test failed:', error);
  process.exit(1);
});
