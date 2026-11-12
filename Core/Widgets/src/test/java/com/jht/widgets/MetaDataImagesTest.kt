package com.jht.widgets

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MetaDataImagesTest {
    private fun resource(name: String): java.io.InputStream =
        requireNotNull(javaClass.getResourceAsStream(name)) { "missing test resource $name" }

    @Test
    fun readsMwgRegionCenter() {
        // You can set XMP regions using ExifToolGUI or other tools
        val focal = resource("/bg_custom_region.jpg").use { readFocalFromRegions(it) }
        assertNotNull("expected MWG region in fixture", focal)
        assertEquals(0.6569f, focal!!.x, 0.001f)
        assertEquals(0.2335f, focal.y, 0.001f)
    }

    @Test
    fun returnsNullWithoutMetaTags() {
        // A plain JPEG with no meta data at src/test/resources/bg_custom_no_meta.jpg
        assertNull(resource("/bg_custom_no_meta.jpg").use { readFocalFromRegions(it) })
    }


    private val ltr = LayoutDirection.Ltr
    private val tall = IntSize(500, 1000)  // portrait image cropped into a square
    private val wide = IntSize(1000, 500)  // landscape image cropped into a square
    private val box  = IntSize(500, 500)   // viewport

    @Test fun centersFocalWhenRoomToScroll() =
        assertEquals(IntOffset(0, -250), ClampedFocalAlignment(0.5f, 0.5f).align(tall, box, ltr))

    @Test fun clampsAtTopEdge() =
        assertEquals(IntOffset(0, 0), ClampedFocalAlignment(0.5f, 0f).align(tall, box, ltr))

    @Test fun clampsAtBottomEdge() =
        assertEquals(IntOffset(0, -500), ClampedFocalAlignment(0.5f, 1f).align(tall, box, ltr))

    @Test fun clampsAtRightEdge() =
        assertEquals(IntOffset(-500, 0), ClampedFocalAlignment(1f, 0.5f).align(wide, box, ltr))
}
