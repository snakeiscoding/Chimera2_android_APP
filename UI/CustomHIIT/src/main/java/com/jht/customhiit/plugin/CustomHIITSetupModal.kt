package com.jht.customhiit.plugin

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.customhiit.R
import com.jht.customhiit.common.HiitUtils
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.data.WorkoutInput
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.resources.robotoFonts
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.PillBox
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.numpad.NumPadDisplay

/**
 * This is designed only for Custom HIIT, hence it contains few hardcoded values such as map key and workoutInputList index number
 */
@Composable
internal fun CustomHIITSetupModal(
    imageUri: Uri,
    workoutInputList: List<WorkoutInput>,
    inputModelForHIIT: HIITWorkoutMap,
    startSelectedWorkout: () -> Unit = {},
) {
    // Input parameters required for parameter setups such as Speed, Incline, and Resistance
    val (inputParameters, additionalInputParameters) = workoutInputList.partition { it.identifier != ConsoleWorkoutVariables.WEIGHT }
    Row(
        modifier = Modifier
            .fillMaxWidth(1800 / 1920f)
            .aspectRatio(1800 / 940f)
            .background(Color.Black)
            .testTag("WorkoutSelection:CustomHIITModal")
    ) {
        // image on left
        GlideAndPreviewImage(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(442 / 786f),
            uri = imageUri,
            contentScale = ContentScale.FillBounds
        )
        Box(modifier = Modifier.fillMaxSize().padding(48.dp.computeSizeHd())) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Heading
                HeaderText()

                HiitPillboxes(inputParameters, inputModelForHIIT, additionalInputParameters)

            }
            // Footer
            StartWorkoutButton(
                onClick = {
                    startSelectedWorkout()
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Title and descriptions for custom HIIT
 */
@Composable
private fun HeaderText() {
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp.computeSize())
    ) {
        Text(
            text = stringResource(id = R.string.hiit_tc),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle3,
            fontSize = 32.sp.computeSize(),
            textAlign = TextAlign.Start,
            modifier = Modifier.testTag("FeaturedTab:CustomHIITWorkout:Text"),
        )
        Text(
            text = stringResource(id = R.string.hiit_description_pg),
            color = DynamicTheme.colors.text1,
            fontFamily = robotoFonts,
            fontSize = 16.sp.computeSize(),
            textAlign = TextAlign.Start,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_custom_hiit),
                contentDescription = "Custom HIIT Icon",
                modifier = Modifier.size(25.dp.computeSize())
            )
            Text(
                text = stringResource(id = R.string.hiit_tc),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3,
                fontFamily = robotoFonts,
                fontSize = 20.sp.computeSize(),
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Rows of pillbox columns for the respective stats of speed/incline/resistance
 * Weight is added as a parameter below the first column
 */
@Composable
private fun HiitPillboxes(
    inputParameters: List<WorkoutInput>,
    inputModelForHIIT: HIITWorkoutMap,
    additionalInputParameters: List<WorkoutInput>
) {
    Row(modifier = Modifier.padding(top = 40.dp).clickable(onClick = {})) {
        //Loops through the three levels of easy, medium, and hard
        HIITIntervalEnum.entries.subList(0, 3).forEachIndexed { index, interval ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(15.dp.computeSize()),
            ) {
                Image(
                    painter = painterResource(id = if (index == 0) R.drawable.ic_custom_hiit_easy else if (index == 1) R.drawable.ic_custom_hiit_medium else R.drawable.ic_custom_hiit_hard),
                    contentDescription = "Workout Level",
                    modifier = Modifier.size(30.dp.computeSize())
                )
                //Adds pillboxes for the respective parameters of speed/incline/resistance
                inputParameters.forEachIndexed { mapIndex, input ->
                    inputModelForHIIT.intervalMap[interval]?.get(input.identifier)?.let {
                        PillBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2.7f),
                            pillboxValue = it,
                            titleLabel = input.label, // some labels have new lines in them, replace with space
                            unitLabel = input.unitString,
                            stepValue = input.stepSize,
                            decimalPlaces = input.precision,
                            displayType = NumPadDisplay.Number, // ConsoleWorkoutVariable
                            min = input.min,
                            max = input.max,
                            forceOnlyStepValues = input.identifier == ConsoleWorkoutVariables.START_INCLINE,
                            usePercentageBar = input.identifier != ConsoleWorkoutVariables.WEIGHT, // ConsoleWorkoutVariable, we don't show bar for weight
                            testTag = "${input.label}:$interval:",
                        )
                    }
                }
                if (index == 0) {
                    additionalInputParameters.forEachIndexed { mapIndex, input ->
                        inputModelForHIIT.intervalMap[HIITIntervalEnum.ADDITIONAL_FIELD]?.get(input.identifier)
                            ?.let {
                                PillBox(
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .fillMaxWidth()
                                        .aspectRatio(2.7f),
                                    pillboxValue = it,
                                    titleLabel = input.label, // some labels have new lines in them, replace with space
                                    unitLabel = input.unitString,
                                    stepValue = input.stepSize,
                                    decimalPlaces = input.precision,
                                    displayType = NumPadDisplay.Number, // ConsoleWorkoutVariable
                                    min = input.min,
                                    max = input.max,
                                    usePercentageBar = false, // ConsoleWorkoutVariable, we don't show bar for weight
                                    testTag = "${input.label}:",
                                )
                            }
                    }
                }

            //Divider lines
            }
            if (index in 0..1) {
                VerticalDivider(
                    modifier = Modifier
                        .padding(18.dp)
                        .width(2.dp.computeSize())
                        .fillMaxHeight(0.6f),
                    color = DynamicTheme.colors.gray3
                )
            }
        }
    }
}

/**
 * Custom HIIT previews
 */
@AllDevicePreviews
@Composable
private fun CustomHIITWorkoutSelectionModalPreview() {
    CustomHIITSetupModal(
        imageUri = Uri.EMPTY,
        workoutInputList = HiitUtils.getHIITWorkoutInputsForPreview(),
        inputModelForHIIT = HiitUtils.getHIITWorkoutMapForPreview()
    )
}