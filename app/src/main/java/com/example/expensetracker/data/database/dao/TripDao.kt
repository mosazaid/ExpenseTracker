package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetracker.data.database.entities.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun getActiveTrip(): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTripSnapshot(): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): Trip?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeTripById(id: Long): Flow<Trip?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("UPDATE trips SET isActive = 0")
    suspend fun clearActiveTrips()

    @Query("UPDATE trips SET isActive = 1 WHERE id = :tripId")
    suspend fun markTripActive(tripId: Long)

    @Transaction
    suspend fun setActiveTrip(tripId: Long) {
        clearActiveTrips()
        markTripActive(tripId)
    }

    @Query("UPDATE trips SET isActive = 0 WHERE id = :tripId")
    suspend fun deactivateTrip(tripId: Long)
}

