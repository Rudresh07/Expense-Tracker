package com.rudy.expensetracker.repository

import com.rudy.expensetracker.database.CategoryDao
import com.rudy.expensetracker.model.CategoryEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var dao: CategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        dao = mockk()
        repository = CategoryRepository(dao)
    }

    @Test
    fun `getAllCategories delegates to dao`() = runTest {
        val categories = listOf(buildCategory())
        coEvery { dao.getAllCategories() } returns categories

        val result = repository.getAllCategories()

        assertEquals(categories, result)
    }

    @Test
    fun `insertCategory delegates to dao`() = runTest {
        val category = buildCategory()
        coJustRun { dao.insertCategory(category) }

        repository.insertCategory(category)

        coVerify(exactly = 1) { dao.insertCategory(category) }
    }

    @Test
    fun `deleteCategory delegates to dao`() = runTest {
        val category = buildCategory()
        coJustRun { dao.deleteCategory(category) }

        repository.deleteCategory(category)

        coVerify(exactly = 1) { dao.deleteCategory(category) }
    }

    @Test
    fun `getCategoryByName returns matching category`() = runTest {
        val category = buildCategory()
        coEvery { dao.getCategoryByName("Food") } returns category

        val result = repository.getCategoryByName("Food")

        assertEquals(category, result)
    }

    @Test
    fun `getCategoryByName returns null when not found`() = runTest {
        coEvery { dao.getCategoryByName("Unknown") } returns null

        val result = repository.getCategoryByName("Unknown")

        assertNull(result)
    }

    @Test
    fun `initializeDefaultCategories inserts 8 categories when database is empty`() = runTest {
        coEvery { dao.getAllCategories() } returns emptyList()
        coJustRun { dao.insertCategory(any()) }

        repository.initializeDefaultCategories()

        coVerify(exactly = 8) { dao.insertCategory(any()) }
    }

    @Test
    fun `initializeDefaultCategories does not insert when categories already exist`() = runTest {
        coEvery { dao.getAllCategories() } returns listOf(buildCategory())

        repository.initializeDefaultCategories()

        coVerify(exactly = 0) { dao.insertCategory(any()) }
    }

    @Test
    fun `initializeDefaultCategories inserts correct default category names`() = runTest {
        val inserted = mutableListOf<CategoryEntity>()
        coEvery { dao.getAllCategories() } returns emptyList()
        coEvery { dao.insertCategory(capture(inserted)) } just runs

        repository.initializeDefaultCategories()

        val names = inserted.map { it.name }
        assertEquals(
            listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Education", "Other"),
            names
        )
    }

    @Test
    fun `initializeDefaultCategories inserts all non-custom categories`() = runTest {
        val inserted = mutableListOf<CategoryEntity>()
        coEvery { dao.getAllCategories() } returns emptyList()
        coEvery { dao.insertCategory(capture(inserted)) } just runs

        repository.initializeDefaultCategories()

        assertTrue(inserted.all { !it.isCustom })
    }

    // region helpers

    private fun buildCategory() = CategoryEntity(
        id = 1, name = "Food", iconName = "restaurant", colorValue = 0L, isCustom = false
    )

    // endregion
}
