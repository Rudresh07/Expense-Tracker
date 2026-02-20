package com.rudy.expensetracker.utils

import com.rudy.expensetracker.database.CategoryDao
import com.rudy.expensetracker.database.ExpenseDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthManager(private val prefs: PreferenceManager,
                  private val expenseDao: ExpenseDao,
                  private val categoryDao: CategoryDao,) {

    fun login(email: String, name: String = "User") {
        prefs.setUserLoginStatus(true)
        prefs.setUserEmail(email)
        prefs.setUserName(name)
    }

    fun logout() {
        prefs.setUserLoginStatus(false)
        prefs.clearUserData()
        // Clear Room database asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            expenseDao.deleteAllTransactions()     // Clear transactions
            categoryDao.deleteAllCategories()    // Clear categories (if needed)
        }

    }

    fun isUserLoggedIn(): Boolean = prefs.getUserLoginStatus()

    fun getUserEmail(): String = prefs.getUserEmail()

    fun getUserName(): String {
        val name = prefs.getUserName()
        return name.ifEmpty {
            val email = prefs.getUserEmail()
            email.substringBefore("@").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
    }
}