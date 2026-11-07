package com.clonical.kasiva

import com.clonical.kasiva.i18n.*
import com.clonical.kasiva.core.i18n.text
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.clonical.kasiva.core.platform.PremiumPlan
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.models.Period
import kotlinx.coroutines.launch

/**
 * Google Play Billing lewat RevenueCat. Kunci SDK publik dibaca dari resource `revenuecat_api_key`;
 * aplikasi tidak pernah memutuskan Premium sendiri — server membaca RevenueCat lalu menulis hak akses.
 */
class PremiumBilling(private val activity: FragmentActivity) {
    private val apiKey: String? get() = activity.resources.getIdentifier("revenuecat_api_key", "string", activity.packageName)
        .takeIf { it != 0 }?.let { activity.getString(it) }?.takeIf { it.startsWith("goog_") }
    private var packages = emptyMap<String, com.revenuecat.purchases.Package>()

    fun configured() = apiKey != null

    private suspend fun ensure(userId: String) {
        val key = requireNotNull(apiKey) { MR.strings.core_premium_not_configured.text() }
        if (!Purchases.isConfigured) Purchases.configure(PurchasesConfiguration.Builder(activity.applicationContext, key).appUserID(userId).build())
        else if (Purchases.sharedInstance.appUserID != userId) Purchases.sharedInstance.awaitLogIn(userId)
    }

    fun plans(userId: String, result: (List<PremiumPlan>?, String?) -> Unit) {
        activity.lifecycleScope.launch {
            try {
                ensure(userId)
                val offering = requireNotNull(Purchases.sharedInstance.awaitOfferings().current) { MR.strings.core_premium_no_packages.text() }
                val mapped = offering.availablePackages.mapNotNull { item ->
                    val kind = when (item.packageType) {
                        PackageType.MONTHLY -> "monthly"; PackageType.SIX_MONTH -> "six_month"
                        PackageType.ANNUAL -> "annual"; PackageType.LIFETIME -> "lifetime"; else -> null
                    } ?: return@mapNotNull null
                    val trial = item.product.defaultOption?.freePhase?.billingPeriod?.let { period ->
                        when (period.unit) { Period.Unit.DAY -> period.value; Period.Unit.WEEK -> period.value * 7; Period.Unit.MONTH -> period.value * 30; else -> 0 }
                    } ?: 0
                    PremiumPlan(item.identifier, kind, item.product.price.formatted,
                        if (kind in listOf("six_month", "annual")) item.product.pricePerMonth()?.formatted else null, trial)
                }
                packages = offering.availablePackages.associateBy { it.identifier }
                result(mapped.sortedBy { listOf("monthly", "six_month", "annual", "lifetime").indexOf(it.kind) }, null)
            } catch (e: PurchasesException) { result(null, e.error.message) }
            catch (e: Exception) { result(null, e.message ?: MR.strings.core_premium_load_failed.text()) }
        }
    }

    fun purchase(planId: String, result: (Boolean, String?) -> Unit) {
        val item = packages[planId] ?: run { result(false, MR.strings.core_premium_reload.text()); return }
        activity.lifecycleScope.launch {
            try { Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, item).build()); result(true, null) }
            catch (e: PurchasesTransactionException) { result(false, if (e.userCancelled) null else e.error.message) }
            catch (e: PurchasesException) { result(false, e.error.message) }
            catch (e: Exception) { result(false, e.message ?: MR.strings.core_premium_purchase_failed.text()) }
        }
    }

    fun restore(userId: String, result: (Boolean, String?) -> Unit) {
        activity.lifecycleScope.launch {
            try { ensure(userId); Purchases.sharedInstance.awaitRestore(); result(true, null) }
            catch (e: PurchasesException) { result(false, e.error.message) }
            catch (e: Exception) { result(false, e.message ?: MR.strings.core_premium_restore_failed.text()) }
        }
    }

    fun manage(): String? = try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${activity.packageName}")))
        null
    } catch (e: Exception) { MR.strings.core_premium_play_unavailable.text() }
}
