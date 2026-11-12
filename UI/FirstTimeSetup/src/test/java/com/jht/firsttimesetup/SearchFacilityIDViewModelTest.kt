package com.jht.firsttimesetup

import com.jht.data.ChannelListPreview
import com.jht.data.OperationState
import com.jht.firsttimesetup.viewmodels.SearchFacilityIdViewModel
import com.jht.mock.interfaces.models.MockSystemModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchFacilityIDViewModelTest {
    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel

    private val systemModel = MockSystemModel()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private lateinit var vm: SearchFacilityIdViewModel

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
        iso = "KA",
        name = "Kanto",
        printableName = "Kanto Region"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // Responses without a timezone log a no-match error, and android.util.Log is not available on the JVM.
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any<String>()) } returns 0
        every { ftsuModel.setClubId(any()) } returns Unit
        vm = SearchFacilityIdViewModel(ftsuModel, dispatcher, systemModel)
    }


    @Test
    fun `Given a successful facility search and valid facilityId we expect uistate to be error free, and facility information`() =
        runTest(dispatcher) {
            val facilityId = "1234"
            every { ftsuModel.previewChannelsFromCore(facilityId.toInt()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            val facilitySearchResponse = FacilitySearchResponse(
                name = "Planet Fitness",
                address = address,
                country = country,
                state = state,
                facilityId = 1234,
                digitalProducts = null
            )
            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                facilitySearchResponse,
                ""
            )
            vm.onQueryChange(facilityId)
            vm.onSearchFacilityClicked()
            val uiState = vm.uiState
            assertEquals(uiState.value.error, null)
        }

    @Test
    fun `Given a facility timezone we expect it mapped onto the model IANA list`() =
        runTest(dispatcher) {
            val facilityId = "1234"
            every { ftsuModel.getIanaTimeZoneList() } returns listOf("Pacific/Honolulu", "America/Chicago", "Asia/Tokyo")
            every { ftsuModel.previewChannelsFromCore(facilityId.toInt()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                FacilitySearchResponse(
                    name = "Planet Fitness",
                    address = address,
                    country = country,
                    state = state,
                    facilityId = 1234,
                    digitalProducts = null,
                    timezone = "Asia/Tokyo",
                    systemSettings = null // optional on the web; the timezone is facility level
                ),
                ""
            )
            vm.onQueryChange(facilityId)
            vm.onSearchFacilityClicked()
            assertEquals(2, vm.uiState.value.facility?.timeZoneIndex)
        }

    @Test
    fun `Given a successful facility search and invalid facilityId we expect uistate error to not be null`() =
        runTest(dispatcher) {
            val facilityId = ""
            val digitalProducts = listOf("ENGAGE_360", "ASSET_MANAGEMENT", "YMCA360")

            every { ftsuModel.previewChannelsFromCore(any()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            val facilitySearchResponse = FacilitySearchResponse(
                name = "Workout Academy of Buff Peepz",
                address = address,
                country = country,
                state = state,
                facilityId = 1234,
                digitalProducts = digitalProducts
            )

            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                facilitySearchResponse,
                ""
            )

            vm.onQueryChange("")
            vm.onSearchFacilityClicked()
            val uiState = vm.uiState
            assertEquals(uiState.value.error, "Facility cannot be blank")
        }

    @Test
    fun `Given an unsuccessful facility search and valid facilityId we expect uistate error to not be null`() =
        runTest(dispatcher) {
            val facilityId = "1234"

            every { ftsuModel.previewChannelsFromCore(facilityId.toInt()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            val facilitySearchResponse = null

            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                facilitySearchResponse,
                "Error"
            )

            vm.onQueryChange(facilityId)
            vm.onSearchFacilityClicked()
            val uiState = vm.uiState
            assertNotNull(uiState.value.error)
            assertEquals(uiState.value.error, "Error")
        }

    @Test
    fun `Given a successful facility search and valid facilityId we expect digital product information to be correct when they have partial ownership of all digital products`() =
        runTest(dispatcher) {
            val facilityId = "1234"
            val digitalProducts = listOf("ENGAGE_360", "ASSET_MANAGEMENT", "YMCA360")

            every { ftsuModel.previewChannelsFromCore(facilityId.toInt()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            val facilitySearchResponse = FacilitySearchResponse(
                name = "Workout Academy of Buff Peepz",
                address = address,
                country = country,
                state = state,
                facilityId = 1234,
                digitalProducts = digitalProducts
            )
            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                facilitySearchResponse,
                ""
            )

            vm.onQueryChange(facilityId)
            vm.onSearchFacilityClicked()
            val uiState = vm.uiState
            val result = uiState.value.facility?.digitalProducts
            result?.forEach {
                if (digitalProducts.contains(it.name)) {
                    assert(it.isAvailable)
                }
            }
        }

    @Test
    fun `Given a successful facility search and valid facilityId we expect digital product information to be correct when they have no ownership of all digital products`() =
        runTest(dispatcher) {
            val facilityId = "1234"
            every { ftsuModel.previewChannelsFromCore(facilityId.toInt()) } returns flowOf(
                OperationState.Success(ChannelListPreview(emptyList()))
            )
            val facilitySearchResponse = FacilitySearchResponse(
                name = "Workout Academy of Buff Peepz",
                address = address,
                country = country,
                state = state,
                facilityId = 1234,
                digitalProducts = null
            )
            coEvery { ftsuModel.searchForFacility(facilityId) } returns Pair(
                facilitySearchResponse,
                ""
            )
            vm.onQueryChange(facilityId)
            vm.onSearchFacilityClicked()
            val uiState = vm.uiState
            val result = uiState.value.facility?.digitalProducts
            result?.forEach { digitalProduct ->
                assert(!digitalProduct.isAvailable)
            }
        }

    @Test
    fun `Given an invalid search query (non numeric) we expect club id to be set to -1`() {
        vm.onQueryChange("SuperCoolFacilityId")
        verify(exactly = 0) { ftsuModel.setClubId(-1) }
    }

    @Test
    fun `Given a valid search query (non numeric) we expect club id not to be set`() {
        val validClubId = "1963" //int
        vm.onQueryChange(validClubId)
        verify(exactly = 1) { ftsuModel.setClubId(1963) }
    }
}