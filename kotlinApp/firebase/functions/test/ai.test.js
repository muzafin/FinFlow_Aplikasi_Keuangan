const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createAi, FREE_LIMITS } = require('../ai');
const { createPremium, entitlementFrom } = require('../premium');

const clone = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value));
function fakeDb(initial = {}) {
  const records = new Map(Object.entries(initial).map(([k, v]) => [k, clone(v)]));
  const snap = path => ({ exists: records.has(path), data: () => clone(records.get(path)) });
  const doc = path => ({
    path, get: async () => snap(path),
    set: async (data, options) => records.set(path, options?.merge ? { ...(records.get(path) || {}), ...clone(data) } : clone(data)),
  });
  return {
    records, doc,
    runTransaction: async work => {
      const writes = [];
      const result = await work({ get: async ref => snap(ref.path), set: (ref, data) => writes.push([ref.path, clone(data)]) });
      for (const [path, data] of writes) records.set(path, data);
      return result;
    },
  };
}
const auth = { uid: 'owner' };
const NOW = Date.parse('2026-11-06T10:00:00Z');
const context = { today: '2026-11-06', baseCurrency: 'IDR', language: 'id',
  categories: [{ id: 'food', name: 'Makan', type: 'expense' }, { id: 'salary', name: 'Gaji', type: 'income' }],
  wallets: [{ id: 'acc_cash', name: 'Kas', currency: 'IDR' }] };

class FakeAnthropic { static APIError = class extends Error {}; static RateLimitError = class extends FakeAnthropic.APIError {};
  static AuthenticationError = class extends FakeAnthropic.APIError {}; static BadRequestError = class extends FakeAnthropic.APIError {}; }

function fakeClient(output, calls = []) {
  return {
    calls,
    messages: { parse: async params => { calls.push(params); if (output instanceof Error) throw output; return { stop_reason: 'end_turn', parsed_output: output }; } },
    beta: { messages: { create: async params => { calls.push(params); return { stop_reason: 'end_turn', content: [{ type: 'text', text: 'Pengeluaran terbesar: Makan.' }] }; } } },
  };
}

test('AI needs login and per-feature consent before any model call', async () => {
  const db = fakeDb();
  const client = fakeClient({ transactions: [], message: null });
  const ai = createAi({ db, client, Anthropic: FakeAnthropic, now: () => NOW });
  await assert.rejects(ai.parseText(null, { mode: 'chat', text: 'makan 20rb', context }), /Masuk/);
  await assert.rejects(ai.parseText(auth, { mode: 'chat', text: 'makan 20rb', context }), /Setujui/);
  assert.equal(client.calls.length, 0);
});

test('chat parse sanitizes model output to known ids and counts quota', async () => {
  const db = fakeDb({ 'users/owner/ai/consent': { chat: true } });
  const client = fakeClient({ transactions: [
    { type: 'expense', amount: 20000, currency: null, title: 'Makan siang', categoryId: 'food', walletId: 'acc_cash', toWalletId: null, date: null, note: null },
    { type: 'weird', amount: 5000, currency: 'idr', title: '', categoryId: 'hack', walletId: 'other', toWalletId: null, date: 'kemarin', note: null },
    { type: 'income', amount: -0, currency: null, title: 'Nol', categoryId: null, walletId: null, toWalletId: null, date: null, note: null },
  ], message: null });
  const ai = createAi({ db, client, Anthropic: FakeAnthropic, now: () => NOW });
  const result = await ai.parseText(auth, { mode: 'chat', text: 'makan siang 20rb, parkir 5rb', context });
  assert.equal(result.transactions.length, 2);
  assert.deepEqual(result.transactions[0], { type: 'expense', amount: 20000, currency: null, date: '2026-11-06', title: 'Makan siang', categoryId: 'food', walletId: 'acc_cash', toWalletId: null, note: null });
  assert.equal(result.transactions[1].type, 'expense');
  assert.equal(result.transactions[1].categoryId, null);
  assert.equal(result.transactions[1].walletId, null);
  assert.equal(result.transactions[1].date, '2026-11-06');
  assert.equal(db.records.get('users/owner/ai/usage').chat, 1);
  assert.match(client.calls[0].messages[0].content, /<input>\nmakan siang 20rb, parkir 5rb\n<\/input>/);
});

test('free quota stops at the limit and failed calls are refunded', async () => {
  const db = fakeDb({ 'users/owner/ai/consent': { scan: true }, 'users/owner/ai/usage': { month: '2026-11', scan: FREE_LIMITS.scan - 1 } });
  const failing = createAi({ db, client: fakeClient(new FakeAnthropic.RateLimitError('busy')), Anthropic: FakeAnthropic, now: () => NOW });
  const scan = { image: 'AAAA', mediaType: 'image/jpeg', ownerName: '', context };
  await assert.rejects(failing.scanReceipt(auth, scan), /sibuk/);
  assert.equal(db.records.get('users/owner/ai/usage').scan, FREE_LIMITS.scan - 1);
  const ok = createAi({ db, client: fakeClient({ receipts: [], message: 'Bukan struk' }), Anthropic: FakeAnthropic, now: () => NOW });
  assert.equal((await ok.scanReceipt(auth, scan)).message, 'Bukan struk');
  await assert.rejects(ok.scanReceipt(auth, scan), /Kuota AI gratis/);
});

