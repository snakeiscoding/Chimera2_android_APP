package com.jht.interfaces.models

import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.HeartRateSource
import com.jht.enums.RelativeLocation
import com.jht.enums.WifiState
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.IStartStop

/**
 * Provides a model for accessing generic info on devices
 */
abstract class DeviceModel : BaseModel {
    /** Provides relative location of the RFID scanner if there is one */
    abstract val rfidReaderLocation: RelativeLocation // or should RFID scanner be a device and this be a getDeviceLocation() ?
    /** Gets a listener for the generic [DeviceState] of given [Device] */
    abstract fun getDeviceState(device:Device) : DataValueListener<DeviceState> // should this return a GenericDevice object which would have a state property?
    /** Gets the current heart rate source */
    abstract val heartRateSource : DataValueListener<HeartRateSource>
    /** Gets a listener for the current [WifiState] */
    abstract val wifiState : DataValueListener<WifiState>

    /** Get a pairing handler for audio output devices. Returns null if not supported */
    abstract fun createPairingHandlerAudioOut(): PairingDeviceHandler?
    /** Get a pairing handler for heart rate devices. Returns null if not supported */
    abstract fun createPairingHandlerHeartRate(): PairingDeviceHandler?
    /** Get a pairing handler for FTMS. Returns null if not supported */
    abstract fun createPairingHandlerFtms(): PairingDeviceHandler?
}



/** The basic tracking data for a Pairing device */
interface PairingDevice {
    /** Friendly name for the device */
    val deviceName: String
    /** A unique identifier for the device, usually an address */
    val deviceId: String
    /** Generic connection state of the device */
    var connectionState: DeviceState
}

/** Event callbacks for [PairingDeviceHandler] */
interface PairingCallbacks {
    /** Called when list structure is updated, such as add/remove items */
    fun onListUpdated()
    /** Called when a device is updated, usually the device state */
    fun onDeviceUpdated(device: PairingDevice)
    /** Called when connect routine receives a PIN */
    fun onPinCode(device: PairingDevice, code: String)
}

/** Provides the consumer data and events for device pairing */
abstract class PairingDeviceHandler : IStartStop, AutoCloseable {
    /** The callbacks to invoke by implementation of this class */
    var callbacks: PairingCallbacks? = null
    /** List of currently known devices */
    abstract val devices: List<PairingDevice>
    /** Indicates handler should start scanning and monitoring devices */
    abstract override fun start()
    /** Indicates handler can stop scanning and monitoring devices */
    abstract override fun stop()
    /** Attempt to connect to the given device from the list */
    abstract fun connect(device: PairingDevice)
    /** Attempt to disconnect to the given device from the list */
    abstract fun disconnect(device: PairingDevice)
    /** Attempt to abort connection attempt to the given device from the list */
    abstract fun abortConnect(device: PairingDevice)
    /** Cleanup resources (ie unregister receivers) */
    abstract override fun close()
}
