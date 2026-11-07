package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.Trip
import kotlinx.coroutines.flow.Flow

interface ITripRepository {
    fun getAllTrips(): Flow<List<Trip>>
    fun getActiveTrip(): Flow<Trip?>
    suspend fun getActiveTripSnapshot(): Trip?
    fun observeTripById(id: Long): Flow<Trip?>
    suspend fun getTripById(id: Long): Trip?
    suspend fun createTrip(trip: Trip): Long
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(trip: Trip)
    suspend fun setActiveTrip(tripId: Long)
    suspend fun deactivateTrip(tripId: Long)

    // Trip-scoped transactions
    fun getTransactionsForTrip(tripId: Long): Flow<List<Transaction>>
    suspend fun getTransactionsForTripSnapshot(tripId: Long): List<Transaction>
    fun getTripTotalExpense(tripId: Long): Flow<Double>
}

