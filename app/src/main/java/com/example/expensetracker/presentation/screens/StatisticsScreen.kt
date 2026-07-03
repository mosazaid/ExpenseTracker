package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.viewModel.StatisticsViewModel
import com.example.expensetracker.presentation.components.StatisticsBarChart
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.theme.CurrencyUtils

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.statisticsState.collectAsState()
    val monthMode by viewModel.monthMode.collectAsState()

    val filterOptions = HistoryPeriod.entries.toList()
    var selectedFilter by remember { mutableStateOf(HistoryPeriod.MONTH) }
    val now = remember { Date() }

    LaunchedEffect(selectedFilter, monthMode) {
        viewModel.loadStatisticsForPeriod(selectedFilter, now)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Statistics", style = MaterialTheme.typography.titleLarge)
        if (state.periodLabel.isNotBlank()) {
            Text(
                state.periodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Income: ${CurrencyUtils.formatCurrency(state.totalIncome)}",
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Expense: ${CurrencyUtils.formatCurrency(state.totalExpense)}",
                    color = MaterialTheme.colorScheme.error
                )
                Text("Balance: ${CurrencyUtils.formatCurrency(state.balance)}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.totalIncome == 0.0 && state.totalExpense == 0.0) {
            Text("No transactions found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            StatisticsBarChart(
                income = state.totalIncome.toFloat(),
                expense = state.totalExpense.toFloat()
            )
        }
    }
}
