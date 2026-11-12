package com.jht.devicepairing

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.jht.enums.DeviceState
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.PairingCallbacks
import com.jht.interfaces.models.PairingDevice
import com.jht.interfaces.models.PairingDeviceHandler
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "DevicePairingViewModel"

const val TestTagAudioOut = "btPairAudioOut"
const val TestTagHearRate = "btPairHr"
const val TestTagFtms = "btPairFtms"

@HiltViewModel
class DevicePairingViewModel @Inject constructor(
    val systemModel: SystemModel,
    val uiBaseModel: UIBaseModel,
    val uiServiceModel: UIServiceModel,
    val deviceModel: DeviceModel,
    val workoutModel: WorkoutModel,
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val safeRegionPx = mutableStateOf(android.graphics.Rect()) // interface gives old rect in px, composable can convert to new rect Dp
    val machineName = systemModel.serialNumberConsole
    val categories = mutableStateListOf<DeviceCategoryHandler>()
    val selectedCategory = mutableStateOf<DeviceCategoryHandler?>(null)
    val pairingTimedOut = mutableStateOf(false)
    private fun onTimeout() {
        pairingTimedOut.value = true
    }

    override fun start() {
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        uiServiceModel.overlayViewportPixels.listenAndTrigger {
            safeRegionPx.value = it
        }
        categories.clear()
        deviceModel.createPairingHandlerAudioOut()?.let {
            categories.add(DeviceCategoryHandler(R.drawable.ic_headphones_enabled, R.string.speakers_headphones_tc, TestTagAudioOut, it, DeviceCategory.AudioOut, ::onTimeout))
        }
        deviceModel.createPairingHandlerHeartRate()?.let {
            categories.add(DeviceCategoryHandler(R.drawable.ic_heart_rate_monitor_enabled, R.string.heart_rate_monitors_tc, TestTagHearRate, it, DeviceCategory.HeartRate, ::onTimeout))
        }
        deviceModel.createPairingHandlerFtms()?.let {
            categories.add(DeviceCategoryHandler(R.drawable.ic_ftms, R.string.ftms_tc, TestTagFtms, it, DeviceCategory.Ftms, ::onTimeout))
        }
        // auto select first
        if (categories.isNotEmpty()) onSelect(categories.firstOrNull())
    }

    override fun stop() {
        onSelect(null) // deselect category to hide clean up during the pause
        categories.forEach { it.close() } // since start will recreate categories, do a full close
        systemModel.close()
        uiServiceModel.close()
        uiBaseModel.close()
        closeParent() // just close when user leaves, this is meant as a form of dialog, best not to have multiples floating around
    }

    fun onSelect(category: DeviceCategoryHandler?) {
        // hide clean up from user by deselecting the previous category before stopping it
        val previousCategory = selectedCategory.value
        selectedCategory.value = category
        previousCategory?.stop()
        category?.start()
    }

    fun onBack() {
        closeParent()
    }
}


enum class DeviceCategory {
    AudioOut,
    HeartRate,
    Ftms,
}

/** Wrapping of a device category for UI */
class DeviceCategoryHandler(
    val iconId: Int,
    val labelId: Int,
    val testTag: String,
    val handler: PairingDeviceHandler? = null, // allowing null for previews
    val type: DeviceCategory = DeviceCategory.HeartRate, // kind of cheesy, but works for now
    val onTimeout: ()->Unit = {},
) : PairingCallbacks, AutoCloseable {
    val devices = mutableStateListOf<DeviceHandler>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var pairingTimer: Job? = null
    private var pairingDevice: PairingDevice? = null

    init {
        handler?.callbacks = this
    }
    override fun close() {
        stop()
        scope.cancel()
        handler?.close()
    }

    private fun startPairTimer(device: PairingDevice) {
        pairingTimer?.cancel()
        pairingTimer = scope.launch {
            delay(30000)
            handler?.abortConnect(device)
            onTimeout()
        }
    }
    private fun stopPairTimer() {
        pairingTimer?.cancel()
    }

    override fun onPinCode(device: PairingDevice, code: String) {
        // TODO - pin dialog
        // we display the PIN and if user rejects we cancel connection
        Log.w(TAG, "PIN dialog not implemented, onPinCode ${device.deviceName} ${device.deviceId} $code")
    }
    override fun onDeviceUpdated(device: PairingDevice) {
        devices.find { it.id == device.deviceId }?.let {
            // if device was connecting but now disconnected, trigger timeout event to alert user there was an issue
            if (it.connectionState.value == DeviceState.Connecting && device.connectionState == DeviceState.Disconnected) {
                stopPairTimer()
                pairingDevice = null
                onTimeout()
            }
            it.connectionState.value = device.connectionState // update UI device state with current device state
            if (device.connectionState == DeviceState.Connecting) { // device is now connecting, start connecting timer
                startPairTimer(device)
                pairingDevice = device
            } else if (device == pairingDevice) { // device was connecting but not anymore, stop connecting timer
                stopPairTimer()
                pairingDevice = null
            }
        }
    }
    override fun onListUpdated() {
        devices.clear()
        handler?.devices?.forEach {
            val myZone = (type == DeviceCategory.HeartRate && it.deviceName.contains("myzone-", true))
            val image = if (myZone) R.drawable.logo_myzone else 0
            devices.add(DeviceHandler(it, it.deviceName, it.deviceId, image))
        }
    }

    fun start() {
        handler?.start()
    }
    fun stop() {
        handler?.stop()
        stopPairTimer() // stop timer after handler to ensure handler does not start it again
    }

    fun onClick(deviceHandler: DeviceHandler) {
        when (deviceHandler.connectionState.value) {
            DeviceState.Connected-> handler?.disconnect(deviceHandler.device)
            DeviceState.Connecting-> handler?.abortConnect(deviceHandler.device)
            else-> handler?.connect(deviceHandler.device)
        }
    }
}

/** Wrapping of a device for UI */
class DeviceHandler(
    val device: PairingDevice,
    val label: String = "",
    val id: String = "",
    val iconId: Int = 0,
) {
    val connectionState = mutableStateOf(device.connectionState)
}
