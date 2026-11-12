package com.jht.mock.interfaces.models

import android.graphics.Rect
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockParcelableValue

class MockUIServiceModel(
    override val stateFullScreen: MockBoolValue = MockBoolValue(),
    override val stateIdle: BoolValueListener = MockBoolValue(),
    override val stateSummary: BoolValueListener = MockBoolValue(),
) : UIServiceModel() {
    override val overlayViewportPixels = MockParcelableValue(Rect())
    override fun toggleFullscreen(showFullscreenOverride: Boolean?) {
        // TODO
    }

    override fun addUiPanel(panelHolder: InnerPanelHolder) {
        // do nothing
    }

    override fun sendUiPanelData(panelData: BasePanelData) {
        // do nothing
    }


    override fun close() {
        // do nothing
    }

}