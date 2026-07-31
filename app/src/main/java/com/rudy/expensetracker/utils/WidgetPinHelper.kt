package com.rudy.expensetracker.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.rudy.expensetracker.ExpenseTrackerWidget

fun requestPinWidget(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(context, ExpenseTrackerWidget::class.java)
        if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
            return
        }
    }
    Toast.makeText(
        context,
        "Long-press your home screen, tap Widgets, then find ExpenseTracker",
        Toast.LENGTH_LONG
    ).show()
}
