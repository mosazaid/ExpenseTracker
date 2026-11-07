package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.ItemPriceRecord
import kotlinx.coroutines.flow.Flow

interface IItemPriceRepository {
    fun getAllPriceRecords(): Flow<List<ItemPriceRecord>>
    fun getHistoryForItem(itemName: String): Flow<List<ItemPriceRecord>>
    suspend fun getLatestPriceForItem(itemName: String): ItemPriceRecord?
    fun searchItemNames(query: String): Flow<List<String>>
    fun getLatestTrackedItems(): Flow<List<ItemPriceRecord>>
    suspend fun recordItemPrice(
        itemName: String,
        unitPrice: Double,
        currency: String = "JOD",
        subCategoryId: Long? = null,
        transactionId: Long? = null,
        storeOrVendor: String? = null,
        notes: String? = null
    ): Long
    suspend fun deleteRecord(id: Long)
}

