package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
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
import com.example.expensetracker.presentation.navigation.AppRoutes

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object History : BottomNavItem(AppRoutes.HISTORY, Icons.Default.List, "History")
    object Add : BottomNavItem(AppRoutes.ADD_TRANSACTION, Icons.Default.Add, "Add")
    object Stats : BottomNavItem(AppRoutes.STATISTICS, Icons.Default.BarChart, "Stats")
    object Categories : BottomNavItem(AppRoutes.CATEGORIES, Icons.Default.Category, "Categories")
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
        BottomNavItem.History,
        BottomNavItem.Add,
        BottomNavItem.Stats,
        BottomNavItem.Categories
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentDestination =
                    navController.currentBackStackEntryAsState().value?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.route?.startsWith(item.route) == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.History.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.History.route) { HistoryScreen(navController) }
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
            composable(BottomNavItem.Stats.route) { StatisticsScreen(navController) }
            composable(BottomNavItem.Categories.route) { CategoriesScreen(navController) }
            composable(AppRoutes.SETTINGS) { SettingsScreen(navController) }
        }
    }
}
