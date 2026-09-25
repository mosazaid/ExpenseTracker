package com.example.expensetracker.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.presentation.components.AccountBalanceCards
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.FinancialInsightsCard
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.*
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import com.example.expensetracker.presentation.viewModel.LoansViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
    loansViewModel: LoansViewModel = hiltViewModel()
) {
    val monthMode by viewModel.monthMode.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    val preservedAmount by loansViewModel.preservedAmount.collectAsState()
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

    val cashBal = monthSummary.currentBalances.cash ?: 0.0
    val bankBal = monthSummary.currentBalances.bank ?: 0.0
    val totalAvailable = (cashBal + bankBal)
    val recentTransactions = remember(allTransactions) { allTransactions.take(4) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                navController = navController
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)
        ) {
            // 1. Salary reminder banner
            visibleReminder?.let { reminder ->
                item(key = "salary-reminder-${reminder.id}") {
                    SalaryReminderBanner(
                        reminder = reminder,
                        onRecord = {
                            navController.navigate(
                                AppRoutes.addTransactionRoute(recurringId = reminder.id)
                            )
                        },
                        onDismiss = {
                            coroutineScope.launch {
                                viewModel.dismissRecurringBanner(reminder)
                                visibleReminder = null
                            }
                        }
                    )
                }
            }

            // 2. Hero Total Available Balance & Asset Distribution
            item(key = "hero-balance-card") {
                HeroBalanceCard(
                    totalBalance = totalAvailable,
                    cashBalance = cashBal,
                    bankBalance = bankBal
                )
            }

            // 3. Quick Action Pills
            item(key = "quick-actions-row") {
                QuickActionsRow(
                    isSalaryMode = monthMode == MonthMode.SALARY,
                    onTransfer = { navController.navigate(AppRoutes.TRANSFER) },
                    onAddTransaction = { navController.navigate(AppRoutes.addTransactionRoute()) },
                    onWallet = { navController.navigate(AppRoutes.WALLET) }
                )
            }

            // 4. Detailed Account Balance Cards (Cash / Bank)
            item(key = "account-balances") {
                AccountBalanceCards(
                    cashBalance = monthSummary.currentBalances.cash,
                    bankBalance = monthSummary.currentBalances.bank
                )
            }

            // 5. Preserved for Loans Banner (if active)
            if (preservedAmount > 0) {
                val spendable = (totalAvailable - preservedAmount).coerceAtLeast(0.0)
                item(key = "preserved-loan-banner") {
                    PreservedLoanCard(
                        preservedAmount = preservedAmount,
                        spendableAmount = spendable,
                        onNavigateLoans = { navController.navigate(AppRoutes.LOANS) }
                    )
                }
            }

            // 6. Monthly Financial Overview (Spending progress, metrics & breakdown)
            item(key = "month-summary-card") {
                MonthlyFinancialSummaryCard(
                    monthSummary = monthSummary,
                    isExpanded = summaryExpanded,
                    onToggleExpand = { summaryExpanded = !summaryExpanded }
                )
            }

            // 7. Smart Financial Insights Card
            item(key = "financial-insights-card") {
                FinancialInsightsCard(
                    totalIncome = monthSummary.monthIncome,
                    totalExpense = monthSummary.monthExpense,
                    totalWallet = monthSummary.walletBalance
                )
            }

            // 8. Recent Activity Preview
            item(key = "recent-activity-preview") {
                RecentActivityCard(
                    recentTransactions = recentTransactions,
                    categoryMap = categoryMap,
                    onViewAll = {
                        navController.navigate(AppRoutes.HISTORY) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTransactionClick = { txn ->
                        when (txn.type) {
                            TransactionType.TRANSFER -> {
                                navController.navigate(AppRoutes.editTransferRoute(txn.id))
                            }
                            TransactionType.WALLET_MOVE -> {
                                navController.navigate(AppRoutes.editWalletRoute(txn.id))
                            }
                            else -> {
                                navController.navigate(AppRoutes.addTransactionRoute(transactionId = txn.id))
                            }
                        }
                    }
                )
            }

            // 8. Yearly Wallet Summary (Salary mode only)
            if (salaryWalletYearSummary.isNotEmpty()) {
                item(key = "wallet-summary-card") {
                    WalletYearSummaryCard(
                        summaries = salaryWalletYearSummary,
                        isExpanded = walletListExpanded,
                        onToggleExpand = { walletListExpanded = !walletListExpanded },
                        onNavigateWallet = { navController.navigate(AppRoutes.WALLET) }
                    )
                }
            }
        }
    }
}

