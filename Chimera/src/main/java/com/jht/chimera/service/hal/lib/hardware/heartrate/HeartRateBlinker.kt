package com.jht.chimera.service.hal.lib.hardware.heartrate

import com.jht.androidcommonalgorithms.timer.PeriodicTimer

class HeartRateBlinker(
    private val heartRateSupplier: () -> Int,
    private val onBlinkToggle: (Boolean) -> Unit,
    private val onBlinkFinish: () -> Unit,
) {
    private var blinkTimer: PeriodicTimer? = null
    private var blinkCounter = 0
    private var toggleCounter = 0
    var isBlinkFinished: Boolean = false

    val isBlinking: Boolean
        @Synchronized get() = blinkTimer != null

    @Synchronized
    fun startBlink(isKeep: Boolean) {
        if (blinkTimer == null) {
            this.isBlinkFinished = false
            blinkTimer = PeriodicTimer(50, 500) {
                this.isBlinkFinished = false
                blinkCounter++
                toggleCounter++
                if (toggleCounter % 2 == 0) {
                    toggleCounter = 0
                    onBlinkToggle(false)
                } else {
                    onBlinkToggle(true)
                }
                if (blinkCounter >= 10) {
                    if (!isKeep) {
                        stopBlink(true)
                    } else if (heartRateSupplier() > 0) {
                        stopBlink(true)
                    }
                }
            }
            blinkTimer?.start("HeartRateBlink")
        }
    }

    @Synchronized
    fun stopBlink(finish: Boolean) {
        blinkTimer?.let {
            it.stop()
            blinkTimer = null
        }
        blinkCounter = 0
        toggleCounter = 0
        if (finish) {
            this.isBlinkFinished = true
        }
        onBlinkFinish()
    }
}
