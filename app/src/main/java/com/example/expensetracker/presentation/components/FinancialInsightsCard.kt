package com.example.expensetracker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning

data class InsightUiModel(
    val title: String,
    val message: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun FinancialInsightsCard(
    totalIncome: Double,
    totalExpense: Double,
    totalWallet: Double,
    modifier: Modifier = Modifier
) {
    val insight = remember(totalIncome, totalExpense, totalWallet) {
        val remaining = totalIncome - totalExpense - totalWallet
        val spendRatio = if (totalIncome > 0) (totalExpense / totalIncome) else 0.0
        val spendPercent = (spendRatio * 100).toInt()

        when {
            remaining < 0 -> InsightUiModel(
                title = "Deficit Warning",
                message = "Your expenses and wallet moves currently exceed your monthly income.",
                icon = Icons.Outlined.Warning,
                accentColor = FinanceNegative
            )
            spendRatio > 0.90 -> InsightUiModel(
                title = "High Spending",
                message = "You have used $spendPercent% of your income this month. Consider pacing remaining expenses.",
                icon = Icons.Outlined.Warning,
                accentColor = FinanceWarning
            )
            spendRatio in 0.01..0.60 && totalExpense > 0 -> InsightUiModel(
                title = "Healthy Pace",
                message = "You have only used $spendPercent% of your income this month. Great financial management!",
                icon = Icons.Outlined.CheckCircle,
                accentColor = FinancePositive
            )
            else -> InsightUiModel(
                title = "Balanced Budget",
                message = "Your finances are steady this month. Keep logging your transactions daily.",
                icon = Icons.Outlined.Lightbulb,
                accentColor = Color(0xFF3B5F74) // Slate primary
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Accent Pill
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(insight.accentColor)
            )

            // Icon container
            Surface(
                shape = CircleShape,
                color = insight.accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = insight.icon,
                        contentDescription = null,
                        tint = insight.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.financial_insights_title) + " • " + insight.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = insight.accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
