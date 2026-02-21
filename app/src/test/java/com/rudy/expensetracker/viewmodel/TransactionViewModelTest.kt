package com.rudy.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.repository.TransactionRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: TransactionViewmodel

    private val fakeCategory = CategoryEntity(id = 1, name = "Food", iconName = "restaurant", colorValue = 0L)
    private val fakeTransaction = Transaction(
        id = 1, category = 1, title = "Groceries",
        transactionType = 0, time = "12:00", amount = -200.0, date = "21 02 2026", note = ""
    )
    private val fakeTwc = TransactionWithCategory(fakeTransaction, fakeCategory)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.allTransactions } returns flowOf(emptyList())
        every { repository.totalBalance } returns flowOf(0.0)
        every { repository.totalIncome } returns flowOf(0.0)
        every { repository.totalExpense } returns flowOf(0.0)
        every { repository.getTodayExpense(any()) } returns flowOf(0.0)
        viewModel = TransactionViewmodel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial transactionList is empty`() {
        assertEquals(emptyList<TransactionWithCategory>(), viewModel.transactionList.value)
    }

    @Test
    fun `initial totalBalance is 0`() {
        assertEquals(0.0, viewModel.totalBalance.value, 0.0)
    }

    @Test
    fun `initial totalIncome is 0`() {
        assertEquals(0.0, viewModel.totalIncome.value, 0.0)
    }

    @Test
    fun `initial totalExpense is 0`() {
        assertEquals(0.0, viewModel.totalExpense.value, 0.0)
    }

    @Test
    fun `initial todayExpense is 0`() {
        assertEquals(0.0, viewModel.todayExpense.value, 0.0)
    }

    @Test
    fun `addTransaction delegates to repository`() = runTest(testDispatcher) {
        coJustRun { repository.addTransaction(fakeTransaction) }

        viewModel.addTransaction(fakeTransaction)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.addTransaction(fakeTransaction) }
    }

    @Test
    fun `updateTransaction delegates to repository`() = runTest(testDispatcher) {
        coJustRun { repository.updateTransaction(fakeTransaction) }

        viewModel.updateTransaction(fakeTransaction)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateTransaction(fakeTransaction) }
    }

    @Test
    fun `deleteTransaction delegates to repository`() = runTest(testDispatcher) {
        coJustRun { repository.deleteTransaction(fakeTransaction) }

        viewModel.deleteTransaction(fakeTransaction)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteTransaction(fakeTransaction) }
    }

    @Test
    fun `getTransactionById updates transactionDetail state`() = runTest(testDispatcher) {
        coEvery { repository.getTransactionById(1) } returns flowOf(fakeTwc)

        viewModel.getTransactionById(1)
        advanceUntilIdle()

        assertEquals(fakeTwc, viewModel.transactionDetail.value)
    }

    @Test
    fun `getTransactionById sets transactionDetail to null when not found`() = runTest(testDispatcher) {
        coEvery { repository.getTransactionById(99) } returns flowOf(null)

        viewModel.getTransactionById(99)
        advanceUntilIdle()

        assertEquals(null, viewModel.transactionDetail.value)
    }

    @Test
    fun `getFilteredTransaction updates filteredTransactionList`() = runTest(testDispatcher) {
        val filtered = listOf(fakeTwc)
        coEvery { repository.getFilteredTransaction("02", "2026") } returns flowOf(filtered)

        viewModel.getFilteredTransaction("02", "2026")
        advanceUntilIdle()

        assertEquals(filtered, viewModel.filteredTransactionList.value)
    }

    @Test
    fun `getFilteredTransaction with no results produces empty list`() = runTest(testDispatcher) {
        coEvery { repository.getFilteredTransaction("01", "2020") } returns flowOf(emptyList())

        viewModel.getFilteredTransaction("01", "2020")
        advanceUntilIdle()

        assertEquals(emptyList<TransactionWithCategory>(), viewModel.filteredTransactionList.value)
    }

    @Test
    fun `loadTodayExpense updates todayExpense state`() = runTest(testDispatcher) {
        every { repository.getTodayExpense(any()) } returns flowOf(350.0)

        viewModel = TransactionViewmodel(repository)
        advanceUntilIdle()

        assertEquals(350.0, viewModel.todayExpense.value, 0.0)
    }
}
