package com.jht.switchStride

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.switchStride.components.CountdownScreen
import com.jht.switchStride.components.ReverseBeltDialogContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SwitchStrideInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialog_shows_correct_text_when_belt_not_reversed() {
        composeTestRule.setContent {
            ReverseBeltDialogContent(
                isBeltInReverse = false,
                onCancel = {},
                onReverse = {},
                testTagPrefix = "SwitchStride"
            )
        }
        composeTestRule
            .onNodeWithText("Reverse to Backward Belt?")
            .assertIsDisplayed()
    }

    @Test
    fun dialog_shows_correct_text_when_belt_is_reversed() {
        composeTestRule.setContent {
            ReverseBeltDialogContent(
                isBeltInReverse = true,
                onCancel = {},
                onReverse = {},
                testTagPrefix = "SwitchStride"
            )
        }
        composeTestRule
            .onNodeWithText("Reverse to Forward Belt?")
            .assertIsDisplayed()
    }

}