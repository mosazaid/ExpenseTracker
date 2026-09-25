package com.example.expensetracker.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums

private val MAX_BAR_HEIGHT = 160.dp
private val BAR_WIDTH = 48.dp

@Composable
fun StatisticsBarChart(
    income: Float,
    expense: Float,
    wallet: Float
) {
    val max = maxOf(income, expense, kotlin.math.abs(wallet), 1f)
    val incomeRatio = (income / max).coerceIn(0f, 1f)
    val expenseRatio = (expense / max).coerceIn(0f, 1f)
    val walletRatio = (kotlin.math.abs(wallet) / max).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        BarColumn(
            label = stringResource(R.string.income),
            amount = income.toDouble(),
            ratio = incomeRatio,
            color = FinancePositive,
            barWidth = BAR_WIDTH
        )
        BarColumn(
            label = stringResource(R.string.expense),
            amount = expense.toDouble(),
            ratio = expenseRatio,
            color = FinanceNegative,
            barWidth = BAR_WIDTH
        )
        if (wallet > 0f) {
            BarColumn(
                label = stringResource(R.string.wallet),
                amount = wallet.toDouble(),
                ratio = walletRatio,
                color = MaterialTheme.colorScheme.tertiary,
                barWidth = BAR_WIDTH
            )
        }
    }
}

@Composable
private fun BarColumn(
    label: String,
    amount: Double,
    ratio: Float,
    color: Color,
    barWidth: Dp
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "barHeightRatio"
    )
    val barHeight = MAX_BAR_HEIGHT * animatedRatio

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(barWidth + 24.dp)
    ) {
        Text(
            text = CurrencyUtils.formatCurrency(amount),
            style = MaterialTheme.typography.labelSmall.withTabularNums(),
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight.coerceAtLeast(4.dp))
        ) {
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
