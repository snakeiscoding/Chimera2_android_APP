package com.jht.chimera.service.ui

/**
 * Decides which summary behaviors [UiLauncherServiceHelper] applies to [CommonScreens.WorkoutSummary]
 * and [CommonScreens.PostWorkoutSurvey]
 *
 * The survey looks and times out like the summary, but the summary is still the screen the user
 * finishes on, so only leaving the summary counts as the end of the post workout flow.
 */
internal object SummaryScreenPolicy {

    /** True when the screen gets the summary panel state and smart watch block on launch and resume */
    fun usesSummaryPanels(screen: CommonScreens?): Boolean =
        screen == CommonScreens.WorkoutSummary || screen == CommonScreens.PostWorkoutSurvey

    /**
     * True when launching the screen should cover the workout-to-summary transition with the overlay
     *
     * @param surveyShown the survey already covered the transition, so the summary after it skips it
     */
    fun showsLaunchOverlay(screen: CommonScreens?, surveyShown: Boolean): Boolean = when (screen) {
        CommonScreens.PostWorkoutSurvey -> true
        CommonScreens.WorkoutSummary -> !surveyShown
        else -> false
    }

    /**
     * True when pausing the screen may mark updates safe and unblock smart watches
     *
     * The survey only skips them when the summary is taking over, since the summary runs them itself.
     *
     * @param nextScreen the screen launched most recently, null when an app launch or splash reset followed
     */
    fun runsSummaryExitEffects(screen: CommonScreens?, nextScreen: CommonScreens?): Boolean = when (screen) {
        CommonScreens.WorkoutSummary -> true
        CommonScreens.PostWorkoutSurvey -> nextScreen != CommonScreens.WorkoutSummary
        else -> false
    }

    /**
     * What launching the screen does to the post workout idle timeout started at workout stop
     *
     * @param surveyShown the summary is opening after the survey closed
     */
    fun postWorkoutTimeout(screen: CommonScreens?, surveyShown: Boolean): PostWorkoutTimeout = when {
        screen == CommonScreens.PostWorkoutSurvey -> PostWorkoutTimeout.Hold
        screen == CommonScreens.WorkoutSummary && surveyShown -> PostWorkoutTimeout.Restart
        else -> PostWorkoutTimeout.Unchanged
    }
}

/** Effect a screen launch has on [com.jht.chimera.lib.common.timer.ThirdPartyFeaturedWorkoutScreenTimeout] */
internal enum class PostWorkoutTimeout {
    /** Survey is up; stop counting so the user isn't sent to splash mid survey */
    Hold,
    /** Summary opened after the survey; start a fresh full timeout */
    Restart,
    /** Leave the timeout as it is */
    Unchanged,
}
