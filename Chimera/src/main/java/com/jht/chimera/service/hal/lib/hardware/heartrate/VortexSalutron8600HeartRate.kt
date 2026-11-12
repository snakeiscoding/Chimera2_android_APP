package com.jht.chimera.service.hal.lib.hardware.heartrate

import android.util.Log
import com.jht.androidcommonalgorithms.timer.PeriodicTimer
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.service.hal.lib.hardware.heartrate.IHeartRateReader.IHeartRateUpdated
import com.jht.chimera.service.hal.lib.salutron.IHeartRateCallback
import com.jht.chimera.service.hal.lib.salutron.SalutronDevice
import com.jht.enums.HeartRateSource
import java.io.IOException
import java.io.RandomAccessFile

private const val TAG = "VortexSalutron8600HeartRate"
private const val HANDGRIP_STATUS_PATH = "/sys/kernel/debug/gpio"
private const val HEART_RATE_PATH = "/sys/heartgpio/heartrate"


internal class VortexSalutron8600HeartRate(
    private var heartRateUpdated: IHeartRateUpdated
) : IHeartRateReader {
    // these booleans are for checking if module is present by seeing if gpio has transitioned
    // gpio is low when gripped BUT also low when there is no module installed (unless maybe we can configure gpio to default hi)
    // however if module was present and grips have not be constantly gripped since startup, we should see high at some point
    private var hasGripStatusBeenFalse = false
    private var hasGripStatusBeenTrue = false
    private fun wasModuleDetected() = hasGripStatusBeenTrue && hasGripStatusBeenFalse

    private val blinker = HeartRateBlinker(
        heartRateSupplier = { heartRate },
        onBlinkToggle = {
            blankState = it
            heartRateUpdated.heartRateUpdated()
        },
        onBlinkFinish = {
            blankState = false
            heartRateUpdated.heartRateUpdated()
        },
    )
    private val heartRateCallback: IHeartRateCallback = object : IHeartRateCallback {
        override fun onHeartRate(value: Int, type: Int) {
            if (value == 0) {
                blinker.isBlinkFinished = false
            }
        }
        override fun onScanning(isScanning: Boolean) {}
        override fun onWorkout(isWorkout: Boolean) {}
        override fun isConnected(isConnected: Boolean, connectDevice: SalutronDevice.SalutronDeviceParcelable?) {}
        override fun version(version: String?) {}
        override fun blink(isKeep: Boolean) {
            blinker.startBlink(isKeep)
        }
        override fun stopBlink() {
            blinker.stopBlink(false)
        }
    }

    @Volatile
    private var heartRate = 0
    @Volatile
    private var blankState = false
    @Volatile
    private var isGripped = false

    private var heartRateFile: RandomAccessFile? = null
    private var gripStatusFile: RandomAccessFile? = null

    private fun readHeartRate(): Int {
        return try {
            heartRateFile?.apply { seek(0) }?.readLine()?.toInt() ?: 0
        } catch (_: Exception) { 0 }
    }

    private fun readGripStatus(): Boolean {
        return try {
            gripStatusFile?.apply { seek(0) }
            var line: String?
            var isLo = false
            while (gripStatusFile?.readLine().also { line = it } != null) {
                if (line?.contains("heart-measure") == true) {
                    isLo = line.contains("lo") == true
                    break
                }
            }
            isLo
        } catch (e: Exception) {
            Log.e(TAG, "Read Debug GPIO Error", e)
            false
        }
    }

    private val hrTimer = PeriodicTimer(1000, 1000) {
        val gripStatus = readGripStatus()
        if (gripStatus) hasGripStatusBeenTrue = true
        else hasGripStatusBeenFalse = true
        isGripped = gripStatus && wasModuleDetected()
        heartRate = readHeartRate()
        if (heartRate > 0) {
            heartRateCallback.stopBlink()
            heartRateUpdated.heartRateUpdated()
        } else {
            if (isGripped) {
                heartRateCallback.blink(true)
            } else {
                heartRate = 0
                heartRateCallback.stopBlink()
                heartRateCallback.onHeartRate(0, 0)
                heartRateUpdated.heartRateUpdated()
            }
        }
    }

    init {
        listOf(HEART_RATE_PATH, HANDGRIP_STATUS_PATH).forEach { path ->
            ExecuteAsRoot.chmod("666", path)
        }
        try {
            heartRateFile = RandomAccessFile(HEART_RATE_PATH, "r")
            gripStatusFile = RandomAccessFile(HANDGRIP_STATUS_PATH, "r")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening files", e)
        }
        if (heartRateFile != null) hrTimer.start()
    }

    override fun close() {
        hrTimer.stop()
        heartRateCallback.stopBlink()
        try {
            heartRateFile?.close()
            gripStatusFile?.close()
        } catch (_: IOException) {}
    }

    override fun heartRate() = heartRate
    override fun blankState() = blankState
    override fun getSource() = HeartRateSource.Grips
    override fun hasConnection() = isGripped || heartRate > 0
}