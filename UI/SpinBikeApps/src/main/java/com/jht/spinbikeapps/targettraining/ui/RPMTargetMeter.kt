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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.RPMComp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.RED_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.jht.spinbikeapps.targettraining.utils.glowPaint

private const val MAX_ARC_RPM = 150f
private const val TARGET_ZONE_OFFSET = 5f

@AllDevicePreviews
@Composable
internal fun RPMTargetMeter(
    modifier: Modifier = Modifier,
    rpm: Float = 0f,
    targetRpm: Float = 60f,
    maxRpm: Float = MAX_ARC_RPM
) {
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        val animatedRpm by animateFloatAsState(
            targetValue = rpm,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            label = stringResource(R.string.rpm_uc)
        )

        val arcStroke = remember { Stroke(width = 40f) }

        Canvas(
            modifier = modifier
                .size(650.dp.computeSize(1920f))
                .graphicsLayer{ }
        ) {
            // Outer arc glow
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawArc(
                    android.graphics.RectF(0f, 0f, size.width, size.height),
                    135f,
                    270f,
                    false,
                    glowPaint(BLUE_1, 25f, 14f)
                )
            }
            drawArc(
                color = BLUE_1,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12f)
            )
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            val totalSweep = 270f
            val startAngleBase = 135f

            fun rpmToAngle(rpm: Float): Float {
                val progress = (rpm / maxRpm)
                    .coerceIn(0f, 1f)

                return startAngleBase + totalSweep * progress
            }

            drawArc(
                color = BLUE_1,
                startAngle = startAngleBase,
                sweepAngle = totalSweep,
                useCenter = false,
                style = Stroke(width = 12f)
            )

            val targetMin = (targetRpm - TARGET_ZONE_OFFSET)
                .coerceAtLeast(0f)

            val targetMax = (targetRpm + TARGET_ZONE_OFFSET)
                .coerceAtMost(maxRpm)

            val greenStartAngle = rpmToAngle(targetMin)
            val greenEndAngle = rpmToAngle(targetMax)

            val blueSweep = greenStartAngle - startAngleBase
            val greenSweep = greenEndAngle - greenStartAngle
            val redSweep = (startAngleBase + totalSweep) - greenEndAngle

            drawMeterArc(
                color = BLUE_2,
                startAngle = startAngleBase,
                sweepAngle = blueSweep,
                stroke = arcStroke
            )

            drawMeterArc(
                color = GREEN_2,
                startAngle = greenStartAngle,
                sweepAngle = greenSweep,
                stroke = arcStroke
            )

            drawMeterArc(
                color = RED_2,
                startAngle = greenEndAngle,
                sweepAngle = redSweep,
                stroke = arcStroke
            )

            val tickRadius = radius - 60f
            val totalTicks = 11
            for (i in 0 until totalTicks) {
                val angle = startAngleBase + (totalSweep / (totalTicks - 1)) * i

                val angleRad =
                    Math.toRadians(angle.toDouble())

                val isMajorTick = i % 2 == 0
                val tickLength = if (isMajorTick) 40f else 20f
                val strokeWidth = if (isMajorTick) 4f else 2f
                val start = Offset(
                    x = center.x + (tickRadius - tickLength) * cos(angleRad).toFloat(),
                    y = center.y + (tickRadius - tickLength) * sin(angleRad).toFloat()
                )
                val end = Offset(
                    x = center.x + tickRadius * cos(angleRad).toFloat(),
                    y = center.y + tickRadius * sin(angleRad).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth
                )
            }

            val needleAngle = rpmToAngle(animatedRpm)

            val needleLength = radius - 50f
            val needleWidth = 6f
            val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()
            val tip = Offset(
                center.x + needleLength * cos(angleRad),
                center.y + needleLength * sin(angleRad)
            )
            val leftBase = Offset(
                center.x + needleWidth * cos(angleRad + Math.PI.toFloat() / 2f),
                center.y + needleWidth * sin(angleRad + Math.PI.toFloat() / 2f)
            )
            val rightBase = Offset(
                center.x + needleWidth * cos(angleRad - Math.PI.toFloat() / 2f),
                center.y + needleWidth * sin(angleRad - Math.PI.toFloat() / 2f)
            )
            val needlePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(leftBase.x, leftBase.y)
                lineTo(rightBase.x, rightBase.y)
                close()
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(
                    needlePath.asAndroidPath(),
                    glowPaint(BLUE_1, 18f)
                )
            }
            drawPath(
                path = needlePath,
                color = BLUE_1
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawCircle(
                    center.x,
                    center.y,
                    25f,
                    glowPaint(BLUE_1, 14f)
                )
            }
            // Needle Hub
            drawCircle(
                color = BLUE_1,
                radius = 25f,
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 20f,
                center = center
            )
            drawCircle(
                color = BLUE_1,
                radius = 12f,
                center = center
            )
        }

        RPMComp(
            Modifier.align(Alignment.BottomCenter),
            rpm = rpm.toInt().toString()
        )
    }
}

fun DrawScope.drawMeterArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    stroke: Stroke
) {
    if (sweepAngle <= 0f) return

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(20f, 20f),
        size = Size(
            width = size.width - 40f,
            height = size.height - 40f
        ),
        style = stroke
    )
}

@AllDevicePreviews
@Composable
private fun RPMTargetMeterPreview(){
    RPMTargetMeter()
}