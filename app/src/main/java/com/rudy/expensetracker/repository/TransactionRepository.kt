package com.rudy.expensetracker.repository

import android.content.Context
import com.rudy.expensetracker.database.ExpenseDao
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.widget.AppWidget
import kotlinx.coroutines.flow.Flow // Ensure this import is present

class TransactionRepository(private val dao: ExpenseDao,
                            private val context: Context
) { // Made dao private val

    // Expose Flow directly from the DAO
    val allTransactions: Flow<List<TransactionWithCategory>> = dao.getAllExpenses()
    val totalBalance: Flow<Double> = dao.getTotalBalance()
    val totalIncome: Flow<Double> = dao.getTotalIncome()
    val totalExpense: Flow<Double> = dao.getTotalExpense()


    fun getTodayExpense(currentDate: String): Flow<Double> {
        return dao.getTodayExpense(currentDate)
    }

    fun getTransactionById(id: Int): Flow<TransactionWithCategory?> {
        return dao.getExpenseById(id)
    }

    val pendingReviewCount: Flow<Int> = dao.getPendingReviewCount()
    val pendingReviewTransactions: Flow<List<TransactionWithCategory>> = dao.getPendingReviewTransactions()

    suspend fun addTransaction(transaction: Transaction): Long {
        val id = dao.insertExpense(transaction)
        AppWidget().updateAll(context)
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        dao.updateExpense(transaction)
        AppWidget().updateAll(context)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteExpense(transaction)
        AppWidget().updateAll(context)
    }

    suspend fun getFilteredTransaction(month: String, year: String): Flow<List<TransactionWithCategory>> {
        return dao.getTransactionsByMonthYear(month, year)
    }

    suspend fun updateCategoryAndClearReview(txnId: Int, categoryId: Int) {
        dao.updateCategoryAndClearReview(txnId, categoryId)
        AppWidget().updateAll(context)
    }

    suspend fun clearNeedsReview(txnId: Int) {
        dao.clearNeedsReview(txnId)
    }

    suspend fun getStaleReviewTransactions(cutoffDate: String) =
        dao.getStaleReviewTransactions(cutoffDate)
}
