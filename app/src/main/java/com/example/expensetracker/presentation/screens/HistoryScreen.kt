package com.example.expensetracker.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.domain.AccountBalances
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.presentation.components.TransactionItem
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import java.util.*
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.theme.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.MONTH) }
    var selectedTypeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var summaryExpanded by remember { mutableStateOf(true) }

    val monthMode by viewModel.monthMode.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.type == TransactionType.EXPENSE }
    }

    val coroutineScope = rememberCoroutineScope()
    val now = remember { Date() }

    // Month summary — always the salary/calendar month, independent of list period
    var monthSummary by remember { mutableStateOf(HistoryViewModel.MonthSummaryUiState()) }
    var visibleReminder by remember { mutableStateOf<RecurringTransaction?>(null) }

    // List period bounds — driven by the selected period chip
    var periodBounds by remember { mutableStateOf<PeriodBounds?>(null) }

    // Category totals and budget progress — scoped to the list period
    var periodUiState by remember { mutableStateOf(HistoryViewModel.PeriodUiState()) }

    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    // 2. Recompute list period bounds whenever period selector or month mode changes
    LaunchedEffect(selectedPeriod, monthMode) {
        periodBounds = viewModel.getPeriodBounds(selectedPeriod, monthMode, now)
    }

    val startDate = periodBounds?.start ?: now
    val endDate = periodBounds?.end ?: now

    val transactions by viewModel.getTransactionsBetweenDates(startDate, endDate)
        .collectAsState(initial = emptyList())

    // 1. Recompute month summary whenever month mode or any transaction changes
    LaunchedEffect(monthMode, allTransactions) {
        monthSummary = viewModel.buildMonthSummary(monthMode, now)
        visibleReminder = monthSummary.dueReminders.firstOrNull { reminder ->
            !viewModel.isRecurringBannerDismissed(reminder)
        }
    }

    // 3. Recompute category/budget data whenever list period or categories change
    LaunchedEffect(startDate, endDate, allCategories) {
        if (allCategories.isNotEmpty()) {
            periodUiState = viewModel.buildPeriodUiState(
                categories = allCategories,
                expenseCategoryIds = expenseCategories.map { it.id },
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    val reimbursedExpenseIds = remember(transactions) {
        transactions.mapNotNull { it.linkedExpenseId }.toSet()
    }

    val linkedExpenseDescriptions = remember(transactions) {
        val expenseById = transactions.filter { it.type == TransactionType.EXPENSE }
            .associateBy { it.id }
        transactions.filter { it.linkedExpenseId != null }.associate { txn ->
            txn.id to expenseById[txn.linkedExpenseId]?.description.orEmpty()
        }
    }

    val sections = remember(
        transactions,
        selectedPeriod,
        selectedTypeFilter,
        selectedCategoryFilter,
        periodBounds,
        reimbursedExpenseIds
    ) {
        HistoryGrouping.group(
            transactions = transactions,
            period = selectedPeriod,
            typeFilter = selectedTypeFilter,
            categoryIdFilter = selectedCategoryFilter?.id,
            reimbursedExpenseIds = reimbursedExpenseIds
        )
    }

    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(sections) {
        expandedSections = sections.map { it.key }.toSet()
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }
    var blockedActionMessage by remember { mutableStateOf<String?>(null) }
    var salaryWalletYearSummary by remember {
        mutableStateOf<List<com.example.expensetracker.domain.SalaryWalletPeriodSummary>>(emptyList())
    }

    LaunchedEffect(selectedPeriod, monthMode, allTransactions) {
        if (selectedPeriod == HistoryPeriod.YEAR && monthMode == MonthMode.SALARY) {
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            salaryWalletYearSummary = viewModel.buildSalaryWalletYearSummary(year)
        } else {
            salaryWalletYearSummary = emptyList()
        }
    }

    if (blockedActionMessage != null) {
        AlertDialog(
            onDismissRequest = { blockedActionMessage = null },
            title = { Text("Closed salary month") },
            text = { Text(blockedActionMessage!!) },
            confirmButton = {
                TextButton(onClick = { blockedActionMessage = null }) { Text("OK") }
            }
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget?.let { viewModel.deleteTransaction(it) }
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // ── Title row
        item(key = "title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // ── Month summary card (always salary/calendar month, collapsible)
        item(key = "month-summary") {
            MonthSummaryCard(
                summary = monthSummary,
                expanded = summaryExpanded,
                onToggle = { summaryExpanded = !summaryExpanded },
                isSalaryMode = monthMode == MonthMode.SALARY,
                onWalletClick = { navController.navigate(AppRoutes.WALLET) }
            )
        }

        // ── Salary reminder banner
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
                            .padding(12.dp),
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

        // ── Month mode toggle + period selector
        item(key = "month-toggle") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (monthMode == MonthMode.SALARY) "Salary month" else "Calendar month",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            if (selectedPeriod == HistoryPeriod.MONTH) {
                                periodBounds?.label ?: ""
                            } else {
                                periodBounds?.label ?: selectedPeriod.label
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        periodBounds?.hint?.let { hint ->
                            Text(
                                hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = monthMode == MonthMode.SALARY,
                        onCheckedChange = { checked ->
                            viewModel.setMonthMode(
                                if (checked) MonthMode.SALARY else MonthMode.CALENDAR
                            )
                        }
                    )
                }
                Text(
                    if (monthMode == MonthMode.SALARY) "From last salary" else "1st – last day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // ── Period selector chips
        item(key = "period-filter") {
            Column {
                Text("Period", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = {
                                selectedPeriod = period
                                selectedCategoryFilter = null
                            },
                            label = { Text(period.label) }
                        )
                    }
                }
            }
        }

        // ── Type filter chips
        item(key = "type-filter") {
            Column {
                Text("Filter", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionTypeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedTypeFilter == filter,
                            onClick = { selectedTypeFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }
        }

        // ── Category filter (expense only, when categories available)
        if (expenseCategories.isNotEmpty()) {
            item(key = "category-filter") {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryFilter?.let {
                            "${it.icon} ${it.name}"
                        } ?: "All categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All categories") },
                            onClick = {
                                selectedCategoryFilter = null
                                categoryMenuExpanded = false
                            }
                        )
                        expenseCategories.forEach { category ->
                            val total = periodUiState.categoryTotals[category.id] ?: 0.0
                            val progress = periodUiState.budgetProgress[category.id]
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "${category.icon} ${category.name} — " +
                                                CurrencyUtils.formatCurrency(total)
                                        )
                                        progress?.let { p ->
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { p.percent.coerceAtMost(1f) },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = when {
                                                    p.isOverBudget -> MaterialTheme.colorScheme.error
                                                    p.isNearLimit -> MaterialTheme.colorScheme.tertiary
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                            Text(
                                                "${CurrencyUtils.formatCurrency(p.spent)} / " +
                                                    CurrencyUtils.formatCurrency(p.limit),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategoryFilter = category
                                    categoryMenuExpanded = false
                                },
                                enabled = total > 0.0 || selectedCategoryFilter?.id == category.id
                            )
                        }
                    }
                }
            }
        }

        // ── Salary wallet year timeline (salary mode + year period only)
        if (salaryWalletYearSummary.isNotEmpty()) {
            item(key = "salary-wallet-year") {
                SalaryWalletYearCard(
                    periods = salaryWalletYearSummary,
                    onAddWalletMove = { navController.navigate(AppRoutes.WALLET) }
                )
            }
        }

        // ── Transaction list or empty state
        if (sections.isEmpty()) {
            item(key = "empty") {
                Text("No transactions found.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            sections.forEach { section ->
                val isExpanded = section.key in expandedSections
                item(key = "header-${section.key}") {
                    HistorySectionHeader(
                        title = section.title,
                        expenseTotal = section.expenseTotal,
                        transactionCount = section.transactions.size,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedSections = if (isExpanded) {
                                expandedSections - section.key
                            } else {
                                expandedSections + section.key
                            }
                        }
                    )
                }
                if (isExpanded) {
                    items(section.transactions, key = { it.id }) { txn ->
                        TransactionItem(
                            transaction = txn,
                            category = categoryMap[txn.categoryId],
                            onDelete = {
                                coroutineScope.launch {
                                    if (viewModel.canModifyTransaction(it)) {
                                        deleteTarget = it
                                    } else {
                                        blockedActionMessage =
                                            "Wallet moves in closed salary months cannot be deleted."
                                    }
                                }
                            },
                            onEdit = { transaction ->
                                coroutineScope.launch {
                                    when (transaction.type) {
                                        TransactionType.TRANSFER -> {
                                            navController.navigate(
                                                AppRoutes.editTransferRoute(transaction.id)
                                            )
                                        }
                                        TransactionType.WALLET_MOVE -> {
                                            if (viewModel.canModifyTransaction(transaction)) {
                                                navController.navigate(
                                                    AppRoutes.editWalletRoute(transaction.id)
                                                )
                                            } else {
                                                blockedActionMessage =
                                                    "Wallet moves in closed salary months are read-only."
                                            }
                                        }
                                        else -> {
                                            navController.navigate(
                                                AppRoutes.addTransactionRoute(
                                                    transactionId = transaction.id
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            isReimbursed = txn.id in reimbursedExpenseIds,
                            isOwed = txn.type == TransactionType.EXPENSE &&
                                txn.awaitingReimbursement &&
                                txn.id !in reimbursedExpenseIds,
                            linkedExpenseDescription = linkedExpenseDescriptions[txn.id]
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Month summary card — always month-scoped, collapsible
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthSummaryCard(
    summary: HistoryViewModel.MonthSummaryUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    isSalaryMode: Boolean,
    onWalletClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row — always visible, tap to collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Month summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    summary.monthBounds?.let { bounds ->
                        Text(
                            bounds.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        "You have ${CurrencyUtils.formatCurrency(summary.currentBalances.total)} now",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (summary.hasBroughtForward) "Remaining" else "Saved this month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val displayAmount = if (summary.hasBroughtForward) {
                            summary.monthRemaining
                        } else {
                            summary.monthNet
                        }
                        Text(
                            formatSigned(displayAmount),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (displayAmount >= 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Expandable body
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (summary.hasBroughtForward) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Brought forward",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Leftover from the previous salary month, added when you started this month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            SummaryMetric(
                                label = "From last month",
                                value = formatSigned(summary.broughtForward),
                                valueColor = if (summary.broughtForward >= 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Income / Expense / Saved — activity in this salary/calendar month only
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Activity this month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Income and spending between the dates above — not your current wallet balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetric(
                                label = "Income",
                                value = "+${CurrencyUtils.formatCurrency(summary.monthIncome)}",
                                valueColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Expense",
                                value = "-${CurrencyUtils.formatCurrency(summary.monthExpense)}",
                                valueColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Saved",
                                value = formatSigned(summary.monthNet),
                                valueColor = if (summary.monthNet >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (summary.hasBroughtForward) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryMetric(
                                    label = "Remaining total",
                                    value = formatSigned(summary.monthRemaining),
                                    valueColor = if (summary.monthRemaining >= 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                "Brought forward + saved this month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Current balances — must match Add expense/income screen
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Your money now",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Cash and bank available right now — same numbers as Add expense/income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val now = summary.currentBalances
                            SummaryMetric(
                                label = "Cash",
                                value = CurrencyUtils.formatCurrency(now.cash),
                                valueColor = if (now.cash < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Bank",
                                value = CurrencyUtils.formatCurrency(now.bank),
                                valueColor = if (now.bank < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Total",
                                value = CurrencyUtils.formatCurrency(now.total),
                                valueColor = if (now.total < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    if (isSalaryMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Wallet this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Money set aside from cash/bank — resets each salary month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    CurrencyUtils.formatCurrency(summary.walletBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                TextButton(onClick = onWalletClick) {
                                    Text("Move to/from wallet")
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // How each cash/bank account changed during this month (includes transfers)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Cash & bank change this month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "How much each account went up or down — total always equals Saved above",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val change = summary.periodChangeByAccount
                            SummaryMetric(
                                label = "Cash",
                                value = formatSigned(change.cash),
                                valueColor = if (change.cash < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Bank",
                                value = formatSigned(change.bank),
                                valueColor = if (change.bank < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Total",
                                value = formatSigned(change.total),
                                valueColor = if (change.total < 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Show transfers row only when transfers exist this month
                    if (summary.hasTransfers) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Transfers this month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Internal moves between your accounts — no effect on total balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val t = summary.transferImpact
                                SummaryMetric(
                                    label = "Cash",
                                    value = formatSigned(t.cash),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetric(
                                    label = "Bank",
                                    value = formatSigned(t.bank),
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetric(
                                    label = "Total",
                                    value = formatSigned(t.total),
                                    valueColor = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Salary wallet year timeline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SalaryWalletYearCard(
    periods: List<com.example.expensetracker.domain.SalaryWalletPeriodSummary>,
    onAddWalletMove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Salary months — wallet summary",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "Remaining in wallet at each salary month end — not carried forward",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            periods.forEach { period ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            period.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            when {
                                period.isCurrent -> "Open"
                                period.isClosed -> "Closed"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (period.isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                    Text(
                        CurrencyUtils.formatCurrency(period.walletRemaining),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (period != periods.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (periods.any { it.isCurrent }) {
                OutlinedButton(
                    onClick = onAddWalletMove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Move to/from wallet")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatSigned(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return prefix + CurrencyUtils.formatCurrency(amount)
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
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

@Composable
private fun HistorySectionHeader(
    title: String,
    expenseTotal: Double,
    transactionCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "$transactionCount transaction${if (transactionCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    CurrencyUtils.formatCurrency(expenseTotal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
