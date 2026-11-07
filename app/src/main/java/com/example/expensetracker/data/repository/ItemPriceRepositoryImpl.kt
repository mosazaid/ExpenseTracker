package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.ItemPriceHistoryDao
import com.example.expensetracker.data.database.entities.ItemPriceRecord
import com.example.expensetracker.domain.repository.IItemPriceRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemPriceRepositoryImpl @Inject constructor(
    private val dao: ItemPriceHistoryDao
) : IItemPriceRepository {

    override fun getAllPriceRecords(): Flow<List<ItemPriceRecord>> = dao.getAllPriceRecords()

    override fun getHistoryForItem(itemName: String): Flow<List<ItemPriceRecord>> {
        val normalized = itemName.trim().lowercase(Locale.getDefault())
        return dao.getHistoryForItem(normalized)
    }

    override suspend fun getLatestPriceForItem(itemName: String): ItemPriceRecord? {
        val normalized = itemName.trim().lowercase(Locale.getDefault())
        return dao.getLatestPriceForItem(normalized)
    }

    override fun searchItemNames(query: String): Flow<List<String>> {
        val normalized = query.trim().lowercase(Locale.getDefault())
        return dao.searchItemNames(normalized)
    }

    override fun getLatestTrackedItems(): Flow<List<ItemPriceRecord>> = dao.getLatestTrackedItems()

    override suspend fun recordItemPrice(
        itemName: String,
        unitPrice: Double,
        currency: String,
        subCategoryId: Long?,
        transactionId: Long?,
        storeOrVendor: String?,
        notes: String?
    ): Long {
        val trimmed = itemName.trim()
        val normalized = trimmed.lowercase(Locale.getDefault())
        val record = ItemPriceRecord(
            itemName = trimmed,
            normalizedItemName = normalized,
            unitPrice = unitPrice,
            currency = currency,
            subCategoryId = subCategoryId,
            transactionId = transactionId,
            storeOrVendor = storeOrVendor?.trim().takeIf { !it.isNullOrBlank() },
            notes = notes?.trim().takeIf { !it.isNullOrBlank() },
            date = Date()
        )
        return dao.insertRecord(record)
    }

    override suspend fun deleteRecord(id: Long) = dao.deleteById(id)
}

