package com.jht.firsttimesetup

import java.util.UUID

/**
 * Model class used to bridge between modules
 */
data class Channel(
    val name: String,
    val number: String,
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    val uuid: String = generateUuid(number, name)
) {
    companion object {
        @JvmStatic
        fun generateUuid(number: String, name: String): String =
            UUID.nameUUIDFromBytes("$number:$name".toByteArray()).toString()
    }
}