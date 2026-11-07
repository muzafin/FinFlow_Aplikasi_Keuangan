const { HttpsError } = require('firebase-functions/v2/https');

// Kurs referensi harian ECB lewat Frankfurter (gratis, tanpa kunci). Disimpan di Firestore `_rates/{base}`
// selama 12 jam agar hemat panggilan; hanya Admin SDK yang menulis (rules menolak klien).
const SOURCE = 'https://api.frankfurter.dev/v1/latest';
const TTL_MS = 12 * 60 * 60 * 1000;

function validBase(value) {
  if (typeof value !== 'string' || !/^[A-Z]{3}$/.test(value)) throw new HttpsError('invalid-argument', 'Kode mata uang tidak valid.');
  return value;
}

function createRates({ db, fetchImpl = fetch, now = () => Date.now() }) {
  async function latest(data) {
    const base = validBase(data?.base);
    const ref = db.doc(`_rates/${base}`);
    const cached = await ref.get();
    if (cached.exists && now() - cached.data().fetchedAt < TTL_MS) return cached.data();
    let json;
    try {
      const response = await fetchImpl(`${SOURCE}?base=${base}`);
      if (response.status === 404 || response.status === 422) throw new HttpsError('not-found', 'Kurs untuk mata uang ini belum tersedia.');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      json = await response.json();
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      if (cached.exists) return { ...cached.data(), stale: true };
      throw new HttpsError('unavailable', 'Kurs belum dapat dimuat. Coba lagi nanti.');
    }
    const rates = Object.fromEntries(Object.entries(json.rates || {})
      .filter(([code, rate]) => /^[A-Z]{3}$/.test(code) && Number.isFinite(rate) && rate > 0));
    const value = { base, date: String(json.date || '').slice(0, 10), rates, fetchedAt: now(), source: 'ECB via Frankfurter' };
    await ref.set(value);
    return value;
  }
  return { latest };
}

module.exports = { createRates, TTL_MS };
