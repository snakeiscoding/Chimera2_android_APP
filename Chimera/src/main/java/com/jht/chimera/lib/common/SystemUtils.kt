@file:JvmName("SystemUtils")
package com.jht.chimera.lib.common

import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.usb.StorageUtils
import com.jhtp.dapiservice.DapiEnvironment
import com.jht.datamatrixlib.DataMatrix
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * Custom wrapper providing a consistent way of acquiring some information about this system. This
 * information likely belongs in a different location; but, it's being housed here for now.
 */
object SystemUtils {
    /**
     * The tag to use when logging messages in this object.
     */
    private const val TAG = "SystemUtils"

    private const val LCCG_FLAVOR = "TouchBase"

    /**
     * Return the given calendar formatted for usage in file and directory names.
     *
     * @param now The given calendar instance.
     * @return The formatted string.
     */
    @JvmStatic
    fun calendarToFilename(now: Calendar): String = String.format(
        Locale.ENGLISH,
        "%d-%02d-%02d_%02d%02d%02d",
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH) + 1,
        now.get(Calendar.DAY_OF_MONTH),
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        now.get(Calendar.SECOND)
    )

    /**
     * Return the path to the log directory created on the given USB.
     *
     * @param now The current time to use when creating the required directory.
     * @return the output directory if existing, else null.
     */
    @JvmStatic
    fun getLogUSBPath(now: Calendar): File? {
        val result = File(
            StorageUtils.getUSBPath(),
            "${if (ProductConfig.platform == ProductConfig.Platform.LCGUI) LCCG_FLAVOR else BuildConfig.FLAVOR}/${DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING)}/${calendarToFilename(now)}"
        )

        // Ensure the path exists, create if not existing.
        // fixme: This should likely be done using ExecuteAsRoot like all consumers of this will do.
        if (!result.isDirectory && !result.mkdirs()) {
            Log.e(TAG, "Failed to create the directory for logging at \"${result.absolutePath}\"")
            return null
        }

        // todo: Include a log message for testing.
        return result
    }

    /**
     * This setting can be configured in MES from the Hardware tab. fixme: make internal
     *
     * @return whether incline errors should be ignored.
     */
    val ignoreInclineErrors: Boolean @JvmStatic @JvmName("ignoreInclineErrors") get() =
        DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IGNORE_INCLINE_ERRORS_BOOL)

    /**
     * This setting can be configured in MES from the Hardware tab. fixme: make internal
     *
     * @return whether B level errors should be ignored.
     */
    val ignoreBLevelErrors: Boolean @JvmStatic @JvmName("ignoreBLevelErrors") get() =
        DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IGNORE_B_LEVEL_ERRORS_BOOL)

    /**
     * This setting is only configurable through the console config. fixme: make internal
     *
     * @return whether all errors should be ignored.
     */
    val ignoreAllErrors: Boolean @JvmStatic @JvmName("ignoreAllErrors") get() =
        DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL)

    /**
     * Ignore all error codes while the workout state is in idle state.
     *
     * @return workout state whether all errors should be ignored.
     */
    val inIdleState: Boolean @JvmStatic @JvmName("inIdleState") get() {
        val currentState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT)
        val isIdle = currentState == WorkoutKeys.Constants.RUN_STATE_STOP
        return isIdle
    }

    /**
     * @return whether the error code mapping is needed.
     */
    val needsErrorCodeMapping: Boolean @JvmStatic @JvmName("needsErrorCodeMapping") get() {
        // EP/Bike/Ascent/Stepper old LCB requires error code mapping
        return isBike || isAscent || isElliptical || isStepper
    }

    /**
     * @return whether machine setup is complete.
     */
    val isMachineSetup: Boolean @JvmStatic @JvmName("isMachineSetup") get() =
        DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)

    /**
     * Return whether is is a debug build or not.
     */
    val isDebugBuild: Boolean @JvmStatic @JvmName("isDebugBuild") get() = BuildConfig.DEBUG

    /**
     * Return whether the production environment is currently selected.
     */
    val isProductionEnvironment: Boolean @JvmStatic @JvmName("isProductionEnvironment") get() =
        DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT) == DapiEnvironment.Production.ordinal

    /**
     * @return whether we are currently in a workout.
     */
    val isInWorkout: Boolean @JvmStatic @JvmName("isInWorkout") get() =
        DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP

    /**
     * @return whether we are currently in demo mode.
     */
    val inDemoMode: Boolean @JvmStatic @JvmName("inDemoMode") get() =
        DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)

    /**
     * @return whether instrumentation testing for the splash screen is running.
     */
    val isRunningInstrumentationSplashTesting: Boolean @JvmStatic @JvmName("isRunningInstrumentationSplashTesting") get() =
        DataMatrix.getBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_SPLASH_TEST_BOOL)

    /**
     * @return whether this is being executed on a climbmill.
     */
    val isClimbmill: Boolean @JvmStatic @JvmName("isClimbmill") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill

    /**
     * @return whether this is being executed on a treadmill.
     */
    val isTreadmill: Boolean @JvmStatic @JvmName("isTreadmill") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill

    /**
     * @return whether this is being executed on a bike.
     */
    val isBike: Boolean @JvmStatic @JvmName("isBike") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Bike

    /**
     * @return whether this is being executed on an elliptical.
     */
    val isElliptical: Boolean @JvmStatic @JvmName("isElliptical") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Elliptical

    /**
     * @return whether this is being executed on a stepper.
     */
    val isStepper: Boolean @JvmStatic @JvmName("isStepper") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper

    /**
     * @return whether this is being executed on an ascent trainer.
     */
    val isAscent: Boolean @JvmStatic @JvmName("isAscent") get() =
        FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent

    /**
     * @return whether the target market is retail.
     */
    val isRetail: Boolean @JvmStatic @JvmName("isRetail") get() =
        ProductConfig.isRetail()

    /**
     * @return Whether the current platform is Onyx.
     */
    val isOnyx: Boolean @JvmStatic @JvmName("isOnyx") get() = ProductConfig.isProjectOne()

    /**
     * This setting can be configured in frame support battery
     *
     * @return whether no battery errors should be ignored.
     */
    val ignoreNoBatteryErrors: Boolean @JvmStatic @JvmName("ignoreNoBatteryErrors") get() =
        if(isOnyx && (isAscent || isBike)) true else false

    /**
     * This setting can be configured in frame support response of motor speed sensor
     *
     * @return whether configured in frame support response of motor speed sensor should be ignored.
     */
    val ignoreNoResponseOfMotorSpeedSensorErrors: Boolean @JvmStatic @JvmName("ignoreNoResponseOfMotorSpeedSensorErrors") get() =
        if(isRetail && (isElliptical || isBike)) true else false

    /**
     * Play the system beep.
     */
    @JvmStatic
    fun playBeep() {
        DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP)
    }
}