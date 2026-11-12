package com.jht.firsttimesetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.data.OperationState
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Viewmodel that handles navigation between Tv setup views via
 * [TVSetupState]
 */
@HiltViewModel
class TvSetupViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    @IODispatcher
    private val dispatcher: CoroutineDispatcher
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(TvSetupUi())

    private val importFromUsbChanel = Channel<Boolean?>(Channel.BUFFERED)

    val uiState = _uiState.asStateFlow()

    val importState = importFromUsbChanel.receiveAsFlow()

    private val _downloadState = Channel<Boolean?>(Channel.BUFFERED)
    val downloadState = _downloadState.receiveAsFlow()

    fun setState(state: TVSetupState) {
        _uiState.value = _uiState.value.copy(
            state = state
        )
    }

    /**
     * Collects import channel state and sends success/error
     * via [downloadState]
     */
    fun onImportChannelsClicked() {
        viewModelScope.launch(dispatcher) {
            ftsuModel.importChannelsFromCore().collect { state ->
                when(state) {
                    is OperationState.Error -> { _downloadState.send(false) }
                    OperationState.Loading -> {}
                    is OperationState.Success<*> -> { _downloadState.send(true)}
                }
            }
        }
    }

    /**
     * Handles import from USB functionality
     * Sends update success/error update via [importFromUsbChanel]
     */
    fun onImportFromUsb() {
        ftsuModel.importChannelsFromUsb(

            onFinished = {
                viewModelScope.launch(dispatcher) {
                    importFromUsbChanel.send(true)
                }
            },
            onError = {
                viewModelScope.launch(dispatcher) {
                    importFromUsbChanel.send(false)
                }
            }
        )
    }

    override fun destroy() {
        importFromUsbChanel.close()
        _downloadState.close()
    }


}

data class TvSetupUi(
    val state: TVSetupState = TVSetupState.TV_SOURCE,
)

enum class TVSetupState {
    TV_SOURCE, IMPORT_CHANNELS, CUSTOMIZE_CHANNELS, ERROR
}