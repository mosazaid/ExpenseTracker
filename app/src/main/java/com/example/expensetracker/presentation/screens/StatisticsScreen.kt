package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.navigation.AppRoutes
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleLarge)
                if (state.periodLabel.isNotBlank()) {
                    Text(
                        state.periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            val remainingIncome = state.totalIncome - state.totalExpense - state.totalWallet
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.income), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "+${CurrencyUtils.formatCurrency(state.totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.expense), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "-${CurrencyUtils.formatCurrency(state.totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (state.totalWallet != 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.wallet), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "-${CurrencyUtils.formatCurrency(state.totalWallet)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Income", style = MaterialTheme.typography.titleMedium)
                    Text(
                        (if (remainingIncome >= 0) "+" else "") + CurrencyUtils.formatCurrency(remainingIncome),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (remainingIncome >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.balance), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        (if (state.balance >= 0) "+" else "") + CurrencyUtils.formatCurrency(state.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.balance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.totalIncome == 0.0 && state.totalExpense == 0.0 && state.totalWallet == 0.0) {
            Text(stringResource(R.string.no_transactions), style = MaterialTheme.typography.bodyMedium)
        } else {
            StatisticsBarChart(
                income = state.totalIncome.toFloat(),
                expense = state.totalExpense.toFloat(),
                wallet = state.totalWallet.toFloat()
            )
        }
    }
}
