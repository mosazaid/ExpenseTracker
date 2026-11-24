package com.mousa.mizani.presentation.feature.main

import com.mousa.mizani.presentation.feature.add_transaction.AddTransactionScreen
import com.mousa.mizani.presentation.feature.alerts.AlertsScreen
import com.mousa.mizani.presentation.feature.categories.CategoriesScreen
import com.mousa.mizani.presentation.feature.currency.CurrencyExchangeScreen
import com.mousa.mizani.presentation.feature.debug.DatabaseBrowserScreen
import com.mousa.mizani.presentation.feature.deposit.DepositScreen
import com.mousa.mizani.presentation.feature.financial_tools.FinancialRunwayScreen
import com.mousa.mizani.presentation.feature.financial_tools.PriceHistoryScreen
import com.mousa.mizani.presentation.feature.history.HistoryScreen
import com.mousa.mizani.presentation.feature.islamic.MurabahaSimulatorScreen
import com.mousa.mizani.presentation.feature.islamic.PrayerQiblahScreen
import com.mousa.mizani.presentation.feature.islamic.SadaqahJarScreen
import com.mousa.mizani.presentation.feature.islamic.ZakahCalculatorScreen
import com.mousa.mizani.presentation.feature.loans.DebtsScreen
import com.mousa.mizani.presentation.feature.loans.LoansScreen
import com.mousa.mizani.presentation.feature.onboarding.OnboardingScreen
import com.mousa.mizani.presentation.feature.overview.OverviewScreen
import com.mousa.mizani.presentation.feature.recurring.RecurringScreen
import com.mousa.mizani.presentation.feature.settings.NotificationSettingsScreen
import com.mousa.mizani.presentation.feature.settings.SettingsScreen
import com.mousa.mizani.presentation.feature.statistics.StatisticsScreen
import com.mousa.mizani.presentation.feature.subscription.SubscriptionScreen
import com.mousa.mizani.presentation.feature.transfer.TransferScreen
import com.mousa.mizani.presentation.feature.trips.TravelModeScreen
import com.mousa.mizani.presentation.feature.wallet.WalletScreen
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mousa.mizani.R
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.presentation.feature.loans.components.LoanReminderBottomSheet
import kotlinx.coroutines.launch
import com.mousa.mizani.presentation.navigation.AddTransaction
import com.mousa.mizani.presentation.navigation.Alerts
import com.mousa.mizani.presentation.navigation.Categories
import com.mousa.mizani.presentation.navigation.DatabaseBrowser
import com.mousa.mizani.presentation.navigation.Debts
import com.mousa.mizani.presentation.navigation.EditTransfer
import com.mousa.mizani.presentation.navigation.EditWallet
import com.mousa.mizani.presentation.navigation.History
import com.mousa.mizani.presentation.navigation.Loans
import com.mousa.mizani.presentation.navigation.More
import com.mousa.mizani.presentation.navigation.Onboarding
import com.mousa.mizani.presentation.navigation.Overview
import com.mousa.mizani.presentation.navigation.Recurring
import com.mousa.mizani.presentation.navigation.Settings
import com.mousa.mizani.presentation.navigation.NotificationSettings
import com.mousa.mizani.presentation.navigation.ZakahCalculator
import com.mousa.mizani.presentation.navigation.PrayerQiblah
import com.mousa.mizani.presentation.navigation.DepositVault
import com.mousa.mizani.presentation.navigation.CurrencyExchangeTracker
import com.mousa.mizani.presentation.navigation.TravelModeTrips
import com.mousa.mizani.presentation.navigation.PriceHistoryTracker
import com.mousa.mizani.presentation.navigation.SadaqahJar
import com.mousa.mizani.presentation.navigation.FinancialRunway
import com.mousa.mizani.presentation.navigation.MurabahaSimulator
import com.mousa.mizani.presentation.navigation.SubscriptionPlan
import com.mousa.mizani.presentation.navigation.Statistics
import com.mousa.mizani.presentation.navigation.Transfer
import com.mousa.mizani.presentation.navigation.Wallet
import com.mousa.mizani.presentation.feature.loans.LoansViewModel
import java.time.LocalDate

