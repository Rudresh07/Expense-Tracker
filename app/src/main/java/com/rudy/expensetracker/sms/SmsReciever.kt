package com.rudy.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.rudy.expensetracker.repository.CategoryRepository
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmsReceiver : BroadcastReceiver(), KoinComponent {

    private val repository: TransactionRepository by inject()
    private val categoryRepository: CategoryRepository by inject()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called — action=${intent.action}")

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val grouped = messages
            .filterNotNull()
            .groupBy { it.originatingAddress.orEmpty() }

        val pendingResult = goAsync()
        scope.launch {
            try {
                grouped.forEach { (sender, parts) ->
                    val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }

                    if (body.isBlank() || sender.isBlank()) return@forEach

                    if (!SmsFilter.shouldProcess(sender, body)) {
                        Log.d(TAG, "Skipped SMS from $sender — OTP, promo, or non-bank")
                        return@forEach
                    }

                    // Resolve category based on merchant + body keywords
                    val merchant = SmsParser.parseMerchant(body)
                    val categoryName = SmsCategorizer.categorize(merchant, body)
                    val categoryId = categoryRepository.getCategoryByName(categoryName)?.id
                        ?: categoryRepository.getCategoryByName("Other")?.id
                        ?: categoryRepository.getAllCategories().firstOrNull()?.id
                        ?: 1

                    Log.d(TAG, "Auto-category: $categoryName (id=$categoryId) for merchant=$merchant")

                    val transaction = SmsParser.parse(body, categoryId)
                    if (transaction == null) {
                        Log.d(TAG, "Could not parse transaction from: $body")
                        return@forEach
                    }

                    Log.d(TAG, "Parsed transaction: amount=${transaction.amount}, " +
                            "type=${transaction.transactionType}, title=${transaction.title}")

                    repository.addTransaction(transaction)
                    Log.d(TAG, "Transaction saved successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
    