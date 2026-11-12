package com.jht.firsttimesetup

import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.SummaryViewModel
import com.jht.firsttimesetup.viewmodels.SystemSettingsUiModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.mock.interfaces.MockIntValue
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test

class SummaryViewModelTest {

    private lateinit var vm: SummaryViewModel

    @MockK
    private lateinit var ftsuModel: MockFirstTimeSetupModel
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    @MockK
    private lateinit var systemModel: SystemModel

    @MockK
    private lateinit var uiBaseModel: UIBaseModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // A relaxed mock returns Object for the generic value, which updateUi() reads as Int.
        every { systemModel.facilityID } returns MockIntValue(-1)
        // updateUi() maps these with plain indexing, so they must not be empty.
        every { ftsuModel.getTimeZoneList() } returns listOf("(GMT-10:00) Hawaii", "(GMT+08:00) Taipei")
        every { ftsuModel.getDateFormats() } returns listOf("YYYY / MM / DD")
        every { ftsuModel.getUnitFormatTypes() } returns listOf("Metric", "Imperial")
        every { ftsuModel.getSupportedTimeFormats() } returns listOf("12 Hour", "24 Hour")

        vm = SummaryViewModel(
            ftsuModel,
            systemModel,
            dispatcher,
            uiBaseModel
        )
    }


    @Test
    fun `Given onFinishClicked and should update is false we expect to exitFTSU`() {
        vm.shouldUpdate = false
        vm.onFinishClicked()
        verify(exactly = 0) { ftsuModel.installUpdate() }
        verify(exactly = 1) { ftsuModel.exitFTSU() }
    }

    @Test
    fun `Given onFinishClicked and should update is true we expect to install update`() {
        vm.shouldUpdate = true
        vm.onFinishClicked()
        verify(exactly = 1) { ftsuModel.installUpdate() }
        verify(exactly = 0) { ftsuModel.exitFTSU() }
    }

    @Test
    fun `Given an imported facility setFacility stores the IANA timezone not the display string`() {
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu", "Asia/Taipei")
        val facility = Facility(
            facilityId = "63",
            name = "Taipei club",
            location = null,
            digitalProducts = emptyList(),
            timeZoneIndex = 1,
            systemSettings = SystemSettingsUiModel(
                colorLedDefault = "",
                timeFormatIs24 = false,
                dateIndex = 0,
                unitFormatIsMetric = false
            ),
        )

        vm.setFacility(facility)

        verify(exactly = 1) { ftsuModel.setTimeZone("Asia/Taipei", 1) }
    }

    @Test
    fun `Given an out of range timezone index setFacility does not set a timezone`() {
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu")
        val facility = Facility(
            facilityId = "63",
            name = "Taipei club",
            location = null,
            digitalProducts = emptyList(),
            timeZoneIndex = 5,
            systemSettings = SystemSettingsUiModel(
                colorLedDefault = "",
                timeFormatIs24 = false,
                dateIndex = 0,
                unitFormatIsMetric = false
            ),
        )

        vm.setFacility(facility)

        verify(exactly = 0) { ftsuModel.setTimeZone(any(), any()) }
    }

    @Test
    fun `Given an imported facility without systemSettings setFacility sets the timezone and nothing else`() {
        every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu", "Asia/Taipei")
        val facility = Facility(
            facilityId = "63",
            name = "Taipei club",
            location = null,
            digitalProducts = emptyList(),
            timeZoneIndex = 1,
            systemSettings = null,
        )

        vm.setFacility(facility)

        verify(exactly = 1) { ftsuModel.setTimeZone("Asia/Taipei", 1) }
        verify(exactly = 0) { ftsuModel.setDateFormat(any()) }
        verify(exactly = 0) { ftsuModel.setLedColor(any()) }
    }
}
