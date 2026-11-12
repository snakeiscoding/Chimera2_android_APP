package com.jht.landmarks.common.components
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.landmarks.R
import com.jht.landmarks.common.utils.Extensions.clipToProgress
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.jht.data.LandmarkWorkoutItem
import com.jht.landmarks.common.utils.Colors.GreenCyan
import com.jht.landmarks.common.utils.Colors.GunMetalLight
import com.jht.landmarks.common.utils.Colors.Mosque
import com.jht.landmarks.common.utils.Colors.Tropical_Rainforest
import com.jht.landmarks.common.utils.Constants.EIGHT_HUNDRED_MILLI
import com.jht.landmarks.common.utils.Constants.FIFTY_FLOAT
import com.jht.landmarks.common.utils.Constants.HUNDRED_FLOAT
import com.jht.landmarks.common.utils.Constants.ONE_FLOAT
import com.jht.landmarks.common.utils.Constants.ONE_TWENTY_FLOAT
import com.jht.landmarks.common.utils.Constants.ONE_TWO_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_NINE_EIGHT_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_SEVEN_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_SEVEN_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_THREE_SEVEN_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_THREE_SIX_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_TWO_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_TWO_FOUR_SEVEN_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_ZERO_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_ZERO_ONE_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_ZERO_THREE_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_ZERO_ZERO_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.SIXTY_FIVE_FLOAT
import com.jht.landmarks.common.utils.Constants.TEN_MILLI
import com.jht.landmarks.common.utils.Constants.THIRTY_MILLI
import com.jht.landmarks.common.utils.Constants.TWENTY_MILLI
import com.jht.landmarks.common.utils.Constants.ZERO_FLOAT
import kotlinx.coroutines.delay

// Using two .png files one on top of other to create a progress animation,
// placing the filled/graded png on top of the empty png. making the filled
// png to be visible from bottom to top, based on the progress of the filled png.
// which looks like animation.
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun LandmarkProgressAnimation(
    progress: Float = 0.0f,
    filledUri: Uri,
    emptyUri: Uri,
    onHeightChange: (Dp) -> Unit
) {
    val componentTag = "LandmarkProgress"

    // Ensure progress is clamped between 0 and 1
    val clampedProgress = progress.coerceIn(ZERO_FLOAT, ONE_FLOAT)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .testTag("$componentTag:Container:Scrollable"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Landmark Empty Image
        GlideImage(
            model = emptyUri,
            contentDescription = "Empty Skeleton",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    val heightInDp = with(density) {
                        layoutCoordinates.size.height.toDp()
                    }
                    onHeightChange(heightInDp)
                }
                .testTag("$componentTag:EmptyImage:Image")
        )

        // Landmark Filled Progress Image
        GlideImage(
            model = filledUri,
            contentDescription = "Floor Progress",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .clipToProgress(clampedProgress)
                .testTag("$componentTag:FilledImage:Image")
        )
    }
}

// The progress indicator composable
@Composable
internal fun LandmarkProgressTrack(progress: Float, maxProgressHeight: Dp) {
    VerticalProgressBar(
        progress = progress,
        lineColor = GunMetalLight,
        gradientColors = listOf(
            Mosque,
            GreenCyan,
        ),
        maxProgressHeight = maxProgressHeight
    )
}

