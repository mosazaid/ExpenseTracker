package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.CurrencyExchange
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyExchangeDao {

    @Query("SELECT * FROM currency_exchanges ORDER BY date DESC")
    fun getAllExchanges(): Flow<List<CurrencyExchange>>

    @Query("SELECT * FROM currency_exchanges WHERE id = :id")
    suspend fun getExchangeById(id: Long): CurrencyExchange?

    @Query("SELECT * FROM currency_exchanges ORDER BY date DESC LIMIT :limit")
    fun getRecentExchanges(limit: Int = 20): Flow<List<CurrencyExchange>>

    @Query("""
        SELECT * FROM currency_exchanges 
        WHERE (soldCurrency = :c1 AND boughtCurrency = :c2) 
           OR (soldCurrency = :c2 AND boughtCurrency = :c1) 
        ORDER BY date DESC
    """)
    fun getExchangesBetweenCurrencies(c1: String, c2: String): Flow<List<CurrencyExchange>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchange(exchange: CurrencyExchange): Long

    @Update
    suspend fun updateExchange(exchange: CurrencyExchange)

    @Delete
    suspend fun deleteExchange(exchange: CurrencyExchange)

    @Query("DELETE FROM currency_exchanges WHERE id = :id")
    suspend fun deleteExchangeById(id: Long)
}

