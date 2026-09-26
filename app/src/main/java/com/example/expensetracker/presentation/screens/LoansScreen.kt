package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.repository.MonthlyLoanItem
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.LoanHistoryBottomSheet
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.LIQUID_ACCOUNTS
import com.example.expensetracker.presentation.viewModel.LoansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    navController: NavController,
    viewModel: LoansViewModel = hiltViewModel()
) {
    val monthlyItems by viewModel.monthlyItems.collectAsState()
    val configuredLoans by viewModel.configuredLoans.collectAsState()
    val preservedAmount by viewModel.preservedAmount.collectAsState()

    var showAddLoanDialog by remember { mutableStateOf(false) }
    var loanToEdit by remember { mutableStateOf<ConfiguredLoan?>(null) }
    var editingPaymentItem by remember { mutableStateOf<MonthlyLoanItem?>(null) }
    var payingPaymentItem by remember { mutableStateOf<MonthlyLoanItem?>(null) }
    var historyLoanTarget by remember { mutableStateOf<ConfiguredLoan?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.loans_title),
                navController = navController,
                canNavigateBack = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    loanToEdit = null
                    showAddLoanDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_loan_config))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // ── Preserved Balance Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (preservedAmount > 0) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preserved_for_loans_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(preservedAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.preserved_for_loans_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Section 1: This Month's Commitments
            item {
                Text(
                    text = stringResource(R.string.this_months_loans),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (monthlyItems.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_loans_configured_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            } else {
                items(monthlyItems, key = { "monthly_${it.payment.id}" }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.payment.isPaid) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.payment.isPaid) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (item.payment.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                    )
                                    Column {
                                        Text(
                                            text = item.loanConfig.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (item.payment.isPaid) {
                                                stringResource(R.string.loan_paid_status)
                                            } else {
                                                stringResource(R.string.loan_pending_status)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.payment.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = CurrencyUtils.formatCurrency(item.payment.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.payment.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { historyLoanTarget = item.loanConfig },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = stringResource(R.string.history_title),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (!item.payment.isPaid) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { editingPaymentItem = item }) {
                                        Text(stringResource(R.string.edit_amount_this_month))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { payingPaymentItem = item }) {
                                        Text(stringResource(R.string.pay_deduct_btn))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 2: Configured Loans
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.recurring_loans_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (configuredLoans.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_configured_loans_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
            } else {
                items(configuredLoans, key = { "configured_${it.id}" }) { loan ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = loan.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Default: ${CurrencyUtils.formatCurrency(loan.defaultAmount)} (${loan.accountType.name})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (loan.deductFromIncome) {
                                        stringResource(R.string.loan_deducted_from_income_badge)
                                    } else {
                                        stringResource(R.string.loan_calculated_in_expenses_badge)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (loan.deductFromIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { historyLoanTarget = loan }) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = stringResource(R.string.history_title),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    loanToEdit = loan
                                    showAddLoanDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteConfiguredLoan(loan.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    historyLoanTarget?.let { loan ->
        LoanHistoryBottomSheet(
            loan = loan,
            historyFlow = remember(loan.id) { viewModel.getPaymentHistoryForLoanFlow(loan.id) },
            onDismissRequest = { historyLoanTarget = null }
        )
    }

    // ── Dialog: Add / Edit Configured Loan
    if (showAddLoanDialog) {
        var nameInput by remember { mutableStateOf(loanToEdit?.name.orEmpty()) }
        var amountInput by remember { mutableStateOf(loanToEdit?.defaultAmount?.toString().orEmpty()) }
        var selectedAccount by remember { mutableStateOf(loanToEdit?.accountType ?: AccountType.BANK) }
        var calculateInExpenses by remember { mutableStateOf(loanToEdit?.let { !it.deductFromIncome } ?: false) }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = {
                Text(
                    if (loanToEdit == null) {
                        stringResource(R.string.add_configured_loan)
                    } else {
                        stringResource(R.string.edit_configured_loan)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.loan_name_label)) },
                        placeholder = { Text("e.g. House Loan, Car Loan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text(stringResource(R.string.default_amount_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.default_account_label), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LIQUID_ACCOUNTS.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccount == acc,
                                    onClick = { selectedAccount = acc },
                                    label = { Text(if (acc == AccountType.BANK) "🏦 Bank" else "💵 Cash") }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(R.string.calculate_in_expenses_label),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.calculate_in_expenses_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = calculateInExpenses,
                            onCheckedChange = { calculateInExpenses = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (nameInput.isNotBlank() && amount > 0) {
                            viewModel.saveConfiguredLoan(
                                name = nameInput.trim(),
                                amount = amount,
                                accountType = selectedAccount,
                                deductFromIncome = !calculateInExpenses,
                                id = loanToEdit?.id ?: 0L
                            )
                            showAddLoanDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoanDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Dialog: Edit Payment Amount for This Month
    editingPaymentItem?.let { item ->
        var tempAmount by remember { mutableStateOf(item.payment.amount.toString()) }
        AlertDialog(
            onDismissRequest = { editingPaymentItem = null },
            title = { Text(stringResource(R.string.edit_month_loan_amount, item.loanConfig.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.edit_month_loan_amount_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text(stringResource(R.string.amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newAmount = tempAmount.toDoubleOrNull()
                        if (newAmount != null && newAmount > 0) {
                            viewModel.updatePaymentAmountForThisMonth(item.payment.id, newAmount)
                        }
                        editingPaymentItem = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPaymentItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── BottomSheet: Deduct / Pay Loan
    payingPaymentItem?.let { item ->
        val targetAccount = item.loanConfig.accountType
        var amountInput by remember { mutableStateOf(CurrencyUtils.cleanDecimalInput(item.payment.amount.toString())) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var availableBalance by remember { mutableStateOf<Double?>(null) }
        var isDeducting by remember { mutableStateOf(false) }

        LaunchedEffect(item.payment.id, targetAccount) {
            availableBalance = viewModel.getAvailableBalance(targetAccount)
        }

        val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
        val isInsufficient = availableBalance != null && parsedAmount > (availableBalance ?: 0.0)

        ModalBottomSheet(
            onDismissRequest = { payingPaymentItem = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (targetAccount == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = item.loanConfig.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.deducting_from_account_label,
                                if (targetAccount == AccountType.BANK) stringResource(R.string.account_bank) else stringResource(R.string.account_cash)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Account & Available Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.balance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (targetAccount == AccountType.BANK) stringResource(R.string.account_bank) else stringResource(R.string.account_cash),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Amount TextField — unfocused until user clicks inside
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = CurrencyUtils.cleanDecimalInput(it)
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isInsufficient || parsedAmount <= 0.0,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isInsufficient) {
                    val accName = if (targetAccount == AccountType.BANK) stringResource(R.string.account_bank) else stringResource(R.string.account_cash)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.loan_insufficient_balance,
                                accName,
                                CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                                CurrencyUtils.formatCurrency(parsedAmount)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { payingPaymentItem = null },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeducting
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (parsedAmount <= 0.0) {
                                errorMessage = "Please enter a valid amount"
                                return@Button
                            }
                            if (isInsufficient) {
                                return@Button
                            }

                            isDeducting = true
                            viewModel.deductAndPayLoan(
                                paymentId = item.payment.id,
                                customAmount = parsedAmount,
                                accountType = targetAccount
                            ) { success, errorMsg ->
                                isDeducting = false
                                if (success) {
                                    payingPaymentItem = null
                                } else {
                                    errorMessage = errorMsg
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isDeducting && parsedAmount > 0.0 && !isInsufficient
                    ) {
                        if (isDeducting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.confirm_deduct))
                        }
                    }
                }
            }
        }
    }
}
