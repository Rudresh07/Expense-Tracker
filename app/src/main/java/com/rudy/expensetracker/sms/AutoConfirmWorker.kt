package com.rudy.expensetracker.sms

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rudy.expensetracker.notifications.NotificationHelper
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class AutoConfirmWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params), KoinComponent {

    private val repository: TransactionRepository by inject()
    private val notificationHelper: NotificationHelper by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cutoff = LocalDate.now()
                .minusDays(STALE_DAYS)
                .format(DateTimeFormatter.ofPattern("dd MM yyyy"))

            val stale = repository.getStaleReviewTransactions(cutoff)
            Log.d(TAG, "Auto-confirming ${stale.size} stale review transactions")

            stale.forEach { txnWithCategory ->
                val txn = txnWithCategory.transaction
                repository.clearNeedsReview(txn.id)
                notificationHelper.cancelNotification(txn.id)
                Log.d(TAG, "Auto-confirmed txnId=${txn.id} keeping category=${txnWithCategory.category.name}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "AutoConfirmWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutoConfirmWorker"
        private const val WORK_NAME = "auto_confirm_review"
        private const val STALE_DAYS = 3L

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoConfirmWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
