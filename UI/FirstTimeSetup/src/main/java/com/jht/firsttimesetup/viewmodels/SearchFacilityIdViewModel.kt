package com.jht.firsttimesetup.viewmodels

import androidx.lifecycle.viewModelScope
import com.jht.annotations.IODispatcher
import com.jht.data.ChannelListPreview
import com.jht.data.OperationState
import com.jht.firsttimesetup.FacilitySearchModelMapper
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.StringFormatter
import com.jht.interfaces.models.SystemModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Viewmodel for facility ID searching
 */
@HiltViewModel
class SearchFacilityIdViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    private val systemModel: SystemModel,
) : ThemedViewModel() {


    override fun stop() {
        systemModel.close()
    }

    private val _uiState = MutableStateFlow(SearchFacilityState())
    private var previousUiState: SearchFacilityState? = null

    val uiState = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(facilityIdSearchQuery = "")

        systemModel.isInternetAvailable.listenAndTrigger { isConnected ->
            _uiState.value = _uiState.value.copy(isConnected = isConnected)
        }
    }

    /**
     * Sets current [FacilityIdState] for view
     */
    fun setState(state: FacilityIdState) {
        _uiState.value = _uiState.value.copy(
            state = state
        )
    }

    /**
     * Sets the previous [SearchFacilityState]
     */
    fun setPreviousUiState(state: SearchFacilityState) {
        previousUiState = state
    }

    /**
     * Undoes current [FacilityIdState] for view
     */
    fun restorePreviousState() {
        previousUiState?.let {
            _uiState.value = it
        }
    }



    /**
     * Validates whether facility ID is valid and if so makes network call
     * to find it. If it's not valid then an error state is updated
     */
    fun onSearchFacilityClicked() {
        val facilityId = _uiState.value.facilityIdSearchQuery
        val isValidFacility = facilityId.isNotEmpty()
        if (isValidFacility) {
            onSearchFacility(facilityId)
        } else {
            _uiState.value = _uiState.value.copy(
                //TODO CGC-20594 this should likely be a resource rather than string
                error = "Facility cannot be blank"
            )
        }
    }

    /**
     * Sets facility id based on user's
     * searchQuery if it's a valid [Int]
     */
    fun setFacilityId() {
        val searchQuery = _uiState.value.facilityIdSearchQuery
        val clubId = searchQuery.toIntOrNull() ?: -1

        ftsuModel.setClubId(clubId)
    }

    /**
     * Makes call to search for facility and toggles ui state to trigger
     * loading and error states. Also fetches channel preview from CORE.
     */
    private fun onSearchFacility(facilityId: String) {
        _uiState.value = _uiState.value.copy(
            state = FacilityIdState.LOADING,
        )

        viewModelScope.launch(ioDispatcher) {
            val response = ftsuModel.searchForFacility(facilityId)
            response.first?.let {
                var facility = FacilitySearchModelMapper.mapFacilitySearchToUIModel(it, ftsuModel.getIanaTimeZoneList())

                // Fetch channel preview if facility found
                if (facility != null) {
                    val clubId = facilityId.toIntOrNull()
                    if (clubId != null) {
                        // Wait for the first non-Loading state (Success or Error)
                        val previewState = ftsuModel.previewChannelsFromCore(clubId)
                            .first { it !is OperationState.Loading }

                        if (previewState is OperationState.Success) {
                            facility = facility.copy(channelPreview = previewState.data)
                        }
                        // If Error, channelPreview remains null and UI shows "--"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    state = if (facility == null) FacilityIdState.ERROR else FacilityIdState.CONFIRM,
                    facility = facility,
                    error = null
                )
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    state = FacilityIdState.ERROR,
                    facility = null,
                    error = response.second
                )
            }
        }
    }

    /**
     * Updates facilityID for uistate to consume
     */
    fun onQueryChange(facilityId: String) {
        _uiState.value = _uiState.value.copy(
            facilityIdSearchQuery = facilityId,
            facility = null,
        )
        val facilityIdInt = facilityId.toIntOrNull()
        facilityIdInt?.let {
            ftsuModel.setClubId(it)
        }
    }
}

/**
 * UI state for facility search
 */
data class SearchFacilityState(
    val state: FacilityIdState = FacilityIdState.SEARCH,
    val error: String? = null,
    val facility: Facility? = null,
    val facilityIdSearchQuery: String = "",
    val isConnected: Boolean = false
)

/**
 * @property timeZoneIndex index into the console's time zone list. Kept apart from
 * [systemSettings] because the web sends the time zone at facility level and
 * `systemSettings` is optional.
 */
data class Facility(
    val facilityId: String,
    val name: String,
    val location: FacilityAddress?,
    val digitalProducts: List<DigitalProduct>,
    val timeZoneIndex: Int,
    val systemSettings: SystemSettingsUiModel?,
    val channelPreview: ChannelListPreview = ChannelListPreview.NONE
)

data class FacilityAddress(
    val street1: String,
    val street2: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val countryIso: String,
    val countryIso3: String,
    val state: String,
    val stateIso: String,
    val shortAddress: String = StringFormatter.getShortAddress(
        city = city,
        state = state,
        country = country
    ),
    val longAddress: String = StringFormatter.getLongAddress(
        city = city,
        street1 = street1,
        postalCode = postalCode,
        state = state,
        country = country
    )
)

@Serializable
data class DigitalProduct(
    val name: String,
    val isAvailable: Boolean
){
    override fun equals(other: Any?): Boolean {
        return (other as? DigitalProduct)?.name == name && other.isAvailable == isAvailable
    }

    override fun hashCode(): Int {
        var result = isAvailable.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

/**
 * Model for system settings
 */
data class SystemSettingsUiModel(
    val colorLedDefault: String,
    val timeFormatIs24: Boolean,
    val dateIndex: Int,
    val unitFormatIsMetric: Boolean
)

/**
 * State facility search can be in
 */
enum class FacilityIdState {
    SEARCH,
    LOADING,
    ERROR,
    CONFIRM,
    DETAIL,
    NO_INTERNET
}