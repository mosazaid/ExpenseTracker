package com.example.expensetracker.glance

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdateHelper {

    suspend fun updateAllWidgets(context: Context) {
        try {
            PaydayPacingWidget().updateAll(context)
        } catch (_: Exception) {}
        try {
            QuickAddWidget().updateAll(context)
        } catch (_: Exception) {}
    }
}

