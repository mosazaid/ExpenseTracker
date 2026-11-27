package com.mousa.mizani.presentation

import com.mousa.mizani.R
import com.mousa.mizani.presentation.feature.main.more.MoreGroup
import com.mousa.mizani.presentation.feature.main.more.MoreMenu
import com.mousa.mizani.presentation.feature.main.more.moreColumns
import com.mousa.mizani.presentation.navigation.Categories
import com.mousa.mizani.presentation.navigation.CurrencyExchangeTracker
import com.mousa.mizani.presentation.navigation.DatabaseBrowser
import com.mousa.mizani.presentation.navigation.Debts
import com.mousa.mizani.presentation.navigation.DepositVault
import com.mousa.mizani.presentation.navigation.FinancialRunway
import com.mousa.mizani.presentation.navigation.Loans
import com.mousa.mizani.presentation.navigation.MurabahaSimulator
import com.mousa.mizani.presentation.navigation.NotificationSettings
import com.mousa.mizani.presentation.navigation.Onboarding
import com.mousa.mizani.presentation.navigation.PrayerQiblah
import com.mousa.mizani.presentation.navigation.PriceHistoryTracker
import com.mousa.mizani.presentation.navigation.Recurring
import com.mousa.mizani.presentation.navigation.SadaqahJar
import com.mousa.mizani.presentation.navigation.Settings
import com.mousa.mizani.presentation.navigation.SubscriptionPlan
import com.mousa.mizani.presentation.navigation.TravelModeTrips
import com.mousa.mizani.presentation.navigation.ZakahCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MoreMenuTest {

    @Test
    fun `destinations are in spec order per group`() {
        val expected = mapOf(
            MoreGroup.EVERYDAY to listOf<Any>(Recurring, Categories, Loans, Debts, DepositVault),
            MoreGroup.PLAN_TRAVEL to listOf<Any>(
                TravelModeTrips,
                CurrencyExchangeTracker,
                PriceHistoryTracker,
                FinancialRunway
            ),
            MoreGroup.ISLAMIC to listOf<Any>(ZakahCalculator, SadaqahJar, PrayerQiblah, MurabahaSimulator),
            MoreGroup.APP to listOf<Any>(Settings, Onboarding)
        )

        val actual = MoreMenu.entries.groupBy({ it.group }, { it.destination })

        assertEquals(expected, actual)
        assertEquals(MoreGroup.values().toList(), actual.keys.toList())
    }

    @Test
    fun `each label opens its own destination`() {
        val expected = listOf<Pair<Int, Any>>(
            R.string.more_item_recurring to Recurring,
            R.string.more_item_budgets to Categories,
            R.string.more_item_loans to Loans,
            R.string.more_item_debts to Debts,
            R.string.more_item_vault to DepositVault,
            R.string.more_item_trips to TravelModeTrips,
            R.string.more_item_exchange to CurrencyExchangeTracker,
            R.string.more_item_price_memory to PriceHistoryTracker,
            R.string.more_item_runway to FinancialRunway,
            R.string.more_item_zakah to ZakahCalculator,
            R.string.more_item_sadaqah to SadaqahJar,
            R.string.more_item_prayer to PrayerQiblah,
            R.string.more_item_murabaha to MurabahaSimulator,
            R.string.settings to Settings,
            R.string.more_item_help to Onboarding
        )

        val actual = MoreMenu.entries.map { it.labelRes to it.destination }

        assertEquals(expected, actual)
    }

    @Test
    fun `each destination appears once`() {
        val destinations = MoreMenu.entries.map { it.destination }

        assertEquals(15, destinations.size)
        assertEquals(destinations.size, destinations.distinct().size)
    }

    @Test
    fun `notifications, database browser and subscription are absent`() {
        val destinations = MoreMenu.entries.map { it.destination }

        listOf<Any>(NotificationSettings, DatabaseBrowser, SubscriptionPlan).forEach { hidden ->
            assertFalse("$hidden must not be in More", hidden in destinations)
        }
    }

    @Test
    fun `three columns at 328 dp with 8 dp spacing and font 1_0`() {
        assertEquals(3, moreColumns(availableWidthDp = 328f, spacingDp = 8f, fontScale = 1.0f))
    }

    @Test
    fun `two columns at font 1_3`() {
        assertEquals(2, moreColumns(availableWidthDp = 328f, spacingDp = 8f, fontScale = 1.3f))
    }

    @Test
    fun `two columns when a tile would be 99 dp`() {
        assertEquals(2, moreColumns(availableWidthDp = 99f * 3 + 2 * 8f, spacingDp = 8f, fontScale = 1.0f))
    }

    @Test
    fun `three columns at exactly 100 dp`() {
        assertEquals(3, moreColumns(availableWidthDp = 100f * 3 + 2 * 8f, spacingDp = 8f, fontScale = 1.0f))
    }
}
