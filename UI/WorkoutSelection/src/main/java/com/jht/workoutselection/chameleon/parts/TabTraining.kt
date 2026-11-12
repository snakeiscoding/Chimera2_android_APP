package com.jht.workoutselection.chameleon.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.LevelDescriptionBox
import com.jht.widgets.LockedOverlay
import com.jht.widgets.PillboxGrid
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.helpers.LoadingWorkoutAnimation
import com.jht.widgets.helpers.TabItem
import com.jht.workoutselection.CategoryWorkoutItems
import com.jht.workoutselection.R
import com.jht.workoutselection.TabTrainingKey
import com.jht.workoutselection.common.parts.TabBase


// Pass any data needed through the create function to feed into composable?
@Composable
internal fun createTabTraining(
    workoutCategories: List<CategoryWorkoutItems>,
    workoutItem:WorkoutItem?,
    workoutInputs:List<WorkoutInputUi> = listOf(),
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    isLoadingWorkout: Boolean,
    startSelectedWorkout: () -> Unit,
    onSelectWorkoutItem: (WorkoutItem?) -> Unit = {},
): TabItem {
    return TabItem(
        { selected, onSelect ->
            TabBase(R.drawable.ic_man_running_outline, R.drawable.ic_man_running, stringResource(R.string.training_exercises_tc), selected, "tab$TabTrainingKey", onSelect)
        },
        {

            LaunchedEffect(workoutCategories) {
                // only runs on initial launch of this composable
                val firstItem = workoutCategories.firstOrNull()?.items?.firstOrNull()
                onSelectWorkoutItem(firstItem) // set manual as selectedWorkoutItem on tab load
            }

            TrainingTab(
                workoutCategories,
                workoutItem,
                workoutInputs,
                workoutInputLevelInfoList,
                isLoadingWorkout = isLoadingWorkout,
                startSelectedWorkout,
                onSelectWorkoutItem,
            )
        },
        TabTrainingKey
    )
}

@Composable
@AllDevicePreviews
private fun PreviewTrainingTab() {
    val mockWorkoutCatalogModel = MockWorkoutCatalogModel()
    val categories = listOf(
        CategoryWorkoutItems(R.string.training_tc, mockWorkoutCatalogModel.getVAWorkouts),
        CategoryWorkoutItems(R.string.goals_tc, mockWorkoutCatalogModel.getVAWorkouts),
        CategoryWorkoutItems(R.string.marathon_tc, mockWorkoutCatalogModel.getVAWorkouts),
        CategoryWorkoutItems(R.string.target_heart_rate_tc, mockWorkoutCatalogModel.getVAWorkouts),
        CategoryWorkoutItems(R.string.fitness_test_tc, mockWorkoutCatalogModel.getVAWorkouts),
    )
    TrainingTab(
        categories,
        mockWorkoutCatalogModel.getVAWorkouts[0],
        mockWorkoutCatalogModel.getWorkoutInputsForPreview(),
        mockWorkoutCatalogModel.getWorkoutInputLevelInfo(0),
        isLoadingWorkout = false,
        {},
        {}
    )
}


