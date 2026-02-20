package com.rudy.expensetracker.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.Transaction

@Database(
    entities = [Transaction::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
}