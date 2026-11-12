package com.jht.enums

/**
 * Defines where the panel will be displayed and if it's overriding another panel. Panels
 * that are overridden will not be displayed.
 */
enum class UiPanelType {
    TopPanel, // This type will show in the slot for the top panel
    ValueUpdatePanelOverride, // This type will replace the current value update panel
}