test('monthly counters reset, premium is unlimited within the daily fair-use cap', async () => {
  const db = fakeDb({ 'users/owner/ai/consent': { voice: true }, 'users/owner/ai/usage': { month: '2026-10', voice: 30 },
    'users/owner/entitlements/premium': { active: true, expiresAt: NOW + 1000 } });
  const ai = createAi({ db, client: fakeClient({ transactions: [], message: null }), Anthropic: FakeAnthropic, now: () => NOW });
  await ai.parseText(auth, { mode: 'voice', text: 'gaji 10jt', context });
  assert.equal(db.records.get('users/owner/ai/usage').voice, 1);
  const status = await ai.status(auth);
  assert.equal(status.premium, true);
  assert.equal(status.usage.voice.limit, null);
});

test('assistant validates alternating turns and sends summary as data with fallback enabled', async () => {
  const db = fakeDb({ 'users/owner/ai/consent': { assistant: true } });
  const client = fakeClient(null);
  const ai = createAi({ db, client, Anthropic: FakeAnthropic, now: () => NOW });
  await assert.rejects(ai.assistant(auth, { messages: [{ role: 'assistant', text: 'hai' }], summary: 'x' }), /Urutan/);
  const result = await ai.assistant(auth, { messages: [{ role: 'user', text: 'Kategori termahal?' }], summary: 'Makan: Rp1.000.000', personality: 'strict', nickname: 'Bos', preference: '', language: 'id' });
  assert.match(result.reply, /Makan/);
  const call = client.calls[0];
  assert.deepEqual(call.betas, ['server-side-fallback-2026-07-01']);
  assert.equal(call.fallbacks, 'default');
  assert.ok(call.system.some(block => block.text.includes('<summary>\nMakan: Rp1.000.000')));
  assert.ok(call.system.some(block => block.text.includes('Bos')));
  assert.equal(db.records.get('users/owner/ai/usage').assistantLifetime, 1);
});

test('consent can be granted and withdrawn only for known features', async () => {
  const db = fakeDb();
  const ai = createAi({ db, now: () => NOW });
  await ai.setConsent(auth, { feature: 'scan', granted: true });
  await ai.setConsent(auth, { feature: 'scan', granted: false });
  assert.equal(db.records.get('users/owner/ai/consent').scan, false);
  await assert.rejects(ai.setConsent(auth, { feature: 'everything', granted: true }), /tidak dikenal/);
});

test('premium entitlement is read from RevenueCat and keeps admin grants', async () => {
  assert.deepEqual(entitlementFrom({ entitlements: { premium: { expires_date: null, product_identifier: 'finflow_lifetime' } }, non_subscriptions: { finflow_lifetime: [{}] } }, NOW),
    { active: true, expiresAt: null, productId: 'finflow_lifetime', store: 'lifetime' });
  assert.equal(entitlementFrom({ entitlements: { premium: { expires_date: '2026-11-01T00:00:00Z', product_identifier: 'm' } } }, NOW).active, false);
  const db = fakeDb({ 'users/owner': {}, 'users/owner/entitlements/premium': { active: false, manualUntil: NOW + 99 } });
  const premium = createPremium({ db, now: () => NOW, webhookSecret: () => 'Bearer s3cret',
    fetchSubscriber: async uid => ({ entitlements: { premium: { expires_date: '2027-01-01T00:00:00Z', product_identifier: 'annual' } }, subscriptions: { annual: { store: 'play_store' } } }) });
  const response = () => { const r = { code: 0, status(c) { r.code = c; return r; }, send() { return r; } }; return r; };
  const denied = response();
  await premium.webhook({ method: 'POST', get: () => 'Bearer wrong', body: { event: { app_user_id: 'owner' } } }, denied);
  assert.equal(denied.code, 401);
  assert.equal(db.records.get('users/owner/entitlements/premium').active, false);
  const accepted = response();
  await premium.webhook({ method: 'POST', get: () => 'Bearer s3cret', body: { event: { app_user_id: 'owner', original_app_user_id: '$RCAnonymousID:x' } } }, accepted);
  assert.equal(accepted.code, 200);
  const saved = db.records.get('users/owner/entitlements/premium');
  assert.equal(saved.active, true);
  assert.equal(saved.store, 'play_store');
  assert.equal(saved.manualUntil, NOW + 99);
  await assert.rejects(premium.refresh(null), /Masuk/);
});
