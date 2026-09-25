package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
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
import com.example.expensetracker.presentation.theme.CurrencyUtils
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
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(monthlyItems, key = { it.payment.id }) { item ->
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

                                Text(
                                    text = CurrencyUtils.formatCurrency(item.payment.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.payment.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
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
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(configuredLoans, key = { it.id }) { loan ->
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
                            }
                            Row {
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

    // ── Dialog: Add / Edit Configured Loan
    if (showAddLoanDialog) {
        var nameInput by remember { mutableStateOf(loanToEdit?.name.orEmpty()) }
        var amountInput by remember { mutableStateOf(loanToEdit?.defaultAmount?.toString().orEmpty()) }
        var selectedAccount by remember { mutableStateOf(loanToEdit?.accountType ?: AccountType.BANK) }

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

    // ── Dialog: Deduct / Pay Loan
    payingPaymentItem?.let { item ->
        var selectedAccount by remember { mutableStateOf(item.payment.accountType) }
        AlertDialog(
            onDismissRequest = { payingPaymentItem = null },
            title = { Text(stringResource(R.string.confirm_loan_deduction_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(
                            R.string.confirm_loan_deduction_desc,
                            item.loanConfig.name,
                            CurrencyUtils.formatCurrency(item.payment.amount)
                        )
                    )
                    Text(stringResource(R.string.deduct_from_account), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LIQUID_ACCOUNTS.forEach { acc ->
                            FilterChip(
                                selected = selectedAccount == acc,
                                onClick = { selectedAccount = acc },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (acc == AccountType.BANK) Icons.Default.AccountBalance else Icons.Default.Payments,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(if (acc == AccountType.BANK) stringResource(R.string.account_bank) else stringResource(R.string.account_cash)) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deductAndPayLoan(item.payment.id, selectedAccount)
                        payingPaymentItem = null
                    }
                ) {
                    Text(stringResource(R.string.confirm_deduct))
                }
            },
            dismissButton = {
                TextButton(onClick = { payingPaymentItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
