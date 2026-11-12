package com.jht.chimera.service.gattserver2.services

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jht.androidcommonalgorithms.timer.PeriodicTimer
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType
import com.jht.chimera.BuildConfig
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.common.conversionlib.GoalConverter
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.keypress.KeyPress
import com.jht.chimera.lib.language.StringLookup
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables
import com.jht.chimera.service.workout.SafeToStartFlags
import com.jht.datamatrixlib.DataMatrix
import com.jht.gatt.characteristic.ConversionHelper
import com.jht.gatt.characteristic.FitnessMachineFeature
import com.jht.gatt.characteristic.SupportedInclinationRange
import com.jht.gatt.characteristic.SupportedResistanceLevelRange
import com.jht.gatt.characteristic.SupportedSpeedRange
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.services.FtmsService
import kotlin.math.floor

private const val TAG = "GattFtmsServiceConsole"

class FtmsServiceConsole(
    server: IGattServer,
    variances: IFtmsVariances,
    frameConfig: FrameConfig,
) : FtmsService(server, variances) {
    private val updateDataTimer = PeriodicTimer(0, 1000) { eventUpdateDataTick() } // only used if BuildConfig.FTMS_DATA_ALWAYS_STREAM set
    private val dataMatrix = DataMatrix()
    private var isMetric = false
    private fun fixSpeed(kph: Float): Float {
        return if (isMetric) kph else SpeedConvertUtil.fixKPH(kph, 2)
    }

    private var frameConfig: FrameConfig? = null
    private var dialog: MessageBoxDialog? = null

    init {
        val frame = convertToFrameInfo(frameConfig)
        setupFrame(frame, DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FTMS_ENABLE_CONTROL_POINT_BOOL))
    }

    private fun convertToFrameInfo(frameConfig: FrameConfig?): FtmsFrameInfo? {
        if (frameConfig == null) return null
        this.frameConfig = frameConfig // so we can make future decisions based on frame config and do range checks?
        val frame = FtmsFrameInfo()
        // do we only set the ranges we use? do we only set these ranges if control option enabled?
        // we could use FrameConfig methods hasSpeed, hasIncline, hasResistance to conditionally include these?
        // should the setting of these be tied to the target setting feature flags?
        setSpeedRange(frameConfig, frame)
        setInclinationRange(frameConfig, frame)
        setResistanceLevelRange(frameConfig, frame)
        frame.machine = when (frameConfig.machineClassType) {
            MachineClassificationType.Climbmill -> FtmsStepClimberConsole()
            MachineClassificationType.Stepper -> FtmsStepClimberConsole()
            MachineClassificationType.Treadmill -> FtmsTreadmillConsole()
            MachineClassificationType.Bike, MachineClassificationType.SpinBike -> FtmsIndoorBikeConsole()
            MachineClassificationType.Ascent, MachineClassificationType.Elliptical , MachineClassificationType.RecumbentElliptical -> FtmsCrossTrainerConsole()
            else -> {
                Log.e(TAG, "Don't know how to create updater for machine type " + frameConfig.machineClassType)
                null
            }
        }
        return frame
    }

    private fun setSpeedRange(frameConfig: FrameConfig, frame: FtmsFrameInfo) {
        frame.supportedSpeedRange = SupportedSpeedRange()
        frame.supportedSpeedRange?.minimumSpeed = frameConfig.speedKph.min.toDouble()
        frame.supportedSpeedRange?.maximumSpeed = frameConfig.speedKph.max.toDouble() // should this be getEffectiveMaxSpeedKph?
        frame.supportedSpeedRange?.minimumIncrement = frameConfig.speedKph.stepSize.toDouble()
    }

    private fun setInclinationRange(frameConfig: FrameConfig, frame: FtmsFrameInfo) {
        frame.supportedInclinationRange = SupportedInclinationRange()
        frame.supportedInclinationRange?.minimumInclination = frameConfig.incline.min.toDouble()
        frame.supportedInclinationRange?.maximumInclination = frameConfig.incline.max.toDouble()
        frame.supportedInclinationRange?.minimumIncrement = frameConfig.incline.stepSize.toDouble()
    }

    private fun setResistanceLevelRange(frameConfig: FrameConfig, frame: FtmsFrameInfo) {
        frame.supportedResistanceLevelRange = SupportedResistanceLevelRange()
        frame.supportedResistanceLevelRange?.setMinimumResistanceLevel(frameConfig.resistance.min.toInt())
        frame.supportedResistanceLevelRange?.setMaximumResistanceLevel(frameConfig.resistance.max.toInt())
        frame.supportedResistanceLevelRange?.setMinimumIncrement(frameConfig.resistance.stepSize.toInt())
    }

    override fun start() {
        super.start()
        dataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) { metric: Boolean -> isMetric = metric }
        // FTMS goes reset, started, paused, stopped - do we set reset on splash reset, stat reset, workout launch, or panels idle? I think stat reset most aligns?
        dataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, { _ -> onWorkoutStop() }, false)
        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, { _ -> onWorkoutReset() }, false)
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value: Int -> eventRunState(value) }, false) // do not fire immediate or you will blow away reset state
        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, { kph: Float -> onSpeedChange(kph) }, false)
        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_INCLINE_FLOAT, { incline: Float -> onInclineChange(incline) },false)
        dataMatrix.createIntAdapter(HALKeys.ACTION_SET_RESISTANCE_INT, { resistance: Int -> onResistanceChange(resistance) }, false)
        // do you really need a timer if we have this listener?
        if (!BuildConfig.FTMS_DATA_ALWAYS_STREAM) {
            dataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, { _ -> eventUpdateDataTick() },false)
        }
    }

    override fun stop() {
        dataMatrix.finish() // clean up DataMatrix
        super.stop()
    }

    private fun eventUpdateDataTick() {
        //Log.d(TAG, "timer tick");
        machine?.update() // Apple GCA strictly expects update every 1 second (within error of 250ms)
    }

    private fun eventRunState(value: Int) {
        if (BuildConfig.FTMS_DATA_ALWAYS_STREAM) {
            // should timer just always run, even if no one is connected to receive it? should we just update on elapsed time change?
            if (value != WorkoutKeys.Constants.RUN_STATE_RUNNING) {
                Log.d(TAG, "timer stop")
                updateDataTimer.stop()
            } else {
                Log.d(TAG, "timer start")
                updateDataTimer.start()
            }
        }
        when (value) {
            WorkoutKeys.Constants.RUN_STATE_STOP -> {
                //Last send of FTMS status on a stop or pause
                machine?.update()
                onWorkoutStop()
            }
            WorkoutKeys.Constants.RUN_STATE_PAUSE -> {
                //Last send of FTMS status on a stop or pause
                machine?.update()
                onWorkoutPause()
            }
            WorkoutKeys.Constants.RUN_STATE_RUNNING -> onWorkoutStartOrResume()
        }
    }

    // should these be filtered by frame capability or the current target setting bits in FTMS features?
    override fun onSpeedChange(kph: Float) {
        if (frameConfig?.hasSpeed() == true) {
            super.onSpeedChange(kph)
        }
    }
    override fun onInclineChange(incline: Float) {
        if (frameConfig?.hasIncline() == true) {
            super.onInclineChange(incline)
        }
    }
    override fun onResistanceChange(resistance: Int) {
        if (frameConfig?.hasResistance() == true) {
            super.onResistanceChange(resistance)
        }
    }

    //region FTMS CONTROL HANDLING
    override fun onRequestControl(device: BluetoothDevice) {
        if (dialog == null || dialog?.isShowing == false) {
            Handler(Looper.getMainLooper()).post {
                val paragraphId = when (frameConfig?.machineClassType) {
                    MachineClassificationType.Treadmill -> R.string.ftms_control_treadmill_pg
                    MachineClassificationType.Elliptical, MachineClassificationType.Bike, MachineClassificationType.SpinBike,
                        MachineClassificationType.Stepper, MachineClassificationType.RecumbentElliptical -> R.string.ftms_control_bike_ep_pg
                    MachineClassificationType.Ascent -> R.string.ftms_control_ascent_pg
                    MachineClassificationType.Climbmill -> R.string.ftms_control_climbmill_pg
                    else -> 0
                }
                if (dialog == null) {
                    dialog = MessageBoxDialog(server.context, R.layout.message_box_accept_decline_layout)
                    dialog?.window?.setType(ChimeraEnums.TYPE_SYSTEM_ALERT)
                }
                dialog?.setMessageBox(
                    StringLookup.get(R.string.confirm_tc),
                    StringLookup.get(paragraphId)
                )
                dialog?.setCanceledOnTouchOutside(false)
                dialog?.setButton1Enabled(true)
                dialog?.setButton1OnClickListener { _ ->
                    dialog?.closeMessageBox()
                    deviceWithControlRequest = device
                    // update the run state so the client know what state we are in
                    eventRunState(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT))
                }
                dialog?.show()
            }
        }
    }

    override fun resetControlToDefaults(): ControlPointResult {
        // original FTMS code did not implement this, but it is mandatory in the spec, so should probably return success ?
        // "shall set the control parameters to their respective default values (e.g., target speed set to 0, inclination set to 0)" ?
        frameConfig?.let {
            setSpeed(it.speedKph.defaultValue)
            setIncline(it.incline.defaultValue)
            setResistance(it.resistance.defaultValue.toInt())
        }
        // "if the Fitness Machine supports the Remaining Time and Elapsed Time features, it shall set the time-related fields to 0" ?
        //DataMatrix.putInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, 0); // can we do this in a running workout? may need more investigation
        return ControlPointResult.Success
    }
    override fun setSpeed(kph: Float): ControlPointResult {
        // don't allow to change if the workout isn't running
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING) return ControlPointResult.OperationFailed
        frameConfig?.let {
            if (kph < it.speedKph.min || kph > it.speedKph.max) return ControlPointResult.InvalidParameter
            DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, kph)
        }
        return ControlPointResult.Success
    }
    override fun setIncline(incline: Float): ControlPointResult {
        // don't allow to change if the workout isn't running
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING) return ControlPointResult.OperationFailed
        frameConfig?.let {
            if (incline < it.incline.min || incline > it.incline.max) return ControlPointResult.InvalidParameter
            DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, it.incline.getClosestStepValue(incline))
        }
        return ControlPointResult.Success
    }
    override fun setResistance(resistance: Int): ControlPointResult {
        // don't allow to change if the workout isn't running
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING) return ControlPointResult.OperationFailed
        frameConfig?.let {
            if (resistance < it.resistance.min || resistance > it.resistance.max) return ControlPointResult.InvalidParameter
            DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, resistance)
        }
        return ControlPointResult.Success
    }

    override fun startResumeWorkout(): ControlPointResult {
        // if the workout is already running, just return success
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_RUNNING) return ControlPointResult.Success
        // make sure it's safe to start
        if (!DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)) {
            //Display a message due to the SafeToStart restriction
            val isSafeToStartShow = DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT)
            SafeToStartFlags.checkSafeToStartWithNotification(isSafeToStartShow)

		return ControlPointResult.OperationFailed
	}
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT)
        } else {
            DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, KeyPress.KeyState.KEY_DOWN))
            DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, KeyPress.KeyState.KEY_UP))
        }
        return ControlPointResult.Success
    }
    override fun pauseStopWorkout(pause: Boolean): ControlPointResult {
        if (pause) {
            // can't pause if the workout is not running
            ///if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING) return ControlPointResult.OperationFailed;
            DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT)
        } else {
            // can't stop if the workout is not running/paused
            ///if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) return ControlPointResult.OperationFailed;
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT)
        }
        return ControlPointResult.Success
    }

    override fun bikeSimulation(gradePercent: Float, wind: Float, rollingCoefficient: Float, windCoefficient: Float): ControlPointResult {
        // we need to come up with some formula to convert all these parameters to a resistance level
        // first stab is | graderPercentPerResistance = 15 / frame resistance range | to compute the what grade % range equals to 1 resistance change
        // then take the | resistance = grade % / graderPercentPerResistance |
        frameConfig?.let {
            val graderPercentPerResistance = 15f / (it.resistance.maxModel - it.resistance.minModel)
            val resistance = it.resistance.clamp(Math.round(gradePercent / graderPercentPerResistance) + 1) // resistance start from 1 so add 1
            if (BuildConfig.DEBUG) Log.d(TAG, "FTMS: graderPercentPerResistance: $graderPercentPerResistance")
            if (BuildConfig.DEBUG) Log.d(TAG, "FTMS: resistance: $resistance")
            DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, resistance)
        }
        return ControlPointResult.Success // think original one really returns OpCodeNotSupported because it does not set result?
    }
    //endregion FTMS CONTROL HANDLING


    //region MACHINE SPECIFIC CLASSES
    protected inner class FtmsIndoorBikeConsole : FtmsIndoorBike() {
        override fun start() {
            dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) { kph: Float -> speedUpdated(kph) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) { kph: Float -> averageSpeedUpdated(kph) }
            dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT) { rpm: Int -> cadenceUpdated(rpm) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) { averageCadence: Float -> averageCadenceUpdated(averageCadence) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) { distanceKm: Float -> distanceUpdated(distanceKm) }
            dataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT) { resistance: Int -> resistanceUpdated(resistance) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_WATTS_FLOAT) { watts: Float -> powerUpdated(watts) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT) { averagePower: Float -> averagePowerUpdated(averagePower) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT) { calories: Float -> caloriesUpdated(calories) }
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT) { heartRate: Int -> heartRateUpdated(heartRate) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) { elapsedTimeMs: Int -> elapsedTimeUpdated(elapsedTimeMs.toFloat()) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT) { goalRemaining: Float -> goalRemainingUpdated(goalRemaining) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT) { value: Float -> metsUpdated(value) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) { caloriesPerSec: Float -> caloricRateUpdated(caloriesPerSec) }
        }
        override fun stop() {
            // since we use parent's dataMatrix, finishing here would be redundant but not an issue either
        }

        fun speedUpdated(kph: Float) {
            dataValue.instantaneousSpeed = fixSpeed(kph).toDouble()
        }

        fun averageSpeedUpdated(kph: Float) {
            dataValue.averageSpeed = fixSpeed(kph).toDouble()
        }

        fun powerUpdated(watts: Float) {
            dataValue.instantaneousPower = floor(watts.toDouble()) // we probably floor for watches similar to the "fixSpeed" ?
        }

        fun cadenceUpdated(rpm: Int) {
            dataValue.instantaneousCadence = rpm.toDouble()
            //updateRotations(rpm, systemTimeInSeconds()); // for CSC, moving that to its own service
        }

        fun elapsedTimeUpdated(elapsedTimeMs: Float) {
            dataValue.elapsedTime = elapsedTimeMs / 1000.0
        }

        fun goalRemainingUpdated(goalRemaining: Float) {
            dataValue.remainingTime = GoalConverter.convert(DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT), ConsoleWorkoutVariables.GOAL_TYPE_TIME, goalRemaining) / 1000
        }

        fun heartRateUpdated(heartRate: Int) {
            dataValue.heartRate = heartRate.toDouble()
        }

        fun caloriesUpdated(calories: Float) {
            //we floor the calories to stay consistent with gymkit/workout panel display of calories.
            //this is a convenient hack and not standard - so maybe no need auto generate functionality in dataValue class
            dataValue.totalEnergy = floor(calories.toDouble())
        }

        fun caloricRateUpdated(caloriesPerSec: Float) {
            dataValue.energyPerHour = (caloriesPerSec * 60f * 60f).toDouble()
            dataValue.energyPerMinute = (caloriesPerSec * 60f).toDouble()
        }

        fun averageCadenceUpdated(averageCadence: Float) {
            dataValue.averageCadence = floor(averageCadence.toDouble())
        }

        fun distanceUpdated(distanceKm: Float) {
            dataValue.totalDistance = floor((ConversionHelper.roundFloor(distanceKm, 3 ) * 1000).toDouble())
        }

        fun resistanceUpdated(resistance: Int) {
            dataValue.resistanceLevel = resistance.toDouble()
        }

        fun averagePowerUpdated(averagePower: Float) {
            dataValue.averagePower = floor(averagePower.toDouble())
        }

        fun metsUpdated(value: Float) {
            dataValue.metabolicEquivalent = value.toDouble()
        }
    } // END CLASS: FtmsIndoorBikeConsole

    protected inner class FtmsTreadmillConsole : FtmsTreadmill() {
        override fun start() {
            Log.d(TAG, "Initializing dataValueUpdater")
            dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) { kph: Float -> speedUpdated(kph) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) { kph: Float -> averageSpeedUpdated(kph) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) { distanceKm: Float -> distanceUpdated(distanceKm) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT) { incline: Float -> inclineUpdated(incline) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) { elevation: Float -> elevationUpdated(elevation) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT) { calories: Float -> caloriesUpdated(calories) }
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT) { heartRate: Int -> heartRateUpdated(heartRate) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) { value: Int -> elapsedTimeUpdated(value.toFloat()) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT) { goalRemaining: Float -> goalRemainingUpdated(goalRemaining) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_WATTS_FLOAT) { watts: Float -> powerUpdated(watts) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT) { value: Float -> metsUpdated(value) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) { caloriesPerSec: Float -> caloricRateUpdated(caloriesPerSec) }
        }
        override fun stop() {
            // since we use parent's dataMatrix, finishing here would be redundant but not an issue either
        }

        fun speedUpdated(kph: Float) {
            dataValue.instantaneousSpeed = fixSpeed(kph).toDouble()
            // The unit seems incorrect. GATT spec shows unit is speed and not pace. I guess if we don't include it, we can't be wrong. pace can be compute from speed
            //dataValue.setInstantaneousPace(60f / kph); // when GATT spec says km/minute I think they actually mean minute/km
        }

        fun averageSpeedUpdated(kph: Float) {
            dataValue.averageSpeed = fixSpeed(kph).toDouble()
            // The unit seems incorrect. GATT spec shows unit is speed and not pace. I guess if we don't include it, we can't be wrong. ave pace can be compute from ave speed
            //dataValue.setAveragePace(60f / kph); // when GATT spec says km/minute I think they actually mean minute/km
        }

        fun inclineUpdated(incline: Float) {
            dataValue.inclination = incline.toDouble()
        }

        fun heartRateUpdated(heartRate: Int) {
            dataValue.heartRate = heartRate.toDouble()
        }

        fun caloriesUpdated(calories: Float) {
            //we floor the calories to stay consistent with gymkit/workout panel display of calories.
            //this is a convenient hack and not standard - so maybe no need auto generate functionality in dataValue class
            dataValue.totalEnergy = floor(calories.toDouble())
        }

        fun caloricRateUpdated(caloriesPerSec: Float) {
            dataValue.energyPerHour = (caloriesPerSec * 60f * 60f).toDouble()
            dataValue.energyPerMinute = (caloriesPerSec * 60f).toDouble()
        }

        fun elevationUpdated(elevation: Float) {
            dataValue.positiveElevationGain = floor(elevation.toDouble())
            dataValue.negativeElevationGain = 0.0
        }

        fun distanceUpdated(distanceKm: Float) {
            dataValue.totalDistance = floor((ConversionHelper.roundFloor(distanceKm, 3) * 1000).toDouble())
        }

        fun powerUpdated(watts: Float) {
            dataValue.powerOutput = floor(watts.toDouble())
        }

        fun elapsedTimeUpdated(value: Float) {
            dataValue.elapsedTime = value / 1000.0
        }

        fun goalRemainingUpdated(goalRemaining: Float) {
            dataValue.remainingTime = GoalConverter.convert(DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT), ConsoleWorkoutVariables.GOAL_TYPE_TIME, goalRemaining) / 1000
        }

        fun metsUpdated(value: Float) {
            dataValue.metabolicEquivalent = value.toDouble()
        }
    } // END CLASS: FtmsTreadmillConsole

    protected inner class FtmsCrossTrainerConsole : FtmsCrossTrainer() {
        override fun setControlFeatures(ftmsFeatureValue: FitnessMachineFeature) {
            super.setControlFeatures(ftmsFeatureValue)
            if (frameConfig?.machineClassType == MachineClassificationType.Ascent) {
                ftmsFeatureValue.setTargetSettings(FitnessMachineFeature.TargetSettingField.InclinationTargetSettingSupported)
            }
        }

        override fun start() {
            dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) { kph: Float -> speedUpdated(kph) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) { kph: Float -> averageSpeedUpdated(kph) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) { distanceKm: Float -> distanceUpdated(distanceKm) }
            dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT) { rpm: Int -> stepPerMinuteUpdated(rpm) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) { averageRpm: Float -> averageStepRateUpdated(averageRpm) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_STRIDE_COUNT_FLOAT) { strideCount: Float -> strideCountUpdated(strideCount) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) { elevation: Float -> elevationUpdated(elevation) } // Apple requires this, even if it is always zero
            dataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT) { resistance: Int -> resistanceUpdated(resistance) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_WATTS_FLOAT) { watts: Float -> powerUpdated(watts) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT) { averagePower: Float -> averagePowerUpdated(averagePower) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT) { calories: Float -> caloriesUpdated(calories) }
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT) { heartRate: Int -> heartRateUpdated(heartRate) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) { elapsedTimeMs: Int -> elapsedTimeUpdated(elapsedTimeMs.toFloat()) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT) { remainingGoal: Float -> remainingGoal(remainingGoal) }
            // ascent incline is not real incline so commenting this out
            //if(frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Ascent) {
            //    dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, dataValue::setInclination);
            //}
            dataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT) { value: Float -> metsUpdated(value) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) { caloriesPerSec: Float -> caloricRateUpdated(caloriesPerSec) }
        }
        override fun stop() {
            // since we use parent's dataMatrix, finishing here would be redundant but not an issue either
        }

        fun distanceUpdated(distanceKm: Float) {
            dataValue.totalDistance = floor((ConversionHelper.roundFloor(distanceKm, 3) * 1000).toDouble())
        }

        fun speedUpdated(kph: Float) {
            dataValue.instantaneousSpeed = fixSpeed(kph).toDouble()
        }

        fun averageSpeedUpdated(kph: Float) {
            dataValue.averageSpeed = fixSpeed(kph).toDouble()
        }

        fun stepPerMinuteUpdated(rpm: Int) {
            dataValue.stepPerMinute = (rpm * 2).toDouble()
        }

        fun averageStepRateUpdated(averageRpm: Float) {
            dataValue.averageStepRate = floor(averageRpm.toDouble()) * 2
        }

        fun elevationUpdated(elevation: Float) {
            dataValue.positiveElevationGain = floor(elevation.toDouble())
            dataValue.negativeElevationGain = 0.0
        }

        fun resistanceUpdated(resistance: Int) {
            dataValue.resistanceLevel = resistance.toDouble()
        }

        fun powerUpdated(watts: Float) {
            dataValue.instantaneousPower = floor(watts.toDouble())
        }

        fun averagePowerUpdated(averagePower: Float) {
            dataValue.averagePower = floor(averagePower.toDouble())
        }

        fun heartRateUpdated(heartRate: Int) {
            dataValue.heartRate = heartRate.toDouble()
        }

        fun caloriesUpdated(calories: Float) {
            //we floor the calories to stay consistent with gymkit/workout panel display of calories.
            //this is a convenient hack and not standard - so maybe no need auto generate functionality in dataValue class
            dataValue.totalEnergy = floor(calories.toDouble())
        }

        fun caloricRateUpdated(caloriesPerSec: Float) {
            dataValue.energyPerHour = (caloriesPerSec * 60f * 60f).toDouble()
            dataValue.energyPerMinute = (caloriesPerSec * 60f).toDouble()
        }

        fun elapsedTimeUpdated(elapsedTimeMs: Float) {
            dataValue.elapsedTime = elapsedTimeMs / 1000.0
        }

        fun remainingGoal(remainingGoal: Float) {
            dataValue.remainingTime = GoalConverter.convert(DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT), ConsoleWorkoutVariables.GOAL_TYPE_TIME, remainingGoal) / 1000
        }

        fun strideCountUpdated(strideCount: Float) {
            dataValue.strideCount = floor(strideCount.toDouble())
        }

        fun metsUpdated(value: Float) {
            dataValue.metabolicEquivalent = value.toDouble()
        }
    } // END CLASS: FtmsCrossTrainerConsole

    protected inner class FtmsStairClimberConsole : FtmsStairClimber() {
        override fun start() {
            Log.d(TAG, "Initializing ClimbmillDataUpdater")
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_FLOORS_FLOAT) { floors: Float -> floorsUpdated(floors) }
            dataMatrix.createIntAdapter(HALKeys.DATA_SPM_INT) { spm: Int -> spmUpdated(spm) }
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT) { heartRate: Int -> heartRateUpdated(heartRate) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) { elevation: Float -> elevationUpdated(elevation) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT) { calories: Float -> caloriesUpdated(calories) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_STEPS_INT) { stepCount: Int -> stepsUpdated(stepCount) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) { elapsedTimeMs: Int -> elapsedTimeUpdated(elapsedTimeMs) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT) { remaining: Float -> goalRemainingUpdated(remaining) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_AVERAGE_SPM_INT) { averageSpm: Int -> averageSpmUpdated(averageSpm) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT) { value: Float -> metsUpdated(value) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) { caloriesPerSec: Float -> caloricRateUpdated(caloriesPerSec) }
        }
        override fun stop() {
            // since we use parent's dataMatrix, finishing here would be redundant but not an issue either
        }

        fun caloriesUpdated(calories: Float) {
            dataValue.totalEnergy = floor(calories.toDouble())
        }

        fun caloricRateUpdated(caloriesPerSec: Float) {
            dataValue.energyPerHour = (caloriesPerSec * 60f * 60f).toDouble()
            dataValue.energyPerMinute = (caloriesPerSec * 60f).toDouble()
        }

        fun floorsUpdated(floors: Float) {
            dataValue.floors = floor(floors.toDouble())
        }

        fun heartRateUpdated(heartRate: Int) {
            dataValue.heartRate = heartRate.toDouble()
        }

        fun spmUpdated(spm: Int) {
            dataValue.stepPerMinute = spm.toDouble()
        }

        fun elevationUpdated(elevation: Float) {
            dataValue.positiveElevationGain = floor(elevation.toDouble())
        }

        fun stepsUpdated(stepCount: Int) {
            dataValue.strideCount = stepCount.toDouble()
        }

        fun elapsedTimeUpdated(elapsedTimeMs: Int) {
            dataValue.elapsedTime = elapsedTimeMs / 1000.0
        }

        fun averageSpmUpdated(averageSpm: Int) {
            dataValue.averageStepRate = averageSpm.toDouble()
        }

        fun goalRemainingUpdated(remaining: Float) {
            dataValue.remainingTime = GoalConverter.convert(DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT), ConsoleWorkoutVariables.GOAL_TYPE_TIME, remaining) / 1000
        }

        fun metsUpdated(value: Float) {
            dataValue.metabolicEquivalent = value.toDouble()
        }
    } // END CLASS: FtmsStairClimberConsole

    protected inner class FtmsStepClimberConsole : FtmsStepClimber() {
        override fun start() {
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_FLOORS_FLOAT) { floors: Float -> floorsUpdated(floors) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_STEPS_INT) { stepCount: Int -> stepsUpdated(stepCount) }
            dataMatrix.createIntAdapter(HALKeys.DATA_SPM_INT) { stepsPerMinute: Int -> stepPerMinuteUpdated(stepsPerMinute) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_AVERAGE_SPM_INT) { averageSpm: Int -> averageStepRateUpdated(averageSpm.toFloat()) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) { elevation: Float -> elevationUpdated(elevation) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT) { calories: Float -> caloriesUpdated(calories) }
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT) { heartRate: Int -> heartRateUpdated(heartRate) }
            dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) { elapsedTimeMs: Int -> elapsedTimeUpdated(elapsedTimeMs.toFloat()) }
            dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT) { goalRemaining: Float -> goalRemaining(goalRemaining) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT) { value: Float -> metsUpdated(value) }
            dataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) { caloriesPerSec: Float -> caloricRateUpdated(caloriesPerSec) }
        }
        override fun stop() {
            // since we use parent's dataMatrix, finishing here would be redundant but not an issue either
        }

        fun floorsUpdated(floors: Float) {
            dataValue.floors = floor(floors.toDouble())
        }

        fun stepsUpdated(stepCount: Int) {
            dataValue.stepCount = stepCount.toDouble()
        }

        fun stepPerMinuteUpdated(stepsPerMinute: Int) {
            dataValue.stepPerMinute = stepsPerMinute.toDouble()
        }

        fun averageStepRateUpdated(averageSpm: Float) {
            dataValue.averageStepRate = floor(averageSpm.toDouble())
        }

        fun elevationUpdated(elevation: Float) {
            dataValue.positiveElevationGain = floor(elevation.toDouble())
        }

        fun heartRateUpdated(heartRate: Int) {
            dataValue.heartRate = heartRate.toDouble()
        }

        fun caloriesUpdated(calories: Float) {
            //we floor the calories to stay consistent with gymkit/workout panel display of calories.
            //this is a convenient hack and not standard - so maybe no need auto generate functionality in dataValue class
            dataValue.totalEnergy = floor(calories.toDouble())
        }

        fun caloricRateUpdated(caloriesPerSec: Float) {
            dataValue.energyPerHour = (caloriesPerSec * 60f * 60f).toDouble()
            dataValue.energyPerMinute = (caloriesPerSec * 60f).toDouble()
        }

        fun elapsedTimeUpdated(elapsedTimeMs: Float) {
            dataValue.elapsedTime = elapsedTimeMs / 1000.0
        }

        fun goalRemaining(goalRemaining: Float) {
            dataValue.remainingTime = GoalConverter.convert(DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT), ConsoleWorkoutVariables.GOAL_TYPE_TIME, goalRemaining) / 1000
        }

        fun metsUpdated(value: Float) {
            dataValue.metabolicEquivalent = value.toDouble()
        }
    } // END CLASS: FtmsStepClimberConsole
    //endregion MACHINE SPECIFIC CLASSES
}
