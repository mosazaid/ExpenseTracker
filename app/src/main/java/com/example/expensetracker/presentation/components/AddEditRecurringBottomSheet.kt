package com.example.expensetracker.presentation.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.TransactionType
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringBottomSheet(
    initialItem: RecurringTransaction? = null,
    categories: List<Category>,
    onSave: (
        id: Long,
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountType: AccountType,
        frequency: RecurrenceFrequency,
        nextDueDate: Date
    ) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = initialItem != null

    var description by remember(initialItem) {
        mutableStateOf(initialItem?.description ?: "")
    }
    var amountStr by remember(initialItem) {
        mutableStateOf(
            if (initialItem != null) CurrencyUtils.cleanDecimalInput(initialItem.amount.toString())
            else ""
        )
    }
    var selectedType by remember(initialItem) {
        mutableStateOf(initialItem?.type ?: TransactionType.EXPENSE)
    }
    var selectedCategoryId by remember(initialItem) {
        mutableStateOf(initialItem?.categoryId)
    }
    var selectedAccount by remember(initialItem) {
        mutableStateOf(initialItem?.accountType ?: AccountType.BANK)
    }
    var selectedFrequency by remember(initialItem) {
        mutableStateOf(initialItem?.frequency ?: RecurrenceFrequency.MONTHLY)
    }
    var selectedDate by remember(initialItem) {
        mutableStateOf(initialItem?.nextDueDate ?: Date())
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Auto-select first matching category if current selection is not in list
    LaunchedEffect(filteredCategories, selectedCategoryId) {
        if (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id
        }
    }

    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember(context, selectedDate) {
        calendar.time = selectedDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDate = cal.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(
                            if (isEditMode) R.string.edit_recurring_title
                            else R.string.add_recurring_title
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.recurring_sheet_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Error display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Type Selector (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { selectedType = TransactionType.EXPENSE },
                    label = {
                        Text(
                            text = stringResource(R.string.expense),
                            fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { selectedType = TransactionType.INCOME },
                    label = {
                        Text(
                            text = stringResource(R.string.income),
                            fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
            }

            // Description / Name
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text(stringResource(R.string.recurring_name_label)) },
                placeholder = { Text(stringResource(R.string.recurring_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Amount
            OutlinedTextField(
                value = amountStr,
                onValueChange = { input ->
                    amountStr = CurrencyUtils.cleanDecimalInput(input)
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text(stringResource(R.string.recurring_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Selector
            if (filteredCategories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.recurring_category_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }
                }
            }

            // Account Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_account_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedAccount == AccountType.BANK,
                        onClick = { selectedAccount = AccountType.BANK },
                        label = { Text(stringResource(R.string.account_bank)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )
                    FilterChip(
                        selected = selectedAccount == AccountType.CASH,
                        onClick = { selectedAccount = AccountType.CASH },
                        label = { Text(stringResource(R.string.account_cash)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )
                }
            }

            // Frequency Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_frequency_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceFrequency.entries.forEach { freq ->
                        val label = when (freq) {
                            RecurrenceFrequency.DAILY -> stringResource(R.string.freq_daily)
                            RecurrenceFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
                            RecurrenceFrequency.MONTHLY -> stringResource(R.string.freq_monthly)
                            RecurrenceFrequency.YEARLY -> stringResource(R.string.freq_yearly)
                        }
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = { selectedFrequency = freq },
                            label = { Text(label) },
                            modifier = Modifier.height(38.dp)
                        )
                    }
                }
            }

            // Next Due Date
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.recurring_start_date_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateUtils.formatDate(selectedDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.recurring_start_date_label),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val trimmedDesc = description.trim()
                        if (trimmedDesc.isEmpty()) {
                            errorMessage = context.getString(R.string.recurring_error_empty_description)
                            return@Button
                        }
                        val amount = amountStr.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            errorMessage = context.getString(R.string.recurring_error_invalid_amount)
                            return@Button
                        }

                        onSave(
                            initialItem?.id ?: 0L,
                            trimmedDesc,
                            amount,
                            selectedType,
                            selectedCategoryId,
                            selectedAccount,
                            selectedFrequency,
                            selectedDate
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
