package com.rudy.expensetracker

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.rudy.expensetracker.widget.AppWidget


class ExpenseTrackerWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AppWidget()
}