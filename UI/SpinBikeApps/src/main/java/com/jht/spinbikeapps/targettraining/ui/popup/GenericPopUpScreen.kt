package com.jht.spinbikeapps.targettraining.ui.popup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.jht.enums.Gender
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.WattsLevelDropdown
import com.jht.spinbikeapps.targettraining.common.FitnessLevel
import com.jht.spinbikeapps.targettraining.common.TargetTrainingToggleButton
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.common.ZoneButton
import com.jht.spinbikeapps.targettraining.model.TargetSetupData
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.spinbikeapps.targettraining.ui.setup.PillBoxIndex
import com.jht.spinbikeapps.targettraining.utils.HeartRateTarget
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_4
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_8
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TEXT_TITLE_PRIMARY
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WattsTarget
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.GenderInput
import com.jht.widgets.PillBox
import com.jht.widgets.PillBoxStyle

@Composable
internal fun GenericPopUpScreen(
    targetSetupData: TargetSetupData,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onClick: (WorkoutMode) -> Unit,
    fitnessLevel: FitnessLevel = FitnessLevel.Low,
    gender: Gender = Gender.Male,
    onValueFitnessLevelChange: (FitnessLevel) -> Unit = {},
    onValueGenderChange: (Gender) -> Unit = {},
    ftpMode: UserFtpMode = UserFtpMode.UserFtpKnown,
    onModeChanged: (UserFtpMode) -> Unit = {},
) {
    val pillBoxValues = remember(targetSetupData) {
        targetSetupData.pillBoxes.map {
            mutableFloatStateOf(it.defaultValue)
        }
    }

    var selectedZone by remember { mutableIntStateOf(0) }

    // show pill boxes based on the training screen and FTP availability.
    val visiblePillBoxes =
        if ((targetSetupData.route == TargetTrainingScreen.WattsScreen.route ||
                    targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) &&
            ftpMode == UserFtpMode.UserFtpKnown
        ) {
            targetSetupData.pillBoxes.take(1)
        } else if (ftpMode == UserFtpMode.UserFtpUnknown) {
            targetSetupData.pillBoxes.drop(1).take(2)
        } else {
            targetSetupData.pillBoxes
        }
    Box(
        modifier = modifier
            .width(1208.dp.computeSize(1920f))
            .wrapContentHeight()
            .background(
                color = GRAY_2,
                shape = RoundedCornerShape(18.dp.computeSize(1920f))
            )
            .border(
                width = 2.dp.computeSize(1902f),
                color = GRAY_8,
                shape = RoundedCornerShape(18.dp.computeSize(1920f))
            )

    ) {
        Column(modifier = Modifier.wrapContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 21.dp.computeSize(1920f),
                        horizontal = 24.dp.computeSize(1920f)
                    ), verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(targetSetupData.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp.computeSizeHd())
                )
                Text(
                    stringResource(R.string.target_setup_tc),
                    fontSize = 22.sp.computeSize(1920f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = TEXT_TITLE_PRIMARY,
                    modifier = Modifier.padding(start = 12.dp.computeSize(1920f)),
                )
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(),
                thickness = 1.dp.computeSize(1920f),
                color = DynamicTheme.colors.gray6
            )
            if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route || targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) {
                TargetTrainingToggleButton(
                    modifier = modifier.padding(
                        start = 32.dp.computeSize(1920f),
                        top = 32.dp.computeSizeHd()
                    ),
                    title = if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route){stringResource(R.string.know_your_ftp_tc)} else{stringResource(R.string.know_your_max_hr_tc)},
                    selectedMode = ftpMode,
                    onModeChanged = {onModeChanged(it)}
                )
            }
            Box(
                modifier = Modifier
                    .padding(
                        horizontal = 40.dp.computeSize(1920f),
                        vertical = 32.dp.computeSize(1920f)
                    )
                    .fillMaxWidth()
                    .height(230.dp.computeSize(1920f))
                    .background(
                        GRAY_4,
                        shape = RoundedCornerShape(18.dp.computeSize(1920f))
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 48.dp.computeSize(1920f),
                            vertical = 64.dp.computeSize(1920f)
                        ),
                    horizontalArrangement = Arrangement.Start
                ) {
                    if ((targetSetupData.route == TargetTrainingScreen.WattsScreen.route) &&
                        ftpMode == UserFtpMode.UserFtpUnknown
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GenderInput(
                                modifier = Modifier.width(350.dp.computeSizeHd()),
                                value = gender,
                                onValueChange = { onValueGenderChange(it) })
                            WattsLevelDropdown(
                                modifier = Modifier.width(350.dp.computeSizeHd()),
                                value = fitnessLevel,
                                onValueChange = { onValueFitnessLevelChange(it)}
                            )
                        }
                    } else {
                        visiblePillBoxes.fastForEachIndexed { index, pillBox ->
                            if (index > 0) {
                                Spacer(
                                    modifier = Modifier.width(80.dp.computeSize(1920f))
                                )
                            }
                            PillBox(
                                modifier = Modifier
                                    .padding(start = 40.dp.computeSize(1920f))
                                    .height(98.dp.computeSize(1920f))
                                    .width(330.dp.computeSize(1920f)),
                                pillboxValue = if (ftpMode == UserFtpMode.UserFtpKnown) {
                                    pillBoxValues[index]
                                } else {
                                    pillBoxValues[index + 1]
                                },
                                titleLabel = pillBox.title,
                                unitLabel = pillBox.unitValue,
                                stepValue = pillBox.stepValue,
                                decimalPlaces = pillBox.decimalPlaces,
                                displayType = pillBox.displayType,
                                min = pillBox.min,
                                max = pillBox.max,
                                forceOnlyStepValues = true,
                                usePercentageBar = true,
                                testTag = pillBox.testTag,
                                style = PillBoxStyle(
                                    borderStroke = BorderStroke(
                                        1.dp.computeSize(1920f),
                                        color = Color.White
                                    ),
                                    dividerStroke = BorderStroke(
                                        1.dp.computeSize(1920f),
                                        color = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp.computeSize(1920f)),
                                    labelTextStyle = TextStyle.Default.copy(color = Color.White),
                                    valueBackground = Color.Black,
                                    valueTextStyle = TextStyle.Default.copy(color = Color.White),
                                    valueIconId = com.jht.widgets.R.drawable.ic_keypad_black,
                                    valueBar = DynamicTheme.colors.primary,
                                    buttonBackground = Color.Black,
                                    buttonForeground = DynamicTheme.colors.primary
                                )
                            )
                        }
                    }
                }
            }

            if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route || targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 40.dp.computeSize(1920f))
                ) {
                    Text(
                        stringResource(R.string.target_zone_tc),
                        fontSize = 18.sp.computeSize(1920f),
                        color = TEXT_TITLE_PRIMARY,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = modifier.padding(
                            bottom = 8.dp.computeSize(1920f)
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp.computeSize(1920f))
                    ) {
                        targetSetupData.targetZoneRange.forEachIndexed { index, zone ->
                            ZoneButton(
                                modifier = Modifier.weight(1f),
                                zone = zone.first,
                                textColor = zone.second,
                                isSelected = selectedZone == index,
                                onClick = {
                                    selectedZone = index
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth(),
                thickness = 1.dp.computeSize(1920f),
                color = DynamicTheme.colors.gray6
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp.computeSize(1920f))
                    .background(GRAY_4),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onCancel() },
                    shape = RoundedCornerShape(18.dp.computeSize(1920f)),
                    colors = ButtonDefaults.buttonColors(
                        DynamicTheme.colors.gray7
                    ),
                    modifier = Modifier
                        .size(
                            width = 246.dp.computeSize(1920f),
                            height = 64.dp.computeSize(1920f)
                        )
                        .padding(end = 16.dp.computeSize(1920f))
                        .testTag("Calories:Cancel:Button")
                ) {
                    Text(
                        stringResource(R.string.cancel_tc),
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 24.sp.computeSize(
                                1920f
                            )
                        ),
                    )
                }
                Button(
                    onClick = {
                        when (targetSetupData.workoutMode) {
                            WorkoutMode.Rpm -> {
                                TargetTrainingTypeData.selectedMode = WorkoutMode.Rpm
                                TargetTrainingTypeData.rpmTarget = pillBoxValues[PillBoxIndex.PRIMARY_VALUE].floatValue
                            }

                            WorkoutMode.Watts -> {
                                TargetTrainingTypeData.selectedMode = WorkoutMode.Watts
                                val selectedTargetZone =
                                    targetSetupData.targetZoneRange[selectedZone].first
                                TargetTrainingTypeData.wattsTarget = WattsTarget(
                                    targetZone = selectedTargetZone,
                                    watts = pillBoxValues[PillBoxIndex.PRIMARY_VALUE].floatValue
                                )
                            }

                            WorkoutMode.HeartRate -> {
                                TargetTrainingTypeData.selectedMode = WorkoutMode.HeartRate
                                val selectedTargetZone =
                                    targetSetupData.targetZoneRange[selectedZone].first
                                TargetTrainingTypeData.heartRateTarget = HeartRateTarget(
                                    targetZone = selectedTargetZone,
                                    maxHr = pillBoxValues[PillBoxIndex.PRIMARY_VALUE].floatValue
                                )
                            }

                            WorkoutMode.Calories -> {
                                TargetTrainingTypeData.selectedMode = WorkoutMode.Calories
                                TargetTrainingTypeData.caloriesTarget = pillBoxValues[PillBoxIndex.PRIMARY_VALUE].floatValue
                            }

                            WorkoutMode.Distance -> {
                                TargetTrainingTypeData.selectedMode = WorkoutMode.Distance
                                TargetTrainingTypeData.distanceTarget = pillBoxValues[PillBoxIndex.PRIMARY_VALUE].floatValue
                                TargetTrainingTypeData.timeTarget = pillBoxValues[PillBoxIndex.TIME_TARGET].floatValue.toInt()
                            }

                            WorkoutMode.None -> {}
                        }
                        onClick(targetSetupData.workoutMode)},
                    shape = RoundedCornerShape(18.dp.computeSize(1920f)),
                    colors = ButtonDefaults.buttonColors(
                        DynamicTheme.colors.primary
                    ),
                    modifier = Modifier
                        .size(width = 246.dp.computeSize(1920f), height = 64.dp.computeSize(1920f))
                        .testTag("Calories:StartWorkout:Button")
                ) {
                    Text(
                        stringResource(R.string.save_tc),
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 24.sp.computeSize(
                                1920f
                            )
                        ),
                    )
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun GenericPopUpScreenPreview(){
    val targetSetupData = TargetSetupData(
        titleRes = R.string.title_tc,
        iconRes = R.drawable.mock_icon,
        descriptionRes = R.string.description_tc,
        route = TargetTrainingScreen.RpmScreen.route,
        workoutMode = WorkoutMode.Rpm,
        pillBoxes = listOf(),
        targetZoneRange = listOf()
    )
    GenericPopUpScreen(
        targetSetupData = targetSetupData,
        onCancel = {},
        modifier = Modifier,
        fitnessLevel = FitnessLevel.Low,
        gender = Gender.Male,
        onValueFitnessLevelChange = {},
        onValueGenderChange = {},
        onClick = {}
    ) { }
}