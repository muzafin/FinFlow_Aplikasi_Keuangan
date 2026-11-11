const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createPush, parseRegistration, localDate, billMessage, tokenId } = require('../push');

const TOKEN = 'fcm-token-'.padEnd(60, 'x');

test('registration requires auth and rejects unknown fields', () => {
  assert.throws(() => parseRegistration(null, { token: TOKEN }));
  assert.throws(() => parseRegistration({ uid: 'u' }, { token: TOKEN, uid: 'victim' }));
  assert.throws(() => parseRegistration({ uid: 'u' }, { token: 'short' }));
  const entry = parseRegistration({ uid: 'u' }, { token: TOKEN, platform: 'android', language: 'xx', timeZone: 'Not/AZone' });
  assert.equal(entry.language, 'en');
  assert.equal(entry.timeZone, 'Asia/Jakarta');
  assert.equal(entry.billReminders, true);
});

test('local dates follow the device time zone', () => {
  const now = new Date('2026-11-11T20:00:00Z'); // 03:00 12 Nov di Jakarta
  assert.equal(localDate(now, 'Asia/Jakarta'), '2026-11-12');
  assert.equal(localDate(now, 'Asia/Jakarta', 1), '2026-11-13');
  assert.equal(localDate(now, 'America/New_York'), '2026-11-11');
});

test('bill copy never contains amounts and prefers today over tomorrow', () => {
  assert.equal(billMessage('id', 0, 0), null);
  assert.equal(billMessage('id', 2, 1).kind, 'bill_today');
  assert.equal(billMessage('fr', 0, 1).title, 'Bills due tomorrow');
  assert.match(billMessage('id', 0, 3).body, /^3 tagihan/);
});

function fakeDb(initial) {
  const store = new Map(Object.entries(initial));
  const docRef = path => ({
    id: path.split('/').pop(), path,
    parent: { parent: { id: path.split('/')[1] } },
    get: async () => ({ exists: store.has(path), data: () => store.get(path) }),
    set: async (value, opts) => store.set(path, opts?.merge ? { ...(store.get(path) ?? {}), ...value } : value),
    delete: async () => store.delete(path),
  });
  const snapshot = paths => ({ docs: paths.map(p => ({ ref: docRef(p), data: () => store.get(p) })) });
  const collection = base => ({
    doc: id => ({ ...docRef(`${base}/${id}`), collection: name => collection(`${base}/${id}/${name}`) }),
    get: async () => snapshot([...store.keys()].filter(k => k.startsWith(`${base}/`) && k.split('/').length === base.split('/').length + 1)),
    where: (field, op, values) => ({
      get: async () => snapshot([...store.keys()].filter(k => k.startsWith(`${base}/`) && k.split('/').length === base.split('/').length + 1 && values.includes(store.get(k)[field]))),
    }),
  });
  const group = name => ({
    get: async () => snapshot([...store.keys()].filter(k => k.split('/').at(-2) === name)),
    where: (field, op, value) => ({ get: async () => snapshot([...store.keys()].filter(k => k.split('/').at(-2) === name && store.get(k)[field] === value)) }),
  });
  return { store, db: { collection, collectionGroup: group } };
}

test('register stores hashed id per user and moves a token between accounts', async () => {
  const { store, db } = fakeDb({});
  const push = createPush({ db, messaging: {}, now: () => new Date(0) });
  await push.register({ uid: 'a' }, { token: TOKEN, platform: 'android', language: 'id', timeZone: 'Asia/Jakarta' });
  assert.ok(store.has(`users/a/pushTokens/${tokenId(TOKEN)}`));
  await push.register({ uid: 'b' }, { token: TOKEN, platform: 'android', language: 'id', timeZone: 'Asia/Jakarta' });
  assert.ok(!store.has(`users/a/pushTokens/${tokenId(TOKEN)}`));
  assert.ok(store.has(`users/b/pushTokens/${tokenId(TOKEN)}`));
  await push.unregister({ uid: 'b' }, { token: TOKEN });
  assert.equal(store.size, 0);
});

test('bill reminders notify only users with active bills due and prune dead tokens', async () => {
  const dead = 'dead-token-'.padEnd(60, 'y');
  const { store, db } = fakeDb({
    [`users/a/pushTokens/${tokenId(TOKEN)}`]: { tokenId: tokenId(TOKEN), token: TOKEN, language: 'id', timeZone: 'Asia/Jakarta', billReminders: true },
    [`users/a/pushTokens/${tokenId(dead)}`]: { tokenId: tokenId(dead), token: dead, language: 'id', timeZone: 'Asia/Jakarta', billReminders: true },
    'users/a/bills/b1': { dueDate: '2026-11-12', archived: false },
    'users/a/bills/b2': { dueDate: '2026-11-12', archived: true },
    'users/a/bills/b3': { dueDate: '2026-12-01', archived: false },
    [`users/c/pushTokens/x`]: { tokenId: 'x', token: TOKEN + 'c', language: 'en', timeZone: 'Asia/Jakarta', billReminders: true },
  });
  const sent = [];
  const messaging = { send: async message => {
    if (message.token === dead) throw Object.assign(new Error('gone'), { code: 'messaging/registration-token-not-registered' });
    sent.push(message);
  } };
  const push = createPush({ db, messaging, now: () => new Date('2026-11-11T01:00:00Z') }); // 08:00 WIB 11 Nov
  const result = await push.billReminders();
  assert.equal(result.sent, 1);
  assert.equal(sent[0].notification.title, 'Tagihan jatuh tempo besok');
  assert.equal(sent[0].android.notification.channelId, 'bills');
  assert.doesNotMatch(JSON.stringify(sent[0]), /amount|Minor/);
  assert.ok(!store.has(`users/a/pushTokens/${tokenId(dead)}`));
});

test('bill reminders reach all eligible accounts and devices without crossing account preferences', async () => {
  const entry = token => ({ token, language: 'id', timeZone: 'Asia/Jakarta', billReminders: true });
  const { db } = fakeDb({
    'users/first/pushTokens/phone': entry('first-phone'),
    'users/first/pushTokens/tablet': entry('first-tablet'),
    'users/first/bills/due': { dueDate: '2026-11-11', archived: false },
    'users/second/pushTokens/phone': entry('second-phone'),
    'users/second/pushTokens/disabled': { ...entry('second-disabled'), billReminders: false },
    'users/second/bills/due': { dueDate: '2026-11-12', archived: false },
    'users/third/pushTokens/phone': entry('third-no-bills'),
  });
  const sent = [];
  const push = createPush({ db, messaging: { send: async message => { sent.push(message); } }, now: () => new Date('2026-11-11T01:00:00Z') });
  const result = await push.billReminders();
  assert.equal(result.users, 3);
  assert.equal(result.sent, 3);
  assert.deepEqual(sent.map(message => message.token).sort(), ['first-phone', 'first-tablet', 'second-phone']);
  assert.equal(sent.find(message => message.token === 'second-phone').notification.title, 'Tagihan jatuh tempo besok');
  assert.notEqual(sent.find(message => message.token === 'first-phone').notification.title, 'Tagihan jatuh tempo besok');
});