sealed class BottomNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int
) {
    object Overview : BottomNavItem(
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard,
        R.string.nav_overview
    )
    object History : BottomNavItem(
        Icons.AutoMirrored.Filled.List,
        Icons.AutoMirrored.Outlined.List,
        R.string.nav_history
    )
    object Stats : BottomNavItem(
        Icons.Filled.BarChart,
        Icons.Outlined.BarChart,
        R.string.nav_stats
    )
    object More : BottomNavItem(
        Icons.Filled.MoreHoriz,
        Icons.Outlined.MoreHoriz,
        R.string.nav_more
    )
}

// Maps each BottomNavItem to its type-safe destination object
private val bottomNavDestinations = listOf(
    BottomNavItem.Overview to com.mousa.mizani.presentation.navigation.Overview,
    BottomNavItem.History  to com.mousa.mizani.presentation.navigation.History,
    BottomNavItem.Stats    to Statistics,
    BottomNavItem.More     to com.mousa.mizani.presentation.navigation.More
)

@Composable
fun MainScreen(
    initialRecurringId: Long? = null,
    initialNavigateTo: String? = null,
    onNavigateHandled: () -> Unit = {},
    loansViewModel: LoansViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialRecurringId) {
        if (initialRecurringId != null) {
            navController.navigate(AddTransaction(recurringId = initialRecurringId))
        }
    }

    LaunchedEffect(initialNavigateTo) {
        if (!initialNavigateTo.isNullOrBlank()) {
            when (initialNavigateTo) {
                "add_expense", "add_transaction" -> navController.navigate(AddTransaction())
                "categories" -> navController.navigate(Categories)
                "loans" -> navController.navigate(Loans)
                "prayer_qiblah" -> navController.navigate(PrayerQiblah)
                "history" -> navController.navigate(History)
                "stats" -> navController.navigate(Statistics)
            }
            onNavigateHandled()
        }
    }

    val currentBackStack by navController.currentBackStackEntryAsState()
    val isDashboardTab = currentBackStack?.destination?.let { dest ->
        dest.hasRoute<Overview>() ||
            dest.hasRoute<History>() ||
            dest.hasRoute<Statistics>() ||
            dest.hasRoute<More>()
    } ?: false

    // Loan reminder bottom sheet — shown once per day for unpaid loans
    val monthlyLoanItems by loansViewModel.monthlyItems.collectAsState()
    val unpaidLoans = remember(monthlyLoanItems) { monthlyLoanItems.filter { !it.payment.isPaid } }
    var hasDismissedLoanSheetSession by rememberSaveable { mutableStateOf(false) }
    val lastShownDate by loansViewModel.lastLoanSheetDate.collectAsState()
    // remember { } is correct here: we capture today once per composition lifetime
    val todayDateStr = remember { LocalDate.now().toString() }
    val shouldShowToday = lastShownDate != todayDateStr
    val isOnOverview = currentBackStack?.destination?.hasRoute<Overview>() == true

    if (unpaidLoans.isNotEmpty() && shouldShowToday && !hasDismissedLoanSheetSession && isOnOverview) {
        LoanReminderBottomSheet(
            unpaidLoans = unpaidLoans,
            onPayLoansClick = { amounts, onResult ->
                coroutineScope.launch {
                    var firstError: String? = null
                    for (item in unpaidLoans) {
                        val amt = amounts[item.payment.id] ?: item.payment.amount
                        val targetAccount = item.loanConfig.accountType
                        val balance = loansViewModel.getAvailableBalance(targetAccount)
                        if (amt > balance) {
                            val accName = if (targetAccount == AccountType.BANK) "Bank" else "Cash"
                            firstError = "Insufficient $accName balance for ${item.loanConfig.name}. Available: ${CurrencyUtils.formatCurrency(balance)}, Required: ${CurrencyUtils.formatCurrency(amt)}"
                            break
                        }
                    }

                    if (firstError != null) {
                        onResult(false, firstError)
                        return@launch
                    }

                    // All balances sufficient, perform deductions
                    for (item in unpaidLoans) {
                        val amt = amounts[item.payment.id] ?: item.payment.amount
                        loansViewModel.deductAndPayLoan(item.payment.id, customAmount = amt)
                    }

                    loansViewModel.markLoanSheetShownToday()
                    hasDismissedLoanSheetSession = true
                    onResult(true, null)
                }
            },
            onDismissRequest = {
                loansViewModel.markLoanSheetShownToday()
                hasDismissedLoanSheetSession = true
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (isDashboardTab) {
                FloatingActionButton(
                    onClick = { navController.navigate(AddTransaction()) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_add),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (isDashboardTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    val currentDestination = currentBackStack?.destination
                    bottomNavDestinations.forEach { (item, destination) ->
                        val isSelected = currentDestination?.hasRoute(destination::class) == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.labelRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                if (!isSelected) {
                                    if (destination is com.mousa.mizani.presentation.navigation.Overview) {
                                        val popped = navController.popBackStack<Overview>(inclusive = false)
                                        if (!popped) {
                                            navController.navigate(Overview) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(destination) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Overview,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            // ── Bottom-tab screens ──────────────────────────────────────────
            composable<Overview>    { OverviewScreen(navController) }
            composable<History>     { HistoryScreen(navController) }
            composable<Statistics>  { StatisticsScreen(navController) }
            composable<More>        { MoreScreen(navController) }

            // ── Secondary screens ───────────────────────────────────────────
            composable<Loans>          { LoansScreen(navController) }
            composable<Alerts>         { AlertsScreen(navController) }
            composable<Debts>          { DebtsScreen(navController) }
            composable<Transfer>       { TransferScreen(navController) }
            composable<Wallet>         { WalletScreen(navController) }
            composable<Categories>     { CategoriesScreen(navController) }
            composable<Settings>       { SettingsScreen(navController) }
            composable<NotificationSettings> { NotificationSettingsScreen(navController) }
            composable<Recurring>      { RecurringScreen(navController) }
            composable<DatabaseBrowser> { DatabaseBrowserScreen(navController) }
            composable<Onboarding>     { OnboardingScreen(onFinish = { navController.popBackStack() }) }
            composable<ZakahCalculator> { ZakahCalculatorScreen(navController) }
            composable<PrayerQiblah>    { PrayerQiblahScreen(navController) }
            composable<DepositVault>    { DepositScreen(navController) }
            composable<CurrencyExchangeTracker> { CurrencyExchangeScreen(navController) }
            composable<TravelModeTrips> { TravelModeScreen(navController) }
            composable<PriceHistoryTracker> { PriceHistoryScreen(navController) }
            composable<SadaqahJar> { SadaqahJarScreen(navController) }
            composable<FinancialRunway> { FinancialRunwayScreen(navController) }
            composable<MurabahaSimulator> { MurabahaSimulatorScreen(navController) }
            composable<SubscriptionPlan> { SubscriptionScreen(navController) }

            // ── Parameterised screens ───────────────────────────────────────
            composable<AddTransaction> { backStackEntry ->
                val route = backStackEntry.toRoute<AddTransaction>()
                AddTransactionScreen(
                    navController = navController,
                    transactionId = route.transactionId.takeIf { it != -1L },
                    recurringId = route.recurringId.takeIf { it != -1L },
                    initialAccountType = route.accountType?.let { runCatching { com.mousa.mizani.data.database.entities.AccountType.valueOf(it) }.getOrNull() }
                )
            }
            composable<EditTransfer> { backStackEntry ->
                val route = backStackEntry.toRoute<EditTransfer>()
                TransferScreen(navController, transactionId = route.transactionId)
            }
            composable<EditWallet> { backStackEntry ->
                val route = backStackEntry.toRoute<EditWallet>()
                WalletScreen(navController, transactionId = route.transactionId)
            }
        }
    }
}
