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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.core.format.CurrencyUtils

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = GlanceDataRepository.getQuickAddData(context)

        provideContent {
            QuickAddContent(context = context, data = data)
        }
    }

    @Composable
    private fun QuickAddContent(context: Context, data: QuickAddWidgetData) {
        val navKey = ActionParameters.Key<String>("navigate_to")
        val addExpenseAction = actionStartActivity<MainActivity>(
            actionParametersOf(navKey to "add_expense")
        )
        val openAppAction = actionStartActivity<MainActivity>()

        val surfaceColor = ColorProvider(Color(0xFF1E293B))
        val textPrimary = ColorProvider(Color(0xFFF8FAFC))
        val textSecondary = ColorProvider(Color(0xFF94A3B8))
        val primaryColor = ColorProvider(Color(0xFF0D9488))
        val textWhite = ColorProvider(Color(0xFFFFFFFF))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .cornerRadius(18.dp)
                .padding(10.dp)
                .clickable(openAppAction)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Spent Today",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(data.todaySpend),
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                Box(
                    modifier = GlanceModifier
                        .background(primaryColor)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clickable(addExpenseAction)
                ) {
                    Text(
                        text = "+ Add",
                        style = TextStyle(
                            color = textWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

