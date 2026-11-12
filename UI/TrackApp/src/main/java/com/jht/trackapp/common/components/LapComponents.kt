package com.jht.trackapp.common.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.trackapp.common.models.LapDistanceEnum
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Track progress bar under-dev
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProgressBarComps(
    trackDimensions: DpSize,
    trackThickness: Float = 60f,
    lapProgress: Float,
    minMaxLapAnimationOffset: Pair<Float, Float> = Pair(0.005f, 0.99f),
    lapDistance: LapDistanceEnum = LapDistanceEnum.DISTANCE_400,
    lapsCompleted: Int = 0
) {

    var progress = lapProgress
    if ((lapsCompleted != 0 || progress > 0.9f) && (progress > minMaxLapAnimationOffset.second || progress < minMaxLapAnimationOffset.first))
        progress = 0.999f // in case of speed set to high and lapdistance set to 100/200
    // track progress will not turn to green also track animation won't show up.

    // Store the previous progress value to detect lap resets.
    val prevProgress = remember { mutableStateOf(progress) }
    val prevLapDistance = remember { mutableStateOf(lapDistance) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = when {
            lapDistance != prevLapDistance.value ->
                tween(durationMillis = 0)

            progress < prevProgress.value ->
                tween(durationMillis = 100, easing = LinearEasing)

            else ->
                tween(durationMillis = 1000, easing = LinearEasing)
        }
    )

    // Update the previous progress value after each recomposition.
    LaunchedEffect(progress) {
        prevProgress.value = progress
        prevLapDistance.value = lapDistance
    }

    // Colors for the gradient
    val startColor = Color(0xFF53420B)
    val endColor = Color(0xFFFFCB27)
    val lapCompleteColor = DynamicTheme.colors.heart3

    Canvas(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .size(trackDimensions.width, trackDimensions.height)
            .graphicsLayer { rotationY = 180f }
    ) {
        val trackWidth = trackDimensions.width.toPx()
        val trackHeight = trackDimensions.height.toPx()
        val radius = trackHeight / 2
        val halfThickness = trackThickness / 2

        val basePath = Path().apply {
            moveTo(trackWidth / 2, trackHeight - halfThickness)
            lineTo(radius, trackHeight - halfThickness)
            cubicTo(
                radius - radius / 2,
                trackHeight - halfThickness,
                halfThickness,
                trackHeight - halfThickness - radius / 2,
                halfThickness,
                radius
            )
            cubicTo(
                halfThickness,
                radius - radius / 2,
                halfThickness + radius / 2,
                halfThickness,
                radius,
                halfThickness
            )
            lineTo(trackWidth - radius, halfThickness)
            cubicTo(
                trackWidth - radius + radius / 2,
                halfThickness,
                trackWidth - halfThickness,
                radius - radius / 2,
                trackWidth - halfThickness,
                radius
            )
            cubicTo(
                trackWidth - halfThickness,
                radius + radius / 2,
                trackWidth - radius + radius / 2,
                trackHeight - halfThickness,
                trackWidth - radius,
                trackHeight - halfThickness
            )
            lineTo(trackWidth / 2, trackHeight - halfThickness)
            close()
        }

        // Draw the base track (solid black)
        drawPath(
            path = basePath,
            color = Color.Black,
            style = Stroke(width = trackThickness)
        )

        val measure = PathMeasure()
        measure.setPath(basePath, false)
        val totalLength = measure.length
        val progressDistance = animatedProgress * totalLength

        if (progressDistance <= 0f) return@Canvas

        val segmentCount = 200
        for (i in 0 until segmentCount) {
            val epsilon = 0.8f // to hide the gap between the segments
            val startDist = i * (progressDistance / segmentCount)
            val endDist = (i + 1) * (progressDistance / segmentCount) + epsilon
            if (startDist >= endDist) break

            val subPath = Path()
            measure.getSegment(startDist, endDist, subPath, true)

            val midDist = (startDist + endDist) / 2f
            val fraction = midDist / progressDistance

            val color = lerp(startColor, endColor, fraction)
            val completeColor = lerp(lapCompleteColor, lapCompleteColor, fraction)
            drawPath(
                path = subPath,
                color = if (animatedProgress >= minMaxLapAnimationOffset.second) completeColor else color,
                style = Stroke(width = trackThickness)
            )

        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LapDistanceComponent(
    width: Float = 0.0f,
    lapDistance: LapDistanceEnum,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {

    val color = if (isSelected) DynamicTheme.colors.primary else DynamicTheme.colors.gray8

    Column(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("TrackApp:${lapDistance.distance}:Button")
            .width(IntrinsicSize.Max)
            .clickable {
                onClick()
            },
        verticalArrangement = Arrangement.spacedBy(8.dp.computeSize())
    ) {
        Text(
            text = "${lapDistance.distance}m",
            color = color,
            style = DynamicTheme.typography.textStyle2,
            fontSize = 20.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(width.dp)
                .padding(top = 15.dp.computeSize())
                .align(Alignment.CenterHorizontally)
        )
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            thickness = 5.dp.computeSize()
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LapSelectionComponent(
    modifier: Modifier,
    itemWidth: Float = 0.0f,
    lapSelection: LapDistanceEnum,
    onClick: (LapDistanceEnum) -> Unit
) {
    LazyRow(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp.computeSize()))
            .background(DynamicTheme.colors.gray1),
        horizontalArrangement = Arrangement.spacedBy(5.dp.computeSize()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(LapDistanceEnum.entries.size) {
            LapDistanceComponent(
                itemWidth,
                LapDistanceEnum.entries[it],
                lapSelection == LapDistanceEnum.entries[it]
            ) {
                onClick(LapDistanceEnum.entries[it])
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun LapComponentsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(100.dp)
    ) {
        /*LapSelectionComponent(Modifier.fillMaxWidth(), 120f, LapDistanceEnum.DISTANCE_400) {

        }*/
        ProgressBarComps(
            trackDimensions = DpSize(1000.dp, 500.dp),
            lapProgress = 1.0f,
            minMaxLapAnimationOffset = Pair(0.005f, 0.99f),
            lapsCompleted = 0
        )
    }
}