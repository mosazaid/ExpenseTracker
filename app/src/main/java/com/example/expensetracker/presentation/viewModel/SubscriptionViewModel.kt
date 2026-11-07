package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.monetization.PlanTier
import com.example.expensetracker.domain.monetization.ProTrialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val status: ProTrialManager.ProStatus = ProTrialManager.calculateStatus(0L, false),
    val selectedTier: PlanTier = PlanTier.PRO,
    val isAnnualBilling: Boolean = true,
    val feedbackMessage: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        initTrialAndObserve()
    }

    private fun initTrialAndObserve() {
        viewModelScope.launch {
            val savedTimestamp = userPreferences.proTrialStartTimestamp.first()
            if (savedTimestamp == 0L) {
                userPreferences.setProTrialStartTimestamp(System.currentTimeMillis())
            }

            launch {
                userPreferences.proTrialStartTimestamp.collect { startTs ->
                    updateStatus(startTs = startTs)
                }
            }

            launch {
                userPreferences.isProSubscriber.collect { isSub ->
                    updateStatus(isSub = isSub)
                }
            }

            launch {
                userPreferences.isOwnerBypass.collect { isOwner ->
                    updateStatus(isOwner = isOwner)
                }
            }

            launch {
                userPreferences.userPlanTier.collect { tierName ->
                    val tier = try { PlanTier.valueOf(tierName) } catch (_: Exception) { PlanTier.PRO }
                    _uiState.value = _uiState.value.copy(selectedTier = tier)
                }
            }
        }
    }

    private suspend fun updateStatus(
        startTs: Long? = null,
        isSub: Boolean? = null,
        isOwner: Boolean? = null
    ) {
        val currentTs = startTs ?: userPreferences.proTrialStartTimestamp.first()
        val currentSub = isSub ?: userPreferences.isProSubscriber.first()
        val currentOwner = isOwner ?: userPreferences.isOwnerBypass.first()
        val tierStr = userPreferences.userPlanTier.first()
        val tier = try { PlanTier.valueOf(tierStr) } catch (_: Exception) { PlanTier.PRO_PLUS }

        val newStatus = ProTrialManager.calculateStatus(
            trialStartTimestamp = currentTs,
            isSubscribed = currentSub,
            selectedTier = tier,
            isOwnerBypass = currentOwner
        )

        _uiState.value = _uiState.value.copy(status = newStatus)
    }

    fun toggleOwnerBypass() {
        viewModelScope.launch {
            val current = userPreferences.isOwnerBypass.first()
            val newBypass = !current
            userPreferences.setIsOwnerBypass(newBypass)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = if (newBypass) {
                    "👑 Owner Bypass Activated: Permanent Lifetime Pro+ Unlocked!"
                } else {
                    "Owner Bypass Deactivated."
                }
            )
            updateStatus(isOwner = newBypass)
        }
    }

    fun selectBillingCycle(isAnnual: Boolean) {
        _uiState.value = _uiState.value.copy(isAnnualBilling = isAnnual)
    }

    fun selectTier(tier: PlanTier) {
        _uiState.value = _uiState.value.copy(selectedTier = tier)
    }

    fun subscribe(tier: PlanTier) {
        viewModelScope.launch {
            userPreferences.setIsProSubscriber(true)
            userPreferences.setUserPlanTier(tier.name)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Welcome to ExpenseTracker ${tier.displayName}! All features are now unlocked."
            )
            updateStatus()
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            val isSub = userPreferences.isProSubscriber.first()
            if (isSub) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Purchases restored successfully!")
            } else {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Active 90-day device trial is active on this device."
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }
}

