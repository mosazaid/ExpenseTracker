package com.mousa.mizani.domain.repository

/** Runs several writes as one database transaction: all of them are saved, or none. */
interface TransactionRunner {
    /**
     * [block] may only do database work: no file, network or preference writes, and no switch
     * to another dispatcher.
     */
    suspend fun <T> run(block: suspend () -> T): T
}
