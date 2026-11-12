package com.jht.concierge

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.jht.concierge.ui.ConciergeButton
import org.junit.Rule
import org.junit.Test

class ConciergeDialogInstrumentationTests {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Test that the dialog properly shows from clicking the concierge button.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testDialogFromButton() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption0").assertExists()
        rule.onNodeWithTag("FacilityRequestOption1").assertExists()
        rule.onNodeWithTag("FacilityRequestOption2").assertExists()
        rule.onNodeWithTag("FacilityRequestOption3").assertExists()
        rule.onNodeWithTag("FacilityRequestOption4").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Do you require assistance?", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Opened state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestOpened() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption0").assertExists().performClick()
        rule.onNodeWithTag("ConciergeCancelButton").assertExists()
        rule.onNodeWithTag("ConciergeDialogMEDIUM").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Help Requested!", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Acknowledged state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestAcknowledged() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption1").assertExists().performClick()
        rule.onNodeWithTag("HoldToCompleteRequestText", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("ConciergeDialogMEDIUM").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Help is on the way!", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Cancelled state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestCancelled() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption0").assertExists().performClick()
        rule.onNodeWithTag("ConciergeCancelButton").assertExists().performClick()
        rule.onNodeWithTag("IconAboveTitle").assertExists()
        rule.onNodeWithTag("ConciergeDialogSMALL").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Request Canceled", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Error state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestError() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption4").assertExists().performClick()
        rule.onNodeWithTag("IconAboveTitle").assertDoesNotExist()
        rule.onNodeWithTag("ConciergeDialogSMALL").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Error Occurred", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Completed state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestCompleted() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption2").assertExists().performClick()
        rule.onNodeWithTag("IconAboveTitle").assertExists()
        rule.onNodeWithTag("ConciergeDialogSMALL").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Request Complete", substring = true, ignoreCase = true)
    }

    /**
     * Test that the dialog properly transitions to the Request Loading state.
     * Data for this test comes from MockConciergeModel
     */
    @Test
    fun testRequestLoading() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ConciergeButton(
                    Modifier
                        .height(70.dp)
                        .align(Alignment.Center))
            }
        }
        rule.onNodeWithTag("ConciergeButton").assertExists().performClick()
        rule.onNodeWithTag("ConciergeDialogVARIABLE").assertExists()
        rule.onNodeWithTag("FacilityRequestOption3").assertExists().performClick()
        rule.onNodeWithTag("IconAboveTitle").assertDoesNotExist()
        rule.onNodeWithTag("ConciergeDialogSpinner", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("ConciergeDialogSMALL").assertExists()
        rule.onNodeWithTag("ConciergeDialogTitle", useUnmergedTree = true).assertExists().assertTextContains("Requesting Help", substring = true, ignoreCase = true)
    }
}