package com.example.expensetracker.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Lock
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
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
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
import com.example.expensetracker.presentation.navigation.Overview
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.math.ArithmeticEvaluator
import com.example.expensetracker.core.storage.ReceiptStorageHelper
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.components.ReceiptAttachmentSection
import com.example.expensetracker.presentation.components.SplitTransactionSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionId: Long? = null,
    recurringId: Long? = null,
    initialAccountType: AccountType? = null,
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
    var walletBalance by remember { mutableStateOf<Double?>(null) }
    var depositBalance by remember { mutableStateOf<Double?>(null) }
    var balanceRefreshKey by remember { mutableIntStateOf(0) }
    var showInsufficientDialog by remember { mutableStateOf(false) }
    var showStrictInsufficientDialog by remember { mutableStateOf(false) }
    var showSalaryMonthDialog by remember { mutableStateOf(false) }
    var showSalaryReminderDialog by remember { mutableStateOf(false) }
    var carryForwardEnabled by remember { mutableStateOf(false) }
    var previousPeriodSaved by remember { mutableStateOf<Double?>(null) }
    var pendingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var unreimbursedExpenses by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var expenseMenuExpanded by remember { mutableStateOf(false) }
    var makeRecurringChecked by remember { mutableStateOf(false) }
    var selectedRecurrenceFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }

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
                        navController.navigate(Overview) {
                            popUpTo<Overview> { inclusive = true }
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

    LaunchedEffect(initialAccountType) {
        if (initialAccountType != null && transactionId == null) {
            viewModel.updateAccountType(initialAccountType)
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
        cashBalance = viewModel.getAvailableBalanceForAccount(AccountType.CASH, editingId)
        bankBalance = viewModel.getAvailableBalanceForAccount(AccountType.BANK, editingId)
        walletBalance = viewModel.getAvailableBalanceForAccount(AccountType.WALLET, editingId)
        depositBalance = viewModel.getAvailableBalanceForAccount(AccountType.DEPOSIT, editingId)
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
        AccountType.WALLET -> walletBalance
        AccountType.DEPOSIT -> depositBalance
    }

    val timeFormatPref by viewModel.timeFormat.collectAsState()
    val is24Hour = timeFormatPref == com.example.expensetracker.data.preferences.TimeFormatPreference.H24
    val timeFormat = remember(is24Hour) {
        java.text.SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm a", Locale.getDefault())
    }
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
        is24Hour
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

    if (showStrictInsufficientDialog) {
        val acctName = if (uiState.accountType == AccountType.WALLET) {
            stringResource(R.string.account_wallet)
        } else {
            stringResource(R.string.deposit_vault_title)
        }
        AlertDialog(
            onDismissRequest = { showStrictInsufficientDialog = false },
            title = { Text(stringResource(R.string.strict_insufficient_balance_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.strict_insufficient_balance_desc,
                        acctName,
                        CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                        CurrencyUtils.formatCurrency(uiState.amount.toDoubleOrNull() ?: 0.0)
                    )
                )
            },
            confirmButton = {
                Button(onClick = { showStrictInsufficientDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
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
            onDismissRequest = {
                showSalaryMonthDialog = false
                pendingTransaction = null
            },
            title = { Text(stringResource(R.string.start_new_month_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(
                            R.string.start_new_month_desc,
                            DateUtils.formatDate(uiState.selectedDate)
                        )
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
                                Text(stringResource(R.string.bring_previous_balance))
                                Text(
                                    stringResource(
                                        R.string.adds_from_current_savings,
                                        formatSignedAmount(previousPeriodSaved ?: 0.0)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.no_savings_to_carry),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
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
                }) { Text(stringResource(R.string.btn_yes_start_new_month)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showSalaryMonthDialog = false
                        pendingTransaction = null
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showSalaryMonthDialog = false
                        pendingTransaction?.let { txn ->
                            coroutineScope.launch {
                                saveAndNavigate(
                                    viewModel,
                                    txn,
                                    navController,
                                    makeRecurringChecked,
                                    selectedRecurrenceFrequency
                                )
                            }
                        }
                        pendingTransaction = null
                    }) {
                        Text(stringResource(R.string.btn_no_just_income))
                    }
                }
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
                        navController.navigate(Overview) {
                            popUpTo<Overview> { inclusive = true }
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
                            onClick = {
                                viewModel.updateTransactionType(type)
                                // Reset split mode when switching to income
                                if (type == TransactionType.INCOME) viewModel.setSplitMode(false)
                            },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        // ── 3b. Split Mode Segmented Control (Expense only)
        if (uiState.transactionType == TransactionType.EXPENSE && !isEditMode) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Single Transaction tab
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (!uiState.isSplitMode)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setSplitMode(false) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (!uiState.isSplitMode)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Single",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!uiState.isSplitMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!uiState.isSplitMode)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Split Items tab
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (uiState.isSplitMode)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setSplitMode(true) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CallSplit,
                                contentDescription = null,
                                tint = if (uiState.isSplitMode)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Split Items",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (uiState.isSplitMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.isSplitMode)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Account Selector (Bank default)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.account), style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AccountType.BANK,
                    AccountType.CASH,
                    AccountType.WALLET,
                    AccountType.DEPOSIT
                ).forEach { type ->
                    FilterChip(
                        selected = uiState.accountType == type,
                        onClick = { viewModel.updateAccountType(type) },
                        leadingIcon = {
                            Icon(
                                imageVector = when (type) {
                                    AccountType.BANK -> Icons.Default.AccountBalance
                                    AccountType.CASH -> Icons.Default.Payments
                                    AccountType.WALLET -> Icons.Outlined.AccountBalanceWallet
                                    AccountType.DEPOSIT -> Icons.Outlined.Lock
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                when (type) {
                                    AccountType.BANK -> stringResource(R.string.account_bank)
                                    AccountType.CASH -> stringResource(R.string.account_cash)
                                    AccountType.WALLET -> stringResource(R.string.account_wallet)
                                    AccountType.DEPOSIT -> stringResource(R.string.account_deposit)
                                }
                            )
                        }
                    )
                }
            }

            // Wallet info card — expenses are automatically excluded from monthly totals
            if (uiState.accountType == AccountType.WALLET) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.account_wallet),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.wallet_auto_excluded_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.available_account_balance_label, stringResource(R.string.account_wallet)) + " " + CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else if (uiState.accountType == AccountType.DEPOSIT) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.deposit_vault_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.deposit_vault_buy_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.available_account_balance_label, stringResource(R.string.deposit_vault_title)) + " " + CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // ── 4b. Time-of-Day Smart Category Suggestions
        if (uiState.transactionType == TransactionType.EXPENSE) {
            val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val (suggestionLabel, suggestedKeywords) = when (currentHour) {
                in 5..11 -> "⚡ Morning:" to listOf("Cafés", "Coffee", "Transportation", "Car", "Groceries")
                in 12..16 -> "⚡ Lunch:" to listOf("Restaurants", "Cafés", "Personal Care", "Shopping")
                else -> "⚡ Evening:" to listOf("Groceries", "Restaurants", "Entertainment", "Utilities", "Bills")
            }

            val suggestedCategories = remember(filteredCategories, suggestedKeywords) {
                filteredCategories.filter { cat ->
                    suggestedKeywords.any { cat.name.contains(it, ignoreCase = true) }
                }.take(3)
            }

            if (suggestedCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = suggestionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    suggestedCategories.forEach { cat ->
                        SuggestionChip(
                            onClick = { viewModel.updateSelectedCategory(cat) },
                            label = { Text("${cat.icon} ${cat.name}") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (uiState.selectedCategory?.id == cat.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    }
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
        if (selectedCategory != null && !uiState.isSplitMode) {
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

        // ── Currency & Trip Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Currency:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CurrencyPickerDropdown(
                    selected = uiState.currency,
                    onSelected = { viewModel.updateCurrency(it) }
                )
            }

            if (uiState.availableTrips.isNotEmpty()) {
                var tripMenuExpanded by remember { mutableStateOf(false) }
                val selectedTrip = uiState.availableTrips.find { it.id == uiState.tripId }
                Box {
                    AssistChip(
                        onClick = { tripMenuExpanded = true },
                        label = {
                            Text(
                                text = selectedTrip?.let { "✈️ ${it.title}" } ?: "✈️ Tag Trip",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = tripMenuExpanded,
                        onDismissRequest = { tripMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Trip (General)") },
                            onClick = {
                                viewModel.updateTripId(null)
                                tripMenuExpanded = false
                            }
                        )
                        uiState.availableTrips.forEach { trip ->
                            DropdownMenuItem(
                                text = { Text("${trip.destination} - ${trip.title}") },
                                onClick = {
                                    viewModel.updateTripId(trip.id)
                                    tripMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── 7. Amount Field (hidden in split mode — the SplitTransactionSection owns the amount)
        if (!uiState.isSplitMode) {
            if (uiState.currency != "JOD") {
                OutlinedTextField(
                    value = uiState.originalAmount,
                    onValueChange = { viewModel.updateOriginalAmount(CurrencyUtils.cleanDecimalInput(it)) },
                    label = { Text("Amount (${uiState.currency})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                val convertedAmount = uiState.amount
                if (convertedAmount.isNotBlank() && convertedAmount.toDoubleOrNull() != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "≈ $convertedAmount JOD (converted to home ledger)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                val hasMath = ArithmeticEvaluator.hasOperator(uiState.amount)
                val evaluatedAmount = remember(uiState.amount) {
                    if (hasMath) ArithmeticEvaluator.evaluate(uiState.amount) else null
                }

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { viewModel.updateAmount(ArithmeticEvaluator.cleanArithmeticInput(it)) },
                    label = { Text("Amount (JOD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (evaluatedAmount != null) {
                            TextButton(
                                onClick = {
                                    viewModel.updateAmount(ArithmeticEvaluator.formatResult(evaluatedAmount))
                                }
                            ) {
                                Text("= " + ArithmeticEvaluator.formatResult(evaluatedAmount), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    isError = uiState.amount.isNotBlank() && uiState.amount.toDoubleOrNull() == null && evaluatedAmount == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── 7b. Split Transaction Section (Subcategories & Debts)
        // The top-level segmented control (§3b) drives isSplitMode; the internal header toggle
        // in SplitTransactionSection is kept for backward-compat but is secondary.
        if (uiState.transactionType == TransactionType.EXPENSE) {
            SplitTransactionSection(
                isSplitMode = uiState.isSplitMode,
                splits = uiState.splits,
                mainAmountStr = uiState.amount,
                availableSubCategories = savedSubCategories,
                onToggleSplitMode = { viewModel.setSplitMode(it) },
                onAddSplit = { viewModel.addSplitItem() },
                onUpdateSplit = { idx, item -> viewModel.updateSplitItem(idx, item) },
                onRemoveSplit = { idx -> viewModel.removeSplitItem(idx) }
            )
        }

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
                                CurrencyUtils.formatCurrency(warning.transactionAmount),
                                warning.categoryName,
                                CurrencyUtils.formatCurrency(warning.projectedSpent),
                                CurrencyUtils.formatCurrency(warning.limit)
                            )
                        } else {
                            stringResource(
                                R.string.budget_warning_near_desc,
                                CurrencyUtils.formatCurrency(warning.transactionAmount),
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

        // ── 9b. Receipt Photo Attachment
        ReceiptAttachmentSection(
            receiptImagePath = uiState.receiptImagePath,
            onReceiptImageSelected = { uri ->
                val path = ReceiptStorageHelper.saveReceiptImage(context, uri)
                viewModel.setReceiptImagePath(path)
            },
            onRemoveReceipt = {
                ReceiptStorageHelper.deleteReceiptImage(uiState.receiptImagePath)
                viewModel.setReceiptImagePath(null)
            }
        )

        // ── 9c. Paste Bank SMS Auto-Fill
        var showPasteSmsDialog by remember { mutableStateOf(false) }
        var pastedSmsText by remember { mutableStateOf("") }

        OutlinedButton(
            onClick = { showPasteSmsDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Paste Bank SMS (Auto-Fill)")
        }

        if (showPasteSmsDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPasteSmsDialog = false
                    pastedSmsText = ""
                },
                title = { Text("Auto-Fill from Bank SMS") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste your bank purchase or withdrawal SMS below to auto-fill amount, merchant, and category without typing:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pastedSmsText,
                            onValueChange = { pastedSmsText = it },
                            placeholder = { Text("e.g. Purchase of JOD 25.00 with card ending 1234 at Carrefour...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.fillFromBankSms(pastedSmsText, categoryList)
                            showPasteSmsDialog = false
                            pastedSmsText = ""
                            if (success) {
                                android.widget.Toast.makeText(context, "Transaction details filled from SMS!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Could not detect amount or merchant from text.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = pastedSmsText.isNotBlank()
                    ) {
                        Text("Parse & Fill")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPasteSmsDialog = false
                        pastedSmsText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
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
        val evaluatedMath = remember(uiState.amount) {
            if (ArithmeticEvaluator.hasOperator(uiState.amount)) ArithmeticEvaluator.evaluate(uiState.amount) else null
        }
        val isFormValid = (uiState.amount.toDoubleOrNull() != null || evaluatedMath != null) &&
            uiState.amount.isNotBlank() &&
            uiState.selectedCategory != null

        Button(
            onClick = {
                coroutineScope.launch {
                    val amountDouble = uiState.amount.toDoubleOrNull() ?: evaluatedMath
                    val category = uiState.selectedCategory
                    if (amountDouble == null || category == null) return@launch

                    // Save subcategory if checked
                    val subDescTrimmed = uiState.subDescription.trim()
                    if (!uiState.isSplitMode && saveAsSubCategoryChecked && subDescTrimmed.isNotBlank()) {
                        viewModel.saveSubCategory(category.id, subDescTrimmed)
                    }

                    val isDeptCat = viewModel.isDeptCategory(category)
                    val computedDebtType = if (isDeptCat) {
                        if (uiState.transactionType == TransactionType.EXPENSE) "LENT"
                        else if (uiState.isIncomeBorrowedDebt) "BORROWED"
                        else "REPAYMENT"
                    } else uiState.debtType

                    val splitSummary = if (uiState.isSplitMode) {
                        uiState.splits.filter { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                            .joinToString(", ") { item ->
                                val name = item.subCategoryName?.takeIf { it.isNotBlank() }
                                    ?: item.note?.takeIf { it.isNotBlank() }
                                    ?: "Other"
                                "$name: ${item.amount}"
                            }.takeIf { it.isNotBlank() }
                    } else null

                    val transaction = Transaction(
                        id = uiState.editingTransactionId ?: 0L,
                        amount = amountDouble,
                        description = uiState.description,
                        subDescription = if (uiState.isSplitMode) splitSummary else uiState.subDescription.takeIf { it.isNotBlank() },
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
                        createdAt = uiState.createdAt ?: Date(),
                        excludeFromMonthExpenses = uiState.excludeFromMonthExpenses || uiState.accountType == AccountType.DEPOSIT || uiState.accountType == AccountType.WALLET,
                        currency = uiState.currency,
                        originalAmount = uiState.originalAmount.toDoubleOrNull(),
                        exchangeRateUsed = uiState.exchangeRateUsed,
                        tripId = uiState.tripId
                    )

                    if (uiState.transactionType == TransactionType.EXPENSE) {
                        val balance = viewModel.getAvailableBalanceForAccount(
                            uiState.accountType,
                            uiState.editingTransactionId
                        )
                        if (amountDouble > balance) {
                            if (uiState.accountType == AccountType.WALLET || uiState.accountType == AccountType.DEPOSIT) {
                                showStrictInsufficientDialog = true
                                return@launch
                            } else {
                                pendingTransaction = transaction
                                showInsufficientDialog = true
                                return@launch
                            }
                        }
                    }

                    val performActualSave: () -> Unit = {
                        coroutineScope.launch {
                            val isSalaryIncomeInSalaryMode = monthMode == MonthMode.SALARY &&
                                uiState.transactionType == TransactionType.INCOME &&
                                viewModel.isSalaryCategory(category)

                            if (isSalaryIncomeInSalaryMode && viewModel.shouldPromptSalaryMonth(category, isEditMode)) {
                                pendingTransaction = transaction
                                showSalaryMonthDialog = true
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                saveAndNavigate(
                                    viewModel,
                                    transaction,
                                    navController,
                                    makeRecurringChecked,
                                    selectedRecurrenceFrequency
                                )
                            }
                        }
                    }

                    if (uiState.accountType == AccountType.DEPOSIT && uiState.transactionType == TransactionType.EXPENSE) {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (activity != null) {
                            val biometricManager = com.example.expensetracker.core.security.BiometricAuthManager(context.applicationContext)
                            biometricManager.authenticate(
                                activity = activity,
                                title = context.getString(R.string.vault_biometric_confirm_title),
                                subtitle = context.getString(R.string.vault_biometric_confirm_sub),
                                onSuccess = { performActualSave() },
                                onError = { /* Cancelled or auth error */ },
                                onFailedAttempt = { /* Failed biometric */ }
                            )
                        } else {
                            performActualSave()
                        }
                    } else {
                        performActualSave()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid
        ) {
            Text(if (isEditMode) "Update Transaction" else "Save Transaction")
        }

        if (!isEditMode && uiState.pendingRecurringId == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { makeRecurringChecked = !makeRecurringChecked }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.make_recurring_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.make_recurring_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = makeRecurringChecked,
                            onCheckedChange = { makeRecurringChecked = it }
                        )
                    }

                    if (makeRecurringChecked) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecurrenceFrequency.entries.forEach { freq ->
                                val label = when (freq) {
                                    RecurrenceFrequency.DAILY -> stringResource(R.string.freq_daily)
                                    RecurrenceFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
                                    RecurrenceFrequency.MONTHLY -> stringResource(R.string.freq_monthly)
                                    RecurrenceFrequency.YEARLY -> stringResource(R.string.freq_yearly)
                                }
                                FilterChip(
                                    selected = selectedRecurrenceFrequency == freq,
                                    onClick = { selectedRecurrenceFrequency = freq },
                                    label = { Text(label) },
                                    modifier = Modifier.height(34.dp)
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

private suspend fun saveAndNavigate(
    viewModel: AddEditTransactionViewModel,
    transaction: Transaction,
    navController: NavController,
    isRecurring: Boolean = false,
    recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY
) {
    viewModel.saveTransaction(transaction)
    val recurringId = viewModel.uiState.value.pendingRecurringId
    if (recurringId != null) {
        viewModel.advanceRecurringAfterSave(recurringId)
    } else if (isRecurring) {
        val calendar = Calendar.getInstance()
        calendar.time = transaction.date
        when (recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        viewModel.createRecurringTemplate(
            amount = transaction.amount,
            description = transaction.description,
            type = transaction.type,
            categoryId = transaction.categoryId,
            accountType = transaction.accountType,
            frequency = recurrenceFrequency,
            nextDueDate = calendar.time
        )
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
