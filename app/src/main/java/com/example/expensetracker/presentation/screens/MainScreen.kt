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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.LoanReminderBottomSheet
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.viewModel.LoansViewModel

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int
) {
    object Overview : BottomNavItem(
        AppRoutes.OVERVIEW,
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard,
        R.string.nav_overview
    )
    object History : BottomNavItem(
        AppRoutes.HISTORY,
        Icons.AutoMirrored.Filled.List,
        Icons.AutoMirrored.Outlined.List,
        R.string.nav_history
    )
    object Stats : BottomNavItem(
        AppRoutes.STATISTICS,
        Icons.Filled.BarChart,
        Icons.Outlined.BarChart,
        R.string.nav_stats
    )
    object More : BottomNavItem(
        AppRoutes.MORE,
        Icons.Filled.MoreHoriz,
        Icons.Outlined.MoreHoriz,
        R.string.nav_more
    )
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
                    val currentDestination =
                        navController.currentBackStackEntryAsState().value?.destination
                    items.forEach { item ->
                        val isSelected = currentDestination?.route == item.route
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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) }
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
            composable(AppRoutes.RECURRING) { RecurringScreen(navController) }
            composable(AppRoutes.DATABASE_BROWSER) { DatabaseBrowserScreen(navController) }
        }
    }
}
