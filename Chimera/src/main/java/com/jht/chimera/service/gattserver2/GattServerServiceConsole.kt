package com.jht.chimera.service.gattserver2

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.rfid.WatchData
import com.jht.chimera.service.gattserver2.services.CscServiceConsole
import com.jht.chimera.service.gattserver2.services.DeviceInformationServiceConsole
import com.jht.chimera.service.gattserver2.services.EnhancedGymServiceConsole
import com.jht.chimera.service.gattserver2.services.FtmsServiceConsole
import com.jht.chimera.service.gattserver2.services.GymKitServiceConsole
import com.jht.chimera.service.usbmedia.ios.lib.auth.AuthCp3
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver2.AppleWatch
import com.jht.gattserver2.BaseWatch
import com.jht.gattserver2.GattServerService2
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.SamsungWatch
import com.jht.gattserver2.WatchState
import com.jht.gattserver2.WatchType
import com.jht.gattserver2.services.BaseGattService
import com.jht.gattserver2.services.BondManagementService
import com.jht.gattserver2.services.FtmsService.IFtmsVariances

private const val TAG = "GattServerServiceConsole"
private fun isBikeFrame(frameConfig: FrameConfig?): Boolean {
    return when (frameConfig?.machineClassType) {
        JHTCommonTypes.MachineClassificationType.Bike, JHTCommonTypes.MachineClassificationType.SpinBike, JHTCommonTypes.MachineClassificationType.RecumbentElliptical -> true
        else -> false
    }
}

