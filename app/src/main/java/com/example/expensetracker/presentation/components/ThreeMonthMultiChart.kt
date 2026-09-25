package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.MonthlyBreakdown
import com.example.expensetracker.presentation.viewModel.ThreeMonthStats
import kotlin.math.max

enum class MultiChartType(val label: String) {
    BAR("Bar Chart"),
    LINE("Trend Line"),
    COMBINED("Combined")
}

private val IncomeColor = Color(0xFF4CAF50)
private val ExpenseColor = Color(0xFFEF5350)
private val WalletColor = Color(0xFF7E57C2)

@Composable
fun ThreeMonthMultiChart(
    stats: ThreeMonthStats,
    modifier: Modifier = Modifier
) {
    val months = stats.months
    if (months.isEmpty()) return

    var selectedChartType by remember { mutableStateOf(MultiChartType.COMBINED) }

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
            // Header with title and chart type switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3-Month Financial Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Income, Expense & Wallet trajectories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Chart Mode Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MultiChartType.entries.forEach { chartType ->
                    FilterChip(
                        selected = selectedChartType == chartType,
                        onClick = { selectedChartType = chartType },
                        label = {
                            Text(
                                text = chartType.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            // Chart Canvas
            val textMeasurer = rememberTextMeasurer()
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

            AnimatedContent(
                targetState = selectedChartType,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MultiChartTransition"
            ) { chartType ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val bottomLabelAreaHeight = 36.dp.toPx()
                    val chartHeight = canvasHeight - bottomLabelAreaHeight
                    val topPadding = 20.dp.toPx()
                    val usableHeight = chartHeight - topPadding

                    val maxVal = max(
                        1.0,
                        months.maxOf { max(it.income, max(it.expense, it.wallet)) }
                    ).toFloat()

                    // Draw 3 horizontal guide lines
                    val steps = 3
                    for (i in 0..steps) {
                        val y = topPadding + (usableHeight / steps) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val monthCount = months.size
                    val colWidth = canvasWidth / monthCount

                    when (chartType) {
                        MultiChartType.BAR -> {
                            drawGroupedBars(
                                months = months,
                                colWidth = colWidth,
                                maxVal = maxVal,
                                usableHeight = usableHeight,
                                chartHeight = chartHeight
                            )
                        }
                        MultiChartType.LINE -> {
                            drawTrendLines(
                                months = months,
                                colWidth = colWidth,
                                maxVal = maxVal,
                                usableHeight = usableHeight,
                                chartHeight = chartHeight,
                                showArea = true
                            )
                        }
                        MultiChartType.COMBINED -> {
                            drawGroupedBars(
                                months = months,
                                colWidth = colWidth,
                                maxVal = maxVal,
                                usableHeight = usableHeight,
                                chartHeight = chartHeight,
                                alpha = 0.7f
                            )
                            drawTrendLines(
                                months = months,
                                colWidth = colWidth,
                                maxVal = maxVal,
                                usableHeight = usableHeight,
                                chartHeight = chartHeight,
                                showArea = false
                            )
                        }
                    }

                    // Draw month labels at bottom
                    for (i in months.indices) {
                        val m = months[i]
                        val x = colWidth * i + colWidth / 2f
                        val labelText = m.monthLabel.take(8)
                        val textLayout = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = labelColor,
                                textAlign = TextAlign.Center
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = x - (textLayout.size.width / 2f),
                                y = chartHeight + 10.dp.toPx()
                            )
                        )
                    }
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = IncomeColor, label = "Income")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = ExpenseColor, label = "Expense")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = WalletColor, label = "Wallet")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3-Month Metrics Summary
            MonthMetricsSummary(stats)
        }
    }
}

