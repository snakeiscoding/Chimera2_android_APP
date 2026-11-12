package com.jht.firsttimesetup.viewmodels

import android.net.wifi.WifiManager
import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.interfaces.models.SystemModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Viewmodel handles all logic necessary for changing network
 * settings such as wired and wireless connections
 */
@HiltViewModel
internal class NetworkSelectViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val wifiManager: WifiManager,
    private val systemModel: SystemModel
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(NetworkSelectUiState())

    val uiState = _uiState.asStateFlow()

    //job used to poll for network names
    private var job: Job? = null

    /**
     * Polls for updates to Wired and Wireless connection
     * in order to update ui
     */
    fun onViewStarted() {
        job = viewModelScope.launch(ioDispatcher) {
            while (isActive) {
                delay(1000)
                updateSSIDName()
            }
        }
    }

    /**
     * Used to cancel job once view is gone
     */
    fun onViewDismissed() {
        job?.cancel()
    }

    /**
     * Updates wireless network name and ethernet ipaddress
     */
    private fun updateSSIDName() {
        _uiState.value = _uiState.value.copy(
            wirelessNetworkName = systemModel.wirelessSSID,
            wiredIpAddress = systemModel.ethernetIpAddress
        )

    }

    /**
     * Sets url for bandwidth test
     * @param url string for the url used for bandwidth test
     */
    fun setSpeedTestUrl(url: String) {
        _uiState.value = _uiState.value.copy(speedTestUrl = url)
        ftsuModel.setSpeedTestUrl(url)
    }

    /**
     * Sends event to show webview for bandwidth test
     * current url overrides and sets url for consistency
     * with the default value
     */
    fun onTestInternetClicked() {
        val currentUrl = _uiState.value.speedTestUrl
        ftsuModel.setSpeedTestUrl(currentUrl) // handles case where url was not reset
        ftsuModel.sendBandWidthTestEvent()
    }

    /**
     * Sets [State] of view
     */
    fun setState(state: State) {
        updateSSIDName()
        _uiState.value = _uiState.value.copy(state = state)
    }

    /**
     * Launches coroutine to export wireless settings
     */
    fun exportWirelessSettings() {
        _uiState.value = _uiState.value.copy(state = State.LOADING)
        viewModelScope.launch(ioDispatcher) {
            val isSuccess = ftsuModel.exportWirelessSettings()
            _uiState.value = _uiState.value.copy(state = if (isSuccess) State.EXPORT_SUCCESS else State.EXPORT_FAILURE)
        }
    }

    /**
     * Launches a coroutine to import wireless settings
     */
    fun importWirelessSettings() {
        _uiState.value = _uiState.value.copy(state = State.LOADING)
        viewModelScope.launch(ioDispatcher) {
            val isSuccess = ftsuModel.importWirelessSettings()
            if (isSuccess) {
                val networkName = systemModel.wirelessSSID
                wifiManager.isWifiEnabled = false
                delay(2000)
                wifiManager.isWifiEnabled = true
                _uiState.value = _uiState.value.copy(
                    state = State.IMPORT_SUCCESS,
                    wirelessNetworkName = networkName
                )
            } else {
                _uiState.value = _uiState.value.copy(state = State.IMPORT_FAILURE)
            }
        }
    }
}

/**
 * Data class for Network select UI
 */
internal data class NetworkSelectUiState(
    val state: State = State.DEFAULT,
    val wirelessNetworkName: String = "<unknown ssid>",
    val wiredIpAddress: String = "",
    val speedTestUrl: String = "https://fast.com/"
)

/**
 * State for Network select
 */
internal enum class State {
    DEFAULT,
    LOADING,
    EXPORT_SUCCESS,
    EXPORT_FAILURE,
    IMPORT_SUCCESS,
    IMPORT_FAILURE,
}