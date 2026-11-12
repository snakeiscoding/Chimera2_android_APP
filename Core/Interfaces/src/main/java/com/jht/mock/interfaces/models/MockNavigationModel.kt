package com.jht.mock.interfaces.models

import com.jht.interfaces.models.LaunchWelcomeScreenOptions
import com.jht.interfaces.models.NavigationModel

class MockNavigationModel() : NavigationModel() {
    /** Number of [launchSplashScreen] calls */
    var splashLaunches = 0
        private set
    /** Activity names passed to [launchWorkoutSummaryAfterSurvey], in call order */
    val summaryLaunchesAfterSurvey = mutableListOf<String>()

    override fun launchSplashScreen() {
        splashLaunches++
    }

    override fun launchWorkoutSummaryAfterSurvey(activityName: String) {
        summaryLaunchesAfterSurvey += activityName
    }

    override fun launchWelcomeScreen(options: LaunchWelcomeScreenOptions?) {
        // do nothing
    }

    override fun launchGoScreen() {
        TODO("Not yet implemented")
    }

    override fun launchOutOfOrder() {
        TODO("Not yet implemented")
    }

    override fun launchSystemSetup() {
        TODO("Not yet implemented")
    }

    override fun launchFirstTimeSetup() {
        TODO("Not yet implemented")
    }

    override fun launchAndroidLauncher() {
        TODO("Not yet implemented")
    }

    override fun launchDevicePairing() {}

    override fun launchApp(appUuid: String) {
        TODO("Not yet implemented")
    }

    override fun launchWorkoutScreen() {
        TODO("Not yet implemented")
    }

    override fun isAppWaitingOnSso(appId: String): Boolean {
        return false
    }

    override fun close() {
        // do nothing
    }
}