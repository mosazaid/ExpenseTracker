package com.mousa.mizani.domain.usecase

import com.mousa.mizani.domain.repository.TransactionRunner

/**
 * Stands in for a database transaction: it records whether work runs inside one, and a
 * failure puts the recurring rows back and runs [onRollback] for anything else written.
 */
class FakeTransactionRunner(
    private val repository: FakeRecurringRepository? = null,
    private val onRollback: () -> Unit = {}
) : TransactionRunner {
    var isActive = false
        private set
    var commits = 0
        private set
    var rollbacks = 0
        private set

    override suspend fun <T> run(block: suspend () -> T): T {
        val rowsBefore = repository?.items?.toMap()
        isActive = true
        try {
            val result = block()
            commits++
            return result
        } catch (e: Throwable) {
            rollbacks++
            rowsBefore?.let { repository.items.clear(); repository.items.putAll(it) }
            onRollback()
            throw e
        } finally {
            isActive = false
        }
    }
}
