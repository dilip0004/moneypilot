package com.yourname.moneypilot.ui.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.moneypilot.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionsPhase2DailyE2ETests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Compatible wait helper (works even if your compose test version does NOT include waitUntil).
     */
    private fun waitUntilTagExists(tag: String, timeoutMs: Long = 8_000) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            composeRule.waitForIdle()
            val found = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            if (found) return
            Thread.sleep(120)
        }
        throw AssertionError("Timeout waiting for tag: $tag")
    }

    private fun openTransactions() {
        // Wait until bottom bar exists
        waitUntilTagExists("bottom_nav_transactions", 8_000)
        composeRule.onNodeWithTag("bottom_nav_transactions").performClick()

        // Wait until Daily tab exists
        waitUntilTagExists("tab_daily", 8_000)
    }

    @Test
    fun addExpenseTransaction_showsInDailyList() {
        openTransactions()

        composeRule.onNodeWithTag("tab_daily").performClick()
        composeRule.onNodeWithTag("fab_add_transaction").performClick()

        waitUntilTagExists("add_tx_save", 8_000)
        composeRule.onNodeWithTag("add_tx_save").assertIsDisplayed()

        val desc = "Tea Phase2"
        composeRule.onNodeWithTag("add_tx_description").performClick()
        composeRule.onNodeWithTag("add_tx_description").performTextInput(desc)

        // Amount is readOnly in some builds (calculator only)
        try {
            composeRule.onNodeWithTag("add_tx_amount").performClick()
            composeRule.onNodeWithTag("add_tx_amount").performTextInput("100")
        } catch (_: Throwable) {
            // ignore
        }

        // Category (required)
        composeRule.onNodeWithTag("add_tx_category").performClick()
        waitUntilTagExists("dropdown_item", 5_000)
        composeRule.onAllNodesWithTag("dropdown_item").onFirst().performClick()

        // Subcategory (optional)
        try {
            composeRule.onNodeWithTag("add_tx_subcategory").performClick()
            waitUntilTagExists("dropdown_item", 2_000)
            composeRule.onAllNodesWithTag("dropdown_item").onFirst().performClick()
        } catch (_: Throwable) { }

        composeRule.onNodeWithTag("add_tx_save").performClick()

        waitUntilTagExists("tx_daily_list", 8_000)
        composeRule.onNodeWithTag("tx_daily_list").assertIsDisplayed()
        composeRule.onNodeWithText(desc).assertIsDisplayed()
    }

    @Test
    fun addTwoTransactions_backToBack_secondDescriptionWorks() {
        openTransactions()
        composeRule.onNodeWithTag("tab_daily").performClick()

        fun addTx(desc: String) {
            composeRule.onNodeWithTag("fab_add_transaction").performClick()
            waitUntilTagExists("add_tx_save", 8_000)

            composeRule.onNodeWithTag("add_tx_description").performClick()
            composeRule.onNodeWithTag("add_tx_description").performTextInput(desc)

            composeRule.onNodeWithTag("add_tx_category").performClick()
            waitUntilTagExists("dropdown_item", 5_000)
            composeRule.onAllNodesWithTag("dropdown_item").onFirst().performClick()

            try {
                composeRule.onNodeWithTag("add_tx_subcategory").performClick()
                waitUntilTagExists("dropdown_item", 2_000)
                composeRule.onAllNodesWithTag("dropdown_item").onFirst().performClick()
            } catch (_: Throwable) { }

            composeRule.onNodeWithTag("add_tx_save").performClick()
        }

        val d1 = "Phase2 Tx1"
        val d2 = "Phase2 Tx2"
        addTx(d1)
        addTx(d2)

        composeRule.onNodeWithText(d1).assertIsDisplayed()
        composeRule.onNodeWithText(d2).assertIsDisplayed()
    }
}
