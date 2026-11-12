package com.jht.firsttimesetup.mappers

import com.jht.firsttimesetup.Address
import com.jht.firsttimesetup.Country
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapFacilitySearchToUIModel
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapFacilityToProfile
import com.jht.firsttimesetup.FacilitySearchModelMapper.mapSystemDefaultsToUiModel
import com.jht.firsttimesetup.FacilitySearchResponse
import com.jht.firsttimesetup.Organization
import com.jht.firsttimesetup.State
import com.jht.firsttimesetup.SystemSettings
import com.jht.firsttimesetup.viewmodels.GeneralSettingsUiState
import com.jht.firsttimesetup.viewmodels.SystemSettingsUiModel
import org.junit.Test

class FacilitySearchModelMapperTest {

    private val ianaTimeZones = listOf("Pacific/Honolulu", "America/Chicago", "Asia/Tokyo")

    private val address = Address(
        city = "Tokyo",
        countryId = 275,
        createdAt = "1234",
        facilityId = 100,
        id = 24,
        postalCode = "34638",
        stateId = 9001,
        street1 = "67 Gamabunta Ave",
        street2 = "Apartment 7",
        updatedAt = "12345"
    )

    private val country = Country(
        id = 32,
        iso = "54321",
        name = "Nihon",
        printableName = "Japan"
    )

    private val state = State(
        countryId = 1,
        id = 23,
        iso = "CT",
        name = "Central Time",
        printableName = "Central Time"
    )

    private val organization = Organization(
        name = "Planet Fitness",
        id = 67,
        createdAt = "5454563413"
    )

    private val systemSettings = SystemSettings(
        colorLEDDefault = "#0000FF",
        dateFormat = 0,
        timeFormatIs24 = true,
        unitFormatIsMetric = true
    )

    private val response = FacilitySearchResponse(
        name = "JHT Test Facility",
        address = address,
        contact = "",
        country = country,
        createdAt = "",
        facilityId = 1,
        organization = organization,
        organizationId = 2,
        state = state,
        timezone = "America/Chicago",
        updatedAt = "",
        digitalProducts = emptyList(),
        systemSettings = systemSettings
    )

