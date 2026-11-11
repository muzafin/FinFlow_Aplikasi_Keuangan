const crypto = require('node:crypto');
const { HttpsError } = require('firebase-functions/v2/https');

/**
 * Push FCM Kasiva.
 * - Token per instalasi disimpan di users/{uid}/pushTokens/{sha256(token)} oleh callable (Admin SDK);
 *   klien tidak bisa menulis koleksi ini langsung (rules tidak membuka path tersebut).
 * - Pengingat tagihan terjadwal: tagihan aktif yang jatuh tempo hari ini / besok (zona waktu perangkat).
 * - Isi notifikasi sengaja tidak memuat nominal atau nama tagihan (bisa tampil di layar kunci).
 */

const TOKEN_PATTERN = /^[A-Za-z0-9_:\-.]{20,4096}$/;
const LANGUAGES = ['id', 'en', 'ms', 'es', 'fr', 'de', 'ja', 'zh', 'hi'];
const INVALID_TOKEN_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
  'messaging/invalid-argument',
]);

function tokenId(token) {
  return crypto.createHash('sha256').update(token).digest('hex').slice(0, 40);
}

function validTimeZone(zone) {
  if (typeof zone !== 'string' || zone.length > 64) return false;
  try { new Intl.DateTimeFormat('en-US', { timeZone: zone }); return true; } catch { return false; }
}

function parseRegistration(auth, data) {
  if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk ke akun Kasiva untuk mengaktifkan notifikasi.');
  const allowed = ['token', 'platform', 'language', 'timeZone', 'appVersion', 'billReminders'];
  if (!data || typeof data !== 'object' || Object.keys(data).some(key => !allowed.includes(key)))
    throw new HttpsError('invalid-argument', 'Data pendaftaran notifikasi tidak valid.');
  if (typeof data.token !== 'string' || !TOKEN_PATTERN.test(data.token))
    throw new HttpsError('invalid-argument', 'Token notifikasi tidak valid.');
  const platform = ['android', 'ios'].includes(data.platform) ? data.platform : 'other';
  const language = LANGUAGES.includes(data.language) ? data.language : 'en';
  const timeZone = validTimeZone(data.timeZone) ? data.timeZone : 'Asia/Jakarta';
  const appVersion = typeof data.appVersion === 'string' ? data.appVersion.slice(0, 32) : '';
  const billReminders = data.billReminders !== false;
  return { uid: auth.uid, token: data.token, platform, language, timeZone, appVersion, billReminders };
}

/** Tanggal lokal YYYY-MM-DD pada zona waktu tertentu. */
function localDate(now, timeZone, offsetDays = 0) {
  const shifted = new Date(now.getTime() + offsetDays * 86_400_000);
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(shifted);
  const get = type => parts.find(part => part.type === type).value;
  return `${get('year')}-${get('month')}-${get('day')}`;
}

const COPY = {
  id: {
    todayTitle: 'Tagihan jatuh tempo hari ini',
    tomorrowTitle: 'Tagihan jatuh tempo besok',
    body: count => `${count} tagihan perlu dibayar. Buka Kasiva untuk menandainya lunas.`,
  },
  en: {
    todayTitle: 'Bills due today',
    tomorrowTitle: 'Bills due tomorrow',
    body: count => `${count} bill${count === 1 ? '' : 's'} to pay. Open Kasiva to mark ${count === 1 ? 'it' : 'them'} as paid.`,
  },
  ms: {
    todayTitle: 'Bil perlu dibayar hari ini',
    tomorrowTitle: 'Bil perlu dibayar esok',
    body: count => `${count} bil perlu dibayar. Buka Kasiva untuk menandakannya selesai.`,
  },
};

function billMessage(language, dueToday, dueTomorrow) {
  const copy = COPY[language] ?? COPY.en;
  if (dueToday > 0) return { title: copy.todayTitle, body: copy.body(dueToday), kind: 'bill_today' };
  if (dueTomorrow > 0) return { title: copy.tomorrowTitle, body: copy.body(dueTomorrow), kind: 'bill_tomorrow' };
  return null;
}

