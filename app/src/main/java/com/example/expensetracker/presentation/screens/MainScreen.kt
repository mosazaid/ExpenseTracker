package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.LoanReminderBottomSheet
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.viewModel.LoansViewModel

sealed class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int) {
    object Overview : BottomNavItem(AppRoutes.OVERVIEW, Icons.Default.Dashboard, R.string.nav_overview)
    object History : BottomNavItem(AppRoutes.HISTORY, Icons.AutoMirrored.Filled.List, R.string.nav_history)
    object Stats : BottomNavItem(AppRoutes.STATISTICS, Icons.Default.BarChart, R.string.nav_stats)
    object More : BottomNavItem(AppRoutes.MORE, Icons.Default.MoreHoriz, R.string.nav_more)
}

@Composable
fun MainScreen(
    initialRecurringId: Long? = null,
    loansViewModel: LoansViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(initialRecurringId) {
        if (initialRecurringId != null) {
            navController.navigate(AppRoutes.addTransactionRoute(recurringId = initialRecurringId))
        }
    }

    val items = listOf(
        BottomNavItem.Overview,
        BottomNavItem.History,
        BottomNavItem.Stats,
        BottomNavItem.More
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isDashboardTab = currentRoute == AppRoutes.OVERVIEW ||
        currentRoute == AppRoutes.HISTORY ||
        currentRoute == AppRoutes.STATISTICS ||
        currentRoute == AppRoutes.MORE

    // Loan reminder bottom sheet on app open if there are unpaid loans (shown once per day)
    val monthlyLoanItems by loansViewModel.monthlyItems.collectAsState()
    val unpaidLoans = remember(monthlyLoanItems) { monthlyLoanItems.filter { !it.payment.isPaid } }
    var hasDismissedLoanSheetSession by rememberSaveable { mutableStateOf(false) }
    val lastShownDate by loansViewModel.lastLoanSheetDate.collectAsState()
    val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val shouldShowToday = lastShownDate != todayDateStr

    if (unpaidLoans.isNotEmpty() && shouldShowToday && !hasDismissedLoanSheetSession && currentRoute == AppRoutes.OVERVIEW) {
        LoanReminderBottomSheet(
            unpaidLoans = unpaidLoans,
            onPayLoansClick = {
                loansViewModel.markLoanSheetShownToday()
                hasDismissedLoanSheetSession = true
                navController.navigate(AppRoutes.LOANS)
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
                    onClick = { navController.navigate(AppRoutes.addTransactionRoute()) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_add)
                    )
                }
            }
        },
        bottomBar = {
            if (isDashboardTab) {
                NavigationBar {
                    val currentDestination =
                        navController.currentBackStackEntryAsState().value?.destination
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.route == item.route,
                            onClick = {
                                if (currentDestination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
            startDestination = AppRoutes.OVERVIEW,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoutes.OVERVIEW) { OverviewScreen(navController) }
            composable(AppRoutes.HISTORY) { HistoryScreen(navController) }
            composable(AppRoutes.STATISTICS) { StatisticsScreen(navController) }
            composable(AppRoutes.MORE) { MoreScreen(navController) }
            composable(AppRoutes.LOANS) { LoansScreen(navController) }
            composable(AppRoutes.ALERTS) { AlertsScreen(navController) }
            composable(AppRoutes.DEBTS) { DebtsScreen(navController) }
            composable(
                route = AppRoutes.ADD_TRANSACTION_WITH_ARGS,
                arguments = listOf(
                    navArgument(AppRoutes.ADD_TRANSACTION_ARG_TRANSACTION_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument(AppRoutes.ADD_TRANSACTION_ARG_RECURRING_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments
                    ?.getLong(AppRoutes.ADD_TRANSACTION_ARG_TRANSACTION_ID) ?: -1L
                val recurringId = backStackEntry.arguments
                    ?.getLong(AppRoutes.ADD_TRANSACTION_ARG_RECURRING_ID) ?: -1L
                AddTransactionScreen(
                    navController = navController,
                    transactionId = if (transactionId == -1L) null else transactionId,
                    recurringId = if (recurringId == -1L) null else recurringId
                )
            }
            composable(AppRoutes.TRANSFER) { TransferScreen(navController) }
            composable(AppRoutes.WALLET) { WalletScreen(navController) }
            composable(
                route = AppRoutes.EDIT_TRANSFER,
                arguments = listOf(navArgument(AppRoutes.EDIT_TRANSFER_ARG) { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments
                    ?.getLong(AppRoutes.EDIT_TRANSFER_ARG) ?: return@composable
                TransferScreen(navController, transactionId = transactionId)
            }
            composable(
                route = AppRoutes.EDIT_WALLET,
                arguments = listOf(navArgument(AppRoutes.EDIT_WALLET_ARG) { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments
                    ?.getLong(AppRoutes.EDIT_WALLET_ARG) ?: return@composable
                WalletScreen(navController, transactionId = transactionId)
            }
            composable(AppRoutes.CATEGORIES) { CategoriesScreen(navController) }
            composable(AppRoutes.SETTINGS) { SettingsScreen(navController) }
            composable(AppRoutes.DATABASE_BROWSER) { DatabaseBrowserScreen(navController) }
        }
    }
}
