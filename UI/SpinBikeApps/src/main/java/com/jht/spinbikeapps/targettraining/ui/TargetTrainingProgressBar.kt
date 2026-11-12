package com.jht.spinbikeapps.targettraining.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_9
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.ORANGE_1
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@AllDevicePreviews
@Composable
internal fun TargetTrainingProgressBar(
    modifier: Modifier = Modifier,
    progress: Float,
    progressColors: List<Color> = listOf(
        ORANGE_1.copy(alpha = 0.7f),
        ORANGE_1
    ),
    trackColor: Color = Color(0xFF383838),
    trailingIcon: @Composable () -> Unit = {},
    trailingLabel: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(68.dp)
            .shadow(
                elevation = 10.dp,
                ambientColor = Color.Black,
                spotColor = Color.Black
            )
            .background(GRAY_9)
            .padding(horizontal = 64.dp, vertical = 6.dp)
            .zIndex(10f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(18.dp.computeSize())
                .fillMaxWidth(0.92f)
                .background(trackColor, CircleShape)
                .clip(CircleShape)
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        cornerRadius = CornerRadius(30f, 30f),
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                progressColors.last().copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.35f
                        )
                    )
                    drawRoundRect(
                        cornerRadius = CornerRadius(30f, 30f),
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                progressColors.last().copy(alpha = 0.3f)
                            ),
                            startY = size.height * 0.70f,
                            endY = size.height
                        )
                    )
                }
        ) {
            // Progress fill
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(progressColors)
                    )
            )
            // Trailing icon (target/crosshair)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                trailingIcon()
            }
        }

        // Trailing label (distance text / calorie count)
        Box(modifier = Modifier) {
            trailingLabel()
        }
    }
}