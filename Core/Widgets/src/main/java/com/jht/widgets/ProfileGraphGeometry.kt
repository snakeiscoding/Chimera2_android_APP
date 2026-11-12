package com.jht.widgets

import kotlin.math.roundToInt

/**
 * Pure geometry for the profile graph's open-ended (no-goal) time axis,
 * extracted from ProfileGraphComponent so it is unit-testable.
 *
 * The critical invariant enforced here: the computed graph width can NEVER exceed
 * [MAX_GRAPH_WIDTH_PX], which stays under Compose's Constraints encoding limit of 2^18-1
 * (262,143 px). Exceeding it throws IllegalArgumentException("Can't represent a width of ...")
 * during layout — an unconditional crash observed at ~14.5h of elapsed workout time before
 * the sliding window + clamp were added.
 */
internal object ProfileGraphGeometry {

    /** Horizontal scale for the open-ended time axis. */
    const val PX_PER_TIME_MS = 5f / 1000

    /** Hard cap under Compose's 262,143 px Constraints limit. */
    const val MAX_GRAPH_WIDTH_PX = 262_000

    /** Start of the sliding window (ms) for a given elapsed time. Never negative. */
    fun windowStartMs(elapsedMs: Float, windowMs: Int): Float =
        maxOf(0f, elapsedMs - windowMs)

    /** Maps an absolute time (ms) into window-relative pixels. */
    fun xForTimeMs(timeMs: Float, elapsedMs: Float, windowMs: Int): Float =
        (timeMs - windowStartMs(elapsedMs, windowMs)) * PX_PER_TIME_MS

    /**
     * The plot width in pixels for an open-ended workout: the plotted span is capped to the
     * sliding window, and the final value is clamped into 0..[MAX_GRAPH_WIDTH_PX] as a safety
     * net for any input (including pathological elapsed values).
     */
    fun plotWidthPx(elapsedMs: Float, windowMs: Int, indicatorCushionPx: Float): Int {
        val plottedMs = minOf(elapsedMs, windowMs.toFloat())
        val raw = plottedMs * PX_PER_TIME_MS + indicatorCushionPx
        if (raw.isNaN()) return 0
        return raw.roundToInt().coerceIn(0, MAX_GRAPH_WIDTH_PX)
    }

    /**
     * Times (ms) of the per-minute x markers that fall inside the sliding window.
     * Never includes a marker at zero; the first marker is the first step boundary at or
     * after the window start.
     */
    fun minuteMarkerTimesMs(elapsedMs: Int, windowMs: Int, stepMs: Int = 60_000): List<Int> {
        if (stepMs <= 0 || elapsedMs <= 0) return emptyList()
        val windowStart = maxOf(0, elapsedMs - windowMs)
        val first = maxOf(stepMs, ((windowStart / stepMs) + 1) * stepMs)
        return (first..elapsedMs step stepMs).toList()
    }
}
