package com.jht.workoutselection.chameleon

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.workoutselection.WorkoutSelectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSelectionChameleonScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isVisibleWhenPTPUserAndNotAlreadyShowing() {
        composeTestRule.setContent {
            WorkoutSelectionScreenChameleon(
                isPTPUser = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertExists()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenPTPAlreadyShowing() {
        composeTestRule.setContent {
            WorkoutSelectionScreenChameleon(
                isPTPUser = true,
                uiState = mutableStateOf(WorkoutSelectionState.Engage360),
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenNotPTPUser() {
        composeTestRule.setContent {
            WorkoutSelectionScreenChameleon(
                isPTPUser = false,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenSecaUserEvenIfPTPUser() {
        composeTestRule.setContent {
            WorkoutSelectionScreenChameleon(
                isPTPUser = true,
                isSecaUser = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }

    // CGC-23055: Verifies PTP button visibility is driven by isPTPUser, not isPTPShowing
    @Test
    fun ptpButton_isNotVisibleWhenGuestEvenIfPTPUser() {
        composeTestRule.setContent {
            WorkoutSelectionScreenChameleon(
                isPTPUser = true,
                isGuest = true,
            )
        }

        composeTestRule.onNodeWithTag("PTPButton").assertDoesNotExist()
    }
}
