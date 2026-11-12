package com.jht.concierge.service

import android.os.Parcelable
import com.jht.concierge.interfaces.FacilityRequestType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class AuthResponse(
    @SerialName("token")
    val token: String,
)

/**
 * This class is used to deserialize and store the JSON data received from the Concierge API.
 */
@Serializable
data class ConciergeRequest(
    @SerialName("id")
    val id: String = "",
    @SerialName("machine_uuid")
    val machineUid: String = "",
    @SerialName("facility_id")
    val facilityId: Int = -1,
    @SerialName("machine_frame_serial_number")
    val frameSerialNumber: String? = null,
    @SerialName("machine_nickname")
    val machineNickname: String? = null,
    @SerialName("machine_modality")
    val machineModality: String? = null,
    @SerialName("request_message")
    val message: String? = null,
    @SerialName("request_state")
    val state: ConciergeRequestState = ConciergeRequestState.CLOSED,
    @SerialName("acknowledge_date")
    val acknowledgeDate: String? = null,
    @SerialName("closed_date")
    val closedDate: String? = null,
    @SerialName("acknowledge_user_id")
    val acknowledgedStaffId: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    @SerialName("request_type_id")
    val facilityRequestTypeId: String? = null,
    @SerialName("request_type")
    val facilityRequestType: FacilityRequestType? = null,
)

/**
 * This class represents the various states of a Concierge request
 */
enum class ConciergeRequestState {
    @SerialName("CLOSED") CLOSED,
    @SerialName("OPEN") OPEN,
    @SerialName("ACKNOWLEDGED") ACKNOWLEDGED
}

/**
 * This class is used to notify the UI of concierge events as they occur. Notification occurs via
 * DataMatrix, hence why the class and its subclasses are parcels
 */
@Parcelize
sealed class ConciergeEvent : Parcelable {
    @Parcelize object RequestClosed: ConciergeEvent() // Represents a concierge request being closed.
    @Parcelize object RequestOpened: ConciergeEvent() // Represents a concierge request being opened.
    @Parcelize object RequestAcknowledged: ConciergeEvent() // Represents a concierge request being acknowledged.
    @Parcelize object RequestCancelled: ConciergeEvent() // Represents a concierge request being cancelled.
    @Parcelize object RequestCompleted: ConciergeEvent() // Represents a concierge request being completed.
    @Parcelize data class Loading(val title: String): ConciergeEvent() // Represents a loading state.
    @Parcelize data class Error(val previousState: ConciergeEvent): ConciergeEvent() // Represents an error occurred.
}