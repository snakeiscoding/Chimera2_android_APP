package com.jht.spinbikeapps.targettraining.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_3
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_4
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_5
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.cos
import kotlin.math.sin

private val heartRateZoneRanges = listOf(
    0f to 60f,
    60f to 70f,
    70f to 80f,
    80f to 90f,
    90f to 100f,
)

private fun heartRateZoneIndex(percentage: Float): Int = when {
    percentage < 60f -> 0
    percentage < 70f -> 1
    percentage < 80f -> 2
    percentage < 90f -> 3
    else -> 4
}

private fun heartRatePointerAngle(percentage: Float): Float {
    val totalSweep = 180f
    val startAngleBase = 136f
    val gap = 3f
    val segmentSweep =
        (totalSweep - gap * (heartRateZoneRanges.size - 1)) / heartRateZoneRanges.size

    val zone = heartRateZoneIndex(percentage)
    val (zoneStart, zoneEnd) = heartRateZoneRanges[zone]
    val zoneProgress = ((percentage - zoneStart) / (zoneEnd - zoneStart)).coerceIn(0f, 1f)

    return startAngleBase + zone * (segmentSweep + gap) + zoneProgress * segmentSweep
}

@AllDevicePreviews
@Composable
private fun Preview() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        HeartRateComp(hr = 116f, maxHr = 192f)
    }
}

@Composable
internal fun HeartRateComp(
    modifier: Modifier = Modifier,
    hr: Float = 0f,
    maxHr: Float = 0f,
    targetZoneIndex: Int? = null,
) {
    val hrPercentage = if (maxHr > 0f && hr > 0f) (hr / maxHr) * 100f else 0f

    val animatedHrPercentage by animateFloatAsState(
        targetValue = hrPercentage,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "heartRatePointer"
    )

    val activeZone = remember(animatedHrPercentage) { heartRateZoneIndex(animatedHrPercentage) }
    val pointerAngle = remember(animatedHrPercentage) { heartRatePointerAngle(animatedHrPercentage) }
    val crosshairPainter = painterResource(id = R.drawable.ic_crosshair)
    val crosshairSize = 46.dp.computeSize(1920f)

    Box(modifier = modifier.wrapContentSize()) {
        Canvas(
            modifier = modifier.size(650.dp.computeSize(1920f))
        ) {
            val colors = listOf(
                TT_HT_ZONE_1,
                TT_HT_ZONE_2,
                TT_HT_ZONE_3,
                TT_HT_ZONE_4,
                TT_HT_ZONE_5,
            )

            val totalSweep = 180f
            val startAngleBase = 136f
            val gap = 3f
            val segmentSweep = (totalSweep - gap * (colors.size - 1)) / colors.size

            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            val crosshairSizePx = crosshairSize.toPx()

            colors.forEachIndexed { index, color ->
                val startAngle = startAngleBase + index * (segmentSweep + gap)

                drawArc(
                    color = if (index == activeZone) {
                        color
                    } else {
                        color.copy(alpha = 0.4f)
                    },
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
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

            val angleRad = Math.toRadians(pointerAngle.toDouble()).toFloat()
            val arrowOuterRadius = radius + 60f

            val tip = Offset(
                x = center.x + (arrowOuterRadius - 20f) * cos(angleRad),
                y = center.y + (arrowOuterRadius - 20f) * sin(angleRad)
            )

            val backCenter = Offset(
                x = center.x + arrowOuterRadius * cos(angleRad),
                y = center.y + arrowOuterRadius * sin(angleRad)
            )

            val perp = angleRad + Math.PI.toFloat() / 2f
            val arrowWidth = 15f

            val left = Offset(
                x = backCenter.x + arrowWidth * cos(perp),
                y = backCenter.y + arrowWidth * sin(perp)
            )

            val right = Offset(
                x = backCenter.x - arrowWidth * cos(perp),
                y = backCenter.y - arrowWidth * sin(perp)
            )

            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }

            drawPath(
                path = path,
                color = Color.White
            )
        }

        Column(
            modifier = modifier
                .wrapContentSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier.padding(bottom = 12.dp.computeSize(1920f)),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = hrPercentage.toInt().toString(),
                    fontSize = 144.sp.computeSize(1920f),
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = modifier.padding(bottom = 24.dp.computeSize(1920f)),
                    text = stringResource(R.string.percent_lc),
                    fontSize = 32.sp.computeSize(1920f),
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                )
            }

            HorizontalDivider(
                modifier = modifier.width(416.5.dp.computeSize(1920f)),
                thickness = 1.dp.computeSize(1920f),
                color = DynamicTheme.colors.gray6
            )
            Row(
                modifier = modifier.padding(top = 12.dp.computeSize(1920f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    hr.toInt().toString(),
                    fontSize = 96.sp.computeSize(1920f),
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )

                Column(modifier = modifier.padding(start = 12.dp.computeSize(1920f))) {
                    Image(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = null,
                        modifier = modifier.size(60.dp.computeSize(1920f))
                    )
                    Text(
                        stringResource(R.string.bpm_uc),
                        fontSize = 32.sp.computeSize(1920f),
                        color = DynamicTheme.colors.gray5,
                        fontFamily = FontFamily.SansSerif,
                    )
                }
            }
        }
    }
}
