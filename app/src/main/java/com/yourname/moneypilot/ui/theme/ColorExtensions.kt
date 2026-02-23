package androidx.compose.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.yourname.moneypilot.ui.theme.IncomeGreen
import com.yourname.moneypilot.ui.theme.ExpenseRed
import com.yourname.moneypilot.ui.theme.BudgetBlue
import com.yourname.moneypilot.ui.theme.GoalOrange

// Semantic finance colors exposed on ColorScheme for consistent usage across the app
val ColorScheme.income: Color
    get() = IncomeGreen

val ColorScheme.expense: Color
    get() = ExpenseRed

val ColorScheme.budget: Color
    get() = BudgetBlue

val ColorScheme.goal: Color
    get() = GoalOrange
