const { HttpsError } = require('firebase-functions/v2/https');
const { timingSafeEqual } = require('node:crypto');

// Hak Premium hanya ditulis server dari data RevenueCat (sumber kebenaran pembelian Google Play / App Store).
const ENTITLEMENT_ID = 'premium';

function validUid(uid) {
  return typeof uid === 'string' && uid.length > 0 && uid.length <= 128 && !uid.includes('/') && !uid.startsWith('$RCAnonymousID');
}

function sameSecret(given, expected) {
  if (typeof given !== 'string' || typeof expected !== 'string' || !expected) return false;
  const a = Buffer.from(given), b = Buffer.from(expected);
  return a.length === b.length && timingSafeEqual(a, b);
}

function entitlementFrom(subscriber, now) {
  const entitlement = subscriber?.entitlements?.[ENTITLEMENT_ID];
  if (!entitlement) return { active: false, expiresAt: null, productId: null, store: null };
  const expiresAt = entitlement.expires_date ? Date.parse(entitlement.expires_date) : null;
  const productId = typeof entitlement.product_identifier === 'string' ? entitlement.product_identifier.slice(0, 200) : null;
  const store = productId && subscriber.subscriptions?.[productId]?.store || (productId && subscriber.non_subscriptions?.[productId] ? 'lifetime' : null);
  return { active: expiresAt == null || expiresAt > now, expiresAt: Number.isFinite(expiresAt) ? expiresAt : null, productId, store: typeof store === 'string' ? store.slice(0, 40) : null };
}

function createPremium({ db, fetchSubscriber, webhookSecret, now = () => Date.now() }) {
  async function sync(uid) {
    const subscriber = await fetchSubscriber(uid);
    const value = entitlementFrom(subscriber, now());
    const ref = db.doc(`users/${uid}/entitlements/premium`);
    await db.runTransaction(async tx => {
      const current = await tx.get(ref);
      // manualUntil (hadiah leaderboard/kode promo dari admin) tidak boleh terhapus oleh sinkron toko.
      tx.set(ref, { ...value, manualUntil: current.exists ? current.data().manualUntil ?? null : null, updatedAt: now() });
    });
    return value;
  }

  async function refresh(auth) {
    const uid = auth?.uid;
    if (!validUid(uid)) throw new HttpsError('unauthenticated', 'Masuk ke akun FinFlow terlebih dahulu.');
    try { return await sync(uid); }
    catch (error) { throw new HttpsError('unavailable', 'Status Premium belum dapat diperiksa. Coba lagi nanti.'); }
  }

  async function webhook(request, response) {
    if (request.method !== 'POST') { response.status(405).send('Method not allowed'); return; }
    if (!sameSecret(request.get('authorization'), webhookSecret())) { response.status(401).send('Unauthorized'); return; }
    const event = request.body?.event;
    const ids = [event?.app_user_id, event?.original_app_user_id, ...(Array.isArray(event?.transferred_to) ? event.transferred_to : []),
      ...(Array.isArray(event?.transferred_from) ? event.transferred_from : [])].filter(validUid);
    for (const uid of new Set(ids)) {
      if (!(await db.doc(`users/${uid}`).get()).exists) continue;
      await sync(uid);
    }
    response.status(200).send('ok');
  }

  return { refresh, webhook, sync };
}

function revenueCatFetcher(secretKey) {
  return async uid => {
    const response = await fetch(`https://api.revenuecat.com/v1/subscribers/${encodeURIComponent(uid)}`, {
      headers: { Authorization: `Bearer ${secretKey()}`, 'Content-Type': 'application/json' },
    });
    if (!response.ok) throw new Error(`RevenueCat ${response.status}`);
    return (await response.json()).subscriber;
  };
}

module.exports = { createPremium, entitlementFrom, revenueCatFetcher, ENTITLEMENT_ID };
