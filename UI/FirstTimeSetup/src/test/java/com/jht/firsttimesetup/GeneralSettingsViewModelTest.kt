package com.jht.firsttimesetup

import com.jht.firsttimesetup.viewmodels.GeneralSettingsViewModel
import com.jht.interfaces.models.SystemModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GeneralSettingsViewModelTest {

    private lateinit var vm: GeneralSettingsViewModel

    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel

    @MockK
    private lateinit var systemModel: SystemModel


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        vm = GeneralSettingsViewModel(
            ftsuModel,
            systemModel
        )
    }

    @Test
    fun `Given a timezone is already set opening general settings selects it and does not change it`() {
        every { ftsuModel.isTimeZoneSet() } returns true
        every { ftsuModel.getCurrentTimeZoneIndex() } returns 63

        vm = GeneralSettingsViewModel(ftsuModel, systemModel)

        assertEquals(63, vm.uiState.value.selectedTimeZoneIndex)
        verify(exactly = 0) { ftsuModel.setTimeZone(any(), any()) }
    }

    @Test
    fun `Given no timezone is set opening general settings applies US Central by IANA id`() {
        every { ftsuModel.isTimeZoneSet() } returns false
        every { ftsuModel.getIanaTimeZoneList() } returns List(10) { "Zone/$it" }.toMutableList().apply {
            set(9, "America/Chicago")
        }
        every { ftsuModel.getCurrentTimeZoneIndex() } returns 9

        vm = GeneralSettingsViewModel(ftsuModel, systemModel)

        verify(exactly = 1) { ftsuModel.setTimeZone("America/Chicago", 9) }
        assertEquals(9, vm.uiState.value.selectedTimeZoneIndex)
    }

    @Test
    fun `Given isEdit is true setLedColor should not be called in ftsu model`() {
        val color = 0xff1256
        vm.isEdit = true
        vm.setLedColor(color)
        verify(exactly = 0) { ftsuModel.setLedColor(color) }
    }

    @Test
    fun `Given isEdit is false setLedColor should be called in ftsu model`() {
        val color = 0xff1256
        vm.isEdit = false
        vm.setLedColor(color)
        verify(exactly = 1) { ftsuModel.setLedColor(color) }
    }

    @Test
    fun `Given isEdit is true setTimeZone should not be called in ftsu model`() {
        val timeZone = "DaylightSavingsAllYear"
        val timeZoneIndex = 0
        vm.isEdit = true
        vm.setTimeZone(timeZone, timeZoneIndex)
        verify(exactly = 0) { ftsuModel.setTimeZone(timeZone, timeZoneIndex) }
    }

    @Test
    fun `Given isEdit is false setTimeZone should be called in ftsu model`() {
        val timeZone = "DaylightSavingsAllYear"
        val timeZoneIndex = 0
        vm.isEdit = false
        vm.setTimeZone(timeZone, timeZoneIndex)
        verify(exactly = 1) { ftsuModel.setTimeZone(timeZone, timeZoneIndex) }
    }


    @Test
    fun `Given isEdit is true setDateFormat should not be called in ftsu model`() {
        val date = 0
        vm.isEdit = true
        vm.setDateFormat(date)
        verify(exactly = 0) { ftsuModel.setDateFormat(date) }
    }

    @Test
    fun `Given isEdit is false setDateFormat should be called in ftsu model`() {
        val date = 0
        vm.isEdit = false
        vm.setDateFormat(date)
        verify(exactly = 1) { ftsuModel.setDateFormat(date) }
    }

    @Test
    fun `Given isEdit is true setUnitFormat should not be called in ftsu model`() {
        val isMetric = true
        vm.isEdit = true
        vm.setUnitFormat(isMetric)
        verify(exactly = 0) { ftsuModel.setIsMetricFormat(isMetric) }
    }

    @Test
    fun `Given isEdit is false setUnitFormat should be called in ftsu model`() {
        val isMetric = true
        vm.isEdit = false
        vm.setUnitFormat(isMetric)
        verify(exactly = 1) { ftsuModel.setIsMetricFormat(isMetric) }
    }


    @Test
    fun `Given isEdit is true setTimeFormat should not be called in ftsu model`() {
        val is24Hour = true
        vm.isEdit = true
        vm.setTimeFormat(is24Hour)
        verify(exactly = 0) { ftsuModel.setTimeFormatType(is24Hour) }
    }

    @Test
    fun `Given isEdit is false setTimeFormat should be called in ftsu model`() {
        val is24Hour = true
        vm.isEdit = false
        vm.setTimeFormat(is24Hour)
        verify(exactly = 1) { ftsuModel.setTimeFormatType(is24Hour) }
    }

    @Test
    fun `Given onSaveClicked we expect ftsu model to update all settings`() {
        vm.onSaveClicked()
        verify(exactly = 1) { ftsuModel.setTimeFormatType(any()) }
        verify(exactly = 1) { ftsuModel.setIsMetricFormat(any()) }
        verify(exactly = 1) { ftsuModel.setDateFormat(any()) }
        verify(exactly = 1) { ftsuModel.setTimeZone(any(), any()) }
        verify(exactly = 1) { ftsuModel.setLedColor(any()) }
        verify(exactly = 1) { ftsuModel.setTimeZone(any(), any()) }
    }
}