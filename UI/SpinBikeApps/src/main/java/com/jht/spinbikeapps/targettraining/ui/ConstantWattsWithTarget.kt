package com.jht.spinbikeapps.targettraining.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.ConstantWattsComp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_ZONE
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_DEFAULT_TEXT
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.LIGHT_GREEN_ZONE
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_5
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_ZONE
import com.jht.spinbikeapps.targettraining.utils.glowPaint
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_GAUGE_PERCENTAGE = 170

private val zoneBoundaries = listOf(
    0, 55, 75, 90, 105, 120, 150, MAX_GAUGE_PERCENTAGE
)

private fun percentageToZoneIndex(percentage: Int): Int {
    return when {
        percentage <= 55 -> 0
        percentage <= 75 -> 1
        percentage <= 90 -> 2
        percentage <= 105 -> 3
        percentage <= 120 -> 4
        percentage <= 150 -> 5
        else -> 6
    }
}

private fun wattsZoneIndex(
    watts: Float, ftp: Int
): Int {
    if (ftp <= 0) return 0
    val percentage = ((watts / ftp) * 100).toInt()
    return percentageToZoneIndex(percentage)
}

private fun calculateGaugeProgress(
    watts: Float, ftp: Int
): Float {
    if (ftp <= 0) return 0f

    val percentage = ((watts / ftp) * 100).toInt().coerceIn(0, MAX_GAUGE_PERCENTAGE)
    val zoneIndex = percentageToZoneIndex(percentage)

    val zoneStart = zoneBoundaries[zoneIndex]
    val zoneEnd = zoneBoundaries[zoneIndex + 1]
    val progressWithinZone = (percentage - zoneStart).toFloat() / (zoneEnd - zoneStart).toFloat()

    return (zoneIndex + progressWithinZone.coerceIn(0f, 1f)) / zoneBoundaries.lastIndex
}
@Composable
internal fun ConstantWattsWithTarget(
    modifier: Modifier = Modifier,
    watts: Int = 0,
    averageWatts: Int = 0,
    userFtp: Int = 0,
    targetZoneIndex: Int? = null,
) {
    Box(modifier = modifier.wrapContentSize()) {
        val animatedWatts by animateFloatAsState(
            targetValue = watts.toFloat(),
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            label = stringResource(R.string.watts_lc)
        )

        val activeZone = remember(animatedWatts, userFtp) {
            wattsZoneIndex(animatedWatts, userFtp)
        }
        val crosshairPainter = painterResource(id = R.drawable.ic_crosshair)
        val crosshairSize = 46.dp.computeSize(1920f)

        Canvas(
            modifier = modifier.size(650.dp.computeSize(1920f))
        ) {
            val colors = listOf(
                BTN_DEFAULT_TEXT,
                BLUE_ZONE,
                LIGHT_GREEN_ZONE,
                YELLOW_ZONE,
                TT_HT_ZONE_5,
                TT_HT_ZONE_5,
                TT_HT_ZONE_5,
            )

            val totalSweep = 270f
            val startAngleBase = 136f
            val gap = 3f

            val segmentSweep =
                (totalSweep - gap * (colors.size - 1)) / colors.size
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawArc(
                    android.graphics.RectF(0f, 0f, size.width, size.height),
                    136f, 270f, false,
                    glowPaint(Color.White, 7f, 14f)
                )
            }

            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension - 60f) / 2f
            val crosshairSizePx = crosshairSize.toPx()

            colors.forEachIndexed { index, color ->

                val startAngle =
                    startAngleBase + index * (segmentSweep + gap)

                drawArc(
                    color = if (index == activeZone) {
                        color
                    } else {
                        color.copy(alpha = 0.4f)
                    },
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    topLeft = Offset(25f, 25f),
                    size = Size(
                        width = size.width - 50f,
                        height = size.height - 50f
                    ),
                    useCenter = false,
                    style = Stroke(width = 50f)
                )

                if (index == targetZoneIndex) {
                    val centerAngle = startAngle + segmentSweep / 2f
                    val angleRad = Math.toRadians(centerAngle.toDouble()).toFloat()
                    val x = center.x + radius * cos(angleRad)
                    val y = center.y + radius * sin(angleRad)

                    translate(x - crosshairSizePx / 2f, y - crosshairSizePx / 2f) {
                        with(crosshairPainter) {
                            draw(
                                size = Size(crosshairSizePx, crosshairSizePx),
                                colorFilter = if (index != activeZone) ColorFilter.tint(color) else null
                            )
                        }
                    }
                }
            }

            drawArc(
                color = Color.White,
                startAngle = 136f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 5f)
            )

            val progress = calculateGaugeProgress(
                watts = animatedWatts,
                ftp = userFtp
            )

            val sweepAngle = 270f * progress
            val endAngle = startAngleBase + sweepAngle

            val strokeWidth = -30f
            val needleRadius = (size.minDimension - strokeWidth) / 2f
            if (progress > 0f) {
                val angleRad = Math.toRadians(endAngle.toDouble())
                val dashLength = 80f
                val innerRadius = needleRadius - dashLength

                val startX = center.x + needleRadius * cos(angleRad).toFloat()
                val startY = center.y + needleRadius * sin(angleRad).toFloat()

                val endX = center.x + innerRadius * cos(angleRad).toFloat()
                val endY = center.y + innerRadius * sin(angleRad).toFloat()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawLine(
                        startX,
                        startY,
                        endX,
                        endY,
                        glowPaint(Color.White, 7f, 5f)
                    )
                }
                drawLine(
                    color = Color.White,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 5f,
                )
            }
        }
        ConstantWattsComp(
            modifier = Modifier.align(Alignment.Center),
            watts = watts.toString(),
            averageWatts = averageWatts.toString(),
            userFtp = if (userFtp > 0) {
                (watts.toFloat() / userFtp * 100).toInt()
            } else 0
        )
    }
}

@AllDevicePreviews
@Composable
private fun ConstantWattsWithTargetPreview() {
    ConstantWattsWithTarget(userFtp = 200, watts = 250)
}
