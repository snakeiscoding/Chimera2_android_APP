package com.jht.spinbikeapps.targettraining

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.jht.enums.Gender
import com.jht.enums.WorkoutStage
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.FitnessLevel
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.model.FtpZone
import com.jht.spinbikeapps.targettraining.model.HeartRateSelectedZone
import com.jht.spinbikeapps.targettraining.model.HeartRateZoneRange
import com.jht.spinbikeapps.targettraining.model.PillBoxData
import com.jht.spinbikeapps.targettraining.model.TabBarItem
import com.jht.spinbikeapps.targettraining.model.TargetSetupData
import com.jht.spinbikeapps.targettraining.model.TargetTrainingSetupData
import com.jht.spinbikeapps.targettraining.model.WattsZoneRange
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.spinbikeapps.targettraining.ui.topbar.TargetTrainingPanelData
import com.jht.spinbikeapps.targettraining.ui.topbar.TargetTrainingTopBarHolder
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode.Companion.toTTType
import com.jht.theme.ThemedViewModel
import com.jht.unit.kilometerUnit
import com.jht.unit.mileUnit
import com.jht.widgets.numpad.NumPadDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class TargetTrainingViewModel @Inject constructor(
    private val workoutModel: WorkoutModel,
    private val uiBaseModel: UIBaseModel,
    private val uiServiceModel: UIServiceModel,
    val navigationModel: NavigationModel,
    @ApplicationContext private val context: Context,
    private val catalogModel: WorkoutCatalogModel,
    private val systemModel: SystemModel,
) : ThemedViewModel() {
    val rpm by lazy { mutableIntStateOf(0) }
    val heartRate by lazy { mutableIntStateOf(0) }
    private val paceStartCalories = mutableFloatStateOf(0f)
    private val totalCalories = mutableFloatStateOf(0f)
    val targetCalories = mutableFloatStateOf(0f)
    val watts by lazy { mutableFloatStateOf(0f) }
    val averageWatts by lazy { mutableFloatStateOf(0f) }
    val userFtp by lazy { mutableIntStateOf(0) }
    val maxHeartRate by lazy { mutableFloatStateOf(0f) }
    val rpmTarget by lazy { mutableIntStateOf(0) }
    val rpmZoneLow by lazy { mutableFloatStateOf(0f) }
    val rpmZoneHigh by lazy { mutableFloatStateOf(0f) }

    val ftpZoneLow by lazy { mutableFloatStateOf(0f) }
    val ftpZoneHigh by lazy { mutableFloatStateOf(0f) }
    val bpmZoneLow by lazy { mutableIntStateOf(0) }
    val bpmZoneHigh by lazy { mutableIntStateOf(0) }
    val targetBpm by lazy { mutableIntStateOf(0) }
    val targetCaloriesUpdate by lazy { mutableIntStateOf(0) }
    val targetHeartRatePercent by lazy { mutableFloatStateOf(0f) }
    val gender by lazy { mutableStateOf(Gender.DEFAULT) }
    val fitnessLevel by lazy { mutableStateOf(FitnessLevel.DEFAULT) }

    // ftpZoneLookupTable and ftpEstimateLookupTable arrays was provided in old implementation to calculate FTP
    private val ftpZoneLookupTable = arrayOf(
        intArrayOf(106, 91, 76, 56, 0),
        intArrayOf(120, 105, 90, 75, 55)
    )
    private val ftpEstimateLookupTable = arrayOf(
        intArrayOf(328, 250, 200, 175, 162),
        intArrayOf(221, 170, 153, 127, 110)
    )
    private val panelHolder by lazy { mutableStateOf<TargetTrainingTopBarHolder?>(null) }

    val currentDistance by lazy { mutableFloatStateOf(0f) }
    val totalGoalDistance by lazy { mutableFloatStateOf(0f) }
    val timeElapsedSeconds by lazy { mutableFloatStateOf(0f) }
    val isMetric by lazy { mutableStateOf(true) }

    val targetDistance by lazy { mutableFloatStateOf(0f) }

    val totalDistanceMax: Float
        get() = if (isMetric.value) workoutModel.targetDistanceMaxKm
        else workoutModel.targetDistanceMaxKm.kilometerUnit.toMiles
    val targetTimeGoalMinutes by lazy { mutableIntStateOf(0) }
    val isTargetTrainingActive by lazy { mutableStateOf(false) }

    private val paceStartDistance by lazy { mutableFloatStateOf(0f) }
    private val paceStartTimeSeconds by lazy { mutableFloatStateOf(0f) }
    val isConfirmationPopUpVisible by lazy { mutableStateOf(false) }

    // Distance relative to when target was set (always starts from 0 at that moment)
    val relativeDistance: Float
        get() {
            val distance = if (isTargetTrainingActive.value) {
                (currentDistance.floatValue - paceStartDistance.floatValue)
                    .coerceAtLeast(0f)
            } else {
                currentDistance.floatValue
            }
            return distance
        }

    val totalDistance by lazy { mutableFloatStateOf(0f) }

    override fun start() {
        initStat()
        handleTopBarVisibility()
        sendPanelUpdate(isVisible = false)
    }
    // Registers the panel once per workout session; null guard prevents duplicate managers.

    private fun handleTopBarVisibility() {
        if (panelHolder.value == null) {
            panelHolder.value = TargetTrainingTopBarHolder()
            panelHolder.value?.let { innerPanelHolder ->
                uiServiceModel.addUiPanel(innerPanelHolder)
            }
        }
    }

    private fun sendPanelUpdate(isVisible: Boolean) {
        val panelData = TargetTrainingPanelData(
            isTargetTrainingTopBarVisible = isVisible,
            selectedMode = TargetTrainingTypeData.selectedMode
        )
        uiServiceModel.sendUiPanelData((panelData))
    }


    private fun close() {
        uiServiceModel.close()
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        sendPanelUpdate(true)
        close()
        uiBaseModel.close()
        navigationModel.close()
        workoutModel.close()
    }

    val targetTimeLabel: String? by derivedStateOf {
        if (!isTargetTrainingActive.value) return@derivedStateOf null
        if (targetTimeGoalMinutes.intValue <= 0) return@derivedStateOf null
        val elapsed = timeElapsedSeconds.floatValue
        val current = relativeDistance
        if (elapsed <= 0f || current <= 0f) return@derivedStateOf null

        val targetPace = targetDistance.floatValue / (targetTimeGoalMinutes.intValue * 60f)
        val ghostDistance = targetPace * elapsed
        val distanceGap = current - ghostDistance
        val timeGapSeconds = (distanceGap * elapsed / current).toInt()
        val abs = kotlin.math.abs(timeGapSeconds)

        val hours = abs / 3600
        val minutes = (abs % 3600) / 60
        val seconds = abs % 60

        if (distanceGap >= 0) {
            if (hours > 0) {
                String.format("+%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("+%02d:%02d", minutes, seconds)
            }
        } else {
            if (hours > 0) {
                String.format("-%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("-%02d:%02d", minutes, seconds)
            }
        }
    }

    /**
     * Navigates the user back to the home (splash) screen.
     */
    fun onHome() {
        navigationModel.launchSplashScreen()
    }

    internal fun endWorkout(){
        workoutModel.stopWorkout()
    }
    internal fun onClickContinue(){
        if (selectedMode.value == WorkoutMode.Calories){
            isConfirmationPopUpVisible.value = false
            TargetTrainingTypeData.caloriesTarget = null
            updateSelectedMode(WorkoutMode.Calories)
        }
        else if (selectedMode.value == WorkoutMode.Distance){
            updateSelectedMode(WorkoutMode.Distance)
            isConfirmationPopUpVisible.value = false
            targetDistance.floatValue = 0f
            updateSelectedMode(WorkoutMode.Distance)
        }
    }

    internal fun onCaloriesTargetReached(currentCalories: Int) {
        val target = TargetTrainingTypeData.caloriesTarget?.toInt()
        if (target != null && currentCalories >= target) {
            isConfirmationPopUpVisible.value = true
        }
    }

    internal fun onDistanceTargetReached(currentDistance: Float) {
        if (isConfirmationPopUpVisible.value) return
        val target = targetDistance.floatValue
        if (target > 0f && currentDistance >= target) {
            isConfirmationPopUpVisible.value = true
        }
    }



    private fun initStat() {
        workoutModel.let {
            it.rpm.listenAndTrigger { value ->
                if (value != -1)
                    rpm.intValue = value
            }
            it.heartRate.listenAndTrigger { value ->
                heartRate.intValue = value
            }
            it.calories.listenAndTrigger { value ->
                totalCalories.floatValue = value
                targetCalories.floatValue =
                    max(totalCalories.floatValue - paceStartCalories.floatValue, 0f)
            }
            it.watts.listenAndTrigger { value ->
                watts.floatValue = value
            }
            it.averageWatts.listenAndTrigger { value ->
                averageWatts.floatValue = value
            }
            it.userFtp.listenAndTrigger { value ->
                userFtp.intValue = value
            }
            it.maxHeartRate.listenAndTrigger { value ->
                maxHeartRate.floatValue = value
            }
            it.rpmTarget.listenAndTrigger { value ->
                rpmTarget.intValue = value
            }
            it.targetHeartRatePercent.listenAndTrigger { value ->
                targetHeartRatePercent.floatValue = value
            }
            it.distanceKm.listenAndTrigger { value ->
                currentDistance.floatValue = if (isMetric.value) {
                    value
                } else {
                    value.kilometerUnit.toMiles
                }
            }
            it.timeElapsedMs.listenAndTrigger { value ->
                timeElapsedSeconds.floatValue = value / 1000f
            }
            it.isMetric.listenAndTrigger { value ->
                isMetric.value = value
            }
            it.workoutStage.listenAndTrigger { stage ->
                if (stage == WorkoutStage.COOL_DOWN) {
                    isTargetTrainingActive.value = false
                }
            }

            it.rpmZoneLow.listenAndTrigger { value ->
                rpmZoneLow.floatValue = value
            }

            it.rpmZoneHigh.listenAndTrigger { value ->
                rpmZoneHigh.floatValue = value
            }

            it.ftpZoneLow.listenAndTrigger { value ->
                ftpZoneLow.floatValue = value
            }

            it.ftpZoneHigh.listenAndTrigger { value ->
                ftpZoneHigh.floatValue = value
            }

            it.targetBpm.listenAndTrigger { value ->
                targetBpm.intValue = value
            }

            it.bpmZoneLow.listenAndTrigger { value ->
                bpmZoneLow.intValue = value
            }

            it.bpmZoneHigh.listenAndTrigger { value ->
                bpmZoneHigh.intValue = value
            }

            it.targetCaloriesUpdate.listenAndTrigger { value ->
                targetCaloriesUpdate.intValue = value
            }
            it.targetDistanceKm.listenAndTrigger { value ->
                targetDistance.floatValue = if(isMetric.value){
                    value
                }else{
                    value.kilometerUnit.toMiles
                }
            }
            it.targetTimeGoalMinutes.listenAndTrigger { value ->
                targetTimeGoalMinutes.intValue = value
            }
            it.goalTotal.listenAndTrigger { value ->
                totalGoalDistance.floatValue = value
            }
            it.distanceKm.listenAndTrigger { value  ->
                totalDistance.floatValue = if(isMetric.value){
                    value
                }else{
                    value.kilometerUnit.toMiles
                } }
            it.targetTrainingType.listenAndTrigger { type ->
                val mode = WorkoutMode.fromTTType(type)
                if (mode == WorkoutMode.Distance) {
                    paceStartDistance.floatValue = currentDistance.floatValue
                    paceStartTimeSeconds.floatValue = timeElapsedSeconds.floatValue
                    isTargetTrainingActive.value = true
                } else {
                    isTargetTrainingActive.value = false
                }
            }
            it.goalTotal
        }
    }

    internal fun setGenderData(genderValue: Gender){
        gender.value = genderValue
    }

    internal fun setFitnessLevel(level: FitnessLevel) {
        fitnessLevel.value = level
    }

    internal fun onClickSavedCalories() {
        paceStartCalories.floatValue = totalCalories.floatValue
        targetCalories.floatValue = 0f
    }

    internal val bottomTabs = listOf(
        TabBarItem(
            title = R.string.rpm_uc,
            selectedIcon = R.drawable.ic_rpm_meter,
            unselectedIcon = R.drawable.ic_unselected_speedometer,
            route = TargetTrainingScreen.RpmScreen.route,
            workoutMode = WorkoutMode.Rpm
        ),
        TabBarItem(
            title = R.string.constant_watts_tc,
            selectedIcon = R.drawable.ic_lightning,
            unselectedIcon = R.drawable.ic_unselected_lightning,
            route = TargetTrainingScreen.WattsScreen.route,
            workoutMode = WorkoutMode.Watts
        ),
        TabBarItem(
            title = R.string.heart_rate_tc,
            selectedIcon = R.drawable.ic_heart,
            unselectedIcon = R.drawable.ic_unselected_heart,
            route = TargetTrainingScreen.HeartRateScreen.route,
            workoutMode = WorkoutMode.HeartRate
        ),
        TabBarItem(
            title = R.string.calories_tc,
            selectedIcon = R.drawable.ic_fire_tt,
            unselectedIcon = R.drawable.ic_unselected_fire,
            route = TargetTrainingScreen.CaloriesScreen.route,
            workoutMode = WorkoutMode.Calories
        ),
        TabBarItem(
            title = R.string.distance_tc,
            selectedIcon = R.drawable.ic_distance_green,
            unselectedIcon = R.drawable.ic_unselected_waypoints,
            route = TargetTrainingScreen.DistanceScreen.route,
            workoutMode = WorkoutMode.Distance
        )
    )


    internal val targetTrainingSetupData = mutableListOf<TargetTrainingSetupData>().apply {
        addAll(
            listOf(
                TargetTrainingSetupData(
                    R.string.rpm_uc,
                    R.string.rpm_description_pg,
                    R.drawable.ic_rpm_meter,
                    TargetTrainingScreen.RpmScreen.route
                ),
                TargetTrainingSetupData(
                    R.string.watts_tc,
                    R.string.watts_description_pg,
                    R.drawable.ic_lightning,
                    TargetTrainingScreen.WattsScreen.route

                ),
                TargetTrainingSetupData(
                    R.string.heart_rate_tc,
                    R.string.heart_rate_description_pg,
                    R.drawable.ic_heart,
                    TargetTrainingScreen.HeartRateScreen.route
                ),
                TargetTrainingSetupData(
                    R.string.calories_tc,
                    R.string.calories_description_pg,
                    R.drawable.ic_fire_tt,
                    TargetTrainingScreen.CaloriesScreen.route
                ),
                TargetTrainingSetupData(
                    R.string.distance_tc,
                    R.string.distance_description_pg,
                    R.drawable.ic_distance_green,
                    TargetTrainingScreen.DistanceScreen.route
                )
            )
        )
    }


    internal fun caloriesData() = TargetSetupData(
        titleRes = R.string.calories_tc,
        iconRes = R.drawable.ic_fire_tt,
        descriptionRes = R.string.calories_setup_description_pg,
        route = TargetTrainingScreen.CaloriesScreen.route,
        workoutMode = WorkoutMode.Calories,
        pillBoxes = listOf(
            PillBoxData(
                title = context.getString(R.string.target_calories_tc),
                unitValue = context.getString(R.string.calories_tc),
                decimalPlaces = 0,
                displayType = NumPadDisplay.Number,
                testTag = "CaloriesSetup:TargetCalories:PillBox",
                min = workoutModel.caloriesMinValue.toFloat(),
                max = workoutModel.caloriesMaxValue.toFloat(),
                stepValue = 1f,
                defaultValue =  TargetTrainingTypeData.caloriesTarget
                    ?.takeIf { it > 0f }
                    ?: workoutModel.caloriesDefaultValue.toFloat()),
            PillBoxData(
                title = context.getString(R.string.target_time_tc),
                unitValue = context.getString(R.string.minutes_lc),
                decimalPlaces = 0,
                displayType = NumPadDisplay.TimeValueMinutes,
                testTag = "CaloriesSetup:TargetTime:PillBox",
                min = workoutModel.targetTimeMinMinutes.toFloat(),
                max = workoutModel.targetTimeMaxMinutes.toFloat(),
                stepValue = 1f,
                defaultValue = TargetTrainingTypeData.timeTarget.toFloat()
                    .takeIf { it > 0f } ?: workoutModel.targetTimeDefaultMinutes.toFloat()
            )
        )
    )
    internal fun rpmData() = TargetSetupData(
        titleRes = R.string.rpm_uc,
        iconRes = R.drawable.ic_rpm_meter,
        descriptionRes = R.string.rpm_setup_description_pg,
        route = TargetTrainingScreen.RpmScreen.route,
        workoutMode = WorkoutMode.Rpm,
        pillBoxes = listOf(
            PillBoxData(
                title = context.getString(R.string.target_rpm_tc),
                unitValue = "",
                decimalPlaces = 0,
                displayType = NumPadDisplay.Number,
                testTag = "RpmSetup:TargetRpm:PillBox",
                min = workoutModel.rpmTargetMinValue.toFloat(),
                max = workoutModel.rpmTargetMaxValue.toFloat(),
                stepValue = 1f,
                defaultValue = TargetTrainingTypeData.rpmTarget
                    .takeIf { it > 0f } ?: workoutModel.rpmTargetDefaultValue.toFloat()
            )
        )
    )

    internal fun heartRateData() = TargetSetupData(
        titleRes = R.string.heart_rate_tc,
        iconRes = R.drawable.ic_heart,
        descriptionRes = R.string.heart_rate_setup_description_pg,
        route = TargetTrainingScreen.HeartRateScreen.route,
        workoutMode = WorkoutMode.HeartRate,
        pillBoxes = listOf(
            PillBoxData(
                title = context.getString(R.string.max_heart_rate_bpm_tc),
                unitValue = "", decimalPlaces = 0,
                displayType = NumPadDisplay.Number,
                testTag = "HeartRateSetup:MaxHeartRate:PillBox",
                min = workoutModel.bpmMinValue.toFloat(),
                max = workoutModel.bpmMaxValue.toFloat(),
                stepValue = 1f,
                defaultValue = TargetTrainingTypeData.heartRateTarget?.maxHr
                    ?: workoutModel.bpmDefaultValue.toFloat()
            ),
            PillBoxData(
                title = context.getString(R.string.age_tc),
                unitValue = "",
                decimalPlaces = 0,
                displayType = NumPadDisplay.Number,
                testTag = "HeartRateSetup:MaxHeartRate:PillBox",
                min = 14f,
                max = 99f,
                stepValue = 1f,
                defaultValue = workoutModel.dataUserAgeDefault.value.toFloat()
            )
        ),
        targetZoneRange = HeartRateZoneRange.entries.map {
            it.label to it.color
        }
    )
    internal fun wattsData() = TargetSetupData(
        titleRes = R.string.watts_tc,
        iconRes = R.drawable.ic_lightning,
        descriptionRes = R.string.watts_setup_description_pg,
        route = TargetTrainingScreen.WattsScreen.route,
        workoutMode = WorkoutMode.Watts,
        pillBoxes = listOf(
            PillBoxData(
                title = "${context.getString(R.string.ftp_watts_tc)} (${context.getString(R.string.watts_lc)})",
                unitValue = context.getString(R.string.ftp_tc),
                decimalPlaces = 0,
                displayType = NumPadDisplay.Number,
                testTag = "WattsSetup:Ftp:PillBox",
                min = workoutModel.ftpMinValue.toFloat(),
                max = workoutModel.ftpMaxValue.toFloat(),
                stepValue = 1f,
                defaultValue = TargetTrainingTypeData.wattsTarget?.watts
                    ?: workoutModel.ftpDefaultValue.toFloat()
            )
        ),
        targetZoneRange = WattsZoneRange.entries.map {
            it.label to it.color
        }
    )
    internal fun distanceData() = TargetSetupData(
        titleRes = R.string.distance_tc,
        iconRes = R.drawable.ic_distance_green,
        descriptionRes = R.string.distance_setup_description_pg,
        route = TargetTrainingScreen.DistanceScreen.route,
        workoutMode = WorkoutMode.Distance,
        pillBoxes = listOf(
            PillBoxData(
                title = context.getString(R.string.target_distance_tc),
                unitValue = context.getString(R.string.miles_tc),
                decimalPlaces = 1,
                displayType = NumPadDisplay.Number,
                testTag = "DistanceSetup:TargetDistance:PillBox",
                min = if (workoutModel.isMetric.value) {
                    workoutModel.targetDistanceMinKm
                } else {
                    workoutModel.targetDistanceMinKm.kilometerUnit.toMiles
                },
                max = if (workoutModel.isMetric.value) {
                    workoutModel.targetDistanceMaxKm
                } else {
                    workoutModel.targetDistanceMaxKm.kilometerUnit.toMiles
                },
                stepValue = 0.1f,
                defaultValue = TargetTrainingTypeData.distanceTarget
                    .takeIf { it > 0f } ?: if (workoutModel.isMetric.value) {
                    workoutModel.targetDistanceDefaultKm
                } else {
                    workoutModel.targetDistanceDefaultKm.kilometerUnit.toMiles
                },
            ),
            PillBoxData(
                title = context.getString(R.string.target_time_tc),
                unitValue = context.getString(R.string.minutes_lc),
                decimalPlaces = 0,
                displayType = NumPadDisplay.TimeValueMinutes,
                testTag = "DistanceSetup:TargetTime:PillBox",
                min = workoutModel.targetTimeMinMinutes.toFloat(),
                max = workoutModel.targetTimeMaxMinutes.toFloat(),
                stepValue = 1f,
                defaultValue = TargetTrainingTypeData.timeTarget.toFloat()
                    .takeIf { it > 0f } ?: workoutModel.targetTimeDefaultMinutes.toFloat()
            )
        )
    )
    private val selectedMode = MutableStateFlow(TargetTrainingTypeData.selectedMode)
    internal fun updateSelectedMode(mode: WorkoutMode, ftpMode: UserFtpMode = UserFtpMode.UserFtpKnown) {
        TargetTrainingTypeData.selectedMode = mode
        selectedMode.value = mode

        when (mode) {
            WorkoutMode.Distance -> {
                workoutModel.setTargetDistanceKm(
                    if (workoutModel.isMetric.value) {
                        TargetTrainingTypeData.distanceTarget
                    } else {
                        TargetTrainingTypeData.distanceTarget.mileUnit.toKilometer
                    }
                )
                workoutModel.setTargetTimeGoalMinutes(TargetTrainingTypeData.timeTarget)
                workoutModel.setTargetTrainingType(WorkoutMode.Distance.toTTType())
            }

            WorkoutMode.Rpm -> {
                val targetRpm = TargetTrainingTypeData.rpmTarget
                workoutModel.setRpmZoneLow((targetRpm - 5))
                workoutModel.setRpmZoneHigh((targetRpm + 5))
                workoutModel.setRpmTarget(targetRpm.toInt())
                workoutModel.setTargetTrainingType(WorkoutMode.Rpm.toTTType())
            }

            WorkoutMode.Watts -> {
                val wattsTarget = TargetTrainingTypeData.wattsTarget ?: return
                val ftpValue: Int
                if (ftpMode == UserFtpMode.UserFtpUnknown) {
                    // FTP calculation when ftp of user is not known from gender and fitness level
                    // Table has male/female rows only; as in the old implementation, anything other than male uses the female row
                    val genderIndex = if (gender.value == Gender.Male) 0 else 1
                    val fitnessLevelIndex = fitnessLevel.value.ordinal
                    ftpValue = ftpEstimateLookupTable[genderIndex][fitnessLevelIndex]
                    val lowPercent = ftpZoneLookupTable[0][fitnessLevelIndex]
                    val highPercent = ftpZoneLookupTable[1][fitnessLevelIndex]
                    val low = ftpValue * lowPercent / 100f
                    val high = ftpValue * highPercent / 100f + 0.00001f
                    workoutModel.setFtpZoneLow(low)
                    workoutModel.setFtpZoneHigh(high)
                } else {
                    // FTP calculation when ftp of user is known
                    ftpValue = wattsTarget.watts.toInt()
                    val (lowPercent, highPercent) = when (wattsTarget.targetZone) {
                        FtpZone.LESS_THAN_55.value -> 0 to 55
                        FtpZone.FROM_56_TO_75.value -> 56 to 75
                        FtpZone.FROM_76_TO_90.value -> 76 to 90
                        FtpZone.FROM_91_TO_105.value -> 91 to 105
                        FtpZone.FROM_106_TO_120.value -> 106 to 120
                        FtpZone.FROM_122_TO_150.value -> 122 to 150
                        FtpZone.GREATER_THAN_150.value -> 150 to Int.MAX_VALUE
                        else -> 56 to 75
                    }
                    val low = ftpValue * lowPercent / 100f
                    val high = if (highPercent == Int.MAX_VALUE) {
                        ftpValue * 2f
                    } else {
                        ftpValue * highPercent / 100f
                    }

                    workoutModel.setFtpZoneLow(low)
                    workoutModel.setFtpZoneHigh(high)
                }

                workoutModel.setFtpZoneSelectedIndex(
                        FtpZone.entries
                            .indexOfFirst { it.value == wattsTarget.targetZone }
                            .coerceAtLeast(0)
                            )
                workoutModel.setUserFtp(ftpValue)
                workoutModel.setTargetTrainingType(WorkoutMode.Watts.toTTType())
            }



            WorkoutMode.HeartRate -> {
                val hrTarget = TargetTrainingTypeData.heartRateTarget ?: return
                val maxBpm = hrTarget.maxHr.toInt() // safe, hrTarget is non-null

                val (lowPercent, highPercent) = when (hrTarget.targetZone) {
                    HeartRateSelectedZone.LESS_THAN_60.value -> 0 to 59
                    HeartRateSelectedZone.FROM_60_TO_69.value -> 60 to 69
                    HeartRateSelectedZone.FROM_70_TO_79.value -> 70 to 79
                    HeartRateSelectedZone.FROM_80_TO_89.value -> 80 to 89
                    HeartRateSelectedZone.GREATER_THAN_OR_EQUAL_90.value -> 90 to 100
                    else -> 60 to 69
                }

                val low = (maxBpm * lowPercent / 100f).toInt()
                val high = (maxBpm * highPercent / 100f).toInt()
                val target = (low + high) / 2

                workoutModel.setTargetBpm(target)
                workoutModel.setBpmZoneLow(low)
                workoutModel.setBpmZoneHigh(high)
                workoutModel.setBpmZoneSelectedIndex(
                        HeartRateSelectedZone.entries
                            .indexOfFirst { it.value == hrTarget.targetZone }
                            .coerceAtLeast(0)
                        )
                workoutModel.setUserMaxHr(maxBpm)
                workoutModel.setTargetTrainingType(WorkoutMode.HeartRate.toTTType())
            }

            WorkoutMode.Calories -> {
                TargetTrainingTypeData.caloriesTarget?.toInt()
                    ?.let { workoutModel.setTargetCalories(it) }
                workoutModel.setTargetTimeGoalMinutes(TargetTrainingTypeData.timeTarget)
                workoutModel.setTargetTrainingType(WorkoutMode.Calories.toTTType())
            }

            WorkoutMode.None -> {}
        }
    }

}
