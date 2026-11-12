package com.jht.workoutselection.vortex.parts.mainscreen

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jht.data.WorkoutBadge
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.resources.robotoFonts
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.DialogFullscreen
import com.jht.widgets.FocusCropImage
import com.jht.widgets.PillboxGrid
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.helpers.LoadingWorkoutAnimation
import com.jht.workoutselection.R
import com.jht.workoutselection.vortex.parts.ThemedBadgeView

@Composable
internal fun WorkoutSelectionModal(
    previewImage: Any?,
    title: String,
    description: String,
    workoutStartingInputs:List<WorkoutInputUi>,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    startSelectedWorkout: () -> Unit,
    showingModal: MutableState<Boolean>,
    workoutBadge: WorkoutBadge,
    badgeLabel: String = title
){
    ModalLoadingBase(
        showingModal = showingModal,
        isLoadingWorkout = workoutStartingInputs.isEmpty(),
        content = { modifier ->
            WorkoutSelectionModalContent(
                modifier = modifier,
                title = title,
                previewImage = previewImage,
                description = description,
                workoutStartingInputs = workoutStartingInputs,
                workoutInputLevelInfoList = workoutInputLevelInfoList,
                startSelectedWorkout = startSelectedWorkout,
                workoutBadge = workoutBadge,
                badgeLabel = badgeLabel,
            )
        }
    )
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ModalLoadingBase(
    showingModal: MutableState<Boolean>,
    isLoadingWorkout: Boolean,
    showOnSelected: Boolean = true,
    showCloseButton: Boolean = true,
    content: @Composable (modifier:Modifier) -> Unit
){
    if(!showOnSelected || (showingModal.value)) {
        // wrap ModalLoadingBase content in a native DialogFullscreen window.
        // preventing touch freezing and overlay collision bugs on the console.
        DialogFullscreen(
            onDismissRequest = { showingModal.value = false }
        ) {
            // box around modal with tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .7f))
                    .clickableNoEffect(
                        onClick = { showingModal.value = false },
                    ),
                contentAlignment = Alignment.Center
            ) {

                val modifier = Modifier
                    .padding(horizontal = 169.dp.computeSize(), vertical = 54.dp.computeSize())
                    .fillMaxSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .clickableNoEffect(
                        onClick = {/* do nothing, so clicking inside the settings window doesn't dismiss it */ },
                    )
                    .semantics { testTagsAsResourceId = true }
                LoadingWorkoutAnimation(modifier, isLoadingWorkout) {
                    content(it)
                    if (showCloseButton) {
                        Image(
                            painter = painterResource(R.drawable.round_icon_button_close_enabled),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(
                                    top = 54.dp.computeSize() - 5.dp.computeSize(),
                                    end = 169.dp.computeSize() - 5.dp.computeSize()
                                )
                                .clickable { showingModal.value = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkoutSelectionModalContent(
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutStartingInputs:List<WorkoutInputUi>,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    startSelectedWorkout: () -> Unit,
){
    // only show level info if there is a level and we have details for it
    val levelInput = workoutStartingInputs.find { it.identifier.contains(ConsoleWorkoutVariables.LEVEL) }
    val levelInfoBoxShowing = levelInput != null && workoutInputLevelInfoList.isNotEmpty()

    Row(
        modifier = modifier
    ) {
        // image on left
        FocusCropImage(
            modifier = Modifier
                .fillMaxSize()
                .weight(2f),
            model = imageUri,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(3f)
                .padding(
                    horizontal = 50.dp.computeSize(),
                    vertical = 20.dp.computeSize()
                )
        ) {
            Text(
                text = title,
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("TrainingTab:SelectedWorkoutTitle:Text"),
            )
            Spacer(modifier = Modifier.size(height = 20.dp.computeSize(), width = 20.dp.computeSize()))
            Text(
                text = description,
                color = DynamicTheme.colors.text1,
                fontFamily = robotoFonts,
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.size(height = 20.dp.computeSize(), width = 20.dp.computeSize()))

            PillboxGrid(
                modifier = Modifier
                    .aspectRatio(ratio = 1.7f)
                    .fillMaxWidth(),
                workoutInputList = workoutStartingInputs,
                itemModifierProvider = { Modifier },
                spacing = 20.dp.computeSize(),
            )

            Spacer(modifier = Modifier.size(height = 30.dp.computeSize(), width = 20.dp.computeSize()))

            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                StartWorkoutButton(
                    onClick = {
                        startSelectedWorkout()
                    },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .height(60.dp.computeSize())
                        .testTag("WorkoutSelection:StartWorkout:Button")
                )
                Spacer(Modifier.weight(1f))
                // LEVEL INFO
                if (levelInfoBoxShowing) {
                    val level = levelInput.currentValue.value
                    LevelInfo(
                        level = level,
                        workoutInputLevelInfoList = workoutInputLevelInfoList,
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                }
            }
        }
    }
}

/** UI component for displaying info for a workout's level */
@Composable
private fun LevelInfo(
    level: Float,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
    ) {
        Text(
            text = String.format(stringResource(R.string.concat_2_space),stringResource(R.string.level_tc),level.toInt()),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle1,
            textAlign = TextAlign.Start,
        )
        for (i in workoutInputLevelInfoList.indices) {
            LevelInfoItem(
                name = String.format(
                    stringResource(R.string.concat_2_space),
                    stringResource(R.string.average_tc),
                    workoutInputLevelInfoList[i].name
                ),
                value = workoutInputLevelInfoList[i].average
            )
            LevelInfoItem(
                name = String.format(
                    stringResource(R.string.concat_2_space),
                    stringResource(R.string.max_tc),
                    workoutInputLevelInfoList[i].name
                ),
                value = workoutInputLevelInfoList[i].max
            )
        }
    }
}
/** Presents a name/value pair for [LevelInfo] */
@Composable
private fun LevelInfoItem(
    name: String,
    value: String,
) {
    Row {
        Text(
            text = name,
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle1,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 2.dp.computeSize())
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle2,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 2.dp.computeSize())
        )
    }
}

@Composable
fun WorkoutSelectionModalContent(
    modifier: Modifier,
    title: String,
    previewImage: Any?,
    description: String,
    workoutStartingInputs: List<WorkoutInputUi>,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    startSelectedWorkout: () -> Unit,
    workoutBadge: WorkoutBadge,
    badgeLabel: String = title
) {
    val baseTag = "TCVSetupModal"
    val levelInput = workoutStartingInputs.find { it.identifier.contains(ConsoleWorkoutVariables.LEVEL) }
    val levelInfoBoxShowing = levelInput != null && workoutInputLevelInfoList.isNotEmpty()
    val level = levelInput?.currentValue?.value ?: 1f

    Row(
        modifier = modifier.semantics { testTagsAsResourceId = true }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(2f)
        ) {
            FocusCropImage(
                modifier = Modifier.fillMaxSize(),
                model = previewImage,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(3.2f)
        ) {
            // Blurred background layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(80.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            //content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 42.dp.computeSize(),
                        start = 24.dp.computeSize(),
                        end = 24.dp.computeSize(),
                        bottom = 48.dp.computeSize()
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = robotoFonts,
                        fontWeight = FontWeight.Normal,
                        fontSize = 36.sp.computeSize(),
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.testTag("$baseTag:SelectedWorkoutTitle:Text"),
                )
                Spacer(Modifier.height(12.dp.computeSize()))
                Text(
                    text = description,
                    color = Color(0xCCEFEFEF), // this shade is not defined in codebase
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = robotoFonts,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp.computeSize(),
                        lineHeight = 1.4.em,
                        letterSpacing = 0.02.sp,
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.testTag("$baseTag:DescriptionTitle:Text")
                )
                Row(
                    modifier = Modifier.padding(top = 20.dp.computeSize()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemedBadgeView(
                        badge = workoutBadge,
                        modifier = Modifier
                            .size(32.dp.computeSizeHd())
                    )
                    Text(
                        text = badgeLabel,
                        color = Color(0xFFD1D1D1), // this shade is not defined in the codebase
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = robotoFonts,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp.computeSize(),
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("$baseTag:SwitchStrideLabel:Text")
                    )
                }
                Spacer(Modifier.height(32.dp.computeSize()))

                PillboxGrid(
                    modifier = Modifier.height(186.dp.computeSize()),
                    workoutInputList = workoutStartingInputs,
                    rows = 2,
                    columns = 2,
                    itemModifierProvider = { Modifier },
                    spacing = 20.dp.computeSize(),
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (levelInfoBoxShowing) {
                        LevelInfo(
                            level = level,
                            workoutInputLevelInfoList = workoutInputLevelInfoList,
                            modifier = Modifier.fillMaxWidth(0.5f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    StartWorkoutButton(
                        onClick = { startSelectedWorkout() },
                        modifier = Modifier
                            .height(36.dp.computeSize())
                            .width(183.dp.computeSize()),
                        buttonColor = Color(0xFFFFCB27),
                        buttonTitle = R.string.start_workout_tc,
                        btnTextColor = DynamicTheme.colors.background1,
                        btnShape = MaterialTheme.shapes.medium.copy(CornerSize(18.dp)),
                        btnTextStyle = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 16.sp.computeSize()
                        ),
                    )
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun WorkoutSelectionModalContentPreview() {
    val mock = MockWorkoutCatalogModel()

    MaterialTheme {
        WorkoutSelectionModalContent(
            modifier = Modifier.fillMaxSize(),
            title = "Title",
            description = "A level based 20-minute High Intensity Interval Training (HIIT) workout program that automatically adjusts the exercise intensity to maximize results.",
            previewImage = Uri.EMPTY,
            workoutStartingInputs = mock.getWorkoutInputsForPreview(),
            workoutInputLevelInfoList = mock.getWorkoutInputLevelInfo(0),
            startSelectedWorkout = {},
            workoutBadge = WorkoutBadge()
        )
    }
}
