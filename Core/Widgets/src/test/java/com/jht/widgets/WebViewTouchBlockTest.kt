package com.jht.widgets

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewTouchBlockTest {
    @Test
    fun `single pointer passes when interaction is allowed and zoom is off`() {
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP).forEach { action ->
            assertFalse(blockTouch(pointerCount = 1, actionMasked = action, zoomEnabled = false, allowUserInteraction = true))
        }
    }

    @Test
    fun `two pointers are blocked when zoom is off`() {
        listOf(MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_UP).forEach { action ->
            assertTrue(blockTouch(pointerCount = 2, actionMasked = action, zoomEnabled = false, allowUserInteraction = true))
        }
    }

    @Test
    fun `pointer down is blocked even if reported with one pointer`() {
        assertTrue(blockTouch(pointerCount = 1, actionMasked = MotionEvent.ACTION_POINTER_DOWN, zoomEnabled = false, allowUserInteraction = true))
    }

    @Test
    fun `two pointers pass when zoom is on`() {
        assertFalse(blockTouch(pointerCount = 2, actionMasked = MotionEvent.ACTION_POINTER_DOWN, zoomEnabled = true, allowUserInteraction = true))
        assertFalse(blockTouch(pointerCount = 2, actionMasked = MotionEvent.ACTION_MOVE, zoomEnabled = true, allowUserInteraction = true))
    }

    @Test
    fun `everything is blocked when interaction is not allowed`() {
        listOf(true, false).forEach { zoom ->
            assertTrue(blockTouch(pointerCount = 1, actionMasked = MotionEvent.ACTION_DOWN, zoomEnabled = zoom, allowUserInteraction = false))
            assertTrue(blockTouch(pointerCount = 2, actionMasked = MotionEvent.ACTION_MOVE, zoomEnabled = zoom, allowUserInteraction = false))
        }
    }
}
