package com.jht.interfaces.models.experimental

import com.jht.chimera.R
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.language.StringLookup.getString
import com.jht.concierge.interfaces.ConciergeHandler
import com.jht.concierge.interfaces.ConciergeModel
import com.jht.concierge.interfaces.FacilityConfigResponse
import com.jht.concierge.interfaces.FacilityRequestType
import com.jht.concierge.service.ConciergeEvent
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixBoolVariableListener
import com.jht.interfaces.MatrixEventListener
import com.jht.interfaces.MatrixStringValueListener
import kotlinx.serialization.json.Json

/**
 * Implements the model for accessing the concierge feature
 */
class MatrixConciergeModel : ConciergeModel() {
    private val manager: CallbackManager = CallbackManager()

    /**
     * TODO: Refactor isConciergeEnabled based on schedule response,
     * response sample -> weekday_name: [{"open": "HH:MM", "close": "HH:MM"}]
     * e.g.
     * "monday": [{"open": "08:00", "close": "17:00"}]
     */
    override val isConciergeEnabled: DataValueListener<Boolean>
        get() = MatrixBoolValueListener(manager, SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL)
    override val isConciergeAvailable: DataVariableListener<Boolean>
        get() = MatrixBoolVariableListener(manager, MainAppKeys.DATA_CONCIERGE_SERVICES_AVAILABLE_BOOL)

    override var facilityConfiguration: String
        get() = DataMatrix.getString(MainAppKeys.DATA_CONCIERGE_FACILITY_CONFIG_RESPONSE_SERIALIZED_STRING)
        set(value) { DataMatrix.putString(MainAppKeys.DATA_CONCIERGE_FACILITY_CONFIG_RESPONSE_SERIALIZED_STRING, value) }
    override var conciergeEvent: ConciergeEvent?
        get() = DataMatrix.getParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent::class.java)
        set(value) { DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, value) }
    override var ledColor: Int
        get() = DataMatrix.getInt(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT)
        set(value) { DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT, value) }

    override fun createRequest(requestId: String) {
        DataMatrix.putString(MainAppKeys.ACTION_CREATE_CONCIERGE_REQUEST_WITH_OPTIONAL_REQUEST_ID_STRING, requestId)
    }
    override val onCreateRequest: DataValueListener<String> = MatrixStringValueListener(manager, MainAppKeys.ACTION_CREATE_CONCIERGE_REQUEST_WITH_OPTIONAL_REQUEST_ID_STRING)

    override fun cancelRequest() {
        DataMatrix.sendEvent(MainAppKeys.ACTION_CANCEL_CONCIERGE_REQUEST)
    }
    override val onCancelRequest: EventListener = MatrixEventListener(manager, MainAppKeys.ACTION_CANCEL_CONCIERGE_REQUEST)

    override fun resetConciergeState() {
        DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestClosed)
    }

    override fun cancelConciergeState() {
        DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestCancelled)
    }

    override fun getFacilityConfigResponse(): FacilityConfigResponse {
        val temp = DataMatrix.getString(MainAppKeys.DATA_CONCIERGE_FACILITY_CONFIG_RESPONSE_SERIALIZED_STRING)
        return if (temp.isNotEmpty()) {
            Json.decodeFromString(temp)
        } else{
            // guaranteed one option for facility requests (general 'need assistance' with no id passed)
            FacilityConfigResponse(
                requestTypes = listOf(
                    FacilityRequestType(
                        buttonText = getString(R.string.need_assistance_tc, ContextUtil.getApplicationContext()),
                        facilityId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT),
                    )
                )
            )
        }
    }

    override fun completeConciergeState() {
        DataMatrix.putParcelable(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestCompleted)
    }

    override fun startConciergeStateHandler(conciergeHandler: ConciergeHandler) {
        manager.addParcelableListener(MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE,
            ConciergeEvent::class.java,
            { conciergeEvent ->
                when (conciergeEvent) {
                    is ConciergeEvent.Loading -> conciergeHandler.onRequestLoading()
                    is ConciergeEvent.RequestClosed -> conciergeHandler.onRequestClosed()
                    is ConciergeEvent.RequestOpened -> conciergeHandler.onRequestOpened()
                    is ConciergeEvent.RequestCancelled -> conciergeHandler.onRequestCancelled()
                    is ConciergeEvent.RequestAcknowledged -> conciergeHandler.onRequestAcknowledged()
                    is ConciergeEvent.RequestCompleted -> conciergeHandler.onRequestCompleted()
                    is ConciergeEvent.Error -> conciergeHandler.onRequestError()
                }
            },
            true
        )
    }

    override fun close() {
        manager.cancelAllListeners()
    }

}