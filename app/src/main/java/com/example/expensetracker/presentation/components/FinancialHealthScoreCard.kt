package com.example.expensetracker.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning
import com.example.expensetracker.presentation.theme.withTabularNums

@Composable
fun FinancialHealthScoreCard(
    totalIncome: Double,
    totalExpense: Double,
    budgetAdherenceRatio: Float = 0.85f,
    activeLoansUnpaidCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Calculate Financial Health Score (0 to 100)
    val savingsRate = if (totalIncome > 0) {
        ((totalIncome - totalExpense) / totalIncome).toFloat().coerceIn(0f, 1f)
    } else 0f

    val savingsScore = (savingsRate * 35).coerceIn(0f, 35f)
    val budgetScore = (budgetAdherenceRatio * 35).coerceIn(0f, 35f)
    val burnRateScore = if (totalExpense <= totalIncome) 15f else (15f * (totalIncome / (totalExpense + 0.01)).toFloat()).coerceIn(0f, 15f)
    val disciplineScore = if (activeLoansUnpaidCount == 0) 15f else 5f

    val totalScore = (savingsScore + budgetScore + burnRateScore + disciplineScore).toInt().coerceIn(0, 100)

    val (tierLabel, tierBadge, tierColor) = when {
        totalScore >= 90 -> Triple("Master Saver", "🌟", Color(0xFF10B981))
        totalScore >= 75 -> Triple("Smart Planner", "🛡️", MaterialTheme.colorScheme.primary)
        totalScore >= 50 -> Triple("Balanced", "⚖️", FinanceWarning)
        else -> Triple("Attention Needed", "⚠️", FinanceNegative)
    }

    val animatedScoreProgress by animateFloatAsState(
        targetValue = totalScore / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "healthScoreAnim"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = tierColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = tierColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Financial Health Score",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$tierBadge $tierLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tierColor
                            )
                        }
                    }
                }

                // Mini Circular Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 5.dp.toPx()
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = tierColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animatedScoreProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$totalScore",
                        style = MaterialTheme.typography.titleMedium.withTabularNums(),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Streak Counter Pill
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "3-Week Budget Streak!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expandable breakdown
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    HealthMetricRow(
                        icon = Icons.Outlined.CheckCircle,
                        label = "Budget Adherence",
                        detail = "${(budgetAdherenceRatio * 100).toInt()}% on budget",
                        scoreText = "${budgetScore.toInt()} / 35 pts"
                    )

                    HealthMetricRow(
                        icon = Icons.Outlined.Speed,
                        label = "Savings Ratio",
                        detail = "${(savingsRate * 100).toInt()}% saved this cycle",
                        scoreText = "${savingsScore.toInt()} / 35 pts"
                    )

                    HealthMetricRow(
                        icon = Icons.Outlined.Shield,
                        label = "Discipline & Loans",
                        detail = if (activeLoansUnpaidCount == 0) "No overdue liabilities" else "$activeLoansUnpaidCount pending payments",
                        scoreText = "${disciplineScore.toInt()} / 15 pts"
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetricRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    scoreText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = scoreText,
            style = MaterialTheme.typography.labelSmall.withTabularNums(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
