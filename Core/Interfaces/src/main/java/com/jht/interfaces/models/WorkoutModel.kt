package com.jht.interfaces.models

import android.os.Bundle
import com.jht.data.FrameControlCollection
import com.jht.data.GraphInfoChannel
import com.jht.data.TrackInfo
import com.jht.data.sprint8.Sprint8SweatScores
import com.jht.enums.CaloriesSource
import com.jht.enums.WorkoutGoalType
import com.jht.enums.WorkoutId
import com.jht.enums.WorkoutStage
import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.FloatValueListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.ParcelableVariableListener
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.StringVariableListener
import com.jht.enums.WorkoutRunState

/**
 * Single source of truth for the workout-graph history window, shared by
 * BOTH graphs (the Compose ProfileGraphComponent and the View-based GraphSetup/WorkoutGraphThreaded).
 * Change [windowMinutes] once — e.g. 15, 30, or 120 — and the provider queries, graph axis width,
 * and in-memory snapshot cap all follow.
 */
object WorkoutGraphConfig {
    /** Sliding history window in minutes. */
    @JvmStatic
    val windowMinutes: Int = 120

    /** Window in seconds (derived). */
    @JvmStatic
    val windowSeconds: Int get() = windowMinutes * 60

    /** Window in milliseconds (derived). */
    @JvmStatic
    val windowMs: Int get() = windowMinutes * 60_000
}

/**
 * Provides a model for exposing and controlling workout stats
 */
abstract class WorkoutModel : BaseModel {

    /////////////Workout States//////////////////////////////////////

    /** Whether the console is actively running a workout */
    abstract val isWorkoutRunning: BoolValueListener

    /** Whether the console currently has a workout paused */
    abstract val isWorkoutPaused: BoolValueListener

    /** Whether the console is switching between workouts */
    abstract val isWorkoutSwitching: BoolValueListener

    /** Whether the console has not started a workout */
    abstract val isWorkoutStopped: BoolValueListener

    /** Whether the console is launching a workout */
    abstract val isWorkoutLaunching: BoolValueListener

    /** Event sent when a workout is launched */
    abstract val onStartWorkout: EventListener

    /** Event send when a workout is resumed from pause */
    abstract val onResumeWorkout: EventListener


    /////////////Workout Meta Data///////////////////////////////////

    /** Provides current workout stage. For example: warm up or cool down. */
    abstract val workoutStage: DataValueListener<WorkoutStage>
    /** Provides current workout run state. For example: stopped or running. */
    abstract val workoutRunState: DataValueListener<WorkoutRunState>

    /** Provides integer ID of the currently running workout. */
    abstract val workoutId: DataValueListener<WorkoutId>

    /** Indicates whether the current workout permits adjustment of the workout goals. */
    abstract val allowUserGoalChanges: DataValueListener<Boolean>

    /** Generic bundle of app data for the currently launched workout app. Generally setup information. */
    abstract val workoutAppSetupBundle: ParcelableVariableListener<Bundle>


    /////////////Workout Data Formatting///////////////////////////////////

    /** Clamps the value of speedKph to the limits of the current frame */
    abstract fun clampSpeedToFrameLimits(speedKph: Float) : Float

    /** Clamps the value of incline to the limits of the current frame */
    abstract fun clampInclineToFrameLimits(incline: Float) : Float

    /** Clamps the value of resistance to the limits of the current frame */
    abstract fun clampResistanceToFrameLimits(resistance: Int) : Int

    /** Provides the precision that should be used when presenting distance values
     * Currently only available as an experimental setting*/
    abstract val distancePrecision: Int

    /** Indicates if the system is currently reporting time in a 24 hour format versus 12 hour cycles */
    abstract val is24HourFormat: Boolean

    /** Indicates if workout information should be reported as metric, otherwise imperial. */
    abstract val isMetric: DataVariableListener<Boolean>

