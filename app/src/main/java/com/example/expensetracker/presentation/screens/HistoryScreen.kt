package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.presentation.components.SwipeableTransactionItem
import com.example.expensetracker.presentation.components.TransactionFilterBottomSheet
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.MONTH) }
    var selectedTypeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) }
    var selectedSubCategoryFilter by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var subCategoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedSortOrder by remember { mutableStateOf(TransactionSortOrder.DESC) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val monthMode by viewModel.monthMode.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState(initial = emptyList())
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.type == TransactionType.EXPENSE }
    }

    val currentSubCategories by remember(selectedCategoryFilter?.id) {
        val catId = selectedCategoryFilter?.id
        if (catId != null) {
            viewModel.getSubCategories(catId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val now = remember { Date() }

    var periodBounds by remember { mutableStateOf<PeriodBounds?>(null) }
    var periodUiState by remember { mutableStateOf(HistoryViewModel.PeriodUiState()) }

    val allTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    LaunchedEffect(selectedPeriod, monthMode) {
        periodBounds = viewModel.getPeriodBounds(selectedPeriod, monthMode, now)
    }

    val startDate = periodBounds?.start
    val endDate = periodBounds?.end

    val transactions by produceState<List<Transaction>>(
        initialValue = emptyList(),
        startDate,
        endDate
    ) {
        if (startDate == null || endDate == null) {
            value = emptyList()
            return@produceState
        }
        viewModel.getTransactionsBetweenDates(startDate, endDate).collect { value = it }
    }

    LaunchedEffect(startDate, endDate, allCategories) {
        if (allCategories.isNotEmpty() && startDate != null && endDate != null) {
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

    var salaryWalletYearSummary by remember {
        mutableStateOf<List<com.example.expensetracker.domain.SalaryWalletPeriodSummary>>(emptyList())
    }

    LaunchedEffect(monthMode, allTransactions) {
        if (monthMode == MonthMode.SALARY) {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            salaryWalletYearSummary = viewModel.buildSalaryWalletYearSummary(year)
        } else {
            salaryWalletYearSummary = emptyList()
        }
    }

    val subCategorySpendMap = remember(transactions, selectedCategoryFilter) {
        val catId = selectedCategoryFilter?.id ?: return@remember emptyMap<String, Double>()
        transactions
            .filter { it.categoryId == catId && it.type == TransactionType.EXPENSE && !it.subDescription.isNullOrBlank() }
            .groupBy { it.subDescription!!.trim().lowercase() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
    }

    val sections = remember(
        transactions,
        selectedPeriod,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedSubCategoryFilter,
        selectedSortOrder,
        periodBounds,
        reimbursedExpenseIds,
        monthMode,
        salaryWalletYearSummary
    ) {
        HistoryGrouping.group(
            transactions = transactions,
            period = selectedPeriod,
            typeFilter = selectedTypeFilter,
            categoryIdFilter = selectedCategoryFilter?.id,
            subCategoryFilter = selectedSubCategoryFilter,
            sortOrder = selectedSortOrder,
            reimbursedExpenseIds = reimbursedExpenseIds,
            monthMode = monthMode,
            salaryPeriods = salaryWalletYearSummary
        )
    }

    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(sections) {
        expandedSections = sections.map { it.key }.toSet()
    }

    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }
    var blockedActionMessage by remember { mutableStateOf<String?>(null) }

    if (showFilterSheet) {
        TransactionFilterBottomSheet(
            selectedType = selectedTypeFilter,
            selectedPeriod = selectedPeriod,
            selectedSortOrder = selectedSortOrder,
            onTypeSelected = { selectedTypeFilter = it },
            onPeriodSelected = {
                selectedPeriod = it
                selectedCategoryFilter = null
                selectedSubCategoryFilter = null
            },
            onSortOrderSelected = { selectedSortOrder = it },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (blockedActionMessage != null) {
        AlertDialog(
            onDismissRequest = { blockedActionMessage = null },
            title = { Text(stringResource(R.string.closed_salary_month)) },
            text = { Text(blockedActionMessage!!) },
            confirmButton = {
                TextButton(onClick = { blockedActionMessage = null }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = { Text(stringResource(R.string.delete_transaction_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget?.let { viewModel.deleteTransaction(it) }
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // ── Title row
        item(key = "title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { navController.navigate(AppRoutes.SETTINGS) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }

        // ── 1. Period from to at the top
        item(key = "period-range") {
            if (startDate != null && endDate != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "From ${DateUtils.formatDate(startDate)} To ${DateUtils.formatDate(endDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = selectedPeriod.label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Compact toggle for Salary/Calendar month + Filter icon button
        item(key = "month-toggle-filter") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (monthMode == MonthMode.SALARY) "Salary Month" else "Calendar Month",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (monthMode == MonthMode.SALARY) "Tracking from salary to salary"
                        else "Tracking from 1st to end of month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = monthMode == MonthMode.SALARY,
                        onCheckedChange = { checked ->
                            viewModel.setMonthMode(
                                if (checked) MonthMode.SALARY else MonthMode.CALENDAR
                            )
                        },
                        modifier = Modifier.scale(0.85f)
                    )
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ── 3. Category dropdown filter
        if (expenseCategories.isNotEmpty()) {
            item(key = "category-filter") {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryFilter?.let {
                            "${it.icon} ${it.name}"
                        } ?: stringResource(R.string.all_categories),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
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
                            text = { Text(stringResource(R.string.all_categories)) },
                            onClick = {
                                selectedCategoryFilter = null
                                selectedSubCategoryFilter = null
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
                                    selectedSubCategoryFilter = null
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── 3.1 Subcategory dropdown filter (when selected category has subcategories)
            if (selectedCategoryFilter != null && currentSubCategories.isNotEmpty()) {
                item(key = "subcategory-filter") {
                    ExposedDropdownMenuBox(
                        expanded = subCategoryMenuExpanded,
                        onExpandedChange = { subCategoryMenuExpanded = !subCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedSubCategoryFilter ?: "All Subcategories",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subcategory") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subCategoryMenuExpanded,
                            onDismissRequest = { subCategoryMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Subcategories") },
                                onClick = {
                                    selectedSubCategoryFilter = null
                                    subCategoryMenuExpanded = false
                                }
                            )
                            currentSubCategories.forEach { subCat ->
                                val subTotal = subCategorySpendMap[subCat.name.trim().lowercase()] ?: 0.0
                                DropdownMenuItem(
                                    text = {
                                        Text("${subCat.name} — ${CurrencyUtils.formatCurrency(subTotal)}")
                                    },
                                    onClick = {
                                        selectedSubCategoryFilter = subCat.name
                                        subCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active filters indication
        item(key = "active-filters") {
            ActiveFiltersLine(
                typeFilter = selectedTypeFilter,
                categoryFilter = selectedCategoryFilter,
                subCategoryFilter = selectedSubCategoryFilter,
                sortOrder = selectedSortOrder
            )
        }

        // ── Transactions list
        if (sections.isEmpty()) {
            item(key = "empty-transactions") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            sections.forEach { section ->
                val isExpanded = section.key in expandedSections
                item(key = "section-header-${section.key}") {
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
                        SwipeableTransactionItem(
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

@Composable
private fun ActiveFiltersLine(
    typeFilter: TransactionTypeFilter,
    categoryFilter: Category?,
    subCategoryFilter: String? = null,
    sortOrder: TransactionSortOrder = TransactionSortOrder.DESC
) {
    val parts = buildList {
        if (typeFilter != TransactionTypeFilter.ALL) add(typeFilter.label)
        categoryFilter?.let { add("${it.icon} ${it.name}") }
        subCategoryFilter?.let { add(it) }
        if (sortOrder == TransactionSortOrder.ASC) add(sortOrder.label)
    }
    if (parts.isEmpty()) return
    Text(
        text = stringResource(R.string.filter_active, parts.joinToString(" · ")),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
    )
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
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
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
                        imageVector = if (isExpanded) androidx.compose.material.icons.Icons.Default.ExpandLess
                        else androidx.compose.material.icons.Icons.Default.ExpandMore,
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
