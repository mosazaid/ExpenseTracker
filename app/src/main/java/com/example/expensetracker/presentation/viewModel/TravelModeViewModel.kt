package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.Trip
import com.example.expensetracker.domain.repository.ITripRepository
import com.example.expensetracker.domain.trip.TripPacingCalculator
import com.example.expensetracker.domain.trip.TripPacingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TravelModeViewModel @Inject constructor(
    private val tripRepository: ITripRepository
) : ViewModel() {

    val allTrips: StateFlow<List<Trip>> = tripRepository
        .getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTrip: StateFlow<Trip?> = tripRepository
        .getActiveTrip()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Active trip transactions & pacing calculation
    val activeTripPacing: StateFlow<TripPacingResult?> = activeTrip
        .flatMapLatest { trip ->
            if (trip == null) {
                flowOf(null)
            } else {
                tripRepository.getTransactionsForTrip(trip.id).map { txns ->
                    TripPacingCalculator.calculate(trip, txns)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeTripTransactions: StateFlow<List<Transaction>> = activeTrip
        .flatMapLatest { trip ->
            if (trip == null) {
                flowOf(emptyList())
            } else {
                tripRepository.getTransactionsForTrip(trip.id)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createTrip(
        title: String,
        destination: String,
        baseCurrency: String,
        totalBudget: Double,
        startDate: Date,
        endDate: Date,
        isActive: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            val trip = Trip(
                title = title.trim(),
                destination = destination.trim(),
                baseCurrency = baseCurrency,
                homeCurrency = "JOD",
                totalBudget = totalBudget,
                startDate = startDate,
                endDate = endDate,
                isActive = isActive,
                notes = notes?.trim().takeIf { !it.isNullOrBlank() }
            )
            val newId = tripRepository.createTrip(trip)
            if (isActive) {
                tripRepository.setActiveTrip(newId)
            }
        }
    }

    fun setActiveTrip(tripId: Long) {
        viewModelScope.launch {
            tripRepository.setActiveTrip(tripId)
        }
    }

    fun deactivateTrip(tripId: Long) {
        viewModelScope.launch {
            tripRepository.deactivateTrip(tripId)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepository.deleteTrip(trip)
        }
    }
}