    /** Provides whether Watts information may need to suppressed due to limitations of the current frame */
    abstract val wattsDisplayAllowed: Boolean
    abstract val pauseTimerRemaining: StringValueListener

    /** Provides whether Cadence information may need to suppressed due to limitations of the current frame */
    abstract val cadenceDisplayAllowed: Boolean

    /////////////Workout Control/////////////////////////////////////

    /** Provides same handlers for frame controls as used by system panels */
    abstract val frameControls: DataValueListener<FrameControlCollection>
    /** Signals back to the provider that the workout goal should be increased by a step. The magnitude of the step depends on the workout running. */
    abstract fun increaseWorkoutGoal()
    /** Signals back to the provider that the workout goal should be decreased by a step. The magnitude of the step depends on the workout running. */
    abstract fun decreaseWorkoutGoal()
    /** Sends a new value as a parameter to update treadmill speed by using ACTION key **/
    abstract fun updateWorkoutSpeed(speedKph: Float)
    /** Sends a new value as a parameter to update Incline of the frame **/
    abstract fun updateWorkoutIncline(incline: Float)
    /** Sends a new value as a parameter to update resistance of the frame **/
    abstract fun updateWorkoutResistance(resistance: Int)
    abstract fun pauseWorkout()
    abstract fun resumeWorkout()
    abstract fun stopWorkout()
    abstract fun actionSkipInterval()

    abstract fun hotLevelUpdateSprint8(level: Int)


    /////////////Workout Data/////////////////////////////////////

    /** Signals to the consumer that the clock has been updated. */
    abstract val clockUpdateEvent: EventListener

    // current stats
    /** Provides the source for the calculation of the [calories] */
    abstract val caloriesSource: ParcelableValueListener<CaloriesSource>
    /** Provides float of calories burned during the current workout */
    abstract val calories: FloatValueListener
    /** Provides integer of calories burned per hour during the current workout */
    abstract val caloriesPerHour: IntValueListener
    /** Provides float of distance covered during the current workout in kilometers */
    abstract val distanceKm: FloatValueListener
    /** Provides float of elevation covered during the current workout in meters */
    abstract val elevationMeter: FloatValueListener
    /** Provides float of elevation covered during the current workout in floors */
    abstract val floors: FloatValueListener
    /** Provides integer of the current beat per minute reported to the system */
    abstract val heartRate: IntValueListener
    /** Provides float of current incline of the equipment as percent (for ascent this value is more like a stride length)
     * [See Treadmill incline](https://support.horizonfitness.com/hc/en-us/articles/4407794379149-What-Does-Incline-Mean-on-Treadmills-#:~:text=The%20gradient%20or%20treadmill%20grade,meters%20is%20a%2015%25%20grade)
     */
    abstract val incline: FloatValueListener
    /** Provides float of mets expended during the current workout */
    abstract val mets: FloatValueListener
    /** Provides integer of the current pace in seconds per kilometer */
    abstract val paceSecPerKm: IntValueListener
    /** Provides integer of the current rotations per minute, if reported by the frame */
    abstract val rpm: IntValueListener
    /** Provides float of the current speed in kilometers per hour */
    abstract val speedKph: FloatValueListener
    /** Provides integer of the current steps per minute, if reported by the frame */
    abstract val spm: IntValueListener
    /** Provides integer of the current accumulation of steps during the workout, if reported by the frame */
    abstract val steps: IntValueListener
    /** Provides float of the watts expended by the user during the current workout */
    abstract val watts: FloatValueListener
    /** Provides float of calories burned during the current workout as calculated for GymKit. If no device connected, it may return a value less than zero. */
    abstract val gymkitCalories: FloatValueListener
    /** Provides integer of the current resistance */
    abstract val resistance: IntValueListener

    abstract val averageWatts: FloatValueListener


    // MyZone stats (note MyZone calories overrides our regular calories in system layer
    /** Provides integer of MyZone effort */
    abstract val myZoneHeartRatePercent: IntValueListener
    /** Provides integer of MyZone MEPs */
    abstract val myZoneMeps: IntValueListener

