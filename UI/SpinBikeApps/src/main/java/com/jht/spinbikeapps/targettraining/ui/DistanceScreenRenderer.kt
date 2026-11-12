package com.jht.spinbikeapps.targettraining.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random


private const val LABEL_EVERY = 0.2f

private const val STROKE_WIDTH_PX = 64f
private const val BORDER_WIDTH_PX = 5f
private val ROW_FILL_COLOR = Color(0xFF191919)
internal const val SEGMENT_DISTANCE = 0.05f

internal const val MAX_SEGMENTS = 80
internal data class Segment(val x: Float, val distance: Float)

/** Result of walking the segment list once: the two polylines plus the
 *  pair of segments the rider marker sits between.  */
internal class TrackPaths(
    val grayPath: Path,
    val traveledPath: Path,
    val beginSeg: Segment?,
    val endSeg: Segment?
)

internal fun buildTrackPaths(
    segments: List<Segment>,
    yForSegment: (Float) -> Float,
    riderYOffset: Float,
    canvasHeight: Float,
    yInc: Float
): TrackPaths {
    val grayPath = Path()
    val traveledPath = Path()
    var grayStarted = false
    var traveledStarted = false

    var beginSeg: Segment? = null
    var endSeg: Segment? = null
    var captureRider = false

    segments.forEach { seg ->
        val y = yForSegment(seg.distance)
        if (y > canvasHeight + yInc + 1) {
            return@forEach // off bottom
        }

        if (!grayStarted) { grayPath.moveTo(seg.x, y); grayStarted = true }
        else grayPath.lineTo(seg.x, y)

        if (y > riderYOffset) {
            if (!traveledStarted) { traveledPath.moveTo(seg.x, y); traveledStarted = true }
            else traveledPath.lineTo(seg.x, y)
        }

        if (y > riderYOffset && !captureRider) {
            beginSeg = seg
        } else if (y <= riderYOffset && !captureRider) {
            captureRider = true
            endSeg = seg
        }
    }

    return TrackPaths(grayPath, traveledPath, beginSeg, endSeg)
}

/**
 * Owns the ping-pong track-segment generation that used to live as four
 * local closures inside DistanceScreen (createNextX / addNextTrack /
 * ensureSegments + the raw vars they shared).
 */
internal class SegmentTrackState(private val canvasWidthProvider: () -> Float) {
    val segments = mutableStateListOf<Segment>()
    var trackMaxDistance by mutableStateOf(-SEGMENT_DISTANCE)
    var pingPong by mutableStateOf(true)
    var lastX by mutableStateOf(0f) // initialised properly once size is known

    private fun mapWorkingWidth() = canvasWidthProvider() / 3f

    private fun createNextX(last: Float): Float {
        val mw = mapWorkingWidth()
        if (mw <= 0f) {
            return canvasWidthProvider() / 2f
        }
        var next = Random.nextInt((mw - 21f).toInt().coerceAtLeast(1)) + 10f
        if (abs(last - next) < mw / 5f) {
            next = Random.nextInt((mw - 21f).toInt().coerceAtLeast(1)) + 14f
        }
        return next + canvasWidthProvider() / 4f
    }

    fun addNextTrack() {
        if (pingPong) {
            lastX = createNextX(lastX)
        }
        pingPong = !pingPong
        trackMaxDistance += SEGMENT_DISTANCE
        segments.add(Segment(lastX, trackMaxDistance))
    }

    fun ensureSegments(currentDistance: Float) {
        if (segments.isEmpty()) {
            trackMaxDistance = -SEGMENT_DISTANCE
            val seedSegments = (currentDistance / SEGMENT_DISTANCE).toInt()
            trackMaxDistance = seedSegments * SEGMENT_DISTANCE - SEGMENT_DISTANCE
            while (segments.size < MAX_SEGMENTS) addNextTrack()
        }
    }
}


/** Finds the pair of segments a given y-coordinate falls between, used both
 *  for the rider marker (via buildTrackPaths) and the target marker below. */
