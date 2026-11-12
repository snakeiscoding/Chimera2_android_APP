package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import com.jht.chimera.service.ui.panels.getPanelTypeLayoutParams
import com.jht.enums.UiPanelType
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import java.util.UUID

class ModulePanel(
    context: Context,
    panelHolder: InnerPanelHolder,
    panelOperations: UiPanelOperations
) : ComposePanel(context) {
    private val panel: InnerPanelBase

    init {
        panelHolder.initializePanel(context, panelOperations)
        panel = panelHolder.getPanel()
    }

    @Composable
    override fun ThemedContent() {
        panel.PanelContent()
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        return getPanelTypeLayoutParams(context, panel.panelType)
    }

    override fun panelAdded() {
        super.panelAdded()
        panel.onPanelShown()
    }

    override fun panelRemoved() {
        super.panelRemoved()
        panel.onPanelHidden()
    }

    fun receivePanelData(panelData: BasePanelData) {
        panel.onPanelReceiveData(panelData)
    }

    override fun dispose() {
        panel.onPanelDestroyed()
        super.dispose()
    }

    fun getPanelId(): UUID {
        return panel.panelId
    }

    fun getType(): UiPanelType {
        return panel.panelType
    }
}