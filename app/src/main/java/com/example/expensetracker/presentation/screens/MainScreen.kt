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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object History : BottomNavItem("history", Icons.Default.List, "History")
    object Add : BottomNavItem("addTransaction", Icons.Default.Add, "Add")
    object Stats : BottomNavItem("statistics", Icons.Default.BarChart, "Stats")
    object Categories : BottomNavItem("categories", Icons.Default.Category, "Categories")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.History.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.History.route) { HistoryScreen(navController) }
            composable(BottomNavItem.Add.route) { AddTransactionScreen(navController) }
            composable(BottomNavItem.Stats.route) { StatisticsScreen(navController) }
            composable(BottomNavItem.Categories.route) { CategoriesScreen(navController) }
        }
    }
}


