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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.preferences.MonthMode
import androidx.compose.ui.res.stringResource
import com.example.expensetracker.R
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils
import com.example.expensetracker.presentation.viewModel.LIQUID_ACCOUNTS
import com.example.expensetracker.presentation.viewModel.WalletDirection
import com.example.expensetracker.presentation.viewModel.WalletMoveValidation
import com.example.expensetracker.presentation.viewModel.WalletViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    transactionId: Long? = null,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthMode by viewModel.monthMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val isEditMode = transactionId != null

    var cashBalance by remember { mutableStateOf<Double?>(null) }
    var bankBalance by remember { mutableStateOf<Double?>(null) }
    var walletBalance by remember { mutableStateOf<Double?>(null) }
    var maxAllowedAmount by remember { mutableStateOf<Double?>(null) }
    var dateInOpenPeriod by remember { mutableStateOf(true) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var blockedMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                com.example.expensetracker.presentation.viewModel.WalletUiEvent.NavigateBack ->
                    navController.popBackStack()
            }
        }
    }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadWalletMoveForEdit(transactionId)
        }
    }

    LaunchedEffect(
        uiState.liquidAccount,
        uiState.direction,
        uiState.selectedDate,
        uiState.editingTransactionId,
        uiState.amount
    ) {
        cashBalance = viewModel.getAvailableLiquidBalance(AccountType.CASH)
        bankBalance = viewModel.getAvailableLiquidBalance(AccountType.BANK)
        walletBalance = viewModel.getWalletBalanceForSelectedPeriod()
        maxAllowedAmount = viewModel.getMaxAllowedAmount()
        dateInOpenPeriod = viewModel.isSelectedDateInOpenPeriod()
    }

    val isSalaryMode = monthMode == MonthMode.SALARY
    val canSave = isSalaryMode && dateInOpenPeriod

    val amount = uiState.amount.toDoubleOrNull()
    val exceedsAvailable = amount != null && maxAllowedAmount != null && amount > maxAllowedAmount!!

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

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text("Cannot move this amount") },
            text = { Text(blockedMessage) },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) stringResource(R.string.edit_wallet_move) else stringResource(R.string.action_wallet),
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
                .padding(16.dp)
        ) {

        if (!isSalaryMode) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "Wallet is only available in Salary month mode. Switch to Salary month in History.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (isSalaryMode && !dateInOpenPeriod) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    "This date is in a closed salary month. Wallet moves cannot be added or changed for closed months.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LIQUID_ACCOUNTS.forEach { acct ->
                val bal = if (acct == AccountType.CASH) cashBalance else bankBalance
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            acct.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (bal != null) CurrencyUtils.formatCurrency(bal) else "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (bal != null && bal < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Wallet this month", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Resets each salary month — not carried forward",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    if (walletBalance != null) CurrencyUtils.formatCurrency(walletBalance!!) else "…",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (walletBalance != null && walletBalance!! < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Direction", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.direction == WalletDirection.TO_WALLET,
                onClick = { viewModel.updateDirection(WalletDirection.TO_WALLET) },
                label = { Text("To wallet") }
            )
            FilterChip(
                selected = uiState.direction == WalletDirection.FROM_WALLET,
                onClick = { viewModel.updateDirection(WalletDirection.FROM_WALLET) },
                label = { Text("From wallet") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (uiState.direction == WalletDirection.TO_WALLET) "From account" else "To account",
            style = MaterialTheme.typography.labelMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LIQUID_ACCOUNTS.forEach { type ->
                FilterChip(
                    selected = uiState.liquidAccount == type,
                    onClick = { viewModel.updateLiquidAccount(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val maxLabel = when (uiState.direction) {
            WalletDirection.TO_WALLET ->
                "Max from ${uiState.liquidAccount.name.lowercase()}: " +
                    (maxAllowedAmount?.let { CurrencyUtils.formatCurrency(it) } ?: "…")
            WalletDirection.FROM_WALLET ->
                "Max from wallet: " +
                    (maxAllowedAmount?.let { CurrencyUtils.formatCurrency(it) } ?: "…")
        }

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(CurrencyUtils.cleanDecimalInput(it)) },
            label = { Text("Amount") },
            supportingText = {
                Text(
                    maxLabel,
                    color = if (exceedsAvailable) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline
                )
            },
            isError = exceedsAvailable,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Note (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { datePicker.show() },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave || isEditMode
        ) {
            Text("Date: ${DateUtils.formatDate(uiState.selectedDate)}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isValid = amount != null && amount > 0 && canSave && !exceedsAvailable

        Button(
            onClick = {
                coroutineScope.launch {
                    when (val validation = viewModel.validateWalletMove()) {
                        is WalletMoveValidation.Valid -> {
                            if (viewModel.saveWalletMove() >= 0L) {
                                viewModel.resetForm()
                                viewModel.emitNavigateBack()
                            }
                        }
                        is WalletMoveValidation.Invalid -> {
                            blockedMessage = validation.message
                            showBlockedDialog = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isValid
        ) {
            Text(if (isEditMode) "Update wallet move" else "Move money")
        }
    }
    }
}
