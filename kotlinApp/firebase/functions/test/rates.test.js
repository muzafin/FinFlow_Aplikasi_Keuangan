const test = require('node:test');
const assert = require('node:assert/strict');
const { createRates, TTL_MS } = require('../rates');

function fakeDb(initial = {}) {
  const records = new Map(Object.entries(initial));
  return { records, doc: path => ({ get: async () => ({ exists: records.has(path), data: () => records.get(path) }), set: async value => { records.set(path, value); } }) };
}
const fetchOk = (json, status = 200) => { const calls = []; const impl = async url => { calls.push(url); return { ok: status === 200, status, json: async () => json }; }; impl.calls = calls; return impl; };

test('fetches, filters and caches rates', async () => {
  const db = fakeDb(); const fetchImpl = fetchOk({ date: '2026-11-06', rates: { USD: 0.000063, EUR: 0.000058, bad: 1, JPY: -1 } });
  const rates = createRates({ db, fetchImpl, now: () => 1000 });
  const value = await rates.latest({ base: 'IDR' });
  assert.deepEqual(value.rates, { USD: 0.000063, EUR: 0.000058 });
  assert.equal(fetchImpl.calls[0], 'https://api.frankfurter.dev/v1/latest?base=IDR');
  assert.equal(db.records.get('_rates/IDR').date, '2026-11-06');
  await rates.latest({ base: 'IDR' });
  assert.equal(fetchImpl.calls.length, 1, 'cache dipakai dalam TTL');
});

test('refreshes after TTL and falls back to stale cache on network error', async () => {
  const db = fakeDb({ '_rates/USD': { base: 'USD', date: '2026-11-01', rates: { IDR: 16000 }, fetchedAt: 0 } });
  const failing = async () => { throw new Error('offline'); };
  const value = await createRates({ db, fetchImpl: failing, now: () => TTL_MS + 1 }).latest({ base: 'USD' });
  assert.equal(value.stale, true);
  assert.equal(value.rates.IDR, 16000);
});

test('rejects invalid base and unknown currency', async () => {
  const rates = createRates({ db: fakeDb(), fetchImpl: fetchOk({}, 404) });
  await assert.rejects(rates.latest({ base: 'idr' }), /tidak valid/);
  await assert.rejects(rates.latest({ base: 'XYZ' }), /belum tersedia/);
});
