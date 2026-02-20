package com.rudy.expensetracker.model


import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ExpenseStatistic(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val icon: ImageVector,
    val color: Color
)