// Using dynamic way to do the progress bar where showing the unprogressed track
// as gray color and the progressed track as gradient color.
@Composable
private fun VerticalProgressBar(
    modifier: Modifier = Modifier,
    progress: Float = 0.0f,
    lineColor: Color = GunMetalLight,
    gradientColors: List<Color> = listOf(
        Mosque,
        GreenCyan,
    ),
    maxProgressHeight: Dp,
) {
    val componentTag = "LandmarkProgressTrack"

    BoxWithConstraints(
        modifier = modifier
            .height(maxProgressHeight)
            .testTag("$componentTag:Container:Scrollable")
    ) {
        val screenWidth = maxWidth
        val thumbSize = screenWidth * POINT_ZERO_FIVE_FLOAT
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(
                    bottom = maxProgressHeight * POINT_ZERO_THREE_FLOAT,
                    top = maxProgressHeight * POINT_ZERO_THREE_FLOAT
                )
                .testTag("$componentTag:TrackRow:Scrollable"),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val lineWidth = screenWidth * POINT_ZERO_THREE_FLOAT

            // Horizontal split decoration
            Column(
                modifier = Modifier
                    .height(maxProgressHeight)
                    .wrapContentWidth()
                    .zIndex(2f)
                    .testTag("$componentTag:DecorationColumn:Scrollable"),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.End
            ) {

                Box(
                    Modifier
                        .width(screenWidth * POINT_TWO_FOUR_SEVEN_FLOAT)
                        .height(maxProgressHeight * POINT_ZERO_ZERO_FIVE_FLOAT)
                        .background(Color.White.copy(alpha = POINT_TWO_FIVE_FLOAT))
                        .testTag("$componentTag:DecorationLineTop:Image")
                )

                Box(
                    Modifier
                        .width(screenWidth * POINT_THREE_SEVEN_FLOAT)
                        .height(maxProgressHeight * POINT_ZERO_ZERO_FIVE_FLOAT)
                        .background(Color.White.copy(alpha = POINT_TWO_FIVE_FLOAT))
                        .testTag("$componentTag:DecorationLineMiddle:Image")
                )

                Box(
                    Modifier
                        .width(screenWidth * POINT_TWO_FOUR_SEVEN_FLOAT)
                        .height(maxProgressHeight * POINT_ZERO_ZERO_FIVE_FLOAT)
                        .background(Color.White.copy(alpha = POINT_TWO_FIVE_FLOAT))
                        .testTag("$componentTag:DecorationLineBottom:Image")
                )

            }

            Box(modifier = Modifier
                .fillMaxHeight()
                .testTag("$componentTag:ProgressTrack:Image")
                .width(lineWidth)) {
                // Draw the vertical line and its progress using Canvas.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2

                    // Draw background not-progressed line.
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, ZERO_FLOAT),
                        end = Offset(centerX, size.height),
                        strokeWidth = lineWidth.toPx(),
                        cap = StrokeCap.Round
                    )

                    val clampedProgress = progress.coerceIn(ZERO_FLOAT, ONE_FLOAT)

                    // Calculate thumb Y position 0f (as top) to size.height (as bottom).
                    val thumbY = size.height * (1 - clampedProgress)
                    // Height of progressed part.
                    val progressedHeight = size.height - thumbY

                    if (progressedHeight > ZERO_FLOAT) {
                        val brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(centerX, size.height),
                            end = Offset(centerX, ZERO_FLOAT)
                        )

                        // draw the progressed part with gradient
                        drawRect(
                            brush = brush,
                            topLeft = Offset(centerX - lineWidth.toPx() / 2, thumbY),
                            size = Size(width = lineWidth.toPx(), height = progressedHeight)
                        )
                    }
                }

            }
        }

        Box(
            modifier = Modifier
                .padding(start = thumbSize - screenWidth * POINT_THREE_SIX_FLOAT)
                .size(thumbSize)
                .offset(y = -(progress * ((maxProgressHeight - thumbSize / 2) * POINT_NINE_EIGHT_FLOAT).value).dp)
                .background(Tropical_Rainforest, CircleShape)
                .align(Alignment.BottomStart)
                .zIndex(3f) // Thumb color and shape
                .testTag("$componentTag:ProgressThumb:Image")
        ) {
            Image(
                painter = painterResource(R.drawable.flag),
                contentDescription = "Progress track head",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .padding(screenWidth * POINT_ZERO_ONE_FIVE_FLOAT)
                    .testTag("$componentTag:ProgressThumbIcon:Image"),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
internal fun LandmarkImageTransition(
    landmarksData: LandmarkWorkoutItem,
    timeoutMillis: Long = TEN_MILLI,
    onImageIndexChanged: (Int) -> Unit = {}
) {
    val componentTag = "LandmarkImageTransition"
    if (landmarksData.background.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(timeoutMillis)
            currentIndex = (currentIndex + 1) % landmarksData.background.size
            onImageIndexChanged(currentIndex)
        }
    }

    Crossfade(
        targetState = currentIndex,
        animationSpec = tween(durationMillis = EIGHT_HUNDRED_MILLI), label = ""
    ) { idx ->
        val imageUri = landmarksData.background[idx]
        val xPivot = landmarksData.xPivotCoordinates.getOrNull(idx) ?: FIFTY_FLOAT
        val yPivot = landmarksData.yPivotCoordinates.getOrNull(idx) ?: FIFTY_FLOAT

        val safeYPivot = if (yPivot > SIXTY_FIVE_FLOAT) SIXTY_FIVE_FLOAT else yPivot

        val bitmap = remember {
            BitmapFactory.decodeFile(imageUri.path)
        }

        bitmap?.let {
            ZoomedKenBurnsImage(
                bitmap = bitmap,
                xPivotPercent = xPivot,
                yPivotPercent = safeYPivot,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("$componentTag:BackgroundImage:Image")
            )
        }
    }
}

/**
 * This composable is exact copy of the images zoom logic which
 * used in loadKenburnAnimation() from AnimationLoader.java file.
 */
@Composable
private fun ZoomedKenBurnsImage(
    bitmap: Bitmap,
    xPivotPercent: Float,
    yPivotPercent: Float,
    modifier: Modifier = Modifier
) {
    val pivotFractionX = xPivotPercent / HUNDRED_FLOAT
    val pivotFractionY = yPivotPercent / HUNDRED_FLOAT

    val infiniteTransition = rememberInfiniteTransition(label = "")

    val translateX by infiniteTransition.animateFloat(
        initialValue = ZERO_FLOAT,
        targetValue = ONE_TWENTY_FLOAT,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TWENTY_MILLI, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val kenBurnsScaleX by infiniteTransition.animateFloat(
        initialValue = ONE_FLOAT,
        targetValue = POINT_SEVEN_FIVE_FLOAT,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = THIRTY_MILLI, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val kenBurnsScaleY by infiniteTransition.animateFloat(
        initialValue = ONE_FLOAT,
        targetValue = POINT_SEVEN_FLOAT,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = THIRTY_MILLI, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val finalScaleX = ONE_TWO_FLOAT * kenBurnsScaleX
    val finalScaleY = ONE_TWO_FLOAT * kenBurnsScaleY

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .graphicsLayer {
                transformOrigin = TransformOrigin(pivotFractionX, pivotFractionY)
                translationX = translateX
                scaleX = finalScaleX
                scaleY = finalScaleY
            }
    )
}

@Composable
fun GlobeVideoPlayer(uri: Uri? = null, onVideoFinished: () -> Unit) {
    val componentTag = "GlobeVideoPlayer"

    if (uri == null) {
        onVideoFinished()
        return
    }
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)

                setVideoURI(uri)
                start()

                // We don't want the media controllers to show up
                postDelayed({
                    mediaController.hide()
                }, 100)

                // callback on completion
                setOnCompletionListener {
                    onVideoFinished()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
            .testTag("$componentTag:VideoView:Image")
    )
}
