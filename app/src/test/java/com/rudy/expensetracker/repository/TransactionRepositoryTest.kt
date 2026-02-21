package com.rudy.expensetracker.repository

import com.rudy.expensetracker.database.ExpenseDao
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionRepositoryTest {

    private lateinit var dao: ExpenseDao
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        dao = mockk()
        every { dao.getAllExpenses() } returns flowOf(emptyList())
        every { dao.getTotalBalance() } returns flowOf(0.0)
        every { dao.getTotalIncome() } returns flowOf(0.0)
        every { dao.getTotalExpense() } returns flowOf(0.0)
        repository = TransactionRepository(dao)
    }

    @Test
    fun `addTransaction delegates to dao insertExpense`() = runTest {
        val transaction = buildTransaction()
        coJustRun { dao.insertExpense(transaction) }

        repository.addTransaction(transaction)

        coVerify(exactly = 1) { dao.insertExpense(transaction) }
    }

    @Test
    fun `updateTransaction delegates to dao updateExpense`() = runTest {
        val transaction = buildTransaction()
        coJustRun { dao.updateExpense(transaction) }

        repository.updateTransaction(transaction)

        coVerify(exactly = 1) { dao.updateExpense(transaction) }
    }

    @Test
    fun `deleteTransaction delegates to dao deleteExpense`() = runTest {
        val transaction = buildTransaction()
        coJustRun { dao.deleteExpense(transaction) }

        repository.deleteTransaction(transaction)

        coVerify(exactly = 1) { dao.deleteExpense(transaction) }
    }

    @Test
    fun `getTodayExpense returns flow from dao`() = runTest {
        val date = "21 02 2026"
        val flow: Flow<Double> = flowOf(150.0)
        every { dao.getTodayExpense(date) } returns flow

        val result = repository.getTodayExpense(date)

        assertEquals(flow, result)
    }

    @Test
    fun `getTransactionById returns flow from dao`() = runTest {
        val id = 1
        val twc = buildTransactionWithCategory()
        val flow: Flow<TransactionWithCategory?> = flowOf(twc)
        every { dao.getExpenseById(id) } returns flow

        val result = repository.getTransactionById(id)

        assertEquals(flow, result)
    }

    @Test
    fun `getFilteredTransaction returns flow from dao`() = runTest {
        val month = "02"
        val year = "2026"
        val flow: Flow<List<TransactionWithCategory>> = flowOf(emptyList())
        every { dao.getTransactionsByMonthYear(month, year) } returns flow

        val result = repository.getFilteredTransaction(month, year)

        assertEquals(flow, result)
    }

    @Test
    fun `allTransactions exposes dao getAllExpenses flow`() {
        val transactions = listOf(buildTransactionWithCategory())
        val expectedFlow = flowOf(transactions)
        every { dao.getAllExpenses() } returns expectedFlow

        val repo = TransactionRepository(dao)

        assertEquals(expectedFlow, repo.allTransactions)
    }

    // region helpers

    private fun buildTransaction() = Transaction(
        id = 1,
        category = 1,
        title = "Groceries",
        transactionType = 0,
        time = "12:00",
        amount = -200.0,
        date = "21 02 2026",
        note = ""
    )

    private fun buildTransactionWithCategory() = TransactionWithCategory(
        transaction = buildTransaction(),
        category = CategoryEntity(id = 1, name = "Food", iconName = "restaurant", colorValue = 0L)
    )

    // endregion
}
