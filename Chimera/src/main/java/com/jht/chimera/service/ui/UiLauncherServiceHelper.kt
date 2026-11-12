package com.jht.chimera.service.ui

import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.BuildConfig
import com.jht.chimera.drivers.audio.AudioSourceInput
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.dapi.DapiUser
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.mastboard.data.UiEventKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.chimera.lib.common.process.KillProcessHelper
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.lib.workoutapps.AppInfo
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.ui.panels.IWorkoutPanelManager
import com.jht.chimera.service.ui.panels.PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY
import com.jht.chimera.service.ui.panels.PANEL_OVERRIDES_INTENT_KEY
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.clubcom.interfaces.ClubComOptions
import com.jht.data.intent.BoolExtra
import com.jht.data.intent.DynamicExtraValue
import com.jht.data.intent.DynamicExtraResolver
import com.jht.data.intent.GoScreenIsHomeBool
import com.jht.data.intent.IntentExtra
import com.jht.data.intent.StringExtra
import com.jht.data.intent.resolve
import com.jht.data.intent.unknownSources
import com.jht.data.intent.toBundle
import com.jht.data.intent.WorkoutEndWorkoutIdInt
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import kotlinx.parcelize.Parcelize
import java.util.LinkedList
import java.util.Locale

private const val TAG = "UiLauncherServiceHelper"

/** key for intent passing of [CommonScreens] */
private const val KeyCommonScreenEnum = "com.jht.commonScreen"


/** Launch able screens which may have preset behaviors */
enum class CommonScreens {
    /** No preset launch behavior, assumes start intent was set */
    Unknown,
    /** Welcome screen */
    Welcome,
    /** Workout selection screen (or Go Screen) */
    WorkoutSelection,
    /** Default workout screen */
    Workout,
    /** Workout summary screen (uses default activity if start intent is not already set) */
    WorkoutSummary,
    /** Post workout survey shown before the summary; start intent must already be set */
    PostWorkoutSurvey,
    /** Launches the given start intent with IDLE workout panels */
    GenericIdleScreen, // used for alternate workout end activities
    /** Out of order screen */
    OutOfOrder,
    /** Machine setup (first time setup?) */
    MachineSetup,
    /** System settings (MES?) */
    SystemSettings,
    /** Device Pairing screen */
    DevicePairing,
    /** Teamviewer which gets special panel */
    TeamViewer,
    /** Blank screen is mostly used for transitions */
    Blank, // the only use outside of this file seems to be Workout service during an error?
}

/** Data to launch an internal screen */
@Parcelize
data class LaunchScreenData @JvmOverloads constructor(
    /** if this is a common screen with preset behavior, specify it */
    val screen: CommonScreens = CommonScreens.Unknown,
    /** if you need to add extras to the launch intent, you may provide it here (mostly for unknown/custom screens without special needs) */
    val intent: Intent = Intent(),
    /** if you need to clear the activity stack for this screen to be new root screen for back flow (may cause UI to flash if already up) */
    val isNewRoot: Boolean = true, // basically should we clear the stack
) : Parcelable {
    /** configures intent with start flags, unless already set */
    fun setStartIntent(context: Context, fullActivityName: String) {
        if (isStartIntentSet()) return
        // check is new to determine if we can re-use the old or create new ???? see FLAG_ACTIVITY_SINGLE_TOP
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isNewRoot) {
            // Intent.FLAG_ACTIVITY_CLEAR_TASK seems to make the activity refresh the page all the time, so if activity is meant to launch without flash if already up do not clear
            // adding FLAG_ACTIVITY_SINGLE_TOP didn't seem to help in the case? maybe if used it instead of clear or new? maybe there are other flag combos we could explore
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) // basically makes this activity root of the stack
        }
        intent.component = ComponentName(context.packageName, fullActivityName)
    }
    /** Returns true if start intent is already setup */
    fun isStartIntentSet() = (intent.component != null)
    /** Returns true if start intent is already setup and not empty */
    fun isActivitySet() = (intent.component?.className?.isNotEmpty() == true)
    /** Sets the "mode" intent extra (used for things like MES) */
    fun setMode(mode: String) {
        intent.putExtra("mode", mode)
    }
}

