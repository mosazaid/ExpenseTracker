package com.example.expensetracker.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StatisticsBarChart(
    income: Float, expense: Float
) {
    val max = maxOf(income, expense, 1f)

    val incomeHeightRatio = income / max
    val expenseHeightRatio = expense / max

    val barWidth = 60.dp

    Row(
        Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Bar(label = "Income", value = incomeHeightRatio, color = Color(0xFF4CAF50), barWidth)
        Bar(label = "Expense", value = expenseHeightRatio, color = Color(0xFFF44336), barWidth)
    }
}

@Composable
fun Bar(label: String, value: Float, color: Color, width: Dp) {
    Column(
        verticalArrangement = Arrangement.Bottom, modifier = Modifier.height(200.dp).width(width)
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().weight(value)
        ) {
            drawRect(color = color, size = Size(size.width, size.height))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}