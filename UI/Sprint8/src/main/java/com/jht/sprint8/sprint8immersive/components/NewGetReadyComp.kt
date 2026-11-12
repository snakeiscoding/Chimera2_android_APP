package com.jht.sprint8.sprint8immersive.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.model.GetReadyConfig
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_GET_READY_COLORS
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable function to display the "Get Ready" message for the Sprint8 workout.
 *
 * @param modifier
 * Modifier applied to the component for layout, styling, or positioning.
 * @param getReadyConfig
 * The message details.
 * @param screenWidth
 * The width of the screen in Dp, used to adjust text size and layout
 * responsively.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NewGetReadyComp(
    getReadyConfig: GetReadyConfig,
    screenWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val componentTag = "NewGetReadyComp"
    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse
        )
    )
    val fadeAlpha by animateFloatAsState(
        targetValue = if (getReadyConfig.isVisible) 1f else 0f, animationSpec = tween(
            durationMillis = 5000,
        )
    )
    val alpha = when {
        getReadyConfig.isBlinking -> {
            blinkAlpha
        }
        getReadyConfig.isVisible -> {
            fadeAlpha
        }
        else -> {
            0f
        }
    }
    Box(
        modifier = modifier
            .alpha(alpha)
            .width(screenWidth * 0.5f)
            .wrapContentHeight()
            .background(
                brush = Brush.horizontalGradient(
                    SPRINT8_NEW_GET_READY_COLORS
                )
            )
            .padding(horizontal = 4.dp.computeSize())
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Text(
            getReadyConfig.title,
            fontSize = 26.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("$componentTag:GetReadyLabel:Text"),
        )

    }
}

@AllDevicePreviews
@Composable
fun NewGetReadyCompPreview(){
    NewGetReadyComp(
        modifier = Modifier,
        getReadyConfig = GetReadyConfig(stringResource(R.string.get_ready_tc)),
        screenWidth = 200.dp.computeSize()
    )
}