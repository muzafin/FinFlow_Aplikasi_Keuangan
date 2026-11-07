const { HttpsError } = require('firebase-functions/v2/https');

// Batas tulis batch Firestore adalah 500; sisakan ruang agar aman.
const BATCH_LIMIT = 400;
const DELETED = ['reimbursements', 'financialEvents', 'transactions'];
const MANAGED = ['bills', 'debts', 'goals', 'notes', 'recurring'];

function validUid(uid) {
  return typeof uid === 'string' && uid.length > 0 && uid.length <= 128 && !uid.includes('/') && !['.', '..'].includes(uid);
}

function deleteTransactionsRequest(auth, data) {
  if (!validUid(auth?.uid)) throw new HttpsError('unauthenticated', 'Masuk kembali sebelum menghapus transaksi.');
  if (!data || typeof data !== 'object' || Array.isArray(data) || data.confirmation !== 'HAPUS TRANSAKSI' ||
      Object.keys(data).some(key => key !== 'confirmation'))
    throw new HttpsError('invalid-argument', 'Konfirmasi penghapusan transaksi tidak valid.');
  return { uid: auth.uid };
}

const revision = value => (Number.isInteger(value?.revision) && value.revision >= 0 ? value.revision : 0) + 1;

/**
 * Perubahan pada catatan terkelola agar tetap memenuhi invarian rules setelah semua jurnal dan event dihapus:
 * tidak ada lagi referensi ke event pembayaran, dan progres yang berasal dari jurnal dikembalikan.
 * Mengembalikan null bila dokumen sudah konsisten (operasi aman diulang).
 */
function managedPatch(collection, value) {
  if (!value || typeof value !== 'object') return null;
  switch (collection) {
    case 'bills':
      if ((value.paidAt ?? 0) === 0 && value.lastPaymentId == null) return null;
      return { paidAt: 0, lastPaymentId: null, revision: revision(value) };
    case 'debts':
      if ((value.paidMinor ?? 0) === 0 && value.lastEventId == null) return null;
      return { paidMinor: 0, lastEventId: null, revision: revision(value) };
    case 'goals': {
      const holding = value.holdingAccountId != null;
      const legacy = Number.isInteger(value.legacySavedMinor) ? value.legacySavedMinor : 0;
      // Progres target ber-dompet berasal dari transfer; progres manual lama (legacy) tetap.
      if (value.lastEventId == null && (!holding || value.savedMinor === legacy)) return null;
      return { ...(holding ? { savedMinor: legacy } : {}), lastEventId: null, revision: revision(value) };
    }
    case 'notes': {
      const items = Array.isArray(value.items) ? value.items : [];
      if (value.lastPaymentId == null && items.every(item => item?.paymentId == null)) return null;
      return {
        items: items.map(item => item?.paymentId == null ? item
          : { id: item.id, name: item.name, amountMinor: item.amountMinor, checked: false, paymentId: null }),
        lastPaymentId: null, revision: revision(value),
      };
    }
    case 'recurring':
      if (value.lastEventId == null) return null;
      return { lastEventId: null, revision: revision(value) };
    default:
      return null;
  }
}

/**
 * Menghapus semua transaksi milik `uid` beserta event keuangan dan klaim reimburse yang bergantung padanya,
 * lalu menyetel ulang status pembayaran tagihan/utang/target/bundle/jadwal. Dompet, kategori, anggaran,
 * keinginan, kartu, investasi, dan catatan teks tetap. Idempoten: pengulangan setelah gagal melanjutkan sisa pekerjaan.
 */
async function deleteAllTransactions(uid, { db }) {
  if (!validUid(uid)) throw new HttpsError('invalid-argument', 'Identitas akun tidak valid.');
  const [deletion, reset] = await Promise.all([
    db.collection('_accountDeletions').doc(uid).get(), db.collection('_dataResets').doc(uid).get(),
  ]);
  if (deletion.exists) throw new HttpsError('failed-precondition', 'Akun sedang dihapus.');
  if (reset.exists && reset.data()?.status !== 'complete')
    throw new HttpsError('failed-precondition', 'Penghapusan data sebelumnya belum selesai.');
  const user = db.collection('users').doc(uid);
  const operations = [];
  let updated = 0;
  for (const name of MANAGED) {
    const snapshot = await user.collection(name).get();
    for (const doc of snapshot.docs) {
      const patch = managedPatch(name, doc.data());
      if (!patch) continue;
      updated++;
      // Prasyarat waktu ubah menolak menimpa perubahan klien yang terjadi setelah dokumen dibaca.
      operations.push(batch => doc.updateTime ? batch.update(doc.ref, patch, { lastUpdateTime: doc.updateTime }) : batch.update(doc.ref, patch));
    }
  }
  const deleted = {};
  for (const name of DELETED) {
    const snapshot = await user.collection(name).select().get();
    deleted[name] = snapshot.size;
    for (const doc of snapshot.docs) operations.push(batch => batch.delete(doc.ref));
  }
  for (let start = 0; start < operations.length; start += BATCH_LIMIT) {
    const batch = db.batch();
    for (const operation of operations.slice(start, start + BATCH_LIMIT)) operation(batch);
    await batch.commit();
  }
  return { completed: true, transactions: deleted.transactions, events: deleted.financialEvents,
    reimbursements: deleted.reimbursements, updated };
}

module.exports = { deleteTransactionsRequest, deleteAllTransactions, managedPatch, BATCH_LIMIT };
