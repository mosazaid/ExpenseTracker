package com.example.expensetracker.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.CategoryRow
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.BudgetViewModel
import com.example.expensetracker.presentation.viewModel.CategoryViewModel
import kotlinx.coroutines.launch

enum class CategoriesScreenTab {
    CATEGORIES,
    BUDGETS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(CategoriesScreenTab.CATEGORIES) }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val categoryFlow = viewModel.getCategoriesByTypeSortedByUsage(selectedType)
    val categories by categoryFlow.collectAsState(initial = emptyList())
    val allExpenseCategories by viewModel.getCategoriesByTypeSortedByUsage(TransactionType.EXPENSE).collectAsState(initial = emptyList())
    val budgetProgressMap by budgetViewModel.budgetProgressMap.collectAsState()

    var showAddCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF3B82F6)) }
    var selectedIcon by remember { mutableStateOf("🛒") }

    var budgetCategory by remember { mutableStateOf<Category?>(null) }
    var budgetAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        budgetViewModel.loadBudgetProgress()
    }

    // Modal to Set/Edit Category Budget
    if (budgetCategory != null) {
        val targetCat = budgetCategory!!
        val currentProgress = budgetProgressMap[targetCat.id]

        AlertDialog(
            onDismissRequest = { budgetCategory = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (currentProgress != null && currentProgress.limit > 0)
                        "Edit Budget: ${targetCat.name}"
                    else
                        "Set Monthly Budget: ${targetCat.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify a monthly spending limit for ${targetCat.name}. The app will track progress and alert you as you near the limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { budgetAmount = CurrencyUtils.cleanDecimalInput(it) },
                        label = { Text("Monthly Budget Limit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50, 100, 200, 500).forEach { preset ->
                            SuggestionChip(
                                onClick = { budgetAmount = preset.toString() },
                                label = { Text("+$preset", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (currentProgress != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Month Spent:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyUtils.formatCurrency(currentProgress.spent),
                                    style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = budgetAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            coroutineScope.launch {
                                budgetViewModel.upsertCategoryBudget(targetCat.id, amount)
                                budgetCategory = null
                                budgetAmount = ""
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentProgress != null && currentProgress.limit > 0) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    budgetViewModel.deleteCategoryBudget(targetCat.id)
                                    budgetCategory = null
                                    budgetAmount = ""
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove")
                        }
                    }
                    TextButton(onClick = { budgetCategory = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Modal to Add / Edit Category
    if (showAddCategorySheet) {
        val iconOptions = listOf(
            "🛒", "🍽️", "🚗", "🎬", "💡", "🏥", "📚", "✈️", "📋",
            "💰", "🏢", "📈", "💻", "🎁", "🎮", "🏋️", "🐾", "🏠", "🛡️", "⛽"
        )
        val colorOptions = listOf(
            Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFEF4444),
            Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
            Color(0xFF06B6D4), Color(0xFF64748B)
        )

        AlertDialog(
            onDismissRequest = { showAddCategorySheet = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (editingCategory != null) "Edit Category" else "New Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Pick Color", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .then(
                                        if (selectedColor == color) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }

                    Text("Pick Icon", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        iconOptions.forEach { icon ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedIcon == icon) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable { selectedIcon = icon }
                            ) {
                                Text(
                                    text = icon,
                                    modifier = Modifier.padding(6.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            val colorHex = String.format("#%06X", selectedColor.toArgb() and 0xFFFFFF)
                            val category = Category(
                                id = editingCategory?.id ?: 0,
                                name = categoryName,
                                icon = selectedIcon,
                                color = colorHex,
                                type = selectedType
                            )
                            if (editingCategory == null) {
                                viewModel.insertCategory(category)
                            } else {
                                viewModel.updateCategory(category)
                            }
                            showAddCategorySheet = false
                            categoryName = ""
                            editingCategory = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (editingCategory != null) "Update" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategorySheet = false
                    editingCategory = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.categories_budgets_title),
                navController = navController,
                canNavigateBack = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    categoryName = ""
                    selectedColor = Color(0xFF3B82F6)
                    selectedIcon = "🛒"
                    showAddCategorySheet = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Category",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Header Segment: Switch between Categories list & Dedicated Budgets view
            item(key = "tab-selector") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = currentTab == CategoriesScreenTab.CATEGORIES,
                        onClick = { currentTab = CategoriesScreenTab.CATEGORIES },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Categories")
                        }
                    }
                    SegmentedButton(
                        selected = currentTab == CategoriesScreenTab.BUDGETS,
                        onClick = {
                            currentTab = CategoriesScreenTab.BUDGETS
                            budgetViewModel.loadBudgetProgress()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Monthly Budgets")
                        }
                    }
                }
            }

            if (currentTab == CategoriesScreenTab.CATEGORIES) {
                // Type Filter Chips: Expense vs Income
                item(key = "type-filter") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = {
                                    Text(
                                        if (type == TransactionType.EXPENSE) stringResource(R.string.expense)
                                        else stringResource(R.string.income)
                                    )
                                }
                            )
                        }
                    }
                }

                items(categories, key = { "cat_${it.id}" }) { category ->
                    CategoryRow(
                        category = category,
                        budgetProgress = budgetProgressMap[category.id],
                        onEdit = { cat ->
                            editingCategory = cat
                            categoryName = cat.name
                            selectedIcon = cat.icon
                            selectedColor = try {
                                Color(android.graphics.Color.parseColor(cat.color))
                            } catch (_: Exception) {
                                Color(0xFF3B82F6)
                            }
                            showAddCategorySheet = true
                        },
                        onDelete = { cat ->
                            if (!cat.isDefault) {
                                viewModel.deleteCategory(cat)
                            }
                        },
                        onSetBudget = if (category.type == TransactionType.EXPENSE) {
                            { cat ->
                                budgetCategory = cat
                                val existingLimit = budgetProgressMap[cat.id]?.limit
                                budgetAmount = if (existingLimit != null && existingLimit > 0) existingLimit.toString() else ""
                            }
                        } else null
                    )
                }
            } else {
                // DEDICATED BUDGETS TAB
                val activeBudgetList = allExpenseCategories.mapNotNull { cat ->
                    val progress = budgetProgressMap[cat.id]
                    if (progress != null && progress.limit > 0) cat to progress else null
                }
                val totalBudget = activeBudgetList.sumOf { it.second.limit }
                val totalSpent = activeBudgetList.sumOf { it.second.spent }

                item(key = "budget-overview-card") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${CurrencyUtils.formatCurrency(totalSpent)} / ${CurrencyUtils.formatCurrency(totalBudget)}",
                                    style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                item(key = "budget-categories-header") {
                    Text(
                        "All Expense Categories & Limits",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(allExpenseCategories, key = { "budget_cat_${it.id}" }) { category ->
                    CategoryRow(
                        category = category,
                        budgetProgress = budgetProgressMap[category.id],
                        onEdit = { cat ->
                            editingCategory = cat
                            categoryName = cat.name
                            selectedIcon = cat.icon
                            selectedColor = try {
                                Color(android.graphics.Color.parseColor(cat.color))
                            } catch (_: Exception) {
                                Color(0xFF3B82F6)
                            }
                            showAddCategorySheet = true
                        },
                        onDelete = { cat ->
                            if (!cat.isDefault) {
                                viewModel.deleteCategory(cat)
                            }
                        },
                        onSetBudget = { cat ->
                            budgetCategory = cat
                            val existingLimit = budgetProgressMap[cat.id]?.limit
                            budgetAmount = if (existingLimit != null && existingLimit > 0) existingLimit.toString() else ""
                        }
                    )
                }
            }
        }
    }
}
