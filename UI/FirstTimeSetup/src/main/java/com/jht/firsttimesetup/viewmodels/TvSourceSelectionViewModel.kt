package com.jht.firsttimesetup.viewmodels

import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
@HiltViewModel
class TvSourceSelectionViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel
) : ThemedViewModel() {
    private val _uiState = MutableStateFlow(TvSourceSelectionUi())
    private var tvSources: List<String> = emptyList()
    private var connectionTypes: List<String> = emptyList()

    val CONNECTIONS_IPTV_INDEX  = 1

    val uiState = _uiState.asStateFlow()

    init {
        tvSources = ftsuModel.getTvSourceList()
        connectionTypes = listOf(ConnectionTypes.COAX.getDisplayName(),ConnectionTypes.IPTV.getDisplayName())
        updateUi()
    }

    fun updateUi() {
        _uiState.value = _uiState.value.copy(
            tvSources = tvSources,
            connectionTypes = connectionTypes
        )
    }

    fun onSaveSettings(tvSourceIndex: Int, connectionTypeIndex: Int) {
        val isIptvEnabled = connectionTypeIndex == CONNECTIONS_IPTV_INDEX
        _uiState.value = _uiState.value.copy(isIptvEnabled = isIptvEnabled)
        ftsuModel.setTvSourceType(tvSourceIndex)
    }

    fun setShowIpTvSettings(newValue: Boolean){
        _uiState.value = _uiState.value.copy(showIpTvSettingsDialog = newValue)
    }

}

/**
 * Ui model for Tv source selection ui
 */
data class TvSourceSelectionUi(
    val tvSources: List<String> = emptyList(),
    val connectionTypes: List<String> = emptyList(),
    val isIptvEnabled: Boolean = true,
    val showIpTvSettingsDialog: Boolean = false,
)

/**
 * Ui model for connection types
 */
enum class ConnectionTypes {
    COAX,
    IPTV
}

fun ConnectionTypes.getDisplayName() : String{
    return when(this) {
        ConnectionTypes.COAX -> "COAX"
        ConnectionTypes.IPTV -> "IPTV"
    }
}