package com.yourname.moneypilot.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import com.yourname.moneypilot.ui.MainActivity

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MoneyPilotWidgetContent()
            }
        }
    }

    @Composable
    private fun MoneyPilotWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MoneyPilot",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )

            Spacer(GlanceModifier.height(8.dp))

            Button(
                text = "+ Add Expense",
                onClick = actionStartActivity<MainActivity>(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary
                )
            )
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