/** Indicates any special circumstances for the launching of an app */
enum class LaunchAppCondition {
    /** Launch is done before starting a workout. App ID should be stored as workout app ID. Like app is an interface that will start workouts later. */
    PreWorkout,
    /** Launching as part of the start/switch workout process */
    StartingWorkout,
    /** App is being launched without any extra consideration */
    None,
}

/** Data to launch an app (whether internal or external) */
@Parcelize
data class LaunchAppData @JvmOverloads constructor(
    val appId: String,
    val condition: LaunchAppCondition = LaunchAppCondition.None,
    val useCountdown: Boolean = true,
) : Parcelable


/** Checks if give app has pending SSO data. This is a UI responsibility as a pre-launch check! */
fun appWaitingOnSso(appInfo: AppInfo?): Boolean {
    if (!DapiUser.isGuestUser() && appInfo != null && appInfo.supportsSSO && BuildConfig.SUPPORTS_SSO) {
        val downloadedAppPackageList = DataMatrix.getStringList(SSOKeys.DATA_SSO_DOWNLOADED_APP_PACKAGE_STRING_LIST)
        val launchedAppPackageList = DataMatrix.getStringList(MainAppKeys.DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST)
        // if we data hasn't been synced and we haven't already launch app (basically only ask once)
        if (!downloadedAppPackageList.contains(appInfo.appPackage) && !launchedAppPackageList.contains(appInfo.appPackage)
        ) {
            return true
        }
    }
    return false
}
fun appWaitingOnSso(appId: String): Boolean {
    val appInfo = AppManager.getInstance().getApp(appId)
    return appWaitingOnSso(appInfo)
}


/** Helper for UI Service to handle UI launch requests
 * [reset] - reset the launcher helper
 * [launch] (x2) - launch an internal screen or application, see parameters for which method is which
 * [onUiResumed] - switch case to run specific screen/application logic when it is launched/resumed
 * [onUiPaused] - switch case to run specific screen/application logic when it is paused
 */
