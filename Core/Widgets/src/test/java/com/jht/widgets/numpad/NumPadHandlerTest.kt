package com.jht.widgets.numpad

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NumPadHandlerTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun TestScope.handler(
        config: NumPadConfig = NumPadConfig(min = 0f, max = 100f),
        initialValue: Float = 0f,
        onDismiss: () -> Unit = {},
    ) = NumPadHandler(this, config, initialValue, onDismiss)

    private val NumPadHandler.display get() = state.value.displayValue
    private val NumPadHandler.canConfirm get() = state.value.isConfirmEnabled
    private val NumPadHandler.isAnimating get() = state.value.isAnimating
    private val NumPadHandler.percentageFraction get() = state.value.percentageFraction
    private val NumPadHandler.showPercentageBar get() = state.value.showPercentageBar
    private val NumPadHandler.unitLabel get() = state.value.unitLabel

    // -------------------------------------------------------------------------
    // Digit appending
    // -------------------------------------------------------------------------

    @Test
    fun `onDigit appends digit to empty buffer`() = runTest {
        val h = handler()
        h.onDigit("5")
        assertEquals("5", h.display)
    }

    @Test
    fun `onDigit appends multiple digits`() = runTest {
        val h = handler()
        h.onDigit("1")
        h.onDigit("2")
        h.onDigit("3")
        assertEquals("123", h.display)
    }

    @Test
    fun `onDigit after sentinel replaces buffer with new digit`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f), initialValue = 50f)
        h.onDigit("3")
        assertEquals("3", h.display)
    }

    @Test
    fun `onDigit prevents multiple leading zeros`() = runTest {
        val h = handler()
        h.onDigit("0")
        h.onDigit("0")
        assertEquals("0", h.display)
    }

    @Test
    fun `onDigit allows non-zero digit after single zero`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0))
        h.onDigit("0")
        h.onDigit("5")
        assertEquals("5", h.display)
    }

    @Test
    fun `onDigit respects maxDigits limit`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 999f, decimalPlaces = 0))
        repeat(10) { h.onDigit("1") }
        assertEquals("111", h.display)
    }

    @Test
    fun `onDigit blocked while animating`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 10f))
        h.onDigit("1")
        advanceTimeBy(2001)
        assertTrue("expected animating", h.isAnimating)
        val displayBeforeAttempt = h.display
        h.onDigit("9")
        assertEquals(displayBeforeAttempt, h.display)
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Test
    fun `onDelete removes last digit`() = runTest {
        val h = handler()
        h.onDigit("1")
        h.onDigit("2")
        h.onDelete()
        assertEquals("1", h.display)
    }

    @Test
    fun `onDelete on single digit clears to zero display`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, displayZeroForNoInput = true))
        h.onDigit("5")
        h.onDelete()
        assertEquals("0", h.display)
    }

    @Test
    fun `onDelete on sentinel buffer clears to empty`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f), initialValue = 50f)
        advanceTimeBy(2001)
        h.onDelete()
        assertEquals("0", h.display)
    }

    @Test
    fun `onLongDelete clears entire buffer`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, displayZeroForNoInput = true))
        h.onDigit("1")
        h.onDigit("2")
        h.onDigit("3")
        h.onLongDelete()
        assertEquals("0", h.display)
    }

    @Test
    fun `onDelete blocked while animating`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 10f))
        h.onDigit("1")
        advanceTimeBy(2001)
        assertTrue(h.isAnimating)
        val displayBefore = h.display
        h.onDelete()
        assertEquals(displayBefore, h.display)
    }

    // -------------------------------------------------------------------------
    // currentFloat
    // -------------------------------------------------------------------------

    @Test
    fun `currentFloat returns initial value when no digits entered`() = runTest {
        val h = handler(initialValue = 42f)
        assertEquals(42f, h.currentFloat())
    }

    @Test
    fun `currentFloat returns entered value for digit buffer`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0))
        h.onDigit("5")
        h.onDigit("5")
        assertEquals(55f, h.currentFloat())
    }

    @Test
    fun `currentFloat with decimal places converts correctly`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 99f, decimalPlaces = 1))
        h.onDigit("5")
        h.onDigit("5")
        assertEquals(5.5f, h.currentFloat())
    }

    @Test
    fun `currentFloat returns min after autocorrect snaps to min`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 10f))
        h.onDigit("1")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(5f, h.currentFloat())
    }

    @Test
    fun `currentFloat returns max after autocorrect snaps to max`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 10f))
        h.onDigit("9")
        h.onDigit("9")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(10f, h.currentFloat())
    }

    // -------------------------------------------------------------------------
    // isConfirmEnabled
    // -------------------------------------------------------------------------

    @Test
    fun `isConfirmEnabled true when initial value in range`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f), initialValue = 50f)
        assertTrue(h.canConfirm)
    }

    @Test
    fun `isConfirmEnabled false when typed value below min`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f))
        h.onDigit("1")
        assertFalse(h.canConfirm)
    }

    @Test
    fun `isConfirmEnabled true when typed value in range`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f))
        h.onDigit("5")
        assertTrue(h.canConfirm)
    }

    @Test
    fun `isConfirmEnabled false when forceStepValues and value not on step`() = runTest {
        val h = handler(
            config = NumPadConfig(
                min = 0f, max = 10f,
                decimalPlaces = 1,
                forceStepValues = true,
                stepValue = 0.5f,
            )
        )
        h.onDigit("3")
        assertFalse(h.canConfirm)
    }

    @Test
    fun `isConfirmEnabled true when forceStepValues and value is on step`() = runTest {
        val h = handler(
            config = NumPadConfig(
                min = 0f, max = 10f,
                decimalPlaces = 1,
                forceStepValues = true,
                stepValue = 0.5f,
            )
        )
        h.onDigit("5")
        h.onDigit("0")
        assertTrue(h.canConfirm)
    }

    // -------------------------------------------------------------------------
    // Autocorrect range clamping
    // -------------------------------------------------------------------------

    @Test
    fun `autocorrect snaps value below min to min after delay`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f))
        h.onDigit("1")
        assertFalse(h.canConfirm)
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(5f, h.currentFloat())
        assertTrue(h.canConfirm)
    }

    @Test
    fun `autocorrect snaps value above max to max after delay`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 10f))
        h.onDigit("9")
        h.onDigit("9")
        assertFalse(h.canConfirm)
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(10f, h.currentFloat())
        assertTrue(h.canConfirm)
    }

    @Test
    fun `autocorrect triggers animation`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f))
        h.onDigit("1")
        advanceTimeBy(2001)
        assertTrue(h.isAnimating)
    }

    @Test
    fun `autocorrect animation clears after duration`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f))
        h.onDigit("1")
        advanceTimeBy(2001)
        assertTrue(h.isAnimating)
        advanceTimeBy(2200)
        assertFalse(h.isAnimating)
    }

    @Test
    fun `autocorrect skipped for Passcode display type`() = runTest {
        val h = handler(
            config = NumPadConfig(
                displayType = NumPadDisplay.Passcode,
                min = 1000f,
                max = 9999f,
            )
        )
        h.onDigit("5")
        advanceTimeBy(5000)
        assertFalse(h.isAnimating)
    }

    // -------------------------------------------------------------------------
    // Autocorrect step rounding
    // -------------------------------------------------------------------------

    @Test
    fun `autocorrect rounds value down to nearest step`() = runTest {
        val h = handler(
            config = NumPadConfig(
                min = 0f, max = 10f,
                decimalPlaces = 1,
                forceStepValues = true,
                stepValue = 0.5f,
            )
        )
        h.onDigit("3")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(0.5f, h.currentFloat(), 0.001f)
    }

    @Test
    fun `autocorrect rounds value up to nearest step when past midpoint`() = runTest {
        val h = handler(
            config = NumPadConfig(
                min = 0f, max = 10f,
                decimalPlaces = 1,
                forceStepValues = true,
                stepValue = 0.5f,
            )
        )
        h.onDigit("8")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(1.0f, h.currentFloat(), 0.001f)
    }

    // -------------------------------------------------------------------------
    // Inactivity timeout
    // -------------------------------------------------------------------------

    @Test
    fun `onDismiss called after inactivity timeout`() = runTest {
        var dismissed = false
        val h = handler(onDismiss = { dismissed = true })
        h.onDigit("5")
        advanceTimeBy(7001)
        assertTrue(dismissed)
    }

    @Test
    fun `inactivity timer resets on each digit input`() = runTest {
        var dismissed = false
        val h = handler(onDismiss = { dismissed = true })
        h.onDigit("1")
        advanceTimeBy(6000)
        h.onDigit("2")
        advanceTimeBy(6000)
        assertFalse(dismissed)
        advanceTimeBy(1001)
        assertTrue(dismissed)
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    @Test
    fun `reset restores handler to initial display`() = runTest {
        val h = handler(initialValue = 50f)
        h.onDigit("1")
        h.onDigit("2")
        h.reset(50f)
        assertEquals(50f, h.currentFloat())
    }

    @Test
    fun `reset with new value updates initial display`() = runTest {
        val h = handler(initialValue = 10f)
        h.onDigit("5")
        h.reset(99f)
        assertEquals(99f, h.currentFloat())
    }

    // -------------------------------------------------------------------------
    // Inactivity timer — fires on init, not just after first digit
    // -------------------------------------------------------------------------

    @Test
    fun `onDismiss called after inactivity timeout with no digit input`() = runTest {
        var dismissed = false
        @Suppress("UNUSED_VARIABLE")
        val h = handler(onDismiss = { dismissed = true })
        advanceTimeBy(7001)
        assertTrue(dismissed)
    }

    // -------------------------------------------------------------------------
    // displayZeroForNoInput
    // -------------------------------------------------------------------------

    @Test
    fun `displayZeroForNoInput false shows empty string after clearing all digits`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, displayZeroForNoInput = false))
        h.onDigit("5")
        h.onDelete()
        assertEquals("", h.display)
    }

    // -------------------------------------------------------------------------
    // Percentage bar
    // -------------------------------------------------------------------------

    @Test
    fun `showPercentageBar is true when usePercentageBar enabled`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, usePercentageBar = true))
        assertTrue(h.showPercentageBar)
    }

    @Test
    fun `showPercentageBar is false when usePercentageBar disabled`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, usePercentageBar = false))
        assertFalse(h.showPercentageBar)
    }

    @Test
    fun `percentageFraction reflects initial value when usePercentageBar enabled`() = runTest {
        val h = handler(
            config = NumPadConfig(min = 0f, max = 100f, usePercentageBar = true),
            initialValue = 50f,
        )
        assertEquals(0.5f, h.percentageFraction, 0.001f)
    }

    @Test
    fun `percentageFraction updates as digits are typed`() = runTest {
        val h = handler(
            config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0, usePercentageBar = true),
        )
        h.onDigit("7")
        h.onDigit("5")
        assertEquals(0.75f, h.percentageFraction, 0.001f)
    }

    @Test
    fun `percentageFraction is zero when max equals min (div-0 guard)`() = runTest {
        val h = handler(
            config = NumPadConfig(min = 10f, max = 10f, usePercentageBar = true),
            initialValue = 10f,
        )
        assertEquals(0f, h.percentageFraction, 0.001f)
    }

    @Test
    fun `percentageFraction stays zero when usePercentageBar is false`() = runTest {
        val h = handler(
            config = NumPadConfig(min = 0f, max = 100f, decimalPlaces = 0, usePercentageBar = false),
            initialValue = 50f,
        )
        assertEquals(0f, h.percentageFraction, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Unit label
    // -------------------------------------------------------------------------

    @Test
    fun `unitLabel in state reflects config unitLabel`() = runTest {
        val h = handler(config = NumPadConfig(min = 0f, max = 100f, unitLabel = "mph"))
        assertEquals("mph", h.unitLabel)
    }

    // -------------------------------------------------------------------------
    // Autocorrect cancellation
    // -------------------------------------------------------------------------

    @Test
    fun `autocorrect cancelled when new digit pressed before delay expires`() = runTest {
        val h = handler(config = NumPadConfig(min = 5f, max = 100f))
        h.onDigit("1")
        advanceTimeBy(1500)
        h.onDigit("0")
        advanceTimeBy(700)
        assertFalse("autocorrect should not have fired yet", h.isAnimating)
    }
}
