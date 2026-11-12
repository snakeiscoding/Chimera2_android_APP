package com.jht.clubcom.interfaces

import com.jht.interfaces.EventListener
import com.jht.interfaces.ParcelableVariableListener
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockEventListener
import com.jht.mock.interfaces.MockParcelableVariable
import com.jht.mock.interfaces.MockStringVariable


class MockClubComModel(
) : ClubComModel() {
    override var urlModelString: String = "7x"
    override val isEnabled = MockBoolValue(true)

    override fun startSession() {}
    override val onStartSession: EventListener
        get() = MockEventListener()
    override fun stopSession() {}
    override val onStopSession: EventListener
        get() = MockEventListener()

    override val options: ParcelableVariableListener<ClubComOptions>
        get() = MockParcelableVariable(ClubComOptions())

    override fun close() {}
}
