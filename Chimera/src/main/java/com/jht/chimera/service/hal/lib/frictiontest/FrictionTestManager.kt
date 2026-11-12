package com.jht.chimera.service.hal.lib.frictiontest

import java.util.Timer
import kotlin.concurrent.fixedRateTimer

// milliseconds between sending data
const val SEND_RATE = 125L

var targetSpeedKph = 0f
var targetIncline = 0f
var lcbSendTimer: Timer? = null

private var sendingData = false

fun startSendingFrictionTestData(sendData: (Float, Float)->Unit) {
    lcbSendTimer?.cancel()
    lcbSendTimer?.purge()
    sendingData = true

    lcbSendTimer = fixedRateTimer(period = SEND_RATE) {
        sendData(targetSpeedKph, targetIncline)
    }
}

fun stopFrictionTestData() {
    lcbSendTimer?.cancel()
    lcbSendTimer?.purge()
    lcbSendTimer = null

    targetSpeedKph = 0f
    targetIncline = 0f

    sendingData = false
}

fun isSendingData() : Boolean {
    return sendingData
}