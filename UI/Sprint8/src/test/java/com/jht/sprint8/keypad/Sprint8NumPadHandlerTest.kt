package com.jht.sprint8.keypad

import com.jht.sprint8.KeypadType
import com.jht.widgets.numpad.NumPadConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Sprint8NumPadHandlerTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun TestScope.speedHandler(
        min: Float = 0.5f,
        max: Float = 15f,
        initialValue: Float = 5f,
        onDismiss: () -> Unit = {},
    ) = Sprint8NumPadHandler(
        scope = this,
        config = NumPadConfig(
            decimalPlaces = 1,
            min = min,
            max = max,
            stepValue = 0.5f,
            forceStepValues = true,
        ),
        initialValue = initialValue,
        keypadType = KeypadType.SPEED,
        onDismiss = onDismiss,
    )

    private fun TestScope.inclineHandler(
        min: Float = 0f,
        max: Float = 15f,
        initialValue: Float = 0f,
        onDismiss: () -> Unit = {},
    ) = Sprint8NumPadHandler(
        scope = this,
        config = NumPadConfig(
            decimalPlaces = 1,
            min = min,
            max = max,
            stepValue = 0.5f,
            forceStepValues = true,
        ),
        initialValue = initialValue,
        keypadType = KeypadType.INCLINE,
        onDismiss = onDismiss,
    )

    private fun TestScope.resistanceHandler(
        min: Float = 1f,
        max: Float = 20f,
        initialValue: Float = 5f,
        onDismiss: () -> Unit = {},
    ) = Sprint8NumPadHandler(
        scope = this,
        config = NumPadConfig(
            decimalPlaces = 0,
            min = min,
            max = max,
            stepValue = 1f,
            forceStepValues = true,
        ),
        initialValue = initialValue,
        keypadType = KeypadType.RESISTANCE,
        onDismiss = onDismiss,
    )

    private val Sprint8NumPadHandler.display get() = state.value.displayValue
    private val Sprint8NumPadHandler.canConfirm get() = state.value.isConfirmEnabled

    // -------------------------------------------------------------------------
    // Speed (decimal formatting, 1 decimal place)
    // -------------------------------------------------------------------------

    @Test
    fun `speed onDigit produces 1-decimal display`() = runTest {
        val h = speedHandler()
        h.onDigit("5")
        h.onDigit("5")
        assertEquals("5.5", h.display)
    }

    @Test
    fun `speed single digit displays as decimal`() = runTest {
        val h = speedHandler()
        h.onDigit("5")
        assertEquals("0.5", h.display)
    }

    @Test
    fun `speed value on step is confirmable`() = runTest {
        val h = speedHandler()
        h.onDigit("5")
        h.onDigit("5")
        assertTrue(h.canConfirm)
    }

    @Test
    fun `speed value not on step is not confirmable`() = runTest {
        val h = speedHandler()
        h.onDigit("5")
        h.onDigit("3")
        assertFalse(h.canConfirm)
    }

    // -------------------------------------------------------------------------
    // Incline (decimal formatting, 1 decimal place, 0.5 rounding)
    // -------------------------------------------------------------------------

    @Test
    fun `incline onDigit produces 1-decimal display`() = runTest {
        val h = inclineHandler()
        h.onDigit("1")
        h.onDigit("0")
        assertEquals("1.0", h.display)
    }

    @Test
    fun `incline autocorrect rounds down to nearest 0_5 step`() = runTest {
        val h = inclineHandler()
        h.onDigit("1")
        h.onDigit("2")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(1.0f, h.currentFloat(), 0.001f)
    }

    @Test
    fun `incline autocorrect rounds up to nearest 0_5 step past midpoint`() = runTest {
        val h = inclineHandler()
        h.onDigit("1")
        h.onDigit("8")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(2.0f, h.currentFloat(), 0.001f)
    }

    // -------------------------------------------------------------------------
    // Resistance (integer formatting, 0 decimal places)
    // -------------------------------------------------------------------------

    @Test
    fun `resistance onDigit produces integer display`() = runTest {
        val h = resistanceHandler()
        h.onDigit("1")
        h.onDigit("2")
        assertEquals("12", h.display)
    }

    @Test
    fun `resistance in-range integer is confirmable`() = runTest {
        val h = resistanceHandler()
        h.onDigit("1")
        h.onDigit("0")
        assertTrue(h.canConfirm)
    }

    @Test
    fun `resistance above max not confirmable`() = runTest {
        val h = resistanceHandler()
        h.onDigit("2")
        h.onDigit("5")
        assertFalse(h.canConfirm)
    }

    @Test
    fun `resistance autocorrect clamps above max`() = runTest {
        val h = resistanceHandler()
        h.onDigit("2")
        h.onDigit("5")
        advanceTimeBy(2001)
        advanceTimeBy(2200)
        assertEquals(20f, h.currentFloat())
        assertTrue(h.canConfirm)
    }

    // -------------------------------------------------------------------------
    // KeypadType identity
    // -------------------------------------------------------------------------

    @Test
    fun `keypadType is correctly set for SPEED`() = runTest {
        val h = speedHandler()
        assertEquals(KeypadType.SPEED, h.keypadType)
    }

    @Test
    fun `keypadType is correctly set for INCLINE`() = runTest {
        val h = inclineHandler()
        assertEquals(KeypadType.INCLINE, h.keypadType)
    }

    @Test
    fun `keypadType is correctly set for RESISTANCE`() = runTest {
        val h = resistanceHandler()
        assertEquals(KeypadType.RESISTANCE, h.keypadType)
    }
}
