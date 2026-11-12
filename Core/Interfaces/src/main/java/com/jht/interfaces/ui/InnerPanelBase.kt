package com.jht.interfaces.ui

import androidx.compose.runtime.Composable
import com.jht.enums.UiPanelType
import java.util.UUID

/**
 * This is a base class for a compose panel that can be added from a module. Extend this class
 * and fill out the necessary functions to create a panel.
 *
 * Usage:
 * 1. Extend [InnerPanelBase] class and fill out abstract functions.
 * 2. Extend [InnerPanelHolder] and fill out the [InnerPanelHolder.createPanel] function.
 * 3. Pass the extended [InnerPanelHolder] class to the UiServiceModel add function when the panel should be active.
 *
 * In order for the panel to be displayed the panel must call show on itself. The panel will manage
 * its own visibility. All panels will be removed after a workout when the UiService panel state
 * goes to IDLE. They will need to be added again during the next workout, if needed.
 */
abstract class InnerPanelBase(
    val panelId: UUID,
    val panelType: UiPanelType,
    private val panelOperations: UiPanelOperations
) {
    /**
     * The composable content to display in a UI panel
     */
    @Composable
    abstract fun PanelContent()

    /**
     * Called when the panel is displayed. Override this if needed.
     */
    open fun onPanelShown() {}

    /**
     * Called when the panel is hidden. Override this if needed.
     */
    open fun onPanelHidden() {}

    /**
     * Called when the panel receives data through the UI Service model. Override to receive that
     * data. Cast to get data specific to a panel.
     */
    open fun onPanelReceiveData(panelData: BasePanelData) {}

    /**
     * Called when the panel is being removed and should be cleaned up
     */
    abstract fun onPanelDestroyed()

    fun show() {
        panelOperations.showPanel(panelId)
    }

    fun hide() {
        panelOperations.hidePanel(panelId)
    }

    fun update() {
        panelOperations.updatePanel(panelId)
    }
}


interface UiPanelOperations {
    fun showPanel(id: UUID)
    fun hidePanel(id: UUID)
    fun updatePanel(id: UUID)
}