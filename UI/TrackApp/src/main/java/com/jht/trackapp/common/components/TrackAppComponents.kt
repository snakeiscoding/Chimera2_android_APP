package com.jht.trackapp.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextWithImage(modifier: Modifier, title: String = "", subTitle: String = "", icon: Int = -1,testTagBase: String = "") {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = "image",
            alignment = Alignment.Center,
            modifier = Modifier
                .wrapContentSize()
                .size(30.dp.computeSize())
        )
        Text(
            text = title,
            fontSize = 75.sp.computeSize(),
            color = Color.White,
            modifier = Modifier.wrapContentSize().semantics { testTagsAsResourceId = true }
                .testTag("$testTagBase:Title:Text"),
            textAlign = TextAlign.Center,
            style = DynamicTheme.typography.textStyle2
        )
        Text(
            text = subTitle,
            fontSize = 20.sp.computeSize(),
            color = DynamicTheme.colors.gray3,
            modifier = Modifier.wrapContentSize().semantics { testTagsAsResourceId = true }
                .testTag("$testTagBase:SubTitle:Text"),
            textAlign = TextAlign.Center,
            style = DynamicTheme.typography.textStyle1
        )
    }
    }

@Composable
internal fun LapCompletePulseAnimation(
    width: Dp,
    height: Dp,
    lapsCompleted: Int = 0,
    lapProgress: Float
) {
    val transition = rememberInfiniteTransition(label = "")
    val duration = 4_000
    val count = 4
    val progress = List(count) {
        transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.2f,
            animationSpec = InfiniteRepeatableSpec(
                animation = tween(durationMillis = duration, easing = LinearEasing),
                initialStartOffset = StartOffset(it * duration / count),
                repeatMode = RepeatMode.Restart,
            ), label = ""
        )
    }

    if (lapsCompleted != 0 || lapProgress > 0.2f) { // check for the first lap.

        val animationColor = DynamicTheme.colors.heart3

        Canvas(
            modifier = Modifier
                .size(width, height)
        ) {
            val cornerRadius = 280.dp.toPx()

            progress.forEach {
                val scale = it.value
                drawRoundRect(
                    color = animationColor.copy(alpha = 1.2f - scale),
                    topLeft = Offset(
                        x = (size.width - (width.toPx() * scale)) / 2,
                        y = (size.height - (height.toPx() * scale)) / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(width.toPx() * scale, height.toPx() * scale),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun LapCompletePulseAnimationBox(
    screenWidth: Dp,
    screenHeight: Dp,
    lapProgress: Float,
    minMaxLapAnimationOffset: Pair<Float, Float> = Pair(0.005f, 0.99f),
    lapsCompleted: Int
) {
    val lastLapCompleteTime = remember { mutableStateOf(0L) }
    val currentTime = remember { mutableStateOf(System.currentTimeMillis()) }

    var animationKey by remember { mutableStateOf(0) }

    val lapCompleteTriggered =
        (lapProgress >= minMaxLapAnimationOffset.second || lapProgress < minMaxLapAnimationOffset.first)

    LaunchedEffect(lapCompleteTriggered) {
        if (lapCompleteTriggered) {
            animationKey++
            lastLapCompleteTime.value = System.currentTimeMillis()
        }
    }

    LaunchedEffect(lapProgress) {
        currentTime.value = System.currentTimeMillis()
    }

    val withinFourSeconds = (currentTime.value - lastLapCompleteTime.value) < 4000L

    val isVisible = lapCompleteTriggered || withinFourSeconds

    Box(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .size(screenWidth, screenHeight)
            .padding(top = 10.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            exit = fadeOut(animationSpec = tween(durationMillis = 1000)), // 1 sec fade-out
            enter = fadeIn(animationSpec = tween(durationMillis = 1000))   // optional: 1 sec fade-in
        ) {
            key(animationKey) {
                LapCompletePulseAnimation(
                    width = screenWidth,
                    height = (screenHeight * 0.61f),
                    lapsCompleted = lapsCompleted,
                    lapProgress = lapProgress
                )
            }
        }
    }
}


@AllDevicePreviews
@Composable
fun ComponentsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TextWithImage(modifier = Modifier.fillMaxWidth())
    }
}