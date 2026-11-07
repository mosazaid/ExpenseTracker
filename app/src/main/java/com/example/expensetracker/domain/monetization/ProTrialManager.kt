package com.example.expensetracker.domain.monetization

import java.util.concurrent.TimeUnit

object ProTrialManager {
    const val TRIAL_DURATION_DAYS = 90
    val TRIAL_DURATION_MILLIS = TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS.toLong())

    data class ProStatus(
        val isSubscribed: Boolean,
        val isTrialActive: Boolean,
        val effectiveTier: PlanTier,
        val trialDaysRemaining: Int,
        val trialPercentageElapsed: Float,
        val trialExpired: Boolean,
        val isOwnerBypass: Boolean = false
    )

    fun calculateStatus(
        trialStartTimestamp: Long,
        isSubscribed: Boolean,
        selectedTier: PlanTier = PlanTier.PRO_PLUS,
        currentTimeMillis: Long = System.currentTimeMillis(),
        isOwnerBypass: Boolean = false
    ): ProStatus {
        if (isOwnerBypass) {
            return ProStatus(
                isSubscribed = true,
                isTrialActive = false,
                effectiveTier = PlanTier.PRO_PLUS,
                trialDaysRemaining = 9999,
                trialPercentageElapsed = 1f,
                trialExpired = false,
                isOwnerBypass = true
            )
        }

        if (isSubscribed) {
            return ProStatus(
                isSubscribed = true,
                isTrialActive = false,
                effectiveTier = selectedTier,
                trialDaysRemaining = 0,
                trialPercentageElapsed = 1f,
                trialExpired = false,
                isOwnerBypass = false
            )
        }

        if (trialStartTimestamp <= 0L) {
            // First run, full trial available
            return ProStatus(
                isSubscribed = false,
                isTrialActive = true,
                effectiveTier = PlanTier.PRO_PLUS,
                trialDaysRemaining = TRIAL_DURATION_DAYS,
                trialPercentageElapsed = 0f,
                trialExpired = false,
                isOwnerBypass = false
            )
        }

        val elapsedMillis = (currentTimeMillis - trialStartTimestamp).coerceAtLeast(0L)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
        val daysRemaining = (TRIAL_DURATION_DAYS - elapsedDays).coerceAtLeast(0)
        val isTrialActive = daysRemaining > 0
        val percentage = (elapsedMillis.toFloat() / TRIAL_DURATION_MILLIS.toFloat()).coerceIn(0f, 1f)

        return ProStatus(
            isSubscribed = false,
            isTrialActive = isTrialActive,
            effectiveTier = if (isTrialActive) PlanTier.PRO_PLUS else PlanTier.FREE,
            trialDaysRemaining = daysRemaining,
            trialPercentageElapsed = percentage,
            trialExpired = !isTrialActive,
            isOwnerBypass = false
        )
    }

    fun isFeatureUnlocked(feature: FeatureGate, status: ProStatus): Boolean {
        if (status.isOwnerBypass || status.isSubscribed || status.isTrialActive) return true
        return status.effectiveTier >= feature.requiredTier
    }
}

