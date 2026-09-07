package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.presentation.viewModel.DatabaseBrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseBrowserScreen(
    navController: NavController,
    viewModel: DatabaseBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.db_browser_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.tables.isEmpty() && state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "table-select") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.db_select_table),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.tables.forEach { table ->
                                FilterChip(
                                    selected = state.selectedTable == table,
                                    onClick = { viewModel.selectTable(table) },
                                    label = { Text(table) }
                                )
                            }
                        }
                    }
                }

                state.tableData?.columns?.let { columns ->
                    item(key = "filters-header") {
                        Text(
                            stringResource(R.string.db_column_filters),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    items(columns, key = { it.name }) { column ->
                        OutlinedTextField(
                            value = state.columnFilters[column.name].orEmpty(),
                            onValueChange = { viewModel.updateColumnFilter(column.name, it) },
                            label = { Text("${column.name} (${column.type})") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item(key = "filter-actions") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.applyFilters() }) {
                                Text(stringResource(R.string.db_apply_filters))
                            }
                            OutlinedButton(onClick = { viewModel.clearFilters() }) {
                                Text(stringResource(R.string.db_clear_filters))
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    item(key = "loading") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                state.tableData?.let { data ->
                    item(key = "row-count") {
                        Text(
                            stringResource(R.string.db_rows_count, data.rows.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (data.rows.isEmpty()) {
                        item(key = "empty") {
                            Text(stringResource(R.string.db_no_rows))
                        }
                    } else {
                        itemsIndexed(data.rows, key = { index, row ->
                            row.entries.sortedBy { it.key }.joinToString("|") { "${it.key}=${it.value}" } + "@$index"
                        }) { _, row ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    data.columns.forEach { column ->
                                        val value = row[column.name]
                                        Text(
                                            text = "${column.name}: ${value ?: "NULL"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
