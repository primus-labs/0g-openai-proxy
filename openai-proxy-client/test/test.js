/**
 * HTTP integration tests against a running server (npm start). Requires env for chat tests.
 */
const path = require('path');
const http = require('http');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

const PORT = process.env.PORT || 3000;
const HAS_API_KEY = process.env.OPENAI_API_KEY && process.env.OPENAI_API_KEY !== 'your_api_key_here';

function parseSseDataJsonObjects(body) {
  const out = [];
  for (const line of body.split('\n')) {
    if (!line.startsWith('data:')) {
      continue;
    }
    const rest = line.slice('data:'.length).trimStart();
    if (rest === '[DONE]') {
      continue;
    }
    try {
      out.push(JSON.parse(rest));
    } catch {
      // ignore non-JSON lines
    }
  }
  return out;
}

function makeRequest(options, data) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          body: body,
        });
      });
    });

    req.on('error', reject);

    if (data) {
      req.write(JSON.stringify(data));
    }
    req.end();
  });
}

async function testHealthEndpoint() {
  console.log('\n=== Testing /health endpoint ===');
  const response = await makeRequest({
    hostname: 'localhost',
    port: PORT,
    path: '/health',
    method: 'GET',
  });

  console.log(`Status: ${response.statusCode}`);
  const data = JSON.parse(response.body);
  console.log('Response:', JSON.stringify(data, null, 2));
  return (
    response.statusCode === 200 &&
    data.status === 'ok' &&
    typeof data.upstreamModel === 'string' &&
    data.upstreamModel.length > 0
  );
}

async function testChatCompletionsNonStream() {
  if (!HAS_API_KEY) {
    console.log('\n=== Skipping /chat/completions (non-stream) - No API key ===');
    return true;
  }

  console.log('\n=== Testing /chat/completions (non-stream) ===');
  const testModel = 'gpt-4-turbo-custom';
  const response = await makeRequest({
    hostname: 'localhost',
    port: PORT,
    path: '/chat/completions',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  }, {
    model: testModel,
    messages: [
      { role: 'user', content: 'Say "Hello, world!"' },
    ],
    stream: false,
  });

  console.log(`Status: ${response.statusCode}`);
  console.log(`Content-Type: ${response.headers['content-type']}`);

  const isJson = response.headers['content-type']?.includes('application/json');
  let data;
  try {
    data = JSON.parse(response.body);
  } catch (e) {
    console.log('Response body:', response.body);
    return false;
  }
  const modelWasRestored = data.model === testModel;
  const hasChoices = data.choices && data.choices.length > 0;
  const hasAttestation = data.attestation != null && typeof data.attestation === 'object';
  console.log('Response model:', data.model);
  console.log(`Model restored to client model: ${modelWasRestored ? 'Yes' : 'No'}`);
  console.log(`Has attestation: ${hasAttestation ? 'Yes' : 'No'}`);
  return response.statusCode === 200 && isJson && modelWasRestored && hasChoices && hasAttestation;
}

async function testChatCompletionsStream() {
  if (!HAS_API_KEY) {
    console.log('\n=== Skipping /chat/completions (stream) - No API key ===');
    return true;
  }

  console.log('\n=== Testing /chat/completions (stream) ===');
  const testModel = 'gpt-4-turbo-stream';
  return new Promise((resolve) => {
    const req = http.request({
      hostname: 'localhost',
      port: PORT,
      path: '/chat/completions',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    }, (res) => {
      console.log(`Status: ${res.statusCode}`);
      console.log(`Content-Type: ${res.headers['content-type']}`);

      const isStream = res.headers['content-type']?.includes('text/event-stream');
      console.log('Is stream:', isStream);

      let chunks = 0;
      let hasData = false;
      let hasDone = false;
      let modelWasRestored = false;
      let finalEventHasAttestation = false;
      let lastJsonEvent = null;

      res.on('data', (chunk) => {
        const text = chunk.toString();
        chunks++;
        if (text.includes('data: ')) hasData = true;
        if (text.includes('[DONE]')) hasDone = true;

        const lines = text.split('\n');
        for (const line of lines) {
          if (line.startsWith('data: ') && !line.includes('[DONE]')) {
            try {
              const json = JSON.parse(line.slice(6));
              lastJsonEvent = json;
              if (
                json.object === 'chat.completion.chunk' &&
                typeof json.model === 'string' &&
                json.model === testModel
              ) {
                modelWasRestored = true;
              }
            } catch (e) {
              // ignore
            }
          }
        }
      });

      res.on('end', () => {
        finalEventHasAttestation =
          !!lastJsonEvent &&
          lastJsonEvent.object === 'chat.completion.chunk' &&
          !!lastJsonEvent.attestation &&
          typeof lastJsonEvent.attestation === 'object';

        console.log(`Received ${chunks} chunks`);
        console.log(`Has data events: ${hasData}`);
        console.log(`Has [DONE]: ${hasDone}`);
        console.log(`Model restored in SSE chunks: ${modelWasRestored ? 'Yes' : 'No'}`);
        console.log(`Final SSE event has attestation: ${finalEventHasAttestation ? 'Yes' : 'No'}`);
        resolve(
          res.statusCode === 200 &&
            isStream &&
            hasData &&
            hasDone &&
            modelWasRestored &&
            finalEventHasAttestation,
        );
      });
    });

    req.on('error', (err) => {
      console.error('Stream request error:', err.message);
      resolve(false);
    });

    req.write(JSON.stringify({
      model: testModel,
      messages: [
        { role: 'user', content: 'Say "Hello, world!"' },
      ],
      stream: true,
    }));
    req.end();
  });
}

async function runTests() {
  console.log('='.repeat(50));
  console.log('Running OpenAI Proxy Tests');
  if (!HAS_API_KEY) {
    console.log('Note: No OPENAI_API_KEY set, running basic tests only');
  }
  console.log('='.repeat(50));

  const results = {
    health: await testHealthEndpoint(),
    chatNonStream: await testChatCompletionsNonStream(),
    chatStream: await testChatCompletionsStream(),
  };

  console.log('\n'.repeat(2));
  console.log('='.repeat(50));
  console.log('Test Results');
  console.log('='.repeat(50));
  console.log(`Health Endpoint: ${results.health ? 'PASS' : 'FAIL'}`);
  console.log(`Chat (non-stream): ${results.chatNonStream ? 'PASS' : 'FAIL'}`);
  console.log(`Chat (stream): ${results.chatStream ? 'PASS' : 'FAIL'}`);
  console.log('='.repeat(50));

  const allPassed = Object.values(results).every(v => v);
  process.exit(allPassed ? 0 : 1);
}

runTests().catch((err) => {
  console.error('Test suite error:', err);
  process.exit(1);
});
