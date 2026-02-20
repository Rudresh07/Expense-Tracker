package com.rudy.expensetracker.repository

import com.rudy.expensetracker.database.ExpenseDao
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow // Ensure this import is present

class TransactionRepository(private val dao: ExpenseDao) { // Made dao private val

    // Expose Flow directly from the DAO
    val allTransactions: Flow<List<TransactionWithCategory>> = dao.getAllExpenses()
    val totalBalance: Flow<Double> = dao.getTotalBalance() // Assuming this method exists in the DAO
    val totalIncome: Flow<Double> = dao.getTotalIncome() // Assuming this method exists in the DAO
    val totalExpense: Flow<Double> = dao.getTotalExpense() // Assuming this method exists in the DAO


    fun getTodayExpense(currentDate: String): Flow<Double> {
        return dao.getTodayExpense(currentDate)
    }

    fun getTransactionById(id: Int): Flow<TransactionWithCategory?> { // Return type matches updated DAO
        return dao.getExpenseById(id)
    }

    suspend fun addTransaction(transaction: Transaction) {
        dao.insertExpense(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        dao.updateExpense(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.deleteExpense(transaction)
    }

    suspend fun getFilteredTransaction(month: String, year: String): Flow<List<TransactionWithCategory>> {
        return dao.getTransactionsByMonthYear(month, year)
    }

}