    @Test
    fun `Given a null facility name mapFacilitySearchToUIModel returns null`() {
        val response = response.copy(name = null)

        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result == null)
    }

    @Test
    fun `Given a null street1 name mapFacilitySearchToUIModel returns empty string`() {
        val address = address.copy(street1 = null)
        val response = response.copy(address = address)

        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.street1 == "")
    }

    @Test
    fun `Given a null street2 name mapFacilitySearchToUIModel returns empty string`() {
        val address = address.copy(street2 = null)
        val response = response.copy(address = address)

        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.street2 == "")
    }

    @Test
    fun `Given a null city name mapFacilitySearchToUIModel returns empty string`() {
        val address = address.copy(city = null)
        val response = response.copy(address = address)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.city == "")
    }

    @Test
    fun `Given a null postal code mapFacilitySearchToUIModel returns empty string`() {
        val address = address.copy(postalCode = null)
        val response = response.copy(address = address)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.postalCode == "")
    }

    @Test
    fun `Given a null country name mapFacilitySearchToUIModel returns empty string`() {
        val country = country.copy(name = null)
        val response = response.copy(country = country)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.country == "")
    }

    @Test
    fun `Given a null country ISO mapFacilitySearchToUIModel returns empty string`() {
        val country = country.copy(iso = null)
        val response = response.copy(country = country)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.countryIso == "")
    }

    @Test
    fun `Given a null country ISO3 mapFacilitySearchToUIModel returns empty string`() {
        val country = country.copy(iso3 = null)
        val response = response.copy(country = country)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.countryIso3 == "")
    }

    @Test
    fun `Given a null state name mapFacilitySearchToUIModel returns empty string`() {
        val state = state.copy(name = null)
        val response = response.copy(state = state)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.state == "")
    }

    @Test
    fun `Given a null state ISO mapFacilitySearchToUIModel returns empty string`() {
        val state = state.copy(iso = null)
        val response = response.copy(state = state)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.stateIso == "")
    }

    @Test
    fun `Given a null facility address fields for facilityAddress mapFacilitySearchToUIModel returns a valid FacilityAddress`() {
        val address = address.copy(street1 = null, street2 = null, city = null, postalCode = null)
        val country = country.copy(name = null, iso = null, iso3 = null)
        val state = state.copy(name = null, iso = null)
        val response = response.copy(address = address, country = country, state = state)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.location?.street1 == "")
        assert(result?.location?.street2 == "")
        assert(result?.location?.postalCode == "")
        assert(result?.location?.country == "")
        assert(result?.location?.countryIso == "")
        assert(result?.location?.countryIso3 == "")
        assert(result?.location?.state == "")
        assert(result?.location?.stateIso == "")


    }

    @Test
    fun `Given a null facilityId mapFacilitySearchToUIModel returns a valid Facility`() {
        val response = response.copy(facilityId = null)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result != null)
    }

    @Test
    fun `Given a null systemSettings mapFacilitySearchToUIModel returns a valid Facility`() {
        val response = response.copy(systemSettings = null)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result != null)
    }

    @Test
    fun `Given a null systemSettings mapSystemDefaultsToUiModel returns a valid null`() {
        val result = mapSystemDefaultsToUiModel(null)
        assert(result == null)
    }

    @Test
    fun `Given a null colorLEDDefault in systemSettings mapSystemDefaultsToUiModel color to be empty string`() {
        val result = mapSystemDefaultsToUiModel(systemSettings.copy(colorLEDDefault = null))
        assert(result?.colorLedDefault == "")
    }

    @Test
    fun `Given a null timeFormatIs24 in systemSettings mapSystemDefaultsToUiModel timeFormatIs24 to be false`() {
        val result = mapSystemDefaultsToUiModel(systemSettings.copy(timeFormatIs24 = null))
        assert(result?.timeFormatIs24 == false)
    }

    @Test
    fun `Given a null dateFormat in systemSettings mapSystemDefaultsToUiModel dateFormat to be empty string`() {
        val result = mapSystemDefaultsToUiModel(systemSettings.copy(dateFormat = null))
        assert(result?.dateIndex == 0)
        //toodo yeet update test
    }

    @Test
    fun `Given a null unitFormatIsMetric in systemSettings mapSystemDefaultsToUiModel unitFormatIsMetric to be false`() {
        val result = mapSystemDefaultsToUiModel(systemSettings.copy(unitFormatIsMetric = null))
        assert(result?.unitFormatIsMetric == false)
    }

    @Test
    fun `Given an empty ledColor for SystemSettingsUiModel we expect mapFacilityToProfile to have a default color of 0 and ledHex to be empty`() {
        val settings = SystemSettingsUiModel(
            colorLedDefault = "",
            timeFormatIs24 = true,
            unitFormatIsMetric = true,
            dateIndex = 0
        )
        val generalSettings = GeneralSettingsUiState()

        val result = mapFacilityToProfile(
            settings, generalSettings = generalSettings,
            dates = generalSettings.dateFormats
        )

        assert(result.ledColor == 0)
        assert(result.ledHexString == "")
    }

    @Test
    fun `Given an empty list of unit formats for general settings we expect mapFacilityToProfile to have a unitFormat to be an empty string`() {
        val settings = SystemSettingsUiModel(
            colorLedDefault = "",
            timeFormatIs24 = true,
            unitFormatIsMetric = true,
            dateIndex = 0
        )
        val generalSettings = GeneralSettingsUiState(unitFormats = emptyList())
        val result = mapFacilityToProfile(
            settings, generalSettings = generalSettings,
            dates = generalSettings.dateFormats
        )
        assert(result.unitFormat == "")
    }

    @Test
    fun `Given an empty list of time formats for general settings we expect mapFacilityToProfile to have a unitFormat to be an empty string`() {
        val settings = SystemSettingsUiModel(
            colorLedDefault = "",
            timeFormatIs24 = true,
            unitFormatIsMetric = true,
            dateIndex = 0
        )
        val generalSettings = GeneralSettingsUiState(timeFormats = emptyList())
        val result = mapFacilityToProfile(
            settings, generalSettings = generalSettings,
            dates = generalSettings.dateFormats,
        )
        assert(result.timeFormat == "")
    }

    @Test
    fun `Given a facility timezone in search response we expect its index in the IANA list`() {
        val response = response.copy(timezone = "Asia/Tokyo")
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.timeZoneIndex == ianaTimeZones.indexOf("Asia/Tokyo"))
    }

    @Test
    fun `Given an unknown facility timezone we expect US Central and an error`() {
        val errors = mutableListOf<String>()
        val response = response.copy(timezone = "Central Time")
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones, logError = { errors += it })
        assert(result?.timeZoneIndex == ianaTimeZones.indexOf("America/Chicago"))
        assert(errors.size == 1)
    }

    @Test
    fun `Given no systemSettings in search response we still map the facility timezone`() {
        val response = response.copy(timezone = "Asia/Tokyo", systemSettings = null)
        val result = mapFacilitySearchToUIModel(response, ianaTimeZones)
        assert(result?.systemSettings == null)
        assert(result?.timeZoneIndex == ianaTimeZones.indexOf("Asia/Tokyo"))
    }
}
