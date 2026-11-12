package com.jht.workoutselection.vortex.parts.mainscreen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jht.data.WorkoutItem
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.LockedOverlay
import com.jht.workoutselection.R
import com.jht.workoutselection.vortex.parts.ThemedBadgeView

const val WORKOUT_ITEM_COUNT_PER_ROW = 7

/**
 * Row of workout items for the featured and training pages
 * Includes an optional title header for the row
 */
@Composable
internal fun WorkoutRowComponent(
    modifier: Modifier = Modifier,
    items: List<WorkoutItem>,
    title: String? = null,
    showTitleButton: Boolean = false,
    onClick: (WorkoutItem)->Unit = {},
    onClickViewAll: () -> Unit = {},
){
    Column (modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        title?.let {
            TitleHeaderRow(it, showTitleButton, onClickViewAll)
        }
        Row {
            for (i in 0 until WORKOUT_ITEM_COUNT_PER_ROW) {
                if (i < items.size) {
                    WorkoutCard(
                        modifier = Modifier.weight(1f).height(intrinsicSize = IntrinsicSize.Max),
                        item = items[i],
                        onClick = onClick
                    )
                } else {
                    //Add spacing if the number of items is less than the row
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Optional row header text and button for featured UI
 */
@Composable
private fun TitleHeaderRow(
    string: String,
    showTitleButton: Boolean,
    onClickViewAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(75 / 77f)
            .padding(bottom = 20.dp)
    ) {
        Text(
            text = string,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        if (showTitleButton) {
            Row(
                modifier = Modifier
                    .clickable { onClickViewAll() }
                    .align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.view_all_tc),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
                Icon(
                    painter = painterResource(R.drawable.caret_right),
                    contentDescription = "view all right arrow icon",
                    modifier = Modifier.size(24.dp.computeSize()),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Image and title text for each workout
 */
@Composable
internal fun WorkoutCard(
    modifier: Modifier = Modifier,
    item: WorkoutItem,
    onClick: (WorkoutItem)->Unit = {},
) {
    val testTag = if (item.optionalId.isEmpty()) item.workoutId else "${item.workoutId}:${item.optionalId}"

    Column(modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            shape = RoundedCornerShape(12.dp.computeSize()),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth(9 / 11f)
                .aspectRatio(1f)
                .clickable { onClick(item) }
                .testTag("WorkoutSelection:WorkoutCard:${item.workoutId}")
        ) {
            Box {
                LockedOverlay(
                    isLocked = item.locked,
                    modifier = Modifier
                        .fillMaxWidth(),
                    lockTestTag = "FeaturedWorkout:$testTag:Lock",
                ) {
                    GlideAndPreviewImage(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("FeaturedWorkout:$testTag:Image"),
                        uri = if(item.thumbnail != Uri.EMPTY) item.thumbnail else if (item.background.isNotEmpty()) item.background[0] else Uri.EMPTY,
                        contentScale = ContentScale.Crop
                    )
                }

                ThemedBadgeView(
                    badge = item.badge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp.computeSizeHd())
                        .size(24.dp.computeSizeHd())
                )
            }
        }
        Text(
            text = stringResource(item.titleId),
            style = MaterialTheme.typography.labelMedium,
            color = DynamicTheme.colors.buttonTertiaryDefault,
            modifier = Modifier
                .fillMaxWidth(9 / 11f)
                .padding(top = 8.dp.computeSize(), bottom = 16.dp.computeSize()),
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewFeaturedItems() {
    val mock = MockWorkoutCatalogModel()
    WorkoutRowComponent(
        title = "Test Row Header",
        modifier = Modifier.background(Color.Black),
        items = mock.getFeaturedWorkouts,
    )
}