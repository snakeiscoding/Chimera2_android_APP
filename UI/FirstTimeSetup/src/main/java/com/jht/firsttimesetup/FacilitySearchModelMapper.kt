package com.jht.firsttimesetup

import com.jht.firsttimesetup.ui.FacilityProfile
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.FacilityAddress
import com.jht.firsttimesetup.viewmodels.GeneralSettingsUiState
import com.jht.firsttimesetup.viewmodels.SystemSettingsUiModel

object FacilitySearchModelMapper {

    /**
     * Maps [ImportFacilityProfile] to [Facility]
     */
    fun mapImportFacilityProfileToFacility(importProfile: ImportFacilityProfile): Facility {
        val address = mapAddressToFacilityAddress(
            address = importProfile.facilityAddress,
            country = importProfile.country,
            state = importProfile.state
        )
        return Facility(
            facilityId = importProfile.facilityId.toString(),
            name = importProfile.facilityName ?: "",
            location = address,
            digitalProducts = StringFormatter.getDigitalProductList(importProfile.digitalProducts),
            timeZoneIndex = importProfile.selectedTimeZoneIndex,
            systemSettings = SystemSettingsUiModel(
                colorLedDefault = importProfile.selectedLedColor.toString(),
                timeFormatIs24 = importProfile.is24Hour,
                dateIndex = importProfile.selectedDateFormatIndex,
                unitFormatIsMetric = importProfile.isUnitFormatMetric
            ),
        )
    }

    /**
     * Maps [GeneralSettingsUiState] to [ImportFacilityProfile]
     */
    fun mapGeneralSettingsToFacilityProfile(settings: GeneralSettingsUiState): ImportFacilityProfile {
        return ImportFacilityProfile(
            selectedDateFormatIndex = settings.selectedDateFormatIndex,
            isUnitFormatMetric = settings.isUnitFormatMetric,
            is24Hour = settings.is24Hour,
            selectedTimeZoneIndex = settings.selectedTimeZoneIndex,
            selectedLedColor = settings.selectedLedColor
        )
    }

    /**
     * Maps [FacilitySearchResponse] to [Facility]
     *
     * @param ianaTimeZones the console's IANA list; `response.timezone` is mapped onto its index.
     * @param logError sink for an unmatched timezone, see [WebTimeZoneMapper.mapToIndex].
     */
    fun mapFacilitySearchToUIModel(
        response: FacilitySearchResponse,
        ianaTimeZones: List<String>,
        logError: (String) -> Unit = { android.util.Log.e("FacilitySearchModelMapper", it) },
    ): Facility? {

        val facilityAddress = mapAddressToFacilityAddress(
            response.address,
            response.country,
            response.state
        )

        return Facility(
            name = response.name ?: return null,
            location = facilityAddress,
            facilityId = response.facilityId.toString(),
            digitalProducts = StringFormatter.getDigitalProductList(
                response.digitalProducts ?: emptyList()
            ),
            timeZoneIndex = WebTimeZoneMapper.mapToIndex(
                webTimeZone = response.timezone,
                ianaList = ianaTimeZones,
                logError = logError
            ),
            systemSettings = mapSystemDefaultsToUiModel(response.systemSettings)
        )
    }

    fun mapAddressToFacilityAddress(
        address: Address?,
        country: Country? = null,
        state: State? = null
        ) : FacilityAddress {
        return FacilityAddress(
            street1 = address?.street1 ?: "",
            street2 = address?.street2 ?: "",
            city = address?.city ?: "",
            postalCode = address?.postalCode ?: "",
            country = country?.name ?: "",
            countryIso = country?.iso
                ?: "", // ISO Country Code, the International Standard Abbreviation (2-Characters)
            countryIso3 = country?.iso3 ?: "", // ISO Country Code (3-Characters)
            state = state?.name ?: "",
            stateIso = state?.iso ?: "" // ISO State Code (2-Characters)
        )
    }

    fun mapSystemDefaultsToUiModel(model: SystemSettings?): SystemSettingsUiModel? {
        if (model == null) return null
        return SystemSettingsUiModel(
            colorLedDefault = model.colorLEDDefault ?: "",
            timeFormatIs24 = model.timeFormatIs24 ?: false,
            dateIndex = model.dateFormat ?: 0,
            unitFormatIsMetric = model.unitFormatIsMetric ?: false
        )
    }

    /**
     * Maps valid attributes from [SystemSettingsUiModel] to [FacilityProfile]
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun mapFacilityToProfile(
        settings: SystemSettingsUiModel,
        generalSettings: GeneralSettingsUiState,
        dates: List<String>
    ): FacilityProfile {
        var ledColor = 0
        var ledHexString = ""
        try {
            ledColor = settings.colorLedDefault.substring(1).hexToInt()
            ledHexString = String.format("#%06X", (0xFFFFFF and ledColor))
        } catch (_: Exception) {
        }

        return FacilityProfile(
            dateFormat = dates.getOrNull(settings.dateIndex) ?: "",
            unitFormat = if (settings.unitFormatIsMetric) {
                generalSettings.unitFormats.getOrNull(0) ?: "" //metric format
            } else {
                generalSettings.unitFormats.getOrNull(1) ?: "" //imperial format
            },
            ledColor = ledColor,
            ledHexString = ledHexString,
            timeFormat = if (settings.timeFormatIs24) {
                generalSettings.timeFormats.getOrNull(1) ?: "" //24 hour
            } else {
                generalSettings.timeFormats.getOrNull(0) ?: "" //12 hour
            }
        )
    }
}