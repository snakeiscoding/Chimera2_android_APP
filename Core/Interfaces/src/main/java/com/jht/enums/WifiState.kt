package com.jht.enums

enum class WifiState {
    NO_CONNECTION,
    STRENGTH_25,
    STRENGTH_50,
    STRENGTH_75,
    STRENGTH_100,
    ETHERNET;

    companion object {
        private val map = WifiState.values().associateBy(WifiState::ordinal)
        fun fromInt(id: Int) = map[id] ?: NO_CONNECTION
    }
}