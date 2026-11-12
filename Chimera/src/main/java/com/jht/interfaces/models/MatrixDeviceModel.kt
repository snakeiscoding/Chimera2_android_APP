package com.jht.interfaces.models

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys
import com.jht.chimera.ui.bluetooth.FTMSDeviceScanner
import com.jht.chimera.ui.bluetooth.HeadphoneDevice
import com.jht.chimera.ui.bluetooth.HeadphoneScanner
import com.jht.chimera.ui.bluetooth.HeartRateDevice
import com.jht.chimera.ui.bluetooth.HeartRateScanner
import com.jht.chimera.ui.bluetooth.IBluetoothConnectionResult
import com.jht.chimera.ui.bluetooth.IBluetoothDevice
import com.jht.chimera.ui.bluetooth.IBluetoothScanner
import com.jht.chimera.ui.bluetooth.MediaBluetoothDevice
import com.jht.chimera.ui.bluetooth.MediaDeviceScanner
import com.jht.chimera.ui.bluetooth.SalutronHeartRateDevice
import com.jht.chimera.ui.bluetooth.SalutronHeartRateScanner
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.HeartRateSource
import com.jht.enums.RelativeLocation
import com.jht.enums.WifiState
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener

private const val TAG = "MatrixDeviceModel"

class MatrixDeviceModel : DeviceModel() {
    private val manager: CallbackManager = CallbackManager()

    override fun close() {
        manager.cancelAllListeners()
    }

