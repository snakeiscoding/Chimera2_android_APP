package com.jht.firsttimesetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles update related logic
 */
@HiltViewModel
class SoftwareUpdateViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(SoftwareUpdateUi())

    val uiState = _uiState.asStateFlow()

    private var updateTimeoutJob: Job? = null

    private var timer = 0

    private val MAX_TIME = 30

    init {
        viewModelScope.launch(ioDispatcher) {
            ftsuModel.canUpdateOtaFlow().combine(ftsuModel.softwareUpdateVersion()){ canUpdate, versionName ->
                when (canUpdate) {
                    true -> {
                        _uiState.value = _uiState.value.copy(
                            updateState = UpdateState.UPDATE_AVAILABLE,
                            softwareVersion = versionName ?: ""
                        )
                    }
                    false -> {
                        _uiState.value = _uiState.value.copy(
                            updateState = UpdateState.NO_UPDATE
                        )
                    }
                    else -> {
                        if (timer > MAX_TIME) {
                            _uiState.value = _uiState.value.copy(
                                updateState = UpdateState.NO_UPDATE
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                updateState = UpdateState.CHECK_UPDATES
                            )
                        }
                    }
                }
            }.collect()
        }

        updateTimeoutJob = viewModelScope.launch(ioDispatcher) {
            while (isActive) {
                delay(1000)
                timer++
            }
        }

        checkUpdate()
    }

    fun killModule(){
        viewModelScope.cancel()
    }

    /**
     * Checks if update is available
     */
    fun checkUpdate() {
        ftsuModel.checkForOtaUpdates()
    }

    /**
     * Downloads update in the background
     */
    fun startBackgroundDownload() {
        ftsuModel.startBackgroundDownload()
    }
}

/**
 * UI model for software update ui
 */
data class SoftwareUpdateUi(
    val updateState: UpdateState = UpdateState.CHECK_UPDATES,
    val softwareVersion: String = ""
)

/**
 * Describes states update view can be in
 */
enum class UpdateState {
    UPDATE_AVAILABLE,
    NO_UPDATE,
    CHECK_UPDATES,

}