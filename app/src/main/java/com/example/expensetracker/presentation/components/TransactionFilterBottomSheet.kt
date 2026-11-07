package com.example.expensetracker.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.screens.HistoryMonthOption
import com.example.expensetracker.presentation.screens.TransactionSortOrder
import com.example.expensetracker.presentation.screens.TransactionTypeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterBottomSheet(
    selectedType: TransactionTypeFilter,
    selectedPeriod: HistoryPeriod,
    selectedSortOrder: TransactionSortOrder = TransactionSortOrder.DESC,
    monthOptions: List<HistoryMonthOption> = emptyList(),
    selectedMonth: HistoryMonthOption? = null,
    onTypeSelected: (TransactionTypeFilter) -> Unit,
    onPeriodSelected: (HistoryPeriod) -> Unit,
    onSortOrderSelected: (TransactionSortOrder) -> Unit = {},
    onMonthSelected: (HistoryMonthOption?) -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filters & Sorting",
                style = MaterialTheme.typography.titleLarge
            )

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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
        }
    }
}
