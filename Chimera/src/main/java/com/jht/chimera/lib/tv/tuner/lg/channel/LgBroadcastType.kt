package com.jht.chimera.lib.tv.tuner.lg.channel

/**
 * Broadcast types supported by LG television devices
 */
sealed interface LgBroadcastType {

    /** The name of this broadcast type (satisfied by [Enum.name] in enum subclasses) */
    val name: String

    /**
     * IP television broadcast types
     */
    enum class IpBroadcast : LgBroadcastType {
        UNKNOWN,
        UDP,
        RTP;

        /** Returns the display name for this broadcast type. */
        fun displayName(): String = when (this) {
            UNKNOWN -> "Unknown"
            UDP -> "UDP"
            RTP -> "RTP"
        }

        companion object {
            /** Returns the [IpBroadcast] whose name matches [value], or [UNKNOWN] if none matches. */
            fun fromName(value: String?): IpBroadcast =
                entries.firstOrNull { it.name.equals(value, true) } ?: UNKNOWN

            /** Returns the selectable IP broadcast types, with [UNKNOWN] first, rest sorted alphabetically. */
            fun asList(): List<IpBroadcast> = listOf(UNKNOWN) +
                (entries - UNKNOWN).sortedBy { it.displayName() }
        }
    }

    /**
     * RF television broadcast types
     */
    enum class RfBroadcast : LgBroadcastType {
        UNKNOWN,
        TERRESTRIAL,
        TERRESTRIAL_2,
        SATELLITE,
        SATELLITE_2,
        SATELLITE_CS1,
        SATELLITE_CS2,
        SATELLITE_S3_BS,
        SATELLITE_S3_CS,
        CABLE,
        CABLE_STD,
        CABLE_HRC,
        CABLE_IRC,
        ANALOG_PAL_BG,
        ANALOG_PAL_DK,
        ANALOG_PAL_I,
        ANALOG_PAL_M,
        ANALOG_PAL_N,
        ANALOG_SECAM_BG,
        ANALOG_SECAM_DK,
        ANALOG_SECAM_L,
        ANALOG_NTSC,
        TERRESTRIAL_ATSC3,
        CABLE_ATSC3;

        /**
         * Returns the display name for this broadcast type in the given [region]. Falls back to
         * the [LgRegion.GLOBAL] display name, then the enum [name], if not mapped for [region].
         */
        fun displayName(region: LgRegion): String = when (this) {
            UNKNOWN -> "Unknown"
            else -> regionBroadcastMap[region to this]
                ?: regionBroadcastMap[LgRegion.GLOBAL to this]
                ?: name
        }

