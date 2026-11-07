package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.presentation.model.AdvancedComparisonData
import com.example.expensetracker.presentation.model.CategoryComparisonItem
import com.example.expensetracker.presentation.model.ComparisonMode
import com.example.expensetracker.presentation.model.SubcategoryComparisonItem
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.SubCategoryComparisonData
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryComparisonCard(
    advancedComparison: AdvancedComparisonData?,
    comparisonData: SubCategoryComparisonData?,
    availableCategories: List<Category>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    onSelectMode: (ComparisonMode) -> Unit,
    onSelectBaseIndex: (Int) -> Unit,
    onSelectCompareIndex: (Int) -> Unit,
    onSubcategoryClick: (categoryId: Long, subcategoryName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var baseExpanded by remember { mutableStateOf(false) }
    var compareExpanded by remember { mutableStateOf(false) }

    // Track which categories are expanded in accordion
    var expandedCategoryIds by remember { mutableStateOf(setOf<Long>()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize(tween(250)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Header with Comparison icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Category & Subcategory Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Day-normalized pacing & waterfall drilldown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ── 2. Period Selectors: Base vs Compare
            val periodOptions = advancedComparison?.availablePeriods.orEmpty()
            if (periodOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Base Period Dropdown
                    val currentBase = periodOptions.firstOrNull { it.bounds.label == advancedComparison?.basePeriod?.label }
                        ?: periodOptions.first()
                    ExposedDropdownMenuBox(
                        expanded = baseExpanded,
                        onExpandedChange = { baseExpanded = !baseExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentBase.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Base Period") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = baseExpanded,
                            onDismissRequest = { baseExpanded = false }
                        ) {
                            periodOptions.forEachIndexed { idx, opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onSelectBaseIndex(idx)
                                        baseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Compare Period Dropdown
                    val currentCompare = periodOptions.firstOrNull { it.bounds.label == advancedComparison?.comparePeriod?.label }
                        ?: periodOptions.getOrElse(1) { currentBase }
                    ExposedDropdownMenuBox(
                        expanded = compareExpanded,
                        onExpandedChange = { compareExpanded = !compareExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentCompare.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Compare To") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = compareExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = compareExpanded,
                            onDismissRequest = { compareExpanded = false }
                        ) {
                            periodOptions.forEachIndexed { idx, opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onSelectCompareIndex(idx)
                                        compareExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Day-Normalized Pacing Toggle
            val currentMode = advancedComparison?.mode ?: ComparisonMode.DAY_NORMALIZED
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ComparisonMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == currentMode,
                        onClick = { onSelectMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ComparisonMode.entries.size)
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (mode == currentMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // ── 4. Day Pacing Active Banner
            if (currentMode == ComparisonMode.DAY_NORMALIZED && advancedComparison?.cutoffDay != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Normalized: Comparing Day 1–${advancedComparison.cutoffDay} of both periods for fair pacing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── 5. Total Spend Overview & Delta Summary
            if (advancedComparison != null && (advancedComparison.baseTotalExpense > 0 || advancedComparison.compareTotalExpense > 0)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Comparison Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = CurrencyUtils.formatCurrency(advancedComparison.baseTotalExpense),
                                    style = MaterialTheme.typography.titleMedium.withTabularNums(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "vs ${CurrencyUtils.formatCurrency(advancedComparison.compareTotalExpense)}",
                                    style = MaterialTheme.typography.bodySmall.withTabularNums(),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Delta badge
                        val overallDelta = advancedComparison.overallDeltaPercent
                        if (overallDelta != null) {
                            val isLower = overallDelta < 0
                            val badgeColor = if (isLower) FinancePositive else FinanceNegative
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLower) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${if (overallDelta > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", overallDelta)}%",
                                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 6. Category Waterfall Leaderboard (Accordion)
            val categories = advancedComparison?.categories.orEmpty()
            if (categories.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses recorded in the selected comparison window.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Text(
                    text = "Category Trend Leaderboard",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.forEach { catItem ->
                        val isExpanded = catItem.categoryId in expandedCategoryIds
                        CategoryWaterfallRow(
                            item = catItem,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCategoryIds = if (isExpanded) {
                                    expandedCategoryIds - catItem.categoryId
                                } else {
                                    expandedCategoryIds + catItem.categoryId
                                }
                            },
                            onSubcategoryClick = { subName ->
                                onSubcategoryClick(catItem.categoryId, subName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryWaterfallRow(
    item: CategoryComparisonItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSubcategoryClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.animateContentSize(tween(250))) {
            // Parent Category Header (Clickable to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
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
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = item.categoryIcon, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.subcategories.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "${item.subcategories.size}",
                                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        // Top spike driver insight
                        if (!item.topSpikeInsight.isNullOrBlank()) {
                            Text(
                                text = item.topSpikeInsight,
                                style = MaterialTheme.typography.labelSmall,
                                color = FinanceWarning,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Amount & Delta Badge & Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyUtils.formatCurrency(item.baseTotal),
                            style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "vs ${CurrencyUtils.formatCurrency(item.compareTotal)}",
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Delta badge
                    val delta = item.deltaPercent
                    if (delta != null) {
                        val isLower = delta < 0
                        val badgeColor = if (isLower) FinancePositive else if (delta > 25) FinanceNegative else FinanceWarning
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${if (delta > 0) "+" else ""}${delta.roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Subcategories Waterfall List (Visible when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    item.subcategories.forEach { subItem ->
                        SubcategoryWaterfallItemRow(
                            subItem = subItem,
                            maxSpendInCat = max(1.0, max(item.baseTotal, item.compareTotal)),
                            onClick = { onSubcategoryClick(subItem.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryWaterfallItemRow(
    subItem: SubcategoryComparisonItem,
    maxSpendInCat: Double,
    onClick: () -> Unit
) {
    val delta = subItem.deltaPercent
    val isLower = delta != null && delta < 0
    val deltaColor = if (delta == null) {
        MaterialTheme.colorScheme.outline
    } else if (isLower) {
        FinancePositive
    } else if (delta > 30) {
        FinanceNegative
    } else {
        FinanceWarning
    }

    val baseRatio = (subItem.baseAmount / maxSpendInCat).toFloat().coerceIn(0.04f, 1f)
    val compareRatio = (subItem.compareAmount / maxSpendInCat).toFloat().coerceIn(0.04f, 1f)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subItem.baseTxnCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "${subItem.baseTxnCount} txns",
                                style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = CurrencyUtils.formatCurrency(subItem.baseAmount),
                        style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "vs ${CurrencyUtils.formatCurrency(subItem.compareAmount)}",
                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = deltaColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (delta != null) "${if (delta > 0) "+" else ""}${delta.roundToInt()}%" else "New",
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "View transactions",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Proportional Comparison Bar: Base bar vs Compare bar
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Base month bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Base",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(baseRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Compare month bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Comp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.width(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(compareRatio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        )
                    }
                }
            }

            // Spike insight text if available
            if (!subItem.varianceInsight.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = FinanceNegative.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = FinanceNegative,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = subItem.varianceInsight,
                            style = MaterialTheme.typography.labelSmall,
                            color = FinanceNegative,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
