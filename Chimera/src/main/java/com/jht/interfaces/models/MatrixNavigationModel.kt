package com.jht.interfaces.models

import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.service.ui.LaunchAppCondition
import com.jht.chimera.service.ui.LaunchAppData
import com.jht.chimera.service.ui.appWaitingOnSso
import com.jht.datamatrixlib.DataMatrix

class MatrixNavigationModel(
) : NavigationModel() {

    override fun launchSplashScreen() {
        LaunchAppUtil.launchSplashScreen()
    }

    override fun launchWelcomeScreen(options: LaunchWelcomeScreenOptions?) {
        LaunchAppUtil.launchWelcomeScreen(options)
    }

    override fun launchGoScreen() {
        LaunchAppUtil.launchGoScreen()
    }

    override fun launchOutOfOrder() {
        LaunchAppUtil.launchOutOfOrderScreen()
    }

    override fun launchFirstTimeSetup() {
        LaunchAppUtil.launchMachineSetup()
    }

    override fun launchDevicePairing() {
        LaunchAppUtil.launchBluetoothPairing()
    }

    override fun launchAndroidLauncher() {
        LaunchAppUtil.launchAndroidLauncher()
    }

    override fun launchSystemSetup() {
        LaunchAppUtil.launchSystemSetup() // should we consider adding enum parameter for the "mode" of the screen?
    }

    override fun launchApp(appUuid: String) {
        // presume any pre-flight checks like SSO have been done by UI and we are launching this thing
        val data = LaunchAppData(appUuid, LaunchAppCondition.None)
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_APP_PARCELABLE, data)
    }
    override fun isAppWaitingOnSso(appId: String): Boolean {
        return appWaitingOnSso(appId)
    }

    override fun launchWorkoutScreen(){
        LaunchAppUtil.launchWorkoutScreen()
    }

    override fun launchWorkoutSummaryAfterSurvey(activityName: String) {
        LaunchAppUtil.launchWorkoutSummaryAfterSurvey(activityName)
    }

    override fun close() {
        // do nothing
    }
}
