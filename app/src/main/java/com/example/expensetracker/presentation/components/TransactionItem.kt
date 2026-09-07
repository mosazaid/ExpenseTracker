package com.example.expensetracker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.CategorySystemKey
import com.example.expensetracker.domain.matchesSystemKey
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils

private val OwedAccent = Color(0xFFFF9800)
private val WalletAccent = Color(0xFF7E57C2)

private fun formatAccountLabel(account: AccountType): String {
    return when (account) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank"
        AccountType.WALLET -> "Wallet"
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit = {},
    isReimbursed: Boolean = false,
    isOwed: Boolean = false,
    linkedExpenseDescription: String? = null
) {
    when (transaction.type) {
        TransactionType.TRANSFER -> TransferItem(transaction, onDelete, onEdit)
        TransactionType.WALLET_MOVE -> WalletMoveItem(transaction, onDelete, onEdit)
        else -> StandardTransactionItem(
            transaction = transaction,
            category = category,
            onDelete = onDelete,
            onEdit = onEdit,
            isReimbursed = isReimbursed,
            isOwed = isOwed,
            linkedExpenseDescription = linkedExpenseDescription
        )
    }
}

@Composable
private fun TransferItem(
    transaction: Transaction,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val from = formatAccountLabel(transaction.accountType)
    val to = transaction.toAccountType?.let { formatAccountLabel(it) } ?: "?"

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(transaction) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description.ifBlank { "Transfer" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "$from → $to · ${DateUtils.formatDate(transaction.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    CurrencyUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transfer",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletMoveItem(
    transaction: Transaction,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
) {
    val from = formatAccountLabel(transaction.accountType)
    val to = transaction.toAccountType?.let { formatAccountLabel(it) } ?: "?"

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(transaction) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = WalletAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        "Wallet move",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = WalletAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    transaction.description.ifBlank { "Wallet move" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "$from → $to · ${DateUtils.formatDate(transaction.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WalletAccent
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    CurrencyUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WalletAccent
                )
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete wallet move",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardTransactionItem(
    transaction: Transaction,
    category: Category?,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    isReimbursed: Boolean,
    isOwed: Boolean,
    linkedExpenseDescription: String?
) {
    val amountColor = when {
        isOwed -> OwedAccent
        transaction.type == TransactionType.INCOME -> Color(0xFF4CAF50)
        transaction.type == TransactionType.EXPENSE -> Color(0xFFF44336)
        else -> Color.Gray
    }

    val amountPrefix = when {
        isOwed -> "↩ "
        transaction.type == TransactionType.INCOME -> "+"
        transaction.type == TransactionType.EXPENSE -> "-"
        else -> ""
    }

    val isDept = category.matchesSystemKey(CategorySystemKey.DEPT) &&
        transaction.type == TransactionType.INCOME

    Surface(
        tonalElevation = if (isOwed) 0.dp else 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = if (isOwed) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isOwed) {
            BorderStroke(1.dp, OwedAccent.copy(alpha = 0.6f))
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(transaction) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (category != null) {
                    Text(
                        text = category.icon,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column {
                    if (isOwed) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = OwedAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Owed to you",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = OwedAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        transaction.description.ifBlank { "No description" },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    transaction.subDescription?.takeIf { it.isNotBlank() }?.let { sub ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category != null) {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOwed) OwedAccent else MaterialTheme.colorScheme.primary
                            )
                            Text("·", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        // Account badge so user can instantly see Cash vs Bank
                        val accountLabel = when (transaction.accountType) {
                            AccountType.CASH -> "💵 Cash"
                            AccountType.BANK -> "🏦 Bank"
                            AccountType.WALLET -> "👛 Wallet"
                        }
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                accountLabel,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text("·", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text(
                            DateUtils.formatDate(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (isOwed) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Awaiting Dept reimbursement",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        transaction.debtorNote?.takeIf { it.isNotBlank() }?.let { note ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (isReimbursed && transaction.type == TransactionType.EXPENSE) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Reimbursed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (isDept) {
                        linkedExpenseDescription?.let { expenseDesc ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "For: $expenseDesc",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        transaction.debtorNote?.takeIf { it.isNotBlank() }?.let { note ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "$amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = amountColor
                )
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transaction",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    category: Category?,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit = {},
    isReimbursed: Boolean = false,
    isOwed: Boolean = false,
    linkedExpenseDescription: String? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(transaction)
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    ) {
        TransactionItem(
            transaction = transaction,
            category = category,
            onDelete = onDelete,
            onEdit = onEdit,
            isReimbursed = isReimbursed,
            isOwed = isOwed,
            linkedExpenseDescription = linkedExpenseDescription
        )
    }
}