private fun DrawScope.drawGroupedBars(
    months: List<MonthlyBreakdown>,
    colWidth: Float,
    maxVal: Float,
    usableHeight: Float,
    chartHeight: Float,
    alpha: Float = 1.0f
) {
    val barWidth = 14.dp.toPx()
    val barSpacing = 4.dp.toPx()
    val groupWidth = (barWidth * 3) + (barSpacing * 2)

    for (i in months.indices) {
        val m = months[i]
        val groupCenterX = colWidth * i + colWidth / 2f
        val groupStartX = groupCenterX - (groupWidth / 2f)

        val incomeHeight = ((m.income.toFloat() / maxVal) * usableHeight).coerceAtLeast(3.dp.toPx())
        val expenseHeight = ((m.expense.toFloat() / maxVal) * usableHeight).coerceAtLeast(3.dp.toPx())
        val walletHeight = ((m.wallet.toFloat() / maxVal) * usableHeight).coerceAtLeast(3.dp.toPx())

        // Income Bar
        val incomeX = groupStartX
        val incomeY = chartHeight - incomeHeight
        drawRoundRect(
            color = IncomeColor.copy(alpha = alpha),
            topLeft = Offset(incomeX, incomeY),
            size = Size(barWidth, incomeHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Expense Bar
        val expenseX = incomeX + barWidth + barSpacing
        val expenseY = chartHeight - expenseHeight
        drawRoundRect(
            color = ExpenseColor.copy(alpha = alpha),
            topLeft = Offset(expenseX, expenseY),
            size = Size(barWidth, expenseHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Wallet Bar
        val walletX = expenseX + barWidth + barSpacing
        val walletY = chartHeight - walletHeight
        drawRoundRect(
            color = WalletColor.copy(alpha = alpha),
            topLeft = Offset(walletX, walletY),
            size = Size(barWidth, walletHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

private fun DrawScope.drawTrendLines(
    months: List<MonthlyBreakdown>,
    colWidth: Float,
    maxVal: Float,
    usableHeight: Float,
    chartHeight: Float,
    showArea: Boolean
) {
    if (months.isEmpty()) return

    val expensePoints = mutableListOf<Offset>()
    val incomePoints = mutableListOf<Offset>()

    for (i in months.indices) {
        val m = months[i]
        val x = colWidth * i + colWidth / 2f
        val expY = chartHeight - ((m.expense.toFloat() / maxVal) * usableHeight).coerceAtLeast(4.dp.toPx())
        val incY = chartHeight - ((m.income.toFloat() / maxVal) * usableHeight).coerceAtLeast(4.dp.toPx())
        expensePoints.add(Offset(x, expY))
        incomePoints.add(Offset(x, incY))
    }

    // Draw Expense Smooth Spline & Area
    if (expensePoints.isNotEmpty()) {
        val expPath = Path().apply {
            moveTo(expensePoints[0].x, expensePoints[0].y)
            for (i in 0 until expensePoints.size - 1) {
                val p0 = expensePoints[i]
                val p1 = expensePoints[i + 1]
                val controlX1 = (p0.x + p1.x) / 2f
                val controlY1 = p0.y
                val controlX2 = (p0.x + p1.x) / 2f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
        }

        if (showArea) {
            val fillPath = Path().apply {
                addPath(expPath)
                lineTo(expensePoints.last().x, chartHeight)
                lineTo(expensePoints.first().x, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ExpenseColor.copy(alpha = 0.35f), ExpenseColor.copy(alpha = 0.02f)),
                    startY = 0f,
                    endY = chartHeight
                ),
                style = Fill
            )
        }

        drawPath(
            path = expPath,
            color = ExpenseColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Dots on expense points
        for (pt in expensePoints) {
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
            drawCircle(color = ExpenseColor, radius = 3.5.dp.toPx(), center = pt)
        }
    }

    // Draw Income Line
    if (incomePoints.isNotEmpty()) {
        val incPath = Path().apply {
            moveTo(incomePoints[0].x, incomePoints[0].y)
            for (i in 0 until incomePoints.size - 1) {
                val p0 = incomePoints[i]
                val p1 = incomePoints[i + 1]
                val controlX1 = (p0.x + p1.x) / 2f
                val controlY1 = p0.y
                val controlX2 = (p0.x + p1.x) / 2f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
        }

        drawPath(
            path = incPath,
            color = IncomeColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        for (pt in incomePoints) {
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = IncomeColor, radius = 2.5.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MonthMetricsSummary(stats: ThreeMonthStats) {
    val months = stats.months
    // Month-over-month calculation between Month 2 and Month 3 (current)
    val momChangePercent = if (months.size >= 2 && months[months.size - 2].expense > 0) {
        val prev = months[months.size - 2].expense
        val curr = months.last().expense
        ((curr - prev) / prev) * 100.0
    } else null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "3-Mo Average Spend",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = CurrencyUtils.formatCurrency(stats.averageMonthlyExpense),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        momChangePercent?.let { change ->
            val isDecrease = change < 0
            val isZero = kotlin.math.abs(change) < 0.1
            val trendColor = when {
                isZero -> MaterialTheme.colorScheme.outline
                isDecrease -> IncomeColor // Decreased spending is good!
                else -> ExpenseColor // Increased spending
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = trendColor.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isZero -> Icons.Default.Remove
                            isDecrease -> Icons.AutoMirrored.Filled.TrendingDown
                            else -> Icons.AutoMirrored.Filled.TrendingUp
                        },
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (change > 0) "+" else ""}${String.format("%.1f", change)}% MoM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = trendColor
                    )
                }
            }
        }
    }
}
