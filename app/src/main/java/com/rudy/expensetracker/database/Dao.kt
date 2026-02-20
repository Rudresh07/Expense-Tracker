package com.rudy.expensetracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Transaction)

    @Update
    suspend fun updateExpense(expense: Transaction)

    @Delete
    suspend fun deleteExpense(expense: Transaction)

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :expenseId")
    fun getExpenseById(expenseId: Int): Flow<TransactionWithCategory?> // Changed to Flow<Transaction?>

    @androidx.room.Transaction
    @Query("""
    SELECT * FROM transactions
    WHERE SUBSTR(date, 4, 2) = :month
      AND SUBSTR(date, 7, 4) = :year
""")
    fun getTransactionsByMonthYear(
        month: String, // "08"
        year: String   // "2025"
    ): Flow<List<TransactionWithCategory>>


    @Query("SELECT SUM(amount) FROM transactions")
    fun getTotalBalance(): Flow<Double> // Assuming this method exists to get total balance

    @Query("SELECT SUM(amount) FROM transactions WHERE date = :currentDate AND amount < 0")
    fun getTodayExpense(currentDate: String): Flow<Double> // Assuming this method exists to

    @Query("SELECT  SUM(amount) FROM transactions where amount > 0 ORDER BY date DESC")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT  SUM(amount) FROM transactions where amount < 0 ORDER BY date DESC")
    fun getTotalExpense(): Flow<Double>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isCustom DESC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Insert
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}


