package com.jht.workoutselection.vortex.parts.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jht.data.FeaturedWorkoutAd
import com.jht.data.WorkoutItem
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.workoutselection.R
import kotlin.math.ceil
import kotlin.math.min

/**
 * Sets up the featured and training UI. They use the same composables with minor
 * differences for the overall list size and title labels
 */
@Composable
internal fun FeaturedTrainingSetup(
    isFeatured: Boolean = false,
    carouselItems: List<CarouselItem>,
    workoutItems: List<WorkoutItem>,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
    onClickViewAll: () -> Unit = {},
) {
    //We are only currently showing two rows of featured items for the Vortex UI
    val featuredItems = if (isFeatured) {
        workoutItems.take(2 * WORKOUT_ITEM_COUNT_PER_ROW)
    }
    else {
        workoutItems
    }

    //Featured UI also has two titles between the rows of items
    val rowTitles = if (isFeatured) {
        listOf(
            stringResource(R.string.recommended_for_you_tc),
            stringResource(R.string.virtual_active_tc)
        )
    } else listOf()

    CarouselAndWorkoutGrid(
        carouselItems = carouselItems,
        workoutItems = featuredItems,
        rowTitles = rowTitles,
        onSelectWorkoutItem = onSelectWorkoutItem,
        onClickViewAll = onClickViewAll
    )
}

/**
 * Composable for the carousel and workout grid
 * To prevent nested scrolling and because the first row of items overlaps with the
 * carousel background, a [LazyColumn] is used to layout the items instead of a [LazyGrid]
 */
@Composable
private fun CarouselAndWorkoutGrid(
    carouselItems: List<CarouselItem>,
    workoutItems: List<WorkoutItem>,
    rowTitles: List<String> = listOf(),
    onSelectWorkoutItem: (WorkoutItem) -> Unit,
    onClickViewAll: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DynamicTheme.colors.gray1),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1920 / 788f)
            ) {
                //Carousel UI
                CarouselComponent(
                    items = carouselItems,
                ) { item ->
                    when (item.tag) {
                        is WorkoutItem -> onSelectWorkoutItem(item.tag)
                        is FeaturedWorkoutAd -> item.tag.onClick()
                    }
                }

                //The first row overlaps with the carousel so it must be within the same item
                WorkoutRowComponent(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .align(Alignment.BottomCenter),
                    items = workoutItems.take(7),
                    title = rowTitles.firstOrNull(),
                    onClick = onSelectWorkoutItem,
                )
            }
        }

        //Vortex UI shows 7 items per row
        val numberOfRows = ceil(workoutItems.size / WORKOUT_ITEM_COUNT_PER_ROW.toDouble()).toInt()

        //Add as many rows as needed for the remaining workout items,
        for(row in 1 until (numberOfRows)) {
            item {
                //Gets the next row of items, doing a bounds check to prevent exceeding the list size
                val items = workoutItems.subList(row * WORKOUT_ITEM_COUNT_PER_ROW,
                    min((row + 1)* WORKOUT_ITEM_COUNT_PER_ROW, workoutItems.size))
                WorkoutRowComponent(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    items = items,
                    title = rowTitles.getOrNull(row),
                    showTitleButton = true,
                    onClick = onSelectWorkoutItem,
                    onClickViewAll = onClickViewAll
                )
            }
        }
        //Extra spacing so all UI is displayed when scrolling to the bottom
        item{
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1920 / 108f)
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewFeaturedSetup() {
    val mock = MockWorkoutCatalogModel()
    FeaturedTrainingSetup(
        isFeatured = true,
        carouselItems = mock.getCarouselWorkouts.map { CarouselItem.fromWorkoutItem(it) },
        workoutItems = mock.getFeaturedWorkouts,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewTrainingSetup() {
    val mock = MockWorkoutCatalogModel()
    FeaturedTrainingSetup(
        carouselItems = mock.getCarouselWorkouts.map { CarouselItem.fromWorkoutItem(it) },
        workoutItems = mock.getFeaturedWorkouts,
    )
}
