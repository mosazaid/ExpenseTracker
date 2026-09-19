package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.components.CategoryPieChart
import com.example.expensetracker.presentation.components.StatisticsBarChart
import com.example.expensetracker.presentation.components.ThreeMonthMultiChart
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.StatisticsViewModel
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel

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
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedFilter, monthMode) {
        viewModel.loadStatisticsForPeriod(selectedFilter, now)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.statistics),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        // Period Filter Chips
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

        // Current Period Summary Card
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
                Text(
                    text = "${selectedFilter.label} Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
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
                    Text(
                        stringResource(R.string.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        (if (state.balance >= 0) "+" else "") + CurrencyUtils.formatCurrency(state.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.balance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }

                if (state.totalIncome > 0 || state.totalExpense > 0 || state.totalWallet > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StatisticsBarChart(
                        income = state.totalIncome.toFloat(),
                        expense = state.totalExpense.toFloat(),
                        wallet = state.totalWallet.toFloat()
                    )
                }
            }
        }

        // ── 3-Month Multi-Chart (Grouped Bar + Trend Line)
        ThreeMonthMultiChart(stats = state.threeMonthStats)

        // ── 3-Month Category Expense Comparison Donut/Pie Chart
        CategoryPieChart(stats = state.threeMonthStats)

        Spacer(modifier = Modifier.height(72.dp))
    }
}
