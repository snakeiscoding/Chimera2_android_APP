package com.jht.mastboard.data

import android.util.Log
import com.jht.concierge.service.ConciergeEvent
import com.jht.enums.WorkoutRunState
import com.jht.interfaces.models.WorkoutModel
import com.jht.mastboard.interfaces.MastboardModel

private const val TAG = "LEDPriority"

/**
 * 3 main sources of information for changing color: Concierge, Workout State, and UI State.
 * Priority order: E-Stop, Concierge, User Override, Workout State, UI State
 */
class LedPriorityManager(
    private val mastboardModel: MastboardModel,
    private val workoutModel: WorkoutModel,
) : AutoCloseable {
    init {
        // LED color goes to both mastboard and IO, so yes this posts an event to mastboard instead of direct calling to avoid doing it twice
        mastboardModel.conciergeEvent.listen { conciergeEvent->
            mastboardModel.setLedColor(handleConciergeLedOverride(conciergeEvent))
        }
        mastboardModel.uiEvent.listenAndTrigger { event ->
            mastboardModel.setLedColor(handleUiLedUpdate(event))
        }
        workoutModel.workoutRunState.listenAndTrigger {
            mastboardModel.setLedColor(handleWorkoutLedOverride())
        }
    }

    override fun close() {
        // since we did not create these, we should not clean them up?
        //mastboardModel.close()
        //workoutModel.close()
    }

    private var nonOverridenLEDItem = LEDItem(mastboardModel.ledDefaultColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
    private var workoutLEDItem = LEDItem(mastboardModel.ledWorkoutRunningColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
    private var conciergeOverrideActive: Boolean = false
    private var eStopOverrideActive: Boolean = false
    private var workoutOverrideActive: Boolean = false

    // only kotlin supports parcelable comparison, hence this helper file rather than dumping into MastBoardCommService
    private fun handleConciergeLedOverride(conciergeEvent: ConciergeEvent) : LEDItem {
        conciergeOverrideActive = when (conciergeEvent) {
            is ConciergeEvent.Loading -> { false }
            is ConciergeEvent.RequestClosed -> { false }
            is ConciergeEvent.RequestOpened -> { true }
            is ConciergeEvent.RequestAcknowledged -> { true }
            is ConciergeEvent.RequestCompleted -> { false }
            is ConciergeEvent.RequestCancelled -> { false }
            is ConciergeEvent.Error -> { false }
        }
        return getPrioritizedColor()
    }

    private fun handleWorkoutLedOverride() : LEDItem {
        // If the user selected NONE, force color to 0.
        if (mastboardModel.userLedStyle.value == LEDStyle.NONE.value) {
            workoutLEDItem.color = 0
        } else {
            when (workoutModel.workoutRunState.value) {
                WorkoutRunState.STOPPED -> {
                    Log.w(TAG, "updateWorkout - ignoring stop.")
                    // This will be the same as summary since we navigate to the summary immediately after stopping
                    workoutLEDItem.color = mastboardModel.ledWorkoutSummaryColor.value
                }
                WorkoutRunState.RUNNING -> {
                    Log.w(TAG, "updateWorkout - handle running.")
                    workoutLEDItem.color = mastboardModel.ledWorkoutRunningColor.value
                    // if the workout is running we should be using the override.
                    // if we check for this here, we can keep the color the same during the workout start,
                    // whether or not the countdown is enabled.
                    workoutOverrideActive = true
                }
                WorkoutRunState.PAUSED -> {
                    Log.w(TAG, "updateWorkout - handle paused/switching.")
                    workoutLEDItem.color = mastboardModel.ledWorkoutPausedColor.value
                }
            }
        }
        return getPrioritizedColor()
    }

    private fun handleUiLedUpdate(uiEvent:String) : LEDItem {
        when (UiEventKeys.getEventFromString(uiEvent)) {
            // Display blue LED's when displaying the Splash Screen. Additionally, to prevent loss
            // of state when a crash is encountered or the state machine is interrupted, every state
            // is allowed to transition to the splash screen (even in workout). This will also
            // address the issue where 3rd party apps are allowed to transition from In Workout to
            // Splash Screen (e.g. EGYM workout ending in EGYM summary not Matrix Workout Summary).
            UiEventKeys.EVENT.SPLASH_SCREEN -> {
                Log.w(TAG, "Configure LEDs for the Splash Screen.")
                nonOverridenLEDItem = LEDItem(mastboardModel.ledDefaultColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
                // we will always bail to splash when the workout is over, either through normal stoppage or e-stop
                // this ensures the workout color only overrides during a workout
                workoutOverrideActive = false // reset on splash
            }

            UiEventKeys.EVENT.SCREENSAVER_SCREEN,
            UiEventKeys.EVENT.WELCOME_SCREEN,
            UiEventKeys.EVENT.EGYM_LOADING,
            UiEventKeys.EVENT.LOGIN_REGISTER -> {
                Log.w(TAG, "UI State LED - case 1")
                nonOverridenLEDItem = LEDItem(mastboardModel.ledDefaultColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
            }

            UiEventKeys.EVENT.GO_SCREEN,
            UiEventKeys.EVENT.TRAINING_EXERCISES_SCREEN,
            UiEventKeys.EVENT.CUSTOM_WORKOUTS_SCREEN -> {
                Log.w(TAG, "UI State LED - case 2")
                // Display Yellow on the go screen
                nonOverridenLEDItem = LEDItem(0xffffff00.toInt(), LedZone.ALL_EXCEPT_PLAY_STOP)
            }

            // enabling the Workout State override is handled by other dataMatrix keys.
            // this just makes sure the non-overridden color is also the run state color so the
            // user sees that color throughout the workout starting.
            UiEventKeys.EVENT.OPEN_COUNTDOWN_POPOVER,
            UiEventKeys.EVENT.CLOSE_COUNTDOWN_POPOVER,
            UiEventKeys.EVENT.WORKOUT -> {
                Log.w(TAG, "UI State LED - case 3")
                val color = if (mastboardModel.userLedStyle.value == LEDStyle.NONE.value) {
                    0
                } else {
                    // show green during the countdown
                    mastboardModel.ledWorkoutRunningColor.value
                }
                nonOverridenLEDItem = LEDItem(color, LedZone.ALL_EXCEPT_PLAY_STOP)
            }

            UiEventKeys.EVENT.FIRST_TIME_SETUP,
            UiEventKeys.EVENT.MES -> {
                Log.w(TAG, "UI State LED - case 4")
                // Display orange for MES
                nonOverridenLEDItem = LEDItem(0xFF6C01, LedZone.ALL_EXCEPT_PLAY_STOP)
            }

            // This event overrides and is a special case
            UiEventKeys.EVENT.SHOW_ESTOP -> {
                Log.w(TAG, "UI State LED - case 5")
                eStopOverrideActive = true
            }

            // This event overrides and is a special case
            UiEventKeys.EVENT.CLOSE_ESTOP -> {
                Log.w(TAG, "UI State LED - case 6")
                eStopOverrideActive = false
            }

            // This event overrides and is a special case
            UiEventKeys.EVENT.WORKOUT_SUMMARY -> {
                Log.w(TAG, "UI State LED - case 7")
                // Display red for workout summary
                nonOverridenLEDItem = LEDItem(mastboardModel.ledWorkoutSummaryColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
                // this color and the color for WorkoutKeys.Constants.RUN_STATE_STOP are the same,
                // so overlap when switching the override off will never be noticeable
                workoutOverrideActive = false // workout is over when we hit the summary screen
            }

            // Ignore the following states that are unused by LED's.
            UiEventKeys.EVENT.OUT_OF_ORDER_SCREEN,
            UiEventKeys.EVENT.UPDATE,
            UiEventKeys.EVENT.YMCA360_LOADING,
            UiEventKeys.EVENT.IFIT_LOADING,
            UiEventKeys.EVENT.OPEN_FEATURE_WORKOUT_SETUP,
            UiEventKeys.EVENT.CLOSE_FEATURE_WORKOUT_SETUP,
            UiEventKeys.EVENT.OPEN_MATRIX_DASHBOARD,
            UiEventKeys.EVENT.CLOSE_MATRIX_DASHBOARD,
            UiEventKeys.EVENT.OPEN_CONNECT_TO_WATCH,
            UiEventKeys.EVENT.CLOSE_CONNECT_TO_WATCH -> {
                Log.d(TAG, "Ignoring unsupported state: $uiEvent")
            }
            else -> {
                Log.d(TAG, "Ignoring unsupported state: $uiEvent")
            }
        }
        return getPrioritizedColor()
    }

    private fun getPrioritizedColor() : LEDItem {
        if (eStopOverrideActive) {
            Log.d(TAG, "estop override active")
            return LEDItem(0xffff0000.toInt(), LedZone.ALL_EXCEPT_PLAY_STOP)
        }
        if (conciergeOverrideActive) {
            Log.d(TAG, "concierge override active")
            return LEDItem(mastboardModel.ledConciergeColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
        }
        if (mastboardModel.ledOverride.value) {
            Log.d(TAG, "user selected override active")
            return LEDItem(mastboardModel.ledDefaultColor.value, LedZone.ALL_EXCEPT_PLAY_STOP)
        }
        if (workoutOverrideActive) {
            Log.d(TAG, "workout override active")
            return workoutLEDItem
        }
        // if no overrides active, show latest color
        Log.d(TAG, "non override active")
        return nonOverridenLEDItem
    }

}
