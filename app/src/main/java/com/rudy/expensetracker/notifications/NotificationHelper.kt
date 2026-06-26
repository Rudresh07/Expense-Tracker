package com.rudy.expensetracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rudy.expensetracker.MainActivity
import com.rudy.expensetracker.R
import com.rudy.expensetracker.widget.EXTRA_NAVIGATE_TO

class NotificationHelper(private val context: Context) {

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Review",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for uncategorised transactions"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    fun postReviewNotification(
        txnId: Int,
        merchant: String,
        amount: Double,
        suggestedCategoryId: Int,
        suggestedCategoryName: String,
        merchantKey: String,
        isWalletProxy: Boolean,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val confirmIntent = PendingIntent.getBroadcast(
            context,
            txnId,
            Intent(context, CategoryConfirmationReceiver::class.java).apply {
                action = ACTION_CONFIRM
                putExtra(EXTRA_TXN_ID, txnId)
                putExtra(EXTRA_CATEGORY_ID, suggestedCategoryId)
                putExtra(EXTRA_MERCHANT_KEY, merchantKey)
                putExtra(EXTRA_IS_WALLET_PROXY, isWalletProxy)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openAppIntent = PendingIntent.getActivity(
            context,
            txnId + 100_000,
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_NAVIGATE_TO, "pending_review")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_attach_money_orange)
            .setContentTitle("Uncategorised transaction")
            .setContentText("$merchant · ₹${"%.2f".format(amount)}")
            .setContentIntent(openAppIntent)
            .addAction(0, "✓ Confirm $suggestedCategoryName", confirmIntent)
            .addAction(0, "Choose Category →", openAppIntent)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(txnId, notification)

        // Summary notification so individual ones collapse into a group
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_attach_money_orange)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
    }

    fun cancelNotification(txnId: Int) {
        NotificationManagerCompat.from(context).cancel(txnId)
    }

    companion object {
        const val CHANNEL_ID = "expense_review"
        const val ACTION_CONFIRM = "com.rudy.expensetracker.ACTION_CONFIRM_CATEGORY"
        const val EXTRA_TXN_ID = "txn_id"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_MERCHANT_KEY = "merchant_key"
        const val EXTRA_IS_WALLET_PROXY = "is_wallet_proxy"
        const val GROUP_KEY = "expense_review_group"
        private const val SUMMARY_ID = 0
    }
}