/** this system's implementation of a [GattServerService2] instance */
@SuppressLint("MissingPermission")
class GattServerServiceConsole : GattServerService2() {
    private val dataMatrix = DataMatrix()
    private var currentFrame: FrameConfigEnum? = null
    private var bleHeartRateAddress: String? = null
    private val ftmsVariance = object : IFtmsVariances {
        override fun needsIndoorCadenceFlip(device: BluetoothDevice?): Boolean {
            // XMLs show "Instantaneous Cadence" presence as the same polarity as everything else, but PDF shows it reversed
            // if anything else needs indoor bike cadence flip, add here
            return watchManager.needsIndoorCadenceFlip(device)
        }
    }
    override val defaultBleName: String
        get() = DataMatrix.getString(SettingKeys.Hardware.DATA_SALUTRON_BLE_NAME_STRING).ifEmpty { DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING) }

    override fun onServerSetupWatches() {
        watchManager.disconnectAll() // if watch setup is called, GATT system must have been restarted so we should force watches to reconnect (some watches don't handle BT toggle well)
        val supportsAppleWatch = (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL) && ProductConfig.showGymkitUI())
        val supportsSamsungWatch = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)
        // handle the watch with the BT/WiFi module if there is no BTSink (gymkit) module
        val hasAppleBtSink = DataMatrix.getString(HALKeys.DATA_GYM_KIT_FIRMWARE_VERSION_STRING) != "N/A"
        watchManager.enableWatch(WatchType.Apple, supportsAppleWatch && !hasAppleBtSink)
        watchManager.enableWatch(WatchType.Samsung, supportsSamsungWatch)
    }
    override fun onWatchStateUpdate(watch: BaseWatch) {
        val key = when (watch.watchType) {
            WatchType.Apple -> HALKeys.DATA_GYM_KIT_STATUS_INT
            WatchType.Samsung -> HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT
            else -> null
        }

        //We ignore the apple watch connecting state because there is no corresponding UI flow
        if(!(watch.watchType == WatchType.Apple && watch.state == WatchState.Connecting)) {
            key?.let { DataMatrix.putInt(it, watch.state.ordinal) }
        }
    }

    override fun shouldIgnoreDeviceConnectionEvent(device: BluetoothDevice): Boolean {
        bleHeartRateAddress?.let { if (it == device.name) return true }
        return false
    }

    // TODO - AuthCp3 is wrapping a low level i2c access and is used in other places, we may want to designate a single arbiter for that somewhere? perhaps create a Linux driver to wrap it?
    override fun getAppleAuthenticationCertificate(): ByteArray = AuthCp3(ProductConfig.portMappings.authCp3Path()).use { it.accessoryCertificate }
    override fun getAppleAuthenticationChallengeResponse(data: ByteArray?): ByteArray = AuthCp3(ProductConfig.portMappings.authCp3Path()).use { it.getChallengeResponse(data) }

    override fun getServicesFor(server: IGattServer): Iterable<BaseGattService> {
        val services = ArrayList<BaseGattService>()
        try {
            services.add(DeviceInformationServiceConsole(server))
            services.add(BondManagementService(server))
            FrameConfigManager.getInstance().frameConfig?.let {
                services.add(FtmsServiceConsole(server, ftmsVariance, it))
                if (isBikeFrame(it)) services.add(CscServiceConsole(server))
            }
            (watchManager[WatchType.Samsung] as? SamsungWatch)?.let { services.add(EnhancedGymServiceConsole(server, it)) }
            (watchManager[WatchType.Apple] as? AppleWatch)?.let { services.add(GymKitServiceConsole(server, it)) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create all GATT services", e)
        }
        return services
    }
    override fun allowAdvertisingFor(service: BaseGattService): Boolean {
        // uncomment this check to conditionally block just the FTMS service
        /*if (service is FtmsService) {
            return DataMatrix.getBoolean(SettingKeys.Hardware.DATA_FTMS_SHOULD_ADVERTISE_BOOL) || overridingBleLocalName
        }*/
        return true
    }
    // uncomment this line to conditionally block all advertising (watching pairing overrides will override this)
    override val shouldAdvertise: Boolean get() = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_FTMS_SHOULD_ADVERTISE_BOOL)

    private fun checkGymKitModuleConnection(): Boolean {
        // Gym kit hardware module could be managing a connection outside of watch manager, so we check just in case
        return if (DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != HALKeys.STATUS_GYM_KIT_DISCONNECTED) {
            Log.d(TAG, "GymKit reports a connection")
            true
        } else {
            false
        }
    }

    override fun onCreate() {
        super.onCreate()
        //ServiceHelper.startForegroundService(this); // original one was not foreground (code examples do run as foreground)
        // is this key meant for all advertising or just FTMS?
        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_FTMS_SHOULD_ADVERTISE_BOOL, { shouldAdvertise: Boolean -> toggleAdvertising() }, false)
        // some services depend on knowing what frame we have, so start/restart services when we get new frame
        dataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, { frameString: String? -> frameChange(frameString) }, true)
        //dataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, e -> disconnectDevices()); // not sure this helps anything, leaving this in case it needs revisiting
        dataMatrix.createParcelableAdapter(HALKeys.EVENT_APPLE_WATCH_DETECTED_PARCELABLE, WatchData::class.java) {
            watchData: WatchData -> watchManager.processNDEF(WatchType.Apple, watchData.watchData())
        }
        dataMatrix.createParcelableAdapter(HALKeys.EVENT_SAMSUNG_WATCH_DETECTED_PARCELABLE, WatchData::class.java) { watchData: WatchData ->
            if (!checkGymKitModuleConnection()) // need to check in case gymkit module is managing a watch
                watchManager.processNDEF(WatchType.Samsung, watchData.watchData())
        }
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value -> watchManager.onWorkoutStopped(value == WorkoutKeys.Constants.RUN_STATE_STOP) }, false)
        dataMatrix.createEventAdapter(HALKeys.ACTION_DISCONNECT_SMART_WATCHES) { watchManager.disconnectAll() }
        dataMatrix.createParcelableAdapter(HALKeys.ACTION_CONNECT_BLE_PARCELABLE, BluetoothDevice::class.java,
            { device: BluetoothDevice? -> if (device != null) bleHeartRateAddress = device.name },
            false)
    }
    override fun onDestroy() {
        dataMatrix.finish()
        super.onDestroy()
    }

    private fun frameChange(frameString: String?) {
        try {
            // FrameConfig config = FrameConfigManager.getInstance().getFrameConfig(); // do frame config instead of enum?
            val newFrame = FrameConfigEnum.getFrameTypeFromString(frameString)
            Log.d(TAG, "current frame= $currentFrame, new frame= $newFrame")
            if (newFrame != null && newFrame != currentFrame) {
                disableBleServices()
                currentFrame = newFrame
                checkStartBleServices()
            }
        } catch (e: Exception) {
            Log.e(TAG, "frame change event crashed", e)
        }
    }

}
