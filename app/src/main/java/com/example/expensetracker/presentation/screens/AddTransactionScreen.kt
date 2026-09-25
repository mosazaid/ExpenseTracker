package com.example.expensetracker.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.presentation.components.AccountBalanceCards
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.viewModel.AddEditTransactionViewModel
import com.example.expensetracker.presentation.viewModel.LIQUID_ACCOUNTS
import kotlinx.coroutines.launch
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionId: Long? = null,
    recurringId: Long? = null,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthMode by viewModel.monthMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val calendar = Calendar.getInstance()
    val isEditMode = uiState.editingTransactionId != null

    val categoryList by viewModel.allCategories.collectAsState(initial = emptyList())
    val filteredCategories = categoryList.filter { it.type == uiState.transactionType }

    var cashBalance by remember { mutableStateOf<Double?>(null) }
    var bankBalance by remember { mutableStateOf<Double?>(null) }
    var balanceRefreshKey by remember { mutableIntStateOf(0) }
    var showInsufficientDialog by remember { mutableStateOf(false) }
    var showSalaryMonthDialog by remember { mutableStateOf(false) }
    var showSalaryReminderDialog by remember { mutableStateOf(false) }
    var carryForwardEnabled by remember { mutableStateOf(false) }
    var previousPeriodSaved by remember { mutableStateOf<Double?>(null) }
    var pendingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var unreimbursedExpenses by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var expenseMenuExpanded by remember { mutableStateOf(false) }

    // Subcategory state
    val selectedCategory = uiState.selectedCategory
    val savedSubCategories by remember(selectedCategory?.id) {
        if (selectedCategory != null) {
            viewModel.getSubCategories(selectedCategory.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    var saveAsSubCategoryChecked by remember { mutableStateOf(false) }
    var subCategoryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                com.example.expensetracker.presentation.viewModel.AddEditUiEvent.NavigateHistory -> {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoutes.OVERVIEW) {
                            popUpTo(AppRoutes.OVERVIEW) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.resetForm()
            viewModel.loadTransactionForEdit(transactionId)
        }
    }

    LaunchedEffect(recurringId) {
        if (recurringId != null && transactionId == null) {
            viewModel.resetForm()
            viewModel.loadRecurringForPrefill(recurringId)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                balanceRefreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(balanceRefreshKey, uiState.accountType, uiState.transactionType, uiState.editingTransactionId) {
        val editingId = uiState.editingTransactionId
        cashBalance = if (editingId != null) {
            viewModel.getAvailableBalanceForAccount(AccountType.CASH, editingId)
        } else {
            viewModel.getAvailableBalanceForAccount(AccountType.CASH, null)
        }
        bankBalance = if (editingId != null) {
            viewModel.getAvailableBalanceForAccount(AccountType.BANK, editingId)
        } else {
            viewModel.getAvailableBalanceForAccount(AccountType.BANK, null)
        }
    }

    LaunchedEffect(uiState.selectedCategory, monthMode) {
        val category = uiState.selectedCategory
        if (category != null && viewModel.isDeptCategory(category)) {
            unreimbursedExpenses = viewModel.getUnreimbursedExpensesForCurrentMonth()
        }
    }

    val availableBalance = when (uiState.accountType) {
        AccountType.CASH -> cashBalance
        AccountType.BANK -> bankBalance
        AccountType.WALLET -> null
    }

    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timePicker = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val cal = Calendar.getInstance().apply {
                time = uiState.selectedDate
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
            }
            viewModel.updateSelectedDate(cal.time)
        },
        Calendar.getInstance().apply { time = uiState.selectedDate }.get(Calendar.HOUR_OF_DAY),
        Calendar.getInstance().apply { time = uiState.selectedDate }.get(Calendar.MINUTE),
        true
    )

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val currentCal = Calendar.getInstance().apply { time = uiState.selectedDate }
            calendar.set(year, month, dayOfMonth, currentCal.get(Calendar.HOUR_OF_DAY), currentCal.get(Calendar.MINUTE))
            viewModel.updateSelectedDate(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    var budgetWarning by remember { mutableStateOf<com.example.expensetracker.presentation.viewModel.BudgetWarningInfo?>(null) }
    LaunchedEffect(uiState.selectedCategory, uiState.amount, uiState.selectedDate, uiState.transactionType) {
        budgetWarning = viewModel.checkBudgetWarning(uiState.selectedCategory, uiState.amount)
    }

    if (showInsufficientDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientDialog = false },
            title = { Text("Insufficient balance") },
            text = {
                Text(
                    "Insufficient ${uiState.accountType.name.lowercase()} balance. " +
                        "Available: ${CurrencyUtils.formatCurrency(availableBalance ?: 0.0)}. " +
                        "Allow negative balance?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInsufficientDialog = false
                    pendingTransaction?.let { txn ->
                        coroutineScope.launch {
                            saveAndNavigate(viewModel, txn.copy(allowNegativeBalance = true), navController)
                        }
                    }
                    pendingTransaction = null
                }) { Text("Allow negative") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInsufficientDialog = false
                    pendingTransaction = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showSalaryMonthDialog) {
        LaunchedEffect(showSalaryMonthDialog, uiState.selectedDate) {
            if (showSalaryMonthDialog) {
                previousPeriodSaved = viewModel.getPreviousPeriodSavedAmount(uiState.selectedDate)
                carryForwardEnabled = pendingTransaction?.carriedForwardBalance != null
            }
        }

        val canCarryForward = (previousPeriodSaved ?: 0.0) != 0.0

        AlertDialog(
            onDismissRequest = { showSalaryMonthDialog = false },
            title = { Text("Start new month?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Start a new salary month from ${DateUtils.formatDate(uiState.selectedDate)}?"
                    )
                    if (canCarryForward) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = carryForwardEnabled,
                                onCheckedChange = { carryForwardEnabled = it }
                            )
                            Column {
                                Text("Bring previous balance into new month")
                                Text(
                                    "Adds ${formatSignedAmount(previousPeriodSaved ?: 0.0)} from the previous salary month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        Text(
                            "No saved balance from the previous salary month to carry forward.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSalaryMonthDialog = false
                    pendingTransaction?.let { txn ->
                        coroutineScope.launch {
                            val carryAmount = if (carryForwardEnabled && canCarryForward) {
                                previousPeriodSaved
                            } else {
                                null
                            }
                            viewModel.saveTransaction(
                                txn.copy(
                                    startsNewPeriod = true,
                                    carriedForwardBalance = carryAmount
                                )
                            )
                            val recurringId = viewModel.uiState.value.pendingRecurringId
                            if (recurringId != null) {
                                viewModel.advanceRecurringAfterSave(recurringId)
                            }
                            pendingTransaction = null
                            if (!isEditMode) {
                                showSalaryReminderDialog = true
                            } else {
                                finishNavigate(viewModel, navController)
                            }
                        }
                    }
                }) { Text("Yes, start new month") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSalaryMonthDialog = false
                    pendingTransaction?.let { txn ->
                        coroutineScope.launch {
                            saveAndNavigate(viewModel, txn, navController)
                        }
                    }
                    pendingTransaction = null
                }) { Text("No") }
            }
        )
    }

    if (showSalaryReminderDialog) {
        val dayOfMonth = Calendar.getInstance().apply { time = uiState.selectedDate }
            .get(Calendar.DAY_OF_MONTH)
        AlertDialog(
            onDismissRequest = {
                showSalaryReminderDialog = false
                coroutineScope.launch { finishNavigate(viewModel, navController) }
            },
            title = { Text("Salary reminder") },
            text = { Text("Remind me monthly on the $dayOfMonth?") },
            confirmButton = {
                TextButton(onClick = {
                    showSalaryReminderDialog = false
                    coroutineScope.launch {
                        val amount = uiState.amount.toDoubleOrNull() ?: 0.0
                        viewModel.createSalaryReminder(amount, uiState.accountType, dayOfMonth)
                        finishNavigate(viewModel, navController)
                    }
                }) { Text("Yes, remind me") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSalaryReminderDialog = false
                    coroutineScope.launch { finishNavigate(viewModel, navController) }
                }) { Text("No thanks") }
            }
        )
    }

    Scaffold(
        topBar = {
            val titleText = if (isEditMode) {
                "Edit ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
            } else {
                "Add ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
            }
            AppTopBar(
                title = titleText,
                navController = navController,
                canNavigateBack = true,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(AppRoutes.OVERVIEW) {
                            popUpTo(AppRoutes.OVERVIEW) { inclusive = true }
                        }
                    }
                },
                showNotificationsBadge = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

        // ── 2. Your Money Now (Balance Cards at top)
        AccountBalanceCards(
            cashBalance = cashBalance,
            bankBalance = bankBalance,
            selectedAccount = uiState.accountType,
            onAccountSelected = { viewModel.updateAccountType(it) }
        )

        // ── 3. Transaction Type (Expense / Income)
        if (!isEditMode) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                        FilterChip(
                            selected = uiState.transactionType == type,
                            onClick = { viewModel.updateTransactionType(type) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        // ── 4. Account Selector (Bank default)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.account), style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LIQUID_ACCOUNTS.forEach { type ->
                    FilterChip(
                        selected = uiState.accountType == type,
                        onClick = { viewModel.updateAccountType(type) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (type == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (type == AccountType.BANK) stringResource(R.string.account_bank) else stringResource(R.string.account_cash)
                            )
                        }
                    )
                }
            }
        }

        // ── 5. Category Dropdown
        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                value = uiState.selectedCategory?.let { "${it.icon} ${it.name}" } ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                filteredCategories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(category.icon)
                                Text(category.name)
                            }
                        },
                        onClick = {
                            viewModel.updateSelectedCategory(category)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // ── 6. Subcategory / Sub-description Picker & Suggestion
        if (selectedCategory != null) {
            ExposedDropdownMenuBox(
                expanded = subCategoryMenuExpanded && savedSubCategories.isNotEmpty(),
                onExpandedChange = { subCategoryMenuExpanded = !subCategoryMenuExpanded }
            ) {
                OutlinedTextField(
                    value = uiState.subDescription,
                    onValueChange = {
                        viewModel.updateSubDescription(it)
                        subCategoryMenuExpanded = true
                    },
                    label = { Text("Subcategory / Sub-description") },
                    placeholder = { Text("e.g. Fuel, Groceries, Dining...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (savedSubCategories.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryMenuExpanded)
                        }
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth()
                )

                if (savedSubCategories.isNotEmpty()) {
                    val matchingSubCats = savedSubCategories.filter {
                        uiState.subDescription.isBlank() || it.name.contains(uiState.subDescription, ignoreCase = true)
                    }
                    if (matchingSubCats.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = subCategoryMenuExpanded,
                            onDismissRequest = { subCategoryMenuExpanded = false }
                        ) {
                            matchingSubCats.forEach { subCat ->
                                DropdownMenuItem(
                                    text = { Text(subCat.name) },
                                    onClick = {
                                        viewModel.updateSubDescription(subCat.name)
                                        subCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Prompt to save as reusable subcategory if entered name is not in saved list
            val trimmedSubDesc = uiState.subDescription.trim()
            val isAlreadySaved = savedSubCategories.any { it.name.equals(trimmedSubDesc, ignoreCase = true) }
            if (trimmedSubDesc.isNotBlank() && !isAlreadySaved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Checkbox(
                        checked = saveAsSubCategoryChecked,
                        onCheckedChange = { saveAsSubCategoryChecked = it }
                    )
                    Text(
                        text = "Save \"$trimmedSubDesc\" as reusable subcategory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 7. Amount Field
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(CurrencyUtils.cleanDecimalInput(it)) },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            isError = uiState.amount.isNotBlank() && uiState.amount.toDoubleOrNull() == null,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Budget Warning Banner (if approaching or exceeded)
        budgetWarning?.let { warning ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (warning.isExceeded) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (warning.isExceeded) {
                            stringResource(R.string.budget_warning_exceeded_title)
                        } else {
                            stringResource(R.string.budget_warning_near_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (warning.isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = if (warning.isExceeded) {
                            stringResource(
                                R.string.budget_warning_exceeded_desc,
                                warning.categoryName,
                                CurrencyUtils.formatCurrency(warning.projectedSpent),
                                CurrencyUtils.formatCurrency(warning.limit)
                            )
                        } else {
                            stringResource(
                                R.string.budget_warning_near_desc,
                                warning.categoryName,
                                CurrencyUtils.formatCurrency(warning.projectedSpent),
                                CurrencyUtils.formatCurrency(warning.limit)
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (warning.isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // ── 8. Description Field
        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text(stringResource(R.string.description)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        // ── 9. Date & Time Selection (Time is optional, defaults to current time)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { datePicker.show() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(DateUtils.formatDate(uiState.selectedDate))
            }
            OutlinedButton(
                onClick = { timePicker.show() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(timeFormat.format(uiState.selectedDate))
            }
        }

        var isDept by remember { mutableStateOf(false) }
        LaunchedEffect(selectedCategory) {
            isDept = viewModel.isDeptCategory(selectedCategory)
        }

        // ── 10. Debt & Reimbursement options
        if (selectedCategory != null && isDept) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.transactionType == TransactionType.EXPENSE) {
                            Text(
                                text = stringResource(R.string.debt_lent_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.debt_lent_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            OutlinedTextField(
                                value = uiState.debtorNote,
                                onValueChange = { viewModel.updateDebtorNote(it) },
                                label = { Text(stringResource(R.string.debtor_name_label)) },
                                placeholder = { Text("e.g. Ahmad, Omar") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.debt_income_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !uiState.isIncomeBorrowedDebt,
                                    onClick = { viewModel.updateIncomeDebtType(false) },
                                    label = { Text(stringResource(R.string.debt_income_repayment_opt)) }
                                )
                                FilterChip(
                                    selected = uiState.isIncomeBorrowedDebt,
                                    onClick = { viewModel.updateIncomeDebtType(true) },
                                    label = { Text(stringResource(R.string.debt_income_borrowed_opt)) }
                                )
                            }

                            if (!uiState.isIncomeBorrowedDebt) {
                                Text(
                                    text = stringResource(R.string.debt_income_repayment_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                ExposedDropdownMenuBox(
                                    expanded = expenseMenuExpanded,
                                    onExpandedChange = { expenseMenuExpanded = !expenseMenuExpanded }
                                ) {
                                    val linkedExpense = unreimbursedExpenses.find { it.id == uiState.linkedExpenseId }
                                        ?: uiState.linkedExpenseId?.let { id ->
                                            unreimbursedExpenses.find { it.id == id }
                                        }
                                    OutlinedTextField(
                                        value = linkedExpense?.let {
                                            "${it.debtorNote?.ifBlank { it.description } ?: it.description} — ${CurrencyUtils.formatCurrency(it.amount)}"
                                        } ?: stringResource(R.string.link_to_expense_optional),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.link_to_expense_debt)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expenseMenuExpanded,
                                        onDismissRequest = { expenseMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.none_opt)) },
                                            onClick = {
                                                viewModel.updateLinkedExpenseId(null)
                                                expenseMenuExpanded = false
                                            }
                                        )
                                        unreimbursedExpenses.forEach { expense ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${DateUtils.formatDate(expense.date)} — " +
                                                            (expense.debtorNote?.ifBlank { expense.description } ?: expense.description) + " — " +
                                                            CurrencyUtils.formatCurrency(expense.amount)
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.updateLinkedExpenseId(expense.id)
                                                    expenseMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.debt_income_borrowed_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                OutlinedTextField(
                                    value = uiState.debtorNote,
                                    onValueChange = { viewModel.updateDebtorNote(it) },
                                    label = { Text(stringResource(R.string.creditor_name_label)) },
                                    placeholder = { Text("e.g. Bank, Friend") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

        if (uiState.transactionType == TransactionType.EXPENSE && !isDept) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Someone owes me",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Mark if you paid and are waiting to be paid back",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = uiState.awaitingReimbursement,
                    onCheckedChange = { viewModel.updateAwaitingReimbursement(it) }
                )
            }
            if (uiState.awaitingReimbursement) {
                OutlinedTextField(
                    value = uiState.debtorNote,
                    onValueChange = { viewModel.updateDebtorNote(it) },
                    label = { Text("Who owes (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 11. Save Button
        val isFormValid = uiState.amount.toDoubleOrNull() != null &&
            uiState.amount.isNotBlank() &&
            uiState.selectedCategory != null

        Button(
            onClick = {
                coroutineScope.launch {
                    val amountDouble = uiState.amount.toDoubleOrNull()
                    val category = uiState.selectedCategory
                    if (amountDouble == null || category == null) return@launch

                    // Save subcategory if checked
                    val subDescTrimmed = uiState.subDescription.trim()
                    if (saveAsSubCategoryChecked && subDescTrimmed.isNotBlank()) {
                        viewModel.saveSubCategory(category.id, subDescTrimmed)
                    }

                    val isDeptCat = viewModel.isDeptCategory(category)
                    val computedDebtType = if (isDeptCat) {
                        if (uiState.transactionType == TransactionType.EXPENSE) "LENT"
                        else if (uiState.isIncomeBorrowedDebt) "BORROWED"
                        else "REPAYMENT"
                    } else uiState.debtType

                    val transaction = Transaction(
                        id = uiState.editingTransactionId ?: 0L,
                        amount = amountDouble,
                        description = uiState.description,
                        subDescription = uiState.subDescription.takeIf { it.isNotBlank() },
                        categoryId = category.id,
                        type = uiState.transactionType,
                        accountType = uiState.accountType,
                        date = uiState.selectedDate,
                        linkedExpenseId = uiState.linkedExpenseId,
                        debtorNote = uiState.debtorNote.takeIf { it.isNotBlank() },
                        awaitingReimbursement = uiState.transactionType == TransactionType.EXPENSE &&
                            (uiState.awaitingReimbursement || (isDeptCat && computedDebtType == "LENT")),
                        debtType = computedDebtType,
                        isDebtSettled = uiState.isDebtSettled,
                        startsNewPeriod = uiState.startsNewPeriod,
                        carriedForwardBalance = uiState.carriedForwardBalance,
                        allowNegativeBalance = uiState.allowNegativeBalance,
                        createdAt = uiState.createdAt ?: Date()
                    )

                    if (uiState.transactionType == TransactionType.EXPENSE) {
                        val balance = viewModel.getAvailableBalanceForAccount(
                            uiState.accountType,
                            uiState.editingTransactionId
                        )
                        if (amountDouble > balance) {
                            pendingTransaction = transaction
                            showInsufficientDialog = true
                            return@launch
                        }
                    }

                    val isSalaryIncomeInSalaryMode = monthMode == MonthMode.SALARY &&
                        uiState.transactionType == TransactionType.INCOME &&
                        viewModel.isSalaryCategory(category)

                    if (isSalaryIncomeInSalaryMode && viewModel.shouldPromptSalaryMonth(category, isEditMode)) {
                        pendingTransaction = transaction
                        showSalaryMonthDialog = true
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saveAndNavigate(viewModel, transaction, navController)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid
        ) {
            Text(if (isEditMode) "Update Transaction" else "Save Transaction")
        }
    }
    }
}

private suspend fun saveAndNavigate(
    viewModel: AddEditTransactionViewModel,
    transaction: Transaction,
    navController: NavController
) {
    viewModel.saveTransaction(transaction)
    val recurringId = viewModel.uiState.value.pendingRecurringId
    if (recurringId != null) {
        viewModel.advanceRecurringAfterSave(recurringId)
    }
    finishNavigate(viewModel, navController)
}

private suspend fun finishNavigate(
    viewModel: AddEditTransactionViewModel,
    navController: NavController
) {
    viewModel.resetForm()
    viewModel.emitNavigateHistory()
}

private fun formatSignedAmount(amount: Double): String {
    val prefix = if (amount >= 0) "+" else ""
    return prefix + CurrencyUtils.formatCurrency(amount)
}
