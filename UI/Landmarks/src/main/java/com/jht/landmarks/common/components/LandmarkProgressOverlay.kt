package com.jht.landmarks.common.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jht.landmarks.R
import com.jht.landmarks.common.utils.Constants.ZERO
import com.jht.theme.DynamicTheme
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize

@Composable
internal fun LandmarkProgressOverlay(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    locationString: String,
    filledUri: Uri,
    emptyUri: Uri,
    progress: Float,
    currentFloor: Int,
    maxFloors: Int
) {

    // we're using this variable to get the height of the image composable
    // and apply the same height to LandmarkProgressTrack() composable,
    // Arbitrarily setting the initial value to 200.dp for previews. This will
    // be overridden by the image loaded during runtime
    var maxProgressTrackHeight by remember { mutableStateOf(200.dp) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Black.copy(0.6f)),
        contentAlignment = Alignment.Center
    ) {

        val screenHeight = maxHeight
        val screenWidth = maxWidth

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = screenHeight * 0.061f,
                    bottom = screenHeight * 0.061f,
                    start = screenHeight * 0.033f,
                    end = screenHeight * 0.033f
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Heading Section
            Box(
                Modifier
                    .weight(1f)
                    .align(Alignment.Start),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomStart),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = title,
                        style = DynamicTheme.typography.textStyle3,
                        fontSize = 30.sp.computeSize(),
                        color = Color.White,
                        modifier = Modifier
                            .padding(bottom = screenHeight*0.0089f)
                            .testTag("Landmarks:Workout:BuildingName:Text")
                    )

                    Text(
                        text = "$maxFloors " + stringResource(R.string.floors_tc),
                        style = DynamicTheme.typography.textStyle3,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp.computeSize(),
                        color = Color.White,
                        modifier = Modifier.testTag("Landmarks:Workout:FloorGoal:Text")
                    )

                    Spacer(modifier = Modifier.height(screenHeight * 0.018f))
                }
            }

            // Progress Animation and Progress track section
            Box (contentAlignment = Alignment.BottomCenter){
                Box(
                    modifier = Modifier
                        .heightIn(max = screenHeight * 0.58f) // setting the maximum height
                        .fillMaxWidth(350 / 507f)
                ) {
                    LandmarkProgressAnimation(
                        progress = progress,
                        filledUri = filledUri,
                        emptyUri = emptyUri
                    ) {
                        if(it.value > 0) {
                            maxProgressTrackHeight = it
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Side min max floor section
                    Column(
                        modifier = Modifier.height(maxProgressTrackHeight),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = "$maxFloors",
                            style = DynamicTheme.typography.textStyle3,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp.computeSize(),
                            color = Color.White,
                            modifier = Modifier.testTag("Landmarks:Workout:Progress:FloorGoal:Text")
                        )

                        Text(
                            text = ZERO,
                            style = DynamicTheme.typography.textStyle3,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp.computeSize(),
                            color = Color.White
                        )
                    }

                    // floor Progress track
                    Box(
                        modifier = Modifier
                            .wrapContentHeight()
                            .padding(start = screenWidth * 0.029f)
                            .align(Alignment.Bottom)
                    ) {
                        LandmarkProgressTrack(progress, maxProgressTrackHeight)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "$currentFloor",
                        style = DynamicTheme.typography.textStyle3,
                        fontWeight = FontWeight.Medium,
                        fontSize = 120.sp.computeSize(),
                        color = Color.White.copy(0.4f),
                        modifier = Modifier.testTag("Landmarks:Workout:CurrentFloor:Text")
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color(0x40D8D8D8))

            // Bottom landmark place description/facts about the place
            Box(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Start),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .zIndex(3f),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(modifier = Modifier.height(screenHeight * 0.018f))

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {

                        Image(
                            painter = painterResource(R.drawable.location_no_fill),
                            contentDescription = "location icon",
                            modifier = Modifier
                                .width(screenWidth * 0.0282f)
                                .wrapContentHeight()
                                .align(Alignment.CenterVertically)
                        )

                        Spacer(Modifier.padding(start = screenWidth * 0.0176f))

                        Text(
                            text = locationString,
                            style = DynamicTheme.typography.textStyle3,
                            fontSize = 18.sp.computeSize(),
                            color = Color.White,
                            modifier = Modifier.testTag("Landmarks:Workout:Location:Text")
                        )
                    }

                    Spacer(modifier = Modifier.height(screenHeight * 0.018f))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Text(
                                text = description,
                                style = DynamicTheme.typography.textStyle3,
                                fontSize = 16.sp.computeSize(),
                                color = Color.White,
                                modifier = Modifier.testTag("Landmarks:Workout:Description:Text")
                            )
                        }
                    }

                }
            }
        }
    }
}

@Device22inPreviews
@Composable
private fun LandmarksProgressOverlayPreview(){
    LandmarkProgressOverlay(
        modifier = Modifier
            .width(567.dp)
            .height(900.dp),
        "Eiffel Tower",
        "Description",
        "Paris France",
        Uri.EMPTY,
        Uri.EMPTY,
        0.5f,
        10,
        103
    )
}