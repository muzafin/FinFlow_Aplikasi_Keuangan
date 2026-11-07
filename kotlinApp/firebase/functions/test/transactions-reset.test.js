const { test } = require('node:test');
const assert = require('node:assert/strict');
const { deleteTransactionsRequest, deleteAllTransactions, managedPatch, BATCH_LIMIT } = require('../transactions-reset');

const clone = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value));

function fixture(extra = []) {
  const records = new Map([
    ['users/owner', { id: 'owner', name: 'Pemilik' }],
    ['users/owner/accounts/cash', { id: 'cash', openingBalanceMinor: 5000 }],
    ['users/owner/categories/food', { id: 'food' }],
    ['users/owner/budgets/b1', { id: 'b1', limitMinor: 100 }],
    ['users/owner/transactions/manual', { id: 'manual', source: 'manual' }],
    ['users/owner/transactions/bill-pay', { id: 'bill-pay', source: 'bill' }],
    ['users/owner/transactions/goal-dep', { id: 'goal-dep', source: 'goal_deposit' }],
    ['users/owner/financialEvents/bill-pay', { id: 'bill-pay', kind: 'bill' }],
    ['users/owner/financialEvents/goal-dep', { id: 'goal-dep', kind: 'goal_deposit' }],
    ['users/owner/financialEvents/debt-pay', { id: 'debt-pay', kind: 'debt_payment', transactionId: null }],
    ['users/owner/reimbursements/manual', { id: 'manual', receivedAt: 0 }],
    ['users/owner/bills/paid', { id: 'paid', paidAt: 77, lastPaymentId: 'bill-pay', revision: 3 }],
    ['users/owner/bills/open', { id: 'open', paidAt: 0, lastPaymentId: null, revision: 1 }],
    ['users/owner/debts/d1', { id: 'd1', paidMinor: 400, lastEventId: 'debt-pay', revision: 2, includeBalance: false }],
    ['users/owner/goals/g1', { id: 'g1', savedMinor: 1500, legacySavedMinor: 500, holdingAccountId: 'goal-g1', lastEventId: 'goal-dep', revision: 4 }],
    ['users/owner/goals/manual', { id: 'manual', savedMinor: 900, legacySavedMinor: 0, holdingAccountId: null, lastEventId: null, revision: 0 }],
    ['users/owner/notes/bundle', { id: 'bundle', kind: 'bundle', lastPaymentId: 'bundle-pay', revision: 2, items: [
      { id: 'a', name: 'A', amountMinor: 100, checked: true, paymentId: 'bundle-pay' },
      { id: 'b', name: 'B', amountMinor: 200, checked: false, paymentId: null },
    ] }],
    ['users/owner/notes/text', { id: 'text', title: 'Teks', body: 'tetap' }],
    ['users/owner/recurring/r1', { id: 'r1', lastEventId: 'rec-1', nextDate: '2026-12-01', revision: 5 }],
    ['users/owner/wishes/w1', { id: 'w1' }],
    ['users/victim/transactions/keep', { id: 'keep' }],
    ['users/victim/bills/keep', { id: 'keep', paidAt: 9, lastPaymentId: 'x', revision: 1 }],
    ...extra,
  ]);
  const commits = [];
  const failures = { commitAt: null };
  const doc = path => ({ path, collection: name => collection(`${path}/${name}`) });
  const query = (path, selected) => ({
    select: () => query(path, true),
    get: async () => {
      const docs = [...records.keys()].filter(key => key.startsWith(`${path}/`) && !key.slice(path.length + 1).includes('/'))
        .map(key => ({ ref: doc(key), updateTime: { key }, data: () => selected ? {} : clone(records.get(key)) }));
      return { docs, size: docs.length };
    },
  });
  const collection = path => ({ doc: id => doc(`${path}/${id}`), ...query(path, false) });
  const db = {
    collection: name => ({ doc: id => ({ ...doc(`${name}/${id}`), get: async () => ({ exists: records.has(`${name}/${id}`), data: () => clone(records.get(`${name}/${id}`)) }) }) }),
    batch: () => {
      const writes = [];
      return {
        update: (ref, patch, precondition) => { assert.ok(precondition?.lastUpdateTime, 'update must carry a precondition'); writes.push(['update', ref.path, clone(patch)]); },
        delete: ref => writes.push(['delete', ref.path]),
        commit: async () => {
          assert.ok(writes.length <= BATCH_LIMIT);
          if (failures.commitAt === commits.length) { failures.commitAt = null; throw new Error('commit interrupted'); }
          commits.push(writes.length);
          for (const [kind, path, patch] of writes) {
            if (kind === 'delete') records.delete(path);
            else { assert.ok(records.has(path)); records.set(path, { ...records.get(path), ...patch }); }
          }
        },
      };
    },
  };
  return { db, records, commits, failures };
}

