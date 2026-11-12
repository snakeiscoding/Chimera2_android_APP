package com.jht.landmarks

import android.R
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import com.jht.landmarks.common.components.LandmarkProgressOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.data.LandmarkWorkoutItem
import com.jht.landmarks.common.components.GlobeVideoPlayer
import com.jht.landmarks.common.components.LandmarkImageTransition
import com.jht.landmarks.common.utils.Constants.POINT_SIX_FLOAT
import com.jht.landmarks.common.utils.Constants.POINT_THREE_FIVE_FOUR_FLOAT
import com.jht.landmarks.common.utils.Constants.TEN_MILLI
import com.jht.landmarks.common.utils.scaledDp
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import com.jht.landmarks.common.components.LevelControlPanel
import com.jht.landmarks.common.components.PauseControlPanel
import com.jht.landmarks.common.components.ToggleFullScreenButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.fullscreen

const val CONTROL_ANIM_DELAY = 500

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LandmarksScreen(
    landmarkItem: LandmarkWorkoutItem,
    progress: Float,
    currentFloor: Int,
    maxFloors: Int,
    isFullScreen: Boolean,
    currentResistanceLevel: Int,
    maxResistance: Int,
    isPaused: Boolean,
    onLevelChange: (Int) -> Unit,
    onLevelUp: () -> Unit,
    onLevelDown: () -> Unit,
    onPauseToggle: () -> Unit,
    onSizeClick: () -> Unit
) {
    var landmarkFactsIndex by remember { mutableIntStateOf(0) }
    var isGlobeVideoPlayed by remember { mutableStateOf(false) }
    var isLaunchTimeOutFinished by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    LaunchedEffect(Unit) {
        //delay applied to show the globe video, after the countdown of 2.
        delay(1500)
        isLaunchTimeOutFinished = true
    }

    if (isLaunchTimeOutFinished) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
        ) {
            val screenHeight = maxHeight.value
            val screenWidth = maxWidth.value

            if (isGlobeVideoPlayed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(POINT_THREE_FIVE_FOUR_FLOAT)
                        .fillMaxHeight()
                        .background(Color.Black.copy(POINT_SIX_FLOAT))
                        .zIndex(1f)
                ) {
                    with(landmarkItem)
                    {
                        LandmarkProgressOverlay(
                            title = localContext.getString(titleId),
                            description = localContext.getString(descriptionId),
                            locationString = localContext.getString(locationTitle),
                            filledUri = silhouetteUriPair.second,
                            emptyUri = silhouetteUriPair.first,
                            progress = progress,
                            currentFloor = currentFloor,
                            maxFloors = maxFloors
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    LandmarkImageTransition(landmarkItem, TEN_MILLI) { index ->
                        landmarkFactsIndex = index
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 32.scaledDp, end = 32.scaledDp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(48.scaledDp)
                    ) {

                        AnimatedVisibility(
                            visible = isFullScreen,
                            enter = slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = CONTROL_ANIM_DELAY
                                ),
                                initialOffsetX = {screenWidth.toInt()}
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 500,
                                    delayMillis = CONTROL_ANIM_DELAY
                                )
                            ),
                            exit = fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(48.scaledDp)) {
                                LevelControlPanel(
                                    level = currentResistanceLevel,
                                    maxLevel = maxResistance,
                                    onLevelUp = onLevelUp,
                                    onLevelDown = onLevelDown,
                                    onLevelSet = onLevelChange
                                )
                                PauseControlPanel(
                                    isPaused = isPaused,
                                    onPauseClick = onPauseToggle
                                )
                            }
                        }

                        Box(modifier = Modifier.testTag("Landmarks:Workout:Fullscreen:Button")) {
                            ToggleFullScreenButton(
                                isFullScreen = isFullScreen,
                                onClick = onSizeClick
                            )
                        }
                    }
                }
            } else {
                val uri = landmarkItem.videoUri
                GlobeVideoPlayer(uri) {
                    isGlobeVideoPlayed = true
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun LevelControlPanelPreview() {
    MaterialTheme {
        Box(Modifier
            .fullscreen()
            .background(Color.Black))
        {
            Column(
                Modifier
                    .background(Color.Black)
                    .align(Alignment.BottomEnd)
                    .padding(50.scaledDp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(48.scaledDp)
            ) {
                LevelControlPanel(
                    level = 10,
                    maxLevel = 25,
                    onLevelUp = {},
                    onLevelDown = {},
                    onLevelSet = {}
                )
                PauseControlPanel(
                    isPaused = false,
                    onPauseClick = {}
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PauseControlPanelPreview() {
    MaterialTheme {
        Box(Modifier
            .background(Color.Black)
            .padding(20.dp)) {
            PauseControlPanel(
                isPaused = false,
                onPauseClick = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ToggleFullScreenButtonPreview() {
    MaterialTheme {
        Box(Modifier
            .background(Color.Black)
            .padding(20.dp)) {
            ToggleFullScreenButton(
                isFullScreen = false,
                onClick = {}
            )
        }
    }
}