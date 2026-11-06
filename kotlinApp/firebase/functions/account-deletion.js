const { HttpsError } = require('firebase-functions/v2/https');

function deletionUid(auth, data, now = Math.floor(Date.now() / 1000)) {
  if (!auth?.uid) throw new HttpsError('unauthenticated', 'Masuk kembali sebelum menghapus akun.');
  if (!Number.isInteger(auth.token?.auth_time) || auth.token.auth_time > now + 60 || now - auth.token.auth_time > 300)
    throw new HttpsError('failed-precondition', 'Login ulang diperlukan untuk menghapus akun.');
  if (data?.confirmation !== 'HAPUS' || Object.keys(data).some(key => key !== 'confirmation'))
    throw new HttpsError('invalid-argument', 'Ketik HAPUS untuk mengonfirmasi penghapusan akun sendiri.');
  return auth.uid;
}

async function deleteAccountData(uid, { db, auth, bucket }) {
  const job = db.collection('_accountDeletions').doc(uid);
  if ((await job.get()).data()?.status === 'complete') return;
  // Tombstone dipertahankan agar token lama tidak dapat membuat ulang data sesudah penghapusan.
  try { await auth.revokeRefreshTokens(uid); }
  catch (error) { if (error.code !== 'auth/user-not-found') throw error; }
  await bucket.deleteFiles({ prefix: `users/${uid}/` });
  await db.recursiveDelete(db.collection('users').doc(uid));
  await db.recursiveDelete(db.collection('_dataResets').doc(uid));
  try { await auth.deleteUser(uid); }
  catch (error) { if (error.code !== 'auth/user-not-found') throw error; }
  await job.update({ status: 'complete', completedAt: Date.now() });
}

module.exports = { deletionUid, deleteAccountData };
