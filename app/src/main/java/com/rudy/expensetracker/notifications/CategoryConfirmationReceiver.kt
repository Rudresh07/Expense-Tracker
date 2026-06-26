package com.rudy.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rudy.expensetracker.repository.MerchantLearningRepository
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CategoryConfirmationReceiver : BroadcastReceiver(), KoinComponent {

    private val transactionRepository: TransactionRepository by inject()
    private val learningRepository: MerchantLearningRepository by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_CONFIRM) return

        val txnId = intent.getIntExtra(NotificationHelper.EXTRA_TXN_ID, -1)
        val categoryId = intent.getIntExtra(NotificationHelper.EXTRA_CATEGORY_ID, -1)
        val merchantKey = intent.getStringExtra(NotificationHelper.EXTRA_MERCHANT_KEY) ?: ""
        val isWalletProxy = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_WALLET_PROXY, true)

        if (txnId == -1 || categoryId == -1) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                transactionRepository.updateCategoryAndClearReview(txnId, categoryId)

                // Only save to learning table for direct merchants, not wallet proxies
                if (!isWalletProxy && merchantKey.isNotBlank()) {
                    learningRepository.saveConfirmation(merchantKey, categoryId)
                    Log.d(TAG, "Saved learning: $merchantKey → categoryId=$categoryId")
                }

                NotificationHelper(context).cancelNotification(txnId)
                Log.d(TAG, "Confirmed txnId=$txnId categoryId=$categoryId isWalletProxy=$isWalletProxy")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to confirm category for txnId=$txnId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CategoryConfirmReceiver"
    }
}