@Composable
private fun TrainingTab(
    workoutCategories: List<CategoryWorkoutItems>,
    selectedWorkoutItem: WorkoutItem?,
    workoutInputList: List<WorkoutInputUi>,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    isLoadingWorkout:Boolean = false,
    startSelectedWorkout: () -> Unit,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
){
    Row(Modifier.fillMaxSize()) {
        // SCROLLING LIST ON LEFT
        ScrollingTrainingExercises(
            modifier = Modifier
                .fillMaxHeight()
                .width(213.dp.computeSize())
                .background(DynamicTheme.colors.background4),
            selectedWorkoutItem = selectedWorkoutItem,
            onSelectWorkoutItem = onSelectWorkoutItem,
            workoutCategories = workoutCategories,
        )

        // BOX WITH DATA ON RIGHT
        if (selectedWorkoutItem != null) LoadingWorkoutAnimation(Modifier.fillMaxHeight().weight(1f), isLoadingWorkout) { modifier->
            Column(
                modifier,
            ){
                // TOP HALF (picture and desc)
                SelectedWorkoutTopHalf(
                    selectedWorkoutItem = selectedWorkoutItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
                // BOTTOM HALF (pillboxes and gender select and start button)
                SelectedWorkoutBottomHalf(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    workoutInputList = workoutInputList,
                    startSelectedWorkout = startSelectedWorkout,
                    selectedWorkoutItem = selectedWorkoutItem,
                    workoutInputLevelInfoList = workoutInputLevelInfoList,
                )
            }
        }
    }
    if (workoutCategories.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SelectedWorkoutTopHalf(
    modifier: Modifier,
    selectedWorkoutItem: WorkoutItem,
) {
    Box(modifier) {
        // BACKGROUND
        GlideAndPreviewImage(modifier = Modifier.fillMaxSize(),
            uri = selectedWorkoutItem.background[0], contentScale = ContentScale.Fit)
        // GRADIENT (for easily readable white text)
        Box(Modifier.fillMaxSize().background(DynamicTheme.colors.gradient8.vertical()))
        // TITLE and DESCRIPTION
        Column(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomStart)
            .padding(
                start = 75.dp.computeSize(),
                bottom = 15.dp.computeSize(),
                end = 100.dp.computeSize()
            )
        ) {
            Text(
                text = stringResource(selectedWorkoutItem.titleId),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("TrainingTab:SelectedWorkout:Text"),
            )
            Spacer(modifier = Modifier.size(height = 10.dp.computeSize(), width = 20.dp.computeSize()))
            Text(
                text = stringResource(selectedWorkoutItem.descriptionId),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle1,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SelectedWorkoutBottomHalf(
    modifier: Modifier,
    workoutInputList:List<WorkoutInputUi>,
    startSelectedWorkout: () -> Unit,
    selectedWorkoutItem: WorkoutItem,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
) {
    val levelInput = workoutInputList.find { it.identifier.contains(ConsoleWorkoutVariables.LEVEL) }
    val levelInfoBoxShowing = levelInput != null && workoutInputLevelInfoList.isNotEmpty()
    val level = levelInput?.currentValue?.value ?: 1f

    Box(modifier) {
        Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    bottom = 20.dp.computeSize(),
                    start = 20.dp.computeSize(),
                    end = 20.dp.computeSize()
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // PILLBOXES
            PillboxGrid(
                modifier = Modifier.weight(1f),
                workoutInputList = workoutInputList,
            )
            Spacer(Modifier.weight(0.1f))
            // LEVEL DESCRIPTION AND START WORKOUT BUTTON
            Column(
                Modifier
                    .weight(0.3f)
                    .padding(10.dp) // match pillbox padding
            ) {
                // LEVEL INFO
                if (levelInfoBoxShowing) {
                    LevelDescriptionBox(
                        modifier = Modifier,
                        level = level,
                        workoutInputLevelInfoList = workoutInputLevelInfoList,
                    )
                }
                Spacer(Modifier.weight(1f))
                // START WORKOUT BUTTON
                StartWorkoutButton(
                    { startSelectedWorkout() },
                    Modifier.testTag("TrainingTab:Start:Button"),
                    fullWidthText = false,
                )
            }
        }
    }
}

@Composable
private fun ScrollingTrainingExercises(
    modifier: Modifier,
    workoutCategories: List<CategoryWorkoutItems>,
    selectedWorkoutItem: WorkoutItem?,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
    ) {
        // title
        item {
            Text(
                text = stringResource(R.string.exercises_in_uc),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle2,
                        textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp.computeSize())
            )
        }
        workoutCategories.forEach { workoutCategory ->
            //Ignore categories with empty workout lists
            if(workoutCategory.items.isNotEmpty()) {
                item {
                    ScrollingWorkoutCategory(workoutCategory.titleId)
                }
                workoutCategory.items.forEach { workoutItem ->
                    item {
                        ScrollingWorkoutItem(workoutItem, selectedWorkoutItem, onSelectWorkoutItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollingWorkoutCategory(
    titleId: Int,
) {
    Text(
        text = stringResource(titleId),
        color = DynamicTheme.colors.text1,
        style = DynamicTheme.typography.textStyle2,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp.computeSize())
    )
}

@Composable
private fun ScrollingWorkoutItem(
    workoutItem: WorkoutItem,
    selectedWorkoutItem: WorkoutItem?,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 32.dp.computeSize(),
                vertical = 10.dp.computeSize()
            )
            .clickable {
                onSelectWorkoutItem(workoutItem)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // if selected highlight border with gold
        val imageModifier = if (selectedWorkoutItem == workoutItem) {
            Modifier.border(
                    width = 2.dp.computeSize(),
                    color = DynamicTheme.colors.primary
                )
        } else {
            Modifier
        }
        LockedOverlay(
            isLocked = workoutItem.locked,
            modifier = imageModifier
                .aspectRatio(1.5f)
                .fillMaxWidth(),
            lockTestTag = "TrainingTab:${workoutItem.workoutId}:Lock",
        ) {
            GlideAndPreviewImage(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("TrainingTab:${workoutItem.workoutId}:Image"),
                uri = workoutItem.background[0], // creative wants this to match the banner image (our thumbnails and backgrounds are not the same for every workout)
                contentScale = ContentScale.Crop // background is wider than space available, need to crop or else black bars on top and below image
            )
        }

        Text(
            text = stringResource(workoutItem.titleId),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp.computeSize())
                .testTag("TrainingTab:${workoutItem.workoutId}:Text")
        )
    }
}