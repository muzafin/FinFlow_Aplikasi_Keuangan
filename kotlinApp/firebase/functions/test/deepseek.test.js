const test = require('node:test');
const assert = require('node:assert/strict');
const { z } = require('zod');
const { createDeepSeekClient, jsonOutputFormat, mapDeepSeekError, DeepSeekError } = require('../deepseek');

function fakeFetch(reply, status = 200) {
  const calls = [];
  const impl = async (url, init) => {
    calls.push({ url, init, body: JSON.parse(init.body) });
    return { ok: status >= 200 && status < 300, status, json: async () => reply };
  };
  impl.calls = calls;
  return impl;
}

const Result = z.object({ transactions: z.array(z.object({ amount: z.number() })), message: z.string().nullable() });

test('parse sends JSON mode with schema in system prompt and validates output', async () => {
  const fetchImpl = fakeFetch({ choices: [{ finish_reason: 'stop', message: { content: '{"transactions":[{"amount":50000}],"message":null}' } }] });
  const client = createDeepSeekClient({ apiKey: 'test-key', fetchImpl });
  const response = await client.messages.parse({
    model: 'deepseek-chat', max_tokens: 4000, output_config: { format: jsonOutputFormat(Result) },
    system: [{ type: 'text', text: 'Ekstrak transaksi.' }], messages: [{ role: 'user', content: 'makan 50rb' }],
  });
  assert.deepEqual(response.parsed_output, { transactions: [{ amount: 50000 }], message: null });
  assert.equal(response.stop_reason, 'end_turn');
  const call = fetchImpl.calls[0];
  assert.equal(call.url, 'https://api.deepseek.com/chat/completions');
  assert.equal(call.init.headers.authorization, 'Bearer test-key');
  assert.deepEqual(call.body.response_format, { type: 'json_object' });
  assert.equal(call.body.messages[0].role, 'system');
  assert.match(call.body.messages[0].content, /JSON Schema/);
  assert.equal(call.body.messages[1].content, 'makan 50rb');
  assert.deepEqual(call.body.thinking, { type: 'disabled' });
  assert.equal(call.body.temperature, 0);
});

test('invalid JSON yields null parsed_output and length maps to max_tokens', async () => {
  const client = createDeepSeekClient({ apiKey: 'k', fetchImpl: fakeFetch({ choices: [{ finish_reason: 'length', message: { content: '{"transactions":' } }] }) });
  const response = await client.messages.parse({ model: 'm', output_config: { format: jsonOutputFormat(Result) }, system: [], messages: [{ role: 'user', content: 'x' }] });
  assert.equal(response.parsed_output, null);
  assert.equal(response.stop_reason, 'max_tokens');
});

test('image blocks become image_url data URLs for vision', async () => {
  const fetchImpl = fakeFetch({ choices: [{ finish_reason: 'stop', message: { content: '{"transactions":[],"message":null}' } }] });
  const client = createDeepSeekClient({ apiKey: 'k', fetchImpl });
  const response = await client.messages.parse({ model: 'm', output_config: { format: jsonOutputFormat(Result) }, system: [],
    messages: [{ role: 'user', content: [
      { type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: 'AAAA' } },
      { type: 'text', text: 'baca struk ini' },
    ] }] });
  assert.deepEqual(response.parsed_output, { transactions: [], message: null });
  assert.equal(fetchImpl.calls.length, 1);
  assert.deepEqual(fetchImpl.calls[0].body.messages[1].content, [
    { type: 'image_url', image_url: { url: 'data:image/jpeg;base64,AAAA' } },
    { type: 'text', text: 'baca struk ini' },
  ]);
});

test('fenced JSON content is still parsed', async () => {
  const fetchImpl = fakeFetch({ choices: [{ finish_reason: 'stop', message: { content: '```json\n{"transactions":[{"amount":5}],"message":null}\n```' } }] });
  const client = createDeepSeekClient({ apiKey: 'k', fetchImpl });
  const response = await client.messages.parse({ model: 'm', output_config: { format: jsonOutputFormat(Result) }, system: [], messages: [{ role: 'user', content: 'x' }] });
  assert.deepEqual(response.parsed_output, { transactions: [{ amount: 5 }], message: null });
});

test('assistant create returns text content with low reasoning effort', async () => {
  const fetchImpl = fakeFetch({ choices: [{ finish_reason: 'stop', message: { content: 'Halo!' } }] });
  const client = createDeepSeekClient({ apiKey: 'k', fetchImpl });
  const response = await client.beta.messages.create({ model: 'm', system: [{ type: 'text', text: 'sys' }], output_config: { effort: 'low' }, messages: [{ role: 'user', content: 'hai' }] });
  assert.equal(response.content[0].text, 'Halo!');
  assert.deepEqual(fetchImpl.calls[0].body.thinking, { type: 'enabled' });
  assert.equal(fetchImpl.calls[0].body.reasoning_effort, 'low');
});

test('HTTP errors map to friendly codes', async () => {
  const client = createDeepSeekClient({ apiKey: 'k', fetchImpl: fakeFetch({}, 429) });
  await assert.rejects(client.beta.messages.create({ model: 'm', system: [], messages: [{ role: 'user', content: 'x' }] }), DeepSeekError);
  assert.equal(mapDeepSeekError(new DeepSeekError(429, 'x')).code, 'unavailable');
  assert.equal(mapDeepSeekError(new DeepSeekError(401, 'x')).code, 'internal');
  assert.equal(mapDeepSeekError(new DeepSeekError(400, 'x')).code, 'invalid-argument');
  assert.equal(mapDeepSeekError(new Error('x')), null);
});
