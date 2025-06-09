package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.presentation.components.TransactionItem
import com.example.expensetracker.presentation.viewModel.TransactionViewModel
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.theme.DateUtils

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val filterOptions = listOf("Day", "Week", "Month", "Year")
    var selectedFilter by remember { mutableStateOf("Month") }

    val now = remember { Date() }

    val (startDate, endDate) = remember(selectedFilter, now) {
        when (selectedFilter) {
            "Day" -> DateUtils.getStartOfDay(now) to DateUtils.getEndOfDay(now)
            "Week" -> DateUtils.getStartOfWeek(now) to DateUtils.getEndOfWeek(now)
            "Month" -> DateUtils.getStartOfMonth(now) to DateUtils.getEndOfMonth(now)
            "Year" -> DateUtils.getStartOfYear(now) to DateUtils.getEndOfYear(now)
            else -> DateUtils.getStartOfMonth(now) to DateUtils.getEndOfMonth(now)
        }
    }

    val transactions by viewModel.getTransactionsBetweenDates(startDate, endDate)
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Transaction History", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of Transactions
        if (transactions.isEmpty()) {
            Text("No transactions found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { txn ->
                    TransactionItem(transaction = txn)
                    // On click: could navigate to edit screen with txn.id
                }
            }
        }
    }
}