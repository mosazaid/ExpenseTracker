package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
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
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.SubCategoryComparisonData
import com.example.expensetracker.presentation.viewModel.SubCategoryMonthlyTrend
import com.example.expensetracker.presentation.viewModel.SubCategoryShare
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private enum class SubCategoryTab(val label: String) {
    PERIOD_BREAKDOWN("This Period"),
    MULTI_MONTH_TREND("3-Month Trend")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryComparisonCard(
    comparisonData: SubCategoryComparisonData?,
    availableCategories: List<Category>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SubCategoryTab.PERIOD_BREAKDOWN) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CompareArrows,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Subcategory Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Breakdown & cross-month comparison",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Category Selector Dropdown
            if (availableCategories.isNotEmpty()) {
                val selectedCat = availableCategories.firstOrNull { it.id == selectedCategoryId }
                    ?: availableCategories.firstOrNull()

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${selectedCat?.icon.orEmpty()} ${selectedCat?.name ?: "Select Category"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(category.icon)
                                        Text(category.name, fontWeight = if (category.id == selectedCategoryId) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    onSelectCategory(category.id)
                                    categoryDropdownExpanded = false
                                },
                                trailingIcon = if (category.id == selectedCategoryId) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Tab Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SubCategoryTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SubCategoryTab.entries.size)
                    ) {
                        Text(tab.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Tab Content
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "subcategoryTabTransition"
            ) { tab ->
                when (tab) {
                    SubCategoryTab.PERIOD_BREAKDOWN -> {
                        PeriodBreakdownView(comparisonData = comparisonData)
                    }
                    SubCategoryTab.MULTI_MONTH_TREND -> {
                        MultiMonthTrendView(comparisonData = comparisonData)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodBreakdownView(comparisonData: SubCategoryComparisonData?) {
    if (comparisonData == null || comparisonData.currentPeriodSubCategories.isEmpty()) {
        EmptySubcategoryState(
            message = "No subcategory expenses recorded for ${comparisonData?.categoryName ?: "this category"} in this period.",
            hint = "Split transactions and note entries are automatically categorized here."
        )
        return
    }

    val total = comparisonData.totalCategoryExpenseCurrent
    val subCategories = comparisonData.currentPeriodSubCategories

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${subCategories.size} Subcategories",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Total: ${CurrencyUtils.formatCurrency(total)}",
                style = MaterialTheme.typography.labelMedium.withTabularNums(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        subCategories.forEach { sub ->
            SubCategoryRow(sub = sub)
        }
    }
}

@Composable
private fun SubCategoryRow(sub: SubCategoryShare) {
    val progress by animateFloatAsState(
        targetValue = (sub.percentageOfCategory / 100f).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "subProgress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sub.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatCurrency(sub.amount),
                    style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${sub.percentageOfCategory.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.withTabularNums(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun MultiMonthTrendView(comparisonData: SubCategoryComparisonData?) {
    if (comparisonData == null || comparisonData.monthlyTrends.isEmpty()) {
        EmptySubcategoryState(
            message = "No cross-month trend data available for ${comparisonData?.categoryName ?: "this category"}.",
            hint = "Data across consecutive months will show comparative trends here."
        )
        return
    }

    val monthLabels = comparisonData.monthLabels
    val trends = comparisonData.monthlyTrends

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Month Labels Header
        if (monthLabels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subcategory",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1.2f)
                )
                Row(
                    modifier = Modifier.weight(2f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(54.dp)) // Space for delta badge
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        }

        // List of Subcategories with 3-Month Amounts and Delta
        trends.forEach { trend ->
            SubCategoryTrendRow(trend = trend)
        }
    }
}

@Composable
private fun SubCategoryTrendRow(trend: SubCategoryMonthlyTrend) {
    val delta = trend.deltaPercent
    val maxAmt = max(1.0, trend.monthlyAmounts.maxOrNull() ?: 1.0)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trend.subCategoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Delta badge (MoM change)
                if (delta != null) {
                    val isDecrease = delta < 0
                    val deltaColor = if (isDecrease) FinancePositive else FinanceNegative
                    val icon = if (isDecrease) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = deltaColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${if (delta > 0) "+" else ""}${delta.roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall.withTabularNums(),
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3-Month Amount Comparison Bar Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trend.monthlyAmounts.forEachIndexed { idx, amount ->
                    val ratio = (amount / maxAmt).toFloat().coerceIn(0.05f, 1f)
                    val isCurrentMonth = idx == trend.monthlyAmounts.lastIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isCurrentMonth) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    )
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(amount),
                            style = MaterialTheme.typography.labelSmall.withTabularNums(),
                            fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySubcategoryState(
    message: String,
    hint: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
