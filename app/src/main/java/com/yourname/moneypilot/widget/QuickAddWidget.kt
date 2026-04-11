package com.yourname.moneypilot.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import com.yourname.moneypilot.ui.QuickAddActivity

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MoneyPilotWidgetContent(context)
            }
        }
    }

    @Composable
    private fun MoneyPilotWidgetContent(context: Context) {
        val intent = Intent(context, QuickAddActivity::class.java).apply {
            action = "ACTION_ADD_TRANSACTION"
        }
        val action = actionStartActivity(intent)

        Button(
            text = "💰 MoneyPilot\n+ Add Expense",
            onClick = action,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary
            )
        )
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}