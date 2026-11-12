package com.jht.chimera.service.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.util.Log
import com.jht.chimera.lib.language.RestringTranslationLoader
import java.util.Locale
import com.jht.appstore.service.AppStoreConfigManager
import com.jht.chimera.lib.common.datamatrixkeys.AppStoreKeys
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.timer.ThirdPartyFeaturedWorkoutScreenTimeout
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.ui.panels.IWorkoutPanelManager
import com.jht.chimera.service.ui.panels.PanelBehavior
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.clubcom.interfaces.ClubComOptions
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.services.ChildConsoleService
import com.jht.services.ConsoleServiceState
import com.jht.util.ServiceHelper

private const val TAG = "UiService"

/**
 * The UiService controls
 * 1.) the behavior of the UI panels that are shown on the screen, [WorkoutPanelManager]
 * 2.) Application and Screen launching, [UiLauncherServiceHelper]
 * 3.) Displaying messages to user, [MessageCenterManager]
 * 4.) Android specific UI "things", [AndroidUiManager]
 * 5.) handling the workout pause logic, [PauseLogic.processWorkoutRunState]
 * 6.) re-initializing user facing apps based on system configurations, [AppManager.reset]
 * 7.) 3rd party featured workout screen timeout, [ThirdPartyFeaturedWorkoutScreenTimeout], held while the post workout survey shows
 */
class UiService : ChildConsoleService() {
    private val mDataMatrix = DataMatrix()
    private var lastLocale: Locale? = null
    private var mWorkoutPanelManager: IWorkoutPanelManager? = WorkoutPanelManager.instance
    private var thirdPartyFeaturedWorkoutScreenTimeout: ThirdPartyFeaturedWorkoutScreenTimeout? = null
    private var messageCenterManager: MessageCenterManager? = null
    private var uiLauncher: UiLauncherServiceHelper? = null
    private var androidUiManager: AndroidUiManager? = null
    // Feels like we should ensure there is only one pause timer at any time? Should this be in Workout Service instead of UI?
    // UI can simply react to run state to know its paused and listen to pause time for display
    private val pauseLogic = PauseLogic()

    override fun onCreate() {
        super.onCreate()

        lastLocale = resources.configuration.locales.takeIf { !it.isEmpty }?.get(0)
        Log.d(TAG, "UI Service onCreate")
        ServiceHelper.startForegroundService(this) // on parker, UI gets funky without UiService set as foreground (needs investigation)

        // init manager classes
        thirdPartyFeaturedWorkoutScreenTimeout = ThirdPartyFeaturedWorkoutScreenTimeout()
        messageCenterManager = MessageCenterManager(applicationContext)
        uiLauncher = mWorkoutPanelManager?.let { UiLauncherServiceHelper(applicationContext, it) }
        androidUiManager = AndroidUiManager(applicationContext)

        startUiService()

        // Signal the splash screen after the UI service has started.
        updateMyServiceState(ConsoleServiceState.READY)
        Log.d("ServiceStarter", "Ending $this")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUiService()
    }

    /**
     * The windows this service adds to the WindowManager are inflated once and survive locale
     * changes, so their texts go stale when the language is switched. Rebuild the panels when
     * the locale actually changes (this callback fires for other configuration changes too).
     * Follows the same restart pattern as the DATA_UI_SYSTEM_ORDINAL_INT adapter.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newLocale = newConfig.locales.takeIf { !it.isEmpty }?.get(0) ?: return
        if (newLocale == lastLocale) return
        lastLocale = newLocale

        Log.d(TAG, "Locale changed to '$newLocale', rebuilding UI panels")
        // The Application reloads translations synchronously in its own onConfigurationChanged,
        // which normally runs first; this is a cheap idempotent safety net in case the
        // callback order ever differs.
        RestringTranslationLoader.ensureLoaded(newLocale)
    }

    private fun startUiService() {
        stopUiService() // cover reentrancy

        // start manager classes
        androidUiManager?.start()

        // Workout Panel Manager
        mWorkoutPanelManager?.initializeWorkoutPanelManager(applicationContext)

        setupDataMatrix()
    }

    private fun stopUiService() {
        mDataMatrix.finish()
        androidUiManager?.stop()
    }

    /**
     * Not a full restart, a soft reset if you will, usually triggered by the splash screen.
     * could also be triggered by a user logging in/out?
     */
    private fun resetUiService() {
        // There are probably a few things that could move into here (should panel manager have a reset method called here?)
        uiLauncher?.reset()
        AppManager.getInstance().reset() // App lists may need adjusting after a settings change
        androidUiManager?.reset()
        thirdPartyFeaturedWorkoutScreenTimeout?.reset()
    }

