package com.rudy.expensetracker.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        initializeCategories()
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                categoryRepository.initializeDefaultCategories()
                loadCategories()
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error initializing categories", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val categoriesList = categoryRepository.getAllCategories()
                _categories.value = categoriesList
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error loading categories", e)
            }
        }
    }

    fun addCategory(name: String, iconName: String, colorValue: Long) {
        viewModelScope.launch {
            try {
                val category = CategoryEntity(
                    name = name,
                    iconName = iconName,
                    colorValue = colorValue,
                    isCustom = true
                )
                categoryRepository.insertCategory(category)
                loadCategories() // Refresh the list
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error adding category", e)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            try {
                categoryRepository.deleteCategory(category)
                loadCategories() // Refresh the list
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error deleting category", e)
            }
        }
    }
}