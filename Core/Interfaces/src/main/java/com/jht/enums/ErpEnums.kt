package com.jht.enums

enum class ErpMode {
    Disabled,
    Inactivity,
    Absolute_Time;

    companion object {
        private val map = ErpMode.values().associateBy(ErpMode::ordinal)
        fun fromInt(id: Int) = map[id] ?: Disabled
    }

    val isDisabled: Boolean get() = this == Disabled
    val isInactivity: Boolean get() = this == Inactivity
    val isAbsoluteTime: Boolean get() = this == Absolute_Time
}