    companion object {
        fun isAppleWatchSupported() = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL) && ProductConfig.showGymkitUI()
        fun isSamsungWatchSupported() = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)
    }

    override val rfidReaderLocation: RelativeLocation
        get() = ProductConfig.getConsoleType().rfidLocation

    override fun getDeviceState(device: Device): DataValueListener<DeviceState> {
        return when (device) {
            Device.AppleWatch -> MatrixIntDeviceStateListener(
                manager,
                HALKeys.DATA_GYM_KIT_STATUS_INT,
                isAppleWatchSupported()
            )

            Device.SamsungWatch -> MatrixIntDeviceStateListener(
                manager,
                HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT,
                isSamsungWatchSupported()
            )

            Device.Headphones -> MatrixBoolDeviceStateListener(
                manager,
                TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL
            )

            Device.BtSink -> MatrixBoolDeviceStateListener(
                manager,
                HALKeys.DATA_BT_SINK_CONNECTED_BOOL,
                BuildConfig.HAS_BT_SINK && BuildConfig.MEDIA_PLAYER_SCANNER
            )

            else -> MatrixIntDeviceStateListener(manager, "", false)
        }
    }

    override val heartRateSource: DataValueListener<HeartRateSource> = MatrixHeartRateSourceListener(manager, HALKeys.DATA_HEART_RATE_SOURCE_INT)

    override fun createPairingHandlerAudioOut(): PairingDeviceHandler? {
        val context = ContextUtil.getApplicationContext() ?: return null
        return MatrixPairingHandler(context, HeadphoneScanner(context))
    }
    override fun createPairingHandlerHeartRate(): PairingDeviceHandler? {
        val context = ContextUtil.getApplicationContext() ?: return null
        val hrScanner = if (ProductConfig.hasSalutron()) SalutronHeartRateScanner(context) else HeartRateScanner(context)
        return MatrixPairingHandler(context, hrScanner)
    }
    override fun createPairingHandlerFtms(): PairingDeviceHandler? {
        val supported = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_FTMS_ADVERTISING_ENABLED_BOOL)
        if (!supported) return null
        val context = ContextUtil.getApplicationContext() ?: return null
        return MatrixPairingHandler(context, FTMSDeviceScanner())
    }

    override val wifiState: DataValueListener<WifiState> = object :
        DataValueListener<WifiState> {
        private val key = TopBarKeys.DATA_WIFI_STRENGTH_ENUM_INT
        override val value: WifiState
            get() = WifiState.fromInt(DataMatrix.getInt(key))

        override fun listen(onChange: (WifiState) -> Unit) {
            manager.addIntListener(key, { onChange(WifiState.fromInt(it)) }, false)
        }

        override fun listenAndTrigger(onChange: (WifiState) -> Unit) {
            manager.addIntListener(key, { onChange(WifiState.fromInt(it)) }, true)
        }
    }

    class MatrixIntDeviceStateListener(private val manager: CallbackManager, private val key: Any, private val isSupported: Boolean = true) :
        DataValueListener<DeviceState> {
        override val value: DeviceState
            get() = if (isSupported) DeviceState.fromInt(DataMatrix.getInt(key)) else DeviceState.NotSupported

        override fun listen(onChange: (DeviceState) -> Unit) {
            if (isSupported) manager.addIntListener(key, { onChange(DeviceState.entries[it]) }, false)
        }

        override fun listenAndTrigger(onChange: (DeviceState) -> Unit) {
            if (isSupported) manager.addIntListener(key, { onChange(DeviceState.entries[it]) }, true)
            else onChange(DeviceState.NotSupported)
        }
    }


    class MatrixBoolDeviceStateListener(private val manager: CallbackManager, private val key: Any, private val isSupported: Boolean = true) : DataValueListener<DeviceState> {
        override val value: DeviceState
            get() = if (isSupported) DeviceState.fromBool(DataMatrix.getBoolean(key)) else DeviceState.NotSupported

        override fun listen(onChange: (DeviceState) -> Unit) {
            if (isSupported) manager.addBoolListener(key, { onChange(DeviceState.fromBool(it)) }, false)
        }

        override fun listenAndTrigger(onChange: (DeviceState) -> Unit) {
            if (isSupported) manager.addBoolListener(key, { onChange(DeviceState.fromBool(it)) }, true)
            else onChange(DeviceState.NotSupported)
        }
    }

    class MatrixStringDeviceStateListener(private val manager: CallbackManager, private val key: Any) : DataValueListener<DeviceState> {
        override val value: DeviceState
            get() = DeviceState.fromString(DataMatrix.getString(key))

        override fun listen(onChange: (DeviceState) -> Unit) {
            manager.addStringListener(key, { onChange(DeviceState.fromString(it)) }, false)
        }

        override fun listenAndTrigger(onChange: (DeviceState) -> Unit) {
            manager.addStringListener(key, { onChange(DeviceState.fromString(it)) }, true)
        }
    }

    class MatrixHeartRateSourceListener(private val manager: CallbackManager, private val key: Any) :
        DataValueListener<HeartRateSource> {
        override val value: HeartRateSource
            get() = HeartRateSource.fromInt(DataMatrix.getInt(key))

        override fun listen(onChange: (HeartRateSource) -> Unit) {
            manager.addIntListener(key, { onChange(HeartRateSource.values()[it]) }, false)
        }

        override fun listenAndTrigger(onChange: (HeartRateSource) -> Unit) {
            manager.addIntListener(key, { onChange(HeartRateSource.values()[it]) }, true)
        }
    }
}


