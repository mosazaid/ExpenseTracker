package com.example.expensetracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.R
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.domain.PacingAnalysis
import com.example.expensetracker.domain.PacingStatus
import com.example.expensetracker.domain.SpendingPacingCalculator
import com.example.expensetracker.presentation.theme.FinanceNegative
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.FinanceWarning
import com.example.expensetracker.presentation.theme.withTabularNums
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun FinancialInsightsCard(
    totalIncome: Double,
    totalExpense: Double,
    totalWallet: Double = 0.0,
    periodStartDate: Date? = null,
    periodEndDate: Date? = null,
    cycleLabel: String? = null,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    val analysis = remember(totalIncome, totalExpense, totalWallet, periodStartDate, periodEndDate, cycleLabel) {
        SpendingPacingCalculator.analyze(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            walletReserved = totalWallet,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            cycleLabel = cycleLabel
        )
    }

    val (accentColor, statusIcon, statusLabel) = when (analysis.status) {
        PacingStatus.DEFICIT_CRITICAL -> Triple(FinanceNegative, Icons.Outlined.Warning, "Deficit Alert")
        PacingStatus.HIGH_BURN -> Triple(FinanceWarning, Icons.Outlined.Speed, "High Velocity")
        PacingStatus.MODERATE_BURN -> Triple(Color(0xFFF59E0B), Icons.Outlined.Speed, "Elevated")
        PacingStatus.OPTIMAL -> Triple(MaterialTheme.colorScheme.primary, Icons.Outlined.CheckCircle, "On Track")
        PacingStatus.SUPER_SAVER -> Triple(FinancePositive, Icons.Outlined.TrendingDown, "Super Saver")
    }

    val animatedSpentProgress by animateFloatAsState(
        targetValue = analysis.budgetConsumedRatio.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "spentProgress"
    )

    if (showInfoDialog) {
        FinancialInsightsInfoDialog(
            analysis = analysis,
            onDismiss = { showInfoDialog = false }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (analysis.status == PacingStatus.HIGH_BURN || analysis.status == PacingStatus.DEFICIT_CRITICAL)
                accentColor.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Sparkle + Title + Info Icon + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.financial_ai_insights_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.pacing_info_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Day ${analysis.dayOfPeriod} of ${analysis.totalPeriodDays} • ${analysis.daysRemaining} days left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            // Dual Progress Comparison: Cycle Days Elapsed vs Income Consumed
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Budget Consumed: ${(analysis.budgetConsumedRatio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (analysis.budgetConsumedRatio > analysis.timeElapsedRatio + 0.15f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Time Elapsed: ${(analysis.timeElapsedRatio * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Dual progress track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    // Time elapsed marker indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = analysis.timeElapsedRatio)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                    // Spent amount progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedSpentProgress)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                }
            }

            // Safe Daily Spend & Run-Rate Highlight Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Safe Daily Allowance Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Safe Daily Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(analysis.safeDailySpendRemaining),
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = "For next ${analysis.daysRemaining} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Projected Month-End Difference
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (analysis.projectedDifference >= 0) "Projected Surplus" else "Projected Deficit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(kotlin.math.abs(analysis.projectedDifference)),
                            style = MaterialTheme.typography.titleMedium.withTabularNums(),
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.projectedDifference >= 0) FinancePositive else FinanceNegative
                        )
                        Text(
                            text = "At current pace",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // AI Actionable Tip Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = analysis.adviceMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialInsightsInfoDialog(
    analysis: PacingAnalysis,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.pacing_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.pacing_info_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Explanations
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Cycle Info
                    InfoExplanationCard(
                        icon = Icons.Outlined.CalendarMonth,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.pacing_info_cycle_title),
                        description = stringResource(R.string.pacing_info_cycle_desc),
                        valueHighlight = "Day ${analysis.dayOfPeriod} of ${analysis.totalPeriodDays} (${(analysis.timeElapsedRatio * 100).roundToInt()}% elapsed)"
                    )

                    // 2. Spending Velocity
                    InfoExplanationCard(
                        icon = Icons.Outlined.Speed,
                        iconTint = Color(0xFFF59E0B),
                        title = stringResource(R.string.pacing_info_velocity_title),
                        description = stringResource(R.string.pacing_info_velocity_desc),
                        valueHighlight = "Current Velocity: ${String.format(java.util.Locale.US, "%.1fx", analysis.velocityRatio)} (Spent ${(analysis.budgetConsumedRatio * 100).roundToInt()}% of budget)"
                    )

                    // 3. Early Cycle Note
                    InfoExplanationCard(
                        icon = Icons.Outlined.Payments,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = stringResource(R.string.pacing_info_early_cycle_title),
                        description = stringResource(R.string.pacing_info_early_cycle_desc),
                        valueHighlight = if (analysis.isEarlyCycle) "Active Now: Early Cycle Damping Engaged" else "Past Early Window"
                    )

                    // 4. Safe Daily Spend
                    InfoExplanationCard(
                        icon = Icons.Outlined.Savings,
                        iconTint = FinancePositive,
                        title = stringResource(R.string.pacing_info_safe_spend_title),
                        description = stringResource(R.string.pacing_info_safe_spend_desc),
                        valueHighlight = "Recommended Allowance: ${CurrencyUtils.formatCurrency(analysis.safeDailySpendRemaining)} / day"
                    )

                    // 5. Projected Outcome
                    InfoExplanationCard(
                        icon = if (analysis.projectedDifference >= 0) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        iconTint = if (analysis.projectedDifference >= 0) FinancePositive else FinanceNegative,
                        title = stringResource(R.string.pacing_info_projection_title),
                        description = stringResource(R.string.pacing_info_projection_desc),
                        valueHighlight = if (analysis.projectedDifference >= 0)
                            "Surplus: +${CurrencyUtils.formatCurrency(analysis.projectedDifference)}"
                        else
                            "Deficit: -${CurrencyUtils.formatCurrency(kotlin.math.abs(analysis.projectedDifference))}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.pacing_info_close), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoExplanationCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    valueHighlight: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconTint.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = valueHighlight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}
