package com.yourname.moneypilot.ui.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.moneypilot.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 0+1 tests:
 * - Validate screens load and essential navigation works.
 * - Validate input fields accept text (no "stuck" description regressions).
 */
@RunWith(AndroidJUnit4::class)
class TransactionsPhase01Tests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun transactions_dailyTab_loads() {
        // By default app opens to Records hub. Daily tab should exist.
        composeRule.onNodeWithTag("tab_daily").assertIsDisplayed()
        composeRule.onNodeWithTag("transactions_daily_root").assertIsDisplayed()
    }

    @Test
    fun transactions_openAddTransactionScreen_fromFab() {
        // FAB lives in Dashboard hub.
        composeRule.onNodeWithTag("fab_add_transaction").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("add_tx_save").assertIsDisplayed()
    }

    @Test
    fun addTransaction_description_acceptsText() {
        composeRule.onNodeWithTag("fab_add_transaction").performClick()
        composeRule.onNodeWithTag("add_tx_description").assertIsDisplayed()
            .performTextInput("Test description")
    }

    @Test
    fun transactions_switchTabs_noCrash() {
        composeRule.onNodeWithTag("tab_calendar").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tab_monthly").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tab_total").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tab_daily").assertIsDisplayed().performClick()
    }
}
