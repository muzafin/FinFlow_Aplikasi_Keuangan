const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');
const { onCall, onRequest } = require('firebase-functions/v2/https');
const { defineSecret, defineString } = require('firebase-functions/params');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { setGlobalOptions } = require('firebase-functions/v2');
const { deletionUid, deleteAccountData } = require('./account-deletion');
const { resetRequest, resetFinancialData } = require('./data-reset');
const { deleteTransactionsRequest, deleteAllTransactions } = require('./transactions-reset');
const { createAi, DEFAULT_MODELS } = require('./ai');
const { createPremium, revenueCatFetcher } = require('./premium');

const aiApiKey = defineSecret('DEEPSEEK_API_KEY');
const revenueCatSecret = defineSecret('REVENUECAT_SECRET_KEY');
const revenueCatWebhookAuth = defineSecret('REVENUECAT_WEBHOOK_AUTH');
const parseModel = defineString('AI_MODEL_PARSE', { default: DEFAULT_MODELS.parse });
const assistantModel = defineString('AI_MODEL_ASSISTANT', { default: DEFAULT_MODELS.assistant });

initializeApp();
setGlobalOptions({ region: 'asia-southeast2', minInstances: 0, maxInstances: 2, memory: '256MiB' });

exports.requestAccountDeletion = onCall(async request => {
  const uid = deletionUid(request.auth, request.data);
  const db = getFirestore();
  await db.runTransaction(async tx => {
    const ref = db.collection('_accountDeletions').doc(uid);
    if (!(await tx.get(ref)).exists) tx.create(ref, { status: 'pending', requestedAt: Date.now() });
  });
  return { accepted: true };
});

exports.resetFinancialData = onCall({ timeoutSeconds:540, concurrency:1, maxInstances:1 }, async request => {
  const {uid,requestId}=resetRequest(request.auth,request.data);
  return resetFinancialData(uid,requestId,{
    db:getFirestore(),auth:getAuth(),bucket:getStorage().bucket(`${process.env.GCLOUD_PROJECT}.firebasestorage.app`),
  });
});

// "Hapus Transaksi Saja": jurnal, event, dan klaim reimburse dihapus; status pembayaran catatan terkelola disetel ulang.
exports.deleteTransactions = onCall({ timeoutSeconds: 300, maxInstances: 2 }, async request => {
  const { uid } = deleteTransactionsRequest(request.auth, request.data);
  return deleteAllTransactions(uid, { db: getFirestore() });
});

exports.deleteAccountData = onDocumentCreated({ document: '_accountDeletions/{uid}', retry: true, timeoutSeconds: 540 }, async event => {
  await deleteAccountData(event.params.uid, {
    db: getFirestore(), auth: getAuth(), bucket: getStorage().bucket(`${process.env.GCLOUD_PROJECT}.firebasestorage.app`),
  });
});

// AI: semua panggilan model lewat server agar kunci API, kuota, dan persetujuan tetap terkendali.
let ai;
function aiService() {
  if (!ai) {
    const { createDeepSeekClient } = require('./deepseek');
    const client = createDeepSeekClient({ apiKey: aiApiKey.value(), timeoutMs: 90_000 });
    ai = createAi({ db: getFirestore(), client, models: { parse: parseModel.value(), assistant: assistantModel.value() } });
  }
  return ai;
}
const aiOptions = { secrets: [aiApiKey], timeoutSeconds: 120, maxInstances: 10 };
exports.aiConsent = onCall(async request => createAi({ db: getFirestore() }).setConsent(request.auth, request.data));
exports.aiStatus = onCall(async request => createAi({ db: getFirestore() }).status(request.auth));
exports.aiParseText = onCall(aiOptions, async request => aiService().parseText(request.auth, request.data));
exports.aiScanReceipt = onCall({ ...aiOptions, memory: '512MiB' }, async request => aiService().scanReceipt(request.auth, request.data));
exports.aiAssistant = onCall(aiOptions, async request => aiService().assistant(request.auth, request.data));

// Premium: RevenueCat memverifikasi pembelian toko; server menyalin hak akses ke users/{uid}/entitlements/premium.
const premium = () => createPremium({ db: getFirestore(), fetchSubscriber: revenueCatFetcher(() => revenueCatSecret.value()), webhookSecret: () => revenueCatWebhookAuth.value() });
exports.refreshPremium = onCall({ secrets: [revenueCatSecret, revenueCatWebhookAuth] }, async request => premium().refresh(request.auth));
exports.revenueCatWebhook = onRequest({ secrets: [revenueCatSecret, revenueCatWebhookAuth], maxInstances: 5 }, (request, response) => premium().webhook(request, response));

// Kurs referensi (ECB via Frankfurter) untuk halaman Kurs; data publik, di-cache 12 jam di `_rates/{base}`.
const { createRates } = require('./rates');
exports.exchangeRates = onCall({ maxInstances: 3 }, async request => createRates({ db: getFirestore() }).latest(request.data));
