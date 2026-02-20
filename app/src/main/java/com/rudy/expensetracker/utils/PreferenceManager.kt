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
}