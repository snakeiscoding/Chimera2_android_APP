package com.jht.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.dpToPx
import kotlin.math.absoluteValue

/*
    File contains widgets for Pager situations
 */


/** Move to next page with wrap around back to the first */
fun PagerState.rotatePages() { this.requestScrollToPage((this.currentPage + 1) % this.pageCount) }


/** Pager indicator using jumping dot style
 * Examples = https://blog.canopas.com/jetpack-compose-how-to-implement-custom-pager-indicators-8b6a01d63964#6923
 */
@Composable
fun PagerJumpingDotIndicator(
    state: PagerState,
    modifier: Modifier = Modifier,
    dotSize: Dp = 20.dp,
    dotSpacing: Dp = 10.dp,
    jumpScale: Float = 0.8f,
    activeColor: Color = DynamicTheme.colors.primary,
    inactiveColor: Color = activeColor.copy(0.25f)
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // row of inactive dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(dotSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(state.pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(
                            color = inactiveColor,
                            shape = CircleShape
                        )
                        .testTag("PagerDot_$index")
                )
            }
        }
        // This is the floating active dot
        Box(
            Modifier
                .jumpingDotTransition(state, dotSpacing, jumpScale)
                .size(dotSize)
                .background(
                    color = activeColor,
                    shape = CircleShape,
                )
        )
    }
}

private fun Modifier.jumpingDotTransition(pagerState: PagerState, dotSpacing: Dp, jumpScale: Float) =
    graphicsLayer {
        val pageOffset = pagerState.currentPageOffsetFraction
        val scrollPosition = pagerState.currentPage + pageOffset
        translationX = scrollPosition * (size.width + dotSpacing.roundToPx())

        val scale: Float
        val targetScale = jumpScale - 1f
        scale = if (pageOffset.absoluteValue < .5) {
            1.0f + (pageOffset.absoluteValue * 2) * targetScale
        } else {
            jumpScale + ((1 - (pageOffset.absoluteValue * 2)) * targetScale)
        }
        scaleX = scale
        scaleY = scale
    }


/**
 * Page Progress indicator typically used for pagers or carousels.
 * Highlights the current page and animates swipe progress using a fill effect.
 *
 * Displays a row of indicators where the selected one fills based on swipe progress,
 * giving users a sense of directional feedback.
 *
 * Important parameters.
 * @param dragOffset Current swipe drag offset used to calculate the progress fill.
 * @param screenWidthPx Width of the screen in pixels to calculate swipe threshold for animation.
 * @param shape Shape applied to indicators (e.g., rounded or circle).
 */
