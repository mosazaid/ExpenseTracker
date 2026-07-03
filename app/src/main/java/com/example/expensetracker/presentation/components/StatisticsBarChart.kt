package com.example.expensetracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MAX_BAR_HEIGHT = 180.dp
private val BAR_WIDTH = 64.dp

@Composable
fun StatisticsBarChart(
    income: Float,
    expense: Float
) {
    val max = maxOf(income, expense, 1f)
    val incomeRatio = income / max
    val expenseRatio = expense / max

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAX_BAR_HEIGHT + 40.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        BarColumn(
            label = "Income",
            ratio = incomeRatio,
            color = Color(0xFF4CAF50),
            barWidth = BAR_WIDTH
        )
        BarColumn(
            label = "Expense",
            ratio = expenseRatio,
            color = Color(0xFFF44336),
            barWidth = BAR_WIDTH
        )
    }
}

@Composable
private fun BarColumn(
    label: String,
    ratio: Float,
    color: Color,
    barWidth: Dp
) {
    val barHeight = MAX_BAR_HEIGHT * ratio

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(barWidth)
    ) {
        Canvas(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight.coerceAtLeast(2.dp))
        ) {
            drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
