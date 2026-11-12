@file:JvmName("UiConfig")
package com.jht.chimera.service.ui

import android.content.Context
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.service.ui.panels.IPanel
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.compose.WorkoutComposeBottomPanel
import com.jht.chimera.service.ui.panels.compose.WorkoutComposeLeftPanel
import com.jht.chimera.service.ui.panels.compose.WorkoutComposeRightPanel
import com.jht.chimera.service.ui.panels.compose.WorkoutComposeTopPanel
import com.jht.datamatrixlib.DataMatrix

/*
    File contains attempt to centralize UI configuration differences.
    For more modularization, could perhaps split out the implementation classes?
 */

abstract class UiConfiguration {
    abstract val activityMachineSetup: String
    abstract val activitySystemSettings: String
    abstract val activitySplash: String
    abstract val activityWelcome: String
    abstract val activityGoScreen: String
    abstract val activityWorkout: String
    abstract val activityWorkoutSummary: String
    abstract val activityBlank: String
    abstract val activityOutOfOrder: String
    abstract val activityDevicePairing: String
    abstract val activitySimpleApp: String
    abstract val activityTrackApp: String
    abstract val activityCustomHIIT: String
    abstract val activityLandmarks: String
    abstract val activitySprint8: String
    abstract val activitySprint8End: String
    abstract val activityTargetTraining: String
    abstract val activitySwitchStride: String

    open fun createComposePanel(id: PanelIds, context: Context): IPanel? = null

    companion object {
        @JvmStatic fun getCurrent(): UiConfiguration {
            if (ProductConfig.hasPremiumUi()) return PremiumUi()
            if (ProductConfig.isParker()) return MatrixParker()
            return MatrixDefault()
        }
    }
}

open class MatrixDefault : UiConfiguration() {
    override val activityMachineSetup = "com.jht.chimera.ui.machinesetup.MachineSetup"
    override val activitySystemSettings = "com.jht.chimera.ui.machinesettings.EngineeringScreen"
    override val activitySplash = "com.jht.splashapp.SplashScreenActivity"
    override val activityWelcome = "com.jht.chimera.ui.mainapp.WelcomeActivity"
    override val activityGoScreen = "com.jht.chimera.ui.mainapp.GoScreenActivity"
    override val activityWorkout = "com.jht.applibrary.chameleon.AppLibraryActivity"
    override val activityWorkoutSummary = "com.jht.chimera.ui.mainapp.workoutsummary.WorkoutSummaryActivity"
    override val activityBlank = "com.jht.chimera.ui.mainapp.BlankActivity"
    override val activityOutOfOrder = "" // not used by old java UI, still needs to be overridden
    override val activityDevicePairing = "com.jht.devicepairing.DevicePairingActivity"
    override val activitySimpleApp = "com.jht.simpleapp.SimpleRunActivity2"
    override val activityTrackApp = "com.jht.trackapp.TrackActivity2"
    override val activityCustomHIIT = "com.jht.customhiit.CustomHIITActivity2"
    override val activityLandmarks = "com.jht.chimera.ui.mainapp.landmark.LandmarkWorkoutActivity"
    override val activitySprint8 = "com.jht.sprint8.Sprint8Activity"
    override val activitySprint8End = "com.jht.sprint8.sprint8workoutsummary.Sprint8WorkoutSummaryActivity"
    override val activityTargetTraining = "com.jht.spinbikeapps.targettraining.TargetTrainingActivity"
    override val activitySwitchStride = "com.jht.switchStride.SwitchStrideActivity"

    override fun createComposePanel(id: PanelIds, context: Context): IPanel? {
        return when (id) {
            PanelIds.TOP_PANEL -> WorkoutComposeTopPanel(context)
            PanelIds.BOTTOM_PANEL -> WorkoutComposeBottomPanel(context)
            PanelIds.LEFT_PANEL -> WorkoutComposeLeftPanel(context)
            PanelIds.RIGHT_PANEL -> WorkoutComposeRightPanel(context)
            else -> null
        }
    }
}

class MatrixParker : MatrixDefault() {
    override val activityWelcome = "com.jht.chimera.ui.mainapp.ParkerGoScreenActivity"
    override val activityGoScreen = "com.jht.chimera.ui.mainapp.ParkerGoScreenActivity"
}

class PremiumUi : MatrixDefault() {
    // eventually everything will use vortex UI variant, but temporarily need to have both, should only impact a couple main screens
    private val useNewSetup = ProductConfig.isCommercial()
    private val useVortexUi = DataMatrix.getInt(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT) == SettingKeys.UiSystem.Vortex.ordinal
    override val activityWelcome = if (useVortexUi) "com.jht.welcome.vortex.WelcomeActivity" else "com.jht.welcome.chameleon.WelcomeActivity"
    override val activityGoScreen = if (useVortexUi) "com.jht.workoutselection.vortex.WorkoutSelectionActivity" else "com.jht.workoutselection.chameleon.WorkoutSelectionActivity"
    override val activityWorkout = if (useVortexUi) "com.jht.applibrary.AppLibraryActivity" else "com.jht.applibrary.chameleon.AppLibraryActivity"
    override val activityMachineSetup = if(useNewSetup) "com.jht.firsttimesetup.FirstTimeSetupActivity" else "com.jht.chimera.ui.machinesetup.MachineSetup"
    override val activityWorkoutSummary = "com.jht.workoutsummary.WorkoutSummaryActivity2"
    override val activityOutOfOrder = "com.jht.outoforder.OutOfOrderActivity" // used by new UI
    override val activityLandmarks = "com.jht.landmarks.LandmarksActivity"
}