        companion object {
            /** Returns the [RfBroadcast] whose name matches [value], or [UNKNOWN] if none matches. */
            fun fromName(value: String?): RfBroadcast =
                entries.firstOrNull { it.name.equals(value, true) } ?: UNKNOWN

            /**
             * Returns the selectable RF broadcast types for [region], with [UNKNOWN] first,
             * followed by region-specific + global types sorted alphabetically by display name.
             */
            fun asList(region: LgRegion): List<RfBroadcast> {
                val regionTypes = regionBroadcastMap.keys.filter { it.first == region }.map { it.second }
                val globalTypes = regionBroadcastMap.keys.filter { it.first == LgRegion.GLOBAL }.map { it.second }
                return listOf(UNKNOWN) + (regionTypes + globalTypes).sortedBy { it.displayName(region) }
            }

            //
            // Broadcast type groups — used to restrict which types are valid per RF channel class.
            // These are whitelists that get intersected with the region list in the dialog.

            /** Satellite broadcast types — RF Class 4 (satellite tuning with satelliteId + polarization). */
            val SATELLITE_TYPES = listOf(SATELLITE, SATELLITE_2, SATELLITE_CS1, SATELLITE_CS2, SATELLITE_S3_BS, SATELLITE_S3_CS)

            /** DVB-T2 broadcast type — RF Class 5 (frequency + programNumber + plpId). */
            val TERRESTRIAL_2_TYPES = listOf(TERRESTRIAL_2)

            /** ATSC 3.0 broadcast types — RF Class 6 (major + minor + plpId). */
            val ATSC3_TYPES = listOf(TERRESTRIAL_ATSC3, CABLE_ATSC3)

            /** Non-satellite types — RF Class 1 (logical number, no satellite tuning params). */
            val NON_SATELLITE_TYPES = entries.toList() - SATELLITE_TYPES.toSet() - listOf(UNKNOWN).toSet()

            /** Terrestrial/cable/analog only — RF Class 2 (frequency tuning, no satellite/T2/ATSC3). */
            val TERRESTRIAL_CABLE_TYPES = NON_SATELLITE_TYPES - TERRESTRIAL_2_TYPES.toSet() - ATSC3_TYPES.toSet()

            /**
             * Defines which RF broadcast types are supported per region and their display names.
             *
             * Each entry maps a (region, broadcast type) pair to its region-specific display name.
             * A type's presence in this map for a given region means it is supported there.
             * [LgRegion.GLOBAL] entries are shared across all regions (analog formats).
             */
            private val regionBroadcastMap: Map<Pair<LgRegion, RfBroadcast>, String> = mapOf(
                // Global (analog formats shared across all regions - at least that's what the docs say🤷)
                (LgRegion.GLOBAL to ANALOG_PAL_BG) to "Analog - Phase Alternating Line B",
                (LgRegion.GLOBAL to ANALOG_PAL_DK) to "Analog - Phase Alternating Line DK",
                (LgRegion.GLOBAL to ANALOG_PAL_I) to "Analog - Phase Alternating Line I",
                (LgRegion.GLOBAL to ANALOG_PAL_M) to "Analog - Phase Alternating Line M",
                (LgRegion.GLOBAL to ANALOG_PAL_N) to "Analog - Phase Alternating Line N",
                (LgRegion.GLOBAL to ANALOG_SECAM_BG) to "Analog - SECAM BG",
                (LgRegion.GLOBAL to ANALOG_SECAM_DK) to "Analog - SECAM DK",
                (LgRegion.GLOBAL to ANALOG_SECAM_L) to "Analog - SECAM L",

                // North America
                (LgRegion.NORTH_AMERICA to ANALOG_NTSC) to "Analog - NTSC",
                (LgRegion.NORTH_AMERICA to CABLE) to "Cable - ATSC",
                (LgRegion.NORTH_AMERICA to CABLE_HRC) to "Cable - Harmonic Related Carrier",
                (LgRegion.NORTH_AMERICA to CABLE_IRC) to "Cable - Incremental Related Carrier",
                (LgRegion.NORTH_AMERICA to CABLE_STD) to "Cable - Standard",
                (LgRegion.NORTH_AMERICA to TERRESTRIAL) to "Terrestrial - ATSC",

                // South America
                (LgRegion.SOUTH_AMERICA to CABLE) to "Cable - ISDB-C",
                (LgRegion.SOUTH_AMERICA to SATELLITE) to "Satellite - ISDB-S",
                (LgRegion.SOUTH_AMERICA to TERRESTRIAL) to "Terrestrial - ISDB-T",

                // Europe
                (LgRegion.EUROPE to CABLE) to "Cable - DVB-C",
                (LgRegion.EUROPE to SATELLITE) to "Satellite - DVB-S",
                (LgRegion.EUROPE to SATELLITE_2) to "Satellite - DVB-S2",
                (LgRegion.EUROPE to TERRESTRIAL) to "Terrestrial - DVB-T",
                (LgRegion.EUROPE to TERRESTRIAL_2) to "Terrestrial - DVB-T2",

                // Japan
                (LgRegion.JAPAN to CABLE) to "Cable - ISDB-C",
                (LgRegion.JAPAN to SATELLITE) to "Satellite - ISDB-S",
                (LgRegion.JAPAN to SATELLITE_CS1) to "Satellite - ISDB-CS1",
                (LgRegion.JAPAN to SATELLITE_CS2) to "Satellite - ISDB-CS2",
                (LgRegion.JAPAN to SATELLITE_S3_BS) to "Satellite - ISDB-S3 BS",
                (LgRegion.JAPAN to SATELLITE_S3_CS) to "Satellite - ISDB-S3 CS",
                (LgRegion.JAPAN to TERRESTRIAL) to "Terrestrial - ISDB-T"
            )
        }
    }

    companion object {
        /**
         * Searches both [IpBroadcast] and [RfBroadcast] for an entry whose name matches [value].
         * Returns `null` if no match is found.
         */
        fun fromName(value: String?): LgBroadcastType? =
            IpBroadcast.entries.firstOrNull { it.name.equals(value, true) }
                ?: RfBroadcast.entries.firstOrNull { it.name.equals(value, true) }
    }
}
