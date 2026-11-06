const { HttpsError } = require('firebase-functions/v2/https');

const requestIdPattern = /^[A-Za-z0-9-]{8,100}$/;
const categories = [
  ['food', 'Makan & minum', 'expense'], ['transport', 'Transportasi', 'expense'],
  ['shopping', 'Belanja', 'expense'], ['health', 'Kesehatan', 'expense'],
  ['bills', 'Tagihan', 'expense'], ['education', 'Pendidikan', 'expense'],
  ['entertainment', 'Hiburan', 'expense'], ['other_expense', 'Lainnya', 'expense'],
  ['salary', 'Gaji', 'income'], ['business', 'Usaha', 'income'], ['other_income', 'Lainnya', 'income'],
];

function validUid(uid) {
  return typeof uid === 'string' && uid.length > 0 && uid.length <= 128 && !uid.includes('/') && !['.', '..'].includes(uid);
}

function resetRequest(auth, data, now = Math.floor(Date.now() / 1000)) {
  if (!validUid(auth?.uid)) throw new HttpsError('unauthenticated', 'Masuk kembali sebelum menghapus data.');
  if (!Number.isInteger(auth.token?.auth_time) || auth.token.auth_time > now + 60 || now - auth.token.auth_time > 300)
    throw new HttpsError('failed-precondition', 'Login ulang diperlukan untuk menghapus data.');
  if (!data || typeof data !== 'object' || Array.isArray(data) || data.confirmation !== 'HAPUS DATA' ||
      typeof data.requestId !== 'string' || !requestIdPattern.test(data.requestId) ||
      Object.keys(data).some(key => !['confirmation', 'requestId'].includes(key)))
    throw new HttpsError('invalid-argument', 'Ketik HAPUS DATA dan gunakan identitas permintaan yang valid.');
  return { uid: auth.uid, requestId: data.requestId };
}

function minimalProfile(uid, value) {
  if (!value || value.id !== uid || typeof value.name !== 'string' || !value.name.trim() || value.name.length > 100 ||
      typeof value.email !== 'string' || value.email.length > 320 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.email) ||
      typeof value.baseCurrency !== 'string' || !/^[A-Z]{3}$/.test(value.baseCurrency) || value.schemaVersion !== 2)
    throw new HttpsError('failed-precondition', 'Profil akun tidak dapat dibaca dengan aman. Penghapusan data dibatalkan.');
  return { id: uid, name: value.name, email: value.email, baseCurrency: value.baseCurrency, schemaVersion: 2 };
}

async function resetFinancialData(uid, requestId, { db, auth, bucket }) {
  if (!validUid(uid) || typeof requestId !== 'string' || !requestIdPattern.test(requestId))
    throw new HttpsError('invalid-argument', 'Identitas penghapusan data tidak valid.');
  const job = db.collection('_dataResets').doc(uid);
  const receipt = job.collection('requests').doc(requestId);
  const user = db.collection('users').doc(uid);
  const accountDeletion = db.collection('_accountDeletions').doc(uid);
  const prepared = await db.runTransaction(async tx => {
    // Bukti tiap permintaan mencegah retry lama menghapus data baru setelah reset berikutnya.
    const completed = await tx.get(receipt);
    if (completed.exists) {
      if (completed.data()?.status === 'complete') return null;
      throw new HttpsError('failed-precondition', 'Status permintaan penghapusan data tidak valid.');
    }
    const snapshot = await tx.get(job);
    const previous = snapshot.exists ? snapshot.data() : null;
    if (previous?.status === 'complete' && previous.requestId === requestId) return null;
    if (previous && !['pending', 'complete'].includes(previous.status))
      throw new HttpsError('failed-precondition', 'Status penghapusan data belum dapat dibaca.');
    if (previous?.status === 'pending' && previous.requestId !== requestId)
      throw new HttpsError('failed-precondition', 'Penghapusan sebelumnya belum selesai. Lanjutkan permintaan yang sama.');
    if ((await tx.get(accountDeletion)).exists)
      throw new HttpsError('failed-precondition', 'Akun sedang dihapus dan tidak dapat direset.');
    if (previous?.status === 'pending') {
      const profile = minimalProfile(uid, previous.profile);
      if (!Number.isInteger(previous.requestedAt) || previous.requestedAt < 0)
        throw new HttpsError('failed-precondition', 'Status penghapusan data tidak valid.');
      return { profile, requestedAt: previous.requestedAt };
    }
    const original = await tx.get(user);
    if (!original.exists) throw new HttpsError('failed-precondition', 'Profil akun belum tersedia. Data tidak dihapus.');
    const profile = minimalProfile(uid, original.data());
    const requestedAt = Date.now();
    tx.set(job, { requestId, status: 'pending', profile, requestedAt });
    return { profile, requestedAt };
  });
  if (!prepared) return { completed: true, requestId };

  const identity = await auth.getUser(uid);
  if (identity.uid !== uid) throw new HttpsError('failed-precondition', 'Identitas akun tidak cocok.');
  const profile = minimalProfile(uid, { ...prepared.profile, email: identity.email ?? prepared.profile.email });
  await bucket.deleteFiles({ prefix: `users/${uid}/` });
  await db.recursiveDelete(user);
  await db.runTransaction(async tx => {
    const snapshot = await tx.get(job);
    const pending = snapshot.exists ? snapshot.data() : null;
    if (pending?.status !== 'pending' || pending.requestId !== requestId)
      throw new HttpsError('aborted', 'Status penghapusan berubah. Lanjutkan permintaan yang sama.');
    if ((await tx.get(accountDeletion)).exists)
      throw new HttpsError('failed-precondition', 'Akun sedang dihapus dan tidak dapat dipulihkan oleh reset.');
    const completedAt = Date.now();
    tx.set(user, profile);
    tx.set(user.collection('accounts').doc('acc_cash'), {
      id: 'acc_cash', name: 'Kas', type: 'cash', currency: profile.baseCurrency,
      openingBalanceMinor: 0, archived: false, createdAt: completedAt,
    });
    for (const [id, name, type] of categories) tx.set(user.collection('categories').doc(id), { id, name, type });
    tx.set(job, { requestId, status: 'complete', requestedAt: prepared.requestedAt, completedAt });
    tx.set(receipt, { status: 'complete', completedAt });
  });
  return { completed: true, requestId };
}

module.exports = { resetRequest, resetFinancialData };
