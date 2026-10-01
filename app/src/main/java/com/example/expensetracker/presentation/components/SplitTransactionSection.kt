package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.TransactionSplitItem

@Composable
fun SplitTransactionSection(
    isSplitMode: Boolean,
    splits: List<TransactionSplitItem>,
    mainAmountStr: String,
    availableSubCategories: List<SubCategory>,
    onToggleSplitMode: (Boolean) -> Unit,
    onAddSplit: () -> Unit,
    onUpdateSplit: (Int, TransactionSplitItem) -> Unit,
    onRemoveSplit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainAmount = mainAmountStr.toDoubleOrNull() ?: 0.0
    val totalSplit = remember(splits) {
        splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }
    val diff = (mainAmount - totalSplit)
    val isBalanced = mainAmount > 0 && Math.abs(diff) < 0.01

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (isSplitMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CallSplit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.split_transaction_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.split_transaction_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(
                    checked = isSplitMode,
                    onCheckedChange = onToggleSplitMode
                )
            }

            AnimatedVisibility(
                visible = isSplitMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Balance status pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isBalanced -> FinancePositive.copy(alpha = 0.12f)
                            diff < 0 -> FinanceNegative.copy(alpha = 0.12f)
                            else -> FinanceWarning.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanced) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = if (isBalanced) FinancePositive else if (diff < 0) FinanceNegative else FinanceWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.split_total_label,
                                        CurrencyUtils.formatCurrency(totalSplit),
                                        CurrencyUtils.formatCurrency(mainAmount)
                                    ),
                                    style = MaterialTheme.typography.bodySmall.withTabularNums(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isBalanced) {
                                Text(
                                    text = stringResource(R.string.split_balanced),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FinancePositive,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.split_difference, CurrencyUtils.formatCurrency(diff)),
                                    style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                    color = if (diff < 0) FinanceNegative else FinanceWarning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Split items list
                    splits.forEachIndexed { index, splitItem ->
                        SplitItemCard(
                            index = index,
                            item = splitItem,
                            availableSubCategories = availableSubCategories,
                            canDelete = splits.size > 1,
                            onUpdate = { updated -> onUpdateSplit(index, updated) },
                            onDelete = { onRemoveSplit(index) }
                        )
                    }

                    // Add button and Auto-balance button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddSplit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_split_item_btn))
                        }

                        if (!isBalanced && diff > 0) {
                            FilledTonalButton(
                                onClick = {
                                    val newAmount = CurrencyUtils.cleanDecimalInput(String.format(java.util.Locale.US, "%.2f", diff))
                                    onAddSplit()
                                    onUpdateSplit(splits.size, TransactionSplitItem(amount = newAmount))
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Fill Remainder")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitItemCard(
    index: Int,
    item: TransactionSplitItem,
    availableSubCategories: List<SubCategory>,
    canDelete: Boolean,
    onUpdate: (TransactionSplitItem) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.split_item_header, index + 1),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete split",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Subcategory Selection (Chips)
            if (availableSubCategories.isNotEmpty()) {
                Text(
                    text = "Subcategory",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSubCategories.forEach { sub ->
                        val isSelected = item.subCategoryId == sub.id || item.subCategoryName == sub.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    onUpdate(item.copy(subCategoryId = null, subCategoryName = null))
                                } else {
                                    onUpdate(item.copy(subCategoryId = sub.id, subCategoryName = sub.name))
                                }
                            },
                            label = { Text(sub.name, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Amount & Note inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.amount,
                    onValueChange = { onUpdate(item.copy(amount = CurrencyUtils.cleanDecimalInput(it))) },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.note,
                    onValueChange = { onUpdate(item.copy(note = it)) },
                    label = { Text("Note / Item") },
                    placeholder = { Text("e.g. Meat") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Debt for someone else toggle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (item.isDebt) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Handshake,
                                contentDescription = null,
                                tint = if (item.isDebt) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.split_owed_by_debtor),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (item.isDebt) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Switch(
                            checked = item.isDebt,
                            onCheckedChange = { onUpdate(item.copy(isDebt = it)) }
                        )
                    }

                    if (item.isDebt) {
                        OutlinedTextField(
                            value = item.debtPersonName,
                            onValueChange = { onUpdate(item.copy(debtPersonName = it)) },
                            label = { Text(stringResource(R.string.debtor_person_name)) },
                            placeholder = { Text("e.g. Omar, Zaid") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