function androidPayload(token, message) {
  return {
    token,
    notification: { title: message.title, body: message.body },
    data: { kind: message.kind, route: 'bills' },
    android: { priority: 'high', notification: { channelId: 'bills', tag: message.kind } },
    apns: { payload: { aps: { sound: 'default' } } },
  };
}

function createPush({ db, messaging, now = () => new Date() }) {
  const tokens = uid => db.collection('users').doc(uid).collection('pushTokens');

  async function register(auth, data) {
    const entry = parseRegistration(auth, data);
    const id = tokenId(entry.token);
    // Satu token hanya milik satu akun: bila pindah akun di perangkat yang sama, hapus milik akun lama.
    const owners = await db.collectionGroup('pushTokens').where('tokenId', '==', id).get();
    await Promise.all(owners.docs.filter(doc => doc.ref.parent.parent?.id !== entry.uid).map(doc => doc.ref.delete()));
    const time = now().getTime();
    await tokens(entry.uid).doc(id).set({
      tokenId: id, token: entry.token, platform: entry.platform, language: entry.language, timeZone: entry.timeZone,
      appVersion: entry.appVersion, billReminders: entry.billReminders, updatedAt: time,
    }, { merge: true });
    return { ok: true };
  }

  async function unregister(auth, data) {
    if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk ke akun Kasiva terlebih dahulu.');
    if (typeof data?.token !== 'string' || !TOKEN_PATTERN.test(data.token)) throw new HttpsError('invalid-argument', 'Token notifikasi tidak valid.');
    await tokens(auth.uid).doc(tokenId(data.token)).delete();
    return { ok: true };
  }

  /** Kirim ke semua token aktif milik pengguna; token yang ditolak FCM dihapus. */
  async function sendToUser(uid, build) {
    const snapshot = await tokens(uid).get();
    let sent = 0;
    for (const doc of snapshot.docs) {
      const entry = doc.data();
      const message = build(entry);
      if (!message) continue;
      try { await messaging.send(androidPayload(entry.token, message)); sent += 1; }
      catch (error) { if (INVALID_TOKEN_CODES.has(error?.code)) await doc.ref.delete(); else throw error; }
    }
    return sent;
  }

  /** Dijalankan terjadwal setiap pagi: satu notifikasi per perangkat bila ada tagihan aktif jatuh tempo hari ini/besok. */
  async function billReminders() {
    const all = await db.collectionGroup('pushTokens').get();
    const byUser = new Map();
    for (const doc of all.docs) {
      const uid = doc.ref.parent.parent?.id;
      if (!uid) continue;
      if (!byUser.has(uid)) byUser.set(uid, []);
      byUser.get(uid).push(doc);
    }
    let sent = 0;
    for (const [uid, docs] of byUser) {
      if (!docs.some(doc => doc.data().billReminders !== false)) continue;
      if ((await db.collection('_accountDeletions').doc(uid).get()).exists) continue;
      const zones = [...new Set(docs.map(doc => doc.data().timeZone || 'Asia/Jakarta'))];
      const dates = new Set();
      for (const zone of zones) { dates.add(localDate(now(), zone)); dates.add(localDate(now(), zone, 1)); }
      const bills = await db.collection('users').doc(uid).collection('bills').where('dueDate', 'in', [...dates]).get();
      const active = bills.docs.map(doc => doc.data()).filter(bill => bill.archived !== true);
      if (active.length === 0) continue;
      try {
        sent += await sendToUser(uid, entry => {
          if (entry.billReminders === false) return null;
          const zone = entry.timeZone || 'Asia/Jakarta';
          const today = localDate(now(), zone);
          const tomorrow = localDate(now(), zone, 1);
          return billMessage(entry.language, active.filter(b => b.dueDate === today).length, active.filter(b => b.dueDate === tomorrow).length);
        });
      } catch (error) {
        console.error('bill reminder failed', { uid, code: error?.code });
      }
    }
    return { users: byUser.size, sent };
  }

  return { register, unregister, sendToUser, billReminders };
}

module.exports = { createPush, parseRegistration, localDate, billMessage, tokenId, androidPayload };
