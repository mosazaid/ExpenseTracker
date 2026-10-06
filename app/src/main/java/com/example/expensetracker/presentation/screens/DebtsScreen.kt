package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.DebtItemUiModel
import com.example.expensetracker.presentation.viewModel.DebtsViewModel

@Composable
fun DebtsScreen(
    navController: NavController,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val lentDebtItems by viewModel.lentDebtItems.collectAsState()
    val borrowedDebtItems by viewModel.borrowedDebtItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRolloverDialog by remember { mutableStateOf(false) }

    var paymentTargetItem by remember { mutableStateOf<DebtItemUiModel?>(null) }
    var forgiveTargetItem by remember { mutableStateOf<DebtItemUiModel?>(null) }

    val shouldAutoShow by viewModel.shouldAutoShowRolloverDialog.collectAsState()
    LaunchedEffect(shouldAutoShow) {
        if (shouldAutoShow) {
            showRolloverDialog = true
            viewModel.dismissAutoRolloverDialog()
        }
    }

    val totalLentRemaining = lentDebtItems.sumOf { it.remainingAmount }
    val totalBorrowedRemaining = borrowedDebtItems.sumOf { it.remainingAmount }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.debts_tracker_title),
                navController = navController,
                canNavigateBack = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Financial KPI summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                stringResource(R.string.debts_owed_to_me),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatCurrency(totalLentRemaining),
                            style = MaterialTheme.typography.titleLarge.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${lentDebtItems.size} active debts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallReceived,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                stringResource(R.string.debts_i_owe),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatCurrency(totalBorrowedRemaining),
                            style = MaterialTheme.typography.titleLarge.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${borrowedDebtItems.size} active debts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── Rollover to Next Month Action
            val hasOpenDebts = lentDebtItems.isNotEmpty() || borrowedDebtItems.isNotEmpty()
            if (hasOpenDebts) {
                OutlinedButton(
                    onClick = { showRolloverDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.carry_debts_to_new_month))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${stringResource(R.string.debts_owed_to_me)} (${lentDebtItems.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${stringResource(R.string.debts_i_owe)} (${borrowedDebtItems.size})") }
                )
            }

            val currentList = if (selectedTab == 0) lentDebtItems else borrowedDebtItems

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = FinancePositive,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (selectedTab == 0) {
                                stringResource(R.string.no_lent_debts)
                            } else {
                                stringResource(R.string.no_borrowed_debts)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
                ) {
                    items(currentList, key = { it.transaction.id }) { item ->
                        DebtItemCard(
                            item = item,
                            onPay = { paymentTargetItem = item },
                            onForgive = { forgiveTargetItem = item }
                        )
                    }
                }
            }
        }
    }

    // ── Payment Dialog
    paymentTargetItem?.let { item ->
        RecordDebtPaymentDialog(
            item = item,
            onDismiss = { paymentTargetItem = null },
            onConfirm = { amount, account ->
                viewModel.recordDebtPayment(
                    debtId = item.transaction.id,
                    paymentAmount = amount,
                    accountType = account,
                    onComplete = { paymentTargetItem = null }
                )
            }
        )
    }

    // ── Forgive / Settle Confirmation Dialog
    forgiveTargetItem?.let { item ->
        AlertDialog(
            onDismissRequest = { forgiveTargetItem = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.VolunteerActivism,
                    contentDescription = null,
                    tint = FinancePositive,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.debt_forgive_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.debt_forgive_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forgiveDebt(item.transaction.id) {
                            forgiveTargetItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinancePositive
                    )
                ) {
                    Text(stringResource(R.string.debt_forgive_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { forgiveTargetItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ── Dialog: Select Debts to Carry Forward
    if (showRolloverDialog) {
        val allDebts = remember(lentDebtItems, borrowedDebtItems) {
            (lentDebtItems + borrowedDebtItems).distinctBy { it.transaction.id }
        }
        val selectedIds = remember {
            mutableStateListOf<Long>().apply { addAll(allDebts.map { it.transaction.id }) }
        }

        AlertDialog(
            onDismissRequest = { showRolloverDialog = false },
            title = { Text(stringResource(R.string.carry_forward_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.carry_forward_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            selectedIds.clear()
                            selectedIds.addAll(allDebts.map { it.transaction.id })
                        }) {
                            Text(stringResource(R.string.select_all))
                        }
                        TextButton(onClick = { selectedIds.clear() }) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allDebts, key = { "debt_rollover_${it.transaction.id}" }) { item ->
                            val isLent = item.isOwedToMe
                            val typeLabel = if (isLent) {
                                stringResource(R.string.debt_type_lent_short)
                            } else {
                                stringResource(R.string.debt_type_borrowed_short)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(item.transaction.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(item.transaction.id) else selectedIds.remove(item.transaction.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.debtorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$typeLabel • ${CurrencyUtils.formatCurrency(item.remainingAmount)}",
                                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                        color = if (isLent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.carryOverDebtsToNewMonth(selectedIds.toList())
                    showRolloverDialog = false
                }) {
                    Text(stringResource(R.string.carry_forward_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRolloverDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DebtItemCard(
    item: DebtItemUiModel,
    onPay: () -> Unit,
    onForgive: () -> Unit
) {
    val progress = if (item.totalAmount > 0.0) {
        (item.paidAmount / item.totalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Debtor/Creditor Name and Original Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.debtorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${DateUtils.formatDate(item.transaction.date)} • ${item.transaction.accountType.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Remaining Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isOwedToMe) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    }
                ) {
                    Text(
                        text = CurrencyUtils.formatCurrency(item.remainingAmount),
                        style = MaterialTheme.typography.titleSmall.withTabularNums(),
                        fontWeight = FontWeight.Bold,
                        color = if (item.isOwedToMe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress bar and breakdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.isOwedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(
                            R.string.debt_paid_progress,
                            CurrencyUtils.formatCurrency(item.paidAmount),
                            CurrencyUtils.formatCurrency(item.totalAmount)
                        ),
                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = stringResource(
                            R.string.debt_remaining_label,
                            CurrencyUtils.formatCurrency(item.remainingAmount)
                        ),
                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isOwedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Forgive / Settle button
                OutlinedButton(
                    onClick = onForgive,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolunteerActivism,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FinancePositive
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.debt_forgive_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pay / Record Payment button
                Button(
                    onClick = onPay,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.debt_pay_action),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordDebtPaymentDialog(
    item: DebtItemUiModel,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, account: AccountType) -> Unit
) {
    var amountText by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", item.remainingAmount))
    }
    var selectedAccount by remember { mutableStateOf(AccountType.CASH) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.debt_record_payment_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.debt_record_payment_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Debtor & Remaining Summary Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.debtorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stringResource(R.string.debt_remaining_label, CurrencyUtils.formatCurrency(item.remainingAmount))}",
                            style = MaterialTheme.typography.labelMedium.withTabularNums(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.debt_payment_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Chip: Pay Full Remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SuggestionChip(
                        onClick = {
                            amountText = String.format(java.util.Locale.US, "%.2f", item.remainingAmount)
                            errorMessage = null
                        },
                        label = {
                            Text(
                                stringResource(
                                    R.string.debt_quick_full_pay,
                                    CurrencyUtils.formatCurrency(item.remainingAmount)
                                ),
                                style = MaterialTheme.typography.labelSmall.withTabularNums()
                            )
                        }
                    )
                }

                // Account Selection: Cash vs Bank
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.debt_payment_account_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedAccount == AccountType.CASH,
                            onClick = { selectedAccount = AccountType.CASH },
                            label = { Text("Cash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedAccount == AccountType.BANK,
                            onClick = { selectedAccount = AccountType.BANK },
                            label = { Text("Bank") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entered = amountText.toDoubleOrNull()
                    if (entered == null || entered <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (entered > item.remainingAmount + 0.001) {
                        errorMessage = "Amount cannot exceed remaining balance (${CurrencyUtils.formatCurrency(item.remainingAmount)})"
                        return@Button
                    }
                    onConfirm(entered, selectedAccount)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
