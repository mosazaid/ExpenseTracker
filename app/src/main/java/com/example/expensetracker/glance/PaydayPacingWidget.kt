package com.example.expensetracker.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.domain.PacingStatus

class PaydayPacingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = GlanceDataRepository.getPaydayPacingData(context)

        provideContent {
            PaydayPacingContent(context = context, data = data)
        }
    }

    @Composable
    private fun PaydayPacingContent(context: Context, data: PaydayPacingWidgetData) {
        val navKey = ActionParameters.Key<String>("navigate_to")
        val addExpenseAction = actionStartActivity<MainActivity>(
            actionParametersOf(navKey to "add_expense")
        )
        val openAppAction = actionStartActivity<MainActivity>()

        val surfaceColor = ColorProvider(Color(0xFF1E293B))
        val textPrimary = ColorProvider(Color(0xFFF8FAFC))
        val textSecondary = ColorProvider(Color(0xFF94A3B8))
        val primaryColor = ColorProvider(Color(0xFF0D9488))
        val pillBg = ColorProvider(Color(0xFF334155))
        val textWhite = ColorProvider(Color(0xFFFFFFFF))

        val statusColor = when (data.pacingStatus) {
            PacingStatus.DEFICIT_CRITICAL -> ColorProvider(Color(0xFFEF4444))
            PacingStatus.HIGH_BURN -> ColorProvider(Color(0xFFF97316))
            PacingStatus.MODERATE_BURN -> ColorProvider(Color(0xFFFBBF24))
            PacingStatus.OPTIMAL -> primaryColor
            PacingStatus.SUPER_SAVER -> ColorProvider(Color(0xFF22C55E))
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .cornerRadius(18.dp)
                .padding(12.dp)
                .clickable(openAppAction)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // ── Top Row: App Title & Payday Countdown Badge
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Tracker",
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    // Countdown Badge
                    Box(
                        modifier = GlanceModifier
                            .background(pillBg)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${data.daysToPayday}d to payday",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ── Metric Row: Available Liquid Cash & Safe Daily Spend
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Liquid Cash
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Liquid Available",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(data.liquidCash),
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Safe Daily Spend
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Safe Daily",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "${CurrencyUtils.formatCurrency(data.safeDailySpend)}/d",
                            style = TextStyle(
                                color = statusColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // ── Bottom Row: Today Spend & Quick Add Action
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Today: ${CurrencyUtils.formatCurrency(data.todaySpend)}",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = data.pacingLabel,
                            style = TextStyle(
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Refresh Button
                    Box(
                        modifier = GlanceModifier
                            .background(pillBg)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .clickable(actionRunCallback<RefreshPaydayPacingAction>())
                    ) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // + Expense Quick Button
                    Box(
                        modifier = GlanceModifier
                            .background(primaryColor)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable(addExpenseAction)
                    ) {
                        Text(
                            text = "+ Expense",
                            style = TextStyle(
                                color = textWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class RefreshPaydayPacingAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        PaydayPacingWidget().update(context, glanceId)
    }
}

class PaydayPacingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PaydayPacingWidget()
}

