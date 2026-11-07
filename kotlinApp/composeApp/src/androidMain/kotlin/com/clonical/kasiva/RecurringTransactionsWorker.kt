package com.clonical.kasiva

import android.content.Context
import androidx.work.*
import com.clonical.kasiva.core.data.FinanceRepository
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class RecurringTransactionsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs=applicationContext.getSharedPreferences("finflow",Context.MODE_PRIVATE)
        val local=prefs.getString("data.mode",null)=="local"
        val repository = if(local)FinanceRepository(local=AndroidLocalFinancePersistence.get(applicationContext)) else FinanceRepository()
        val uid=repository.ownerId ?: return Result.success()
        if (uid != inputData.getString("uid")) return Result.success()
        if(!prefs.getString("restore.$uid",null).isNullOrEmpty() || !prefs.getString("reset.$uid",null).isNullOrEmpty() || local && !prefs.getString("local.sync.checkpoint",null).isNullOrEmpty())return Result.success()
        return try {
            repository.planner.processDueRecurring()
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: IllegalArgumentException) { Result.failure() }
        catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }

    companion object {
        fun schedule(context: Context, uid: String?) {
            val work = WorkManager.getInstance(context)
            work.cancelAllWorkByTag("finflow-recurring")
            if (uid == null) return
            val request = PeriodicWorkRequestBuilder<RecurringTransactionsWorker>(6, TimeUnit.HOURS)
                .setInputData(workDataOf("uid" to uid))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if(uid=="local")NetworkType.NOT_REQUIRED else NetworkType.CONNECTED).build())
                .addTag("finflow-recurring").build()
            work.enqueueUniquePeriodicWork("finflow-recurring-$uid", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
