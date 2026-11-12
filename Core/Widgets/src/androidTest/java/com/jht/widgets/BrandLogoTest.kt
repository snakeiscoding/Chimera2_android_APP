package com.jht.widgets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * [Claude/AI written with @author Samantha.Macknick instructions]
 *
 * Covers only the stateless [BrandLogo] overload (the one that takes [logoUri] and the
 * callbacks directly). The zero-arg overload wires up [BrandLogoState], which resolves a
 * [com.jht.interfaces.models.SystemModel] via [com.jht.widgets.helpers.getInjectionEntryPoint]
 * (Hilt DI) - that wiring isn't exercised here.
 */
class BrandLogoTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /**
     * BrandLogo has no intrinsic size of its own (it sizes to its image content), so tests that
     * need room to drag/swipe give clickModifier an explicit size.
     */
    private val roomyModifier = Modifier.size(200.dp)

    @Test
    fun defaultLogo_isDisplayed() {
        rule.setContent { BrandLogo() }

        rule.onNodeWithTag(brandLogoTestTag).assertIsDisplayed()
    }

    @Test
    fun tap_firesOnClickOnce() {
        var clicks = 0
        rule.setContent { BrandLogo(onClick = { clicks++ }) }

        rule.onNodeWithTag(brandLogoTestTag).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun quickPress_doesNotFireOnLongPress() {
        var clicks = 0
        var longPresses = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            BrandLogo(onClick = { clicks++ }, onLongPress = { longPresses++ })
        }

        rule.onNodeWithTag(brandLogoTestTag).performTouchInput {
            down(center)
            up()
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(1, clicks)
        assertEquals(0, longPresses)
    }

    @Test
    fun heldPress_firesOnLongPressAndNotOnClick() {
        var clicks = 0
        var longPresses = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            BrandLogo(onClick = { clicks++ }, onLongPress = { longPresses++ })
        }

        // BuildConfig.DEBUG is true for test builds, so the overridden long-press timeout is 1000ms.
        rule.onNodeWithTag(brandLogoTestTag).performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithTag(brandLogoTestTag).performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(1, longPresses)
        assertEquals(0, clicks)
    }

    @Test
    fun slowLongDrag_doesNotFireOnDebugFling() {
        var flings = 0
        rule.setContent {
            BrandLogo(clickModifier = roomyModifier, onDebugFling = { flings++ })
        }

        // 100px over 2000ms clears the distance threshold but is far too slow to count as a fling.
        rule.onNodeWithTag(brandLogoTestTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f), delayMillis = 2_000)
            up()
        }
        rule.waitForIdle()

        assertEquals(0, flings)
    }

    @Test
    fun fastShortDrag_doesNotFireOnDebugFling() {
        var flings = 0
        rule.setContent {
            BrandLogo(clickModifier = roomyModifier, onDebugFling = { flings++ })
        }

        // 30px over 10ms is fast enough, but well under the 80f total-movement requirement.
        rule.onNodeWithTag(brandLogoTestTag).performTouchInput {
            down(center)
            moveBy(Offset(30f, 0f), delayMillis = 10)
            up()
        }
        rule.waitForIdle()

        assertEquals(0, flings)
    }
}