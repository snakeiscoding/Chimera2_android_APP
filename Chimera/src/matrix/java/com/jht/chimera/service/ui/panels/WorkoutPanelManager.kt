package com.jht.chimera.service.ui.panels

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import com.jht.androidcommonalgorithms.timer.JHTTimer
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.process.KillProcessHelper
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.ui.PanelManager
import com.jht.chimera.service.ui.UiConfiguration.Companion.getCurrent
import com.jht.chimera.service.ui.panels.ExtraBarOptions.Companion.defaultOptions
import com.jht.chimera.service.ui.panels.PanelBehavior.Companion.NormalPanelBehavior
import com.jht.chimera.service.ui.panels.compose.ComposePanel
import com.jht.chimera.service.ui.panels.compose.ModulePanel
import com.jht.chimera.service.ui.panels.compose.WorkoutBottomPanel2
import com.jht.chimera.service.ui.panels.compose.WorkoutTopPanel2
import com.jht.chimera.service.ui.panels.compose.debug.DebugPanel
import com.jht.chimera.service.ui.panels.compose.hybridTreadmill.ReverseBeltPanel
import com.jht.chimera.service.ui.panels.compose.hybridTreadmill.ReverseCountdownPanel
import com.jht.chimera.service.ui.panels.sidepanel.BorderLeftPanel
import com.jht.chimera.service.ui.panels.sidepanel.BorderRightPanel
import com.jht.chimera.service.ui.panels.toppanel.TreadSenseTopPanel
import com.jht.chimera.service.ui.panels.toppanel.ValueUpdatePanel
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.UiPanelType
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.mastboard.data.UiEventKeys
import com.jht.theme.DynamicThemeLoader.getCustomTheme
import com.jhtp.dapiservice.an.types.ButtonPressType

/**
 * A singleton class that adds or removes panels using the [PanelManager]. This class tracks
 * all the UI panels that will be added and provides a high level API for displaying or hiding
 * them correctly.
 */
class WorkoutPanelManager private constructor() : IWorkoutPanelManager {
    private var currentPanelSet: Array<PanelConfig>? = null // panel set actually showing
    private var currentPanelBehavior = NormalPanelBehavior // panel behavior being followed
    private var cachedAppPanelBehavior: PanelBehavior? =
        null // panel behavior used when switched to a third party app (null defers to default)
    private var currentPanelOptions: ExtraBarOptions? =
        defaultOptions // the last used panel options for detecting changes
    private var mPanelManager: PanelManager? = null
    private val mHandler = Handler()
    private var overlayDelayMs = 0

    // POC auto hide top/bottom workout panel only
    private var workoutTopPanel2: WorkoutTopPanel2? = null
    private var workoutBottomPanel2: WorkoutBottomPanel2? = null

    // Other panels
    private var mErrorPanel: ErrorPanel? = null
    private var mOverlayPanel: BlackOverlayPanel? = null
    private var mTreadSensePanel: TreadSenseTopPanel? = null
    private var mValueUpdatePanel: ValueUpdatePanel? = null
    private var mReverseBeltPanel: ReverseBeltPanel? = null
    private var mReverseCountdownPanel: ReverseCountdownPanel? = null
    private var mTeamViewerPanel: TeamViewerPanel? = null
    private var mFloatingMinimizePanel: FloatingMinimizePanel? = null
    private var mDebugPanel: DebugPanel? = null