function assertCleared(f) {
  const owner = [...f.records.keys()].filter(key => key.startsWith('users/owner/'));
  for (const name of ['transactions', 'financialEvents', 'reimbursements'])
    assert.equal(owner.filter(key => key.startsWith(`users/owner/${name}/`)).length, 0, name);
  for (const key of ['users/owner', 'users/owner/accounts/cash', 'users/owner/categories/food', 'users/owner/budgets/b1', 'users/owner/wishes/w1', 'users/owner/notes/text'])
    assert.ok(f.records.has(key), key);
  assert.deepEqual(f.records.get('users/owner/bills/paid'), { id: 'paid', paidAt: 0, lastPaymentId: null, revision: 4 });
  assert.equal(f.records.get('users/owner/bills/open').revision, 1);
  assert.deepEqual(f.records.get('users/owner/debts/d1'), { id: 'd1', paidMinor: 0, lastEventId: null, revision: 3, includeBalance: false });
  const goal = f.records.get('users/owner/goals/g1');
  assert.equal(goal.savedMinor, 500); assert.equal(goal.lastEventId, null); assert.equal(goal.revision, 5); assert.equal(goal.holdingAccountId, 'goal-g1');
  assert.deepEqual(f.records.get('users/owner/goals/manual'), { id: 'manual', savedMinor: 900, legacySavedMinor: 0, holdingAccountId: null, lastEventId: null, revision: 0 });
  const bundle = f.records.get('users/owner/notes/bundle');
  assert.equal(bundle.lastPaymentId, null); assert.equal(bundle.revision, 3);
  assert.deepEqual(bundle.items, [
    { id: 'a', name: 'A', amountMinor: 100, checked: false, paymentId: null },
    { id: 'b', name: 'B', amountMinor: 200, checked: false, paymentId: null },
  ]);
  const recurring = f.records.get('users/owner/recurring/r1');
  assert.equal(recurring.lastEventId, null); assert.equal(recurring.nextDate, '2026-12-01'); assert.equal(recurring.revision, 6);
  assert.ok(f.records.has('users/victim/transactions/keep'));
  assert.deepEqual(f.records.get('users/victim/bills/keep'), { id: 'keep', paidAt: 9, lastPaymentId: 'x', revision: 1 });
}

test('request needs a signed-in caller and the exact confirmation only', () => {
  assert.deepEqual(deleteTransactionsRequest({ uid: 'owner' }, { confirmation: 'HAPUS TRANSAKSI' }), { uid: 'owner' });
  for (const [auth, data] of [
    [null, { confirmation: 'HAPUS TRANSAKSI' }], [{ uid: '' }, { confirmation: 'HAPUS TRANSAKSI' }],
    [{ uid: 'a/b' }, { confirmation: 'HAPUS TRANSAKSI' }], [{ uid: 'owner' }, { confirmation: 'HAPUS' }],
    [{ uid: 'owner' }, { confirmation: 'HAPUS TRANSAKSI', uid: 'victim' }], [{ uid: 'owner' }, null], [{ uid: 'owner' }, []],
  ]) assert.throws(() => deleteTransactionsRequest(auth, data));
});

test('deletes journal, events and claims of the caller and resets managed payment state', async () => {
  const f = fixture();
  const result = await deleteAllTransactions('owner', f);
  assert.deepEqual(result, { completed: true, transactions: 3, events: 3, reimbursements: 1, updated: 5 });
  assertCleared(f);
});

test('is idempotent: a second run changes nothing', async () => {
  const f = fixture();
  await deleteAllTransactions('owner', f);
  const snapshot = JSON.stringify([...f.records.entries()]);
  const result = await deleteAllTransactions('owner', f);
  assert.deepEqual(result, { completed: true, transactions: 0, events: 0, reimbursements: 0, updated: 0 });
  assert.equal(JSON.stringify([...f.records.entries()]), snapshot);
});

test('large histories are split into bounded batches and an interrupted run resumes', async () => {
  const many = Array.from({ length: 950 }, (_, i) => [`users/owner/transactions/t${i}`, { id: `t${i}` }]);
  const f = fixture(many);
  f.failures.commitAt = 1;
  await assert.rejects(deleteAllTransactions('owner', f));
  assert.ok([...f.records.keys()].some(key => key.startsWith('users/owner/transactions/')));
  await deleteAllTransactions('owner', f);
  assertCleared(f);
  assert.ok(f.commits.every(count => count <= BATCH_LIMIT));
});

test('pending account deletion or data reset blocks the request before any write', async () => {
  for (const [path, value] of [['_accountDeletions/owner', { status: 'pending' }], ['_dataResets/owner', { status: 'pending' }]]) {
    const f = fixture([[path, value]]);
    await assert.rejects(deleteAllTransactions('owner', f), { code: 'failed-precondition' });
    assert.deepEqual(f.commits, []);
    assert.ok(f.records.has('users/owner/transactions/manual'));
  }
  const f = fixture([['_dataResets/owner', { status: 'complete' }]]);
  await deleteAllTransactions('owner', f);
  assertCleared(f);
});

test('managed patch leaves consistent documents untouched', () => {
  assert.equal(managedPatch('bills', { paidAt: 0, lastPaymentId: null }), null);
  assert.equal(managedPatch('debts', { paidMinor: 0, lastEventId: null }), null);
  assert.equal(managedPatch('goals', { savedMinor: 3, legacySavedMinor: 3, holdingAccountId: 'goal-x', lastEventId: null }), null);
  assert.equal(managedPatch('notes', { kind: 'note' }), null);
  assert.equal(managedPatch('recurring', { lastEventId: null }), null);
  assert.equal(managedPatch('wishes', { anything: true }), null);
  assert.deepEqual(managedPatch('goals', { savedMinor: 10, holdingAccountId: 'goal-x', lastEventId: 'e' }), { savedMinor: 0, lastEventId: null, revision: 1 });
});
