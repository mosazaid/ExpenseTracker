package com.mousa.mizani.presentation.feature.main.more

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.mousa.mizani.R
import com.mousa.mizani.presentation.navigation.Categories
import com.mousa.mizani.presentation.navigation.CurrencyExchangeTracker
import com.mousa.mizani.presentation.navigation.Debts
import com.mousa.mizani.presentation.navigation.DepositVault
import com.mousa.mizani.presentation.navigation.FinancialRunway
import com.mousa.mizani.presentation.navigation.Loans
import com.mousa.mizani.presentation.navigation.MurabahaSimulator
import com.mousa.mizani.presentation.navigation.Onboarding
import com.mousa.mizani.presentation.navigation.PrayerQiblah
import com.mousa.mizani.presentation.navigation.PriceHistoryTracker
import com.mousa.mizani.presentation.navigation.Recurring
import com.mousa.mizani.presentation.navigation.SadaqahJar
import com.mousa.mizani.presentation.navigation.TravelModeTrips
import com.mousa.mizani.presentation.navigation.ZakahCalculator
import com.mousa.mizani.presentation.navigation.Settings as SettingsDestination

private const val MAX_COLUMNS = 3
private const val COMPACT_COLUMNS = 2
private const val MIN_TILE_WIDTH_DP = 100f
private const val LARGE_FONT_SCALE = 1.3f

/** Groups of the More screen, in the order they are shown. */
enum class MoreGroup { EVERYDAY, PLAN_TRAVEL, ISLAMIC, APP }

data class MoreEntry(
    val group: MoreGroup,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val destination: Any
)

/** Everything the More screen links to. The list order is the order on screen. */
object MoreMenu {
    val entries: List<MoreEntry> = listOf(
        MoreEntry(MoreGroup.EVERYDAY, R.string.more_item_recurring, Icons.Outlined.Autorenew, Recurring),
        MoreEntry(MoreGroup.EVERYDAY, R.string.more_item_budgets, Icons.Default.Category, Categories),
        MoreEntry(MoreGroup.EVERYDAY, R.string.more_item_loans, Icons.Default.AccountBalance, Loans),
        MoreEntry(MoreGroup.EVERYDAY, R.string.more_item_debts, Icons.Default.Handshake, Debts),
        MoreEntry(MoreGroup.EVERYDAY, R.string.more_item_vault, Icons.Outlined.Lock, DepositVault),

        MoreEntry(MoreGroup.PLAN_TRAVEL, R.string.more_item_trips, Icons.Default.FlightTakeoff, TravelModeTrips),
        MoreEntry(
            MoreGroup.PLAN_TRAVEL,
            R.string.more_item_exchange,
            Icons.Default.CurrencyExchange,
            CurrencyExchangeTracker
        ),
        MoreEntry(
            MoreGroup.PLAN_TRAVEL,
            R.string.more_item_price_memory,
            Icons.Default.PriceChange,
            PriceHistoryTracker
        ),
        MoreEntry(MoreGroup.PLAN_TRAVEL, R.string.more_item_runway, Icons.Default.Shield, FinancialRunway),

        MoreEntry(MoreGroup.ISLAMIC, R.string.more_item_zakah, Icons.Outlined.VolunteerActivism, ZakahCalculator),
        MoreEntry(MoreGroup.ISLAMIC, R.string.more_item_sadaqah, Icons.Default.Savings, SadaqahJar),
        MoreEntry(MoreGroup.ISLAMIC, R.string.more_item_prayer, Icons.Outlined.Explore, PrayerQiblah),
        MoreEntry(MoreGroup.ISLAMIC, R.string.more_item_murabaha, Icons.Default.Calculate, MurabahaSimulator),

        MoreEntry(MoreGroup.APP, R.string.settings, Icons.Default.Settings, SettingsDestination),
        MoreEntry(MoreGroup.APP, R.string.more_item_help, Icons.AutoMirrored.Outlined.HelpOutline, Onboarding)
    )
}

/** Three tiles per row, or two when text is large or a tile would be too narrow. */
fun moreColumns(availableWidthDp: Float, spacingDp: Float, fontScale: Float): Int {
    val tileWidthDp = (availableWidthDp - (MAX_COLUMNS - 1) * spacingDp) / MAX_COLUMNS
    val isCompact = fontScale >= LARGE_FONT_SCALE || tileWidthDp < MIN_TILE_WIDTH_DP
    return if (isCompact) COMPACT_COLUMNS else MAX_COLUMNS
}
