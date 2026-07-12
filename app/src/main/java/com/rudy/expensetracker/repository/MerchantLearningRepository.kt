package com.rudy.expensetracker.repository

import com.rudy.expensetracker.database.MerchantLearningDao
import com.rudy.expensetracker.model.MerchantLearning

class MerchantLearningRepository(private val dao: MerchantLearningDao) {

    suspend fun findConfirmedCategory(merchantKey: String): Int? =
        dao.findByKey(merchantKey.normalize())?.confirmedCategoryId

    suspend fun saveConfirmation(merchantKey: String, categoryId: Int) {
        val key = merchantKey.normalize()
        val existing = dao.findByKey(key)
        dao.upsert(
            MerchantLearning(
                merchantKey = key,
                confirmedCategoryId = categoryId,
                confirmCount = (existing?.confirmCount ?: 0) + 1,
                lastConfirmedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun String.normalize(): String =
        lowercase().trim().replace(Regex("[^a-z0-9 ]"), "").trim()
}
