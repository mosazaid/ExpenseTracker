package com.example.expensetracker.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import com.example.expensetracker.presentation.viewModel.AddEditTransactionViewModel
import com.example.expensetracker.presentation.viewModel.LIQUID_ACCOUNTS
import kotlinx.coroutines.launch
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils

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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                com.example.expensetracker.presentation.viewModel.AddEditUiEvent.NavigateHistory -> {
                    navController.navigate(AppRoutes.HISTORY) {
                        popUpTo(AppRoutes.HISTORY) { inclusive = true }
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

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            viewModel.updateSelectedDate(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            if (isEditMode) {
                "Edit ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
            } else {
                "Add ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}"
            },
            style = MaterialTheme.typography.titleLarge
        )

        if (!isEditMode) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { navController.navigate(AppRoutes.TRANSFER) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer Cash ↔ Bank")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { navController.navigate(AppRoutes.WALLET) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wallet ↔ Cash/Bank")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Your money now",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Same balances shown in History month summary",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Always show both account balances so the user can see the full picture
        // before choosing which account to debit.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LIQUID_ACCOUNTS.forEach { acct ->
                val bal = if (acct == AccountType.CASH) cashBalance else bankBalance
                val isSelected = uiState.accountType == acct
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            acct.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (bal != null) CurrencyUtils.formatCurrency(bal) else "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                bal == null -> MaterialTheme.colorScheme.outline
                                bal < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = uiState.amount.isNotBlank() && uiState.amount.toDoubleOrNull() == null,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.subDescription,
            onValueChange = { viewModel.updateSubDescription(it) },
            label = { Text(stringResource(R.string.sub_description)) },
            placeholder = { Text(stringResource(R.string.sub_description_hint)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!isEditMode) {
            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.filter { it != TransactionType.TRANSFER }.forEach { type ->
                    FilterChip(
                        selected = uiState.transactionType == type,
                        onClick = { viewModel.updateTransactionType(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
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
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
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

        val selectedCategory = uiState.selectedCategory
        if (selectedCategory != null) {
            var isDept by remember { mutableStateOf(false) }
            LaunchedEffect(selectedCategory) {
                isDept = viewModel.isDeptCategory(selectedCategory)
            }
            if (isDept) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.debtorNote,
                    onValueChange = { viewModel.updateDebtorNote(it) },
                    label = { Text("Who owes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                            "${it.description.ifBlank { "Expense" }} — ${CurrencyUtils.formatCurrency(it.amount)}"
                        } ?: "Link to expense (optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to expense") },
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
                            text = { Text("None") },
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
                                            expense.description.ifBlank { "Expense" } + " — " +
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
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.transactionType == TransactionType.EXPENSE) {
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.debtorNote,
                    onValueChange = { viewModel.updateDebtorNote(it) },
                    label = { Text("Who owes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Account", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LIQUID_ACCOUNTS.forEach { type ->
                FilterChip(
                    selected = uiState.accountType == type,
                    onClick = { viewModel.updateAccountType(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Date: ${DateUtils.formatDate(uiState.selectedDate)}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isFormValid = uiState.amount.toDoubleOrNull() != null &&
            uiState.amount.isNotBlank() &&
            uiState.selectedCategory != null

        Button(
            onClick = {
                coroutineScope.launch {
                    val amountDouble = uiState.amount.toDoubleOrNull()
                    val category = uiState.selectedCategory
                    if (amountDouble == null || category == null) return@launch

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
                            uiState.awaitingReimbursement,
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
