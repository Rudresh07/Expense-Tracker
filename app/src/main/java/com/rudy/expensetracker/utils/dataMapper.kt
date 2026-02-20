package com.rudy.expensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.graphics.Color
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.model.ExpenseStatistic

fun List<TransactionWithCategory>.toExpenseStatistics(): List<ExpenseStatistic> {
    val transactions = this
    if (transactions.isEmpty()) return emptyList()

    val total = transactions.sumOf { kotlin.math.abs(it.transaction.amount) } // total absolute spending

    return transactions
        .groupBy { it.category }
        .map { (category, group) ->
            val sum = group.sumOf { kotlin.math.abs(it.transaction.amount) }
            val percentage = (sum / total) * 100.0

           // Log.d("IconVector","${group.first().category.iconName}")

            ExpenseStatistic(
                category = category.name,
                amount = sum,
                percentage = percentage,
                icon = IconManager.getIconByName(group.first().category.iconName)?:Icons.Filled.Error,
                color = group.first().category.colorValue.toColor()
            )
        }

}

fun Long.toColor(): Color = Color(this.toULong())

