package com.jht.firsttimesetup

import android.net.wifi.WifiManager
import app.cash.turbine.test
import com.jht.firsttimesetup.viewmodels.NetworkSelectViewModel
import com.jht.firsttimesetup.viewmodels.State
import com.jht.interfaces.models.SystemModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnitParamsRunner::class)
class NetworkSelectViewModelTest {

    private lateinit var vm: NetworkSelectViewModel

    @MockK
    private lateinit var ftsuModel: MockFirstTimeSetupModel
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    @MockK
    private lateinit var systemModel: SystemModel
    @MockK
    private lateinit var wifiManager: WifiManager


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { wifiManager.setWifiEnabled(false) } answers { false }
        every { wifiManager.setWifiEnabled(true) } answers { true }

        vm = NetworkSelectViewModel(
            ftsuModel = ftsuModel,
            ioDispatcher = dispatcher,
            wifiManager = wifiManager,
            systemModel = systemModel

        )
    }

    @Test
    fun `initial viewmodel state`() = runTest {
        vm.uiState.test {
            val uistate = awaitItem()
            assertEquals(uistate.state, State.DEFAULT)
            assertEquals(uistate.speedTestUrl, "https://fast.com/")
            assertEquals(uistate.wirelessNetworkName, "<unknown ssid>")
            assertEquals(uistate.wiredIpAddress, "")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful import wireless`() = runTest(dispatcher) {
        val networkName = "SuperCoolWifi"
        every { systemModel.wirelessSSID } returns networkName
        every { ftsuModel.importWirelessSettings() } returns true
        vm.uiState.test {
            awaitItem() //initial state
            vm.importWirelessSettings()
            val loadingState = awaitItem()
            assertEquals(State.LOADING, loadingState.state)
            val successState = awaitItem()
            assertEquals(State.IMPORT_SUCCESS, successState.state)
            assertEquals(networkName, successState.wirelessNetworkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unsuccessful import wireless`() = runTest(dispatcher) {
        every { ftsuModel.importWirelessSettings() } returns false
        vm.uiState.test {
            awaitItem() //initial state
            vm.importWirelessSettings()
            val loadingState = awaitItem()
            assertEquals(State.LOADING, loadingState.state)
            val errorState = awaitItem()
            assertEquals(State.IMPORT_FAILURE, errorState.state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unsuccessful export wireless`() = runTest(dispatcher) {
        every { ftsuModel.exportWirelessSettings() } returns false
        vm.uiState.test {
            awaitItem() //initial state
            vm.exportWirelessSettings()
            val loadingState = awaitItem()
            assertEquals(State.LOADING, loadingState.state)
            val errorState = awaitItem()
            assertEquals(State.EXPORT_FAILURE, errorState.state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful export wireless`() = runTest(dispatcher) {
        every { ftsuModel.exportWirelessSettings() } returns true
        vm.uiState.test {
            awaitItem() //initial state
            vm.exportWirelessSettings()
            val loadingState = awaitItem()
            assertEquals(State.LOADING, loadingState.state)
            val errorState = awaitItem()
            assertEquals(State.EXPORT_SUCCESS, errorState.state)
            cancelAndIgnoreRemainingEvents()
        }
    }
}