internal fun findClampBounds(
    segments: List<Segment>,
    yForSegment: (Float) -> Float,
    clampedY: Float
): Pair<Segment?, Segment?> {
    var begin: Segment? = null
    var end: Segment? = null
    var captured = false
    segments.forEach { seg ->
        val y = yForSegment(seg.distance)
        if (y > clampedY && !captured) {
            begin = seg
        } else if (y <= clampedY && !captured) {
            captured = true
            end = seg
        }
    }
    return begin to end
}

/** Draws each alternating map "row" behind the track, plus its border and
 *  top edge highlight, for every currently-visible segment. */
internal fun DrawScope.drawSegmentRows(
    segments: List<Segment>,
    yForSegment: (Float) -> Float,
    yInc: Float,
    canvasHeight: Float
) {
    segments.forEach { seg ->
        drawSegmentRow(seg, yForSegment, yInc, canvasHeight)
    }
}

internal fun DrawScope.drawSegmentRow(
    seg: Segment,
    yForSegment: (Float) -> Float,
    yInc: Float,
    canvasHeight: Float
) {
    val y = yForSegment(seg.distance)
    val nextY = y - yInc

    if (nextY >= canvasHeight || y <= 0f) {
        return
    }

    val rowTop = nextY.coerceAtLeast(0f)
    val rowBottom = y.coerceAtMost(canvasHeight)

    drawRect(
        color = ROW_FILL_COLOR,
        topLeft = Offset(0f, rowTop),
        size = Size(size.width, rowBottom - rowTop)
    )

    drawRect(
        color = Color.Black,
        topLeft = Offset(0f, rowTop),
        style = Stroke(width = BORDER_WIDTH_PX),
        size = Size(size.width, rowBottom - rowTop)
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(ROW_FILL_COLOR, Color.Transparent),
            startY = rowTop + 2f,
            endY = rowTop + 8f
        ),
        topLeft = Offset(0f, rowTop),
        size = Size(size.width, rowBottom - rowTop),
    )

    if (y in 0f..canvasHeight) {
        drawLine(
            color = Color(0xFF393939),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2f
        )
    }
}

internal fun DrawScope.drawTrackShadow(grayPath: Path) {
    repeat(3) { i ->
        val spread = i * 4f
        val alpha = 0.12f - i * 0.03f
        drawPath(
            path = grayPath,
            color = Color.Black.copy(alpha = alpha),
            style = Stroke(width = STROKE_WIDTH_PX + spread, cap = StrokeCap.Round)
        )
    }
}

internal fun DrawScope.drawGrayTrack(grayPath: Path) {
    drawPath(
        path = grayPath,
        color = Color(0xFF5C5C5C),
        style = Stroke(width = STROKE_WIDTH_PX, cap = StrokeCap.Round)
    )
}

internal fun DrawScope.drawTraveledTrack(traveledPath: Path, riderYOffset: Float, yInc: Float) {
    drawPath(
        path = traveledPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFEFEFEF), Color(0xFF8A8A8A).copy(alpha = 0f)),
            startY = riderYOffset,
            endY = riderYOffset + yInc * 6f
        ),
        style = Stroke(width = STROKE_WIDTH_PX, cap = StrokeCap.Round)
    )
}

internal fun DrawScope.drawDistanceLabels(
    segments: List<Segment>,
    yForSegment: (Float) -> Float,
    canvasHeight: Float,
    isMetric: Boolean,
    canvasWidthPx: Float
) {
    val numberPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#8A8A8A")
        textAlign = Paint.Align.RIGHT
        textSize = 56f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val unitPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#8A8A8A")
        textAlign = Paint.Align.LEFT
        textSize = 56f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    segments.forEach { seg ->
        val y = yForSegment(seg.distance)
        if (y !in 0f..canvasHeight) {
            return@forEach
        }
        val rounded = (seg.distance / LABEL_EVERY).roundToInt()
        if (abs(rounded * LABEL_EVERY - seg.distance) < SEGMENT_DISTANCE / 2f) {
            val number = String.format("%.1f", rounded * LABEL_EVERY)
            val unit = if (isMetric) " km" else " mi"
            val x = canvasWidthPx - 24f
            drawContext.canvas.nativeCanvas.apply {
                drawText(number, x - unitPaint.measureText(unit), y - 8f, numberPaint)
                drawText(unit, x - unitPaint.measureText(unit), y - 8f, unitPaint)
            }
        }
    }
}

