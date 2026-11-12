package com.jht.spinbikeapps.targettraining.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.jht.enums.Gender
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.WattsLevelDropdown
import com.jht.spinbikeapps.targettraining.common.FitnessLevel
import com.jht.spinbikeapps.targettraining.common.TargetTrainingToggleButton
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.common.ZoneButton
import com.jht.spinbikeapps.targettraining.model.TargetSetupData
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.spinbikeapps.targettraining.utils.HeartRateTarget
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BG_3
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TEXT_TITLE_SECONDARY
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
internal fun GenericTargetSetupScreen(
    targetSetupData: TargetSetupData,
    navController: NavController? = null,
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
    LaunchedEffect(targetSetupData.route) {
        onModeChanged(UserFtpMode.UserFtpKnown)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {}
            }
            .background(DynamicTheme.colors.gray1)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(DynamicTheme.colors.gray2)
                .align(Alignment.BottomCenter)
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart),
                thickness = 1.dp.computeSize(1920f),
                color = DynamicTheme.colors.gray6
            )
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
                    onClick(targetSetupData.workoutMode)
                }, shape = RoundedCornerShape(18.dp.computeSize(1920f)),
                colors = ButtonDefaults.buttonColors(
                    DynamicTheme.colors.primary
                ),
                modifier = Modifier
                    .padding(vertical = 18.dp.computeSize(1920f))
                    .wrapContentSize()
                    .align(Alignment.BottomCenter)
                    .size(width = 340.dp.computeSize(1920f), height = 64.dp.computeSize(1920f))
                    .testTag(startWorkoutButtonTag(targetSetupData.workoutMode))
            ) {
                Text(
                    stringResource(R.string.start_tc),
                    color = Color.Black,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 24.sp.computeSize()),
                )
            }
        }
        Column(modifier = Modifier.wrapContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 120.dp.computeSize(1920f),
                        end = 120.dp.computeSize(1920f),
                        top = 126.dp.computeSize(1920f)
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_tt), contentDescription = null,
                    modifier = Modifier.clickable { navController?.popBackStack() })
                Text(
                    text = stringResource(targetSetupData.titleRes),
                    fontSize = 42.sp.computeSize(1920f),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route || targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) {
                TargetTrainingToggleButton(
                    modifier = Modifier.padding(
                        start = 250.dp.computeSize(1920f),
                        bottom = 40.dp.computeSize(1920f)
                    ),
                    title = if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route){stringResource(R.string.know_your_ftp_tc)} else{stringResource(R.string.know_your_max_hr_tc)},
                    selectedMode = ftpMode,
                    onModeChanged = { onModeChanged(it)}
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 250.dp.computeSize(1920f),
                        end = 250.dp.computeSize(1920f),
                        top =
                            if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route || targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) {
                                (0.dp.computeSize(1920f))
                            } else {
                                171.dp.computeSizeHd()
                            }
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .size(
                            width = 876.dp.computeSize(1920f),
                            height = 230.dp.computeSize(1920f)
                        )
                        .background(
                            DynamicTheme.colors.gray2,
                            shape = RoundedCornerShape(18.dp.computeSize(1920f))
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 48.dp.computeSize(1920f),
                                vertical = 64.dp.computeSize(1920f)
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                PillBox(
                                    modifier = Modifier
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
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp.computeSize(1920f))
                        .size(
                            width = 524.dp.computeSize(1920f),
                            height = 230.dp.computeSize(1920f)
                        )
                        .background(
                            DynamicTheme.colors.gray2,
                            shape = RoundedCornerShape(18.dp.computeSize(1920f))
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp.computeSize(1920f))
                    ) {
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp.computeSizeHd())
                                    .background(
                                        BG_3,
                                        shape = RoundedCornerShape(20.dp.computeSizeHd())
                                    )
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_light_bulb),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(26.dp.computeSizeHd())
                                        .align(Alignment.Center)
                                )
                            }
                            Text(
                                stringResource(R.string.helpful_tip_tc),
                                fontSize = 20.sp.computeSize(1920f),
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White,
                                modifier = Modifier.padding(start = 12.dp.computeSize(1920f)),
                            )
                        }
                        Text(
                            stringResource(targetSetupData.descriptionRes),
                            fontSize = 20.sp.computeSize(1920f),
                            fontFamily = FontFamily.SansSerif,
                            color = TEXT_TITLE_SECONDARY,
                            modifier = Modifier.padding(top = 32.dp.computeSize()),
                        )
                    }
                }
            }
            if (targetSetupData.route == TargetTrainingScreen.WattsScreen.route || targetSetupData.route == TargetTrainingScreen.HeartRateScreen.route) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 250.dp.computeSize(1920f))
                ) {
                    Text(
                        stringResource(R.string.target_zone_tc),
                        fontSize = 18.sp.computeSize(1920f),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(
                            top = 32.dp.computeSize(),
                            bottom = 16.dp.computeSize(1920f)
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
        }
    }
}

/** One screen serves all five target types, so the start button tag is keyed on the mode. Shared with the tests. */
internal fun startWorkoutButtonTag(mode: WorkoutMode) = "${mode.name}Setup:StartWorkout:Button"

@AllDevicePreviews
@Composable
private fun GenericTargetSetupScreenPreview() {
    GenericTargetSetupScreen(
        targetSetupData = TargetSetupData(
            titleRes = R.string.target_setup_tc,
            iconRes = R.drawable.ic_heart,
            descriptionRes = R.string.heart_rate_setup_description_pg,
            route = TargetTrainingScreen.HeartRateScreen.route,
            workoutMode = WorkoutMode.HeartRate,
            pillBoxes = listOf(),
            targetZoneRange = listOf()
        ),
        fitnessLevel = FitnessLevel.Low,
        gender = Gender.Male,
        onValueFitnessLevelChange = {},
        onValueGenderChange = {},
        onClick = {}
    )
}

internal object PillBoxIndex{
    const val PRIMARY_VALUE: Int = 0 // Contains Target Values
    const val TIME_TARGET: Int = 1
}