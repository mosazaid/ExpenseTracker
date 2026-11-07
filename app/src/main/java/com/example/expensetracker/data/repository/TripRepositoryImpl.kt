package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.dao.TripDao
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.Trip
import com.example.expensetracker.domain.repository.ITripRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val transactionDao: TransactionDao
) : ITripRepository {

    override fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    override fun getActiveTrip(): Flow<Trip?> = tripDao.getActiveTrip()

    override suspend fun getActiveTripSnapshot(): Trip? = tripDao.getActiveTripSnapshot()

    override fun observeTripById(id: Long): Flow<Trip?> = tripDao.observeTripById(id)

    override suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    override suspend fun createTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    override suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    override suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    override suspend fun setActiveTrip(tripId: Long) = tripDao.setActiveTrip(tripId)

    override suspend fun deactivateTrip(tripId: Long) = tripDao.deactivateTrip(tripId)

    override fun getTransactionsForTrip(tripId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForTrip(tripId)

    override suspend fun getTransactionsForTripSnapshot(tripId: Long): List<Transaction> =
        transactionDao.getTransactionsForTripSnapshot(tripId)

    override fun getTripTotalExpense(tripId: Long): Flow<Double> =
        transactionDao.getTripTotalExpense(tripId)
}