class UiLauncherServiceHelper(
    val context: Context,
    val panelManager: IWorkoutPanelManager,
) {
    private val mHandler = Handler(Looper.getMainLooper())
    private val launchedAppPackageList: MutableSet<String> = HashSet() // this is for SSO, should it be in SSO service?
    private val recentApps: MutableList<String> = mutableListOf() // this is for the background app limiting code
    private var resumeWorkoutScreenTimestamp : Long = 0L // used for debounce mechanism for cache behavior fix
    private var pauseWorkoutScreenTimestamp : Long = 0L // used for debounce mechanism for cache behavior fix
    // splash and app launches bypass screen launches, so those clear it
    private var lastLaunchedScreen: CommonScreens? = null

    private fun setWorkoutPanels(fullscreen: Boolean) {
        panelManager.setWorkoutPanelState(if (fullscreen) UiServiceKeys.WorkoutPanelState.FULLSCREEN else UiServiceKeys.WorkoutPanelState.REGULAR)
    }
    fun reset() {
        lastLaunchedScreen = null
        recentApps.clear()
        launchedAppPackageList.clear() // DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST is cleared on splash (but should it?)
        resetWorkoutPanels()
        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, false)
        emitUiReady(false) // if resetting, signal to anything waiting on UI (in case UI service restarted in the middle of something)
    }

    private fun resetWorkoutPanels() {
        panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.IDLE)
        panelManager.setWorkoutPanelBehavior(null)
        panelManager.updateCachedAppWorkoutPanelBehavior(null)
    }

    private fun blockDuringWorkout(screen: CommonScreens): Boolean {
        val isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP
        val isLaunchingWorkout = DataMatrix.getBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL)
        if (isInWorkout || isLaunchingWorkout) {
            Log.w(TAG, "Blocking launch of $screen during workout")
            return true
        }
        return false
    }
    private fun blockOutsideWorkout(screen: CommonScreens): Boolean {
        val isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP
        val isLaunchingWorkout = DataMatrix.getBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL)
        if (!isInWorkout && !isLaunchingWorkout) {
            Log.w(TAG, "Blocking launch of $screen outside workout")
            return true
        }
        return false
    }

    private fun applySummaryLaunch(data: LaunchScreenData) {
        val surveyShown = data.intent.getBooleanExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, false)
        if (SummaryScreenPolicy.showsLaunchOverlay(data.screen, surveyShown)) panelManager.showOverlay(2200)
        panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY)
    }

    /** Internal screen launch */
    fun launch(data: LaunchScreenData) {
        lastLaunchedScreen = data.screen
        data.intent.setExtrasClassLoader(context.classLoader)
        // include common screen so we can catch it later
        data.intent.putExtra(KeyCommonScreenEnum, data.screen)
        // could we even pass the theme URI as intent string instead of needing to use interface methods? ThemedActivity could auto apply? would need to be in both screen and app launch intent?
        val uiConfig = UiConfiguration.getCurrent()
        when (data.screen) {
            CommonScreens.Blank-> {
                data.setStartIntent(context, uiConfig.activityBlank)
            }
            CommonScreens.Welcome-> {
                if (blockDuringWorkout(data.screen)) return
                // LaunchWelcomeScreenOptions should be applied to intent beforehand, if used
                resetWorkoutPanels()
                data.setStartIntent(context, uiConfig.activityWelcome)
            }
            CommonScreens.OutOfOrder-> {
                resetWorkoutPanels()
                data.setStartIntent(context, uiConfig.activityOutOfOrder)
            }
            CommonScreens.WorkoutSelection-> {
                if (blockDuringWorkout(data.screen)) return
                val isHomeScreen =
                    (!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)
                            && !DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL))
                data.intent.putExtra(GoScreenIsHomeBool, isHomeScreen) // this way UI does not need to know the conditions leading to the flow, only what flow is
                resetWorkoutPanels()
                data.setStartIntent(context, uiConfig.activityGoScreen)
            }
            CommonScreens.Workout-> {
                if (blockOutsideWorkout(data.screen)) return
                panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.REGULAR)
                panelManager.setWorkoutPanelBehavior(null)
                data.setStartIntent(context, uiConfig.activityWorkout)
            }
            CommonScreens.MachineSetup-> {
                resetWorkoutPanels()
                data.setStartIntent(context, uiConfig.activityMachineSetup)
            }
            CommonScreens.SystemSettings-> {
                resetWorkoutPanels()
                DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, true)
                data.setStartIntent(context, uiConfig.activitySystemSettings)
            }
            CommonScreens.DevicePairing-> {
                // do the dialog here or keep higher up? the dialog is not really launching ui per se?
                data.setStartIntent(context, uiConfig.activityDevicePairing)
                data.intent.putExtra(PANEL_OVERRIDES_INTENT_KEY, Bundle().apply {
                    putBoolean(PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY, true)
                })
            }
            CommonScreens.WorkoutSummary-> {
                applySummaryLaunch(data)
                // in case the UI is using a single activity instead of separate for summary, pass intent so it can navigate internally
                val workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
                data.intent.putExtra(WorkoutEndWorkoutIdInt, workoutId) // do we need to pass anything, or just need something to check for?
                data.setStartIntent(context, uiConfig.activityWorkoutSummary) // will be ignored if already set
            }
            CommonScreens.PostWorkoutSurvey-> {
                applySummaryLaunch(data)
            }
            CommonScreens.GenericIdleScreen-> {
                // start intent is assumed to be set already
                if (data.isStartIntentSet()) {
                    panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.IDLE)
                } else {
                    Log.e(TAG, "${data.screen} passed without start intent")
                }
            }
            CommonScreens.TeamViewer-> {
                // TeamViewer more than a screen but not quite an app either? it is internal feature leveraging an external apk?
                /* CGC-20971
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(AppManager.TEAM_VIEWER_APP_NAME)
                    intent?.putExtras(data.intent) // not sure there would be any extras passed to here but feels odd to completely ignore any passed intent?
                    ConsoleEvent.LogEvent("Launching TeamViewer")
                    context.startActivity(intent)
                    panelManager.showPanel(PanelIds.TEAM_VIEWER_PANEL, true) // not really a workout panel, but could we add launch panel ids options for AppInfo?
                } catch (e: Exception) {
                    Log.e(TAG, "launchTeamviewer", e)
                }*/
                return
            }
            else -> {}
        }
        // start the intent
        if (data.isActivitySet()) {
            try {
                ConsoleEvent.logEvent("Launching Screen: ${data.intent.component?.flattenToString()}")
                context.startActivity(data.intent)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        } else {
            Log.e(TAG, "start intent not set ${data.screen}")
        }
    }

    /** Check the intent of resumed activity to see if we need to take any special action */
    fun onUiResumed(intent: Intent?) {
        try {
            intent?.setExtrasClassLoader(context.classLoader)
            //Log.d(TAG, "onUiResumed ${intent?.component?.className}")
            val screen = intent?.getSerializableExtra(KeyCommonScreenEnum) as? CommonScreens
            if (SummaryScreenPolicy.usesSummaryPanels(screen)) {
                DataMatrix.putBoolean(HALKeys.ACTION_BLOCK_SMART_WATCHES_BOOL, true) // extra insurance
                panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY) // be extra sure that panels are correct
            }
            when (screen) {
                CommonScreens.Welcome, CommonScreens.WorkoutSelection -> resetWorkoutPanels() // be extra sure that panels are reset
                CommonScreens.Workout -> {
                    resumeWorkoutScreenTimestamp = SystemClock.uptimeMillis()
                    // if you back out into Workout screen bypassing launch, the panels need resetting
                    // Should we consider checking if we are still in/launching workout and bail to splash if not as an extra safe guard against strange states?
                    /* Old UI had 100 ms delay on this due to the below quote, but is this still needed with the new panel handling?
                  "showing the UI panels/hiding the app resize bar while starting the activity can cause the activity screen to not be sized correctly
                   (it won't account for the app resize bar being hidden) this slight delay fixes that issue" */
                    panelManager.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.REGULAR)
                    panelManager.setWorkoutPanelBehavior(null)
                }
                CommonScreens.MachineSetup -> {
                    DataMatrix.putBoolean(HALKeys.ACTION_BLOCK_SMART_WATCHES_BOOL, true)
                }
                else -> {}
            }
            intent?.getBundleExtra(PANEL_OVERRIDES_INTENT_KEY)?.let { panelManager.setPanelOverrides(it) }
            // we could create intent keys for workout panel state and behavior then catch them here, if we need some more manual control? but maybe there are better ways that keep it generic
        } catch (e: Exception) {
            Log.e(TAG, "Error in onUiResumed", e)
        }
    }
    /** Check the intent of paused activity to see if we need to take any special action */
    fun onUiPaused(intent: Intent?) {
        try {
            intent?.setExtrasClassLoader(context.classLoader)

            val screen = intent?.getSerializableExtra(KeyCommonScreenEnum) as? CommonScreens
            if (SummaryScreenPolicy.runsSummaryExitEffects(screen = screen, nextScreen = lastLaunchedScreen)) {
                // some more old UI code
                // check if the user has selected to install the updater after workout
                if (DataMatrix.getBoolean(MainAppKeys.DATA_INSTALL_AFTER_WORKOUT_BOOL)) {
                    DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, true)
                }
                DataMatrix.putBoolean(HALKeys.ACTION_BLOCK_SMART_WATCHES_BOOL, false) // extra insurance
            }
            when (screen) {
                CommonScreens.Workout -> {
                    pauseWorkoutScreenTimestamp = SystemClock.uptimeMillis()
                    DataMatrix.putBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL, true) // TODO - revisit these keys?

                    // debounce the cache panel behavior fix to only run if the workout screen was paused and did not resume
                    mHandler.postDelayed({
                        if (pauseWorkoutScreenTimestamp - resumeWorkoutScreenTimestamp > 0) {
                            // Sometimes third party apps can stop and restart themselves, this lets us re-enable the correct panel state
                            panelManager.useCachedAppWorkoutPanelBehavior()
                        }
                    }, 500)
                }
                CommonScreens.MachineSetup -> {
                    DataMatrix.putBoolean(HALKeys.ACTION_BLOCK_SMART_WATCHES_BOOL, false)
                }
                else -> {}
            }
            intent?.getBundleExtra(PANEL_OVERRIDES_INTENT_KEY)?.let { panelManager.clearPanelOverrides(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onUiPaused", e)
        }
    }

    private fun emitUiReady(success: Boolean) {
        // if countdown is used, it will emit ready true
        // if no countdown, this will need to be emitted to unlock start workout
        // if UI cannot be launched this will need to emit false
        DataMatrix.putBoolean(UiServiceKeys.ACTION_START_WORKOUT_UI_READY_BOOL, success)
    }

    /** App launch (internal or external apps) */
    fun launch(data: LaunchAppData) {
        lastLaunchedScreen = null
        val delayMillis = 0L
        var currentAppId = data.appId

        // Intercepts the default app launch routing. If the current FrameConfig specifies a defaultAppOverrideId, we rewrite currentAppId with the override target.
        if (AppIds.DEFAULT_APP_UUID == currentAppId) {
            val overrideAppId = FrameConfigManager.getInstance().frameConfig.defaultAppOverrideId
            if (overrideAppId != null) {
                currentAppId = overrideAppId
                Log.d(TAG, "Intercepted Default App Launch: Routing override to $overrideAppId")
            }
        }

        val appInfo = AppManager.getInstance().getApp(currentAppId)
        val useDefaultWorkoutApp = (AppIds.DEFAULT_APP_UUID == currentAppId)
        val skipApp = (AppIds.SKIP_APP_UUID == currentAppId)
        if (appInfo == null && !useDefaultWorkoutApp && !skipApp) {
            Log.e(TAG, "could not find app for $currentAppId")
            emitUiReady(false)
            return // stop the launch process (switch can get away with it since it is in a workout UI already)
        }
        if (data.condition == LaunchAppCondition.PreWorkout) {
            // should we set workout ID to something first, even though workout is not starting?
            // normally sent by workout service when starting a workout
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, currentAppId)
        } else if (data.condition == LaunchAppCondition.StartingWorkout) {
            // check to make sure the workout start was not cancelled after the launch app request was posted to DM
            if (!DataMatrix.getBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL)) {
                Log.w(TAG, "Blocking workout app launch outside of workout $currentAppId")
                return
            }
            // doesn't use the countdown or launch any app (basically it is already up when workout starts)
            if (appInfo?.bypassWorkoutUiProcess == true) { // third party interface and EGYM check was here, now using bypassWorkoutUiProcess to trigger this flow
                setWorkoutPanels(appInfo.launchesFullscreen)
                emitUiReady(true)
                return
            }
            // blank screen
            if (!skipApp) {
                // start blank activity to speed up loading of the workout panels
                launchBlankScreen()
            }
            // countdown
            if (data.useCountdown) {
                panelManager.showPanel(PanelIds.COUNTDOWN_PANEL, true)
            }
            else {
                emitUiReady(true)
            }
            // club com check
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)) {
                val clubComOptions = DataMatrix.getParcelable(ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE, ClubComOptions::class.java)
                val preRollUrl = clubComOptions?.preRollUrls?.firstOrNull()?.url ?: ""
                val workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
                if (preRollUrl.isNotEmpty() && !WorkoutIds.isClubComSkip(workoutId)) {
                    launchClubCom(context, preRollUrl, false, 100)
                    return
                }
            }
            // Make sure VA selection is set for the app being launched as part of workout start
            setVaSelection(appInfo?.uuid)
            // launch app or workout activity (IS HANDLER POST NECESSARY??)
            if (skipApp) {
                ConsoleEvent.logEvent("Launching App: SKIPPED")
            } else {
                if (appInfo != null && !useDefaultWorkoutApp) {
                    setWorkoutPanels(appInfo.launchesFullscreen)
                    mHandler.postDelayed({
                        launchApp(appInfo, true)
                    }, delayMillis)
                } else {
                    mHandler.postDelayed({
                        launch(LaunchScreenData(CommonScreens.Workout, isNewRoot = true)) // if no app to launch, launch workout activity as new root activity
                    }, delayMillis)
                }
            }
            return
        }
        // If you're in a workout and you need fullscreen toggle fullscreen workout panels.
        else if (appInfo?.launchesFullscreen == true && DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
            setWorkoutPanels(fullscreen = true)
        }
        // regular launch
        appInfo?.let { launchApp(it, false) }
    }

    fun launchPanel(panelId: String){
        panelManager.showPanel(PanelIds.findById(panelId), true)
    }


    private fun setVaSelection(appId: String? = null) {
        // VA must apply workout selection only when workout is initially launched and NOT when re-entered via icon!
        // If not running VA, set it to random so "change workout" works correctly!
        if (AppIds.VA_UUID == appId) {
            DataMatrix.putString(VaKeys.ACTION_SELECT_WORKOUT_UNIQUEID_STRING, DataMatrix.getString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, ""))
        } else {
            DataMatrix.putString(VaKeys.ACTION_SELECT_WORKOUT_UNIQUEID_STRING, "")
        }
    }
    private fun launchClubCom(context: Context, preRollUrl: String, fullscreen: Boolean, delay: Int) {
        // (Wondering if this should be setting a random VA course instead, but deferring to the original code)
        // Transmit the last selected workout id to allow Virtual Active to prepare the Service prior to the workout starting.
        // This ensures a smooth user experience
        setVaSelection(AppIds.VA_UUID)
        // set initial panel state and post the start
        setWorkoutPanels(fullscreen)
        mHandler.postDelayed({
            try {
                val activity = "com.jht.chimera.ui.mainapp.ClubComActivity"
                val intent = Intent()
                val extras = Bundle()
                extras.putString("url", preRollUrl) //NON-NLS
                intent.putExtra("extras", extras) //NON-NLS
                intent.component = ComponentName(context.packageName, activity)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ConsoleEvent.logEvent("Launching App: " + intent.component?.className)
            } catch (e: Exception) {
                Log.e(TAG, "launchClubCom", e)
            }
        }, delay.toLong())
    }

    private fun launchBlankScreen() {
        // should we launch blank with or without clearing out the activity task-stack?
        launch(LaunchScreenData(CommonScreens.Blank, isNewRoot = false))
    }

    private fun launchApp(appInfo: AppInfo, workoutStarting: Boolean) {
        try {
            ConsoleEvent.logEvent("Launching App: " + appInfo.uuid + " " + appInfo.appPackage + " " + appInfo.appActivity)
            //Stores app open button press events for Dapi Analytics
            DataMatrix.putInt(DAPIKeys.EVENT_DAPI_ANALYTICS_APP_INT, appInfo.getAppAnalyticsId())

            // some app might required a delayed kill like iFit so make sure the app is killed before launching for the first time
            if (appInfo.requiresPrelaunchKill) {
                val killProcessHelper = KillProcessHelper()
                if (DapiUser.isGuestUser()) {
                    killProcessHelper.addPackagesToClear(appInfo.getAppPackage(), false)
                    // should probably re-set permissions after this clear, however chimera put a general permission set in launchAppByIcon so it would be redundant
                } else {
                    killProcessHelper.addPackagesToStop(appInfo.getAppPackage(), false)
                }
                killProcessHelper.kill()
            }

            // Make sure the app is enabled
            AppManager.enableApp(appInfo)

            // Make sure it has the correct permissions.
            AppManager.grantPermissions(appInfo)

            val intent: Intent
            if (appInfo.getAppActivity().isNotEmpty()) { // internal
                intent = Intent()
                intent.component = ComponentName(appInfo.getAppPackage(), appInfo.getAppActivity())
                if (appInfo.url != null) {
                    val extras = Bundle()
                    extras.putString("url", appInfo.url) //NON-NLS
                    extras.putString("useragent", appInfo.userAgent) //NON-NLS
                    extras.putString("webAppDomainBan", appInfo.webAppDomainBan) //NON-NLS
                    extras.putString("webAppDomainInclude", appInfo.webAppDomainInclude) //NON-NLS
                    extras.putBoolean("clearCachePeriodically", appInfo.clearWebViewCachePeriodically)
                    intent.putExtra("extras", extras) //NON-NLS
                }
            } else { // external
                val packageIntent = context.packageManager.getLaunchIntentForPackage(appInfo.getAppPackage())
                if (packageIntent == null) {
                    Log.e(TAG, "Failed to create a start intent, bailing! " + appInfo.getAppPackage())
                    return
                }
                intent = packageIntent
            }
            // extras for app launch intent - declared on the AppInfo, dynamic entries filled in now
            appInfo.startExtras?.let { extras ->
                val unknownItems = extras.unknownSources()
                if (unknownItems.isNotEmpty()) Log.w(TAG, "app ${appInfo.uuid} declares unknown dynamic sources: $unknownItems")
                intent.putExtras(extras.resolve(LaunchDynamicExtraResolver).toBundle())
            }

            // pass branding - should intent extra or JHT owned be part of app meta data?
            if (appInfo.appPackage.startsWith("com.jht.")) {
                intent.putExtra("com.jht.brandingName", ProductConfig.getBrandingName())
            }

            // was originally marked "if (!ProductFlavorEnum.usingParker())" seems to cause MyMedia to stop playing when starting other apps on parker, but no other side effects?
            if (appInfo.audioInput != AudioSourceInput.Default) {
                if (!(ProductConfig.hardware == ProductConfig.ConsoleHardware.Parker_3 && appInfo.audioInput == AudioSourceInput.HDMI)) {
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, appInfo.audioInput.ordinal)
                }
            }

            // Handle LCD toggle event for specific app launch, but do not fight the toggles for in-workout when starting actual workout
            if (appInfo.uiEventName?.isNotEmpty() == true && !workoutStarting) {
                DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, appInfo.uiEventName)
            }

            if (AppIds.IFIT_UUID == appInfo.uuid) {
                // show the blank screen in case the app finishes during the workout to prevent showing the home screen
                // also if third party interface crashes, the blank timeout might allow us to bail in orderly fashion
                launchBlankScreen()
                SystemClock.sleep(100)
            }

            if (!DapiUser.isGuestUser() && appInfo.supportsSSO) {
                // TODO - should we just emit an ACTION and leave this list to SSO service?
                launchedAppPackageList.add(appInfo.appPackage)
                DataMatrix.putStringList(MainAppKeys.DATA_LAUNCHED_SSO_APP_PACKAGE_STRING_LIST, LinkedList(launchedAppPackageList))
            }

            // We can only keep a few background processes otherwise we will run out of memory.
            // Add the new app to the recents list and kill apps if the list is >= the threshold set in Engineering.
            // Note if it is a third party app we use am force-stop to kill the package.  If this is
            // one of ours we need to find the process attached to it.  Most of our apps are on the com.jht.mainapp
            // process, but not all and that may change.
            Thread(Runnable {
                var appProcess = "package:" + appInfo.appPackage //NON-NLS
                if (appInfo.appPackage == context.packageName) {
                    // This is one of ours. Find the process name.
                    try {
                        val info = context.applicationContext.packageManager.getPackageInfo(appInfo.appPackage, PackageManager.GET_ACTIVITIES)
                        val activities = info.activities
                        if (activities != null) {
                            for (activityInfo in activities) {
                                if (appInfo.appActivity == activityInfo.name) {
                                    appProcess = "process:" + activityInfo.processName //NON-NLS
                                    // Ignore main app and persistent processes.
                                    if (activityInfo.processName == "com.jht.mainapp" || activityInfo.processName.startsWith("com.jhtp.")) {
                                        // Let's try ignoring this and leaving our apps up all the time.
                                        return@Runnable
                                    }
                                    break
                                }
                            }
                        }
                    } catch (ignored: PackageManager.NameNotFoundException) {
                    }
                }

                // Update the list.
                if (!recentApps.contains(appProcess)) {
                    recentApps.add(appProcess)
                    val recentWorkoutLimit = DataMatrix.getInt(SettingKeys.Experimental.DATA_RECENT_WORKOUT_APP_LIMIT_2_INT)
                    if (recentWorkoutLimit > 0 && recentApps.size >= recentWorkoutLimit) {
                        // Too many apps open.  Kill kill kill.
                        val appToRemove = recentApps.removeAt(0)

                        val overrideId = DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING)
                        val overrideAppInfo = AppManager.getInstance().getApp(overrideId)

                        val excludedPackages = listOfNotNull(
                            "com.jht.egym".takeIf { AppIds.EGYM_UUID == DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING) },
                            overrideAppInfo?.appPackage
                        )
                        killRecentApp(appToRemove, excludedPackages)
                    } else {
                        // The opened app is already in the list.  Let's move it to the back to sort
                        // the list.
                        recentApps.remove(appProcess)
                        recentApps.add(appProcess)
                    }
                }
            }, "BackAppPurge").start()

            //if (v != null) intent.sourceBounds = v.getClipBounds()
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            panelManager.setWorkoutPanelBehavior(appInfo.specialPanelBehavior)
            panelManager.updateCachedAppWorkoutPanelBehavior(appInfo.specialPanelBehavior) // Handles edge case where app restarts itself, which would mess up which panels behavior is active
            DataMatrix.putBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL, true)
            if (appInfo.category != AppInfo.Category.Entertainment) { // workout and integrations are probably safe to set this
                if (FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
                    // This should go away, but for now iFit doesn't recognize spin bikes.
                    intent.putExtra("equipment_type", JHTCommonTypes.MachineClassificationType.Bike.toString().lowercase(Locale.getDefault()))
                } else {
                    intent.putExtra("equipment_type", FrameConfigManager.getInstance().frameConfig.machineClassType.toString().lowercase(Locale.getDefault()))
                }
            }

            // For Kindle we need to turn on continuous scrolling for it to display correctly on Chimera.  There
            // is a script in the assets that takes care of turning on the settings.  Reference the script for more
            // information.
            if (ProductConfig.hardware == ProductConfig.ConsoleHardware.Chimera || ProductConfig.hardware == ProductConfig.ConsoleHardware.Chimera_2 && appInfo.appPackage.equals("com.amazon.kindle", ignoreCase = true)) {
                ExecuteAsRoot.run("sh /sdcard/jht/kindle/enableContinuousScroll.sh")
                ExecuteAsRoot.fixFilesInPackage("com.amazon.kindle")
            }

            // send an event with app uuid before launching
            if (appInfo.uuid != null) DataMatrix.putString(UiServiceKeys.EVENT_LAUNCHING_APP_UUID_STRING, appInfo.uuid)

            // We need to add the workout activity to the backstack on initial launch for normal apps so the
            // back button or a crash take you back to the workout screen instead of the blank activity
            if (workoutStarting
                && !appInfo.useThirdPartyInterface
                && !appInfo.bypassWorkoutUiProcess
            ) {
                val uiConfig = UiConfiguration.getCurrent()
                val workoutScreenIntent = Intent()
                workoutScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                workoutScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                workoutScreenIntent.putExtra(KeyCommonScreenEnum, CommonScreens.Workout)
                workoutScreenIntent.component = ComponentName(context.packageName, uiConfig.activityWorkout)

                val stackBuilder = TaskStackBuilder.create(context)
                stackBuilder.addNextIntent(workoutScreenIntent)
                stackBuilder.addNextIntent(intent)
                stackBuilder.startActivities()
            }
            else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch " + appInfo.appPackage, e)
        }
    }

    private fun killRecentApp(recentApp: String, excludedPackages: List<String> = listOf()) {
        // split the app string which is in the format 'package:com.abc' or 'process:com.abc'
        val appToRemove = recentApp.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        if (appToRemove.size < 2 || excludedPackages.contains(appToRemove[1])) return
        // kill the app using the appropriate command (force stop packages or kill process names)
        val executeAsRoot = ExecuteAsRoot()
        if (appToRemove[0] == "package") {
            executeAsRoot.addCommand("am force-stop ${appToRemove[1]}") //NON-NLS
        } else {
            executeAsRoot.addCommand("kill -9 $(pidof ${appToRemove[1]})") //NON-NLS
        }
        Log.d(TAG, "kill command: $executeAsRoot")
        executeAsRoot.execute()
    }

} // END CLASS: UiLauncherServiceHelper


/** Provides console side values for the [DynamicExtraValue] items in start intent extras. */
private object LaunchDynamicExtraResolver : DynamicExtraResolver {
    override fun resolve(key: String, source: DynamicExtraValue): IntentExtra? = when (source) {
        DynamicExtraValue.UnitsAreImperial ->
            BoolExtra(key, !DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL))

        DynamicExtraValue.BrandingName ->
            StringExtra(key, ProductConfig.getBrandingName())

        // omitted rather than sent as false - third party apps may treat "present" as the signal
        DynamicExtraValue.CommercialFlag ->
            if (ProductConfig.isCommercial()) BoolExtra(key, true) else null
    }
}
