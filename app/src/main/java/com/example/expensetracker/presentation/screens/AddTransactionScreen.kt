package com.example.expensetracker.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.viewModel.TransactionViewModel
import kotlinx.coroutines.launch
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.theme.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController, viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val categoryList by viewModel.allCategories.collectAsState(initial = emptyList())

    val selectedDate = remember(uiState.selectedDate) { uiState.selectedDate }

    val filteredCategories = categoryList.filter { it.type == uiState.transactionType }

    // Date Picker
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text(
            "Add ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Transaction Type
        Row(Modifier.fillMaxWidth()) {
            TransactionType.values().forEach { type ->
                val selected = uiState.transactionType == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateTransactionType(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = uiState.selectedCategory?.let { "${it.icon} ${it.name}" } ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true) // TODO check delete it or keep it
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredCategories.forEach { category ->
                    DropdownMenuItem(text = {
                        Text(category.icon)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(category.name)
                    }, onClick = {
                        viewModel.updateSelectedCategory(category)
                        expanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Account Type
        Row(Modifier.fillMaxWidth()) {
            AccountType.values().forEach { type ->
                val selected = uiState.accountType == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateAccountType(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date
        Button(onClick = { datePicker.show() }) {
            Text("Date: ${DateUtils.formatDate(selectedDate)}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val amountDouble = uiState.amount.toDoubleOrNull()
                    val category = uiState.selectedCategory
                    if (amountDouble != null && category != null) {
                        val transaction = Transaction(
                            id = 0,
                            amount = amountDouble,
                            description = uiState.description,
                            categoryId = category.id,
                            type = uiState.transactionType,
                            accountType = uiState.accountType,
                            date = uiState.selectedDate
                        )
                        viewModel.insertTransaction(transaction)
                        viewModel.resetForm()
                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.amount.isNotBlank() && uiState.selectedCategory != null
        ) {
            Text("Save Transaction")
        }
    }
}