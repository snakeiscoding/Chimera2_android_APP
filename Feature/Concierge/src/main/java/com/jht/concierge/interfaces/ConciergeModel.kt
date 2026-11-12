package com.jht.concierge.interfaces

import com.jht.concierge.service.ConciergeEvent
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar


/**
 * Provides a model for accessing the concierge feature
 */
abstract class ConciergeModel : BaseModel {

    abstract var facilityConfiguration: String
    abstract var conciergeEvent: ConciergeEvent?
    abstract var ledColor: Int

    /** Indicates if the concierge setting is enabled */
    abstract val isConciergeEnabled: DataValueListener<Boolean>

    /** Indicates if the concierge services are available at this time */
    abstract val isConciergeAvailable: DataVariableListener<Boolean>

    /** Creates a Concierge request */
    abstract fun createRequest(requestId:String)
    abstract val onCreateRequest: DataValueListener<String>

    /** Cancels a Concierge request */
    abstract fun cancelRequest()
    abstract val onCancelRequest: EventListener

    /** Resets the internal Concierge state */
    abstract fun resetConciergeState()

    /** Completes the internal Concierge state */
    abstract fun completeConciergeState()

    /** Starts Handling for a Concierge Request */
    abstract fun startConciergeStateHandler(conciergeHandler: ConciergeHandler)

    /** Cancels the internal Concierge state */
    abstract fun cancelConciergeState()
    /** Gets a list of FacilityRequestType to display pre-made concierge options to user */
    abstract fun getFacilityConfigResponse(): FacilityConfigResponse
}

/** Provides event handling for a Concierge Request */
abstract class ConciergeHandler {
    /** The current concierge request is closed */
    abstract fun onRequestClosed()

    /** The current concierge request is closed */
    abstract fun onRequestOpened()

    /** The current concierge request is acknowledged */
    abstract fun onRequestAcknowledged()

    /** The current concierge request is completed */
    abstract fun onRequestCompleted()

    /** The current concierge request is loading */
    abstract fun onRequestLoading()

    /** The current concierge request has an error */
    abstract fun onRequestError()

    /** The current concierge request is cancelled */
    abstract fun onRequestCancelled()
}

/**
 * This class is used to deserialize and store the JSON data received from the Concierge API.
 * I believe this makes the most sense here for now, since the UI needs access to this data to
 * make it easier to get all these elements to the screen.
 * The Chimera module consumes this interface model, so the back end can still properly function.
 */
@Serializable
data class FacilityRequestType(
    @SerialName("id")
    val id: String = "",
    @SerialName("facility_id")
    val facilityId: Int = -1,
    @SerialName("name")
    val name: String = "",
    @SerialName("button_text")
    val buttonText: String = "",
    @SerialName("icon_url")
    val buttonIconUrl: String? = null,
    @SerialName("priority")
    val buttonPriority: Int = -1,
    @SerialName("color")
    val ledColor: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

/**
 * Returns scheduled availability of concierge feature
 * @param active is a boolean to show if concierge is enabled or disabled
 * @param headerName Header text to display on the console
 * @param requestTypes is a list of request types available for current facility
 * @param customTextFields Optional object containing all the custom header for concierge request modal/dialog
 * @param color hex code for the onyx console's color [May change in future, because requestType also contains color for backward compatibility]
 */
@Serializable
data class FacilityConfigResponse(
    @SerialName("facility_id")
    val facilityId: Int = -1,
    @SerialName("active")
    val active: Boolean = false,
    @SerialName("header")
    val headerName: String = "",
    @SerialName("request_types")
    val requestTypes: List<FacilityRequestType> = emptyList(),
    @SerialName("custom_text")
    val customTextFields: Map<CustomTextEnum, String>? = emptyMap(),
    @SerialName("color")
    val color: String? = "",
    @SerialName("schedule")
    val availabilitySchedule: Map<WeekDays, List<TimeOpenAndCloseModel>>? = emptyMap()
)

enum class WeekDays(val calendarDoW: Int) {
    @SerialName("sunday") SUNDAY(Calendar.SUNDAY),
    @SerialName("monday") MONDAY(Calendar.MONDAY),
    @SerialName("tuesday") TUESDAY(Calendar.TUESDAY),
    @SerialName("wednesday") WEDNESDAY(Calendar.WEDNESDAY),
    @SerialName("thursday") THURSDAY(Calendar.THURSDAY),
    @SerialName("friday") FRIDAY(Calendar.FRIDAY),
    @SerialName("saturday") SATURDAY(Calendar.SATURDAY),
}

enum class CustomTextEnum {
    @SerialName("select_request_header") SELECT_REQUEST_HEADER,
    @SerialName("select_request_subtitle") SELECT_REQUEST_SUBTITLE,
    @SerialName("help_requested_header") HELP_REQUESTED_HEADER,
    @SerialName("help_requested_subtitle") HELP_REQUESTED_SUBTITLE,
    @SerialName("help_en_route_header") HELP_EN_ROUTE_HEADER,
    @SerialName("help_en_route_subtitle") HELP_EN_ROUTE_SUBTITLE
}

@Serializable
data class TimeOpenAndCloseModel(
    @SerialName("open")
    val open: String = "",
    @SerialName("close")
    val close: String = ""
)