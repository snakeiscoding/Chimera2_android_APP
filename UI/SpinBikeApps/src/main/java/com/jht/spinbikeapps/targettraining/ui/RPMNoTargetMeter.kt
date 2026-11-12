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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.jht.spinbikeapps.targettraining.common.RPMComp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_4
import com.jht.spinbikeapps.targettraining.utils.glowPaint
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun RPMNoTargetMeter(
    modifier: Modifier = Modifier,
    rpm: Float = 0f,
    maxRpm: Float = 150f
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
            label = "rpm"
        )
        Canvas(
            modifier = modifier
                .size(650.dp.computeSize(1920f))
                .graphicsLayer { }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            // Needle
            val sweepProgress = if (maxRpm > 0f) {
                (animatedRpm / maxRpm).coerceIn(0f, 1f)
            } else {
                0f
            }
            val sweepAngle = 270f * sweepProgress

            val needleAngle = 135f + (270f * sweepProgress)
            val needleLength = radius - 50f
            val needleWidth = 6f
            val angleRad = Math.toRadians(needleAngle.toDouble()).toFloat()

            // Outer arc glow
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawArc(
                    android.graphics.RectF(0f, 0f, size.width, size.height),
                    135f, 270f, false,
                    glowPaint(BLUE_1, 25f, 14f)
                )
            }

            // Crisp outer arc
            drawArc(
                color = BLUE_1,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 14f)
            )

            // Background track
            drawArc(
                color = BLUE_4,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(20f, 20f),
                size = Size(width = size.width - 40f, height = size.height - 40f),
                style = Stroke(width = 40f)
            )

            // Progress fill
            drawArc(
                color = BLUE_2,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(20f, 20f),
                size = Size(width = size.width - 40f, height = size.height - 40f),
                style = Stroke(width = 40f)
            )

            // Tick marks
            val tickRadius = radius - 60f

            val totalTicks = 11

            for (i in 0 until totalTicks) {

                val angle = 135f + (270f / (totalTicks - 1)) * i
                val tickAngleRad = Math.toRadians(angle.toDouble())
                val isMajorTick = i % 2 == 0

                val tickLength = if (isMajorTick) 40f else 20f
                val strokeWidth = if (isMajorTick) 4f else 2f

                val start = Offset(
                    x = center.x + (tickRadius - tickLength) * cos(tickAngleRad).toFloat(),
                    y = center.y + (tickRadius - tickLength) * sin(tickAngleRad).toFloat()
                )

                val end = Offset(
                    x = center.x + tickRadius * cos(tickAngleRad).toFloat(),
                    y = center.y + tickRadius * sin(tickAngleRad).toFloat()
                )
                drawLine(color = Color.White, start = start, end = end, strokeWidth = strokeWidth)
            }

            // Needle path
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

            // Needle glow
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPath(
                    needlePath.asAndroidPath(),
                    glowPaint(BLUE_1, 18f)
                )
            }

            // needle
            drawPath(path = needlePath, color = BLUE_1)

            // Hub glow
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawCircle(
                    center.x, center.y, 25f,
                    glowPaint(BLUE_1, 14f)
                )
            }
            // Hub rings
            drawCircle(color = BLUE_1, radius = 25f, center = center)
            drawCircle(color = Color.Black, radius = 20f, center = center)
            drawCircle(color = BLUE_1, radius = 12f, center = center)
        }
        RPMComp(
            Modifier.align(Alignment.BottomCenter),
            rpm = rpm.toInt().toString(),
        )
    }
}

@AllDevicePreviews
@Composable
private fun RPMNoTargetMeterPreview(){
    RPMTargetMeter()
}
