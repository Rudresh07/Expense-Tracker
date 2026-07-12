package com.rudy.expensetracker.utils

import android.content.Context
import androidx.core.content.edit

class PreferenceManager(context: Context) {

    private val mPrefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    fun setUserName(name: String) {
        mPrefs.edit { putString("user_name", name) }
    }

    fun getUserName(): String {
        return mPrefs.getString("user_name", "") ?: ""
    }

    fun setUserEmail(email: String) {
        mPrefs.edit { putString("user_email", email) }
    }

    fun getUserEmail(): String {
        return mPrefs.getString("user_email", "") ?: ""
    }

    fun setUserLoginStatus(isLoggedIn: Boolean) {
        mPrefs.edit { putBoolean("is_logged_in", isLoggedIn) }
    }

    fun getUserLoginStatus(): Boolean {
        return mPrefs.getBoolean("is_logged_in", false)
    }

    fun clearUserData() {
        mPrefs.edit { clear() }
    }

    fun setMonthlyBudget(amount: Double) { mPrefs.edit { putFloat("monthly_budget", amount.toFloat()) } }
    fun getMonthlyBudget(): Double = mPrefs.getFloat("monthly_budget", 0f).toDouble()

    fun setInitialSmsScanDone() { mPrefs.edit { putBoolean("initial_sms_scan_done", true) } }
    fun isInitialSmsScanDone(): Boolean = mPrefs.getBoolean("initial_sms_scan_done", false)

    fun setBatteryOptimizationPromptShown() { mPrefs.edit { putBoolean("battery_opt_prompt_shown", true) } }
    fun isBatteryOptimizationPromptShown(): Boolean = mPrefs.getBoolean("battery_opt_prompt_shown", false)

}