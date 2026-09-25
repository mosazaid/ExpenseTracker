package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.presentation.components.SwipeableTransactionItem
import com.example.expensetracker.presentation.components.TransactionFilterBottomSheet
import com.example.expensetracker.presentation.navigation.AddTransaction
import com.example.expensetracker.presentation.navigation.EditTransfer
import com.example.expensetracker.presentation.navigation.EditWallet
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
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
        val catExpenses = transactions.filter { it.categoryId == catId && it.type == TransactionType.EXPENSE }
        val map = catExpenses
            .filter { !it.subDescription.isNullOrBlank() && !it.subDescription.equals(HistoryGrouping.SUBCATEGORY_OTHER, ignoreCase = true) }
            .groupBy { it.subDescription!!.trim().lowercase() }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toMutableMap()
        val otherSum = catExpenses
            .filter { it.subDescription.isNullOrBlank() || it.subDescription.equals(HistoryGrouping.SUBCATEGORY_OTHER, ignoreCase = true) }
            .sumOf { it.amount }
        map[HistoryGrouping.SUBCATEGORY_OTHER.lowercase()] = otherSum
        map
    }

    val filteredTransactions = remember(transactions, searchQuery, categoryMap) {
        if (searchQuery.isBlank()) {
            transactions
        } else {
            val q = searchQuery.trim().lowercase()
            transactions.filter { txn ->
                txn.description.lowercase().contains(q) ||
                (txn.subDescription?.lowercase()?.contains(q) == true) ||
                (categoryMap[txn.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                txn.amount.toString().contains(q)
            }
        }
    }

    val sections = remember(
        filteredTransactions,
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
            transactions = filteredTransactions,
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

    Scaffold(
        topBar = {
            com.example.expensetracker.presentation.components.AppTopBar(
                title = stringResource(R.string.history_title),
                navController = navController
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {

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

        // ── Search Bar
        item(key = "search-bar") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.search_transactions_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
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
                            val otherTotal = subCategorySpendMap[HistoryGrouping.SUBCATEGORY_OTHER.lowercase()] ?: 0.0
                            DropdownMenuItem(
                                text = {
                                    Text("${HistoryGrouping.SUBCATEGORY_OTHER} — ${CurrencyUtils.formatCurrency(otherTotal)}")
                                },
                                onClick = {
                                    selectedSubCategoryFilter = HistoryGrouping.SUBCATEGORY_OTHER
                                    subCategoryMenuExpanded = false
                                }
                            )
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_transactions_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { navController.navigate(AddTransaction()) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.add_new_transaction),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
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
                                                EditTransfer(transaction.id)
                                            )
                                        }
                                        TransactionType.WALLET_MOVE -> {
                                            if (viewModel.canModifyTransaction(transaction)) {
                                                navController.navigate(
                                                    EditWallet(transaction.id)
                                                )
                                            } else {
                                                blockedActionMessage =
                                                    "Wallet moves in closed salary months are read-only."
                                            }
                                        }
                                        else -> {
                                            navController.navigate(
                                                AddTransaction(
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "$transactionCount",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (expenseTotal > 0.0) {
                    Text(
                        CurrencyUtils.formatCurrency(expenseTotal),
                        style = MaterialTheme.typography.labelLarge.withTabularNums(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Icon(
                    imageVector = if (isExpanded) androidx.compose.material.icons.Icons.Default.ExpandLess
                    else androidx.compose.material.icons.Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
