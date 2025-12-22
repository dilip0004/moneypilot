package com.yourname.moneypilot.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.yourname.moneypilot.ui.MainActivity

class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MoneyPilotWidgetContent()
        }
    }

    @Composable
    private fun MoneyPilotWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1C1B1F)))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MoneyPilot",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.White)
                )
            )
            
            Spacer(GlanceModifier.height(8.dp))
            
            Button(
                text = "+ Add Expense",
                onClick = actionStartActivity<MainActivity>(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(Color(0xFF0067FF)),
                    contentColor = ColorProvider(Color.White)
                )
            )
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
