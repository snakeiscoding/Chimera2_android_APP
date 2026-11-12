package com.jht.landmarks.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jht.landmarks.common.components.LevelControlPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LandmarkLevelControlPanelTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun initialRender_displaysCorrectLevel() { //Mount the Composable with an initial level = 5 and verify it is displayed on the screen.
        rule.setContent {
            LevelControlPanel(
                level = 5,
                maxLevel = 20,
                onLevelUp = {},
                onLevelDown = {},
                onLevelSet = {}
            )
        }
        rule.onNodeWithText("5").assertIsDisplayed()
        rule.onNodeWithContentDescription("Increase Level").assertIsDisplayed()
        rule.onNodeWithContentDescription("Decrease Level").assertIsDisplayed()
    }

    @Test
    fun singleTapUp_firesOnLevelUpOnce() { //Verify onLevelUp callback is fired exactly once
        var upCount = 0
        rule.setContent {
            LevelControlPanel(
                level = 4,
                maxLevel = 20,
                onLevelUp = { upCount++ },
                onLevelDown = {},
                onLevelSet = {}
            )
        }
        rule.onNodeWithContentDescription("Increase Level").performClick()
        rule.waitForIdle()

        assertEquals(1, upCount)
    }

    @Test
    fun singleTapDown_firesOnLevelDownOnce() { //Verify onLevelDown callback is fired exactly once
        var downCount = 0
        rule.setContent {
            LevelControlPanel(
                level = 6,
                maxLevel = 20,
                onLevelUp = {},
                onLevelDown = { downCount++ },
                onLevelSet = {}
            )
        }
        rule.onNodeWithContentDescription("Decrease Level").performClick()
        rule.waitForIdle()

        assertEquals(1, downCount)
    }

    @Test
    fun longPressUp_enforcesLimitOfThree() {
        var upCount = 0
        rule.mainClock.autoAdvance = false

        rule.setContent {
            LevelControlPanel(
                level = 5,
                maxLevel = 20,
                onLevelUp = { upCount++ },
                onLevelDown = {},
                onLevelSet = {}
            )
        }

        rule.onNodeWithContentDescription("Increase Level").performTouchInput {
            down(center) // Simulate touch down and hold
        }

        // Advance clock by enough time to trigger initial press + 3 repeated increments
        // Initial press happens immediately. Wait 900ms, then it increments up to 3 times (every 100ms)
        rule.mainClock.advanceTimeBy(900 + (100 * 5).toLong())
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Increase Level").performTouchInput {
            up() // Release the touch
        }
        rule.mainClock.advanceTimeByFrame()

        assertEquals(3, upCount) // Should be 3 total (1 initial + 2 held increments, capped by the `upChangesCount < 3` logic)
    }

    @Test
    fun longPressDown_enforcesBoundaryAtOne() {
        var downCount = 0
        rule.mainClock.autoAdvance = false

        rule.setContent {
            var currentLevel by androidx.compose.runtime.remember { mutableIntStateOf(4) }
            LevelControlPanel(
                level = currentLevel,
                maxLevel = 20,
                onLevelUp = {},
                onLevelDown = {
                    if (currentLevel > 1) {
                        currentLevel--
                        downCount++
                    }
                },
                onLevelSet = {}
            )
        }

        rule.onNodeWithContentDescription("Decrease Level").performTouchInput {
            down(center) // Simulate touch down and hold
        }

        // Start at 4. 4->3 on initial hit.
        // After 900ms: 3->2,
        // After 1000ms: 2->1,
        // After 1100ms: Should check if level > 1 and stop.
        rule.mainClock.advanceTimeBy(3000L)
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Decrease Level").performTouchInput {
            up() // Release the touch
        }

        assertEquals(3, downCount) // Initial level was 4, min level is 1. The count of decrements should be exactly 3.
    }

}
