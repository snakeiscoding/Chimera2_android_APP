package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewZoomSettingsTest {
    @Test
    fun `default options turn zoom and both controls off`() {
        assertEquals(
            WebViewZoomSettings(supportZoom = false, builtInZoomControls = false, displayZoomControls = false),
            WebViewWidgetOptions().zoomSettings(),
        )
    }

    @Test
    fun `zoom disabled turns off zoom and both controls`() {
        assertEquals(
            WebViewZoomSettings(supportZoom = false, builtInZoomControls = false, displayZoomControls = false),
            WebViewWidgetOptions(zoomEnabled = false).zoomSettings(),
        )
    }
}
