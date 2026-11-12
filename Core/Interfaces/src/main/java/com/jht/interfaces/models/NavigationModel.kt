package com.jht.interfaces.models

import android.os.Parcelable
import com.jht.data.DeviceLoginData
import com.jht.data.LoginType
import com.jht.interfaces.BaseModel
import kotlinx.parcelize.Parcelize

/**
 * Model for navigation in the system.
 */
abstract class NavigationModel : BaseModel {
    /** Launches the main splash screen */
    abstract fun launchSplashScreen()
    /** Launches the welcome screen */
    abstract fun launchWelcomeScreen(options: LaunchWelcomeScreenOptions? = null)
    /** Launches the workout selection or "GO" screen */
    abstract fun launchGoScreen()
    /** Launches the out of order screen */
    abstract fun launchOutOfOrder()
    /** Launches the matrix engineering screen */
    abstract fun launchSystemSetup()
    /** Launches the first time setup screen */
    abstract fun launchFirstTimeSetup()
    /** Launches the device pairing screen */
    abstract fun launchDevicePairing()
    /** Launches the Android screen */
    abstract fun launchAndroidLauncher()
    /** Launches an app for which Matrix has defined settings. UI should probably check [isAppWaitingOnSso] before calling. This will launch app without any additional checking. */
    abstract fun launchApp(appUuid: String)

    /**Launches the workout screen.*/
    abstract fun launchWorkoutScreen()

    /** Check if given app is still waiting on SSO sync */
    abstract fun isAppWaitingOnSso(appId: String): Boolean

    /**
     * Launches the workout summary after the post workout survey
     *
     * The launch is marked as survey shown, so it never routes back to the survey and the summary
     * gets a fresh post workout timeout.
     *
     * @param activityName full class name of the summary to open; empty opens the default summary
     */
    abstract fun launchWorkoutSummaryAfterSurvey(activityName: String)
}

/** Intent extras shared by the post workout survey and the summaries it leads to */
object PostWorkoutSurveyExtras {
    /** String extra: summary activity the survey opens when it closes, empty for the default summary */
    const val TARGET_ACTIVITY = "com.jht.postWorkoutSurvey.targetActivity" //NON-NLS
    /** Boolean extra: the post workout survey already ran for this workout */
    const val SURVEY_SHOWN = "com.jht.postWorkoutSurvey.shown" //NON-NLS
}


/** Additional options for launching the welcome screen. */
@Parcelize
data class LaunchWelcomeScreenOptions(
    /** If welcome screen provides login selections, pre-select this login if available */
    val preselectLogin: LoginType? = null,
    /** If welcome screen provides device association, use this device data. Requires [preselectLogin] to indicate the login type for association. */
    val associateDevice: DeviceLoginData? = null,
) : Parcelable {
    companion object {
        /** Unique string key for use in passing as a key/value pair (for instance as Intent extra) */
        const val key = "LaunchWelcomeScreenOptions" //NON-NLS
    }
}
