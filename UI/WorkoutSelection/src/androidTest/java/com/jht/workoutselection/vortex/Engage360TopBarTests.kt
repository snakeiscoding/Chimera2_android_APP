package com.jht.workoutselection.vortex

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.workoutselection.WorkoutSelectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Engage360TopBarTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeButtonShows() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                isHomeScreen = false
            )
        }

        composeTestRule.onNodeWithTag("HomeButton").assertExists()
        composeTestRule.onNodeWithTag("LanguageButton").assertDoesNotExist()
    }

    @Test
    fun languageButtonShows() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                isHomeScreen = true,
            )
        }

        composeTestRule.onNodeWithTag("LanguageButton").assertExists()
        composeTestRule.onNodeWithTag("HomeButton").assertDoesNotExist()
    }

    @Test
    fun homeButtonCallback() {
        var callbackTriggered = false
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                onHome = {callbackTriggered = true}
            )
        }
        composeTestRule.onNodeWithTag("HomeButton").performClick()
        assert(callbackTriggered)
    }

    @Test
    fun userButton() {
        var callbackTriggered = false
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                onUserButton = {callbackTriggered = true}
            )
        }
        composeTestRule.onNodeWithTag("User:Button").performClick()
        assert(callbackTriggered)
    }

    @Test
    fun ptpButtonCallback() {
        var callbackTriggered = false
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                isPTPUser = true,
                onPtpShow = {callbackTriggered = true}
            )
        }
        composeTestRule.onNodeWithTag("PTPButton").performClick()
        assert(callbackTriggered)
    }


    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isVisibleWhenPTPUserAndNotAlreadyShowing() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                isPTPUser = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertExists()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenPTPAlreadyShowing() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                uiState = mutableStateOf(WorkoutSelectionState.Engage360),
                isPTPUser = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenNotPTPUser() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex()
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenSecaUserEvenIfPTPUser() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                isPTPUser = true,
                isSecaUser = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }
}
