package com.jht.firsttimesetup

import com.jht.data.ChannelListPreview
import com.jht.firsttimesetup.viewmodels.DigitalProduct
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.FacilityProfileViewModel
import com.jht.interfaces.models.SystemModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import com.jht.firsttimesetup.viewmodels.SystemSettingsUiModel
import junit.framework.TestCase.assertEquals
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnitParamsRunner::class)
class FacilityProfileViewModelTest {

    private lateinit var vm: FacilityProfileViewModel

    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel

    @MockK
    private lateinit var systemModel: SystemModel

    @Before
    fun setup() {

        MockKAnnotations.init(this, relaxed = true)
        every { ftsuModel.isOnyx() } returns true

        vm = FacilityProfileViewModel(
            ftsuModel,
            systemModel
        )
    }

    @Test
    fun `Given an onyx frame type we expect to show LED color`() {
        val fakeFacility = Facility(
            facilityId = "67",
            name = "World of fitness pro plus",
            digitalProducts = emptyList(),
            location = null,
            timeZoneIndex = 9,
            systemSettings = null,
        )
        every { ftsuModel.isOnyx() } returns true
        vm = FacilityProfileViewModel(
            ftsuModel,
            systemModel
        )

        vm.setFacility(fakeFacility)
        val uiState = vm.uiState.value
        assertEquals(true, uiState.profile.showLedColor)
    }

    @Test
    fun `Given a non onyx frame type we expect to show LED color`() {
        val fakeFacility = Facility(
            facilityId = "67",
            name = "World of fitness pro plus",
            digitalProducts = emptyList(),
            location = null,
            timeZoneIndex = 9,
            systemSettings = null,
        )
        every { ftsuModel.isOnyx() } returns false
        vm = FacilityProfileViewModel(
            ftsuModel,
            systemModel
        )

        vm.setFacility(fakeFacility)
        val uiState = vm.uiState.value
        assertEquals(false, uiState.profile.showLedColor)
    }

    @Test
    fun `Importing facility settings sets the IANA timezone not the display string`() {
        every { ftsuModel.getTimeZoneList() } returns listOf("(GMT-10:00) Hawaii", "(GMT-06:00) Central Time")
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu", "America/Chicago")
        vm.setFacility(
            Facility(
                facilityId = "67",
                name = "World of fitness pro plus",
                digitalProducts = emptyList(),
                location = null,
                timeZoneIndex = 1,
                systemSettings = SystemSettingsUiModel(
                    colorLedDefault = "",
                    timeFormatIs24 = false,
                    dateIndex = 0,
                    unitFormatIsMetric = false
                ),
            )
        )

        vm.setLocalSettingsToImportedSettings()

        verify(exactly = 1) { ftsuModel.setTimeZone("America/Chicago", 1) }
    }

    @Test
    fun `Importing facility settings with an out of range index does not crash`() {
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu")
        vm.setFacility(
            Facility(
                facilityId = "67",
                name = "World of fitness pro plus",
                digitalProducts = emptyList(),
                location = null,
                timeZoneIndex = 5,
                systemSettings = SystemSettingsUiModel(
                    colorLedDefault = "",
                    timeFormatIs24 = false,
                    dateIndex = 0,
                    unitFormatIsMetric = false
                ),
            )
        )

        vm.setLocalSettingsToImportedSettings()

        verify(exactly = 0) { ftsuModel.setTimeZone(any(), any()) }
    }

    @Test
    fun `Importing a facility without systemSettings still sets its timezone and nothing else`() {
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu", "Asia/Tokyo")
        vm.setFacility(
            Facility(
                facilityId = "67",
                name = "World of fitness pro plus",
                digitalProducts = emptyList(),
                location = null,
                timeZoneIndex = 1,
                systemSettings = null,
            )
        )

        vm.setLocalSettingsToImportedSettings()

        verify(exactly = 1) { ftsuModel.setTimeZone("Asia/Tokyo", 1) }
        verify(exactly = 0) { ftsuModel.setDateFormat(any()) }
        verify(exactly = 0) { ftsuModel.setIsMetricFormat(any()) }
    }

    @Test
    fun `A facility without systemSettings still shows its timezone`() {
        every { ftsuModel.getTimeZoneList() } returns listOf("(GMT-10:00) Hawaii", "(GMT+09:00) Osaka, Sapporo, Tokyo")
        vm.setFacility(
            Facility(
                facilityId = "67",
                name = "World of fitness pro plus",
                digitalProducts = emptyList(),
                location = null,
                timeZoneIndex = 1,
                systemSettings = null,
            )
        )

        assertEquals("(GMT+09:00) Osaka, Sapporo, Tokyo", vm.uiState.value.profile.timeZone)
    }

    @Test
    fun `A facility without systemSettings still shows its digital products and channels`() {
        val products = listOf(DigitalProduct("Engage 360", true), DigitalProduct("iFit", true))
        val tv = Channel(name = "News", number = "2")
        vm.setFacility(
            Facility(
                facilityId = "67",
                name = "World of fitness pro plus",
                digitalProducts = products,
                location = null,
                timeZoneIndex = 1,
                systemSettings = null,
                channelPreview = ChannelListPreview(channels = listOf(tv), defaultChannel = tv),
            )
        )

        val profile = vm.uiState.value.profile
        assertEquals(products, profile.digitalProducts)
        assertEquals("1", profile.numberOfChannels)
        assertEquals("News", profile.defaultChannel)
    }
}
