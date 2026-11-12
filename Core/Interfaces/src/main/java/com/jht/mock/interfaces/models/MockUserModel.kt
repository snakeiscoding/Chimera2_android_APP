package com.jht.mock.interfaces.models

import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockDataListener
import com.jht.mock.interfaces.MockFloatValue
import com.jht.mock.interfaces.MockIntValue
import com.jht.mock.interfaces.MockStringValue

class MockUserModel(
    override val uuid: MockStringValue = MockStringValue(),
    override val name: MockStringValue = MockStringValue(),
    override val gender: MockIntValue = MockIntValue(),
    override val weight: MockFloatValue = MockFloatValue(),
    override val age: MockIntValue = MockIntValue(),
    override val isGuest: Boolean = true,
    override val isPTP: MockBoolValue = MockBoolValue(false),
    override val isSecaUser: Boolean = false,
    override val walletPass: MockStringValue = MockStringValue(),
    override val editUrl: MockStringValue = MockStringValue(),
    override val hasCompletedPaceSurvey: Boolean = false,
    override var jumpAccessToken: String = "",
    override var jumpIdToken: String = "",
) : UserModel() {
    override fun close() {
        // Do nothing
    }
}
