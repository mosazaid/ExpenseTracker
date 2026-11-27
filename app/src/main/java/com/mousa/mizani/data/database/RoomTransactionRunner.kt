package com.mousa.mizani.data.database

import androidx.room.withTransaction
import com.mousa.mizani.domain.repository.TransactionRunner
import javax.inject.Inject

/** The DAOs all come from this one [AppDatabase], so their writes join its transaction. */
class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.withTransaction { block() }
}
