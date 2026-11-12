package com.jht.spinbikeapps.targettraining.ui

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.ui.popup.ConfirmationPopUp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.DISTANCE_ACCENT
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_4
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlinx.coroutines.delay

private const val MAX_TARGET_TRAINING_DISTANCE = 23.49f
private const val MIN_DISTANCE_STEP_VALUE = 0.001f
private const val MAP_DISTANCE = 9f

/**
 * Target training distance visualization.
 *
 * Renders the workout path, progress, target marker, rider marker and
 * distance labels. The implementation maintains a rolling window of
 * path segments similar to the legacy BikeMapWidget implementation.
 */
@Composable
internal fun DistanceScreen(
    modifier: Modifier = Modifier,
    currentDistance: Float = 0f,
    totalDistance: Float,
    targetDistance: Float? = null,
    targetTimeLabel: String? = null,
    isMetric: Boolean,
    onClickEndWorkout: () -> Unit = {},
    onClickContinue: () -> Unit = {},
    isPopUpVisible: Boolean = false,
    totalDistanceCovered: Float = 0f
) {
    val userMarkerBitmap = ImageBitmap.imageResource(id = R.drawable.ic_avatar_user)
    val targetMarkerBitmap = ImageBitmap.imageResource(id = R.drawable.ic_target_green)

    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    var trackStateKey by remember { mutableStateOf(0) }

    LaunchedEffect(currentDistance) {
        if (currentDistance < MIN_DISTANCE_STEP_VALUE) {
            trackStateKey++
            canvasWidth = 0f
            canvasHeight = 0f
        }
    }

    val trackState = remember(trackStateKey) {
        SegmentTrackState(canvasWidthProvider = { canvasWidth })
    }
    val segments = trackState.segments

    val animatedDistance by animateFloatAsState(
        targetValue = currentDistance,
        animationSpec = tween(
            durationMillis = 1000, // longer than update interval = smooth bridging
            easing = LinearEasing
        ),
        label = "AnimatedDistance"
    )

    val effectiveTotal = targetDistance ?: totalDistance
    val progress = (animatedDistance / effectiveTotal).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (targetDistance != null) {
            TargetTrainingProgressBar(
                progress = progress,
                progressColors = listOf(GREEN_4, DISTANCE_ACCENT),
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.distance_target_progress_bar_icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(12.dp.computeSize())
                    )
                },
                trailingLabel = {
                    Row(modifier = Modifier.padding(start = 16.dp.computeSize()),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("%.1f", currentDistance.coerceAtMost(targetDistance)),
                            color = DISTANCE_ACCENT,
                            fontSize = 20.sp.computeSize(),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            modifier = Modifier.align(Alignment.Bottom),
                            text = " " + if(isMetric) stringResource(R.string.km_lc) else stringResource(R.string.mi_lc),
                            color = DISTANCE_ACCENT.copy(0.6f),
                            fontSize = 16.sp.computeSize(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        TotalMilesCard(
            modifier = Modifier
                .zIndex(10f)
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            totalMiles = totalDistanceCovered,
            isMetric = isMetric
        )

        Canvas(Modifier.fillMaxSize()) {

            if (canvasWidth != size.width || canvasHeight != size.height) {
                canvasWidth = size.width
                canvasHeight = size.height
                if (trackState.lastX == 0f) {
                    trackState.lastX = 70f
                } // original mLastX initial value
            }

            trackState.ensureSegments(currentDistance)

            val yInc = (canvasHeight / SEGMENT_DISTANCE) / MAP_DISTANCE
            val riderYOffset = canvasHeight * 0.5f
            val yForSegment: (Float) -> Float = { segDist -> (animatedDistance - segDist) * yInc + riderYOffset }

            while (segments.size > 1 && yForSegment(segments.first().distance) > canvasHeight + yInc + 1) {
                segments.removeAt(0)
                trackState.addNextTrack()
            }
            while (segments.size < MAX_SEGMENTS) trackState.addNextTrack()

            drawSegmentRows(segments, yForSegment, yInc, canvasHeight)

            val trackPaths = buildTrackPaths(segments, yForSegment, riderYOffset, canvasHeight, yInc)
            drawTrackShadow(trackPaths.grayPath)
            drawGrayTrack(trackPaths.grayPath)
            drawTraveledTrack(trackPaths.traveledPath, riderYOffset, yInc)

            drawDistanceLabels(segments, yForSegment, canvasHeight, isMetric, size.width)

            drawRiderMarker(
                beginSeg = trackPaths.beginSeg,
                endSeg = trackPaths.endSeg,
                animatedDistance = animatedDistance,
                riderYOffset = riderYOffset,
                yInc = yInc,
                yForSegment = yForSegment,
                userMarkerBitmap = userMarkerBitmap
            )
            if (targetDistance != null && !targetTimeLabel.isNullOrBlank()) {
                val isAhead = targetTimeLabel.startsWith("+")
                // "+" means user is behind target → marker goes ABOVE rider (smaller Y)
                // "-" means user is ahead of target → marker goes BELOW rider (larger Y)
                val fixedTargetY = if (isAhead) {
                    riderYOffset - canvasHeight * 0.28f
                }
                else {
                    riderYOffset + canvasHeight * 0.28f
                }
                val clampedTargetY = fixedTargetY.coerceIn(130f, canvasHeight - 130f)

                val (tBegin, tEnd) = findClampBounds(segments, yForSegment, clampedTargetY)
                val targetX = if (tBegin != null && tEnd != null) {
                    val impliedDistance = tBegin.distance +
                            ((clampedTargetY - yForSegment(tBegin.distance)) /
                                    (yForSegment(tEnd.distance) - yForSegment(tBegin.distance))) *
                            SEGMENT_DISTANCE
                    val diff = (impliedDistance - tBegin.distance) / SEGMENT_DISTANCE
                    tBegin.x + (tEnd.x - tBegin.x) * diff.coerceIn(0f, 1f)
                } else {
                    canvasWidth / 2f
                }

                drawTargetMarkerAtPosition(
                    clampedTargetY = clampedTargetY,
                    targetX = targetX,
                    targetTimeLabel = targetTimeLabel,
                    targetMarkerBitmap = targetMarkerBitmap
                )
            }
            drawEdgeFades(size, canvasHeight)
        }

            if (isPopUpVisible){
                ConfirmationPopUp(
                    onClickEndWorkout = onClickEndWorkout,
                    onClickContinue = onClickContinue,
                    modifier = modifier.align(Alignment.Center)
                )
            }
    }
}

@Composable
private fun TotalMilesCard(
    modifier: Modifier = Modifier,
    totalMiles: Float,
    isMetric: Boolean
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 38.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color(0x40070707),
                spotColor = Color(0x40070707)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF272727), Color(0xFF1d1d1d))
                )
            )
            .border(
                width = 1.3.dp,
                color = Color(0xFF393939),
                shape = RoundedCornerShape(12.dp)
            )
            .drawWithContent {
                drawContent()
                // Top inner shadow approximation
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF292929), Color.Transparent),
                        startY = 0f,
                        endY = with(density) { 12.9.dp.toPx() }
                    )
                )
                // Bottom inner shadow approximation
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF1c1c1c)),
                        startY = size.height - with(density) { 12.9.dp.toPx() },
                        endY = size.height
                    )
                )
            }
            .padding(horizontal = 40.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.2f", totalMiles),
                fontSize = 88.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFf5f5f5),
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color(0x40ffffff),
                        offset = Offset(1.3f, 1.3f),
                        blurRadius = 2.6f
                    )
                ),
                letterSpacing = 0.sp,
                lineHeight = 88.sp
            )
            Text(
                text = if(isMetric) stringResource(R.string.total_kms_tc) else stringResource(R.string.total_miles_tc),
                fontSize = 28.sp,
                color = Color(0xFFf5f5f5).copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewDistanceAnimated() {
    var d by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (d < MAX_TARGET_TRAINING_DISTANCE) {
            delay(50)
            d += 0.001f
        }
    }

    val targetDist = 18f
    val timeLabel = if (d > targetDist) "- 12:30" else "+ 01:15"

    DistanceScreen(
        totalDistance = MAX_TARGET_TRAINING_DISTANCE,
        currentDistance = d,
        targetDistance = targetDist,
        targetTimeLabel = timeLabel,
        isMetric = false
    )
}

@AllDevicePreviews
@Composable
private fun PreviewTotalMilesCard() {
    TotalMilesCard(totalMiles = 23.49f, isMetric =  false)
}
