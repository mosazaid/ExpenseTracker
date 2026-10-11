package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.security.BiometricAuthManager
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.viewModel.DepositFilter
import com.example.expensetracker.presentation.viewModel.DepositUiEvent
import com.example.expensetracker.presentation.viewModel.DepositViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    navController: NavController,
    viewModel: DepositViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val enableWalletTransfer by viewModel.enableDepositWalletTransfer.collectAsState()
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showDepositDialog by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    val biometricManager = remember { BiometricAuthManager(context.applicationContext) }
    val promptTitle = stringResource(R.string.deposit_vault_security_prompt)
    val promptSub = stringResource(R.string.deposit_vault_security_sub)

    // Trigger biometric authentication on entry if locked
    LaunchedEffect(uiState.isUnlocked) {
        if (!uiState.isUnlocked && activity != null) {
            biometricManager.authenticate(
                activity = activity,
                title = promptTitle,
                subtitle = promptSub,
                onSuccess = { viewModel.unlock() },
                onError = { /* Keep locked */ },
                onFailedAttempt = { /* Failed attempt */ }
            )
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.deposit_vault_title),
                navController = navController,
                canNavigateBack = true,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        if (!uiState.isUnlocked) {
            // Locked screen view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.deposit_vault_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = stringResource(R.string.deposit_vault_locked_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (activity != null) {
                                    biometricManager.authenticate(
                                        activity = activity,
                                        title = promptTitle,
                                        subtitle = promptSub,
                                        onSuccess = { viewModel.unlock() },
                                        onError = { /* Keep locked */ },
                                        onFailedAttempt = { /* Failed */ }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.deposit_vault_security_prompt))
                        }
                    }
                }
            }
        } else {
            // Unlocked Great Wallet Dashboard
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
            ) {
                // ── Vault Balance Master Card ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.deposit_vault_balance),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleBalanceVisibility() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Balance Visibility",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            val displayBalance = if (uiState.isBalanceHidden) "••••••••"
                            else CurrencyUtils.formatCurrency(uiState.summary.currentBalance)

                            Text(
                                text = displayBalance,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.deposit_vault_total_deposited),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.isBalanceHidden) "••••••"
                                        else CurrencyUtils.formatCurrency(uiState.summary.totalDeposited),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.deposit_vault_total_spent),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.isBalanceHidden) "••••••"
                                        else CurrencyUtils.formatCurrency(uiState.summary.totalCapitalPurchases),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Action Buttons ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDepositDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.deposit_vault_action_deposit))
                        }

                        OutlinedButton(
                            onClick = {
                                navController.navigate(
                                    com.example.expensetracker.presentation.navigation.AddTransaction(
                                        accountType = AccountType.DEPOSIT.name
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.deposit_vault_action_buy))
                        }
                    }
                }

                // Transfer between Wallet and Deposit (Controlled by config)
                if (enableWalletTransfer) {
                    item {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTransferDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.deposit_vault_action_transfer),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pocket Wallet Available: ${CurrencyUtils.formatCurrency(uiState.walletBalance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Filter Chips ──
                item {
                    Text(
                        text = "Vault Transactions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DepositFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = {
                                    Text(
                                        when (filter) {
                                            DepositFilter.ALL -> "All"
                                            DepositFilter.PURCHASES -> "Purchases"
                                            DepositFilter.INFLOWS -> "Deposits"
                                            DepositFilter.TRANSFERS -> "Transfers"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // ── Transactions List ──
                if (uiState.filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No vault transactions yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Deposit money into your Great Wallet to build reserves outside regular circulation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.filteredTransactions, key = { it.id }) { txn ->
                        DepositTransactionCard(txn)
                    }
                }
            }
        }
    }

    // ── Dialog: Deposit Funds ──
    if (showDepositDialog) {
        DepositFundsDialog(
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount, account, note ->
                viewModel.depositFunds(amount, account, note)
                showDepositDialog = false
            }
        )
    }

    // ── Dialog: Buy from Vault ──
    if (showBuyDialog) {
        BuyFromVaultDialog(
            categories = categories,
            onDismiss = { showBuyDialog = false },
            onConfirm = { amount, desc, catId ->
                viewModel.buyFromDeposit(amount, desc, catId)
                showBuyDialog = false
            }
        )
    }

    // ── Dialog: Transfer Between Wallet and Vault ──
    if (showTransferDialog) {
        WalletVaultTransferDialog(
            vaultBalance = uiState.summary.currentBalance,
            walletBalance = uiState.walletBalance,
            onDismiss = { showTransferDialog = false },
            onTransferToWallet = { amount, note ->
                viewModel.transferToWallet(amount, note)
                showTransferDialog = false
            },
            onTransferFromWallet = { amount, note ->
                viewModel.transferFromWallet(amount, note)
                showTransferDialog = false
            }
        )
    }
}

@Composable
fun DepositTransactionCard(txn: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = when (txn.type) {
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (txn.type) {
                            TransactionType.EXPENSE -> Icons.Default.ShoppingBag
                            TransactionType.INCOME -> Icons.Default.Add
                            else -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (txn.type) {
                            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                            TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description.ifBlank { "Vault Transaction" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${DateUtils.formatDate(txn.date)} • ${
                        when (txn.type) {
                            TransactionType.EXPENSE -> "Capital Purchase"
                            TransactionType.INCOME -> "Deposit Inflow"
                            else -> "Vault Transfer"
                        }
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = (if (txn.type == TransactionType.EXPENSE) "-" else "+") + CurrencyUtils.formatCurrency(txn.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (txn.type) {
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

@Composable
fun DepositFundsDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, sourceAccount: AccountType, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(AccountType.BANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deposit_vault_action_deposit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyUtils.cleanDecimalInput(it) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Source Account", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AccountType.BANK, AccountType.CASH, AccountType.DEPOSIT).forEach { acct ->
                        FilterChip(
                            selected = selectedSource == acct,
                            onClick = { selectedSource = acct },
                            label = {
                                Text(
                                    when (acct) {
                                        AccountType.BANK -> "Bank"
                                        AccountType.CASH -> "Cash"
                                        AccountType.DEPOSIT -> "Direct Injection"
                                        AccountType.WALLET -> "Wallet"
                                    }
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Description / Source Note") },
                    placeholder = { Text("e.g. Savings allocation, bonus") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, selectedSource, note)
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BuyFromVaultDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String, categoryId: Long?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deposit_vault_action_buy)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.deposit_vault_buy_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyUtils.cleanDecimalInput(it) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Asset / Purchase Name") },
                    placeholder = { Text("e.g. Real estate share, gold, laptop") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, description, selectedCategoryId)
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WalletVaultTransferDialog(
    vaultBalance: Double,
    walletBalance: Double,
    onDismiss: () -> Unit,
    onTransferToWallet: (amount: Double, note: String) -> Unit,
    onTransferFromWallet: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var toWallet by remember { mutableStateOf(true) } // true: Vault -> Wallet; false: Wallet -> Vault

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Funds") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = toWallet,
                        onClick = { toWallet = true },
                        label = { Text("Vault → Wallet") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !toWallet,
                        onClick = { toWallet = false },
                        label = { Text("Wallet → Vault") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = if (toWallet) "Available in Vault: ${CurrencyUtils.formatCurrency(vaultBalance)}"
                    else "Available in Wallet: ${CurrencyUtils.formatCurrency(walletBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyUtils.cleanDecimalInput(it) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        if (toWallet) onTransferToWallet(amt, note)
                        else onTransferFromWallet(amt, note)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
