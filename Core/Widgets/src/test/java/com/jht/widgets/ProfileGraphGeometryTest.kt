package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logic tests for the profile graph's time-axis geometry — most importantly the invariant that
 * the computed width can never exceed Compose's Constraints encoding limit (2^18-1 = 262,143 px),
 * which crashed with IllegalArgumentException at ~14.5h elapsed before the window+clamp existed.
 */
class ProfileGraphGeometryTest {

    private val window120MinMs = 120 * 60 * 1000 // the default config window
    private val cushion = 12f // half a 24px indicator

    // --- the crash limit ---

    @Test
    fun `width stays under the Compose Constraints limit for any elapsed time`() {
        val elapsedCases = listOf(
            0f,
            30f * 60 * 1000,                  // 30 min
            52_400_000f,                      // ~14.5h — the observed crash point pre-fix
            100f * 60 * 60 * 1000,            // 100 h
            Int.MAX_VALUE.toFloat(),
            Float.MAX_VALUE,                  // pathological
        )
        val windowCases = listOf(15, 30, 120, 100_000) // minutes; incl. an absurd config value
        for (elapsed in elapsedCases) {
            for (windowMin in windowCases) {
                val width = ProfileGraphGeometry.plotWidthPx(elapsed, windowMin * 60_000, cushion)
                assertTrue(
                    "width $width for elapsed=$elapsed window=${windowMin}min must be in 0..262000",
                    width in 0..ProfileGraphGeometry.MAX_GRAPH_WIDTH_PX,
                )
                assertTrue("must stay under Compose's 262143 limit", width < 262_143)
            }
        }
    }

    @Test
    fun `width handles pathological inputs without throwing`() {
        assertEquals(0, ProfileGraphGeometry.plotWidthPx(Float.NaN, window120MinMs, cushion))
        // negative elapsed never yields a negative width
        assertEquals(0, ProfileGraphGeometry.plotWidthPx(-5_000f, window120MinMs, cushion))
        assertEquals(0, ProfileGraphGeometry.plotWidthPx(-99_999f, window120MinMs, cushion))
    }

    @Test
    fun `width grows with elapsed time until the window, then stays fixed`() {
        val w30min = ProfileGraphGeometry.plotWidthPx(30f * 60_000, window120MinMs, cushion)
        val w120min = ProfileGraphGeometry.plotWidthPx(120f * 60_000, window120MinMs, cushion)
        val w300min = ProfileGraphGeometry.plotWidthPx(300f * 60_000, window120MinMs, cushion)
        assertTrue(w30min < w120min)
        assertEquals(w120min, w300min) // window slides; width is constant past the window
        // 120min * 0.005 px/ms = 36,000 px + cushion
        assertEquals(36_012, w120min)
    }

    // --- sliding window mapping ---

    @Test
    fun `window start is zero until the window fills, then slides`() {
        assertEquals(0f, ProfileGraphGeometry.windowStartMs(60_000f, window120MinMs))
        val elapsed = 130f * 60_000 // 130 min
        assertEquals(10f * 60_000, ProfileGraphGeometry.windowStartMs(elapsed, window120MinMs))
    }

    @Test
    fun `latest point maps to the window end and pre-window points map off-screen`() {
        val elapsed = 130f * 60_000
        // the newest sample sits at windowMs * scale
        assertEquals(
            window120MinMs * ProfileGraphGeometry.PX_PER_TIME_MS,
            ProfileGraphGeometry.xForTimeMs(elapsed, elapsed, window120MinMs),
        )
        // a sample older than the window maps to negative x (off-screen left)
        val old = 5f * 60_000
        assertTrue(ProfileGraphGeometry.xForTimeMs(old, elapsed, window120MinMs) < 0f)
        // before the window slides, t=0 maps to x=0 (origin preserved for short workouts)
        assertEquals(0f, ProfileGraphGeometry.xForTimeMs(0f, 60_000f, window120MinMs))
    }

    // --- markers ---

    @Test
    fun `markers are per minute, never at zero, and only inside the window`() {
        // short workout: 3.5 minutes -> markers at 1,2,3 min
        assertEquals(
            listOf(60_000, 120_000, 180_000),
            ProfileGraphGeometry.minuteMarkerTimesMs(210_000, window120MinMs),
        )
        // no markers before the first full minute, none at zero
        assertEquals(emptyList<Int>(), ProfileGraphGeometry.minuteMarkerTimesMs(30_000, window120MinMs))
        assertEquals(emptyList<Int>(), ProfileGraphGeometry.minuteMarkerTimesMs(0, window120MinMs))
    }

    @Test
    fun `markers outside the sliding window are dropped`() {
        val windowMs = 10 * 60_000 // 10 min window
        val elapsed = 25 * 60_000  // 25 min elapsed -> window covers minutes 15..25
        val markers = ProfileGraphGeometry.minuteMarkerTimesMs(elapsed, windowMs)
        assertEquals(16 * 60_000, markers.first()) // first step boundary after window start
        assertEquals(25 * 60_000, markers.last())
        assertTrue(markers.all { it >= elapsed - windowMs })
    }

    @Test
    fun `marker count stays bounded for very long workouts`() {
        // pre-fix this generated one marker per elapsed minute (unbounded); now window-bounded
        val markers = ProfileGraphGeometry.minuteMarkerTimesMs(
            15 * 60 * 60 * 1000, // 15 hours
            window120MinMs,
        )
        assertEquals(120, markers.size)
    }
}
