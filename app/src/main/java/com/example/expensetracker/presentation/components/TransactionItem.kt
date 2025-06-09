package com.example.expensetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils

@Composable
fun TransactionItem(transaction: Transaction) {
    val amountColor = when (transaction.type.name) {
        "INCOME" -> Color(0xFF4CAF50) // Green
        "EXPENSE" -> Color(0xFFF44336) // Red
        else -> Color.Gray
    }

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(transaction.description, style = MaterialTheme.typography.bodyLarge)
                Text(
                    CurrencyUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = amountColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                DateUtils.formatDateTime(transaction.date),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
