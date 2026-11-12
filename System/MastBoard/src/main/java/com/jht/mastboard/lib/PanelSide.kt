@file:JvmName("PanelSide")
package com.jht.mastboard.lib

import kotlin.jvm.Throws

/**
 * fixme: I am pretty sure this can be deprecated.
 *
 * fixme: This doesn't need to be parcelable. Only the parent does. This is not being passed
 *  anywhere by parcel.
 */
enum class PanelSide {
    LEFT,
    RIGHT;

    companion object {
        /**
         * Return the PanelSide value matching the given ordinal.
         *
         * @param ordinal The given ordinal
         * @return The PanelSide value with the matching ordinal.
         * @throws RuntimeException If no PanelSide value exists with the given ordinal.
         */
        @Throws(RuntimeException::class)
        @JvmStatic
        fun fromOrdinal(ordinal: Int): PanelSide {
            return values().firstOrNull { it.ordinal == ordinal }
                ?: throw RuntimeException("No panel with ordinal: $ordinal")
        }
    }
}