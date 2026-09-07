package com.example.expensetracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.LIQUID_ACCOUNTS

@Composable
fun AccountBalanceCards(
    cashBalance: Double?,
    bankBalance: Double?,
    selectedAccount: AccountType? = null,
    onAccountSelected: ((AccountType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Your money now",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LIQUID_ACCOUNTS.forEach { acct ->
                val bal = if (acct == AccountType.CASH) cashBalance else bankBalance
                val isSelected = selectedAccount == acct
                val isSelectable = onAccountSelected != null

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isSelectable) Modifier.clickable { onAccountSelected(acct) }
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(if (acct == AccountType.CASH) "💵" else "🏦")
                            Text(
                                acct.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (bal != null) CurrencyUtils.formatCurrency(bal) else "…",
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                bal == null -> MaterialTheme.colorScheme.outline
                                bal < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}
