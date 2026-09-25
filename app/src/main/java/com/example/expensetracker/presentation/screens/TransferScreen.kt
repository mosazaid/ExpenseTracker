package com.example.expensetracker.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.AccountType
import androidx.compose.ui.res.stringResource
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.Overview
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.viewModel.TransferUiState
import com.example.expensetracker.presentation.viewModel.TransferViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    navController: NavController,
    transactionId: Long? = null,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val isEditMode = transactionId != null

    var availableBalance by remember { mutableStateOf<Double?>(null) }
    var showInsufficientDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                com.example.expensetracker.presentation.viewModel.TransferUiEvent.NavigateBack ->
                    navController.popBackStack()
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransferForEdit(transactionId)
        }
    }

    LaunchedEffect(uiState.transferFromAccount, uiState.editingTransactionId) {
        availableBalance = viewModel.getAvailableBalanceForEdit(uiState.transferFromAccount)
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
                    "Insufficient ${uiState.transferFromAccount.name.lowercase()} balance. " +
                        "Available: ${CurrencyUtils.formatCurrency(availableBalance ?: 0.0)}. " +
                        "Allow negative balance?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInsufficientDialog = false
                    coroutineScope.launch {
                        saveTransfer(viewModel, uiState, allowNegative = true)
                        viewModel.resetForm()
                        viewModel.emitNavigateBack()
                    }
                }) { Text("Allow negative") }
            },
            dismissButton = {
                TextButton(onClick = { showInsufficientDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) stringResource(R.string.edit_transfer) else stringResource(R.string.action_transfer),
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
                .padding(16.dp)
        ) {
            Text(
                "Available ${uiState.transferFromAccount.name.lowercase()}: " +
                    CurrencyUtils.formatCurrency(availableBalance ?: 0.0),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(CurrencyUtils.cleanDecimalInput(it)) },
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Note (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("From", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.filter { it != AccountType.WALLET }.forEach { type ->
                FilterChip(
                    selected = uiState.transferFromAccount == type,
                    onClick = { viewModel.updateTransferFromAccount(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("To", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountType.entries.filter { it != AccountType.WALLET }.forEach { type ->
                FilterChip(
                    selected = uiState.transferToAccount == type,
                    onClick = { viewModel.updateTransferToAccount(type) },
                    enabled = type != uiState.transferFromAccount,
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Date: ${DateUtils.formatDate(uiState.selectedDate)}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        val amount = uiState.amount.toDoubleOrNull()
        val isValid = amount != null && amount > 0 &&
            uiState.transferFromAccount != uiState.transferToAccount

        Button(
            onClick = {
                coroutineScope.launch {
                    if (amount == null) return@launch
                    val balance = viewModel.getAvailableBalanceForEdit(uiState.transferFromAccount)
                    if (amount > balance) {
                        showInsufficientDialog = true
                    } else {
                        saveTransfer(viewModel, uiState, allowNegative = false)
                        viewModel.resetForm()
                        viewModel.emitNavigateBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isValid
        ) {
            Text(if (isEditMode) "Update Transfer" else "Transfer")
        }
    }
    }
}

private suspend fun saveTransfer(
    viewModel: TransferViewModel,
    uiState: TransferUiState,
    allowNegative: Boolean
) {
    viewModel.saveTransfer(allowNegative)
}