    // average stats
    /** Provides integer average pace accumulated during the current workout in terms of seconds per unit distance */
    abstract val averageSpeedKph: FloatValueListener
    /** Provides integer average steps per minute during the current workout */
    abstract val averageSpm: IntValueListener


    /** Provides integer stride length during the current workout */
    abstract  val strideLengthMeters: FloatValueListener

    /** Provides integer average stride length during the current workout */
    abstract  val averageStrideLengthMeters: FloatValueListener

    // peak stats
    /** Provides integer of the peak beat per minute during the current workout */
    abstract val peakHeartRate: IntValueListener

    // misc stats
    /** Provides float of the target heart rate for the current workout */
    abstract val targetHeartRatePercent: FloatValueListener

    // goal stats
    /** Provides integer of the time that has passed during the current workout in milliseconds */
    abstract val timeElapsedMs: IntValueListener
    /** Provides float of the remaining goal time in the current workout in seconds */
    abstract val goalRemaining: FloatValueListener
    /** Provides float of the total goal time in the current workout in seconds */
    abstract val goalTotal: FloatValueListener
    /** Provides the type of goal associated with the current workout */
    abstract val goalType: DataValueListener<WorkoutGoalType>

    // interval from the workout script
    /**
     * Listener for the interval number. This number is manually set by the active script, and is only available in certain workout scripts.
     */
    abstract val intervalNumber: IntValueListener
    /**
     * Listener for the interval goal remaining. Returns the time remaining in an interval in a
     * workout script. This is available when the script sets an interval goal and is not available
     * for every script.
     */
    abstract val intervalGoalRemaining: FloatValueListener

    /**
     * Listener for track app
     */
    abstract val trackDataParcelable: ParcelableValueListener<TrackInfo>
    /**
     * Listener to handle graph maximum points
     */
    abstract val maxSpeedKph: Float
    abstract val maxIncline: Float
    abstract val maxResistance: Float

    /**
     * Represents an additional metadata string for an existing workout,
     * allowing extra information to be attached and managed dynamically.
     */
    abstract val workoutOptionId: StringVariableListener

    ///////////////////////////////////// Graph Data /////////////////////////////////////
    abstract val heartGraphDataPoints: ParcelableValueListener<GraphInfoChannel>
    abstract val speedGraphDataPoints: ParcelableValueListener<GraphInfoChannel>
    abstract val inclineGraphDataPoints: ParcelableValueListener<GraphInfoChannel>
    abstract val resistanceGraphDataPoints: ParcelableValueListener<GraphInfoChannel>
    abstract val rpmGraphDataPoint: ParcelableValueListener<GraphInfoChannel>
    abstract val spmGraphDataPoint: ParcelableValueListener<GraphInfoChannel>
    abstract val graphUpdateEvent: EventListener

    /**
     * Refreshes the snapshot of workout history. Suspends because the refresh may do
     * cross-process I/O; the implementation runs the work off the caller's thread.
     */
    open suspend fun refreshGraphData() {}


    /**
     * Listener for workout scripted events
     * */
    abstract val scriptNextIncline: FloatValueListener
    abstract val scriptNextSpeed: FloatValueListener
    abstract val scriptNextResistance: IntValueListener
    abstract val scriptIsSprinting: BoolValueListener
    abstract val scriptIsRamping: BoolValueListener

    //Sprint 8 listeners
    abstract val numberOfSprints: IntValueListener
    abstract val sweatScore: FloatValueListener
    abstract val intervalRemaining: FloatValueListener
    abstract val sprint8State: IntValueListener
    abstract val sweatScoreParcel: ParcelableValueListener<Sprint8SweatScores>
    abstract val isRamping: BoolValueListener
    abstract val isSprinting: BoolValueListener

    abstract val intervalGoal: FloatValueListener

