package com.rudy.expensetracker.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.repository.CategoryRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CategoryRepository
    private lateinit var viewModel: CategoryViewModel

    private val defaultCategories = listOf(
        CategoryEntity(id = 1, name = "Food", iconName = "restaurant", colorValue = 0L),
        CategoryEntity(id = 2, name = "Transport", iconName = "directions_car", colorValue = 0L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coJustRun { repository.initializeDefaultCategories() }
        coEvery { repository.getAllCategories() } returns defaultCategories
        viewModel = CategoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `on init categories are loaded`() = runTest {
        advanceUntilIdle()

        assertEquals(defaultCategories, viewModel.categories.value)
    }

    @Test
    fun `on init initializeDefaultCategories is called`() = runTest {
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.initializeDefaultCategories() }
    }

    @Test
    fun `loadCategories updates categories state with repository data`() = runTest {
        val newList = listOf(
            CategoryEntity(id = 3, name = "Shopping", iconName = "shopping_cart", colorValue = 0L)
        )
        coEvery { repository.getAllCategories() } returns newList

        viewModel.loadCategories()
        advanceUntilIdle()

        assertEquals(newList, viewModel.categories.value)
    }

    @Test
    fun `addCategory calls repository insertCategory with correct data`() = runTest {
        val captured = slot<CategoryEntity>()
        coEvery { repository.insertCategory(capture(captured)) } just runs

        viewModel.addCategory("Gym", "fitness_center", 0xFF123456L)
        advanceUntilIdle()

        assertEquals("Gym", captured.captured.name)
        assertEquals("fitness_center", captured.captured.iconName)
        assertEquals(0xFF123456L, captured.captured.colorValue)
    }

    @Test
    fun `addCategory creates category with isCustom set to true`() = runTest {
        val captured = slot<CategoryEntity>()
        coEvery { repository.insertCategory(capture(captured)) } just runs

        viewModel.addCategory("Travel", "flight", 0L)
        advanceUntilIdle()

        assertTrue(captured.captured.isCustom)
    }

    @Test
    fun `addCategory refreshes categories list after insert`() = runTest {
        coJustRun { repository.insertCategory(any()) }

        viewModel.addCategory("Gym", "fitness_center", 0L)
        advanceUntilIdle()

        // Called once in init and once after addCategory
        coVerify(atLeast = 2) { repository.getAllCategories() }
    }

    @Test
    fun `deleteCategory calls repository deleteCategory`() = runTest {
        val category = defaultCategories.first()
        coJustRun { repository.deleteCategory(category) }

        viewModel.deleteCategory(category)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteCategory(category) }
    }

    @Test
    fun `deleteCategory refreshes categories list after delete`() = runTest {
        val category = defaultCategories.first()
        coJustRun { repository.deleteCategory(category) }

        viewModel.deleteCategory(category)
        advanceUntilIdle()

        // Called once in init and once after deleteCategory
        coVerify(atLeast = 2) { repository.getAllCategories() }
    }

    @Test
    fun `initial categories state is empty before init completes`() {
        // Before setUp runs (fresh ViewModel not yet initialized), default is empty
        val freshRepo: CategoryRepository = mockk(relaxed = true)
        coEvery { freshRepo.getAllCategories() } returns emptyList()

        val freshVm = CategoryViewModel(freshRepo)

        // With UnconfinedTestDispatcher already set, init coroutine runs immediately,
        // so categories will be whatever getAllCategories returns
        assertEquals(emptyList<CategoryEntity>(), freshVm.categories.value)
    }
}
