package com.jht.mock.interfaces.models

import android.os.Bundle
import com.jht.data.FrameControlCollection
import com.jht.data.GraphInfoChannel
import com.jht.data.TrackInfo
import com.jht.data.sprint8.Sprint8SweatScores
import com.jht.enums.CaloriesSource
import com.jht.enums.WorkoutGoalType
import com.jht.enums.WorkoutId
import com.jht.enums.WorkoutRunState
import com.jht.enums.WorkoutStage
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatValueListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.ParcelableVariableListener
import com.jht.interfaces.StringVariableListener
import com.jht.interfaces.models.WorkoutModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockBoolVariable
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockEventListener
import com.jht.mock.interfaces.MockFloatValue
import com.jht.mock.interfaces.MockIntValue
import com.jht.mock.interfaces.MockParcelableValue
import com.jht.mock.interfaces.MockParcelableVariable
import com.jht.mock.interfaces.MockStringValue
import com.jht.mock.interfaces.MockStringVariable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MockWorkoutModel(
    override val distancePrecision: Int = 2,
    override val is24HourFormat: Boolean = false,
    override var isMetric: MockBoolVariable = MockBoolVariable(),
    override val wattsDisplayAllowed: Boolean = true,
    override val cadenceDisplayAllowed: Boolean = false,
    override val workoutId: MockDataValue<WorkoutId> = MockDataValue(WorkoutId.MANUAL),
    override val workoutStage: MockDataValue<WorkoutStage> = MockDataValue(WorkoutStage.WORKOUT),
    override val workoutRunState: MockDataValue<WorkoutRunState> = MockDataValue(WorkoutRunState.STOPPED),
    override val allowUserGoalChanges: MockBoolValue = MockBoolValue(true),
    override val clockUpdateEvent: MockEventListener = MockEventListener(),
    override val caloriesSource: ParcelableValueListener<CaloriesSource> = MockParcelableValue(CaloriesSource.Standard),
    override val calories: MockFloatValue = MockFloatValue(),
    override val caloriesPerHour: MockIntValue = MockIntValue(),
    override val distanceKm: MockFloatValue = MockFloatValue(),
    override val elevationMeter: MockFloatValue = MockFloatValue(),
    override val floors: MockFloatValue = MockFloatValue(),
    override val heartRate: MockIntValue = MockIntValue(),
    override val incline: MockFloatValue = MockFloatValue(),
    override val mets: MockFloatValue = MockFloatValue(),
    override val myZoneHeartRatePercent: MockIntValue = MockIntValue(),
    override val myZoneMeps: MockIntValue = MockIntValue(),
    override val paceSecPerKm: MockIntValue = MockIntValue(),
    override val rpm: MockIntValue = MockIntValue(),
    override val speedKph: MockFloatValue = MockFloatValue(),
    override val spm: MockIntValue = MockIntValue(),
    override val steps: MockIntValue = MockIntValue(),
    override val averageStrideLengthMeters: MockFloatValue = MockFloatValue(),
    override val strideLengthMeters: MockFloatValue = MockFloatValue(),
    override val watts: MockFloatValue = MockFloatValue(),
    override val averageSpeedKph: MockFloatValue = MockFloatValue(),
    override val averageSpm: MockIntValue = MockIntValue(),
    override val peakHeartRate: MockIntValue = MockIntValue(),
    override val targetHeartRatePercent: MockFloatValue = MockFloatValue(),
    override val timeElapsedMs: MockIntValue = MockIntValue(),
    override val goalRemaining: MockFloatValue = MockFloatValue(),
    override val pauseTimerRemaining: MockStringValue = MockStringValue(),
    override val goalTotal: MockFloatValue = MockFloatValue(),
    override val goalType: MockDataValue<WorkoutGoalType> = MockDataValue(WorkoutGoalType.GOAL_TYPE_TIME),
    override val gymkitCalories: MockFloatValue = MockFloatValue(-1f),
    override val resistance: IntValueListener = MockIntValue(),
    override val intervalNumber: IntValueListener = MockIntValue(),
    override val sprint8State: IntValueListener = MockIntValue(),
    override val intervalGoalRemaining: FloatValueListener = MockFloatValue(),
    override val sweatScore: FloatValueListener = MockFloatValue(),
    override val intervalRemaining: FloatValueListener = MockFloatValue(),
    override val isWorkoutRunning: BoolValueListener = MockBoolValue(),
    override val isRamping: BoolValueListener = MockBoolValue(),
    override val isSprinting: BoolValueListener = MockBoolValue(),
    override val isWorkoutPaused: MockBoolValue = MockBoolValue(),
    override val isWorkoutSwitching: BoolValueListener = MockBoolValue(),
    override val isWorkoutStopped: BoolValueListener = MockBoolValue(),
    override val isWorkoutLaunching: BoolValueListener = MockBoolValue(),
    override val trackDataParcelable: ParcelableValueListener<TrackInfo> = MockParcelableValue(
        TrackInfo()
    ),
    override val sweatScoreParcel: ParcelableValueListener<Sprint8SweatScores> = MockParcelableValue(
        Sprint8SweatScores()
    ),
    override val maxSpeedKph: Float = -1f,
    override val maxIncline: Float = -1f,
    override val maxResistance: Float = -1f,
    override val workoutAppSetupBundle: ParcelableVariableListener<Bundle> = MockParcelableVariable(
        Bundle()
    ),
    override val inclineGraphDataPoints: ParcelableValueListener<GraphInfoChannel> = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val heartGraphDataPoints: ParcelableValueListener<GraphInfoChannel>  = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val resistanceGraphDataPoints: ParcelableValueListener<GraphInfoChannel> = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val speedGraphDataPoints: ParcelableValueListener<GraphInfoChannel> = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val rpmGraphDataPoint: ParcelableValueListener<GraphInfoChannel> = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val spmGraphDataPoint: ParcelableValueListener<GraphInfoChannel> = MockParcelableValue(
        GraphInfoChannel()
    ),
    override val graphUpdateEvent: EventListener = MockEventListener(),
    override val workoutOptionId: StringVariableListener = MockStringVariable(),
    override val onStartWorkout:  EventListener = MockEventListener(),
    override val onResumeWorkout: EventListener = MockEventListener(),
    override val scriptNextIncline: FloatValueListener = MockFloatValue(),
    override val scriptNextSpeed: FloatValueListener = MockFloatValue(),
    override val scriptNextResistance: IntValueListener = MockIntValue(),
    override val scriptIsSprinting: BoolValueListener = MockBoolValue(),
    override val scriptIsRamping: BoolValueListener = MockBoolValue(),
    override val intervalGoal: FloatValueListener = MockFloatValue(),
    override val numberOfSprints: IntValueListener = MockIntValue(),
    override val workoutLevel: IntValueListener = MockIntValue(),
    override val supportsReverseBelt: Boolean = false,
    override val isSafeToStartWorkout: BoolValueListener = MockBoolValue(),
    override val isBeltInReverse: BoolValueListener = MockBoolValue(),
    override val singleAppMode: String = "",
    override val rpmTarget: IntValueListener = MockIntValue(),
    override val targetDistanceKm: MockFloatValue = MockFloatValue(15f),
    override val targetTimeGoalMinutes: MockIntValue = MockIntValue(2),
    override val targetTrainingType: MockIntValue = MockIntValue(),
    override val targetDistanceMinKm: Float = 0f,
    override val targetDistanceMaxKm: Float = 0f,
    override val targetDistanceDefaultKm: Float = 0f,
    override val targetTimeMinMinutes: Int = 0,
    override val targetTimeMaxMinutes: Int = 0,
    override val targetTimeDefaultMinutes: Int = 0,
    override val rpmZoneLow: MockFloatValue = MockFloatValue(),
    override val rpmZoneHigh: MockFloatValue = MockFloatValue(),
    override val ftpZoneLow: MockFloatValue = MockFloatValue(),
    override val ftpZoneHigh: MockFloatValue = MockFloatValue(),
    override val targetBpm: MockIntValue = MockIntValue(),
    override val bpmZoneLow: MockIntValue = MockIntValue(),
    override val bpmZoneHigh: MockIntValue = MockIntValue(),
    override val targetCaloriesUpdate: MockIntValue = MockIntValue(),
    override val rpmTargetMinValue: Int = 30,
    override val rpmTargetMaxValue: Int = 150,
    override val rpmTargetDefaultValue: Int = 60,
    override val bpmMinValue: Int = 80,
    override val bpmMaxValue: Int = 210,
    override val bpmDefaultValue: Int = 192,
    override val ftpMinValue: Int = 50,
    override val ftpMaxValue: Int = 500,
    override val ftpDefaultValue: Int = 200,
    override val caloriesMinValue: Int = 10,
    override val caloriesMaxValue: Int = 1000,
    override val caloriesDefaultValue: Int = 300,
    override val dataUserAgeDefault: IntValueListener = MockIntValue(),
    override val intervalWorkTimeGoal: FloatValueListener = MockFloatValue(),
    override val intervalWorkDistanceGoal: FloatValueListener = MockFloatValue(),
    override val intervalRestTimeGoal: FloatValueListener = MockFloatValue(),
    override val numberOfIntervals: IntValueListener = MockIntValue(),

    ) : WorkoutModel() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    init {
        workoutStage.emit(WorkoutStage.WORKOUT)
        targetTrainingType.emit(3) // Represents the workout mode to be emitted (Rpm, Watts, HeartRate, Calories, Distance)
    }

    /**
     * Ticks distance and time upwards for previews. Not started from init: that needs a main dispatcher a JVM test
     * does not have, and a test dispatcher would pump the whole simulation into whatever is being asserted on.
     */
    fun startDistanceSimulation() {
        runCatching { // no main dispatcher, e.g. a JVM unit test
            scope.launch {
                var distance = 0f
                var elapsedMs = 0

                while (distance <= 23.49f) {
                    delay(50)

                    elapsedMs += 50
                    distance += 0.05f

                    distanceKm.emit(distance)
                    timeElapsedMs.emit(elapsedMs)
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    override var frameControls = MockDataValue(FrameControlCollection())

    override fun increaseWorkoutGoal() {
        // TODO
    }

    override fun decreaseWorkoutGoal() {
        // TODO
    }

    override fun hasFrameControl(): Boolean {
        return true
    }

    override fun updateWorkoutSpeed(speedKph: Float) {
        this.speedKph.emit(speedKph)
    }

    override fun updateWorkoutIncline(incline: Float) {
        this.incline.emit(incline)
    }

    override fun updateWorkoutResistance(resistance: Int) {
        (this.resistance as MockIntValue).emit(resistance)
    }

    override fun pauseWorkout() {
        return
    }

    override fun resumeWorkout() {
        return
    }

    override fun stopWorkout() {
        return
    }

    override fun actionSkipInterval() {
        return
    }

    override fun hotLevelUpdateSprint8(level: Int) {
        return
    }

    override val averageWatts: FloatValueListener
        get() = MockFloatValue()

    override fun setBeltInReverse(inReverse: Boolean) {
        // TODO
    }

    override val userFtp: IntValueListener
        get() = MockIntValue()
    override val maxHeartRate: FloatValueListener
        get() = MockFloatValue()
    // Target training setters record what was pushed down, and emit where a listener exists, as the console does
    var lastRpmTarget: Int? = null
    var lastFtpZoneSelectedIndex: Int? = null
    var lastUserFtp: Int? = null
    var lastBpmZoneSelectedIndex: Int? = null
    var lastUserMaxHr: Int? = null
    var lastUserAge: Int? = null
    var lastTargetCalories: Int? = null

    override fun setRpmTarget(value: Int) { lastRpmTarget = value }
    override fun setRpmZoneLow(value: Float) { rpmZoneLow.emit(value) }
    override fun setRpmZoneHigh(value: Float) { rpmZoneHigh.emit(value) }
    override fun setFtpZoneLow(value: Float) { ftpZoneLow.emit(value) }
    override fun setFtpZoneHigh(value: Float) { ftpZoneHigh.emit(value) }
    override fun setFtpZoneSelectedIndex(value: Int) { lastFtpZoneSelectedIndex = value }
    override fun setFtpZoneFitnessSelectedIndex(value: Int) {}
    override fun setUserFtp(value: Int) { lastUserFtp = value }
    override fun setTargetBpm(value: Int) { targetBpm.emit(value) }
    override fun setBpmZoneLow(value: Int) { bpmZoneLow.emit(value) }
    override fun setBpmZoneHigh(value: Int) { bpmZoneHigh.emit(value) }
    override fun setBpmZoneSelectedIndex(value: Int) { lastBpmZoneSelectedIndex = value }
    override fun setUserMaxHr(value: Int) { lastUserMaxHr = value }
    override fun setUserAge(value: Int) { lastUserAge = value }
    override fun setTargetCalories(value: Int) {
        lastTargetCalories = value
        targetCaloriesUpdate.emit(value)
    }

    override fun setTargetDistanceKm(value: Float) {
        targetDistanceKm.emit(value)
    }

    override fun setTargetTimeGoalMinutes(value: Int) {
        targetTimeGoalMinutes.emit(value)
    }

    override fun setTargetTrainingType(value: Int) {
        targetTrainingType.emit(value)
    }


    override fun clampSpeedToFrameLimits(speedKph: Float): Float {
        return speedKph
    }

    override fun clampInclineToFrameLimits(incline: Float): Float {
        return incline
    }

    override fun clampResistanceToFrameLimits(resistance: Int): Int {
        return resistance
    }
}
