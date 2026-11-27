package com.mousa.mizani.presentation.feature.settings.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.presentation.common.AppTopBar
import kotlinx.coroutines.launch

private val ShownAccounts = listOf(AccountType.CASH, AccountType.BANK)

@Composable
fun AccountBalancesScreen(
    navController: NavController,
    viewModel: AccountBalancesViewModel = hiltViewModel()
) {
    val balances by viewModel.balances.collectAsState()
    var dialog by rememberSaveable(stateSaver = CorrectionDialogStateSaver) {
        mutableStateOf<CorrectionDialogState?>(null)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    ErrorSnackbars(viewModel, snackbarHostState)
    // Balances change on other screens, so they are read again each time this page returns.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_accounts_balances),
                navController = navController,
                canNavigateBack = true
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShownAccounts.forEach { account ->
                AccountBalanceCard(
                    account = account,
                    balance = balances[account],
                    onCorrect = { dialog = CorrectionDialogState(account) }
                )
            }
        }
    }

    dialog?.let { open ->
        val decimals = shownDecimals(CurrencyUtils.activeCurrencyCode)
        CorrectBalanceDialog(
            account = open.account,
            input = open.input,
            // Computed on every composition, so a reloaded balance updates the preview.
            preview = previewCorrection(open, balances, decimals),
            decimals = decimals,
            onInputChange = { dialog = dialog?.typed(it) },
            onConfirm = { change ->
                // Reads the current state, which the first tap has already cleared.
                confirmCorrection(dialog, change)?.let {
                    dialog = null
                    viewModel.apply(it.account, it.change)
                }
            },
            onDismiss = { dialog = null }
        )
    }
}

@Composable
private fun ErrorSnackbars(viewModel: AccountBalancesViewModel, hostState: SnackbarHostState) {
    val context = LocalContext.current
    LaunchedEffect(viewModel, hostState) {
        viewModel.errors.collect { messageRes ->
            // showSnackbar waits until the message is gone, which must not hold up collection.
            launch { hostState.showSnackbar(context.getString(messageRes)) }
        }
    }
}

/** [balance] is null while loading and after a failed load; it cannot be corrected then. */
@Composable
private fun AccountBalanceCard(account: AccountType, balance: Double?, onCorrect: () -> Unit) {
    val name = stringResource(account.correctionLabelRes)
    val buttonDescription = stringResource(R.string.correct_balance_title, name)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            if (balance == null) {
                Text(
                    text = stringResource(R.string.balance_not_available),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                AmountText(
                    amount = CurrencyUtils.formatCurrency(balance),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            OutlinedButton(
                onClick = onCorrect,
                enabled = balance != null,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = buttonDescription }
            ) {
                Text(stringResource(R.string.correct_balance))
            }
        }
    }
}
