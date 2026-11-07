package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.ItemPriceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPriceHistoryDao {

    @Query("SELECT * FROM item_price_history ORDER BY date DESC")
    fun getAllPriceRecords(): Flow<List<ItemPriceRecord>>

    @Query("SELECT * FROM item_price_history WHERE normalizedItemName = :normalizedName ORDER BY date DESC")
    fun getHistoryForItem(normalizedName: String): Flow<List<ItemPriceRecord>>

    @Query("SELECT * FROM item_price_history WHERE normalizedItemName = :normalizedName ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPriceForItem(normalizedName: String): ItemPriceRecord?

    @Query("""
        SELECT DISTINCT itemName FROM item_price_history 
        WHERE normalizedItemName LIKE '%' || :query || '%' 
        ORDER BY itemName ASC LIMIT 10
    """)
    fun searchItemNames(query: String): Flow<List<String>>

    @Query("""
        SELECT * FROM item_price_history 
        WHERE id IN (
            SELECT MAX(id) FROM item_price_history GROUP BY normalizedItemName
        )
        ORDER BY date DESC
    """)
    fun getLatestTrackedItems(): Flow<List<ItemPriceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ItemPriceRecord): Long

    @Update
    suspend fun updateRecord(record: ItemPriceRecord)

    @Delete
    suspend fun deleteRecord(record: ItemPriceRecord)

    @Query("DELETE FROM item_price_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}