    /** Provides the user-selected goal for work intervals in a time workout (in milliseconds) */
    abstract val intervalWorkTimeGoal: FloatValueListener

    /** Provides the user-selected goal for work intervals in a distance workout (in kilometers) */
    abstract val intervalWorkDistanceGoal: FloatValueListener

    /** Provides the user-selected goal for rest intervals (in milliseconds) */
    abstract val intervalRestTimeGoal: FloatValueListener

    /** Provides the user-selected number of intervals */
    abstract val numberOfIntervals: IntValueListener

    abstract val workoutLevel: IntValueListener

    abstract val isSafeToStartWorkout: BoolValueListener

    /**
     * Hybrid treadmill
     */
    abstract val supportsReverseBelt: Boolean // TODO this probably should move to a FrameModel
    abstract val isBeltInReverse: BoolValueListener
    /** If console is operating in a single app mode, this is the app ID to use, otherwise is empty string */
    abstract val singleAppMode: String
    abstract fun setBeltInReverse(inReverse: Boolean)
    abstract val userFtp: IntValueListener
    abstract val maxHeartRate: FloatValueListener
    abstract val rpmTarget: IntValueListener
    abstract val targetDistanceKm: FloatValueListener
    abstract val targetTimeGoalMinutes: IntValueListener
    abstract val targetTrainingType: IntValueListener
    abstract val targetDistanceMinKm: Float
    abstract val targetDistanceMaxKm: Float
    abstract val targetDistanceDefaultKm: Float

    abstract val targetTimeMinMinutes: Int
    abstract val targetTimeMaxMinutes: Int
    abstract val targetTimeDefaultMinutes: Int
    // --- RPM mode ---
    abstract val rpmZoneLow: FloatValueListener
    abstract val rpmZoneHigh: FloatValueListener

    // --- Watts/FTP mode ---
    abstract val ftpZoneLow: FloatValueListener
    abstract val ftpZoneHigh: FloatValueListener


    // --- BPM/Heart Rate mode ---
    abstract val targetBpm: IntValueListener
    abstract val bpmZoneLow: IntValueListener
    abstract val bpmZoneHigh: IntValueListener

    // --- Calories mode ---
    abstract val targetCaloriesUpdate: IntValueListener

    // WorkoutModel abstract additions
    abstract val rpmTargetMinValue: Int
    abstract val rpmTargetMaxValue: Int
    abstract val rpmTargetDefaultValue: Int

    abstract val bpmMinValue: Int
    abstract val bpmMaxValue: Int
    abstract val bpmDefaultValue: Int
    abstract val dataUserAgeDefault: IntValueListener

    abstract val ftpMinValue: Int
    abstract val ftpMaxValue: Int
    abstract val ftpDefaultValue: Int
    abstract val caloriesMinValue: Int
    abstract val caloriesMaxValue: Int
    abstract val caloriesDefaultValue: Int
    abstract fun setRpmTarget(value: Int)
    abstract fun setRpmZoneLow(value: Float)
    abstract fun setRpmZoneHigh(value: Float)

    abstract fun setFtpZoneLow(value: Float)
    abstract fun setFtpZoneHigh(value: Float)
    abstract fun setFtpZoneSelectedIndex(value: Int)
    abstract fun setFtpZoneFitnessSelectedIndex(value: Int)
    abstract fun setUserFtp(value: Int)
    abstract fun setTargetBpm(value: Int)
    abstract fun setBpmZoneLow(value: Int)
    abstract fun setBpmZoneHigh(value: Int)
    abstract fun setBpmZoneSelectedIndex(value: Int)
    abstract fun setUserMaxHr(value: Int)
    abstract fun setUserAge(value: Int)
    abstract fun setTargetCalories(value: Int)
    abstract fun setTargetDistanceKm(value: Float)
    abstract fun setTargetTimeGoalMinutes(value: Int)
    abstract fun setTargetTrainingType(value: Int)
    abstract fun hasFrameControl(): Boolean
}
