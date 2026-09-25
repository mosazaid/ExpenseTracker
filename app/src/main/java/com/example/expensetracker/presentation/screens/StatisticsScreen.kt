package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.CategoryPieChart
import com.example.expensetracker.presentation.components.StatisticsBarChart
import com.example.expensetracker.presentation.components.ThreeMonthMultiChart
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.StatisticsViewModel
import java.util.*

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.statistics),
                navController = navController
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.periodLabel.isNotBlank()) {
                Text(
                    text = state.periodLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Period Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = filter == selectedFilter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Current Period Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                val remainingIncome = state.totalIncome - state.totalExpense - state.totalWallet
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "${selectedFilter.label} " + stringResource(R.string.monthly_summary),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.income),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+${CurrencyUtils.formatCurrency(state.totalIncome)}",
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.SemiBold,
                            color = FinancePositive
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.expense),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${CurrencyUtils.formatCurrency(state.totalExpense)}",
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.SemiBold,
                            color = FinanceNegative
                        )
                    }
                    if (state.totalWallet != 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.wallet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${CurrencyUtils.formatCurrency(state.totalWallet)}",
                                style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.remaining_income_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = (if (remainingIncome >= 0) "+" else "") + CurrencyUtils.formatCurrency(remainingIncome),
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = if (remainingIncome >= 0) FinancePositive else FinanceNegative
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.balance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = (if (state.balance >= 0) "+" else "") + CurrencyUtils.formatCurrency(state.balance),
                            style = MaterialTheme.typography.bodySmall.withTabularNums(),
                            color = if (state.balance >= 0) MaterialTheme.colorScheme.onSurface else FinanceNegative
                        )
                    }

                    if (state.totalIncome > 0 || state.totalExpense > 0 || state.totalWallet > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
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
}
