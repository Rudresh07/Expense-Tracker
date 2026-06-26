package com.rudy.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.rudy.expensetracker.notifications.NotificationHelper
import com.rudy.expensetracker.repository.CategoryRepository
import com.rudy.expensetracker.repository.MerchantLearningRepository
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: TransactionRepository by inject()
    private val categoryRepository: CategoryRepository by inject()
    private val learningRepository: MerchantLearningRepository by inject()
    private val notificationHelper: NotificationHelper by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called — action=${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val grouped = messages.filterNotNull().groupBy { it.originatingAddress.orEmpty() }

        val pendingResult = goAsync()
        scope.launch {
            try {
                grouped.forEach { (sender, parts) ->
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }
                    val smsTimestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    if (body.isBlank() || sender.isBlank()) return@forEach
                    if (!SmsFilter.shouldProcess(sender, body)) {
                        Log.d(TAG, "Skipped SMS from $sender")
                        return@forEach
                    }

                    processSms(body, smsTimestamp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processSms(body: String, smsTimestamp: Long) {
        val merchant = SmsParser.parseMerchant(body)
        val merchantKey = merchant?.lowercase()?.trim() ?: ""

        // Detect wallet proxy before anything else
        val rawVpa = extractRawVpa(body)
        val isWalletProxy = rawVpa != null && WalletVpaDetector.isWalletProxy(rawVpa)

        // Categorize: keyword matching first
        val categoryName = SmsCategorizer.categorize(merchant, body)
        // Wallet proxies (Amazon Pay, Paytm, PhonePe, …) can be used at any merchant,
        // so the keyword-matched category is meaningless — always treat as Other.
        val isOther = categoryName == "Other" || isWalletProxy

        val resolvedCategoryId: Int
        val needsReview: Boolean

        if (isOther) {
            if (!isWalletProxy && merchantKey.isNotBlank()) {
                // Direct merchant: check learning table
                val learned = learningRepository.findConfirmedCategory(merchantKey)
                if (learned != null) {
                    Log.d(TAG, "Learning hit: $merchantKey → categoryId=$learned")
                    resolvedCategoryId = learned
                    needsReview = false
                } else {
                    resolvedCategoryId = otherCategoryId()
                    needsReview = true
                }
            } else {
                // Wallet proxy: always prompt, never look up learning
                resolvedCategoryId = otherCategoryId()
                needsReview = true
            }
        } else {
            resolvedCategoryId = resolveCategoryId(categoryName)
            needsReview = false
        }

        val transaction = SmsParser.parse(body, resolvedCategoryId)
            ?.copy(needsReview = needsReview)
            ?: return.also { Log.w(TAG, "Parse failed for body: ${body.take(80)}") }

        val txnId = repository.addTransaction(transaction).toInt()
        Log.d(TAG, "Saved txnId=$txnId needsReview=$needsReview merchant=$merchant")

        if (needsReview) {
            val smsDate = Instant.ofEpochMilli(smsTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val isToday = smsDate >= LocalDate.now(ZoneId.systemDefault())
            if (isToday) {
                val suggestedCategory = categoryRepository.getCategoryByName("Other")
                notificationHelper.postReviewNotification(
                    txnId = txnId,
                    merchant = transaction.title,
                    amount = abs(transaction.amount),
                    suggestedCategoryId = resolvedCategoryId,
                    suggestedCategoryName = suggestedCategory?.name ?: "Other",
                    merchantKey = merchantKey,
                    isWalletProxy = isWalletProxy,
                )
            } else {
                Log.d(TAG, "Skipping notification for txnId=$txnId — SMS is from $smsDate, not today")
            }
        }
    }

    private suspend fun resolveCategoryId(categoryName: String): Int {
        return categoryRepository.getCategoryByName(categoryName)?.id
            ?: otherCategoryId()
    }

    private suspend fun otherCategoryId(): Int {
        return categoryRepository.getCategoryByName("Other")?.id
            ?: categoryRepository.getAllCategories().firstOrNull()?.id
            ?: 1
    }

    // Extracts the raw VPA string from the SMS body before sanitizeMerchant translates it
    private fun extractRawVpa(body: String): String? {
        val vpaRegex = Regex("""[A-Za-z0-9._\-]+@[A-Za-z0-9]+""")
        return vpaRegex.find(body)?.value
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
