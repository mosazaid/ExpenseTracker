package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.DatabaseInspector
import com.example.expensetracker.domain.DbTableData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseBrowserViewModel @Inject constructor(
    private val databaseInspector: DatabaseInspector
) : ViewModel() {

    private val _state = MutableStateFlow(DatabaseBrowserState())
    val state: StateFlow<DatabaseBrowserState> = _state.asStateFlow()

    init {
        val tables = databaseInspector.getTableNames()
        _state.value = _state.value.copy(
            tables = tables,
            selectedTable = tables.firstOrNull()
        )
        tables.firstOrNull()?.let { loadTable(it, emptyMap()) }
    }

    fun selectTable(tableName: String) {
        _state.value = _state.value.copy(
            selectedTable = tableName,
            columnFilters = emptyMap(),
            tableData = null
        )
        loadTable(tableName, emptyMap())
    }

    fun updateColumnFilter(column: String, value: String) {
        _state.value = _state.value.copy(
            columnFilters = _state.value.columnFilters.toMutableMap().apply {
                if (value.isBlank()) remove(column) else put(column, value)
            }
        )
    }

    fun applyFilters() {
        val table = _state.value.selectedTable ?: return
        loadTable(table, _state.value.columnFilters)
    }

    fun clearFilters() {
        val table = _state.value.selectedTable ?: return
        _state.value = _state.value.copy(columnFilters = emptyMap())
        loadTable(table, emptyMap())
    }

    private fun loadTable(tableName: String, filters: Map<String, String>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                databaseInspector.inspectTable(tableName, filters)
            }.onSuccess { data ->
                _state.value = _state.value.copy(tableData = data, isLoading = false)
            }.onFailure {
                _state.value = _state.value.copy(isLoading = false, tableData = null)
            }
        }
    }
}

data class DatabaseBrowserState(
    val tables: List<String> = emptyList(),
    val selectedTable: String? = null,
    val columnFilters: Map<String, String> = emptyMap(),
    val tableData: DbTableData? = null,
    val isLoading: Boolean = false
)