//region Device Pairing
class MatrixPairDevice(val matrixDevice: IBluetoothDevice) : PairingDevice {
    override var connectionState: DeviceState = DeviceState.Disconnected
    override val deviceName: String = matrixDevice.displayName()
    override val deviceId: String = matrixDevice.macAddress()
}
private class MatrixPairingHandler(
    val context: Context,
    val scanner: IBluetoothScanner,
) : PairingDeviceHandler() {
    override val devices = mutableListOf<MatrixPairDevice>()

    private fun clear() {
        devices.forEach { it.matrixDevice.close() }
        devices.clear()
        callbacks?.onListUpdated()
    }

    private fun updateState(device: MatrixPairDevice, newState: DeviceState? = null) {
        device.connectionState = newState ?: readDeviceState(device.matrixDevice)
        callbacks?.onDeviceUpdated(device)
    }

    override fun start() {
        // BluetoothPairingActivity onCreate does some things for BT sink, but BT sink may be deprecated? not sure if model should handle it or UI flow control?
        //clear() // clearing at stop should be enough
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        context.registerReceiver(bluetoothDeviceNotifications, intentFilter)
        scanner.scan { device, scanRecord ->
            if (isValidDevice(scanner, device)) {
                val existing = devices.find { it.deviceId == device.macAddress() }
                if (existing == null) {
                    val newPair = MatrixPairDevice(device)
                    devices.add(newPair)
                    callbacks?.onListUpdated()
                    updateState(newPair)
                }
            }
        }
    }
    override fun stop() {
        scanner.stopScan()
        try { context.unregisterReceiver(bluetoothDeviceNotifications) } catch (_:Exception) {}
        clear()
        DataMatrix.sendEvent(BTSinkKeys.ACTION_PAIRING_COMPLETE) // BT sink may be deprecated? should this in model or UI launcher for when DevicePairing pauses?
    }

    override fun connect(device: PairingDevice) {
        (device as? MatrixPairDevice)?.let {matrixPair ->
            if (scanner is HeartRateScanner || scanner is MediaDeviceScanner) {
                // Clear any previous connected HR Devices (scanner.clearDevices does not work on salutron, also not sure if filters to just HR BLE either?)
                devices.forEach {
                    // you should call disconnect even if device is disconnected, salutron might try to recover a lost connection if not explicitly disconnected
                    it.matrixDevice.disconnect(false)
                    updateState(it)
                }
            }
            //updateState(matrixPair, DeviceState.Connecting) // should we force state before connect call, or rely on reading state after connect call?
            val pinCode = matrixPair.matrixDevice.connect { result, btDevice ->
                Log.d(TAG, "Device connection result $result '${btDevice.displayName()}' ${btDevice.macAddress()}")
                when (result) {
                    IBluetoothConnectionResult.ConnectionResult.Failed-> updateState(matrixPair, DeviceState.Disconnected)
                    else -> updateState(matrixPair)
                }
            }
            if (pinCode?.isNotEmpty() == true) callbacks?.onPinCode(matrixPair, pinCode)
            updateState(matrixPair)
        }
    }
    override fun abortConnect(device: PairingDevice) {
        (device as? MatrixPairDevice)?.let {
            it.matrixDevice.cancelConnection()
            updateState(it)
        }
    }

    override fun close() {
        scanner.close()
    }

    override fun disconnect(device: PairingDevice) {
        (device as? MatrixPairDevice)?.let {
            it.matrixDevice.disconnect(true)
            updateState(it)
        }
    }
    private val bluetoothDeviceNotifications = object : BroadcastReceiver() {
        // seems like we should really just listen to the device handlers for state changes instead of shadowing BT events?
        override fun onReceive(context: Context?, intent: Intent?) {
            val disconnected = intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED
            val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            devices.find { it.deviceId == device?.address }?.let {
                // should we probably defer to the device classes to handle this? but then they'd need to keep listeners open? maybe review as future rework?`
                if (disconnected && !it.matrixDevice.connecting()) { // don't force disconnect device if in the connecting phase, there might be retry logic for the device
                    it.matrixDevice.disconnect(false) // make sure device is actually disconnected
                }
                updateState(it)
            }
        }
    }
}
private fun readDeviceState(device: IBluetoothDevice): DeviceState {
    if (device.connected()) return DeviceState.Connected
    if (device.connecting()) return DeviceState.Connecting
    return DeviceState.Disconnected
}
fun isValidDevice(activeScanner: IBluetoothScanner, device: IBluetoothDevice): Boolean {
    return (activeScanner is SalutronHeartRateScanner && device is SalutronHeartRateDevice)
            || (device.macAddress().matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex())
            && ((activeScanner is HeadphoneScanner && device is HeadphoneDevice)
            || (activeScanner is MediaDeviceScanner && device is MediaBluetoothDevice)
            || (activeScanner is SalutronHeartRateScanner && device is HeartRateDevice)
            || (activeScanner is HeartRateScanner && device is HeartRateDevice)))
}
//endregion Device Pairing
