package com.jht.spinbikeapps.targettraining.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jht.enums.Gender
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.CurrentTargetComp
import com.jht.spinbikeapps.targettraining.common.FitnessLevel
import com.jht.spinbikeapps.targettraining.common.TabView
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.model.BottomTab
import com.jht.spinbikeapps.targettraining.model.TabBarItem
import com.jht.spinbikeapps.targettraining.model.TargetSetupData
import com.jht.spinbikeapps.targettraining.ui.popup.GenericPopUpScreen
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BG_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.ORANGE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.RED_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.unit.round

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun TargetTrainingMainScreen(
    selectedTab: String,
    rpm: Float? = 0f,
    watts: Int? = 0,
    averageWatts: Int? = 0,
    heartRate: Float? = 0f,
    maxHeartRate: Float = 0f,
    userFtp: Int = 0,
    calories: Float = 0f,
    heartRatePercentage: Float = 0f,
    currentDistance: Float = 0f,
    totalGoalDistance: Float = 0f,
    targetDistance: Float? = null,
    targetTimeLabel: String? = null,
    isMetric: Boolean = true,
    bottomTabs: List<TabBarItem>,
    totalDistance: Float,
    rpmData: () -> TargetSetupData,
    wattsData: () -> TargetSetupData,
    heartRateData: () -> TargetSetupData,
    caloriesData: () -> TargetSetupData,
    distanceData: () -> TargetSetupData,
    onUpdateSelectedMode: (WorkoutMode) -> Unit,
    onClickSavedCalories: () -> Unit,
    fitnessLevel: FitnessLevel = FitnessLevel.Low,
    gender: Gender = Gender.Male,
    onValueFitnessLevelChange: (FitnessLevel) -> Unit = {},
    onValueGenderChange: (Gender) -> Unit = {},
    ftpMode: UserFtpMode = UserFtpMode.UserFtpKnown,
    onModeChanged: (UserFtpMode) -> Unit = {},
    onClickEndWorkout: () -> Unit = {},
    onClickContinue: () -> Unit = {},
    isPopUpVisible: Boolean = false,
    totalDistanceCovered : Float = 0f
) {
    val rpmTarget = TargetTrainingTypeData.rpmTarget
    val caloriesTarget = TargetTrainingTypeData.caloriesTarget
    val wattsTarget = TargetTrainingTypeData.wattsTarget
    val heartRateTarget = TargetTrainingTypeData.heartRateTarget
    val activeMode = TargetTrainingTypeData.selectedMode
    val componentTag = "TTMainScreen"
    val tabBarItems = bottomTabs.map {
        TabBarItem(
            title = it.title,
            selectedIcon = it.selectedIcon,
            unselectedIcon = it.unselectedIcon,
            route = it.route,
            workoutMode = it.workoutMode
        )
    }
    val navController = rememberNavController()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_1)
            .semantics {
                testTagsAsResourceId = true
            },
    ) {
        var activePopup by remember {
            mutableStateOf<TargetSetupData?>(null)
        }
        Scaffold(
            bottomBar = {
                TabView(tabBarItems, navController)
            },
            containerColor = Color.Black
        ) {
            LaunchedEffect(selectedTab) {
                navController.navigate(selectedTab) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            NavHost(navController = navController, startDestination = selectedTab) {
                composable(bottomTabs[BottomTab.RPM.ordinal].route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.rpm_uc),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp.computeSize(1920f),
                            ),
                            color = DynamicTheme.colors.gray4,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 56.dp.computeSizeHd(),
                                    bottom = 41.dp.computeSizeHd()
                                )
                        )
                        CurrentTargetComp(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .testTag("$componentTag:RpmData:CurrentTargetComp"),
                            onClick = { activePopup = rpmData() },
                            target = if (activeMode == WorkoutMode.Rpm) {
                                rpmTarget.toInt().toString()
                            } else {
                                "- -"
                            }
                        )
                        if (activeMode == WorkoutMode.Rpm) {
                            RPMTargetMeter(
                                modifier = Modifier.align(Alignment.Center),
                                rpm = rpm ?: 0f,
                                targetRpm = rpmTarget,
                                maxRpm = rpmData().pillBoxes.first().max
                            )
                        } else {
                            RPMNoTargetMeter(
                                modifier = Modifier.align(Alignment.Center),
                                rpm = rpm ?: 0f
                            )
                        }
                    }
                }
                composable(bottomTabs[BottomTab.WATTS.ordinal].route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.watts_tc),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp.computeSize(1920f),
                            ),
                            color = DynamicTheme.colors.gray4,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 56.dp.computeSizeHd(),
                                    bottom = 41.dp.computeSizeHd()
                                )
                        )
                        CurrentTargetComp(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .testTag("$componentTag:WattsData:CurrentTargetComp"),
                            color = YELLOW_1,
                            onClick = { activePopup = wattsData();onModeChanged(UserFtpMode.UserFtpKnown) },
                            target = (if (activeMode == WorkoutMode.Watts) {
                                wattsTarget?.targetZone
                            } else {
                                "- -"
                            }).toString(),
                            subtitle = if (activeMode == WorkoutMode.Watts)  stringResource(R.string.tt_ftp_colon_uc) else "",
                            subtitleValue = if (activeMode == WorkoutMode.Watts) {
                                wattsTarget?.watts?.toInt().toString()
                            } else {
                                ""
                            }
                        )
                        if (activeMode == WorkoutMode.Watts) {
                            ConstantWattsWithTarget(
                                modifier = Modifier.align(Alignment.Center),
                                watts = watts ?: 0,
                                averageWatts = averageWatts ?: 0,
                                userFtp = userFtp,
                                targetZoneIndex = wattsTarget?.let { target ->
                                    wattsData().targetZoneRange.indexOfFirst{ it.first == target.targetZone }
                                }?.takeIf { it != -1 }
                            )
                        } else {
                            ConstantWattsNoTargetMeter(
                                modifier = Modifier.align(Alignment.Center),
                                watts = watts ?: 0,
                                averageWatts = averageWatts ?: 0,
                                userFtp = userFtp
                            )
                        }
                    }
                }
                composable(bottomTabs[BottomTab.HEART_RATE.ordinal].route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.heart_rate_tc),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp.computeSize(1920f),
                            ),
                            color = DynamicTheme.colors.gray4,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 56.dp.computeSizeHd(),
                                    bottom = 41.dp.computeSizeHd()
                                )
                        )
                        CurrentTargetComp(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .testTag("$componentTag:HeartRateData:CurrentTargetComp"),
                            color = RED_1,
                            onClick = { activePopup = heartRateData(); onModeChanged(UserFtpMode.UserFtpKnown) },
                            target = if (activeMode == WorkoutMode.HeartRate) {
                                heartRateTarget?.targetZone
                            } else {
                                "- -"
                            }.toString(),
                            subtitle = if (activeMode == WorkoutMode.HeartRate) stringResource(R.string.max_heart_rate_tc) else "",
                            subtitleValue = if (activeMode == WorkoutMode.HeartRate) {
                                heartRateTarget?.maxHr?.toInt()?.toString().orEmpty()
                            } else ""
                        )
                        HeartRateComp(
                            modifier = Modifier.align(Alignment.Center),
                            hr = heartRate ?: 0f,
                            maxHr = heartRateTarget?.maxHr
                                ?: maxHeartRate,
                            targetZoneIndex = heartRateTarget
                                ?.takeIf { activeMode == WorkoutMode.HeartRate }
                                ?.let { target ->
                                    heartRateData().targetZoneRange
                                        .indexOfFirst { it.first == target.targetZone }
                                        .takeIf { it != -1 }
                                }
                        )
                    }
                }
                val currentTargetCalories = if (TargetTrainingTypeData.caloriesTarget != null) {
                    caloriesTarget?.toInt().toString()
                } else {
                    "- -"
                }

                composable(bottomTabs[BottomTab.CALORIES.ordinal].route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CaloriesScreen(
                            modifier = Modifier.align(Alignment.Center),
                            calories = calories,
                            targetCalories = TargetTrainingTypeData.caloriesTarget,
                            activeMode = activeMode,
                            onClickEndWorkout = onClickEndWorkout,
                            onClickContinue = onClickContinue,
                            isPopUpVisible = isPopUpVisible
                        )
                        Text(
                            stringResource(R.string.calories_tc),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp.computeSize(1920f),
                            ),
                            color = DynamicTheme.colors.gray4,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 56.dp.computeSizeHd(),
                                    bottom = 41.dp.computeSizeHd()
                                )
                        )
                        CurrentTargetComp(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .testTag("$componentTag:CaloriesData:CurrentTargetComp"),
                            color = ORANGE_1,
                            onClick = { activePopup = caloriesData() },
                            target = currentTargetCalories
                        )
                    }
                }
                composable(bottomTabs[BottomTab.DISTANCE.ordinal].route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.distance_tc),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp.computeSize(1920f),
                            ),
                            color = DynamicTheme.colors.gray4,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 56.dp.computeSizeHd(),
                                    bottom = 41.dp.computeSizeHd()
                                )
                                .zIndex(20f)
                        )
                        DistanceScreen(
                            modifier = Modifier.align(Alignment.Center),
                            currentDistance = currentDistance,
                            totalDistance = totalDistance,
                            targetDistance = if (activeMode == WorkoutMode.Distance) targetDistance else null,
                            targetTimeLabel = if (activeMode == WorkoutMode.Distance) targetTimeLabel else null,
                            isMetric = isMetric,
                            onClickEndWorkout = onClickEndWorkout,
                            onClickContinue = onClickContinue,
                            isPopUpVisible = isPopUpVisible,
                            totalDistanceCovered = totalDistanceCovered
                        )
                        val currentTargetDistance = if (activeMode == WorkoutMode.Distance) {
                            targetDistance?.let { displayDistance ->
                                "${displayDistance.round(2)} ${
                                    if (isMetric) {
                                        stringResource(R.string.km_lc)
                                    } else {
                                        stringResource(R.string.mi_lc)
                                    }
                                }"
                            } ?: "_ _"
                        } else {
                            "- -"
                        }
                        CurrentTargetComp(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .zIndex(10f)
                                .testTag("$componentTag:DistanceData:CurrentTargetComp"),
                            color = GREEN_1,
                            onClick = { activePopup = distanceData() },
                            target = currentTargetDistance
                        )
                    }
                }
            }
        }
        activePopup?.let { popupData ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                GenericPopUpScreen(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp.computeSizeHd())
                        .testTag("$componentTag:PopUpData:GenericPopUpScreen"),
                    onCancel = { activePopup = null },
                    targetSetupData = popupData,
                    onClick = {
                        if (popupData.workoutMode == WorkoutMode.Calories){onClickSavedCalories()}
                        onUpdateSelectedMode(popupData.workoutMode);
                        activePopup = null
                    },
                    fitnessLevel = fitnessLevel,
                    gender = gender,
                    onValueFitnessLevelChange = {onValueFitnessLevelChange(it)},
                    onValueGenderChange = {onValueGenderChange(it)},
                    ftpMode = ftpMode,
                    onModeChanged = {
                        onModeChanged(it)
                    }
                )
            }
        }
    }
}


