package com.jht.widgets

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adobe.internal.xmp.XMPException
import com.adobe.internal.xmp.XMPMeta
import com.adobe.internal.xmp.XMPMetaFactory
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.xmp.XmpDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/*
    This file contains components that leverage image meta data to perform functions.
 */

private const val TAG = "MetaDataImages"

/**
 * Normalized focal point in [0,1] image-relative coordinates.
 * (0,0) = top-left, (0.5,0.5) = center, (1,1) = bottom-right.
 */
internal data class FocalPoint(val x: Float, val y: Float)

/**
 * XMPCore reads strictly by namespace URI, so this must match byte-for-byte.
 */
private const val NS_MWG_RS  = "http://www.metadataworkinggroup.com/schemas/regions/"
private const val NS_ST_AREA = "http://ns.adobe.com/xmp/sType/Area#"

/** Process-wide cache so we parse each file's metadata at most once. Limit max size of cache. */
private val focalCache = LruCache<Uri, FocalPoint>(512)

/** Sentinel cached for files we've checked that have no focal point. */
private val NO_FOCAL = FocalPoint(Float.NaN, Float.NaN)

/** URI schemes for local resource */
private val LOCAL_SCHEMES = setOf("content", "file", "android.resource")

/**
 * Attempts to find the content focus from XMP metadata. Return null if not found.
 *
 * Streams metadata straight from the input stream rather than buffering the
 * whole file: for JPEGs, XMP lives in an APP1 segment near the front, and the
 * reader stops at the image-scan data, so only the header region is read.
 * Blocking I/O — call off the main thread.
 */
internal fun readFocalFromRegions(input: InputStream): FocalPoint? {
    val meta = ImageMetadataReader.readMetadata(input)

    return meta.getDirectoriesOfType(XmpDirectory::class.java).firstNotNullOfOrNull { dir ->
        val xmpMeta = dir.xmpMeta ?: return@firstNotNullOfOrNull null
        if (!xmpMeta.doesPropertyExist(NS_MWG_RS, "Regions")) return@firstNotNullOfOrNull null

        // get a list of regions from meta data
        val regions = XmpMetaRegion.extract(xmpMeta)

        // Try "Focus", then "Face", fallback to first found
        regions.firstOrNull { it.type.equals("Focus", true) && it.point != null }?.point
            ?: regions.firstOrNull { it.type.equals("Face", true) && it.point != null }?.point
            ?: regions.firstOrNull { it.point != null }?.point
    }
}

/** Stores meta data for an image region */
private class XmpMetaRegion(
    val type: String? = null,
    //val name: String? = null,
    //val description: String? = null,
    val point: FocalPoint? = null,
) {
    companion object {
        private val mwg: String
        private val area: String
        init {
            // Pin the prefixes path expressions will use.
            val registry = XMPMetaFactory.getSchemaRegistry()
            mwg = registry.registerNamespace(NS_MWG_RS, "mwg-rs:")
            area = registry.registerNamespace(NS_ST_AREA, "stArea:")
        }
        private val regionList = "Regions/${mwg}RegionList"
        /** Extract list of region data */
        fun extract(xmpMeta: XMPMeta): List<XmpMetaRegion> {
            val count = xmpMeta.countArrayItems(NS_MWG_RS, regionList)
            val regions = (1..count).map { i ->
                extract(xmpMeta, i)
            }
            return regions
        }
        /** Extract region data at a given one-based index */
        fun extract(xmpMeta: XMPMeta, i: Int): XmpMetaRegion {
            val regionPath = "$regionList[$i]/"
            val x = xmpMeta.getStringOrNull(regionPath, "${mwg}Area/${area}x")?.toFloatOrNull()
            val y = xmpMeta.getStringOrNull(regionPath, "${mwg}Area/${area}y")?.toFloatOrNull()
            val point = if (x != null && y != null) FocalPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)) else null
            return XmpMetaRegion(
                type = xmpMeta.getStringOrNull(regionPath, "${mwg}Type"),
                //name = xmpMeta.getStringOrNull(regionPath, "${mwg}Name"),
                //description = xmpMeta.getStringOrNull(regionPath, "${mwg}Description"),
                point = point,
            )
        }
        private fun XMPMeta.getStringOrNull(path: String, property: String) = try {
            getPropertyString(NS_MWG_RS, path + property)
        } catch (_: XMPException) {
            null
        }
    }
}

