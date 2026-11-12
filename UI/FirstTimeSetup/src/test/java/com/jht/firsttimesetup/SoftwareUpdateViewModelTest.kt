package com.jht.firsttimesetup

import app.cash.turbine.test
import com.jht.firsttimesetup.viewmodels.SoftwareUpdateViewModel
import com.jht.firsttimesetup.viewmodels.UpdateState
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

@OptIn(ExperimentalCoroutinesApi::class)
class SoftwareUpdateViewModelTest {

    @get:Rule
    val timeout: Timeout = Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

    private lateinit var vm: SoftwareUpdateViewModel
    private lateinit var ftsuModel: MockFirstTimeSetupModel
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()


    @Before
    fun setup() {
        ftsuModel = MockFirstTimeSetupModel()
        vm = SoftwareUpdateViewModel(
            ftsuModel,
            dispatcher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Given a non-null version name and can update via ota is true we expect ui state to be UPDATE_AVAILABLE`() =
        runTest {
            ftsuModel.setCanUpdate(true)
            ftsuModel.setVersionName("1.2.0")

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(state.updateState, UpdateState.UPDATE_AVAILABLE)
                assertEquals(state.softwareVersion, "1.2.0")
            }

            vm.killModule()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Given a null version name and can update via ota is true we expect ui state to be UPDATE_AVAILABLE`() =
        runTest {
            ftsuModel.setCanUpdate(true)
            ftsuModel.setVersionName(null)

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(UpdateState.UPDATE_AVAILABLE, state.updateState)
                assertEquals("", state.softwareVersion)
            }

            vm.killModule()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Given a non null version name and can update via ota is false we expect ui state to be NO_UPDATE`() {
        runTest {
            ftsuModel.setCanUpdate(false)
            ftsuModel.setVersionName("1.2.0")

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(state.updateState, UpdateState.NO_UPDATE)
            }

            vm.killModule()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Given a null version name and can update via ota is null we expect ui state to be CHECK_UPDATES`() {
        runTest {
            ftsuModel.setCanUpdate(null)
            ftsuModel.setVersionName(null)

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(state.updateState, UpdateState.CHECK_UPDATES)
            }

            vm.killModule()
        }
    }
}