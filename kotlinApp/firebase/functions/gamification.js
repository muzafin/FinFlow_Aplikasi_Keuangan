const { HttpsError } = require('firebase-functions/v2/https');

/**
 * XP, level, dan peringkat bulanan Kasiva — dihitung server agar tidak bisa dimanipulasi dari aplikasi.
 *
 * Aturan (disepakati pemilik 11 Nov 2026):
 * - +5 XP per transaksi yang dicatat pengguna (source manual/notification, bukan terjadwal), maksimal 20 transaksi
 *   ber-XP per hari (zona Asia/Jakarta, berdasarkan waktu server saat dokumen dibuat — bukan tanggal transaksi).
 * - +10 XP untuk catatan pertama setiap hari.
 * - XP tidak berkurang saat transaksi dihapus; hapus-lalu-catat-ulang hanya dihitung lagi bila masih di bawah batas
 *   harian. Impor CSV / pulihkan cadangan / dokumen ber-createdAt lama (>2 hari) tidak memberi XP.
 * - Level: total XP untuk mencapai level L = 50·L·(L−1) → Lvl 2 = 100, Lvl 3 = 300, Lvl 4 = 600, …
 * - Peringkat bulanan memakai XP bulan berjalan. Nama ditampilkan disamarkan ("Muhammad Z."); peserta otomatis
 *   (opt-out) dan bisa keluar lewat `setLeaderboardVisibility`.
 *
 * Data:
 *   users/{uid}/gamification/stats      — ringkasan (hanya Admin SDK; dibaca lewat callable)
 *   users/{uid}/xpLedger/{txId}          — penanda transaksi yang sudah dinilai (idempoten saat trigger diulang)
 *   leaderboards/{yyyy-MM}/entries/{uid} — baris peringkat bulanan
 */

const XP_PER_TRANSACTION = 5;
const DAILY_TRANSACTION_CAP = 20;
const FIRST_RECORD_BONUS = 10;
const MAX_AGE_MS = 2 * 86_400_000;
const ELIGIBLE_SOURCES = new Set(['manual', 'notification']);
const ZONE = 'Asia/Jakarta';

