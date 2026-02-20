package com.rudy.expensetracker.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
@Entity(tableName = "transactions") // Using "transactions" as the table name
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: Int,
    val title:String,
    val transactionType: Int,
    val time: String,
    val amount: Double,
    val date: String,
    val note:String,
): Parcelable


// Updated CategoryEntity for the hybrid approach
@Parcelize
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconName: String,
    val colorValue: Long,
    val isCustom: Boolean = false // Track if it's a user-created category
): Parcelable

data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "category",
        entityColumn = "id"
    )
    val category: CategoryEntity
)


fun getRandomDate(): String {
    val day = Random.nextInt(1, 28) // 1 to 27 (safe for all months)
    val month = Random.nextInt(1, 12) // 1 to 12
    val year = 2025
    return "$day/${month.toString().padStart(2, '0')}/$year"
}
