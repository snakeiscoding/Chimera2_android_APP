package com.jht.widgets.numpad

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.widgets.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumPadEntryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testStyle() = NumPadStyle(
        background = Color.White,
        enterBackground = Color.Blue,
        enterForeground = Color.White,
        enterDisabledBackground = Color.LightGray,
        enterDisabledForeground = Color.Gray,
        buttonBackground = Color.LightGray,
        buttonTextStyle = TextStyle(color = Color.Black, fontSize = 20.sp),
        valueBackground = Color.White,
        valueTextStyle = TextStyle(color = Color.Black, fontSize = 20.sp),
        valueBar = Color.Blue,
        popUpStroke = 0.dp,
        numberButtonStroke = 0.dp,
        enterButtonStroke = 0.dp,
        deleteButtonBackground = Color.LightGray,
        deleteButtonStroke = 0.dp,
        valueTextPanelStroke = 0.dp,
        valueTextPanelBackground = Color.White,
        deleteIcon = R.drawable.ic_delete,
        percentageBarWidth = 8.dp,
    )

    private fun setNumPad(
        config: NumPadConfig = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0),
        initialValue: Float = 0f,
        onConfirm: (Float) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val handler = remember {
                NumPadHandler(scope = scope, config = config, initialValue = initialValue)
            }
            val state by handler.state.collectAsState()
            MaterialTheme {
                NumPadEntry(
                    state = state,
                    config = config,
                    style = testStyle(),
                    onDigitPressed = handler::onDigit,
                    onDelete = handler::onDelete,
                    onLongDelete = handler::onLongDelete,
                    onConfirm = { onConfirm(handler.currentFloat()) },
                    onDismiss = onDismiss,
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Initial display
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_initialValueIsDisplayed() {
        setNumPad(
            config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0),
            initialValue = 42f,
        )
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("42"))
    }

    @Test
    fun numPadEntry_zeroInitialValueDisplaysZero() {
        setNumPad(config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0, displayZeroForNoInput = true))
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("0"))
    }

    // -------------------------------------------------------------------------
    // Digit input
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_pressDigitUpdatesDisplay() {
        setNumPad()
        composeTestRule.onNodeWithTag("NumPad5").performClick()
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("5"))
    }

    @Test
    fun numPadEntry_multipleDigitsAccumulate() {
        setNumPad()
        composeTestRule.onNodeWithTag("NumPad1").performClick()
        composeTestRule.onNodeWithTag("NumPad2").performClick()
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("12"))
    }

    @Test
    fun numPadEntry_decimalInputFormatsCorrectly() {
        setNumPad(config = NumPadConfig(min = 0f, max = 15f, decimalPlaces = 1))
        composeTestRule.onNodeWithTag("NumPad5").performClick()
        composeTestRule.onNodeWithTag("NumPad5").performClick()
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("5.5"))
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_deleteRemovesLastDigit() {
        setNumPad()
        composeTestRule.onNodeWithTag("NumPad1").performClick()
        composeTestRule.onNodeWithTag("NumPad2").performClick()
        composeTestRule.onNodeWithTag("NumPadDeleteButton").performClick()
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("1"))
    }

    @Test
    fun numPadEntry_longDeleteClearsToZero() {
        setNumPad(config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0, displayZeroForNoInput = true))
        composeTestRule.onNodeWithTag("NumPad1").performClick()
        composeTestRule.onNodeWithTag("NumPad2").performClick()
        composeTestRule.onNodeWithTag("NumPad3").performClick()
        composeTestRule.onNodeWithTag("NumPadDeleteButton").performTouchInput {
            down(0, center)
            advanceEventTime(600L)
            up(0)
        }
        composeTestRule.onNodeWithTag("NumPadValue").assert(hasText("0"))
    }

    // -------------------------------------------------------------------------
    // Confirm button enabled state
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_confirmButtonDisabledWhenValueBelowMin() {
        setNumPad(config = NumPadConfig(min = 50f, max = 100f, decimalPlaces = 0))
        composeTestRule.onNodeWithTag("NumPad1").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").assertIsNotEnabled()
    }

    @Test
    fun numPadEntry_confirmButtonEnabledWhenValueInRange() {
        setNumPad(config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0))
        composeTestRule.onNodeWithTag("NumPad5").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").assertIsEnabled()
    }

    @Test
    fun numPadEntry_confirmButtonDisabledWhenValueAboveMax() {
        setNumPad(config = NumPadConfig(min = 0f, max = 20f, decimalPlaces = 0))
        composeTestRule.onNodeWithTag("NumPad9").performClick()
        composeTestRule.onNodeWithTag("NumPad9").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").assertIsNotEnabled()
    }

    @Test
    fun numPadEntry_confirmButtonDisabledWhenValueNotOnStep() {
        setNumPad(
            config = NumPadConfig(
                min = 0f, max = 10f,
                decimalPlaces = 1,
                forceStepValues = true,
                stepValue = 0.5f,
            )
        )
        composeTestRule.onNodeWithTag("NumPad3").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").assertIsNotEnabled()
    }

    // -------------------------------------------------------------------------
    // Confirm callback
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_confirmFiresCallbackWithCurrentValue() {
        var confirmed = -1f
        setNumPad(
            config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0),
            onConfirm = { confirmed = it },
        )
        composeTestRule.onNodeWithTag("NumPad7").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").performClick()
        assertEquals(7f, confirmed, 0.001f)
    }

    @Test
    fun numPadEntry_confirmNotFiredWhenButtonDisabled() {
        var confirmed = false
        setNumPad(
            config = NumPadConfig(min = 50f, max = 100f, decimalPlaces = 0),
            onConfirm = { confirmed = true },
        )
        composeTestRule.onNodeWithTag("NumPad1").performClick()
        composeTestRule.onNodeWithTag("NumPadEnterButton").performClick()
        assertEquals(false, confirmed)
    }

    // -------------------------------------------------------------------------
    // Percentage bar labels
    // -------------------------------------------------------------------------

    @Test
    fun numPadEntry_minMaxLabelsVisibleWhenPercentageBarEnabled() {
        setNumPad(
            config = NumPadConfig(
                min = 1f, max = 20f,
                decimalPlaces = 0,
                usePercentageBar = true,
            )
        )
        composeTestRule.onNodeWithTag("NumPadMinValue").assertExists()
        composeTestRule.onNodeWithTag("NumPadMaxValue").assertExists()
    }

    @Test
    fun numPadEntry_minMaxLabelsAbsentWhenPercentageBarDisabled() {
        setNumPad(config = NumPadConfig(min = 0f, max = 100f, usePercentageBar = false))
        composeTestRule.onNodeWithTag("NumPadMinValue").assertDoesNotExist()
        composeTestRule.onNodeWithTag("NumPadMaxValue").assertDoesNotExist()
    }
}