    private fun setupDataMatrix() {
        //region: Shared Datamatrix CallBacks
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET) {
            messageCenterManager?.removeMessagesToHideOnSplash()
            resetUiService()
        }
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value: Int ->
            pauseLogic.processWorkoutRunState(value) // TODO should this be in UiService?
            androidUiManager?.processWorkoutRunState(value)
            thirdPartyFeaturedWorkoutScreenTimeout?.processWorkoutRunState(value)

            //region: workoutpanelmanager logic
            if (value == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
                // Show the workout panels if they are not currently shown and if the app is not handling the behavior
                val appHandlesPause = DataMatrix.getBoolean(UiServiceKeys.DATA_APP_HANDLES_PAUSE_BOOL) // TODO - should this be panel behavior or other app meta data?
                if (!appHandlesPause) {
                    mWorkoutPanelManager?.setWorkoutPanelState(UiServiceKeys.WorkoutPanelState.REGULAR)
                }
            }
            //endregion: workoutpanelmanager logic
        }, true) // fire immediate in case of crash recovery during a paused state
        mDataMatrix.createIntAdapter(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT, {
            // TESTING - add live update for new UI setting for demo and dev, in real product this will likely be left set at start up based on product
            // restart UI so correct panels can be setup
            startUiService()
            AppManager.getInstance().reset() // UI may have different internal apps
        }, false) // do not fire immediate or you will loop
        mDataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, {
            // Some apps have user dependent data in URLs
            // Do we need a better way to handle that? maybe app launch can do a callback for user data?
            // Should we just generate the list every launch request?
            AppManager.getInstance().reset()
            androidUiManager?.reset() // clear clipboard on new/signed out user?
        }, false)
        mDataMatrix.createParcelableAdapter(ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE, ClubComOptions::class.java, {
            AppManager.getInstance().reset() // club com apps may change
        }, false)
        mDataMatrix.createEventAdapter(AppStoreKeys.EVENT_NEW_APP_LIST) { value: String? ->
            // App Store has updated the app list so refresh it
            // reset app list cache in case it changed via club id/dapi environment
            //  TODO do we reset it in manager? do it in reset event above?
            AppStoreConfigManager.reset()
            AppManager.getInstance().reset()
        }
        //endregion: Shared Datamatrix Callbacks

        workoutPanelManagerSpecificAdapters()
        messageCenterManagerSpecificAdapters()
        uiLauncherSpecificAdapters()
        thirdPartyFeaturedWorkoutScreenTimeoutSpecificAdapters()
    }

    private fun workoutPanelManagerSpecificAdapters() {
        // this is only used for panel state requests outside of internal UI service functions, mostly fullscreen toggling (should we just create a toggle fullscreen action?)
        mDataMatrix.createIntAdapter(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT) { newState: Int ->
            // should block attempts to toggle regular and full screen panels outside of a workout? or should we use internal UI states to control that? maybe block in IDLE and SUMMARY?
            val oldState = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT)
            if ((newState != UiServiceKeys.WorkoutPanelState.REGULAR && newState != UiServiceKeys.WorkoutPanelState.FULLSCREEN)
                || (oldState != UiServiceKeys.WorkoutPanelState.IDLE && oldState != UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY)) {
                mWorkoutPanelManager?.setWorkoutPanelState(newState)
            } else {
                Log.d(TAG, "Blocked ACTION_SET_WORKOUT_PANEL_STATE_INT old=$oldState new=$newState")
            }
        }
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_BEHAVIOR_PARCELABLE, PanelBehavior::class.java) { value: PanelBehavior? ->
            mWorkoutPanelManager?.setWorkoutPanelBehavior(value)
        }
        mDataMatrix.createBoolAdapter(WorkoutKeys.DATA_LAUNCH_STATE_BOOL, { value: Boolean ->
            if (value) {
                // if workout launch/switch, reset panels for next workout
                mWorkoutPanelManager?.resetForWorkout()
            } else {
                // if workout launch bails/ends, make sure these panels are not up
                mWorkoutPanelManager?.showPanel(PanelIds.COUNTDOWN_PANEL, false)
            }
        }, false)
        mDataMatrix.createIntAdapter(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_MS_INT, { timeMs: Int ->
            mWorkoutPanelManager?.showOverlay(timeMs)
        }, false)
        mDataMatrix.createBoolAdapter(UiServiceKeys.ACTION_SHOW_DIGITAL_SAFETY_PIN_PROMPT_BOOL) { value: Boolean ->
            // by checking this, we allow for UI to request a check/prompt before actually attempting a workout start which may be smoother than waiting til last moment
            if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL) && DataMatrix.getBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL)) {
                mWorkoutPanelManager?.showDigitalSafetyPrompt(value)
            }
        }
        mDataMatrix.createEventAdapter(HALKeys.EVENT_SCREEN_TOUCH) { mWorkoutPanelManager?.onTouch() } // FOR AUTO HIDE POC
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING) { value: String? -> mWorkoutPanelManager?.updateTheme(value) } // Compose panel theming
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_ADD_UI_PANEL_PARCELABLE, InnerPanelHolder::class.java) { panelHolder: InnerPanelHolder? ->
            mWorkoutPanelManager?.addInnerPanel(this, panelHolder)
        }
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_SEND_PANEL_DATA_PARCELABLE, BasePanelData::class.java) { panelData: BasePanelData? ->
            mWorkoutPanelManager?.sendInnerPanelData(panelData)
        }

        mDataMatrix.createEventAdapter(UiServiceKeys.ACTION_SHOW_DEBUG_PANEL_EVENT) {
            mWorkoutPanelManager?.showDebugPanel()
        }
    }

    private fun messageCenterManagerSpecificAdapters() {
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, MessageCenterMessage::class.java, { message: MessageCenterMessage? ->
            message?.let { messageCenterManager?.queueMessage(it) }
        }, false)
        mDataMatrix.createStringAdapter(UiServiceKeys.ACTION_REMOVE_MESSAGE_BY_TAG_STRING, {
            messageCenterManager?.removeMessagesByTag(it)
        }, false)
        mDataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_START) {
            messageCenterManager?.onWorkoutStart()
        }
        mDataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, {
            messageCenterManager?.onHeartRate(it)
        }, false)
        mDataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, {
            messageCenterManager?.processWorkoutPanelState()
        }, false)
        mDataMatrix.createParcelableAdapter(UiServiceKeys.DATA_UI_OVERLAY_VIEWPORT_PARCELABLE, Rect::class.java, {
            // ok, if there is an overlay view port, then the workout panels went overlay, if message panel was up, it would be underneath in z order
            if (it != null && !it.isEmpty) messageCenterManager?.reAddPanels()
        }, false)
    }

    private fun uiLauncherSpecificAdapters() {
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, LaunchScreenData::class.java) { value: LaunchScreenData ->
            uiLauncher?.launch(value)
            applyPostWorkoutTimeout(data = value, timeout = thirdPartyFeaturedWorkoutScreenTimeout)
        }
        mDataMatrix.createParcelableAdapter(UiServiceKeys.ACTION_LAUNCH_APP_PARCELABLE, LaunchAppData::class.java) { value: LaunchAppData -> uiLauncher?.launch(value) }
        mDataMatrix.createParcelableAdapter(UiServiceKeys.EVENT_UI_RESUMED_PARCELABLE, Intent::class.java) { value: Intent -> uiLauncher?.onUiResumed(value) }
        mDataMatrix.createParcelableAdapter(UiServiceKeys.EVENT_UI_PAUSED_PARCELABLE, Intent::class.java) { value: Intent -> uiLauncher?.onUiPaused(value) }
        mDataMatrix.createStringAdapter(UiServiceKeys.ACTION_LAUNCH_UI_PANEL_STRING) { value: String -> uiLauncher?.launchPanel(value) }
    }

    private fun thirdPartyFeaturedWorkoutScreenTimeoutSpecificAdapters() {
        mDataMatrix.createBoolAdapter(WorkoutKeys.ACTION_PASSIVE_WORKOUT_BOOL, {
            thirdPartyFeaturedWorkoutScreenTimeout?.handlePassiveWorkouts(it)
        }, false)
        mDataMatrix.createStringAdapter(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, {
            thirdPartyFeaturedWorkoutScreenTimeout?.processWorkoutAppUUID(it)
        }, false)
    }

}

/** Applies what a screen launch does to the post workout [timeout], see [SummaryScreenPolicy.postWorkoutTimeout] */
internal fun applyPostWorkoutTimeout(data: LaunchScreenData, timeout: ThirdPartyFeaturedWorkoutScreenTimeout?) {
    val surveyShown = data.intent.getBooleanExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, false)
    when (SummaryScreenPolicy.postWorkoutTimeout(screen = data.screen, surveyShown = surveyShown)) {
        PostWorkoutTimeout.Hold -> timeout?.holdForSurvey()
        PostWorkoutTimeout.Restart -> timeout?.restartAfterSurvey()
        PostWorkoutTimeout.Unchanged -> Unit
    }
}
