package com.jht.firsttimesetup

import app.cash.turbine.test
import com.jht.data.OperationState
import com.jht.firsttimesetup.viewmodels.TVSetupState
import com.jht.firsttimesetup.viewmodels.TvSetupViewModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TvSetupViewModelTest {

    private lateinit var vm: TvSetupViewModel

    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        vm = TvSetupViewModel(
            ftsuModel,
            dispatcher
        )
    }


    @Test
    fun `Given Error state for setState() we expect ui state to be ERROR`() {
        vm.setState(TVSetupState.ERROR)
        val uiState = vm.uiState
        assert(uiState.value.state == TVSetupState.ERROR)
    }

    @Test
    fun `Given TV_SOURCE state for setState() we expect ui state to be TV_SOURCE`() {
        vm.setState(TVSetupState.TV_SOURCE)
        val uiState = vm.uiState
        assert(uiState.value.state == TVSetupState.TV_SOURCE)
    }

    @Test
    fun `Given CUSTOMIZE_CHANNELS state for setState() we expect ui state to be CUSTOMIZE_CHANNELS`() {
        vm.setState(TVSetupState.CUSTOMIZE_CHANNELS)
        val uiState = vm.uiState
        assert(uiState.value.state == TVSetupState.CUSTOMIZE_CHANNELS)
    }

    @Test
    fun `Given IMPORT_CHANNELS state for setState() we expect ui state to be IMPORT_CHANNELS`() {
        vm.setState(TVSetupState.IMPORT_CHANNELS)
        val uiState = vm.uiState
        assert(uiState.value.state == TVSetupState.IMPORT_CHANNELS)
    }

    @Test
    fun `Given  onImportChannelsClicked and Core success we expect download state to be true`() =
        runTest(dispatcher) {
            every { ftsuModel.importChannelsFromCore() } returns flowOf(OperationState.Success(Unit))
            vm.onImportChannelsClicked()
            vm.downloadState.test {
                val state = awaitItem()
                assert(state == true) // should be true when import from core is successful
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given  onImportChannelsClicked and Core error we expect download state to be false`() =
        runTest(dispatcher) {
            every { ftsuModel.importChannelsFromCore() } returns flowOf(
                OperationState.Error(
                    Exception()
                )
            )
            vm.onImportChannelsClicked()
            vm.downloadState.test {
                val state = awaitItem()
                assert(state == false) // should be true when import from core is successful
                cancelAndIgnoreRemainingEvents()
            }
        }
}