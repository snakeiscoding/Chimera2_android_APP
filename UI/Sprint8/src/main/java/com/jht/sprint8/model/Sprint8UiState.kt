package com.jht.sprint8.model

import androidx.compose.runtime.Immutable
import com.jht.enums.WorkoutStage
import com.jht.data.sprint8.Sprint8SweatScores
import com.jht.sprint8.sprint8classic.components.SprintStateType
import com.jht.sprint8.sprint8classic.components.TimerStyle
import com.jht.sprint8.sprint8immersive.components.LevelDirection

/**
 * Represents the UI state of the Sprint8 workout screen.
 *
 * @property heartRate The current heart rate of the user in beats per minute (BPM). Defaults to 0.
 * @property rpm The current rotations per minute (RPM) of the user. Defaults to 0.
 * @property remainingTime Remaining workout time in milliseconds. Defaults to 180,000 ms.
 * @property totalTime Total workout time in milliseconds. Defaults to 180,000 ms.
 * @property isRunning Indicates whether the workout timer is currently running. Defaults to false.
 * @property isSprinting Indicates whether the user is currently in a sprint phase. Defaults to false.
 * @property timerStyle The style of the timer displayed, represented by [TimerStyle]. Defaults to [TimerStyle.Warmup].
 * @property progressStyle The style of the sprint progress indicator, represented by [SprintStateType]. Defaults to [SprintStateType.WarmUp].
 * @property sprint8State Represents the current internal state of Sprint8 workout. Defaults to 0.
 * @property sweatScore The current sweat score of the user. Defaults to 0.
 * @property sweatScoreParcel Detailed sweat score data encapsulated in [Sprint8SweatScores]. Defaults to a new instance.
 * @property isRamping Indicates if the workout intensity is currently ramping up. Defaults to false.
 * @property isFullScreen Whether the Sprint8 UI is displayed in full-screen mode. Defaults to false.
 * @property intervalRemaining Remaining time in the current interval or sprint segment, in seconds. Defaults to 0.
 * @property sprintCount Current sprint count, starting from -1 before the first sprint. Defaults to -1.
 * @property workoutStage Current stage of the workout, represented by [WorkoutStage]. Defaults to [WorkoutStage.WARM_UP].
 * @property numberOfSprints Total number of sprints in the workout. Defaults to 8.
 * @property hasAssessmentBeenAnswered Indicates whether the pre-workout assessment has been answered. Defaults to false.
 * @property showAssessmentFollowUp Indicates whether the follow-up UI for assessment should be displayed. Defaults to false.
 * @property workoutLevel The numeric workout level for the current session. Defaults to 0.
 * @property levelDirection Indicates whether the user should increase, decrease, or stay at the current level, represented by [LevelDirection]. Defaults to [LevelDirection.STAY_HERE].
 */
@Immutable
data class Sprint8UiState(
    val heartRate: Int = 0,
    val rpm: Int = 0,
    val remainingTime: Float = 180_000f,
    val totalTime: Float = 180_000f,
    val isRunning: Boolean = false,
    val isSprinting: Boolean = false,
    val timerStyle: TimerStyle = TimerStyle.Warmup,
    val progressStyle: SprintStateType = SprintStateType.WarmUp,
    val sprint8State: Int = 0,
    val sweatScore: Int = 0,
    val sweatScoreParcel: Sprint8SweatScores = Sprint8SweatScores(),
    val isRamping: Boolean = false,
    val isFullScreen: Boolean = false,
    val intervalRemaining: Int = 0,
    val sprintCount: Int = -1,
    val workoutStage: WorkoutStage = WorkoutStage.WARM_UP,
    val numberOfSprints: Int = 8,
    val hasAssessmentBeenAnswered: Boolean = false,
    val showAssessmentFollowUp: Boolean = false,
    val workoutLevel : Int = 0,
    val levelDirection: LevelDirection = LevelDirection.STAY_HERE
)

