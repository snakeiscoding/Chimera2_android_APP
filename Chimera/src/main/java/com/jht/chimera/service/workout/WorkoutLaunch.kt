@file:JvmName("WorkoutLauncher") // expose package level functions as statics of WorkoutLauncher in Java code
package com.jht.chimera.service.workout

import android.content.Context
import android.os.Parcelable
import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.mastboard.data.UiEventKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.service.ui.LaunchAppCondition
import com.jht.chimera.service.ui.LaunchAppData
import com.jht.datamatrixlib.DataMatrix
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.concurrent.atomic.AtomicBoolean


/*
    Contains classes for launching workouts
    Largely replaces old .\Chimera\src\main\java\com\jht\chimera\lib\workout\WorkoutLauncher.java
    Initial conditions and settings were combined into WorkoutLaunchData. Used "JvmField" to avoid changing all the uses.
 */
private const val TAG = "WorkoutLauncher"


@Parcelize
data class InitialCondition(
    @JvmField val conditionName: String,
    @JvmField val conditionValue: Float,
) : Parcelable


@Parcelize
data class InitialConditionsList(
    @JvmField val initialConditions : List<InitialCondition>,
) : Parcelable


@Parcelize
data class WorkoutSettingOverrides(
    @JvmField val settings : MutableList<SettingValue> = mutableListOf(),
) : Parcelable {
    fun add(settingValue: SettingValue) { settings.add(settingValue) }
    fun add(setting: String, value: Float) { add(SettingValue(setting, value)) }
}


/** Class containing everything needed to launch workout in one transaction */
@Parcelize
data class WorkoutLaunchData(
    val initialConditionsList: InitialConditionsList,
    val workoutSettingOverrides: WorkoutSettingOverrides,
    val isSwitchWorkout: Boolean = false,
) : Parcelable {
    constructor(initialConditions : List<InitialCondition>, workoutSettingOverrides: WorkoutSettingOverrides, isSwitchWorkout: Boolean = false) : this(InitialConditionsList(initialConditions), workoutSettingOverrides, isSwitchWorkout)
}


/** Checks if a workout can be launched, or more accurately if there are specific blockers to launching a workout.
 *  Note that these conditions can change after being called! Consider it a pre-screening rather than an absolute.
 *  System has other code to react when these conditions change.
 */
fun canLaunchWorkout(
    isSwitchWorkout: Boolean,
) : Boolean {
    val config = FrameConfigManager.getInstance().frameConfig

    val isDemoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)
    val isSafeToStart = DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)

    if (!isSwitchWorkout && !isSafeToStart) {
        Log.d(TAG, "==> Not safe to start workout")
        safetyPinCheck() // safety PIN may be one of the safety factors, so prompt if needed

        //Display a message due to the SafeToStart restriction
        val isSafeToStartShow = DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT)
        if(!isSafeToStart) SafeToStartFlags.checkSafeToStartWithNotification(isSafeToStartShow)

        return false
    }

    // disable starting a workout for climb if safety is pulled (demo mode does not need to check this)
    if (!isDemoMode && config.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
        val status = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT)
        // should we just define a blocking key mask in frame config?
        val controlZoneTriggered = status and ChimeraEnums.SAFETY_KEY_CLIMBMILL_CZ > 0
        val irSensorTriggered = status and ChimeraEnums.SAFETY_KEY_CLIMBMILL_IR > 0
        val climbmillEStopTriggered = status and ChimeraEnums.SAFETY_KEY_CLIMBMILL_UCB > 0
        val handrailStopTriggered = status and ChimeraEnums.SAFETY_KEY_CLIMBMILL_HANDRAIL > 0
        if (controlZoneTriggered || irSensorTriggered || climbmillEStopTriggered || handrailStopTriggered) {
            return false
        }
    }
    return true
}
/** Trigger PIN prompt if needed, returns true if flow can proceed without prompt */
private fun safetyPinCheck() : Boolean {
    // if we blocked due to safety PIN being needed, trigger prompt now (go screen can do pre-checks to avoid having to retry, but this is last line of defense)
    if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL) && DataMatrix.getBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL)) {
        // don't think we can make the system wait here or make this async with callback without causing other problems?
        DataMatrix.putBoolean(UiServiceKeys.ACTION_SHOW_DIGITAL_SAFETY_PIN_PROMPT_BOOL, true)
        return false
    }
    return true
}


/** Attempts to launch a workout by sending launch data to workout service */
fun launchWorkout(
    initialConditions: List<InitialCondition>,
    isSwitchWorkout: Boolean,
    settingOverrides: WorkoutSettingOverrides
): Boolean {
    // perform a synchronous pre-check of conditions before attempting launch (note this is just a snapshot in time)
    if (!canLaunchWorkout(isSwitchWorkout)) return false

    // pass everything needed by the service to launch the workout, the service will handle blocking if a launch is in progress already
    val workoutLaunchData = WorkoutLaunchData(initialConditions, settingOverrides, isSwitchWorkout)
    DataMatrix.putParcelable(WorkoutKeys.ACTION_LAUNCH_WORKOUT_PARCELABLE, workoutLaunchData)
    return true
}


