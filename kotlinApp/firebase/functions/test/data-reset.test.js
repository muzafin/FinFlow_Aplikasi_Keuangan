const { test } = require('node:test');
const assert = require('node:assert/strict');
const { resetRequest, resetFinancialData } = require('../data-reset');

const clone = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value));
const profile = { id: 'owner', name: 'Nama tetap', email: 'old@example.test', baseCurrency: 'USD', schemaVersion: 2,
  photoPath: 'users/owner/profile/avatar.jpg', photoUpdatedAt: 99 };
const firstId = 'reset-first-1234';
const secondId = 'reset-second-5678';

function fixture() {
  const records = new Map([
    ['users/owner', clone(profile)], ['users/owner/accounts/bank', { amountMinor: 5000 }],
    ['users/owner/transactions/expense', { amountMinor: 1000 }], ['users/victim', { untouched: true }],
    ['users/victim/accounts/bank', { amountMinor: 9999 }],
  ]);
  const files = new Set(['users/owner/profile/avatar.jpg', 'users/owner/other/file', 'users/victim/profile/avatar.jpg']);
  const calls = [];
  const failures = { storage: false, final: false, readPath: null, accountDeletionAfterDelete: false };
  const doc = path => ({ path, collection: name => ({ doc: id => doc(`${path}/${name}/${id}`) }) });
  const snapshot = ref => {
    if (failures.readPath === ref.path) throw new Error('unreadable');
    return { exists: records.has(ref.path), data: () => clone(records.get(ref.path)) };
  };
  const db = {
    collection: name => ({ doc: id => doc(`${name}/${id}`) }),
    runTransaction: async callback => {
      const writes = [];
      const result = await callback({ get: async ref => snapshot(ref), set: (ref, data) => writes.push([ref.path, clone(data)]) });
      if (failures.final && writes.some(([path, data]) => path === '_dataResets/owner' && data.status === 'complete')) {
        failures.final = false; throw new Error('commit interrupted');
      }
      for (const [path, data] of writes) records.set(path, data);
      return result;
    },
    recursiveDelete: async ref => {
      calls.push(`delete:${ref.path}`);
      for (const path of records.keys()) if (path === ref.path || path.startsWith(`${ref.path}/`)) records.delete(path);
      if (failures.accountDeletionAfterDelete) records.set('_accountDeletions/owner', { status: 'pending' });
    },
  };
  const auth = {
    getUser: async uid => { calls.push(`auth-read:${uid}`); return { uid, email: 'current@example.test' }; },
    deleteUser: async () => { throw new Error('Auth deletion must never be called'); },
    revokeRefreshTokens: async () => { throw new Error('Token revocation must never be called'); },
  };
  const bucket = { deleteFiles: async ({ prefix }) => {
    calls.push(`files:${prefix}`);
    const pending = records.get('_dataResets/owner');
    assert.equal(pending.status, 'pending');
    assert.equal(pending.profile.name, profile.name);
    assert.equal(pending.profile.photoPath, undefined);
    if (failures.storage) throw new Error('storage unavailable');
    for (const path of files) if (path.startsWith(prefix)) files.delete(path);
  } };
  return { db, auth, bucket, records, files, calls, failures };
}

function assertReset(f) {
  assert.deepEqual(f.records.get('users/owner'), {
    id: 'owner', name: profile.name, email: 'current@example.test', baseCurrency: 'USD', schemaVersion: 2,
  });
  const cash = f.records.get('users/owner/accounts/acc_cash');
  assert.equal(cash.currency, 'USD'); assert.equal(cash.openingBalanceMinor, 0); assert.equal(cash.archived, false);
  assert.ok(Number.isInteger(cash.createdAt));
  const seeded = [...f.records.entries()].filter(([path]) => path.startsWith('users/owner/categories/'));
  assert.equal(seeded.length, 11);
  assert.deepEqual(seeded.map(([, value]) => value.id).sort(),
    ['food', 'transport', 'shopping', 'health', 'bills', 'education', 'entertainment', 'other_expense', 'salary', 'business', 'other_income'].sort());
  assert.equal(f.records.get('users/owner/categories/food').name, 'Makan & minum');
  assert.equal(f.records.get('users/owner/categories/salary').type, 'income');
  assert.equal(f.records.has('users/owner/accounts/bank'), false);
  assert.equal(f.records.has('users/owner/transactions/expense'), false);
  assert.deepEqual(f.records.get('users/victim'), { untouched: true });
  assert.deepEqual(f.records.get('users/victim/accounts/bank'), { amountMinor: 9999 });
  assert.deepEqual([...f.files], ['users/victim/profile/avatar.jpg']);
  assert.equal(f.records.get('_dataResets/owner').status, 'complete');
  assert.equal(f.records.get('_dataResets/owner').profile, undefined);
}

