package com.jht.mock.interfaces.models

import com.jht.enums.FrameType
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.models.UIBaseModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockStringValue

class MockUIBaseModel(
    override var frameType: FrameType = FrameType.Treadmill,
    override val isDebugBuild: Boolean = false,
    override val isDemoMode: BoolValueListener = MockBoolValue(false),
    override val isCommercial: Boolean = true,
    override val isRetail: Boolean = false,
    override var themeUri: MockStringValue = MockStringValue("preset:light"),
) : UIBaseModel(){
    override fun close() {
        //Do Nothing
    }
}
