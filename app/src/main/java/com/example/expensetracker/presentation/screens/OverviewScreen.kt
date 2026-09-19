package com.example.expensetracker.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.presentation.components.AccountBalanceCards
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val monthMode by viewModel.monthMode.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var monthSummary by remember { mutableStateOf(HistoryViewModel.MonthSummaryUiState()) }
    var visibleReminder by remember { mutableStateOf<RecurringTransaction?>(null) }
    var summaryExpanded by remember { mutableStateOf(true) }
    var walletListExpanded by remember { mutableStateOf(false) }

    var salaryWalletYearSummary by remember {
        mutableStateOf<List<com.example.expensetracker.domain.SalaryWalletPeriodSummary>>(emptyList())
    }

    LaunchedEffect(monthMode, allTransactions) {
        val currentNow = Date()
        try {
            monthSummary = viewModel.buildMonthSummary(monthMode, currentNow)
            visibleReminder = monthSummary.dueReminders.firstOrNull { reminder ->
                !viewModel.isRecurringBannerDismissed(reminder)
            }
            if (monthMode == MonthMode.SALARY) {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                salaryWalletYearSummary = viewModel.buildSalaryWalletYearSummary(year)
            } else {
                salaryWalletYearSummary = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // 1. Salary reminder box at the very top
            visibleReminder?.let { reminder ->
                item(key = "salary-reminder-${reminder.id}") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Salary reminder", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Tap to record ${CurrencyUtils.formatCurrency(reminder.amount)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = {
                                navController.navigate(
                                    AppRoutes.addTransactionRoute(recurringId = reminder.id)
                                )
                            }) { Text("Record") }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    viewModel.dismissRecurringBanner(reminder)
                                    visibleReminder = null
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }
            }

            // 2. Balances Section
            item(key = "account-balances") {
                AccountBalanceCards(
                    cashBalance = monthSummary.currentBalances.cash,
                    bankBalance = monthSummary.currentBalances.bank
                )
            }

            // 3. Monthly summary card with brought-forward details & transfer button
            item(key = "month-summary-card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { summaryExpanded = !summaryExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Monthly Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                monthSummary.monthBounds?.let { bounds ->
                                    Text(
                                        bounds.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Remaining Income",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    val displayAmount = if (monthSummary.hasBroughtForward) {
                                        monthSummary.monthRemaining
                                    } else {
                                        monthSummary.monthNet - monthSummary.walletBalance
                                    }
                                    Text(
                                        formatSigned(displayAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (displayAmount >= 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    )
                                }
                                Icon(
                                    imageVector = if (summaryExpanded) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = if (summaryExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        AnimatedVisibility(visible = summaryExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Detailed Brought Forward section if previous month balance exists
                                if (monthSummary.hasBroughtForward) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "Added from Previous Month",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Leftover balance carried forward from the previous month and added into this month's available balance.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                formatSigned(monthSummary.broughtForward),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = if (monthSummary.broughtForward >= 0)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }

                                // Income / Expense / (Wallet) / Remaining
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricBox(
                                        label = "Income",
                                        value = "+${CurrencyUtils.formatCurrency(monthSummary.monthIncome)}",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBox(
                                        label = "Expense",
                                        value = "-${CurrencyUtils.formatCurrency(monthSummary.monthExpense)}",
                                        valueColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (monthSummary.walletBalance != 0.0) {
                                        MetricBox(
                                            label = "Wallet",
                                            value = "-${CurrencyUtils.formatCurrency(monthSummary.walletBalance)}",
                                            valueColor = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    val netAfterWallet = monthSummary.monthNet - monthSummary.walletBalance
                                    MetricBox(
                                        label = "Remaining",
                                        value = formatSigned(netAfterWallet),
                                        valueColor = if (netAfterWallet >= 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (monthSummary.hasBroughtForward) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Total remaining (including brought forward):",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            formatSigned(monthSummary.monthRemaining),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (monthSummary.monthRemaining >= 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Quick Actions: Transfer & Wallet Move
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { navController.navigate(AppRoutes.TRANSFER) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Transfer Cash ↔ Bank")
                                    }
                                    if (monthMode == MonthMode.SALARY) {
                                        OutlinedButton(
                                            onClick = { navController.navigate(AppRoutes.WALLET) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Move to/from Wallet")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Wallet summary card (Categorized by year, collapsed by default)
            if (salaryWalletYearSummary.isNotEmpty()) {
                item(key = "wallet-summary-card") {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { walletListExpanded = !walletListExpanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Wallet Summary ($currentYear)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Remaining in wallet per salary month",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Icon(
                                    imageVector = if (walletListExpanded) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = if (walletListExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }

                            AnimatedVisibility(visible = walletListExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    salaryWalletYearSummary.forEach { period ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(period.label, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    when {
                                                        period.isCurrent -> "Open"
                                                        period.isClosed -> "Closed"
                                                        else -> ""
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (period.isCurrent) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            Text(
                                                CurrencyUtils.formatCurrency(period.walletRemaining),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (period != salaryWalletYearSummary.last()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { navController.navigate(AppRoutes.WALLET) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Move to/from Wallet")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor
        )
    }
}

private fun formatSigned(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return prefix + CurrencyUtils.formatCurrency(amount)
}
