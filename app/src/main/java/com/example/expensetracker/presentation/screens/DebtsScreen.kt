package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.viewModel.DebtsViewModel

@Composable
fun DebtsScreen(
    navController: NavController,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val lentDebts by viewModel.lentDebts.collectAsState()
    val borrowedDebts by viewModel.borrowedDebts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRolloverDialog by remember { mutableStateOf(false) }

    val shouldAutoShow by viewModel.shouldAutoShowRolloverDialog.collectAsState()
    LaunchedEffect(shouldAutoShow) {
        if (shouldAutoShow) {
            showRolloverDialog = true
            viewModel.dismissAutoRolloverDialog()
        }
    }

    val totalLent = lentDebts.sumOf { it.amount }
    val totalBorrowed = borrowedDebts.sumOf { it.amount }

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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.debts_owed_to_me), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(totalLent),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.debts_i_owe), style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(totalBorrowed),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Rollover to Next Month Action
            val hasOpenDebts = lentDebts.isNotEmpty() || borrowedDebts.isNotEmpty()
            if (hasOpenDebts) {
                OutlinedButton(
                    onClick = { showRolloverDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.carry_debts_to_new_month))
                }
            }

            // ── Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${stringResource(R.string.debts_owed_to_me)} (${lentDebts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${stringResource(R.string.debts_i_owe)} (${borrowedDebts.size})") }
                )
            }

            val currentList = if (selectedTab == 0) lentDebts else borrowedDebts

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) {
                            stringResource(R.string.no_lent_debts)
                        } else {
                            stringResource(R.string.no_borrowed_debts)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
                ) {
                    items(currentList, key = { it.id }) { debt ->
                        DebtItemCard(
                            debt = debt,
                            isOwedToMe = selectedTab == 0,
                            onSettle = { viewModel.settleDebt(debt.id) }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog: Select Debts to Carry Forward
    if (showRolloverDialog) {
        val allDebts = remember(lentDebts, borrowedDebts) { (lentDebts + borrowedDebts).distinctBy { it.id } }
        val selectedIds = remember { mutableStateListOf<Long>().apply { addAll(allDebts.map { it.id }) } }

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
                            selectedIds.addAll(allDebts.map { it.id })
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
                        items(allDebts, key = { "debt_rollover_${it.id}" }) { item ->
                            val isLent = item.awaitingReimbursement
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
                                    checked = selectedIds.contains(item.id),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.debtorNote?.ifBlank { item.description } ?: item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$typeLabel • ${CurrencyUtils.formatCurrency(item.amount)}",
                                        style = MaterialTheme.typography.labelSmall,
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
    debt: Transaction,
    isOwedToMe: Boolean,
    onSettle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = debt.debtorNote?.ifBlank { debt.description } ?: debt.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${DateUtils.formatDate(debt.date)} • ${debt.accountType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = CurrencyUtils.formatCurrency(debt.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOwedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Button(onClick = onSettle) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.mark_settled_btn))
            }
        }
    }
}
