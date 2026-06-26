package com.rudy.expensetracker.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rudy.expensetracker.model.CategoryEntity
import com.rudy.expensetracker.model.MerchantLearning
import com.rudy.expensetracker.model.Transaction

@Database(
    entities = [Transaction::class, CategoryEntity::class, MerchantLearning::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantLearningDao(): MerchantLearningDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN needsReview INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `merchant_learning` (
                        `merchantKey` TEXT NOT NULL,
                        `confirmedCategoryId` INTEGER NOT NULL,
                        `confirmCount` INTEGER NOT NULL,
                        `lastConfirmedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`merchantKey`)
                    )
                """.trimIndent())
            }
        }
    }
}