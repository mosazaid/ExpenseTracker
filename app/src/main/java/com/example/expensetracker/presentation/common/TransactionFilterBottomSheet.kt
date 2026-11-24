package com.example.expensetracker.presentation.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.domain.constants.HistoryPeriod
import com.example.expensetracker.presentation.feature.history.HistoryMonthOption
import com.example.expensetracker.presentation.feature.history.TransactionSortOrder
import com.example.expensetracker.presentation.feature.history.TransactionTypeFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterBottomSheet(
    selectedType: TransactionTypeFilter,
    selectedPeriod: HistoryPeriod,
    selectedSortOrder: TransactionSortOrder = TransactionSortOrder.DESC,
    monthOptions: List<HistoryMonthOption> = emptyList(),
    selectedMonth: HistoryMonthOption? = null,
    categories: List<Category> = emptyList(),
    excludedCategoryIds: Set<Long> = emptySet(),
    availableSubCategories: List<String> = emptyList(),
    excludedSubCategories: Set<String> = emptySet(),
    onTypeSelected: (TransactionTypeFilter) -> Unit,
    onPeriodSelected: (HistoryPeriod) -> Unit,
    onSortOrderSelected: (TransactionSortOrder) -> Unit = {},
    onMonthSelected: (HistoryMonthOption?) -> Unit = {},
    onToggleExcludedCategory: (Long) -> Unit = {},
    onToggleExcludedSubCategory: (String) -> Unit = {},
    onClearAllExclusions: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters & Sorting",
                    style = MaterialTheme.typography.titleLarge
                )
                if (excludedCategoryIds.isNotEmpty() || excludedSubCategories.isNotEmpty()) {
                    TextButton(onClick = onClearAllExclusions) {
                        Text(
                            "Clear Exclusions (${excludedCategoryIds.size + excludedSubCategories.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Period Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Period",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            // Month Filter Section (When Year period is active)
            if (selectedPeriod == HistoryPeriod.YEAR && monthOptions.isNotEmpty()) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter by Month",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMonth == null,
                            onClick = { onMonthSelected(null) },
                            label = { Text("All Months") }
                        )
                        monthOptions.forEach { month ->
                            FilterChip(
                                selected = selectedMonth?.id == month.id,
                                onClick = { onMonthSelected(month) },
                                label = { Text(month.label) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Type Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionTypeFilter.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { onTypeSelected(type) },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Sort Order Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sort by Date",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionSortOrder.entries.forEach { sortOrder ->
                        FilterChip(
                            selected = selectedSortOrder == sortOrder,
                            onClick = { onSortOrderSelected(sortOrder) },
                            label = { Text(sortOrder.label) }
                        )
                    }
                }
            }

            // Exclude Categories Section
            if (categories.isNotEmpty()) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exclude Categories (Hide)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (excludedCategoryIds.isNotEmpty()) {
                            Text(
                                text = "${excludedCategoryIds.size} hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { category ->
                            val isExcluded = category.id in excludedCategoryIds
                            FilterChip(
                                selected = isExcluded,
                                onClick = { onToggleExcludedCategory(category.id) },
                                label = { Text("${category.icon} ${category.name}") },
                                leadingIcon = if (isExcluded) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Block,
                                            contentDescription = "Excluded",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }

            // Exclude Subcategories Section
            if (availableSubCategories.isNotEmpty()) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exclude Subcategories (Hide)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (excludedSubCategories.isNotEmpty()) {
                            Text(
                                text = "${excludedSubCategories.size} hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSubCategories.forEach { subName ->
                            val isExcluded = subName in excludedSubCategories
                            FilterChip(
                                selected = isExcluded,
                                onClick = { onToggleExcludedSubCategory(subName) },
                                label = { Text(subName) },
                                leadingIcon = if (isExcluded) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Block,
                                            contentDescription = "Excluded",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }
}
