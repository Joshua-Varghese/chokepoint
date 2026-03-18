package com.joshua.chokepoint

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLoginAndDashboard() {
        composeTestRule.waitForIdle()

        // Check if Login Screen is visible
        val loginHeader = composeTestRule.onAllNodesWithText("Welcome Back").fetchSemanticsNodes()
        if (loginHeader.isNotEmpty()) {
            composeTestRule.onNodeWithText("Email").performTextInput("admin@chokepoint.com")
            composeTestRule.onNodeWithText("Password").performTextInput("Admin123!")
            composeTestRule.onNodeWithText("Login").performClick()
        }

        // Wait for Dashboard to appear (timeout increased for remote device latency)
        composeTestRule.waitUntil(20000) {
            composeTestRule.onAllNodesWithText("Dashboard").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify key Dashboard elements
        composeTestRule.onNodeWithText("Dashboard").assertIsDisplayed()
        
        // Wait a bit to ensure Firestore data loads
        composeTestRule.mainClock.advanceTimeBy(3000)
    }
}
