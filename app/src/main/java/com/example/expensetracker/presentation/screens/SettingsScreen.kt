package com.example.expensetracker.presentation.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val salaryReminders by viewModel.activeSalaryReminder.collectAsState()
    val exportUri by viewModel.exportUri.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentCash by remember { mutableStateOf(0.0) }
    var currentBank by remember { mutableStateOf(0.0) }
    var cashInput by remember { mutableStateOf("") }
    var bankInput by remember { mutableStateOf("") }
    var justSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentCash = viewModel.getCurrentCashBalance()
        currentBank = viewModel.getCurrentBankBalance()
        cashInput = currentCash.toString()
        bankInput = currentBank.toString()
    }

    LaunchedEffect(exportUri) {
        exportUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV"))
            viewModel.clearExportUri()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Text("Balances", style = MaterialTheme.typography.titleSmall)
        Text(
            "Enter the amount of cash/bank money you actually have right now. " +
                "This recalibrates your balance so it matches reality going forward, " +
                "regardless of any income/expense history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        OutlinedTextField(
            value = cashInput,
            onValueChange = { cashInput = it; justSaved = false },
            label = { Text("Current cash balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bankInput,
            onValueChange = { bankInput = it; justSaved = false },
            label = { Text("Current bank balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.setCurrentCashBalance(cashInput.toDoubleOrNull() ?: currentCash)
                    viewModel.setCurrentBankBalance(bankInput.toDoubleOrNull() ?: currentBank)
                    currentCash = viewModel.getCurrentCashBalance()
                    currentBank = viewModel.getCurrentBankBalance()
                    cashInput = currentCash.toString()
                    bankInput = currentBank.toString()
                    justSaved = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save current balances")
        }
        if (justSaved) {
            Text(
                "Saved. Balances now reflect what you entered.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        Text("Salary reminder", style = MaterialTheme.typography.titleSmall)
        if (salaryReminders.isEmpty()) {
            Text(
                "No active salary reminder. Set one when saving a salary with Start new month.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            salaryReminders.forEach { reminder ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${reminder.description} — ${CurrencyUtils.formatCurrency(reminder.amount)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Next: ${com.example.expensetracker.presentation.theme.DateUtils.formatDate(reminder.nextDueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = { viewModel.deactivateSalaryReminder(reminder.id) }) {
                            Text("Turn off reminder", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Data", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { viewModel.exportCsv() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export CSV")
        }

        HorizontalDivider()

        Text(
            "Expense Tracker v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
