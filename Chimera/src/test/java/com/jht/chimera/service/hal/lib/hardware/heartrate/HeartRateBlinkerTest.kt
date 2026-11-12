package com.jht.chimera.service.hal.lib.hardware.heartrate

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Exercises HeartRateBlinker with its real PeriodicTimer (50ms initial delay, 500ms period)
 * to verify the blink cadence, the 10-toggle limit, and the isKeep behavior.
 */
class HeartRateBlinkerTest {

    @Volatile
    private var heartRate = 0
    private val toggles = CopyOnWriteArrayList<Boolean>()
    private val finishCount = AtomicInteger(0)
    private lateinit var blinker: HeartRateBlinker

    @Before
    fun setup() {
        heartRate = 0
        toggles.clear()
        finishCount.set(0)
        blinker = HeartRateBlinker(
            heartRateSupplier = { heartRate },
            onBlinkToggle = { toggles.add(it) },
            onBlinkFinish = { finishCount.incrementAndGet() },
        )
    }

    @After
    fun tearDown() {
        blinker.stopBlink(false)
    }

    private fun awaitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun `blink toggles blank state on then off repeatedly`() {
        assertFalse(blinker.isBlinking)
        blinker.startBlink(false)
        assertTrue(blinker.isBlinking)
        assertTrue("expected at least 4 toggles", awaitUntil(6000) { toggles.size >= 4 })
        // first tick blanks the display, second restores it, and so on
        assertEquals(listOf(true, false, true, false), toggles.take(4))
    }

    @Test
    fun `blink without keep finishes after ten toggles`() {
        blinker.startBlink(false)
        assertTrue("blink never finished", awaitUntil(12000) { finishCount.get() > 0 })
        assertEquals(10, toggles.size)
        assertTrue(blinker.isBlinkFinished)
        // final callback restores the display (last toggle is off)
        assertFalse(toggles.last())
    }

    @Test
    fun `blink with keep continues past ten toggles while heart rate is zero`() {
        blinker.startBlink(true)
        assertTrue("expected blinking to continue", awaitUntil(12000) { toggles.size >= 12 })
        assertEquals(0, finishCount.get())
        assertFalse(blinker.isBlinkFinished)
    }

    @Test
    fun `blink with keep finishes once a heart rate arrives`() {
        blinker.startBlink(true)
        assertTrue(awaitUntil(12000) { toggles.size >= 10 })
        heartRate = 70
        assertTrue("blink should stop after heart rate arrives", awaitUntil(3000) { finishCount.get() > 0 })
        assertTrue(blinker.isBlinkFinished)
    }

    @Test
    fun `startBlink while already blinking does not start a second timer`() {
        val timerField = HeartRateBlinker::class.java.getDeclaredField("blinkTimer").apply { isAccessible = true }
        blinker.startBlink(false)
        val firstTimer = timerField.get(blinker)
        blinker.startBlink(false)
        assertSame(firstTimer, timerField.get(blinker))
    }

    @Test
    fun `stopBlink without finish notifies but does not mark finished`() {
        blinker.startBlink(false)
        assertTrue(awaitUntil(2000) { toggles.isNotEmpty() })
        blinker.stopBlink(false)
        assertFalse(blinker.isBlinking)
        assertEquals(1, finishCount.get())
        assertFalse(blinker.isBlinkFinished)
    }
}
