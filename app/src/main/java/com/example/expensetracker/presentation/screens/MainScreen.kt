package com.example.expensetracker.presentation.screens

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
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.LoanReminderBottomSheet
import com.example.expensetracker.presentation.navigation.AddTransaction
import com.example.expensetracker.presentation.navigation.Alerts
import com.example.expensetracker.presentation.navigation.Categories
import com.example.expensetracker.presentation.navigation.DatabaseBrowser
import com.example.expensetracker.presentation.navigation.Debts
import com.example.expensetracker.presentation.navigation.EditTransfer
import com.example.expensetracker.presentation.navigation.EditWallet
import com.example.expensetracker.presentation.navigation.History
import com.example.expensetracker.presentation.navigation.Loans
import com.example.expensetracker.presentation.navigation.More
import com.example.expensetracker.presentation.navigation.Onboarding
import com.example.expensetracker.presentation.navigation.Overview
import com.example.expensetracker.presentation.navigation.Recurring
import com.example.expensetracker.presentation.navigation.Settings
import com.example.expensetracker.presentation.navigation.Statistics
import com.example.expensetracker.presentation.navigation.Transfer
import com.example.expensetracker.presentation.navigation.Wallet
import com.example.expensetracker.presentation.viewModel.LoansViewModel
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
    BottomNavItem.Overview to com.example.expensetracker.presentation.navigation.Overview,
    BottomNavItem.History  to com.example.expensetracker.presentation.navigation.History,
    BottomNavItem.Stats    to Statistics,
    BottomNavItem.More     to com.example.expensetracker.presentation.navigation.More
)

@Composable
fun MainScreen(
    initialRecurringId: Long? = null,
    loansViewModel: LoansViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(initialRecurringId) {
        if (initialRecurringId != null) {
            navController.navigate(AddTransaction(recurringId = initialRecurringId))
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
            onPayLoansClick = {
                loansViewModel.markLoanSheetShownToday()
                hasDismissedLoanSheetSession = true
                navController.navigate(Loans)
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
                                    if (destination is com.example.expensetracker.presentation.navigation.Overview) {
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
            composable<Recurring>      { RecurringScreen(navController) }
            composable<DatabaseBrowser> { DatabaseBrowserScreen(navController) }
            composable<Onboarding>     { OnboardingScreen(onFinish = { navController.popBackStack() }) }

            // ── Parameterised screens ───────────────────────────────────────
            composable<AddTransaction> { backStackEntry ->
                val route = backStackEntry.toRoute<AddTransaction>()
                AddTransactionScreen(
                    navController = navController,
                    transactionId = route.transactionId.takeIf { it != -1L },
                    recurringId = route.recurringId.takeIf { it != -1L }
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
