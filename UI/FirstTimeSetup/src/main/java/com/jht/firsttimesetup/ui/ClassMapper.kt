package com.jht.firsttimesetup.ui

import com.jht.firsttimesetup.FacilitySearchResponse
import com.jht.firsttimesetup.ImportFacilityProfile
import com.jht.firsttimesetup.SystemSettings
import com.jht.firsttimesetup.viewmodels.GeneralSettingsUiState

/**
 * This class helps map form one class to another
 */
object ClassMapper {

    /**
     * Maps [ImportFacilityProfile] to [FacilitySearchResponse]
     * does not include any system settings
     */
    fun mapImportProfileToFacilitySearchResponse(importProfile : ImportFacilityProfile) : FacilitySearchResponse {
        return FacilitySearchResponse(
            address = importProfile.facilityAddress,
            country = importProfile.country,
            facilityId = importProfile.facilityId,
            name = importProfile.facilityName,
            state = importProfile.state,
            systemSettings = SystemSettings(
                dateFormat = importProfile.selectedDateFormatIndex,
                timeFormatIs24 = importProfile.is24Hour,
                unitFormatIsMetric = importProfile.isUnitFormatMetric

            )
        )
    }

    /**
     * Maps available properties such as timezone, time format,date format, unit format, and led color
     * from [com.jht.firsttimesetup.Navigation.GeneralSettings] to [Summary]
     */
    internal fun mapGeneralSettingsToSummary(settings: GeneralSettingsUiState): Summary {
        return Summary(
            facilityId = settings.clubId.toString(),
            timeZone = settings.timeZones.getOrNull(settings.selectedTimeZoneIndex) ?: "",
            timeFormat = if (settings.is24Hour) {
                settings.timeFormats[1] //24 hour
            } else {
                settings.timeFormats[0] //12 hour
            },
            dateFormat = settings.dateFormats.getOrNull(settings.selectedDateFormatIndex) ?: "",
            unitFormat = if (settings.isUnitFormatMetric) {
                settings.unitFormats[0] //metric format
            } else {
                settings.unitFormats[1] //imperial format
            },
            ledColor = if (settings.showLedColorSelection) {
                settings.selectedLedColor
            } else {
                null
            },
        )
    }

    /**
     * Maps overlapping properties from [GeneralSettingsUiState] to [FacilityProfile]
     */
    internal fun mapGeneralSettingsToFacilityProfile(settings: GeneralSettingsUiState): FacilityProfile {
        return FacilityProfile(
            timeZone = settings.timeZones[settings.selectedTimeZoneIndex],
            timeFormat = if (settings.is24Hour) {
                settings.timeFormats[1]
            } else {
                settings.timeFormats[0]
            },
            dateFormat = settings.dateFormats[settings.selectedDateFormatIndex],
            unitFormat = if (settings.isUnitFormatMetric) {
                settings.unitFormats[0]
            } else {
                settings.unitFormats[1]
            },
            ledColor = if (settings.showLedColorSelection) {
                settings.selectedLedColor
            } else {
                null
            },
        )
    }
}