test('reset request accepts only the recent caller, exact confirmation and bounded request ID', () => {
  const auth = { uid: 'owner', token: { auth_time: 900 } };
  const data = { confirmation: 'HAPUS DATA', requestId: firstId };
  assert.deepEqual(resetRequest(auth, data, 1000), { uid: 'owner', requestId: firstId });
  assert.equal(resetRequest({ ...auth, token: { auth_time: 700 } }, data, 1000).uid, 'owner');
  for (const [identity, input] of [
    [null, data], [{ uid: 'owner' }, data], [{ ...auth, token: { auth_time: 699 } }, data],
    [{ ...auth, token: { auth_time: 2000 } }, data], [auth, { ...data, uid: 'victim' }],
    [auth, { ...data, confirmation: 'HAPUS' }], [auth, { ...data, requestId: 'short' }],
    [auth, { ...data, requestId: 'x'.repeat(101) }], [auth, { ...data, requestId: 'reset/forged' }],
    [auth, null], [auth, []],
  ]) assert.throws(() => resetRequest(identity, input, 1000));
});

test('reset removes only owner finances, retains Auth and atomically restores profile and defaults', async () => {
  const f = fixture();
  assert.deepEqual(await resetFinancialData('owner', firstId, f), { completed: true, requestId: firstId });
  assertReset(f);
  assert.deepEqual(f.calls, ['auth-read:owner', 'files:users/owner/', 'delete:users/owner']);
  assert.equal(f.records.get(`_dataResets/owner/requests/${firstId}`).status, 'complete');
});

test('storage failure keeps a durable profile, rejects a different pending request and resumes', async () => {
  const f = fixture(); f.failures.storage = true;
  await assert.rejects(resetFinancialData('owner', firstId, f));
  assert.equal(f.records.get('_dataResets/owner').status, 'pending');
  assert.equal(f.records.get('users/owner/accounts/bank').amountMinor, 5000);
  const count = f.calls.length;
  await assert.rejects(resetFinancialData('owner', secondId, f), { code: 'failed-precondition' });
  assert.equal(f.calls.length, count);
  f.records.delete('users/owner'); f.failures.storage = false;
  await resetFinancialData('owner', firstId, f);
  assertReset(f);
});

test('interrupted final commit resumes even after the original profile was deleted', async () => {
  const f = fixture(); f.failures.final = true;
  await assert.rejects(resetFinancialData('owner', firstId, f));
  assert.equal(f.records.has('users/owner'), false);
  assert.equal(f.records.get('_dataResets/owner').status, 'pending');
  await resetFinancialData('owner', firstId, f);
  assertReset(f);
});

test('completed requests remain idempotent even after a later reset completed', async () => {
  const f = fixture();
  await resetFinancialData('owner', firstId, f);
  f.records.set('users/owner/transactions/new', { amountMinor: 88 });
  let count = f.calls.length;
  await resetFinancialData('owner', firstId, f);
  assert.equal(f.calls.length, count);
  assert.equal(f.records.get('users/owner/transactions/new').amountMinor, 88);
  await resetFinancialData('owner', secondId, f);
  f.records.set('users/owner/transactions/newer', { amountMinor: 99 });
  count = f.calls.length;
  await resetFinancialData('owner', firstId, f);
  assert.equal(f.calls.length, count);
  assert.equal(f.records.get('users/owner/transactions/newer').amountMinor, 99);
});

test('missing, unreadable or malformed original profile fails before pending or deletion', async () => {
  for (const mode of ['missing', 'unreadable', 'malformed']) {
    const f = fixture();
    if (mode === 'missing') f.records.delete('users/owner');
    if (mode === 'unreadable') f.failures.readPath = 'users/owner';
    if (mode === 'malformed') f.records.set('users/owner', { ...profile, baseCurrency: 'invalid' });
    await assert.rejects(resetFinancialData('owner', firstId, f));
    assert.equal(f.records.has('_dataResets/owner'), false);
    assert.deepEqual(f.calls, []);
    assert.equal(f.records.get('users/owner/accounts/bank').amountMinor, 5000);
  }
});

test('account deletion blocks reset before any financial data is removed', async () => {
  const f = fixture(); f.records.set('_accountDeletions/owner', { status: 'pending' });
  await assert.rejects(resetFinancialData('owner', firstId, f), { code: 'failed-precondition' });
  assert.deepEqual(f.calls, []);
  assert.equal(f.records.has('_dataResets/owner'), false);
});

test('account deletion started during reset prevents profile recreation', async () => {
  const f = fixture(); f.failures.accountDeletionAfterDelete = true;
  await assert.rejects(resetFinancialData('owner', firstId, f), { code: 'failed-precondition' });
  assert.equal(f.records.has('users/owner'), false);
  assert.equal(f.records.get('_dataResets/owner').status, 'pending');
  assert.deepEqual(f.records.get('users/victim'), { untouched: true });
});
