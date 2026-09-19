package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.CategoryExpenseShare
import com.example.expensetracker.presentation.viewModel.ThreeMonthStats
import kotlin.math.atan2
import kotlin.math.sqrt

private val FallbackColors = listOf(
    Color(0xFF42A5F5), // Blue
    Color(0xFFEC407A), // Pink
    Color(0xFFFFA726), // Orange
    Color(0xFF26A69A), // Teal
    Color(0xFFAB47BC), // Purple
    Color(0xFF8D6E63), // Brown
    Color(0xFF78909C), // Blue Grey
    Color(0xFFFFB300), // Amber
    Color(0xFF66BB6A)  // Green
)

fun parseCategoryColor(hex: String, index: Int): Color {
    return try {
        if (hex.isNotBlank()) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            FallbackColors[index % FallbackColors.size]
        }
    } catch (e: Exception) {
        FallbackColors[index % FallbackColors.size]
    }
}

@Composable
fun CategoryPieChart(
    stats: ThreeMonthStats,
    modifier: Modifier = Modifier
) {
    val months = stats.months
    if (months.isEmpty()) return

    // 0 = All 3 Months, 1..3 = individual months
    var selectedMonthTab by remember { mutableIntStateOf(0) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var isExpandedList by remember { mutableStateOf(false) }

    val currentBreakdown: List<CategoryExpenseShare> = remember(selectedMonthTab, stats) {
        when (selectedMonthTab) {
            0 -> stats.combinedCategoryBreakdown
            in 1..months.size -> months[selectedMonthTab - 1].categoryBreakdown
            else -> emptyList()
        }
    }

    val totalExpenseForSelection: Double = remember(selectedMonthTab, stats) {
        when (selectedMonthTab) {
            0 -> stats.totalExpense
            in 1..months.size -> months[selectedMonthTab - 1].expense
            else -> 0.0
        }
    }

    val selectedCategoryShare = remember(selectedCategoryId, currentBreakdown) {
        currentBreakdown.find { it.categoryId == selectedCategoryId }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Expense Comparison",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compare spending share across the last 3 months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Month Selector Chips (Scrollable row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMonthTab == 0,
                    onClick = {
                        selectedMonthTab = 0
                        selectedCategoryId = null
                    },
                    label = { Text("All 3 Months", style = MaterialTheme.typography.labelSmall) }
                )
                months.forEachIndexed { index, m ->
                    val tabIdx = index + 1
                    FilterChip(
                        selected = selectedMonthTab == tabIdx,
                        onClick = {
                            selectedMonthTab = tabIdx
                            selectedCategoryId = null
                        },
                        label = {
                            Text(
                                text = m.monthLabel.take(8),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            // Pie/Donut Chart Center Section
            if (currentBreakdown.isEmpty() || totalExpenseForSelection <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "No expenses recorded for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeWidthDp = 30.dp

                    Canvas(
                        modifier = Modifier
                            .size(210.dp)
                            .pointerInput(currentBreakdown, totalExpenseForSelection) {
                                detectTapGestures { tapOffset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = tapOffset.x - centerX
                                    val dy = tapOffset.y - centerY
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val radius = (size.width - strokeWidthDp.toPx()) / 2f
                                    val innerR = radius - strokeWidthDp.toPx() / 2f
                                    val outerR = radius + strokeWidthDp.toPx() / 2f

                                    if (dist in innerR..outerR) {
                                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        // Standard atan2 has 0 at 3 o'clock; our start is at 12 o'clock (-90 degrees)
                                        angleDeg = (angleDeg + 90f + 360f) % 360f

                                        var currentSweep = 0f
                                        var matchedId: Long? = null
                                        for (item in currentBreakdown) {
                                            val sweep = (item.amount / totalExpenseForSelection).toFloat() * 360f
                                            if (angleDeg >= currentSweep && angleDeg < currentSweep + sweep) {
                                                matchedId = item.categoryId
                                                break
                                            }
                                            currentSweep += sweep
                                        }

                                        selectedCategoryId = if (selectedCategoryId == matchedId) null else matchedId
                                    } else {
                                        // Click outside ring deselects
                                        selectedCategoryId = null
                                    }
                                }
                            }
                    ) {
                        val canvasSize = size.minDimension
                        val strokePx = strokeWidthDp.toPx()
                        val arcSize = Size(canvasSize - strokePx, canvasSize - strokePx)
                        val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                        var startAngle = -90f // Start at top (12 o'clock)

                        for (i in currentBreakdown.indices) {
                            val item = currentBreakdown[i]
                            val sweepAngle = ((item.amount / totalExpenseForSelection).toFloat() * 360f).coerceAtLeast(0.5f)
                            val isSelected = item.categoryId == selectedCategoryId
                            val color = parseCategoryColor(item.categoryColor, i)

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) strokePx * 1.25f else strokePx,
                                    cap = StrokeCap.Butt
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center Donut Details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp)
                    ) {
                        if (selectedCategoryShare != null) {
                            Text(
                                text = selectedCategoryShare.categoryIcon,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = selectedCategoryShare.categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${String.format("%.1f", selectedCategoryShare.percentage)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(selectedCategoryShare.amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(totalExpenseForSelection),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${currentBreakdown.size} categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Category Legend List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayedList = if (isExpandedList) currentBreakdown else currentBreakdown.take(4)

                    displayedList.forEachIndexed { index, share ->
                        val isSelected = share.categoryId == selectedCategoryId
                        val color = parseCategoryColor(share.categoryColor, index)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedCategoryId = if (isSelected) null else share.categoryId
                                },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = share.categoryIcon,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = share.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", share.percentage)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = CurrencyUtils.formatCurrency(share.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (currentBreakdown.size > 4) {
                        TextButton(
                            onClick = { isExpandedList = !isExpandedList },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(if (isExpandedList) "Show Less" else "Show All (${currentBreakdown.size})")
                            Icon(
                                imageVector = if (isExpandedList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