@AllDevicePreviews
@Composable
private fun TargetTrainingMainScreenPreview(){

    fun targetSetupData() = TargetSetupData(
        titleRes = R.string.title_tc,
        iconRes = R.drawable.mock_icon,
        descriptionRes = R.string.description_tc,
        route = TargetTrainingScreen.RpmScreen.route,
        workoutMode = WorkoutMode.Rpm,
        pillBoxes = listOf(),
        targetZoneRange = listOf()
    )
    val selectedMode = TargetTrainingTypeData.selectedMode
    val selectedTab = when (selectedMode) {
        WorkoutMode.Rpm -> TargetTrainingScreen.RpmScreen.route
        WorkoutMode.Watts -> TargetTrainingScreen.WattsScreen.route
        WorkoutMode.HeartRate -> TargetTrainingScreen.HeartRateScreen.route
        WorkoutMode.Calories -> TargetTrainingScreen.CaloriesScreen.route
        WorkoutMode.Distance -> TargetTrainingScreen.DistanceScreen.route
        WorkoutMode.None -> TargetTrainingScreen.RpmScreen.route
    }
    TargetTrainingMainScreen(
        selectedTab = selectedTab,
        rpm = 0f,
        watts = 0,
        averageWatts = 0,
        heartRate = 10f,
        maxHeartRate = 100f,
        userFtp = 1,
        calories = 10f,
        heartRatePercentage = 10f,
        currentDistance = 10f,
        totalGoalDistance = 10f,
        targetDistance = 10f,
        targetTimeLabel = "",
        isMetric = true,
        bottomTabs = listOf(),
        totalDistance = 10f,
        onUpdateSelectedMode = {},
        rpmData = {targetSetupData()},
        wattsData = {targetSetupData()},
        heartRateData = {targetSetupData()},
        caloriesData = {targetSetupData()},
        distanceData = {targetSetupData()},
        onClickSavedCalories = {  },
        fitnessLevel = FitnessLevel.Low,
        gender = Gender.Male,
        onValueFitnessLevelChange = {},
        onValueGenderChange = {},
    )
}