    // List of panels added by modules
    private val modulePanels: MutableList<ModulePanel> = ArrayList()
    override fun initializeWorkoutPanelManager(context: Context) {
        val themeContext =
            ContextUtil.getThemeContext(context) // needed by AppCompatTextView, AppCompatImageButton, AppCompatImageButton
        cleanupWorkoutPanelManager()
        workoutTopPanel2 = WorkoutTopPanel2(context)
        workoutBottomPanel2 = WorkoutBottomPanel2(context)
        mErrorPanel = ErrorPanel(themeContext)
        mOverlayPanel = BlackOverlayPanel(themeContext)
        mTeamViewerPanel = TeamViewerPanel(themeContext)
        mFloatingMinimizePanel = FloatingMinimizePanel(themeContext)
        if (BuildConfig.DEBUG) {
            mDebugPanel = DebugPanel(themeContext)
        }
        mReverseBeltPanel = ReverseBeltPanel(themeContext)
        mReverseCountdownPanel = ReverseCountdownPanel(themeContext)

        // Tread Sense and Value Update Panels share a common layout across build variants,
        // and need to request a theme context in order to apply the correct style
        mTreadSensePanel = TreadSenseTopPanel(themeContext)
        mValueUpdatePanel = ValueUpdatePanel(themeContext)

        mPanelManager = PanelManager(context)

        // POC auto hide
        mPanelManager?.addPanel(PanelIds.TOP_PANEL2.id, workoutTopPanel2) // TODO TESTING NEW PANELS
        mPanelManager?.addPanel(PanelIds.BOTTOM_PANEL2.id, workoutBottomPanel2)
        val uiConfig = getCurrent()
        val panelIds = arrayOf(
            PanelIds.TOP_PANEL,
            PanelIds.LEFT_PANEL,
            PanelIds.RIGHT_PANEL,
            PanelIds.BOTTOM_PANEL
        )
        for (panelId in panelIds) {
            val panel = uiConfig.createComposePanel(panelId, context)
            if (panel != null) mPanelManager?.addPanel(panelId.id, panel)
        }
        mPanelManager?.addPanel(PanelIds.COUNTDOWN_PANEL.id, CountdownPanel(themeContext))
        mPanelManager?.addPanel(PanelIds.BORDER_LEFT_PANEL.id, BorderLeftPanel(context))
        mPanelManager?.addPanel(PanelIds.BORDER_RIGHT_PANEL.id, BorderRightPanel(context))
        mPanelManager?.addPanel(PanelIds.E_STOP_PANEL.id, mErrorPanel)
        mPanelManager?.addPanel(PanelIds.OVERLAY_PANEL.id, mOverlayPanel)
        mPanelManager?.addPanel(PanelIds.TREAD_SENSE_PANEL.id, mTreadSensePanel)
        mPanelManager?.addPanel(PanelIds.VALUE_UPDATE_PANEL.id, mValueUpdatePanel)
        mPanelManager?.addPanel(PanelIds.REVERSE_BELT_PANEL.id, mReverseBeltPanel)
        mPanelManager?.addPanel(PanelIds.REVERSE_COUNTDOWN_PANEL.id, mReverseCountdownPanel)
        mPanelManager?.addPanel(PanelIds.TEAM_VIEWER_PANEL.id, mTeamViewerPanel)
        mPanelManager?.addPanel(PanelIds.FLOATING_MINIMIZE_PANEL.id, mFloatingMinimizePanel)
        mDebugPanel?.let { mPanelManager?.addPanel(PanelIds.DEBUG_PANEL.id, it) }
        mErrorPanel?.runPanelLogic()

        // Do not cache if we are restarting the UI service, can likely be mid-workout
        val hasBeenInitialized = DataMatrix.getBoolean(HALKeys.DATA_CONSOLE_SERVICES_INIT_BOOL)
        if (hasBeenInitialized) {
            var activeAppUuid = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING)
            if (activeAppUuid.isEmpty()) { //if UUID is empty, which currently never will be, it will fall back to App ID so we never lose track of active application eg. Hero's Journey
                activeAppUuid = DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING)
            }
            val activeApp = AppManager.getInstance().getApp(activeAppUuid)
            if (activeApp?.specialPanelBehavior != null) { // get panel behavior from AppManager and restore it.
                cachedAppPanelBehavior = activeApp.specialPanelBehavior
                setWorkoutPanelBehavior(activeApp.specialPanelBehavior)
            } else {
                refreshPanelSet()
            }
        }
    }

    private fun cleanupWorkoutPanelManager() {
        currentPanelSet = null
        if (mPanelManager != null) mPanelManager?.disposeAll() // hide/dispose all and clear tracking list
        modulePanels.clear() //removeInnerPanels(); panel manager dumped all it's panels, so we can probably just clear this?
    }

    override fun isPanelShown(workoutPanelKey: PanelIds): Boolean {
        return mPanelManager?.isAdded(workoutPanelKey.id) ?: true
    }

    override fun setWorkoutPanelBehavior(behavior: PanelBehavior?) {
        val useAutoHide =
            ProductConfig.hasPremiumUi() && DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_AUTO_HIDE_PANELS_BOOL)
        // this should never be null, default to normal if not specified
        val newBehavior =
            behavior ?: if (useAutoHide) AppManager.PocNormalPanelBehavior else NormalPanelBehavior
        if (!newBehavior.equals(currentPanelBehavior)) {
            currentPanelBehavior = newBehavior
            updatePanelOptions()
            refreshPanelSet() // do we need to kick a refresh?
        }
    }

    private fun refreshPanelSet() {
        val state = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT)
        setWorkoutPanelState(state)
    }

    override fun setWorkoutPanelState(state: Int) {
        DataMatrix.putInt(
            UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT,
            state
        ) // expose current state to rest of system, only UI service should set this
        when (state) {
            UiServiceKeys.WorkoutPanelState.IDLE -> {
                removeInnerPanels()
                applyPanelSet(currentPanelBehavior.idle)
            }

            UiServiceKeys.WorkoutPanelState.REGULAR -> {
                fullscreenChange(false)
                applyPanelSet(currentPanelBehavior.regular)
            }

            UiServiceKeys.WorkoutPanelState.FULLSCREEN -> {
                fullscreenChange(true)
                applyPanelSet(currentPanelBehavior.fullscreen)
            }

            UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY -> {
                removeInnerPanels()
                applyPanelSet(currentPanelBehavior.summary)
            }
        }
    }

    override fun updateCachedAppWorkoutPanelBehavior(panelBehavior: PanelBehavior?) {
        cachedAppPanelBehavior = panelBehavior
    }


    override fun useCachedAppWorkoutPanelBehavior() {
        setWorkoutPanelBehavior(cachedAppPanelBehavior)
    }

    override fun setPanelOverrides(panelOverrides: Bundle) {
        mPanelManager?.forEachPanel { uid, panel: IPanel? ->
            if (panel is ComposePanel) {
                panel.setPanelOverrides(panelOverrides)
                mPanelManager?.updatePanel(uid)
            }
        }
    }
    override fun clearPanelOverrides(panelOverrides: Bundle) {
        mPanelManager?.forEachPanel { uid, panel: IPanel? ->
            if (panel is ComposePanel) {
                panel.clearPanelOverrides(panelOverrides)
                mPanelManager?.updatePanel(uid)
            }
        }
    }

    private fun fullscreenChange(isFull: Boolean) {
        // since full screen alters the layout, we need to make sure we change the panels before layout gets updated
        mPanelManager?.forEachPanel { uid, panel: IPanel? ->
            if (panel is ComposePanel) {
                panel.setFullscreen(isFull)
            }
        }
        if (isFull) autoHideTimer.stop() // if going full anyway, stop any auto hide timeout that is running
        else resetAutoHidePanels() // if going to regular panels, restart the auto hide
    }

    private fun applyPanelSet(panelSet: Array<PanelConfig>?) {
        val diff = PanelSetDifference(currentPanelSet, panelSet)
        if (diff.isNotEmpty()) {
            val viewportCalculator = ViewportCalculator()
            for ((panel) in diff.removeList) {
                showPanel(panel, false)
            }
            for ((panelId, mode) in diff.addList) {
                mPanelManager?.getPanel(panelId.id)?.let { panel ->
                    panel.setPanelMode(mode)
                    showPanel(panelId, true)
                    viewportCalculator.onPanelAdd(panelId, panel, mode)
                }
            }
            currentPanelSet = panelSet
            DataMatrix.putParcelable(UiServiceKeys.DATA_UI_OVERLAY_VIEWPORT_PARCELABLE, viewportCalculator.computeResult())
        }
        // since we may have re-arranged the screen, make sure any panels outside the set also get updated
        mPanelManager?.updatePanels() // should we only do the visible panels that were not a part of the current set? seems fine to just do all
    }

    private fun updatePanelOptions() {
        // use default options if none (could leave it null since we only care if changed)
        val options = currentPanelBehavior.options ?: defaultOptions
        if (currentPanelOptions !== options) {
            if (mPanelManager?.isAdded(PanelIds.BOTTOM_PANEL.id)
                    ?: true
            ) mPanelManager?.updatePanel(
                PanelIds.BOTTOM_PANEL.id
            )
            mPanelManager?.forEachPanel { uid, panel: IPanel? ->
                if (panel is ComposePanel) {
                    panel.setAppBar(options)
                }
            }
            if (mPanelManager?.isAdded(PanelIds.BOTTOM_PANEL2.id)
                    ?: true
            ) mPanelManager?.updatePanel(
                PanelIds.BOTTOM_PANEL2.id
            )
            currentPanelOptions = options
        }
    }

    override fun showOverlay(delayMs: Int) { // is there a better generic way to handle panels with a time parameter?
        overlayDelayMs = delayMs
        showPanel(PanelIds.OVERLAY_PANEL, true)
    }

    override fun showPanel(panelKey: PanelIds?, isVisible: Boolean) {
        if (panelKey == null) return
        // Do not allow showing panels that are overridden
        if (isVisible && isPanelOverridden(panelKey)) return
        var panelId = panelKey.id
        if (PanelIds.E_STOP_PANEL == panelKey) {
            panelId = null
            showEStopPanel(isVisible)
        } else if (PanelIds.OVERLAY_PANEL == panelKey && isVisible) {
            mHandler.postDelayed(
                { mOverlayPanel?.removePanelWithAnimation() },
                overlayDelayMs.toLong()
            )
        }
        if (panelId != null) {
            if (isVisible) {
                mPanelManager?.showPanel(panelId)
            } else {
                mPanelManager?.hidePanel(panelId)
            }
        }
    }

    override fun resetForWorkout() {
        mPanelManager?.forEachPanel { uid, panel: IPanel? ->
            if (panel is ComposePanel) {
                panel.onWorkoutReset()
            }
        }
    }

    /**
     * Checks whether a registered module panel overrides a specific panel. If the panel is
     * overridden it should not be displayed, and the module will handle displaying a panel in its
     * place.
     *
     * @param panelKey The panel to check
     * @return Whether the panel is overridden
     */
    private fun isPanelOverridden(panelKey: PanelIds): Boolean {
        return when (panelKey) {
            PanelIds.VALUE_UPDATE_PANEL -> isPanelTypeAdded(UiPanelType.ValueUpdatePanelOverride)
            else -> false
        }
    }

    private fun isPanelTypeAdded(panelType: UiPanelType): Boolean {
        for (panel in modulePanels) {
            if (panel.getType() == panelType) return true
        }
        return false
    }

    override fun updatePanel(workoutPanelKey: PanelIds) {
        mPanelManager?.updatePanel(workoutPanelKey.id)
    }


    /**
     * Displays or hides the EStop panel
     *
     * @param isVisible Whether to show or hide the panel
     */
    private fun showEStopPanel(isVisible: Boolean) {
        if (isVisible) {
            DataMatrix.putString(
                UiEventKeys.Key.DATA_UI_EVENT_STRING,
                UiEventKeys.EVENT.SHOW_ESTOP.name
            )
            if (mPanelManager?.isAdded(PanelIds.E_STOP_PANEL.id) ?: true) return
            mPanelManager?.showPanel(PanelIds.E_STOP_PANEL.id)
        } else {
            DataMatrix.putString(
                UiEventKeys.Key.DATA_UI_EVENT_STRING,
                UiEventKeys.EVENT.CLOSE_ESTOP.name
            )
            mPanelManager?.hidePanel(PanelIds.E_STOP_PANEL.id)
        }
    }

    private var safetyPrompt: DigitalSafetyPinDialog? = null
    override fun showDigitalSafetyPrompt(show: Boolean) {
        Log.d(TAG, "showDigitalSafetyPrompt $show")
        // show or hide pin prompt
        if (show) {
            if (safetyPrompt == null) {
                safetyPrompt = DigitalSafetyPinDialog(ContextUtil.getApplicationContext())
            }
            safetyPrompt?.show()
        } else {
            if (safetyPrompt != null) safetyPrompt?.hide()
        }
    }

    override fun addInnerPanel(context: Context, panelHolder: InnerPanelHolder) {
        val modulePanel = ModulePanel(context, panelHolder, mPanelManager ?: PanelManager(context))
        modulePanels.add(modulePanel)
        mPanelManager?.addPanel(modulePanel.getPanelId(), modulePanel)
    }

    override fun sendInnerPanelData(panelData: BasePanelData) {
        for (panel in modulePanels) {
            if (panel.getPanelId() == panelData.getPanelId()) {
                panel.receivePanelData(panelData)
                return
            }
        }
    }

    override fun removeInnerPanels() {
        for (panel in modulePanels) {
            mPanelManager?.hidePanel(panel.getPanelId()) { _: Void? ->
                mPanelManager?.removePanel(panel.getPanelId())
                panel.dispose()
            }
        }
        modulePanels.clear()
    }

    override fun updateTheme(themeUri: String) {
        val themeData = getCustomTheme(themeUri)
        mPanelManager?.forEachPanel { uid, panel: IPanel? ->
            // instanceof is often frowned upon, but I don't want to touch every IPanel right now
            // TODO - consider adding updateTheme() to IPanel interface or maybe create a common base for non-compose panels so they can opt-in to these new methods?
            if (panel is ComposePanel) {
                panel.updateTheme(themeData)
            }
        }
    }

    override fun showDebugPanel() {
        mDebugPanel?.let {
            if (!isPanelShown(PanelIds.DEBUG_PANEL)) {
                showPanel(PanelIds.DEBUG_PANEL, true)
            }
        }
    }

    //region autoHide feature
    enum class AutoHideBehavior {
        TOUCH,
        SWIPE,
        ICON
    }

    private var currentBehavior = AutoHideBehavior.TOUCH
    override fun setAutoHideBehavior(behavior: AutoHideBehavior) {
        currentBehavior = behavior
        workoutTopPanel2?.setAutoHideBehavior(behavior)
        workoutBottomPanel2?.setAutoHideBehavior(behavior)
        resetAutoHidePanels()
    }

    override fun resetAutoHidePanels() {
        autoHideTimer.stop()
        val useAutoHide =
            ProductConfig.hasPremiumUi() && DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_AUTO_HIDE_PANELS_BOOL)
        if (useAutoHide) {
            if (DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN) {
                DataMatrix.putInt(
                    UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT,
                    UiServiceKeys.WorkoutPanelState.REGULAR
                )
            }
            autoHideTimer.start()
        }
    }

    private val autoHideTimer = JHTTimer(5000) {
        if (DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.REGULAR) {
            DataMatrix.putInt(
                UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT,
                UiServiceKeys.WorkoutPanelState.FULLSCREEN
            )
        }
    }

    override fun onTouch() {
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
            if (AutoHideBehavior.TOUCH == currentBehavior) {
                resetAutoHidePanels()
            } else {
                autoHideTimer.resetIfActive() // if user is active, don't auto hide, but if already fullscreen do not revert either
            }
        }
    } //endregion autoHide feature

    companion object {
        private const val TAG = "WorkoutPanelManager"
        private var mIWorkoutPanelManager: IWorkoutPanelManager? = null

        @JvmStatic
        val instance: IWorkoutPanelManager?
            get() {
                if (mIWorkoutPanelManager == null) {
                    mIWorkoutPanelManager = WorkoutPanelManager()
                }
                return mIWorkoutPanelManager
            }

        // Some common click handlers shared by some panels
        // These don't need to be static, could add to interface and invoke via WorkoutPanelManager.getInstance() ?
        @JvmStatic
        fun onHomeClick(v: View?) {
            //Stores workout selection button press event for Dapi Analytics
            DataMatrix.putString(
                DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING,
                ButtonPressType.HOME_BTN_PRESS.name
            )
            if (DataMatrix.getBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL)) {
                DataMatrix.putBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL, false)
                if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
                    LaunchAppUtil.launchWorkoutScreen()
                }
            } else {
                Log.w(
                    TAG,
                    "onHomeClick DATA_HOME_BUTTON_ENABLED_BOOL is false"
                ) // TODO - we should reevaluate these keys, maybe home button can handle home key events directly?
            }
        }

        /**
         * Dialog must show over the third party app.
         * Do we need something to dismiss this if third party interface exits by another means?
         * @param v
         */
        @JvmStatic
        fun onExitClick(v: View) {
            val context = v.context
            val dialog = ExitDialog(context)
            dialog.init { yes: Boolean ->
                if (yes) {
                    exitThirdPartyInterface()
                }
            }
            dialog.show()
        }

        private fun exitThirdPartyInterface() {
            // You cannot exit in the middle of a workout. This is safer than trying to abort the workout with abnormal flow and stop workout could trigger summary which is odd.
            val isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP
            val isLaunchingWorkout = DataMatrix.getBoolean(WorkoutKeys.DATA_LAUNCH_STATE_BOOL)
            if (isInWorkout || isLaunchingWorkout) {
                Log.w(TAG, "You cannot exit third party interface while in workout or launching a workout.")
                return
            }

            val uuid = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING)

            // Only launch go screen on commercial if welcome screen is enabled
            if (ProductConfig.isCommercial() && DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)) {
                DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, "")
                DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_APP_SETUP_PARCELABLE, Bundle.EMPTY) // probably not needed, but to be thorough
                LaunchAppUtil.launchGoScreen()
            } else {
                // Commercial doesn't require updating SSO at this point, but update it for retail
                if (!ProductConfig.isCommercial()) DataMatrix.putString(
                    SSOKeys.ACTION_SSO_UPDATE_APP_STRING,
                    uuid
                ) // make sure workout end logic like SSO still happens
                LaunchAppUtil.launchSplashScreen()
            }
            // Stop the app
            val app = AppManager.getInstance().getApp(uuid)
            if (app != null) {
                val killProcessHelper = KillProcessHelper()
                killProcessHelper.addPackagesToStop(app.getAppPackage(), true)
                killProcessHelper.kill()
            }
            // stop workout just in case (should probably be AFTER the app stop to avoid race with app?)
            DataMatrix.sendEvent(WorkoutKeys.ACTION_ABORT_WORKOUT)
        }
    }
}

