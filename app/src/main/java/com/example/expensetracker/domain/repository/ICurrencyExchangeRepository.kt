package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.CurrencyExchange
import kotlinx.coroutines.flow.Flow

interface ICurrencyExchangeRepository {
    fun getAllExchanges(): Flow<List<CurrencyExchange>>
    fun getRecentExchanges(limit: Int = 20): Flow<List<CurrencyExchange>>
    fun getExchangesBetween(c1: String, c2: String): Flow<List<CurrencyExchange>>
    suspend fun getExchangeById(id: Long): CurrencyExchange?
    suspend fun insertExchange(exchange: CurrencyExchange): Long
    suspend fun updateExchange(exchange: CurrencyExchange)
    suspend fun deleteExchange(exchange: CurrencyExchange)
    suspend fun deleteExchangeById(id: Long)
}

