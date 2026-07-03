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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val openingCash by viewModel.openingCashBalance.collectAsState()
    val openingBank by viewModel.openingBankBalance.collectAsState()
    val salaryReminders by viewModel.activeSalaryReminder.collectAsState()
    val exportUri by viewModel.exportUri.collectAsState()
    val context = LocalContext.current

    var cashInput by remember(openingCash) { mutableStateOf(openingCash.toString()) }
    var bankInput by remember(openingBank) { mutableStateOf(openingBank.toString()) }

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

        Text("Opening balances", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = cashInput,
            onValueChange = { cashInput = it },
            label = { Text("Cash opening balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bankInput,
            onValueChange = { bankInput = it },
            label = { Text("Bank opening balance") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                viewModel.setOpeningCashBalance(cashInput.toDoubleOrNull() ?: 0.0)
                viewModel.setOpeningBankBalance(bankInput.toDoubleOrNull() ?: 0.0)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save opening balances")
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