/** Need to calculate the visible region for an app given in-place vs overlay panels */
private class ViewportCalculator() {
    private val fullscreenRect = Rect(0, 0, RealScreen.widthPixels - 1, RealScreen.heightPixels - 1)
    private val normalViewport = Rect(fullscreenRect)
    private val overlayViewport = Rect(fullscreenRect)

    fun onPanelAdd(id: PanelIds, panel: IPanel, panelMode: WorkoutPanelMode) {
        val currentViewport = if (panelMode == WorkoutPanelMode.NORMAL) normalViewport else overlayViewport
        val params = panel.panelParams
        // could check the gravity, but some floating panels aren't meant to impact the viewport?
        // could we check gravity and the w/h to see if panel covers a full edge?
        // if we gear this to the workout panels, then we likely miss the border panels? but borders are for TPA which won't have access to this
        // NOTE: most panels give 0,0 for x,y so don't necessarily trust those
        when (id) {
            PanelIds.LEFT_PANEL -> {
                currentViewport.left = maxOf(currentViewport.left, params.width)
            }
            PanelIds.RIGHT_PANEL -> {
                currentViewport.right = minOf(currentViewport.right, fullscreenRect.right - params.width)
            }
            PanelIds.TOP_PANEL -> {
                currentViewport.top = maxOf(currentViewport.top, params.height)
            }
            PanelIds.BOTTOM_PANEL -> {
                currentViewport.bottom = minOf(currentViewport.bottom, fullscreenRect.bottom - params.height)
            }
            else -> {}
        }
    }
    fun computeResult(): Rect {
        // if viewport is the same, provide empty result, there is no need for a keep-out region
        if (normalViewport == overlayViewport) return Rect()
        // adjust overlay viewport relative to normal viewport? for now assume all or nothing
        return overlayViewport
    }
}
