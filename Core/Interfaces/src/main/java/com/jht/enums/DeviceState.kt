package com.jht.enums

enum class DeviceState {
    // mirror the WatchStates
    Connecting,
    Connected,
    Disconnected,
    TemporarilyDisconnected,
    // leave not supported last
    NotSupported,
    ;

    companion object {
        private val map = entries.associateBy(DeviceState::ordinal)
        fun fromInt(id: Int) = map[id] ?: Disconnected
        fun fromBool(isConnected : Boolean) = if (isConnected) Connected else Disconnected
        fun fromString(address : String?) = if (address.isNullOrEmpty()) Disconnected else Connected
    }

    val isDisconnected: Boolean get() = this == Disconnected
    val isTemporarilyDisconnected: Boolean get() = this == TemporarilyDisconnected
    val isConnecting: Boolean get() = this == Connecting
    val isConnected: Boolean get() = this == Connected
    val isSupported: Boolean get() = this != NotSupported
}

enum class Device{ // should these be categories or specific devices or both?
    AppleWatch,
    SamsungWatch,
    Headphones,
    BtSink,
    ;

    companion object {
        private val map = entries.associateBy(Device::ordinal)
        fun fromInt(id: Int) = map[id] ?: AppleWatch
    }
}

enum class HeartRateSource {
    /** There is no source */
    None,
    /** There is source, but do not know what */
    Unknown,
    /** Simulated heart rate such as demo mode */
    Simulated,
    Grips,
    FIVE_KHZ,
    BLE,
    ANT_HR,
    ANT_EXD,
    Watch,
    MyZone,
    ;

    companion object {
        private val map = entries.associateBy(HeartRateSource::ordinal)
        fun fromInt(id: Int) = map[id] ?: Unknown
    }
}
