package com.yourname.moneypilot.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.unit.ColorProvider
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.ui.QuickAddActivity
import java.time.LocalDate
import java.time.LocalTime

class QuickAddWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(100.dp, 100.dp), DpSize(250.dp, 200.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = MoneyPilotDatabase.getDatabase(context)
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = LocalDate.now().atTime(LocalTime.MAX)
        
        val spentToday = db.transactionDao().getTotalSumByType(TransactionType.Expense, todayStart, todayEnd) ?: 0.0
        val inflowToday = db.transactionDao().getTotalSumByType(TransactionType.Income, todayStart, todayEnd) ?: 0.0
        val totalBalance = db.walletDao().getTotalBalance() ?: 0.0

        provideContent {
            GlanceTheme {
                MoneyPilotWidgetContent(spentToday, inflowToday, totalBalance)
            }
        }
    }

    @Composable
    private fun MoneyPilotWidgetContent(spentToday: Double, inflowToday: Double, totalBalance: Double) {
        val context = LocalContext.current
        val size = LocalSize.current
        val isExpanded = size.width >= 200.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(24.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExpanded) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MoneyPilot Summary",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryItem("Spent", spentToday, GlanceTheme.colors.error, GlanceModifier.defaultWeight())
                if (isExpanded) {
                    SummaryItem("Inflow", inflowToday, GlanceTheme.colors.tertiary, GlanceModifier.defaultWeight())
                }
                SummaryItem("Balance", totalBalance, GlanceTheme.colors.primary, GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Primary Action
            PrimaryActionButton(
                action = createAction(context, null),
                modifier = GlanceModifier.fillMaxWidth()
            )

            if (isExpanded) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuickAction("Expense", createAction(context, TransactionType.Expense), GlanceModifier.defaultWeight())
                    QuickAction("Income", createAction(context, TransactionType.Income), GlanceModifier.defaultWeight())
                    QuickAction("Transfer", createAction(context, TransactionType.Transfer), GlanceModifier.defaultWeight())
                }
            }
        }
    }

    @Composable
    private fun SummaryItem(label: String, amount: Double, color: ColorProvider, modifier: GlanceModifier = GlanceModifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
            )
            Text(
                text = "₹${amount.toInt()}",
                style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun PrimaryActionButton(action: Action, modifier: GlanceModifier = GlanceModifier) {
        Box(
            modifier = modifier
                .background(GlanceTheme.colors.primary)
                .cornerRadius(16.dp)
                .padding(vertical = 8.dp)
                .clickable(action),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Add Transaction",
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun QuickAction(text: String, action: Action, modifier: GlanceModifier = GlanceModifier) {
        Box(
            modifier = modifier
                .padding(horizontal = 4.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(vertical = 6.dp)
                .clickable(action),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            )
        }
    }

    private fun createAction(context: Context, type: TransactionType?): Action {
        val intent = Intent(context, QuickAddActivity::class.java).apply {
            action = "ACTION_ADD_TRANSACTION"
            if (type != null) {
                putExtra("type", type.name)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return actionStartActivity(intent)
    }

    companion object {
        suspend fun update(context: Context) {
            QuickAddWidget().updateAll(context)
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
