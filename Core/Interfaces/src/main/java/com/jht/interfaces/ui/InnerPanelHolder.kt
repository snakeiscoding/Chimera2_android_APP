package com.jht.interfaces.ui

import android.content.Context
import android.os.Parcelable

/**
 * Holder class that will be sent to the UI Service to create a UI panel for a module. Derived
 * classes must implement [createPanel] to create the specific UI panel that's intended.
 */
abstract class InnerPanelHolder : Parcelable {
    /** The UI service will initialize the panel once it is sent over the UI model */
    private lateinit var panelInternal: InnerPanelBase

    /**
     * Create the panel.
     */
    protected abstract fun createPanel(context: Context, panelOperations: UiPanelOperations): InnerPanelBase

    /**
     * Initializes the panel. This should only be called by the UI Service. The panel will not be
     * initialized until this is called.
     */
    fun initializePanel(context: Context, panelOperations: UiPanelOperations) {
        panelInternal = createPanel(context, panelOperations)
    }

    /**
     * Gets the panel from the holder
     */
    fun getPanel(): InnerPanelBase {
        return panelInternal
    }
}
