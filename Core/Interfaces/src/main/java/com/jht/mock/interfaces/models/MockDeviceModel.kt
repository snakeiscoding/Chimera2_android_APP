package com.jht.mock.interfaces.models

import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.HeartRateSource
import com.jht.enums.RelativeLocation
import com.jht.enums.WifiState
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.PairingDevice
import com.jht.interfaces.models.PairingDeviceHandler
import com.jht.mock.interfaces.MockDataValue

class MockDeviceModel(
) : DeviceModel() {
    override var rfidReaderLocation = RelativeLocation.NONE
    override fun getDeviceState(device: Device): DataValueListener<DeviceState> = MockDataValue(DeviceState.Disconnected)

    var mockPairingAudioOut: MockPairingHandler? = null
    override fun createPairingHandlerAudioOut(): PairingDeviceHandler? = mockPairingAudioOut
    var mockPairingHeartRate: MockPairingHandler? = null
    override fun createPairingHandlerHeartRate(): PairingDeviceHandler? = mockPairingHeartRate
    var mockPairingFtms: MockPairingHandler? = null
    override fun createPairingHandlerFtms(): PairingDeviceHandler? = mockPairingFtms

    override val heartRateSource: MockDataValue<HeartRateSource> = MockDataValue(HeartRateSource.None)

    override val wifiState: MockDataValue<WifiState> = MockDataValue(WifiState.STRENGTH_100)

    override fun close() {
        //Do nothing
    }
}

//region Device Pairing
class MockPairingHandler : PairingDeviceHandler() {
    var running = false
        private set
    private val mockDevices = mutableListOf<PairingDevice>()
    override val devices: List<PairingDevice> = mockDevices
    override fun start() { running = true }
    override fun stop() { running = false }
    fun addDevice(device: PairingDevice) {
        mockDevices.add(device)
        callbacks?.onListUpdated()
    }
    fun removeDevice(device: PairingDevice) {
        mockDevices.remove(device)
        callbacks?.onListUpdated()
    }
    fun clearDevices() {
        mockDevices.clear()
        callbacks?.onListUpdated()
    }
    fun changeState(device: PairingDevice, state: DeviceState) {
        device.connectionState = state
        callbacks?.onDeviceUpdated(device)
    }
    override fun connect(device: PairingDevice) = changeState(device, DeviceState.Connecting)
    override fun abortConnect(device: PairingDevice) = changeState(device, DeviceState.Disconnected)
    override fun disconnect(device: PairingDevice) = changeState(device, DeviceState.Disconnected)
    override fun close() {}
}
class MockPairingDevice(override val deviceName: String, override val deviceId: String) : PairingDevice {
    override var connectionState: DeviceState = DeviceState.Disconnected
}
//endregion Device Pairing
