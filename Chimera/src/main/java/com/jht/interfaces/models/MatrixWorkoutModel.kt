package com.jht.interfaces.models

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.os.Bundle
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.SetupDefault
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.service.ui.panels.compose.FrameControlsValueListener
import com.jht.data.DataPoint
import com.jht.data.GraphChannelChange
import com.jht.data.GraphInfoChannel
import com.jht.data.INVALID_GRAPH_DATA
import com.jht.data.TrackInfo
import com.jht.data.sprint8.Sprint8SweatScores
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.CaloriesSource
import com.jht.enums.WorkoutGoalType
import com.jht.enums.WorkoutId
import com.jht.enums.WorkoutRunState
import com.jht.enums.WorkoutStage
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatValueListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.MatrixBoolStateValueListener
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixBoolVariableListener
import com.jht.interfaces.MatrixEventListener
import com.jht.interfaces.MatrixEventStateValueListener
import com.jht.interfaces.MatrixFloatValueListener
import com.jht.interfaces.MatrixIntValueListener
import com.jht.interfaces.MatrixParcelableValueListener
import com.jht.interfaces.MatrixParcelableVariableListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.interfaces.MatrixStringVariableListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.StringVariableListener
import com.jht.workoutcontrollerlib.WorkoutController
import com.jht.workoutdata.client.WorkoutDataClient
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.model.WorkoutFutureData
import com.jht.workoutdata.model.WorkoutMetric
import com.jht.workoutdata.query.WorkoutDataQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private fun isTreadmill(): Boolean {
    val config = FrameConfigManager.getInstance().frameConfig
    return config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill
}

private const val TAG = "MatrixWorkoutModel"