internal fun DrawScope.drawRiderMarker(
    beginSeg: Segment?,
    endSeg: Segment?,
    animatedDistance: Float,
    riderYOffset: Float,
    yInc: Float,
    yForSegment: (Float) -> Float,
    userMarkerBitmap: ImageBitmap
) {
    val start = beginSeg
    val end = endSeg
    if (start != null && end != null) {
        val diff = (animatedDistance - start.distance) / SEGMENT_DISTANCE
        val riderX = start.x + (end.x - start.x) * diff.coerceIn(0f, 1f)

        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEFEFEF), Color(0xFF8A8A8A).copy(alpha = 0f)),
                startY = riderYOffset,
                endY = riderYOffset + yInc * 6f
            ),
            start = Offset(start.x, yForSegment(start.distance)),
            end = Offset(riderX, riderYOffset),
            strokeWidth = STROKE_WIDTH_PX,
            cap = StrokeCap.Round
        )

        val markerSz = 114
        drawImage(
            image = userMarkerBitmap,
            dstOffset = IntOffset((riderX - markerSz / 2f).toInt(), (riderYOffset - markerSz / 2f).toInt()),
            dstSize = IntSize(markerSz, markerSz)
        )
    }
}

internal fun DrawScope.drawTargetMarkerAtPosition(
    clampedTargetY: Float,
    targetX: Float,
    targetTimeLabel: String?,
    targetMarkerBitmap: ImageBitmap
) {
    val markerSz = 144
    drawImage(
        image = targetMarkerBitmap,
        dstOffset = IntOffset(
            (targetX - markerSz / 2f).toInt(),
            (clampedTargetY - markerSz / 2f).toInt()
        ),
        dstSize = IntSize(markerSz, markerSz)
    )

    if (!targetTimeLabel.isNullOrBlank()) {
        val isBehind = targetTimeLabel.startsWith("-")
        val pillBackground = if (isBehind) Color(0xFF062A1A) else Color(0xFF1C1C1C)
        val pillBorder = if (isBehind) Color(0xFF00E676) else Color(0xFF9E9E9E)
        val pillTextPaint = Paint().apply {
            color = if (isBehind)
                android.graphics.Color.parseColor("#00F58A")
            else
                android.graphics.Color.WHITE
            textAlign = Paint.Align.LEFT
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        drawTimeLabel(
            clampRadius = 130f,
            targetTimeLabel = targetTimeLabel,
            targetX = targetX,
            clampedTargetY = clampedTargetY,
            pillBackground = pillBackground,
            pillBorder = pillBorder,
            pillTextPaint = pillTextPaint
        )
    }
}

internal fun DrawScope.drawEdgeFades(size: Size, canvasHeight: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0A0A), Color(0x000A0A0A)),
            startY = 0f, endY = 150f
        ),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, 150f)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x000A0A0A), Color(0xFF0A0A0A)),
            startY = canvasHeight * 0.85f, endY = canvasHeight
        ),
        topLeft = Offset(0f, canvasHeight * 0.85f),
        size = Size(size.width, canvasHeight * 0.15f)
    )
}

internal fun DrawScope.drawTimeLabel(
    clampRadius: Float,
    targetTimeLabel: String,
    targetX: Float,
    clampedTargetY: Float,
    pillBackground: Color,
    pillBorder: Color,
    pillTextPaint: Paint
) {
    val pillPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#00F58A")
        textAlign = Paint.Align.LEFT
        textSize = 44f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val pillH = 64f
    val pillPadH = 28f
    val pillGap = clampRadius - 82f
    val pillW = pillPaint.measureText(targetTimeLabel) + pillPadH * 2f
    val pillRight = targetX - pillGap
    val pillLeft = pillRight - pillW
    val pillTop = clampedTargetY - pillH / 2f

    drawRoundRect(
        color = pillBackground,
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(pillH / 2f)
    )
    drawRoundRect(
        color = pillBorder,
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(pillH / 2f),
        style = Stroke(width = 3f)
    )
    drawContext.canvas.nativeCanvas.drawText(
        targetTimeLabel,
        pillLeft + pillPadH,
        clampedTargetY + pillPaint.textSize / 3f,
        pillTextPaint
    )
}