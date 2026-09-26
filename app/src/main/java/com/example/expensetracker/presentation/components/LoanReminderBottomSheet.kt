package com.example.expensetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.repository.MonthlyLoanItem
import com.example.expensetracker.core.format.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanReminderBottomSheet(
    unpaidLoans: List<MonthlyLoanItem>,
    onPayLoansClick: (amounts: Map<Long, Double>, onResult: (success: Boolean, errorMsg: String?) -> Unit) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Amounts state keyed by payment ID. Defaults to the current payment amount.
    // Notice: NO auto-focus is requested, so TextFields remain unfocused until the user explicitly taps them.
    val amountsState = remember(unpaidLoans) {
        mutableStateMapOf<Long, String>().apply {
            unpaidLoans.forEach {
                put(it.payment.id, CurrencyUtils.cleanDecimalInput(it.payment.amount.toString()))
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val totalPreserved = remember(amountsState, unpaidLoans) {
        unpaidLoans.sumOf { item ->
            amountsState[item.payment.id]?.toDoubleOrNull() ?: item.payment.amount
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.loan_reminder_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.loan_reminder_sheet_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            if (errorMessage != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            unpaidLoans.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.loanConfig.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.payment.accountType == AccountType.BANK) {
                                            Icons.Default.AccountBalance
                                        } else {
                                            Icons.Default.Payments
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = if (item.payment.accountType == AccountType.BANK) {
                                            stringResource(R.string.account_bank)
                                        } else {
                                            stringResource(R.string.account_cash)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        val currentText = amountsState[item.payment.id] ?: item.payment.amount.toString()
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { input ->
                                errorMessage = null
                                amountsState[item.payment.id] = CurrencyUtils.cleanDecimalInput(input)
                            },
                            label = { Text(stringResource(R.string.amount)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = currentText.toDoubleOrNull() == null || (currentText.toDoubleOrNull() ?: 0.0) <= 0.0,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.total_preserved_amount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(totalPreserved),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                ) {
                    Text(stringResource(R.string.remind_later))
                }
                Button(
                    onClick = {
                        val parsedAmounts = mutableMapOf<Long, Double>()
                        var hasInvalid = false
                        unpaidLoans.forEach { item ->
                            val amt = amountsState[item.payment.id]?.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                hasInvalid = true
                            } else {
                                parsedAmounts[item.payment.id] = amt
                            }
                        }

                        if (hasInvalid) {
                            errorMessage = "Please enter valid amounts for all loans."
                            return@Button
                        }

                        isProcessing = true
                        errorMessage = null

                        onPayLoansClick(parsedAmounts) { success, errorMsg ->
                            isProcessing = false
                            if (!success) {
                                errorMessage = errorMsg
                            } else {
                                onDismissRequest()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && totalPreserved > 0
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.pay_loans_btn))
                    }
                }
            }
        }
    }
}