class MatrixWorkoutModel(
    private val context: Context,
) : WorkoutModel() {
    private val manager: CallbackManager = CallbackManager()

    private val frameManager = FrameConfigManager.getInstance().frameConfig

    // client used retrieve workout history data
    private val workoutDataClient by lazy { WorkoutDataClient(context) }

    // ContentObservers registered via graphUpdateEvent; dropped in close()
    // (the graph UI calls close() to release listeners, mirroring CallbackManager behavior).
    private val graphObservers = mutableListOf<ContentObserver>()

    override fun close() {
        manager.cancelAllListeners()
        // release provider change observers alongside DataMatrix listeners
        graphObservers.forEach { workoutDataClient.unregisterChangeListener(it) }
        graphObservers.clear()
    }

    override val frameControls by lazy { FrameControlsValueListener(manager) }

    override fun increaseWorkoutGoal() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_INCREASE_WORKOUT_GOAL)
    }

    override fun decreaseWorkoutGoal() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_DECREASE_WORKOUT_GOAL)
    }

    override fun updateWorkoutSpeed(speedKph: Float) {
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, speedKph)
    }

    override fun updateWorkoutIncline(incline: Float) {
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, incline)
    }

    override fun updateWorkoutResistance(resistance: Int) {
        DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, resistance)
    }

    //O indexed levels
    override fun hotLevelUpdateSprint8(level: Int) {
        DataMatrix.putInt(WorkoutKeys.ACTION_HOT_SWAP_LEVEL_INT, level)
    }

    override val workoutLevel: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_WORKOUT_LEVEL_INT)

    override val isSafeToStartWorkout: BoolValueListener =
        MatrixBoolValueListener(manager, HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)

    override val supportsReverseBelt: Boolean
        get() = frameManager.TM_SUPPORTS_REVERSE_BELT

    override fun hasFrameControl(): Boolean {
        val config = FrameConfigManager.getInstance().frameConfig
        return config.machineType != JHTCommonTypes.MachineType.ManualResistanceSpinBike
    }

    override val isBeltInReverse: BoolValueListener =
        MatrixBoolValueListener(manager, HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL)

    override val singleAppMode: String get() =
        // we should consider migrating to a single string key for a generic single app mode instead of specific ones
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_SWITCH_STRIDE_MODE_ONLY_BOOL) && frameManager.TM_SUPPORTS_REVERSE_BELT) AppIds.HYBRID_TD_SWITCH_STRIDE_UUID
        else ""

    override fun setBeltInReverse(inReverse: Boolean) {
        DataMatrix.updateBoolean(HALKeys.ACTION_TM_REVERSE_BELT_BOOL, inReverse)
    }

    override fun pauseWorkout() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT)
    }
    override fun resumeWorkout() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT)

    }

    override fun stopWorkout() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT)
    }

    override fun actionSkipInterval() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_SKIP_INTERVAL)
    }
    override val allowUserGoalChanges: DataValueListener<Boolean> =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_ALLOW_USER_GOAL_CHANGES_BOOL)
    override val isMetric: DataVariableListener<Boolean> =
        MatrixBoolVariableListener(manager, UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)

    override val wattsDisplayAllowed: Boolean
        get() = !FrameConfigManager.getInstance().frameConfig.WATTS_INACCURATE

    override val cadenceDisplayAllowed: Boolean
        get() = FrameConfigManager.getInstance().frameConfig.HAS_CADENCE

    override val distancePrecision: Int
        get() = DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT)

    override val is24HourFormat: Boolean
        get() = DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL)

    override fun clampSpeedToFrameLimits(speedKph: Float): Float {
        val isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)

        val minSpeed: Float
        val maxSpeed: Float
        if (isMetric) {
            minSpeed = FrameConfigManager.getInstance().frameConfig.speedKph.min
            maxSpeed = FrameConfigManager.getInstance().frameConfig.speedKph.max
        } else {
            minSpeed =
                SpeedConvertUtil.convertMphToKph(FrameConfigManager.getInstance().frameConfig.speedMph.min)
            maxSpeed =
                SpeedConvertUtil.convertMphToKph(FrameConfigManager.getInstance().frameConfig.speedMph.max)
        }

        return speedKph.coerceIn(minSpeed, maxSpeed)
    }

    override fun clampInclineToFrameLimits(incline: Float): Float {
        val minIncline = FrameConfigManager.getInstance().frameConfig.incline.min
        val maxIncline = FrameConfigManager.getInstance().frameConfig.incline.max
        return incline.coerceIn(minIncline, maxIncline)
    }

    override fun clampResistanceToFrameLimits(resistance: Int): Int {
        val minResistance = FrameConfigManager.getInstance().frameConfig.resistance.min.toInt()
        val maxResistance = FrameConfigManager.getInstance().frameConfig.resistance.max.toInt()
        return resistance.coerceIn(minResistance, maxResistance)
    }

    override val clockUpdateEvent: EventListener =
        MatrixEventListener(
            manager,
            WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED
        ) // TODO pause won't send this
    override val caloriesSource: ParcelableValueListener<CaloriesSource> =
        CaloriesSourceListener(manager)
    override val calories: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_CALORIES_FLOAT)
    override val caloriesPerHour: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_CALORIES_PER_HOUR_INT)
    override val distanceKm: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_DISTANCE_KM_FLOAT)
    override val elevationMeter: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_ELEVATION_METERS_FLOAT)
    override val floors: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_FLOORS_FLOAT)
    override val heartRate: IntValueListener =
        MatrixIntValueListener(manager, HALKeys.DATA_HEART_RATE_INT)
    override val numberOfSprints: IntValueListener =
        MatrixIntValueListener(manager, Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT)
    override val sweatScore: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_SPRINT8_SWEAT_SCORE_FLOAT)
    override val intervalRemaining: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_INTERVAL_GOAL_REMAINING_FLOAT)
    override val myZoneHeartRatePercent: IntValueListener =
        MatrixIntValueListener(manager, HALKeys.DATA_MYZONE_EFFORT_INT)
    override val myZoneMeps: IntValueListener =
        MatrixIntValueListener(manager, HALKeys.DATA_MYZONE_MEPS_INT)
    override val incline: FloatValueListener =
        MatrixFloatValueListener(manager, HALKeys.DATA_TARGET_INCLINE_FLOAT)
    override val mets: FloatValueListener =
        MatrixFloatValueListener(manager, HALKeys.DATA_METS_FLOAT)
    override val paceSecPerKm: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_PACE_SEC_PER_KM_INT)
    override val rpm: IntValueListener = MatrixIntValueListener(manager, HALKeys.DATA_RPM_INT)
    override val speedKph: FloatValueListener =
        if (isTreadmill()) MatrixFloatValueListener(manager, HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)
        else MatrixFloatValueListener(manager, HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT)
    override val spm: IntValueListener = MatrixIntValueListener(manager, HALKeys.DATA_SPM_INT)
    override val steps: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_STEPS_INT)
    override  val averageStrideLengthMeters: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_AVERAGE_STRIDE_LENGTH_METERS_FLOAT)
    override  val strideLengthMeters: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_STRIDE_LENGTH_METERS_FLOAT)
    override val watts: FloatValueListener =
        MatrixFloatValueListener(manager, HALKeys.DATA_WATTS_FLOAT)
    override val averageSpeedKph: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT)
    override val averageSpm: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_AVERAGE_SPM_INT)
    override val peakHeartRate: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_PEAK_HEART_RATE_INT)
    override val sprint8State: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_SPRINT8_CURRENT_STATE_INT)
    override val targetHeartRatePercent: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT)
    override val timeElapsedMs: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_TIME_ELAPSED_MS_INT)
    override val goalRemaining: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_GOAL_REMAINING_FLOAT)
    override val goalTotal: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_GOAL_VALUE_FLOAT)
    override val scriptNextSpeed: FloatValueListener =
        MatrixFloatValueListener(manager,WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_SPEED_FLOAT)
    override val scriptNextIncline: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_INCLINE_FLOAT)
    override val scriptNextResistance: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_RESISTANCE_INT)
    override val scriptIsSprinting: BoolValueListener =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_SPRINTING_BOOL)
    override val scriptIsRamping: BoolValueListener =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_RAMPING_BOOL)
    override val rpmTarget =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_RPM_TARGET_INT)
    override val targetDistanceKm: FloatValueListener =
        MatrixFloatValueListener(manager, MainAppKeys.DATA_TT_TARGET_DISTANCE_KM_FLOAT)
    override val targetTimeGoalMinutes: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_TIME_GOAL_TARGET_INT)
    override val targetTrainingType: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_TARGET_TRAINING_TYPE_INT)
    override val workoutOptionId: StringVariableListener =
        MatrixStringVariableListener(manager, WorkoutKeys.DATA_WORKOUT_OPTION_ID_STRING)
    override val targetDistanceMinKm = MainAppKeys.TT_DEFAULTS.TT_MIN_DISTANCE_KM
    override val targetDistanceMaxKm = MainAppKeys.TT_DEFAULTS.TT_MAX_DISTANCE_KM
    override val targetDistanceDefaultKm = MainAppKeys.TT_DEFAULTS.TT_DEFAULT_DISTANCE_KM

    override val targetTimeMinMinutes = MainAppKeys.TT_DEFAULTS.TT_MIN_TIME_MINUTES
    override val targetTimeMaxMinutes = MainAppKeys.TT_DEFAULTS.TT_MAX_TIME_MINUTES
    override val targetTimeDefaultMinutes = MainAppKeys.TT_DEFAULTS.TT_DEFAULT_DISTANCE_TIME_MINUTES
    // --- RPM mode ---
    override val rpmZoneLow: FloatValueListener =
        MatrixFloatValueListener(manager, MainAppKeys.DATA_TT_RPM_ZONE_LOW_FLOAT)
    override val rpmZoneHigh: FloatValueListener =
        MatrixFloatValueListener(manager, MainAppKeys.DATA_TT_RPM_ZONE_HIGH_FLOAT)

    override fun setRpmTarget(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_RPM_TARGET_INT, value)
    }
    override fun setRpmZoneLow(value: Float) {
        DataMatrix.putFloat(MainAppKeys.DATA_TT_RPM_ZONE_LOW_FLOAT, value)
    }
    override fun setRpmZoneHigh(value: Float) {
        DataMatrix.putFloat(MainAppKeys.DATA_TT_RPM_ZONE_HIGH_FLOAT, value)
    }

    // --- Watts/FTP mode ---
    override val ftpZoneLow: FloatValueListener =
        MatrixFloatValueListener(manager, MainAppKeys.DATA_TT_FTP_ZONE_LOW_FLOAT)
    override val ftpZoneHigh: FloatValueListener =
        MatrixFloatValueListener(manager, MainAppKeys.DATA_TT_FTP_ZONE_HIGH_FLOAT)

    override fun setFtpZoneLow(value: Float) {
        DataMatrix.putFloat(MainAppKeys.DATA_TT_FTP_ZONE_LOW_FLOAT, value)
    }
    override fun setFtpZoneHigh(value: Float) {
        DataMatrix.putFloat(MainAppKeys.DATA_TT_FTP_ZONE_HIGH_FLOAT, value)
    }
    override fun setFtpZoneSelectedIndex(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_FTP_ZONE_SELECTED_INDEX_INT, value)
    }
    override fun setFtpZoneFitnessSelectedIndex(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_FTP_ZONE_FITNESS_SELECTED_INDEX_INT, value)
    }
    override fun setUserFtp(value: Int) {
        DataMatrix.putInt(DAPIKeys.DATA_USER_FTP_INT, value)
    }

    // --- BPM/Heart Rate mode ---
    override val targetBpm: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_TARGET_BPM_INT)
    override val bpmZoneLow: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_BPM_ZONE_LOW_INT)
    override val bpmZoneHigh: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_BPM_ZONE_HIGH_INT)

    override val dataUserAgeDefault: IntValueListener =
        MatrixIntValueListener(manager, SetupDefault.DATA_DEFAULT_AGE_INT)

    override fun setTargetBpm(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_TARGET_BPM_INT, value)
    }
    override fun setBpmZoneLow(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_BPM_ZONE_LOW_INT, value)
    }
    override fun setBpmZoneHigh(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_BPM_ZONE_HIGH_INT, value)
    }
    override fun setBpmZoneSelectedIndex(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_BPM_ZONE_SELECTED_INDEX_INT, value)
    }
    override fun setUserMaxHr(value: Int) {
        DataMatrix.putInt(DAPIKeys.DATA_USER_MAXHR_INT, value)
    }
    override fun setUserAge(value: Int) {
        DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, value)
    }

    // --- Calories mode ---
    override val targetCaloriesUpdate: IntValueListener =
        MatrixIntValueListener(manager, MainAppKeys.DATA_TT_TARGET_CALORIES_INT)

    override fun setTargetCalories(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_TARGET_CALORIES_INT, value)
    }

    override val rpmTargetMinValue: Int = MainAppKeys.TT_DEFAULTS.TT_RPM_MIN_VALUE.toInt()
    override val rpmTargetMaxValue: Int = MainAppKeys.TT_DEFAULTS.TT_RPM_MAX_VALUE.toInt()
    override val rpmTargetDefaultValue: Int
        get() = DataMatrix.getInt(MainAppKeys.DATA_TT_RPM_TARGET_INT)
            .takeIf { it > 0 } ?: 60

    override val bpmMinValue: Int = MainAppKeys.TT_DEFAULTS.TT_BPM_MIN_VALUE
    override val bpmMaxValue: Int = MainAppKeys.TT_DEFAULTS.TT_BPM_MAX_VALUE
    override val bpmDefaultValue: Int
        get() = DataMatrix.getInt(DAPIKeys.DATA_USER_MAXHR_INT)
            .takeIf { it > 0 } ?: 192

    override val ftpMinValue: Int = MainAppKeys.TT_DEFAULTS.TT_WATTS_MIN_VALUE
    override val ftpMaxValue: Int = MainAppKeys.TT_DEFAULTS.TT_WATTS_MAX_VALUE
    override val ftpDefaultValue: Int
        get() = DataMatrix.getInt(DAPIKeys.DATA_USER_FTP_INT, MainAppKeys.TT_DEFAULTS.TT_WATTS_DEFAULT_VALUE)

    override val caloriesMinValue: Int = MainAppKeys.TT_DEFAULTS.TT_CALORIES_MIN_VALUE.toInt()
    override val caloriesMaxValue: Int = MainAppKeys.TT_DEFAULTS.TT_CALORIES_MAX_VALUE.toInt()
    override val caloriesDefaultValue: Int
        get() = DataMatrix.getInt(MainAppKeys.DATA_TT_TARGET_CALORIES_INT)
            .takeIf { it > 0 } ?: DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT)
            .takeIf { it > 0 } ?: 300

    override fun setTargetDistanceKm(value: Float) {
        DataMatrix.updateFloat(MainAppKeys.DATA_TT_TARGET_DISTANCE_KM_FLOAT, value)
    }
    override val pauseTimerRemaining: StringValueListener =
        MatrixStringValueListener(manager, UiServiceKeys.DATA_PAUSE_TIMER_STRING)

    override fun setTargetTimeGoalMinutes(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_TIME_GOAL_TARGET_INT, value)
    }

    override fun setTargetTrainingType(value: Int) {
        DataMatrix.putInt(MainAppKeys.DATA_TT_TARGET_TRAINING_TYPE_INT, value)
    }

    override val gymkitCalories: FloatValueListener = object : FloatValueListener {
        private val INVALID_VALUE = -1f
        private val activeCaloriesKey = WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT
        private val gymkitStatusKey = HALKeys.DATA_GYM_KIT_STATUS_INT
        private var isGymkitConnected = false

        private fun checkConnected(state: Int = DataMatrix.getInt(gymkitStatusKey)): Boolean {
            // need to show if connected OR just out of range
            return (state == HALKeys.STATUS_GYM_KIT_CONNECTED || state == HALKeys.STATUS_GYM_KIT_OUT_OF_RANGE)
        }

        private fun getCurrentValue(isConnected: Boolean): Float {
            if (isConnected) return DataMatrix.getFloat(activeCaloriesKey)
            return INVALID_VALUE
        }

        override val value: Float
            get() = getCurrentValue(checkConnected()) // value could be accessed without any listeners, so check live connection state

        override fun listen(onChange: (Float) -> Unit) {
            // the active calories event should only happen when watch is sending data
            manager.addFloatListener(activeCaloriesKey, {
                onChange(if (isGymkitConnected) it else INVALID_VALUE)
            }, false)

            // need to listen when watch disconnect so we can reset the active calories data
            isGymkitConnected = checkConnected() // be sure to update current before subscribing
            manager.addIntListener(gymkitStatusKey, {
                isGymkitConnected = checkConnected(it)
                onChange(getCurrentValue(isGymkitConnected))
            }, false)
        }

        override fun listenAndTrigger(onChange: (Float) -> Unit) {
            listen(onChange)
            onChange(value)
        }
    }
    override val resistance: IntValueListener =
        MatrixIntValueListener(manager, HALKeys.DATA_RESISTANCE_INT)
    override val averageWatts: FloatValueListener = MatrixFloatValueListener(manager, WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT)
    override val userFtp: IntValueListener = MatrixIntValueListener(manager, DAPIKeys.DATA_USER_FTP_INT)
    override val maxHeartRate: FloatValueListener = MatrixFloatValueListener(manager, WorkoutKeys.DATA_MAX_HEART_RATE_FLOAT)
    // Allows tracking the run state from the console
    override val isWorkoutRunning = buildRunStateListener(WorkoutKeys.Constants.RUN_STATE_RUNNING)
    override val isRamping: BoolValueListener =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_RAMPING_BOOL)
    override val isSprinting: BoolValueListener =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_WORKOUT_SCRIPT_IS_SPRINTING_BOOL)
    override val isWorkoutPaused = buildRunStateListener(WorkoutKeys.Constants.RUN_STATE_PAUSE)
    override val isWorkoutSwitching =
        buildRunStateListener(WorkoutKeys.Constants.RUN_STATE_SWITCHING)
    override val isWorkoutStopped = buildRunStateListener(WorkoutKeys.Constants.RUN_STATE_STOP)
    override val isWorkoutLaunching: BoolValueListener =
        MatrixBoolValueListener(manager, WorkoutKeys.DATA_LAUNCH_STATE_BOOL)

    override val onStartWorkout: EventListener = MatrixEventStateValueListener(
        manager,
        WorkoutKeys.DATA_RUN_STATE_INT,
        WorkoutKeys.Constants.RUN_STATE_RUNNING,
        WorkoutKeys.Constants.RUN_STATE_STOP
    )

    override val onResumeWorkout: EventListener = MatrixEventStateValueListener(
        manager,
        WorkoutKeys.DATA_RUN_STATE_INT,
        WorkoutKeys.Constants.RUN_STATE_RUNNING,
        WorkoutKeys.Constants.RUN_STATE_PAUSE
    )

    override val workoutStage: DataValueListener<WorkoutStage> = object : DataValueListener<WorkoutStage> {
            private val key = WorkoutKeys.DATA_WORKOUT_STAGE_INT
            override val value: WorkoutStage
                get() = fromInt(DataMatrix.getInt(key))
            override fun listen(onChange: (WorkoutStage) -> Unit) {
                manager.addIntListener(key, { onChange(fromInt(it)) }, false)
            }
            override fun listenAndTrigger(onChange: (WorkoutStage) -> Unit) {
                manager.addIntListener(key, { onChange(fromInt(it)) }, true)
            }
            fun fromInt(stage: Int): WorkoutStage = when (stage) {
                WorkoutController.WORKOUT_STAGE_COOLDOWN -> WorkoutStage.COOL_DOWN
                WorkoutController.WORKOUT_STAGE_WARMUP -> WorkoutStage.WARM_UP
                else -> WorkoutStage.WORKOUT
            }
        }
    override val workoutRunState: DataValueListener<WorkoutRunState> = object : DataValueListener<WorkoutRunState> {
            private val key = WorkoutKeys.DATA_RUN_STATE_INT
            override val value: WorkoutRunState
                get() = fromInt(DataMatrix.getInt(key))
            override fun listen(onChange: (WorkoutRunState) -> Unit) {
                manager.addIntListener(key, { onChange(fromInt(it)) }, false)
            }
            override fun listenAndTrigger(onChange: (WorkoutRunState) -> Unit) {
                manager.addIntListener(key, { onChange(fromInt(it)) }, true)
            }
            fun fromInt(state: Int): WorkoutRunState = when (state) {
                WorkoutController.WORKOUT_STATE_PAUSED -> WorkoutRunState.PAUSED
                WorkoutController.WORKOUT_STATE_RUNNING -> WorkoutRunState.RUNNING
                else -> WorkoutRunState.STOPPED
            }
        }


    override val workoutId: DataValueListener<WorkoutId> = object : DataValueListener<WorkoutId> {
        private val key = WorkoutKeys.DATA_WORKOUT_ID_INT
        override val value: WorkoutId
            get() = WorkoutId.fromId(DataMatrix.getInt(key))

        override fun listen(onChange: (WorkoutId) -> Unit) {
            manager.addIntListener(key, { onChange(WorkoutId.fromId(it)) }, false)
        }

        override fun listenAndTrigger(onChange: (WorkoutId) -> Unit) {
            manager.addIntListener(key, { onChange(WorkoutId.fromId(it)) }, true)
        }
    }

    override val goalType: DataValueListener<WorkoutGoalType> =
        object : DataValueListener<WorkoutGoalType> {
            private val key = WorkoutKeys.DATA_GOAL_TYPE_INT
            override val value: WorkoutGoalType
                get() = WorkoutGoalType.fromId(DataMatrix.getInt(key))

            override fun listen(onChange: (WorkoutGoalType) -> Unit) {
                manager.addIntListener(key, { onChange(WorkoutGoalType.fromId(it)) }, false)
            }

            override fun listenAndTrigger(onChange: (WorkoutGoalType) -> Unit) {
                manager.addIntListener(key, { onChange(WorkoutGoalType.fromId(it)) }, true)
            }
        }

    override val intervalNumber: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_INTERVAL_NUM_INT)
    override val intervalGoalRemaining: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_INTERVAL_GOAL_REMAINING_FLOAT)

    override val intervalGoal: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_INTERVAL_GOAL_VALUE_FLOAT)

    override val intervalWorkTimeGoal: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_WORKOUT_INTERVAL_TIME_GOAL_FLOAT)

    override val intervalWorkDistanceGoal: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_WORKOUT_INTERVAL_DISTANCE_GOAL_KM_FLOAT)

    override val intervalRestTimeGoal: FloatValueListener =
        MatrixFloatValueListener(manager, WorkoutKeys.DATA_WORKOUT_REST_TIME_GOAL_FLOAT)

    override val numberOfIntervals: IntValueListener =
        MatrixIntValueListener(manager, WorkoutKeys.DATA_WORKOUT_NUMBER_OF_INTERVALS_INT)


    private fun buildRunStateListener(state: Int): BoolValueListener {
        val key = WorkoutKeys.DATA_RUN_STATE_INT
        return MatrixBoolStateValueListener(manager, key, state)
    }

    // Track parcelable which stores list of 100m lap data.
    override val trackDataParcelable by lazy {
        MatrixParcelableValueListener(
            manager,
            WorkoutKeys.DATA_TRACK_LAPS_PARCELABLE,
            TrackInfo(emptyList()),
            TrackInfo::class.java
        )
    }

    override val sweatScoreParcel by lazy {
        MatrixParcelableValueListener(
            manager,
            WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE,
            Sprint8SweatScores(),
            Sprint8SweatScores::class.java
        )
    }

    override val maxSpeedKph: Float =
        DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT)

    override val maxIncline: Float =
        DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT)

    override val maxResistance: Float =
        frameManager.resistance.max

    override val workoutAppSetupBundle by lazy {
        MatrixParcelableVariableListener(
            manager,
            WorkoutKeys.DATA_WORKOUT_APP_SETUP_PARCELABLE,
            Bundle(),
            Bundle::class.java
        )
    }

    // Only `value` is implemented: it rebuilds the GraphInfoChannel entirely from the
    // in-memory snapshots maintained by refreshGraphData() — no cross-process I/O on the
    // calling thread. listen() is intentionally a no-op: redraws are driven by
    // graphUpdateEvent, which fires on the WorkoutData provider's change notifications.
    override val heartGraphDataPoints by lazy { graphValueListener(WorkoutMetric.HEART_RATE) }
    override val inclineGraphDataPoints by lazy { graphValueListener(WorkoutMetric.INCLINE) }
    override val resistanceGraphDataPoints by lazy { graphValueListener(WorkoutMetric.RESISTANCE) }
    override val speedGraphDataPoints by lazy { graphValueListener(WorkoutMetric.SPEED) }
    override val rpmGraphDataPoint by lazy { graphValueListener(WorkoutMetric.RPM) }
    override val spmGraphDataPoint by lazy { graphValueListener(WorkoutMetric.SPM) }

    override val graphUpdateEvent: EventListener = object : EventListener {
        override fun listen(onEvent: () -> Unit) {
            val historyObserver = workoutDataClient.registerChangeListener(
                Handler(Looper.getMainLooper())
            ) { onEvent() }
            graphObservers.add(historyObserver)

            val futureObserver = workoutDataClient.registerFutureChangeListener(
                Handler(Looper.getMainLooper())
            ) {
                futureDirty = true
                onEvent()
            }
            graphObservers.add(futureObserver)
        }
    }

    /**
     * A [ParcelableValueListener] whose [value] is rebuilt from the WorkoutData
     * ContentProvider for [metric]. listen() is a no-op by design (redraws are
     * driven by [graphUpdateEvent]); listenAndTrigger emits the current value once.
     */
    private fun graphValueListener(metric: WorkoutMetric) =
        object : ParcelableValueListener<GraphInfoChannel> {
            override val value: GraphInfoChannel get() = buildGraphChannel(metric)
            override fun listen(onChange: (GraphInfoChannel) -> Unit) { /* driven by graphUpdateEvent */ }
            override fun listenAndTrigger(onChange: (GraphInfoChannel) -> Unit) { onChange(value) }
        }

    // Sliding-window snapshot state machine (bulk load / incremental
    // append / gap backfill / reset detection / eviction), extracted to GraphSnapshotManager
    // for unit-testability. This model just adapts WorkoutDataClient to its Source interface.
    private val snapshotManager = GraphSnapshotManager(object : GraphSnapshotManager.Source {
        override fun window(minutes: Int): List<WorkoutDataPoint> =
            workoutDataClient.queryPointsBlocking(WorkoutDataQuery.lastMinutes(minutes))

        override fun since(fromSec: Int): List<WorkoutDataPoint> =
            workoutDataClient.queryPointsSinceBlocking(fromSec)

        override fun between(fromSec: Int, toSec: Int): List<WorkoutDataPoint> =
            workoutDataClient.queryPointsBetweenBlocking(fromSec, toSec)

        override fun generation(): Long = workoutDataClient.getGenerationBlocking()
    })

    // Snapshot read by buildGraphChannel (on Main); written by refreshGraphData (on IO).
    private val graphPointsSnapshot: List<WorkoutDataPoint> get() = snapshotManager.snapshot

    // Cached future profiles, re-fetched only when the provider's
    // future-change notification marks them dirty (not on every 1Hz history tick).
    @Volatile
    private var futureSnapshot: WorkoutFutureData = WorkoutFutureData()
    @Volatile
    private var futureDirty = true // fetch on first refresh

    // Goal type and workout stage snapshotted once per refresh on IO so buildGraphChannel
    // (called per channel per repaint on Main) never performs a DataMatrix binder call.
    @Volatile
    private var goalTypeSnapshot = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT)
    @Volatile
    private var workoutStageSnapshot = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT)

    /**
     * Refreshes the graph snapshot from the provider (called once per graph update by the UI).
     * See [GraphSnapshotManager] for the incremental-load semantics; the snapshot is bounded to
     * the last [WorkoutGraphConfig.windowMinutes] minutes.
     */
    override suspend fun refreshGraphData() {
        withContext(Dispatchers.IO) {
            // Refresh the future only when its change trigger marked it dirty. The flag is
            // cleared BEFORE the fetch so a change notification landing mid-fetch re-marks
            // it instead of being clobbered; on failure it is re-set so the next tick retries.
            if (futureDirty) {
                futureDirty = false
                try {
                    futureSnapshot = workoutDataClient.getFutureBlocking()
                } catch (e: Exception) {
                    futureDirty = true
                    Log.e(TAG, "Failed to fetch workout future", e)
                }
            }
            goalTypeSnapshot = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT)
            workoutStageSnapshot = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT)
            try {
                snapshotManager.refresh()
            } catch (e: Exception) {
                // Keep the previous snapshot on failure; a permanently failing provider
                // must still be visible in the log, not a silently frozen graph.
                Log.e(TAG, "Failed to refresh workout graph snapshot", e)
            }
        }
    }

    /**
     * Projects [metric] from the current [graphPointsSnapshot] into a
     * [GraphInfoChannel]. The goal axis (mGoalValues) matches the active goal type,
     * mirroring the old GraphManager behaviour. Future-profile data is unused, so
     * it's left empty. Returns an empty channel when no snapshot is available.
     */
    private fun buildGraphChannel(metric: WorkoutMetric): GraphInfoChannel {
        val points = graphPointsSnapshot
        if (points.isEmpty()) return GraphInfoChannel()

        val goalType = goalTypeSnapshot
        val timeMs = ArrayList<Float>(points.size)
        val goalValues = ArrayList<Float>(points.size)
        val dataValues = ArrayList<GraphChannelChange>(points.size)
        points.forEachIndexed { index, point ->
            timeMs.add(point.elapsedTimeSec * 1000f)
            goalValues.add(goalAxisValue(goalType, point))
            // The provider stores raw values (heartRate 0 = no strap).
            // Map invalid heart-rate readings to the pen-up sentinel (like the old GraphManager)
            // so the graph draws a gap instead of a line along zero.
            val raw = point.valueOf(metric)
            val value = if (metric == WorkoutMetric.HEART_RATE && raw <= 0f) INVALID_GRAPH_DATA else raw
            dataValues.add(GraphChannelChange(index, value))
        }
        return GraphInfoChannel(timeMs, goalValues, dataValues, futureDataFor(metric, goalType))
    }

    /**
     * Projects the cached future profile for [metric] into DataPoints.
     * Time goals only, and never during cooldown (mirrors the old GraphManager projection and
     * GraphSetup.futureDataFor): the future's axis is elapsed time, so it would not line up
     * with a distance/calorie/floor goal axis, and the future must not repaint while
     * cooling down.
     */
    private fun futureDataFor(metric: WorkoutMetric, goalType: Int): Array<DataPoint>? {
        val timeAxis = goalType == WorkoutKeys.Constants.GOAL_TYPE_TIME ||
            goalType == WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE
        if (!timeAxis) return null
        if (workoutStageSnapshot == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) return null
        val profile = futureSnapshot.profileFor(metric) ?: return null
        return Array(profile.points.size) { i ->
            val point = profile.points[i]
            val ms = point.timeSec * 1000f
            DataPoint(ms, ms, point.value)
        }
    }

    /** Maps the active goal type to its axis value for a point (mirrors GraphManager). */
    private fun goalAxisValue(goalType: Int, point: WorkoutDataPoint): Float = when (goalType) {
        WorkoutKeys.Constants.GOAL_TYPE_DISTANCE,
        WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE -> point.distanceKm
        WorkoutKeys.Constants.GOAL_TYPE_CALORIES,
        WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE -> point.totalCalories
        WorkoutKeys.Constants.GOAL_TYPE_FLOORS,
        WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE -> point.totalFloors
        else -> point.elapsedTimeSec * 1000f // time goals, GOAL_TYPE_NONE, and default
    }

}


/** Provides and updates what the current source of calories is */
private class CaloriesSourceListener(private val manager: CallbackManager) :
    ParcelableValueListener<CaloriesSource> {
    private var evoltSMM = 0f
    override val value: CaloriesSource
        get() = if (evoltSMM > 0) CaloriesSource.EVolt else CaloriesSource.Standard

    override fun listen(onChange: (CaloriesSource) -> Unit) {
        manager.addFloatListener(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT, {
            evoltSMM = it
            onChange(value)
        }, false)
    }

    override fun listenAndTrigger(onChange: (CaloriesSource) -> Unit) {
        manager.addFloatListener(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT, {
            evoltSMM = it
            onChange(value)
        }, true)
    }
}