@Composable
fun PageProgressIndicator(
    modifier: Modifier = Modifier,
    dragOffset: Animatable<Float, AnimationVector1D> = Animatable(0f),
    currentIndex: Int,
    itemCount: Int,
    screenWidthPx: Float = 0f, // used to calculate swipe on pager/carousel for indicator lerp effect
    indicatorHeight: Dp = 12.dp.computeSize(),
    indicatorWidth: Dp = 12.dp.computeSize(),
    selectedIndicatorWidth: Dp = 12.dp.computeSize(),
    shape: Shape = RoundedCornerShape(50.dp),
    paddingValues: PaddingValues = PaddingValues(),
    selectedIndicatorColor: Color = DynamicTheme.colors.primary,
    indicatorColor: Color = DynamicTheme.colors.textFieldStroke,
    shadow: Dp = 0.dp,
    strokeWidth: Dp = 0.dp,
    strokeColor: Color = Color.Transparent,
    selectedIndicatorStrokeColor: Color = Color.Transparent,
    strokeShape: Shape = RoundedCornerShape(50.dp),
    indicatorProgress: Float = 1f
) {
    // Compute progress based on the drag offset relative to a threshold.
    // In case there is no swipe (dragOffset.value == 0), we set progress to 1 (full fill).
    val threshold = screenWidthPx / 2 // generally swipe happens less than half of screen width. can also keep it to full width.
    val progress = when {
        dragOffset.value < 0 -> (-dragOffset.value / threshold).coerceIn(0f, 1f)
        dragOffset.value > 0 -> (1 - (dragOffset.value / threshold)).coerceIn(0f, 1f)
        else -> indicatorProgress
    }

    Row(
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(itemCount) { index ->

            val width = if (index == currentIndex)
                selectedIndicatorWidth
            else indicatorWidth

            if (index == currentIndex) {
                // For the selected indicator render Progress Indicator.
                Box(
                    modifier = modifier
                        .padding(paddingValues)
                        .height(indicatorHeight)
                        .width(width)
                        .border(
                            strokeWidth,
                            selectedIndicatorStrokeColor,
                            strokeShape
                        )
                        .shadow(shadow)
                ) {
                    ProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        unProgressedColor = indicatorColor,
                        progressedColor = selectedIndicatorColor,
                        shape = shape,
                        progress = progress
                    )
                }
            } else {
                // For the unselected ones, render static indicator.
                Box(
                    modifier = modifier
                        .padding(paddingValues)
                        .height(indicatorHeight)
                        .width(width)
                        .border(
                            strokeWidth,
                            strokeColor,
                            strokeShape
                        )
                        .background(
                            color = indicatorColor,
                            shape = shape
                        )
                        .shadow(shadow)
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    unProgressedColor: Color,
    progressedColor: Color,
    shape: Shape,
    progress: Float = 1f // if no progress attached, will consider selected indicator is filled.
) {
    // Outer Box: the un-progressed background.
    Box(
        modifier = modifier
            .clip(shape)
            .background(unProgressedColor)
    ) {
        // Inner Box: filled proportionally based on progress.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(progressedColor)
        )
    }
}


/**
 * Basic usage of customizable page indicators is demonstrated here.
 * For an example of a progressing selected indicator that syncs with page swipes,
 * refer to the Carousel implementation in the new WorkoutSelectionScreen.
 */
@AllDevicePreviews
@Composable
private fun CarouselIndicatorsPreview() {
    val currentItem = remember { mutableStateOf(0) }
    val totalItem = 3

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_arrow_left_white),
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable {
                    currentItem.value = (currentItem.value - 1 + totalItem) % totalItem
                }
        )
        Image(
            painter = painterResource(R.drawable.ic_arrow_left_white),
            contentDescription = "",
            modifier = Modifier
                .rotate(180f)
                .align(Alignment.CenterEnd)
                .clickable {
                    currentItem.value = (currentItem.value + 1) % totalItem
                }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageProgressIndicator(
                currentIndex = currentItem.value,
                itemCount = 3,
                screenWidthPx = 500.dp.dpToPx(),
                selectedIndicatorWidth = 30.dp,
                paddingValues = PaddingValues(6.dp)
            )
            Spacer(Modifier.padding(10.dp))
            PageProgressIndicator(
                currentIndex = currentItem.value,
                itemCount = 3,
                screenWidthPx = 500.dp.dpToPx(),
                selectedIndicatorWidth = 30.dp,
                paddingValues = PaddingValues(6.dp),
                shape = RectangleShape,
                selectedIndicatorColor = Color.Magenta,
                indicatorColor = Color.DarkGray
            )
            Spacer(Modifier.padding(10.dp))
            PageProgressIndicator(
                currentIndex = currentItem.value,
                itemCount = 3,
                screenWidthPx = 500.dp.dpToPx(),
                paddingValues = PaddingValues(20.dp),
                indicatorWidth = 30.dp,
                indicatorHeight = 30.dp,
                selectedIndicatorWidth = 30.dp
            )
            Spacer(Modifier.padding(10.dp))
            PageProgressIndicator(
                currentIndex = currentItem.value,
                itemCount = 3,
                screenWidthPx = 500.dp.dpToPx(),
                paddingValues = PaddingValues(20.dp),
                shape = RectangleShape,
                strokeShape = RectangleShape,
                indicatorColor = Color.Transparent,
                strokeWidth = 1.dp,
                strokeColor = DynamicTheme.colors.textFieldStroke
            )
            Spacer(Modifier.padding(10.dp))
            PageProgressIndicator(
                currentIndex = currentItem.value,
                itemCount = 3,
                screenWidthPx = 500.dp.dpToPx(),
                paddingValues = PaddingValues(10.dp),
                shape = RoundedCornerShape(50.dp),
                indicatorColor = Color.Transparent,
                strokeWidth = 1.dp,
                strokeColor = DynamicTheme.colors.textFieldStroke,
                indicatorWidth = 30.dp,
                indicatorHeight = 30.dp,
                selectedIndicatorWidth = 30.dp
            )
        }

    }
}


@Preview
@Composable
private fun PreviewPagerJumpingDotIndicator() {
    val pagerState = rememberPagerState(2) {5}
    PagerJumpingDotIndicator(
        state = pagerState,
    )
}
