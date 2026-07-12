package com.rudy.expensetracker.utils

import android.content.Context
import androidx.core.content.edit

class PreferenceManager(context: Context) {

    private val mPrefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    fun setMonthlyBudget(amount: Double) { mPrefs.edit { putFloat("monthly_budget", amount.toFloat()) } }
    fun getMonthlyBudget(): Double = mPrefs.getFloat("monthly_budget", 0f).toDouble()

    fun setInitialSmsScanDone() { mPrefs.edit { putBoolean("initial_sms_scan_done", true) } }
    fun isInitialSmsScanDone(): Boolean = mPrefs.getBoolean("initial_sms_scan_done", false)

    fun setBatteryOptimizationPromptShown() { mPrefs.edit { putBoolean("battery_opt_prompt_shown", true) } }
    fun isBatteryOptimizationPromptShown(): Boolean = mPrefs.getBoolean("battery_opt_prompt_shown", false)

}