// ==========================================
// Sub-Components
// ==========================================

@Composable
private fun HeroBalanceCard(
    totalBalance: Double,
    cashBalance: Double,
    bankBalance: Double,
    modifier: Modifier = Modifier
) {
    val totalSafe = (cashBalance.coerceAtLeast(0.0) + bankBalance.coerceAtLeast(0.0)).coerceAtLeast(0.01)
    val cashRatio = (cashBalance.coerceAtLeast(0.0) / totalSafe).toFloat().coerceIn(0f, 1f)
    val bankRatio = (bankBalance.coerceAtLeast(0.0) / totalSafe).toFloat().coerceIn(0f, 1f)

    val animatedCashRatio by animateFloatAsState(
        targetValue = cashRatio,
        animationSpec = tween(durationMillis = 350),
        label = "cashRatio"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_available_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "JOD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = CurrencyUtils.formatCurrency(totalBalance),
                style = MaterialTheme.typography.displaySmall.withTabularNums(),
                fontWeight = FontWeight.Bold,
                color = when {
                    totalBalance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Segmented Asset Distribution Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    if (animatedCashRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(animatedCashRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    val remainingWeight = (1f - animatedCashRatio).coerceAtLeast(0.001f)
                    if (bankRatio > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(remainingWeight)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "${stringResource(R.string.account_cash)}: ${(cashRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "${stringResource(R.string.account_bank)}: ${(bankRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    isSalaryMode: Boolean,
    onTransfer: () -> Unit,
    onAddTransaction: () -> Unit,
    onWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            label = stringResource(R.string.action_transfer),
            icon = Icons.Outlined.SwapHoriz,
            onClick = onTransfer,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.add_transaction),
            icon = Icons.Outlined.AddCircleOutline,
            onClick = onAddTransaction,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (isSalaryMode) {
            QuickActionButton(
                label = stringResource(R.string.action_wallet),
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onWallet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 46.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MonthlyFinancialSummaryCard(
    monthSummary: HistoryViewModel.MonthSummaryUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthIncome = monthSummary.monthIncome
    val monthExpense = monthSummary.monthExpense
    val spentRatio = if (monthIncome > 0) (monthExpense / monthIncome).toFloat() else 0f
    val spentPercentage = (spentRatio * 100).toInt()

    val progressColor = when {
        spentRatio <= 0.70f -> FinancePositive
        spentRatio <= 0.90f -> FinanceWarning
        else -> FinanceNegative
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.monthly_summary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    monthSummary.monthBounds?.let { bounds ->
                        Text(
                            text = bounds.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // Spending Progress Indicator (Always visible)
            if (monthIncome > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.spending_progress_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$spentPercentage%",
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { spentRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }

            // Expanded content: 4-metric row and balance breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Metrics Row (Inflows & Outflows: Income, Expense, Wallet)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricBox(
                            label = stringResource(R.string.income),
                            amountPrefix = "+",
                            amount = monthIncome,
                            valueColor = FinancePositive,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricBox(
                            label = stringResource(R.string.expense),
                            amountPrefix = "-",
                            amount = monthExpense,
                            valueColor = FinanceNegative,
                            modifier = Modifier.weight(1f)
                        )
                        if (monthSummary.walletBalance != 0.0) {
                            SummaryMetricBox(
                                label = stringResource(R.string.action_wallet),
                                amountPrefix = "-",
                                amount = monthSummary.walletBalance,
                                valueColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Prominent Net Remaining Income Card (Bottom line)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (monthSummary.remainingIncome >= 0) {
                            FinancePositive.copy(alpha = 0.12f)
                        } else {
                            FinanceNegative.copy(alpha = 0.12f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (monthSummary.remainingIncome >= 0) FinancePositive.copy(alpha = 0.35f)
                            else FinanceNegative.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (monthSummary.remainingIncome >= 0)
                                        Icons.AutoMirrored.Filled.TrendingUp
                                    else
                                        Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (monthSummary.remainingIncome >= 0) FinancePositive else FinanceNegative,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.remaining_income_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (monthSummary.remainingIncome >= 0)
                                            stringResource(R.string.net_surplus_label)
                                        else
                                            stringResource(R.string.net_deficit_label),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                val remPrefix = if (monthSummary.remainingIncome >= 0) "+" else ""
                                Text(
                                    text = "$remPrefix${CurrencyUtils.formatAmountOnly(monthSummary.remainingIncome)}",
                                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (monthSummary.remainingIncome >= 0) FinancePositive else FinanceNegative,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "JOD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // Detailed Breakdown surface
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Carried from last month:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatSigned(monthSummary.previousMonthRemaining),
                                    style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current month savings:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatSigned(monthSummary.remainingIncome),
                                    style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (monthSummary.remainingIncome >= 0) FinancePositive else FinanceNegative
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Remaining (Cash + Bank):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = CurrencyUtils.formatAmountOnly(monthSummary.totalRemaining),
                                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthSummary.totalRemaining >= 0) FinancePositive else FinanceNegative,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "JOD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.End
                                    )
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
private fun SummaryMetricBox(
    label: String,
    amountPrefix: String = "",
    amount: Double,
    currency: String = "JOD",
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                softWrap = true,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$amountPrefix${CurrencyUtils.formatAmountOnly(amount)}",
                style = MaterialTheme.typography.titleSmall.withTabularNums(),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = currency,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecentActivityCard(
    recentTransactions: List<Transaction>,
    categoryMap: Map<Long, Category>,
    onViewAll: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.view_all_history),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentTransactions.forEach { txn ->
                        val cat = categoryMap[txn.categoryId]
                        RecentTransactionRow(
                            transaction = txn,
                            category = cat,
                            onClick = { onTransactionClick(txn) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor = when {
        isIncome -> FinancePositive
        isTransfer -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val amountPrefix = when {
        isIncome -> "+"
        isTransfer -> ""
        else -> "-"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isIncome -> FinancePositiveBg
                            isTransfer -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isIncome -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.TrendingUp
                        isTransfer -> Icons.Outlined.SwapHoriz
                        else -> Icons.Outlined.ShoppingBag
                    },
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = transaction.description.ifBlank { category?.name ?: "Transaction" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category?.name ?: ""} • ${DateUtils.format(transaction.date, DateUtils.PATTERN_MONTH_DAY)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Text(
            text = "$amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.titleSmall.withTabularNums(),
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

@Composable
private fun PreservedLoanCard(
    preservedAmount: Double,
    spendableAmount: Double,
    onNavigateLoans: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onNavigateLoans,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.preserved_for_loans_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = CurrencyUtils.formatCurrency(preservedAmount),
                    style = MaterialTheme.typography.titleLarge.withTabularNums(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(
                        R.string.spendable_balance_label,
                        CurrencyUtils.formatCurrency(spendableAmount)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = onNavigateLoans,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.loans_title))
            }
        }
    }
}

@Composable
private fun SalaryReminderBanner(
    reminder: RecurringTransaction,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salary reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Tap to record ${CurrencyUtils.formatCurrency(reminder.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onRecord) {
                Text("Record", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun WalletYearSummaryCard(
    summaries: List<com.example.expensetracker.domain.SalaryWalletPeriodSummary>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateWallet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Wallet Summary ($currentYear)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Remaining in wallet per salary month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    summaries.forEach { period ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(period.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = when {
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
                                text = CurrencyUtils.formatCurrency(period.walletRemaining),
                                style = MaterialTheme.typography.titleSmall.withTabularNums(),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onNavigateWallet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to/from Wallet")
                    }
                }
            }
        }
    }
}

private fun formatSigned(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return prefix + CurrencyUtils.formatCurrency(amount)
}

