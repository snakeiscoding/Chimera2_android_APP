package com.jht.chimera.service.workout

import com.jht.chimera.lib.common.DebouncedSetter
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.math.abs

/* CGC-19069
    The onyx treadmill LCB does not handle rapidly flipping low/high speed requests. Ideally LCB would know frame limits and accommodate as it could.
    New HIIT creates a scenario were someone could (if they wanted) spam these changes between the interval cards and quick keys, as there is no speed jump confirmation.
    There may be UI mitigations we could do, but we do not always control these requests everywhere, for example integrations. So we are attempting to filter them before LCB.
    The 250 ms debounce to last request value seems to work on our onyx treadmill, but our sample size was low. We believe this latency small enough not to be impactful.
 */


/** Provide debounce for frame control requests from [WorkoutControllerService] */
class FrameControlDebounce : AutoCloseable {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lastSpeed = 0f
    private val speedDebounce = DebouncedSetter(
        scope = scope,
        delayMs = 250,
        commit = ::commitSpeed
    )
    private fun commitSpeed(speedKph: Float) {
        lastSpeed = speedKph
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, speedKph)
    }
    fun updateSpeedKph(speedKph: Float) {
        // the update latency is noticeable with increment and decrement buttons, so maybe skip to it for small changes
        if (abs(speedKph - lastSpeed) > 2) {
            speedDebounce.set(speedKph)
        } else {
            commitSpeed(speedKph)
        }
    }

    private var lastIncline = 0f
    private val inclineDebounce = DebouncedSetter(
        scope = scope,
        delayMs = 250,
        commit = ::commitIncline
    )
    private fun commitIncline(incline: Float) {
        lastIncline = incline
        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, incline)
    }
    fun updateIncline(incline: Float) {
        // the update latency is noticeable with increment and decrement buttons, so maybe skip to it for small changes
        if (abs(incline - lastIncline) > 1) {
            inclineDebounce.set(incline)
        } else {
            commitIncline(incline)
        }
    }

    fun cancel() {
        speedDebounce.cancel()
        inclineDebounce.cancel()
    }

    override fun close() {
        scope.cancel()
    }
}
