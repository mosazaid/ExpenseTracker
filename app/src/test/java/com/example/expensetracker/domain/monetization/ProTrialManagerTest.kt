package com.example.expensetracker.domain.monetization

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProTrialManagerTest {

    private val now = 1_700_000_000_000L // arbitrary fixed time

    @Test
    fun freshInstall_givesFull90DayTrial() {
        val status = ProTrialManager.calculateStatus(
            trialStartTimestamp = 0L,
            isSubscribed = false,
            currentTimeMillis = now
        )

        assertTrue(status.isTrialActive)
        assertFalse(status.isSubscribed)
        assertEquals(90, status.trialDaysRemaining)
        assertEquals(0f, status.trialPercentageElapsed, 0.001f)
        assertEquals(PlanTier.PRO_PLUS, status.effectiveTier)
        assertFalse(status.trialExpired)
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.DEPOSIT_VAULT, status))
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.MULTI_CURRENCY, status))
    }

    @Test
    fun midTrial_calculatesRemainingDaysAccurately() {
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val status = ProTrialManager.calculateStatus(
            trialStartTimestamp = thirtyDaysAgo,
            isSubscribed = false,
            currentTimeMillis = now
        )

        assertTrue(status.isTrialActive)
        assertEquals(60, status.trialDaysRemaining)
        assertEquals(30f / 90f, status.trialPercentageElapsed, 0.02f)
        assertEquals(PlanTier.PRO_PLUS, status.effectiveTier)
    }

    @Test
    fun trialExpired_revertsToFreeTierAndLocksProFeatures() {
        val ninetyOneDaysAgo = now - TimeUnit.DAYS.toMillis(91)
        val status = ProTrialManager.calculateStatus(
            trialStartTimestamp = ninetyOneDaysAgo,
            isSubscribed = false,
            currentTimeMillis = now
        )

        assertFalse(status.isTrialActive)
        assertTrue(status.trialExpired)
        assertEquals(0, status.trialDaysRemaining)
        assertEquals(PlanTier.FREE, status.effectiveTier)

        assertFalse(ProTrialManager.isFeatureUnlocked(FeatureGate.DEPOSIT_VAULT, status))
        assertFalse(ProTrialManager.isFeatureUnlocked(FeatureGate.MULTI_CURRENCY, status))
    }

    @Test
    fun activeSubscriber_unlocksFeaturesRegardlessOfTrial() {
        val oneHundredDaysAgo = now - TimeUnit.DAYS.toMillis(100)
        val status = ProTrialManager.calculateStatus(
            trialStartTimestamp = oneHundredDaysAgo,
            isSubscribed = true,
            selectedTier = PlanTier.PRO_PLUS,
            currentTimeMillis = now
        )

        assertTrue(status.isSubscribed)
        assertFalse(status.isTrialActive)
        assertFalse(status.trialExpired)
        assertEquals(PlanTier.PRO_PLUS, status.effectiveTier)
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.MULTI_CURRENCY, status))
    }

    @Test
    fun ownerBypass_unlocksAllFeaturesPermanently() {
        val twoYearsAgo = now - TimeUnit.DAYS.toMillis(730)
        val status = ProTrialManager.calculateStatus(
            trialStartTimestamp = twoYearsAgo,
            isSubscribed = false,
            currentTimeMillis = now,
            isOwnerBypass = true
        )

        assertTrue(status.isOwnerBypass)
        assertTrue(status.isSubscribed)
        assertEquals(PlanTier.PRO_PLUS, status.effectiveTier)
        assertFalse(status.trialExpired)
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.DEPOSIT_VAULT, status))
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.MULTI_CURRENCY, status))
        assertTrue(ProTrialManager.isFeatureUnlocked(FeatureGate.INFLATION_TRACKER, status))
    }
}

