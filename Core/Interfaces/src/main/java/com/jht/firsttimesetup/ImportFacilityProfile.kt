package com.jht.firsttimesetup

import kotlinx.serialization.Serializable

@Serializable
data class ImportFacilityProfile(
    val selectedTimeZoneIndex: Int = 0,
    val selectedDateFormatIndex: Int = 0,
    val isUnitFormatMetric: Boolean = false,
    val is24Hour: Boolean = false,
    val selectedLedColor: Int = 0,
    val digitalProducts: List<String> = emptyList(),
    val facilityName: String? = null,
    val facilityId: Int = -1,
    val facilityAddress: Address? = null,
    val selectedLanguageIndex: Int = 0,
    val country: Country? = null,
    val state: State? = null
    //todo add tv information  CGC-22100
)