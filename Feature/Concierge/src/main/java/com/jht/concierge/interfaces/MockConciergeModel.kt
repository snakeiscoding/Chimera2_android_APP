package com.jht.concierge.interfaces

import com.jht.concierge.service.ConciergeEvent
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockDataVariable

class MockConciergeModel(
) : ConciergeModel() {
    override val isConciergeEnabled: MockBoolValue = MockBoolValue(true)
    override val isConciergeAvailable: DataVariableListener<Boolean> = MockDataVariable(true)

    override var facilityConfiguration: String = ""
    override var conciergeEvent: ConciergeEvent? = null
    override var ledColor: Int = 0xffffff00.toInt()

    private lateinit var conciergeHandler : ConciergeHandler
    private lateinit var requestId : String

    override fun createRequest(requestId: String) {
        this.requestId = requestId
        println("mocked requestId: $requestId")
        when(requestId) {
            // corresponds to ConciergeDialogInstrumentationTests
            //"closed" -> { conciergeHandler.onRequestClosed()} // default start, no need to mock
            "opened" -> { conciergeHandler.onRequestOpened() } // 0
            "acknowledged" -> { conciergeHandler.onRequestAcknowledged() } // 1
            "completed" -> { conciergeHandler.onRequestCompleted() } // 2
            //"cancelled" -> { conciergeHandler.onRequestCancelled() } // can be reached from opened state, no need to mock
            "loading" -> { conciergeHandler.onRequestLoading() } // 3
            "error" -> { conciergeHandler.onRequestError() } // 4
        }
    }
    override val onCreateRequest: DataValueListener<String>
        get() = TODO("Not yet implemented")

    override fun cancelRequest() {
        //Do nothing (this would be the call to the system to ping the MCA endpoint to cancel the request on the web side)
    }
    override val onCancelRequest: EventListener
        get() = TODO("Not yet implemented")

    override fun resetConciergeState() {
        conciergeHandler.onRequestClosed()
    }

    override fun completeConciergeState() {
        conciergeHandler.onRequestCompleted()
    }

    override fun startConciergeStateHandler(conciergeHandler: ConciergeHandler) {
        this.conciergeHandler = conciergeHandler
    }

    override fun cancelConciergeState() {
        conciergeHandler.onRequestCancelled()
    }

    override fun getFacilityConfigResponse(): FacilityConfigResponse {
        return FacilityConfigResponse(
            requestTypes = listOf(
                FacilityRequestType(
                    id = "opened",
                    buttonPriority = 0,
                    buttonText = "Test opened Concierge Request",
                ),
                FacilityRequestType(
                    id = "acknowledged",
                    buttonPriority = 1,
                    buttonText = "Test acknowledged Concierge Request",
                ),
                FacilityRequestType(
                    id = "completed",
                    buttonPriority = 2,
                    buttonText = "Test completed Concierge Request",
                ),
                FacilityRequestType(
                    id = "loading",
                    buttonPriority = 3,
                    buttonText = "Test loading Concierge Request",
                ),
                FacilityRequestType(
                    id = "error",
                    buttonPriority = 4,
                    buttonText = "Test error Concierge Request",
                )
            )
        )
    }

    override fun close() {
        //Do nothing
    }

}