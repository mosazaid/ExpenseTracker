package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.components.CategoryRow
import com.example.expensetracker.presentation.viewModel.BudgetViewModel
import com.example.expensetracker.presentation.viewModel.CategoryViewModel
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val categoryFlow = viewModel.getCategoriesByType(selectedType)
    val categories by categoryFlow.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Gray) }
    var selectedIcon by remember { mutableStateOf("📋") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var budgetCategory by remember { mutableStateOf<Category?>(null) }
    var budgetAmount by remember { mutableStateOf("") }

    if (budgetCategory != null) {
        AlertDialog(
            onDismissRequest = { budgetCategory = null },
            title = { Text("Set budget for ${budgetCategory?.name}") },
            text = {
                Column {
                    Text(
                        "Limit for the current month period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { budgetAmount = it },
                        label = { Text("Budget amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = budgetAmount.toDoubleOrNull()
                    val category = budgetCategory
                    if (amount != null && category != null) {
                        coroutineScope.launch {
                            budgetViewModel.upsertCategoryBudget(category.id, amount)
                            budgetCategory = null
                            budgetAmount = ""
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    budgetCategory = null
                    budgetAmount = ""
                }) { Text("Cancel") }
            }
        )
    }

    val iconOptions = listOf(
        "🍽️", "🚗", "🛒", "🎬", "💡", "🏥", "📚", "✈️", "📋",
        "💰", "🏢", "📈", "💻", "🎁", "🎮", "🏋️", "🐾", "🏠"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        item(key = "title") {
            Text("Manage Categories", style = MaterialTheme.typography.titleLarge)
        }

        item(key = "type-filter") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            editingCategory = null
                            name = ""
                            selectedColor = Color.Gray
                            selectedIcon = "📋"
                        },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        item(key = "name-field") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item(key = "color-label") {
            Text("Pick a Color", style = MaterialTheme.typography.bodySmall)
        }

        item(key = "color-picker") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Color.Red,
                    Color(0xFF4CAF50),
                    Color.Blue,
                    Color.Magenta,
                    Color.Cyan,
                    Color.Gray,
                    Color(0xFFFF9800),
                    Color(0xFF9C27B0)
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                            .then(
                                if (selectedColor == color) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }
        }

        item(key = "icon-label") {
            Text("Pick an Icon", style = MaterialTheme.typography.bodySmall)
        }

        item(key = "icon-picker") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconOptions.forEach { emoji ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (selectedIcon == emoji) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.clickable { selectedIcon = emoji }
                    ) {
                        Text(
                            text = emoji,
                            modifier = Modifier.padding(4.dp),
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        )
                    }
                }
            }
        }

        item(key = "save-button") {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val colorHex = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF)
                        val category = Category(
                            id = editingCategory?.id ?: 0,
                            name = name,
                            icon = selectedIcon,
                            color = colorHex,
                            type = selectedType
                        )
                        if (editingCategory == null) {
                            viewModel.insertCategory(category)
                        } else {
                            viewModel.updateCategory(category)
                        }
                        name = ""
                        selectedColor = Color.Gray
                        selectedIcon = "📋"
                        editingCategory = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingCategory == null) "Add Category" else "Update Category")
            }
        }

        if (editingCategory != null) {
            item(key = "cancel-edit") {
                TextButton(
                    onClick = {
                        name = ""
                        selectedColor = Color.Gray
                        selectedIcon = "📋"
                        editingCategory = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Edit")
                }
            }
        }

        item(key = "list-header") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your categories", style = MaterialTheme.typography.labelMedium)
        }

        items(categories, key = { it.id }) { category ->
            CategoryRow(
                category = category,
                onEdit = {
                    name = it.name
                    selectedColor = try {
                        Color(android.graphics.Color.parseColor(it.color))
                    } catch (e: IllegalArgumentException) {
                        Color.Gray
                    }
                    selectedIcon = it.icon
                    editingCategory = it
                },
                onDelete = {
                    if (!it.isDefault) {
                        viewModel.deleteCategory(it)
                    }
                },
                onSetBudget = if (category.type == TransactionType.EXPENSE) {
                    { cat ->
                        budgetCategory = cat
                        budgetAmount = ""
                    }
                } else {
                    null
                }
            )
        }
    }
}
