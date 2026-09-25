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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.SwapHoriz
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.CategorySystemKey
import com.example.expensetracker.domain.matchesSystemKey
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.DateUtils
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning
import com.example.expensetracker.presentation.theme.withTabularNums

private val OwedAccent = FinanceWarning
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
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 1. Description and from -> to in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.description.ifBlank { "Transfer" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "$from → $to",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // 2. Below category / transfer type with date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Transfer",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 3. Below subcategory if found and in same line at the right the amount (delete icon hidden)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subDesc = transaction.subDescription?.takeIf { it.isNotBlank() }
                    if (subDesc != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = subDesc,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = CurrencyUtils.formatCurrency(transaction.amount),
                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                        color = MaterialTheme.colorScheme.tertiary
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
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(transaction) }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = WalletAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = WalletAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // 1. Description and from -> to in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.description.ifBlank { "Wallet move" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = WalletAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$from → $to",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = WalletAccent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // 2. Below type with date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Wallet",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 3. Below subcategory if found and in same line at the right the amount (delete icon hidden)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subDesc = transaction.subDescription?.takeIf { it.isNotBlank() }
                    if (subDesc != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = subDesc,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = CurrencyUtils.formatCurrency(transaction.amount),
                        style = MaterialTheme.typography.titleMedium,
                        color = WalletAccent
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
        transaction.type == TransactionType.INCOME -> FinancePositive
        transaction.type == TransactionType.EXPENSE -> FinanceNegative
        else -> MaterialTheme.colorScheme.outline
    }

    val amountPrefix = when {
        isOwed -> "↩ "
        transaction.type == TransactionType.INCOME -> "+"
        transaction.type == TransactionType.EXPENSE -> "-"
        else -> ""
    }

    val isDept = category.matchesSystemKey(CategorySystemKey.DEPT) &&
        transaction.type == TransactionType.INCOME

    val accountLabel = when (transaction.accountType) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank"
        AccountType.WALLET -> "Wallet"
    }

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
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon at left for all of them
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = category?.icon ?: "📋",
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 1. Description and cash/bank in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.description.ifBlank { "No description" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = accountLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Below category with the date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = category?.name ?: "Other",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date),
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
                        Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (isReimbursed && transaction.type == TransactionType.EXPENSE) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Reimbursed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                if (isDept) {
                    linkedExpenseDescription?.let { expenseDesc ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("For: $expenseDesc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Below subcategory if found and in same line at the right the amount (delete icon hidden)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val subDesc = transaction.subDescription?.takeIf { it.isNotBlank() }
                    if (subDesc != null) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = subDesc,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = "$amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                        color = amountColor
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
