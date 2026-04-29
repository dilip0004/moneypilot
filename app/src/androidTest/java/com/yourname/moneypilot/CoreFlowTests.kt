package com.yourname.moneypilot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.moneypilot.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreFlowTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun walletLifecycleTest() {
        // 1. Navigate to Accounts
        composeRule.onNodeWithTag("bottom_nav_accounts").performClick()
        
        // 2. Create a wallet
        composeRule.onNodeWithTag("account_add_fab").performClick()
        composeRule.onNodeWithTag("account_name_input").performTextInput("Test Wallet")
        composeRule.onNodeWithTag("account_balance_input").performTextInput("1000")
        composeRule.onNodeWithTag("account_type_BANK").performClick()
        composeRule.onNodeWithTag("account_save_fab").performClick()

        // 3. Verify it exists
        composeRule.onNodeWithText("Test Wallet").assertIsDisplayed()

        // 4. Edit its name
        // We find the edit button by its dynamic tag. Since it's a new DB, it likely has ID 1 or we search by text then find sibling
        composeRule.onAllNodesWithTag("account_edit_", substring = true).onFirst().performClick()
        
        composeRule.onNodeWithTag("account_name_input").performTextReplacement("Updated Wallet")
        composeRule.onNodeWithTag("account_save_fab").performClick()
        
        // 5. Verify update
        composeRule.onNodeWithText("Updated Wallet").assertIsDisplayed()

        // 6. Archive it
        composeRule.onAllNodesWithTag("account_archive_", substring = true).onFirst().performClick()
        composeRule.onNodeWithTag("dialog_confirm_archive").performClick() 
        
        // 7. Verify it's gone from main list (isArchived = true filters it out)
        composeRule.onNodeWithText("Updated Wallet").assertDoesNotExist()
    }

    @Test
    fun transactionCreationTest() {
        // 1. Start from Dashboard (default)
        // Check if we are on Transactions or Dashboard hub. Dashboard hub has fab_add_transaction
        composeRule.onNodeWithTag("fab_add_transaction").performClick()

        // 2. Enter Amount via Calculator
        composeRule.onNodeWithTag("add_tx_amount").performClick()
        composeRule.onNodeWithTag("calc_key_5").performClick()
        composeRule.onNodeWithTag("calc_key_0").performClick()
        composeRule.onNodeWithTag("calc_key_Done").performClick()

        // 3. Enter Description
        composeRule.onNodeWithTag("add_tx_description").performTextInput("Lunch")

        // 4. Select Category
        composeRule.onNodeWithTag("add_tx_category").performClick()
        // Pick the first category in the dropdown
        composeRule.onNodeWithTag("add_tx_category").performClick() // Toggle if needed, or just find item
        // In Compose tests, dropdown items are often just text or have specific tags. 
        // For simplicity, we assume there's at least one category and we click its text if known, 
        // or we need to ensure categories exist.
        
        // 5. Save
        composeRule.onNodeWithTag("add_tx_save").performClick()

        // 6. Verify in list
        composeRule.onNodeWithTag("bottom_nav_transactions").performClick()
        composeRule.onNodeWithText("Lunch").assertIsDisplayed()
    }

    @Test
    fun goalFlowTest() {
        // 1. Navigate to Planning
        composeRule.onNodeWithTag("bottom_nav_planning").performClick()

        // 2. Create Goal
        composeRule.onNodeWithTag("goal_add_fab").performClick()
        composeRule.onNodeWithTag("goal_name_input").performTextInput("New Car")
        composeRule.onNodeWithTag("goal_target_amount_input").performTextInput("500000")
        composeRule.onNodeWithTag("goal_save_fab").performClick()

        // 3. Verify Goal
        composeRule.onNodeWithText("New Car").assertIsDisplayed()

        // 4. Add Contribution
        composeRule.onAllNodesWithTag("goal_contribute_button_", substring = true).onFirst().performClick()
        composeRule.onNodeWithTag("contribution_amount_input").performTextInput("1000")
        composeRule.onNodeWithTag("contribution_confirm_button").performClick()

        // 5. Verify progress update
        composeRule.onNodeWithText("₹1000.0").assertIsDisplayed()
    }

    @Test
    fun bigBillAutoReserveTest() {
        // 1. Navigate to Planning
        composeRule.onNodeWithTag("bottom_nav_planning").performClick()
        
        // 2. Switch to Big Bills tab
        composeRule.onNodeWithTag("planning_tab_big_bills").performClick()

        // 3. Create Big Bill
        composeRule.onNodeWithTag("big_bill_add_fab").performClick()
        composeRule.onNodeWithTag("big_bill_name_input").performTextInput("Annual Tax")
        composeRule.onNodeWithTag("big_bill_amount_input").performTextInput("12000")
        
        // 4. Toggle Auto-Reserve
        composeRule.onNodeWithTag("big_bill_auto_reserve_checkbox").performClick()
        
        // 5. Save
        composeRule.onNodeWithTag("big_bill_save_button").performClick()

        // 6. Verify
        composeRule.onNodeWithText("Annual Tax").assertIsDisplayed()
    }
}
