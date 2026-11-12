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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.ConstantWattsComp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_ZONE
import com.jht.spinbikeapps.targettraining.utils.glowPaint
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ConstantWattsNoTargetMeter(
    modifier: Modifier = Modifier,
    watts: Int = 0,
    maxWatts: Float = 2500f,
    averageWatts: Int = 0,
    userFtp: Int = 0
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
        Canvas(
            modifier = modifier
                .size(650.dp.computeSize(1920f))
                .graphicsLayer{}
        ) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawArc(
                    android.graphics.RectF(0f, 0f, size.width, size.height),
                    136f, 270f, false,
                    glowPaint(YELLOW_ZONE, 7f, 14f)
                )
            }
            drawArc(
                color = YELLOW_ZONE,
                startAngle = 136f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12f)
            )
            val progress = (animatedWatts / maxWatts).coerceIn(0f, 1f)
            val sweepAngle = 270f * progress
            drawArc(
                color = Color(0xFF8F8232),
                startAngle = 136f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(25f, 25f),
                size = Size(
                    width = size.width - 50f,
                    height = size.height - 50f
                ),
                style = Stroke(
                    width = 50f,
                )
            )
            drawArc(
                color = YELLOW_ZONE,
                startAngle = 136f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(25f, 25f),
                size = Size(
                    width = size.width - 50f,
                    height = size.height - 50f
                ),
                style = Stroke(
                    width = 50f,
                )
            )
            val startAngle = 136f
            val endAngle = startAngle + sweepAngle
            val strokeWidth = -30f
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (progress > 0f) {
                val angleRad = Math.toRadians(endAngle.toDouble())
                val dashLength = 80f
                val outerRadius = radius
                val innerRadius = radius - dashLength

                val startX = center.x + outerRadius * cos(angleRad).toFloat()
                val startY = center.y + outerRadius * sin(angleRad).toFloat()

                val endX = center.x + innerRadius * cos(angleRad).toFloat()
                val endY = center.y + innerRadius * sin(angleRad).toFloat()

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawLine(
                        startX,
                        startY,
                        endX,
                        endY,
                        glowPaint(YELLOW_ZONE, 12f, 5f)
                    )
                }
                drawLine(
                    color = YELLOW_ZONE,
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
            userFtp = userFtp
        )
    }
}

@AllDevicePreviews
@Composable
private fun ConstantWattsNoTargetPreview(){
    ConstantWattsNoTargetMeter()
}