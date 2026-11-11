const { test } = require('node:test');
const assert = require('node:assert/strict');
const { applyTransaction, backfill, levelFor, levelStart, maskName, eligible, emptyStats, createGamification } = require('../gamification');

const at = iso => new Date(iso).getTime();

test('level curve: 100 XP for level 2, 300 for level 3', () => {
  assert.equal(levelFor(0), 1); assert.equal(levelFor(99), 1); assert.equal(levelFor(100), 2);
  assert.equal(levelFor(299), 2); assert.equal(levelFor(300), 3); assert.equal(levelFor(600), 4);
  assert.equal(levelStart(3), 300);
});

test('names are masked', () => {
  assert.equal(maskName('muhammad zaenal arifin'), 'Muhammad Z.');
  assert.equal(maskName('JOHAN RIFQI'), 'Johan R.');
  assert.equal(maskName('Budi'), 'Budi');
  assert.equal(maskName(''), 'Pengguna Kasiva');
});

test('only user-recorded, fresh transactions are eligible', () => {
  const now = at('2026-11-11T05:00:00Z');
  assert.ok(eligible({ source: 'manual', scheduled: false, createdAt: now }, now));
  assert.ok(eligible({ source: 'notification', scheduled: false, createdAt: now }, now));
  for (const source of ['ivy', 'finflow', 'recurring', 'debt_initial', 'bill']) assert.ok(!eligible({ source, scheduled: false, createdAt: now }, now));
  assert.ok(!eligible({ source: 'manual', scheduled: true, createdAt: now }, now));
  assert.ok(!eligible({ source: 'manual', scheduled: false, createdAt: now - 3 * 86_400_000 }, now));
});

test('daily bonus, cap of 20 transactions per day, streaks and month rollover', () => {
  let stats = emptyStats();
  const day1 = at('2026-11-10T02:00:00Z');
  let gainedTotal = 0;
  for (let i = 0; i < 25; i += 1) { const r = applyTransaction(stats, day1 + i); stats = r.stats; gainedTotal += r.gained; }
  assert.equal(gainedTotal, 10 + 20 * 5);
  assert.equal(stats.transactions, 20);
  assert.equal(stats.streak, 1);
  stats = applyTransaction(stats, at('2026-11-11T02:00:00Z')).stats;
  assert.equal(stats.streak, 2); assert.equal(stats.activeDays, 2);
  stats = applyTransaction(stats, at('2026-12-01T02:00:00Z')).stats;
  assert.equal(stats.streak, 1); assert.equal(stats.month, '2026-12'); assert.equal(stats.monthXp, 15);
  assert.equal(stats.xp, 110 + 15 + 15);
});

test('backfill uses creation time and ignores imports', () => {
  const now = at('2026-11-11T05:00:00Z');
  const stats = backfill([
    { source: 'manual', scheduled: false, createdAt: at('2026-11-09T03:00:00Z') },
    { source: 'manual', scheduled: false, createdAt: at('2026-11-10T03:00:00Z') },
    { source: 'manual', scheduled: false, createdAt: at('2026-11-10T04:00:00Z') },
    { source: 'ivy', scheduled: false, createdAt: at('2026-11-10T04:00:00Z') },
  ], now);
  assert.equal(stats.xp, 15 + 20);
  assert.equal(stats.longestStreak, 2);
  assert.equal(stats.streak, 2);
  assert.equal(stats.month, '2026-11');
});

function fakeDb(seed = {}) {
  const store = new Map(Object.entries(seed));
  const ref = path => ({
    id: path.split('/').pop(), path,
    get: async () => snap(path),
    set: async v => store.set(path, v),
    delete: async () => store.delete(path),
    collection: name => coll(`${path}/${name}`),
  });
  const snap = path => ({ id: path.split('/').pop(), exists: store.has(path), data: () => store.get(path) });
  const children = base => [...store.keys()].filter(k => k.startsWith(`${base}/`) && k.split('/').length === base.split('/').length + 1);
  const query = (base, filter = () => true, order = null, limit = Infinity) => ({
    orderBy: (field, dir) => query(base, filter, { field, dir }, limit),
    limit: n => query(base, filter, order, n),
    where: (field, op, value) => query(base, k => filter(k) && (op === '>' ? store.get(k)[field] > value : store.get(k)[field] === value), order, limit),
    count: () => ({ get: async () => ({ data: () => ({ count: children(base).filter(filter).length }) }) }),
    get: async () => {
      let keys = children(base).filter(filter);
      if (order) keys.sort((a, b) => (store.get(b)[order.field] - store.get(a)[order.field]) * (order.dir === 'desc' ? 1 : -1));
      keys = keys.slice(0, limit);
      return { docs: keys.map(k => ({ id: k.split('/').pop(), ref: ref(k), data: () => store.get(k) })) };
    },
  });
  const coll = base => ({ doc: id => ref(`${base}/${id}`), ...query(base) });
  const db = {
    collection: name => coll(name),
    runTransaction: async fn => fn({ get: async r => (r.get ? r.get() : r), set: (r, v) => store.set(r.path, v) }),
  };
  return { store, db };
}

test('trigger awards XP once per transaction and builds the monthly leaderboard', async () => {
  let clock = at('2026-11-11T03:00:00Z');
  const { store, db } = fakeDb({
    'users/a': { name: 'muhammad zaenal arifin' }, 'users/b': { name: 'Johan Rifqi' },
  });
  const g = createGamification({ db, now: () => clock });
  const tx = { source: 'manual', scheduled: false, createdAt: clock };
  store.set('users/a/transactions/t1', tx);
  const first = await g.onTransactionCreated('a', 't1', tx);
  assert.equal(first.backfilled, true);
  assert.equal((await g.onTransactionCreated('a', 't1', tx)).skipped, true);
  store.set('users/a/transactions/t2', tx);
  assert.equal((await g.onTransactionCreated('a', 't2', tx)).gained, 5);
  store.set('users/b/transactions/x', tx);
  await g.onTransactionCreated('b', 'x', tx);
  const board = await g.leaderboard({ uid: 'b' }, {});
  assert.equal(board.month, '2026-11');
  assert.deepEqual(board.rows.map(r => [r.rank, r.name, r.xp]), [[1, 'Muhammad Z.', 20], [2, 'Johan R.', 15]]);
  assert.equal(board.rows[1].me, true);
  const hidden = await g.setVisibility({ uid: 'a' }, { visible: false });
  assert.equal(hidden.leaderboard, false);
  assert.equal((await g.leaderboard({ uid: 'b' }, {})).rows.length, 1);
  const status = await g.status({ uid: 'a' });
  assert.equal(status.xp, 20); assert.equal(status.level, 1); assert.equal(status.nextLevelXp, 100);
});
