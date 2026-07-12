package com.rudy.expensetracker.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.rudy.expensetracker.repository.CategoryRepository
import com.rudy.expensetracker.repository.MerchantLearningRepository
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

class PastSmsScanWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params), KoinComponent {

    private val repository: TransactionRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val learningRepository: MerchantLearningRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "doWork() started, attempt=$runAttemptCount")
        try {
            val resolver = applicationContext.contentResolver

            val startOfMonth = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val now = System.currentTimeMillis()
            Log.d(TAG, "Querying SMS from $startOfMonth to $now")

            val uri = Uri.parse("content://sms/inbox")

            val cursor = resolver.query(
                uri,
                arrayOf("address", "body", "date"),
                "date >= ? AND date <= ?",
                arrayOf(startOfMonth.toString(), now.toString()),
                "date DESC"
            )

            if (cursor == null) {
                Log.e(TAG, "Cursor is null — READ_SMS permission likely denied")
                return@withContext Result.failure()
            }

            Log.d(TAG, "Total SMS in range: ${cursor.count}")

            categoryRepository.initializeDefaultCategories()
            Log.d(TAG, "Default categories initialized")

            var totalSeen = 0
            var skippedNotBank = 0
            var skippedParseFail = 0
            var saved = 0

            cursor.use {
                val addrIdx = it.getColumnIndexOrThrow("address")
                val bodyIdx = it.getColumnIndexOrThrow("body")

                while (it.moveToNext()) {
                    totalSeen++
                    val sender = it.getString(addrIdx) ?: continue
                    val body   = it.getString(bodyIdx)  ?: continue

                    if (!SmsFilter.shouldProcess(sender, body)) {
                        Log.v(TAG, "Skipped [$sender] — not a bank/transaction SMS")
                        skippedNotBank++
                        continue
                    }

                    Log.d(TAG, "Bank SMS from [$sender]: ${body.take(80)}…")

                    val merchant = SmsParser.parseMerchant(body)
                    val merchantKey = merchant?.lowercase()?.trim() ?: ""
                    val categoryName = SmsCategorizer.categorize(merchant, body)

                    // For historical scan: apply learning for direct merchants, but no notifications
                    val rawVpa = Regex("""[A-Za-z0-9._\-]+@[A-Za-z0-9]+""").find(body)?.value
                    val isWalletProxy = rawVpa != null && WalletVpaDetector.isWalletProxy(rawVpa)
                    val learnedCategoryId = if (!isWalletProxy && merchantKey.isNotBlank() && categoryName == "Other")
                        learningRepository.findConfirmedCategory(merchantKey) else null

                    val categoryId = learnedCategoryId
                        ?: categoryRepository.getCategoryByName(categoryName)?.id
                        ?: categoryRepository.getCategoryByName("Other")?.id
                        ?: categoryRepository.getAllCategories().firstOrNull()?.id
                        ?: 1

                    Log.d(TAG, "Resolved: merchant='$merchant', category='$categoryName', id=$categoryId, learned=${learnedCategoryId != null}")

                    val txn = SmsParser.parse(body, categoryId)
                    if (txn == null) {
                        Log.w(TAG, "Parse failed for body: ${body.take(120)}")
                        skippedParseFail++
                        continue
                    }

                    Log.d(TAG, "Saving — title=${txn.title}, amount=${txn.amount}, type=${txn.transactionType}")
                    repository.addTransaction(txn)
                    saved++
                }
            }

            Log.d(TAG, "Scan complete — seen=$totalSeen, skippedNotBank=$skippedNotBank, parseFail=$skippedParseFail, saved=$saved")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork() crashed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "PastSmsScan"
        private const val WORK_NAME = "past_sms_scan"

        fun enqueue(ctx: Context) {
            val request = OneTimeWorkRequestBuilder<PastSmsScanWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(ctx).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
