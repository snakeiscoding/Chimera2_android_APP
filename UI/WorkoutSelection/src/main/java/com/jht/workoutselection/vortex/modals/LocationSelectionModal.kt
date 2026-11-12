package com.jht.workoutselection.vortex.modals

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutItem
import com.jht.resources.robotoFonts
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.GlideAndPreviewImage
import com.jht.workoutselection.R
import com.jht.workoutselection.vortex.parts.ThemedBadgeView

/**
 * LocationSelectionModal is being added to cater the Location selection dialog for Virtual Active and Landmarks,
 * This acts as the first phase of workout selection, post selection of the location it will lead user to phase 2 which is standard modal with pillbox grid and start workout button
 */
@Composable
fun LocationSelectionModal(
    modifier: Modifier = Modifier,
    workoutItems: List<WorkoutItem>,
    onSelectWorkout: (WorkoutItem) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val bgLandmark = Brush.horizontalGradient(defaultDynamicTheme.colors.gradient7.colors)
    val bgSelectionCardView = Brush.verticalGradient(defaultDynamicTheme.colors.gradient1.colors)
    val isLandmarks = workoutItems.firstOrNull() is LandmarkWorkoutItem
    val interactionSource = remember { MutableInteractionSource() }


    Column(
        modifier = modifier
            .fillMaxSize(.80f)
            .background(Color.Unspecified, RoundedCornerShape(8.dp))
            .clickable(   //This is here to not have the onclick of the background to close the modal to be called when not clicking a workout item card
                onClick = {},
                indication = null,
                interactionSource = interactionSource,
            )
            .testTag("WorkoutSelection:LocationSelectionModal")
            .semantics { testTagsAsResourceId = true }
    ) {
        TitleBanner(bgLandmark, isLandmarks, onClose)
        // Lazy load all the locations
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .background(bgSelectionCardView)
                .padding(top = 30.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("Landmarks:Selection:Card:View"),
            verticalArrangement = Arrangement.spacedBy(32.dp.computeSize())
        ) {
            itemsIndexed(workoutItems) { index, item ->
                Log.d("LocationSelectionModal", "itemBackground: ${item.thumbnail}, itemTitle: ${item.titleId}")
                val title = LocalContext.current.getString(item.titleId)
                val imageUri =
                    if (item.background.isNotEmpty()) {
                        item.background[0]
                    } else {
                        Uri.EMPTY
                    }

                LocationCard(
                    title = title,
                    imageUri = item.thumbnail, //imageUri,
                    onSelection = { location ->
                        onSelectWorkout(location)
                    },
                    workoutItem = item,
                    testTag = "Landmarks:Selection:Card",
                    index = index
                )
            }

        }

    }
}

@Composable
private fun TitleBanner(bgLandmark: Brush, isLandmarks: Boolean, onClose: () -> Unit) {
    // Dynamic Header Resources
    val headerIcon = if (isLandmarks) R.drawable.ic_landmark else R.drawable.ic_virtual_active_logo
    val headerText = if (isLandmarks) R.string.help_landmarks_pg else R.string.help_va_pg

    Column(
        modifier = Modifier
            .fillMaxHeight(0.32f)
            .fillMaxWidth()
            .background(bgLandmark)
            .padding(start = 60.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, end = 45.dp), horizontalArrangement = Arrangement.End){
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.width(40.dp.computeSizeHd()).height(40.dp.computeSizeHd())
                    .clickable{ onClose() }
            )
        }
        Image(
            painterResource(headerIcon), contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.38f)
                .fillMaxHeight(0.45f)

        )
        Text(
            stringResource(headerText),
            fontSize = 16.sp.computeSize(),
            color = Color.White,
            modifier = Modifier.padding(top = 20.dp)
        )
    }
}

@Composable
fun LocationCard(
    onSelection: (WorkoutItem) -> Unit = {},
    title: String,
    imageUri: Uri,
    workoutItem: WorkoutItem,
    testTag: String,
    index: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(intrinsicSize = IntrinsicSize.Max)
            .height(intrinsicSize = IntrinsicSize.Max)
            .clickable(indication = null, interactionSource = interactionSource,) {
                onSelection(workoutItem)
            }
            .testTag("$testTag:Item:$index"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
//                .aspectRatio(1f)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.TopStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                GlideAndPreviewImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .background(Color.Transparent)
                        .testTag("$testTag:Image:$index")
                        .align(Alignment.Center),
                    uri = imageUri,
                    contentScale = ContentScale.FillHeight,
                )
                ThemedBadgeView(
                    badge = workoutItem.badge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp.computeSizeHd())
                        .size(24.dp.computeSizeHd())
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = robotoFonts,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp.computeSize(),
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("$testTag:Text:$index"),
            maxLines = 2
        )

        if (workoutItem is LandmarkWorkoutItem) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stairs),
                    contentDescription = "Stairs Icon",
                    modifier = Modifier.size(20.dp.computeSize())
                )
                Spacer(modifier = Modifier.width(8.dp.computeSize()))
                Text(
                    text = workoutItem.floors.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = robotoFonts,
                        fontWeight = FontWeight.Normal, //Zeplin says to use Input Text, was unable to find it so using Normal instead.
                        fontSize = 18.sp.computeSize(),
                    ),
                    color = Color(0xFFD1D1D1), //this specific shade of grey, grey15 wasn't defined in our code, so hardcoding it here.
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("$testTag:Floors:$index")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun LocationSelectionModalPreview() {
    LocationSelectionModal(
        workoutItems = listOf(
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.chichen_itza_tc,
                locationTitle = R.string.chichen_itza_location_tc
            ),
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.eiffel_tower_tc,
                locationTitle = R.string.eiffel_tower_location_tc
            ),
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.rock_of_gibraltar_tc,
                locationTitle = R.string.rock_of_gibraltar_location_tc
            ),
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.chichen_itza_tc,
                locationTitle = R.string.chichen_itza_location_tc
            ),
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.eiffel_tower_tc,
                locationTitle = R.string.eiffel_tower_location_tc
            ),
            LandmarkWorkoutItem(
                0,
                "",
                titleId = R.string.rock_of_gibraltar_tc,
                locationTitle = R.string.rock_of_gibraltar_location_tc
            ),
        ),
    )
}

@AllDevicePreviews
@Composable
private fun VirtualActiveLocationSelectionModalPreview() {
    LocationSelectionModal(
        workoutItems = listOf(
            WorkoutItem(0, "", titleId = R.string.chichen_itza_tc),
            WorkoutItem(0, "", titleId = R.string.eiffel_tower_tc),
            WorkoutItem(0, "", titleId = R.string.rock_of_gibraltar_tc),
            WorkoutItem(0, "", titleId = R.string.chichen_itza_tc),
            WorkoutItem(0, "", titleId = R.string.eiffel_tower_tc),
            WorkoutItem(0, "", titleId = R.string.rock_of_gibraltar_tc),

//            WorkoutItem(0, "", titleId = R.string.alberta_rockies_run_tc),
//            WorkoutItem(0, "", titleId = R.string.american_northeast_run_tc),
//            WorkoutItem(0, "", titleId = R.string.american_southwest_2_hike_tc),
//            WorkoutItem(0, "", titleId = R.string.argentine_patagonia_run_tc),
//            WorkoutItem(0, "", titleId = R.string.bay_area_bike_tc),
//            WorkoutItem(0, "", titleId = R.string.big_island_run_tc),
        ),
    )
}