function dayKey(time) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(time));
}
const monthKey = time => dayKey(time).slice(0, 7);
function previousDay(key) {
  const date = new Date(`${key}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() - 1);
  return date.toISOString().slice(0, 10);
}

function levelFor(xp) {
  let level = 1;
  while (50 * (level + 1) * level <= xp) level += 1;
  return level;
}
const levelStart = level => 50 * level * (level - 1);

function maskName(name) {
  const words = String(name ?? '').trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return 'Pengguna Kasiva';
  const first = words[0].slice(0, 20);
  const pretty = first.charAt(0).toUpperCase() + first.slice(1).toLowerCase();
  return words.length > 1 ? `${pretty} ${words[1].charAt(0).toUpperCase()}.` : pretty;
}

function eligible(tx, now) {
  if (!tx || tx.scheduled === true || !ELIGIBLE_SOURCES.has(tx.source)) return false;
  // Dokumen lama (pulihkan cadangan, sinkron ulang) tidak memberi XP.
  if (typeof tx.createdAt === 'number' && now - tx.createdAt > MAX_AGE_MS) return false;
  return true;
}

const emptyStats = () => ({ xp: 0, month: '', monthXp: 0, monthTransactions: 0, monthActiveDays: 0, day: '', dayCount: 0,
  activeDays: 0, streak: 0, longestStreak: 0, transactions: 0, hidden: false, updatedAt: 0 });

/** Terapkan satu transaksi ber-XP ke statistik. Mengembalikan XP yang ditambahkan (0 bila kena batas harian). */
function applyTransaction(stats, now) {
  const next = { ...emptyStats(), ...stats };
  const today = dayKey(now);
  const month = monthKey(now);
  if (next.month !== month) Object.assign(next, { month, monthXp: 0, monthTransactions: 0, monthActiveDays: 0 });
  let gained = 0;
  if (next.day !== today) {
    next.streak = next.day === previousDay(today) ? next.streak + 1 : 1;
    next.longestStreak = Math.max(next.longestStreak, next.streak);
    next.day = today;
    next.dayCount = 0;
    next.activeDays += 1;
    next.monthActiveDays += 1;
    gained += FIRST_RECORD_BONUS;
  }
  if (next.dayCount < DAILY_TRANSACTION_CAP) {
    next.dayCount += 1;
    next.transactions += 1;
    next.monthTransactions += 1;
    gained += XP_PER_TRANSACTION;
  }
  next.xp += gained;
  next.monthXp += gained;
  next.updatedAt = now;
  return { stats: next, gained };
}

/** Bangun statistik awal dari transaksi lama (akun sebelum fitur ini ada), dengan aturan yang sama. */
function backfill(transactions, now) {
  const eligibleTx = transactions
    .filter(tx => tx && tx.scheduled !== true && ELIGIBLE_SOURCES.has(tx.source) && typeof tx.createdAt === 'number' && tx.createdAt <= now)
    .sort((a, b) => a.createdAt - b.createdAt);
  let stats = emptyStats();
  for (const tx of eligibleTx) stats = applyTransaction(stats, tx.createdAt).stats;
  // Bulan & streak dinilai relatif terhadap sekarang.
  if (stats.month !== monthKey(now)) Object.assign(stats, { month: monthKey(now), monthXp: 0, monthTransactions: 0, monthActiveDays: 0 });
  if (stats.day !== dayKey(now) && stats.day !== previousDay(dayKey(now))) stats.streak = 0;
  stats.updatedAt = now;
  return stats;
}

function publicStatus(stats) {
  const level = levelFor(stats.xp);
  const today = dayKey(Date.now());
  const streakAlive = stats.day === today || stats.day === previousDay(today);
  const currentMonth = stats.month === monthKey(Date.now());
  return {
    xp: stats.xp, level, levelStartXp: levelStart(level), nextLevelXp: levelStart(level + 1),
    monthXp: currentMonth ? stats.monthXp : 0, monthTransactions: currentMonth ? stats.monthTransactions : 0,
    monthActiveDays: currentMonth ? stats.monthActiveDays : 0,
    transactions: stats.transactions, activeDays: stats.activeDays,
    streak: streakAlive ? stats.streak : 0, longestStreak: stats.longestStreak,
    todayCount: stats.day === today ? stats.dayCount : 0, dailyCap: DAILY_TRANSACTION_CAP,
    leaderboard: !stats.hidden, month: monthKey(Date.now()),
  };
}

function createGamification({ db, now = () => Date.now() }) {
  const statsRef = uid => db.collection('users').doc(uid).collection('gamification').doc('stats');
  const entryRef = (month, uid) => db.collection('leaderboards').doc(month).collection('entries').doc(uid);
  const userRef = uid => db.collection('users').doc(uid);

  async function entryFor(uid, stats, tx) {
    const profile = tx ? await tx.get(userRef(uid)) : await userRef(uid).get();
    return { name: maskName(profile.data()?.name), xp: stats.monthXp, transactions: stats.monthTransactions,
      activeDays: stats.monthActiveDays, level: levelFor(stats.xp), updatedAt: stats.updatedAt };
  }

  /** Trigger onCreate transaksi. */
  async function onTransactionCreated(uid, txId, tx) {
    const time = now();
    if (!eligible(tx, time)) return { gained: 0, skipped: true };
    if ((await db.collection('_accountDeletions').doc(uid).get()).exists) return { gained: 0, skipped: true };
    return db.runTransaction(async t => {
      const ledger = userRef(uid).collection('xpLedger').doc(txId);
      const [ledgerSnap, statsSnap] = [await t.get(ledger), await t.get(statsRef(uid))];
      if (ledgerSnap.exists) return { gained: 0, skipped: true };
      // Akun lama tanpa statistik: hitung ulang dari transaksi yang ada (sudah termasuk transaksi ini).
      if (!statsSnap.exists) {
        const all = await t.get(userRef(uid).collection('transactions'));
        const stats = backfill(all.docs.map(doc => doc.data()), time);
        const entry = await entryFor(uid, stats, t);
        t.set(statsRef(uid), stats);
        t.set(ledger, { at: time, xp: 0, backfill: true });
        if (!stats.hidden && stats.monthXp > 0) t.set(entryRef(stats.month, uid), entry);
        return { gained: stats.xp, backfilled: true };
      }
      const { stats, gained } = applyTransaction(statsSnap.data(), time);
      const entry = await entryFor(uid, stats, t);
      t.set(statsRef(uid), stats);
      t.set(ledger, { at: time, xp: gained });
      if (!stats.hidden && stats.monthXp > 0) t.set(entryRef(stats.month, uid), entry);
      return { gained };
    });
  }

  async function ensureStats(uid) {
    const snap = await statsRef(uid).get();
    if (snap.exists) return snap.data();
    const all = await userRef(uid).collection('transactions').get();
    const stats = backfill(all.docs.map(doc => doc.data()), now());
    await statsRef(uid).set(stats);
    if (!stats.hidden && stats.monthXp > 0) await entryRef(stats.month, uid).set(await entryFor(uid, stats));
    return stats;
  }

  async function status(auth) {
    if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk ke akun Kasiva untuk melihat XP.');
    return publicStatus(await ensureStats(auth.uid));
  }

  async function leaderboard(auth, data) {
    if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk ke akun Kasiva untuk melihat peringkat.');
    const month = typeof data?.month === 'string' && /^\d{4}-\d{2}$/.test(data.month) ? data.month : monthKey(now());
    const stats = await ensureStats(auth.uid);
    const entries = db.collection('leaderboards').doc(month).collection('entries');
    const top = await entries.orderBy('xp', 'desc').limit(50).get();
    const rows = top.docs.map((doc, index) => ({ rank: index + 1, me: doc.id === auth.uid, ...pick(doc.data()) }));
    let me = rows.find(row => row.me) ?? null;
    if (!me && !stats.hidden && stats.month === month && stats.monthXp > 0) {
      const ahead = await entries.where('xp', '>', stats.monthXp).count().get();
      me = { rank: ahead.data().count + 1, me: true, ...pick(await entryFor(auth.uid, stats)) };
    }
    const total = (await entries.count().get()).data().count;
    return { month, rows, me, participants: total, hidden: !!stats.hidden };
  }

  async function setVisibility(auth, data) {
    if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk ke akun Kasiva terlebih dahulu.');
    if (typeof data?.visible !== 'boolean') throw new HttpsError('invalid-argument', 'Pilihan peringkat tidak valid.');
    const stats = await ensureStats(auth.uid);
    const next = { ...stats, hidden: !data.visible };
    await statsRef(auth.uid).set(next);
    const month = monthKey(now());
    if (data.visible) { if (next.month === month && next.monthXp > 0) await entryRef(month, auth.uid).set(await entryFor(auth.uid, next)); }
    else await entryRef(month, auth.uid).delete();
    return publicStatus(next);
  }

  return { onTransactionCreated, status, leaderboard, setVisibility };
}

function pick(entry) {
  return { name: entry.name, xp: entry.xp, transactions: entry.transactions, activeDays: entry.activeDays, level: entry.level };
}

module.exports = { createGamification, applyTransaction, backfill, levelFor, levelStart, maskName, eligible, dayKey, emptyStats };
