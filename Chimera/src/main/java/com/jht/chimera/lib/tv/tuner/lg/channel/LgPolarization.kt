package com.jht.chimera.lib.tv.tuner.lg.channel

/**
 * Polarization types that LG devices support
 */
enum class LgPolarization {
    HORIZONTAL,
    LEFT_HAND_CIRCULAR,
    RIGHT_HAND_CIRCULAR,
    VERTICAL,

    /**
     * Unknown is an invalid polarization option whose only purpose is helping enforce that a
     * valid polarization is chosen. Sending [UNKNOWN] to LG device will not work.
     */
    UNKNOWN;

    /** Returns a user-friendly display name for this polarization type. */
    fun displayName(): String = when (this) {
        UNKNOWN -> "Unknown"
        HORIZONTAL -> "Horizontal"
        VERTICAL -> "Vertical"
        LEFT_HAND_CIRCULAR -> "Left Hand Circular"
        RIGHT_HAND_CIRCULAR -> "Right Hand Circular"
    }

    companion object {
        /** Returns the [LgPolarization] whose name matches [value], or [UNKNOWN] if none matches. */
        fun fromName(value: String?): LgPolarization =
            entries.firstOrNull { it.name.equals(value, true) } ?: UNKNOWN

        /** Returns selectable polarization types with [UNKNOWN] first, rest sorted alphabetically. */
        fun asList(): List<LgPolarization> = listOf(UNKNOWN) +
            (entries - UNKNOWN).sortedBy { it.displayName() }
    }
}
