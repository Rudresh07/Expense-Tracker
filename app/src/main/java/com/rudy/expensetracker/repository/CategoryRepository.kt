package com.rudy.expensetracker.repository

import androidx.compose.ui.graphics.Color
import com.rudy.expensetracker.database.CategoryDao
import com.rudy.expensetracker.model.CategoryEntity

class CategoryRepository(private val categoryDao: CategoryDao) {

    suspend fun getAllCategories(): List<CategoryEntity> {
        return categoryDao.getAllCategories()
    }

    suspend fun insertCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return categoryDao.getCategoryByName(name)
    }

    // Initialize default categories
    suspend fun initializeDefaultCategories() {
        val existingCategories = getAllCategories()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(
                    name = "Food",
                    iconName = "restaurant",
                    colorValue = Color(0xFFFF5722).value.toLong(),
                    isCustom = false
                ),
                CategoryEntity(name = "Transport", iconName = "directions_car", colorValue = Color(0xFF2196F3).value.toLong(), isCustom = false),
                CategoryEntity(name = "Shopping", iconName = "shopping_cart", colorValue = Color(0xFF4CAF50).value.toLong(), isCustom = false),
                CategoryEntity(name = "Bills", iconName = "electric_bolt", colorValue = Color(0xFFFF9800).value.toLong(), isCustom = false),
                CategoryEntity(name = "Entertainment", iconName = "movie", colorValue = Color(0xFF9C27B0).value.toLong(), isCustom = false),
                CategoryEntity(name = "Health", iconName = "local_hospital", colorValue = Color(0xFFF44336).value.toLong(), isCustom = false),
                CategoryEntity(name = "Education", iconName = "school", colorValue = Color(0xFF607D8B).value.toLong(), isCustom = false),
                CategoryEntity(name = "Other", iconName = "category", colorValue = Color(0xFF795548).value.toLong(), isCustom = false)
            )

            defaultCategories.forEach { category ->
                insertCategory(category)
            }
        }
    }
}