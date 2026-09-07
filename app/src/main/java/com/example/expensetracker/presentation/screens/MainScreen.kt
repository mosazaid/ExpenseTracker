package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.expensetracker.R
import com.example.expensetracker.presentation.navigation.AppRoutes

sealed class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int) {
    object Overview : BottomNavItem(AppRoutes.OVERVIEW, Icons.Default.Dashboard, R.string.nav_overview)
    object History : BottomNavItem(AppRoutes.HISTORY, Icons.Default.List, R.string.nav_history)
    object Stats : BottomNavItem(AppRoutes.STATISTICS, Icons.Default.BarChart, R.string.nav_stats)
}

@Composable
fun MainScreen(initialRecurringId: Long? = null) {
    val navController = rememberNavController()

    LaunchedEffect(initialRecurringId) {
        if (initialRecurringId != null) {
            navController.navigate(AppRoutes.addTransactionRoute(recurringId = initialRecurringId))
        }
    }

    val items = listOf(
        BottomNavItem.Overview,
        BottomNavItem.History,
        BottomNavItem.Stats
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isDashboardTab = currentRoute == AppRoutes.OVERVIEW ||
        currentRoute == AppRoutes.HISTORY ||
        currentRoute == AppRoutes.STATISTICS

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
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
