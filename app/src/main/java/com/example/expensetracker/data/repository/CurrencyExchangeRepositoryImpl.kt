package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.CurrencyExchangeDao
import com.example.expensetracker.data.database.entities.CurrencyExchange
import com.example.expensetracker.domain.repository.ICurrencyExchangeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyExchangeRepositoryImpl @Inject constructor(
    private val currencyExchangeDao: CurrencyExchangeDao
) : ICurrencyExchangeRepository {

    override fun getAllExchanges(): Flow<List<CurrencyExchange>> {
        return currencyExchangeDao.getAllExchanges()
    }

    override fun getRecentExchanges(limit: Int): Flow<List<CurrencyExchange>> {
        return currencyExchangeDao.getRecentExchanges(limit)
    }

    override fun getExchangesBetween(c1: String, c2: String): Flow<List<CurrencyExchange>> {
        return currencyExchangeDao.getExchangesBetweenCurrencies(c1, c2)
    }

    override suspend fun getExchangeById(id: Long): CurrencyExchange? {
        return currencyExchangeDao.getExchangeById(id)
    }

    override suspend fun insertExchange(exchange: CurrencyExchange): Long {
        return currencyExchangeDao.insertExchange(exchange)
    }

    override suspend fun updateExchange(exchange: CurrencyExchange) {
        currencyExchangeDao.updateExchange(exchange)
    }

    override suspend fun deleteExchange(exchange: CurrencyExchange) {
        currencyExchangeDao.deleteExchange(exchange)
    }

    override suspend fun deleteExchangeById(id: Long) {
        currencyExchangeDao.deleteExchangeById(id)
    }
}

