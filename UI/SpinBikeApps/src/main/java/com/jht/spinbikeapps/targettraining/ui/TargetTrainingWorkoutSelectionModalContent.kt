package com.jht.spinbikeapps.targettraining.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.jht.data.WorkoutInputUi
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.resources.robotoFonts
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.TargetTrainingStartWorkoutButton
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.theme.DynamicTheme
import com.jht.ui.Device22inPreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.FocusCropImage
import com.jht.widgets.PillboxGrid

private const val BASE_TAG = "TargetTrainingSetupModal"

/**
 * Setup modal shown as the start destination of [TargetTrainingNavHost]. Dismissal is owned by the
 * enclosing modal container, so taps on the card itself are swallowed rather than handled here.
 */
@Composable
internal fun TargetTrainingWorkoutSelectionModalContent(
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    startSelectedWorkout: () -> Unit,
    content: @Composable (startSelectedWorkout: () -> Unit) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .background(
                    color = TargetTrainingColors.SCRIM,
                    shape = RoundedCornerShape(30.dp)
                )
                .fillMaxSize(0.7f)
                .height(710.dp.computeSize())
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures {}
                }
        ) {
            Row(
                modifier = Modifier.semantics {
                    testTagsAsResourceId = true
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.8f)
                ) {
                    FocusCropImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(
                                shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp),
                                color = Color.Transparent
                            ),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(3.2f)
                        .padding(
                            top = 36.dp.computeSize(),
                            start = 24.dp.computeSize(),
                            end = 24.dp.computeSize(),
                            bottom = 24.dp.computeSize()
                        ),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = robotoFonts,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp.computeSize(),
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("$BASE_TAG:SelectedWorkoutTitle:Text"),
                    )
                    Spacer(Modifier.height(12.dp.computeSize()))
                    Text(
                        text = description,
                        color = TargetTrainingColors.TEXT_DESCRIPTION,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = robotoFonts,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp.computeSize(1920f),
                            lineHeight = 1.4.em,
                            letterSpacing = 0.02.sp,
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("$BASE_TAG:DescriptionTitle:Text")
                    )
                    Spacer(Modifier.height(20.dp.computeSize()))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_target_training_type),
                            contentDescription = stringResource(id = R.string.training_workout_icon_cd),
                            modifier = Modifier
                                .size(25.dp.computeSize())
                                .testTag("$BASE_TAG:TrainingTypeIcon:Image")
                        )
                        Text(
                            text = stringResource(id = R.string.target_training_tc),
                            color = TargetTrainingColors.TEXT_BADGE_LABEL,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = robotoFonts,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp.computeSize(),
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.testTag("$BASE_TAG:TrainingTypeLabel:Text")
                        )
                    }
                    Spacer(Modifier.height(15.dp.computeSize()))

                    PillboxGrid(
                        modifier = Modifier
                            .height(80.dp.computeSize())
                            .aspectRatio(4f),
                        workoutInputList = workoutInputList,
                        rows = 1,
                        columns = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        content { startSelectedWorkout() }
                    }
                }
            }
        }
    }
}

@Device22inPreviews
@Composable
private fun TargetTrainingModalPreview() {
    val mock = MockWorkoutCatalogModel()
    TargetTrainingWorkoutSelectionModalContent(
        title = "Title",
        description = "A level based 20-minute High Intensity Interval Training (HIIT) workout program that automatically adjusts the exercise intensity to maximize results.",
        imageUri = Uri.EMPTY,
        workoutInputList = mock.getWorkoutInputsForPreview(),
        startSelectedWorkout = {},
        // Mirrors the modifier the enclosing modal container supplies at runtime.
        modifier = Modifier
            .background(
                color = DynamicTheme.colors.background4,
                shape = RoundedCornerShape(30.dp)
            )
            .border(0.2.dp, Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(30.dp))
            .clickableNoEffect(
                onClick = { }, //do nothing, so clicking inside the settings window doesn't dismiss it
            )
            .fillMaxSize(),
        content = { onClick -> TargetTrainingStartWorkoutButton(onClick = onClick) }
    )
}