/** Helper for the workout service to track and perform workout launches */
class WorkoutLaunchServiceHelper(val context: Context) : AutoCloseable {
    private val dataMatrix = DataMatrix()
    private val safeToStart = MutableStateFlow(false)
    private val uiReady = MutableStateFlow<Boolean?>(null) // null = waiting, true = ok, false = fail

    private val callLock = Object() // lock calls to avoid any unexpected issues calling abort while starting up the jobs
    private val scope = CoroutineScope(Dispatchers.Default)
    private var launchJob : Job? = null
    private val launching = AtomicBoolean(false) // would Job.isActive be enough?
    private val aborting = AtomicBoolean(false)
    //private val lastWorkoutAborted = AtomicBoolean(false) // do we need to track if last workout was aborted? set in abort/clear in launch?
    override fun close() { dataMatrix.finish(); scope.cancel() }
    fun isBusy() = launching.get() || aborting.get()

    init {
        dataMatrix.createBoolAdapter(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL, {
            safeToStart.value = it
        }, true)
        dataMatrix.createBoolAdapter(UiServiceKeys.ACTION_START_WORKOUT_UI_READY_BOOL, {
            uiReady.value = it
        }, false)
    }

    /** If a launch is in progress, aborts it. [abortAction] is code that is run during abort process. Return true if abort process started. */
    @JvmOverloads fun abortLaunch(abortAction: () -> Unit = {}) : Boolean {
        synchronized(callLock) {
            if (!launching.get()) return false // make sure there is a launch to abort
            if (aborting.getAndSet(true)) return false // if already aborting, ignore
            scope.launch {
                try {
                    launchJob?.cancelAndJoin()
                    delay(100) // some of these processes do not stop on a dime
                    abortAction()
                } finally {
                    launching.set(false) // should not be needed, but let's make sure after abort that a launch is always possible
                    aborting.set(false)
                }
            }
            return true
        }
    }

    /** Called by service to perform a workout launch */
    fun launchWorkout(setupFunction: ()-> Unit, appId: String, startCountdown : Boolean, startFunction: ()-> Unit) {
        synchronized(callLock) {
            if (aborting.get()) return // if aborting, do not attempt another launch
            if (launching.getAndSet(true)) return // if already launching, ignore
            DataMatrix.putBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL, true)
            launchJob = scope.launch {
                    try {
                        setupFunction()
                        if (launchUi(appId, startCountdown)) {
                            startFunction() // after this call, service is either in running state or bailed to splash
                        }
                        delay(100) // original code had a pad before resetting tracking flag?
                    } finally {
                        launching.set(false) // reset if complete or cancelled
                        launchJob = null
                        DataMatrix.putBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL, false)
                    }
                }
        }
    }

    /** Called by service to switch a workout during a workout */
    fun launchPanel(cleanupFunction: ()-> Unit, setupFunction: ()-> Unit, panelId: String, startFunction: ()-> Unit) {
        synchronized(callLock) {
            if (aborting.get()) return // if aborting, do not attempt another launch
            if (launching.getAndSet(true)) return // if already launching, ignore
            DataMatrix.putBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL, true)
            launchJob = scope.launch {
                    try {
                        cleanupFunction()
                        if (!safeToStart.value) {
                            safeToStart.first { it } // suspends until true
                        }
                        setupFunction()
                        launchPanel(panelId)
                        startFunction() // after this call, service is either in running state or bailed to splash
                        delay(100) // original code had a pad before resetting tracking flag?
                    } finally {
                        launching.set(false) // reset if complete or cancelled
                        launchJob = null
                        DataMatrix.putBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL, false)
                    }
                }
        }
    }

    private suspend fun launchUi(appId: String, useCountdown: Boolean): Boolean {
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT.name) // TODO need to sort these out
        // the thought is that if a stray ui ready event occurs during this, the launch is probably aborting?
        // could use random UUID transaction ID, but emitter would need to know it?
        uiReady.value = null
        // there is no SSO sync check here, that should have been handled by UI doing the workout launch before hand, we assume user pressed forward anyway
        val data = LaunchAppData(appId, LaunchAppCondition.StartingWorkout, useCountdown)
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_APP_PARCELABLE, data)
        return waitForUIReady()
    }

    private suspend fun launchPanel(panelId: String): Boolean {
        uiReady.value = null
        DataMatrix.putString(UiServiceKeys.ACTION_LAUNCH_UI_PANEL_STRING, panelId)
        return waitForUIReady()
    }

    private suspend fun waitForUIReady(): Boolean{
        if (uiReady.value == null) {
            uiReady.first { it != null } // suspends until not null
        }
        return (uiReady.value == true)
    }
}