/** parse focal point from Uri, return null if not found */
private fun parseFocal(context: Context, uri: Uri): FocalPoint? {
    // contentResolver only handles local schemes; bail for http/https/etc.
    if (uri.scheme !in LOCAL_SCHEMES) return null
    return try {
        context.contentResolver.openInputStream(uri)?.use { readFocalFromRegions(it) }
    } catch (e: Exception) {
        Log.w(TAG, "Focal parse failed for $uri", e)
        null
    }
}

/**
 * Custom [Alignment] for use with [ContentScale.Crop]. Crop scales the image so
 * its smaller relative dimension fills the viewport; this then slides the
 * overflowing dimension so the focal point sits as close to viewport center as
 * possible, clamped so no blank background is ever revealed.
 * Parameters are normalized such that (0,0) = top-left, (0.5,0.5) = center, (1,1) = bottom-right.
 */
data class ClampedFocalAlignment(
    private val focalX: Float,
    private val focalY: Float
) : Alignment {
    override fun align(
        size: IntSize,   // total scaled size of the (cropped) image
        space: IntSize,  // physical viewport bounds
        layoutDirection: LayoutDirection
    ): IntOffset {
        // How much excess image exists to scroll through on each axis.
        val maxHorizontalScroll = (size.width - space.width).coerceAtLeast(0)
        val maxVerticalScroll = (size.height - space.height).coerceAtLeast(0)

        // Focal point located in scaled-image pixel space.
        val focalPixelX = size.width * focalX
        val focalPixelY = size.height * focalY

        // Ideal scroll to center the focal point in the viewport.
        val idealLeft = focalPixelX - (space.width / 2f)
        val idealTop = focalPixelY - (space.height / 2f)

        // Clamp so we never reveal blank background. Moving the image left/up
        // requires negative offsets relative to the viewport's top-left.
        val clampedLeft = idealLeft.coerceIn(0f, maxHorizontalScroll.toFloat())
        val clampedTop = idealTop.coerceIn(0f, maxVerticalScroll.toFloat())

        return IntOffset(
            x = -clampedLeft.toInt(),
            y = -clampedTop.toInt()
        )
    }
}

/**
 * Self-contained focal-point-aware image. Crops to fill [modifier]'s bounds
 * while keeping the image's focal point as close to viewport center as
 * possible (clamped so no blank space shows).
 *
 * No ImageLoader/interceptor setup required: the XMP focal point is parsed off
 * the main thread in a side effect (header-only read), cached per file, and
 * applied when ready. Center-crops until then, and for any image without
 * metadata (incl. remote images). Add `.crossfade(true)` to your
 * ImageLoader if the re-align is noticeable.
 *
 * Note: focal parsing only works for local URIs (content://, file://,
 * android.resource://). Remote images simply center-crop.
 */
@Composable
fun FocusCropImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val uri = model as? Uri
    var alignment by remember(model) {
        val cached = uri?.let { focalCache[it]?.takeIf { focal -> focal !== NO_FOCAL } }
        mutableStateOf(
            if (cached != null) ClampedFocalAlignment(cached.x, cached.y) else Alignment.Center
        )
    }

    LaunchedEffect(model) {
        if (uri == null) return@LaunchedEffect
        val cached = focalCache[uri]
        val focal = when {
            cached === NO_FOCAL -> null
            cached != null -> cached
            else -> withContext(Dispatchers.IO) { parseFocal(context, uri) }
                .also { focalCache.put(uri, it ?: NO_FOCAL) }
        }
        if (focal != null) {
            alignment = ClampedFocalAlignment(focal.x, focal.y)
        }
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        alignment = alignment,
        modifier = modifier
    )
}


@Preview(showBackground = true, widthDp = 520)
@Composable
private fun PreviewFocalCrop() {
    val painter = painterResource(R.drawable.ic_apple_watch)

    // XMP isn't read in preview, so hardcode focal points to eyeball the
    // crop and the clamping at the extremes.
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "top-left" to FocalPoint(0f, 0f),
            "center" to FocalPoint(0.5f, 0.5f),
            "bottom-right" to FocalPoint(1f, 1f),
        ).forEach { (label, fp) ->
            Text(label)
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = ClampedFocalAlignment(fp.x, fp.y),
                modifier = Modifier.size(160.dp),
            )
        }
    }
}
