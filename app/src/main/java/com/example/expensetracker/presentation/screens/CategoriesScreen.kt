package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.viewModel.CategoryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetracker.presentation.components.CategoryRow

@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val categoryFlow = viewModel.getCategoriesByType(selectedType)
    val categories by categoryFlow.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Gray) }
    var selectedIcon by remember { mutableStateOf("📋") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Manage Categories", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransactionType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Pick a Color", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Color.Red,
                Color.Green,
                Color.Blue,
                Color.Magenta,
                Color.Cyan,
                Color.Gray
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color)
                        .clickable { selectedColor = color }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Pick an Icon", style = MaterialTheme.typography.bodySmall)

        val iconOptions =
            listOf("🍽️", "🚗", "🛒", "🎬", "💡", "🏥", "📚", "✈️", "📋", "💰", "🏢", "📈", "💻", "🎁")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            iconOptions.forEach { emoji ->
                Text(
                    text = emoji,
                    modifier = Modifier
                        .clickable { selectedIcon = emoji }
                        .padding(4.dp),
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val category = Category(
                        id = editingCategory?.id ?: 0,
                        name = name,
                        icon = selectedIcon,
                        color = String.format("#%06X", 0xFFFFFF and selectedColor.value.toInt()),
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
            }
        ) {
            Text(if (editingCategory == null) "Add Category" else "Update Category")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        name = it.name
                        selectedColor = Color(android.graphics.Color.parseColor(it.color))
                        selectedIcon = it.icon
                        editingCategory = it
                    },
                    onDelete = {
                        viewModel.deleteCategory(it)
                    }
                )
            }
        }
    }
}
