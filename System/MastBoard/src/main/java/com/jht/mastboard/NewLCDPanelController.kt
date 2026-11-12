package com.jht.mastboard

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.jht.enums.FrameType
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.mastboard.interfaces.MastboardModel
import com.jht.mastboard.interfaces.getMastBoardInjectionEntryPoint
import com.jht.mastboard.lib.PanelSide
import com.jht.mastboard.lib.TouchData
import com.jht.mastboard.ui.GridViewAdapter
import com.jht.mastboard.ui.LCDPresentation
import com.jht.mastboard.ui.hapticwidgets.HapticAppCompatImageButton
import com.jht.mastboard.ui.hapticwidgets.HapticOnClickListener
import com.jht.mastboard.ui.hapticwidgets.HapticOnTouchListener
import com.jht.mastboard.ui.hapticwidgets.HapticView
import com.jht.widgets.helpers.getInjectionEntryPoint

/**
 * The tag to use when outputting logs for this class.
 */
private const val TAG = "NewLCDPanelController"


/**
 * This is the new controller I would like to use.
 */
class NewLCDPanelController(val context: Context) : AutoCloseable {
    private var mLeftPanelPresentation: LCDPresentation
    private var mRightPanelPresentation: LCDPresentation
    private val mastboardModel: MastboardModel
    private val workoutModel: WorkoutModel
    private val navigationModel: NavigationModel
    private val uiBaseModel: UIBaseModel
    init {
        // injection
        mastboardModel = getMastBoardInjectionEntryPoint(context).mastboard()
        val injection = getInjectionEntryPoint(context)
        workoutModel = injection.workout()
        navigationModel = injection.navigation()
        uiBaseModel = injection.uiBase()
        // ported from: setupDisplays()
        val mDisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val mDisplayLeft = mDisplayManager.createVirtualDisplay("Left LCD", LCDPresentation.WIDTH, LCDPresentation.HEIGHT, LCDPresentation.DENSITY, null, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        val mDisplayRight = mDisplayManager.createVirtualDisplay("Right LCD", LCDPresentation.WIDTH, LCDPresentation.HEIGHT, LCDPresentation.DENSITY, null, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        mLeftPanelPresentation = LCDPresentation(context, mastboardModel, mDisplayLeft.display, PanelSide.LEFT)
        mRightPanelPresentation = LCDPresentation(context, mastboardModel, mDisplayRight.display, PanelSide.RIGHT)
    }

    override fun close() {
        mastboardModel.close()
        workoutModel.close()
        navigationModel.close()
        uiBaseModel.close()
    }

    /**
     * The workout stat being displayed for a panel.
     */
    private enum class WorkoutPanelStat(@DrawableRes val iconId: Int) {
        Incline(R.drawable.ic_incline_yellow),
        Resistance(R.drawable.ic_resistance_yellow),
        Speed(R.drawable.ic_speed_yellow)
    }

    /**
     * Used to update right LCD display when reconnected. This needs to be called after a disconnection, in order to
     * properly update displays.
     */
    @Synchronized
    fun reInitRightLCD() {
        val mDisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val mDisplayRight = mDisplayManager.createVirtualDisplay("Right LCD", LCDPresentation.WIDTH, LCDPresentation.HEIGHT, LCDPresentation.DENSITY, null, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        mRightPanelPresentation.dismiss()
        mRightPanelPresentation = LCDPresentation(context, mastboardModel, mDisplayRight.display, PanelSide.RIGHT)
        updateToggleDisplays()
    }

    /**
     * Used to update left LCD display when reconnected. This needs to be called after a disconnection, in order to
     * properly update displays.
     */
    @Synchronized
    fun reInitLeftLCD() {
        val mDisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val mDisplayLeft = mDisplayManager.createVirtualDisplay("Left LCD", LCDPresentation.WIDTH, LCDPresentation.HEIGHT, LCDPresentation.DENSITY, null, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        mLeftPanelPresentation.dismiss()
        mLeftPanelPresentation = LCDPresentation(context, mastboardModel, mDisplayLeft.display, PanelSide.LEFT)
        updateToggleDisplays()
    }

    /**
     * The current panel state. If null, then we don't know the state and need to be informed of the
     * state.
     */
    private var currentState: PanelControllerState? = null

    /**
     * Whether the LCD debugging panels is being displayed or not. These must only be displayed in
     * MES and must no longer be displayed if leaving.
     */
    private var displayingLCDDebuggingPanels: Boolean = false

    /**
     * Whether the matrix dashboard popup is being displayed or not.
     */
    private var displayingMatrixDashboard: Boolean = false

    /**
     * The E-Stop is being displayed or not.
     */
    private var displayingEStopActive: Boolean = false

    /**
     * Whether the connect to watch popup is being displayed or not.
     */
    private var displayingConnectToWatch: Boolean = false

    /**
     * Whether the featured workout setup popup is displaying or not.
     */
    private var displayingFeaturedWorkoutSetup: Boolean = false

    /**
     * Whether the countdown popover is displaying or not.
     *
     * fixme: Check if this needs to be altered to be synchronized or not.
     */
    private var displayingCountdownPopover: Boolean = false

    /**
     * Enable the haptic debug testing panels. These must only be enabled when [currentState] is
     * [PanelControllerState.DisplayingMES]. In all other states, this needs to be ignored or
     * disabled.
     *
     * After marking as enabled, this must request [updateToggleDisplays].
     */
    @Synchronized
    fun enableLCDDebuggingPanels() {
        if (currentState == PanelControllerState.DisplayingMES) {
            Log.d(TAG, "Toggling the custom LCD debugging.")
            displayingLCDDebuggingPanels = true
            updateToggleDisplays()
        } else {
            Log.w(TAG, "Ignoring request because display state is: ${currentState?.name}")
        }
    }

    /**
     * Set the value of [displayingMatrixDashboard] to the given value if different. If the given
     * value changes the previous value, [updateToggleDisplays] is called to ensure the toggles are
     * displaying correctly. If the given value matches the current value, nothing changes.
     *
     * @param displaying `true` if the matrix dashboard is being displayed, else `false`.
     */
    @Synchronized
    fun setDisplayingMatrixDashboard(displaying: Boolean) {
        if (displayingMatrixDashboard != displaying) {
            Log.d(TAG, "Setting displaying matrix dashboard to: $displaying")
            displayingMatrixDashboard = displaying
            updateToggleDisplays()
        }
    }

    /**
     * Set the value of [displayingConnectToWatch] to the given value if different. If the given
     * value changes the previous value, [updateToggleDisplays] is called to ensure the toggles are
     * displaying correctly. If the given value matches the current value, nothing changes.
     *
     * * ## Warnings
     *
     * Attempts to enable this while in a workout must be ignored. Not ignoring will result in
     * loss of workout control buttons.
     *
     * @param displaying `true` if the connect to watch popup is being displayed, else `false`.
     */
    @Synchronized
    fun setDisplayingConnectToWatch(displaying: Boolean) {
        Log.d(TAG, "setDisplayingConnectToWatch($displaying)")
        outputCurrentStates()

        if (currentState == PanelControllerState.InWorkout) {
            if (displayingConnectToWatch) {
                Log.d(TAG, "Connect to watch toggles are not allowed during a workout. Forcing to not displayed.")
                displayingConnectToWatch = false
                updateToggleDisplays()
            } else {
                Log.w(TAG, "Connect to watch is not allowed during a workout, ignoring the request.")
            }

            return
        }

        if (displayingConnectToWatch != displaying) {
            displayingConnectToWatch = displaying
            updateToggleDisplays()
        }
    }

    /**
     * Set the value of [displayingFeaturedWorkoutSetup] to the given value if different. If the
     * given value changes the previous value, [updateToggleDisplays] is called to ensure the
     * toggles are displaying correctly. If the given value matches the current value, nothing
     * changes.
     *
     * ## Warnings
     *
     * * This value must not update if we are in a workout. The reason we need to check this one is
     * because we can do this through "Change Workout".
     * * This value must not update if [PanelControllerState.IFITLoading],
     * [PanelControllerState.EGYMLoading], or [PanelControllerState.YMCA360Loading]. The way our
     * states are published causes these to load and then false report a loading state.
     *
     *
     * @param displaying `true` if displaying featured workout setup popup, else `false`.
     */
    @Synchronized
    fun setDisplayingFeaturedWorkoutSetup(displaying: Boolean) {
        outputCurrentStates()
        // Must not allow showing during these states.
        if (currentState == PanelControllerState.EGYMLoading || currentState == PanelControllerState.YMCA360Loading || currentState == PanelControllerState.IFITLoading || currentState == PanelControllerState.InWorkout) {
            if (displayingFeaturedWorkoutSetup) {
                Log.w(TAG, "Force hiding the featured workout setup popup because it's not allowed at this time.")
                displayingFeaturedWorkoutSetup = false
                updateToggleDisplays()
            } else {
                Log.w(TAG, "Ignoring an outside attempt to configure featured workout setup popup to '$displaying' because it's not allowed at this time.")
            }

            return
        }

        if (displayingFeaturedWorkoutSetup != displaying) {
            displayingFeaturedWorkoutSetup = displaying
            updateToggleDisplays()
        }
    }

    @Synchronized
    fun setDisplayingEStopActive(displaying: Boolean) {
        if (displayingEStopActive != displaying) {
            Log.w(TAG, "Setting displaying E-Stop active to: $displaying")
            displayingEStopActive = displaying

            // in the event we get here, just set all to false. This might not be needed.
            if (displayingEStopActive) {
                displayingFeaturedWorkoutSetup = false
                displayingMatrixDashboard = false
                displayingConnectToWatch = false
            }

            updateToggleDisplays()
        }
    }

    /**
     * Set the value of [displayingCountdownPopover] to the given value if different. If the given
     * value changes the previous value, [updateToggleDisplays] is called to ensure the toggles are
     * displaying correctly. If the given value matches the current value, nothing changes.
     *
     * @param displaying `true` if the countdown popover is being displayed, else `false`.
     */
    @Synchronized
    fun setDisplayingCountdownPopover(displaying: Boolean) {
        if (displayingCountdownPopover != displaying) {
            Log.w(TAG, "Setting displaying countdown popover to: $displaying")
            displayingCountdownPopover = displaying

            /*
             * If settings to true, force the featured workouts setup to false. Can't have both.
             */
            if (displayingCountdownPopover) {
                displayingFeaturedWorkoutSetup = false
                displayingConnectToWatch = false
            }

            updateToggleDisplays()
        }
    }

    /**
     * This is responsible for updating the current panel state. This is also responsible for
     * ensuring we only perform the update if it's possible. A message is logged if not possible.
     */
    @Synchronized
    fun setState(state: PanelControllerState) {
        // Don't attempt to change to what's already being displayed.
        if (currentState == state)
            return

        val warningMessage: String by lazy { "Unable to transition from ${currentState!!.name} to ${state.name}" }
        var applied = false

        // when there is already a current state
        when (currentState) {
            PanelControllerState.DisplayingSplashScreen -> {
                when (state) {
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingWelcomeScreen,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.EGYMLoading -> {
                        currentState = state
                        applied = true
                        displayingFeaturedWorkoutSetup = false
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.InScreensaver -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.EGYMLoading,
                    PanelControllerState.DisplayingWelcomeScreen -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.InFirstTimeSetup -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.DisplayingWelcomeScreen,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.EGYMLoading -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.DisplayingLoginRegisterScreen -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen, // note: going to splash screen may be a bug.
                    PanelControllerState.DisplayingWelcomeScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.DisplayingWelcomeScreen -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingLoginRegisterScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.DisplayingGoScreen -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingLoginRegisterScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.IFITLoading,
                    PanelControllerState.YMCA360Loading,
                    PanelControllerState.DisplayingTrainingExercisesScreen,
                    PanelControllerState.DisplayingCustomWorkoutScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.InWorkout -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.IFITLoading, // some workouts do not transition to our summary before exiting, so instead go back to loading grips
                    PanelControllerState.YMCA360Loading,
                    PanelControllerState.EGYMLoading,
                    PanelControllerState.DisplayingWorkoutSummary -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.IFITLoading -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                        // This state overrides the featured workout setup popup.
                        displayingFeaturedWorkoutSetup = false
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.EGYMLoading -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver, // note: this is presently displaying on transition to matrix environment. This may cause issues but it's a different flow problem.
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout -> {
                        currentState = state
                        applied = true
                        // This state overrides the featured workout setup popup.
                        displayingFeaturedWorkoutSetup = false
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.YMCA360Loading -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                        // This state overrides the featured workout setup popup.
                        displayingFeaturedWorkoutSetup = false
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.DisplayingTrainingExercisesScreen -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingLoginRegisterScreen,
                    PanelControllerState.DisplayingWelcomeScreen,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingCustomWorkoutScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            /*
             * When the custom workout screen is being displayed, the only states you can transition
             * to are:
             *
             * FIXME: I need to verify that the following list is actually accurate.
             *
             * * Displaying Splashscreen.
             * * In Screensaver
             * * Displaying Login Screen.
             * * Displaying Welcome Screen.
             * * Displaying Go Screen.
             * * Displaying Training Exercises.
             * * The E-Stop is Active.
             * * Displaying MES.
             *
             * All other states must be ignored.
             */
            PanelControllerState.DisplayingCustomWorkoutScreen -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.InScreensaver,
                    PanelControllerState.InFirstTimeSetup,
                    PanelControllerState.DisplayingLoginRegisterScreen,
                    PanelControllerState.DisplayingWelcomeScreen,
                    PanelControllerState.DisplayingGoScreen,
                    PanelControllerState.InWorkout,
                    PanelControllerState.DisplayingTrainingExercisesScreen,
                    PanelControllerState.DisplayingOutOfOrder,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            /**
             * When out of order is displaying.
             */
            PanelControllerState.DisplayingOutOfOrder -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen,
                    PanelControllerState.DisplayingMES -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            /*
             * When currently displaying MES, the only valid return is the splash screen. There is
             * no alternative exit for this section (that I am aware of).
             *
             * NOTE: When leaving MES, the LCD debugging panels need to be disabled.
             */
            PanelControllerState.DisplayingMES -> {
                when (state) {
                    PanelControllerState.DisplayingSplashScreen -> {
                        currentState = state
                        displayingLCDDebuggingPanels = false
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            PanelControllerState.DisplayingWorkoutSummary -> {
                when (state) {
                    PanelControllerState.IFITLoading, // third parties can transition from summary back to loading
                    PanelControllerState.YMCA360Loading,
                    PanelControllerState.EGYMLoading,
                    PanelControllerState.DisplayingSplashScreen -> {
                        currentState = state
                        applied = true
                    }

                    else -> Log.w(TAG, warningMessage)
                }
            }

            /*
             * When the current state is null, the next given value must always take priority. There
             * is no other way to re-enter the state machine.
             */
            null -> {
                currentState = state
                applied = true
            }
        }

        // If the given state was applied, update the toggle displays using the current state.
        if (applied)
            updateToggleDisplays()
    }

    /**
     * This is responsible for forcing the workout toggle displays to update. This is necessary to
     * provide support for pause/stop based on the current workout state. *This **must** only be
     * called from a DataMatrix adapter on `DATA_RUN_STATE_INT`.*
     *
     * ## Notes
     * 1. If the workout toggles are not currently displayed, this request is ignored.
     * 2. If E-Stop is displayed, this request is ignored.
     */
    @Synchronized
    fun updateWorkoutToggleDisplays() {
        if (displayingEStopActive) {
            Log.w(TAG, "Ignoring workout toggle display update request because displaying E-Stop")
            return
        }

        if (currentState != PanelControllerState.InWorkout) {
            Log.w(TAG, "Ignoring workout toggle display update request because current state is: ${currentState?.name ?: "not yet defined"}")
            return
        }

        Log.d(TAG, "Forcing an update of the workout control panels.")
        applyWorkoutControlsPanel(mLeftPanelPresentation)
        applyWorkoutControlsPanel(mRightPanelPresentation)
    }

    /**
     * Force an update to the panel showing featured workouts. This will always be the left panel.
     * This should still only be done if the panel should be shown.
     *
     * FIXME: Remove once the Featured Workouts issues are repaired.
     */
    @Synchronized
    fun updateFeatureWorkoutsPanel() {
        updateToggleDisplays(true)
    }

    /**
     * This is responsible for updating the toggles to ensure their displays match the current
     * state. By design, this will first ensure the popups are all displaying correctly because they
     * override the [currentState]. If no popups are displayed, the [currentState] is used to ensure
     * we are displaying everything correctly.
     *
     * This must only update a toggle if it's current state differs from the required one. For
     * example, if the panel is blank and the new state will also leverage blank, the change will be
     * ignored.
     *
     * fixme: There still needs to be logic for handling the play/pause during a workout; but, that
     *  is for a different frame then this one.
     *
     * @param forceFeaturedWorkoutsUpdate `true` to force, else `false`.
     */
    @Synchronized
    private fun updateToggleDisplays(forceFeaturedWorkoutsUpdate: Boolean = false) {
        outputCurrentStates()

        // Don't show anything else EStop is being displayed.
        if (displayingEStopActive) {
            Log.w(TAG, "Displaying EStop.")
            applyBlankPanel(mLeftPanelPresentation)
            applyBlankPanel(mRightPanelPresentation)
            return
        }

        // Check displaying LCD debugging panels is enabled.
        if (displayingLCDDebuggingPanels) {
            Log.w(TAG, "Displaying LCD debugging panels.")
            applyLCDDebuggingPanel(mLeftPanelPresentation)
            applyLCDDebuggingPanel(mRightPanelPresentation)
            return
        }

        // Check each of the popups to see if they need to override the state. This cannot override
        // if in a workout.
        if (displayingMatrixDashboard && currentState != PanelControllerState.InWorkout) {
            Log.w(TAG, "Displaying Matrix Dashboard popup.")
            applyMatrixLogoPanel(mLeftPanelPresentation)
            applyMatrixLogoPanel(mRightPanelPresentation)
            return
        }

        if (displayingConnectToWatch) {
            Log.w(TAG, "Displaying Connect to watch popup")
            applyMatrixLogoPanel(mLeftPanelPresentation)
            applyMatrixLogoPanel(mRightPanelPresentation)
            return
        }

        if (displayingFeaturedWorkoutSetup) {
            Log.w(TAG, "Displaying Featured Workout popup")
            applyMatrixLogoPanel(mLeftPanelPresentation)
            applyMatrixLogoPanel(mRightPanelPresentation)
            return
        }

        if (displayingCountdownPopover) {
            Log.w(TAG, "Displaying Countdown popover")
            applyWorkoutControlsPanel(mLeftPanelPresentation)
            applyWorkoutControlsPanel(mRightPanelPresentation)
            return
        }

        // Apply the correct base state here.
        when (currentState) {
            PanelControllerState.DisplayingSplashScreen -> {
                Log.w(TAG, "Displaying Splash Screen")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.InScreensaver -> {
                Log.w(TAG, "Displaying ScreenSaver")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.InFirstTimeSetup -> {
                Log.w(TAG, "Displaying First Time Setup")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingLoginRegisterScreen -> {
                Log.w(TAG, "Displaying Login Screen")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingWelcomeScreen -> {
                Log.w(TAG, "Displaying Welcome Screen")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingGoScreen -> {
                Log.w(TAG, "Displaying Go Screen")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyGoButtonPanel(mRightPanelPresentation)
            }

            PanelControllerState.InWorkout -> {
                Log.w(TAG, "Displaying In workout")
                applyWorkoutControlsPanel(mLeftPanelPresentation)
                applyWorkoutControlsPanel(mRightPanelPresentation)
            }

            PanelControllerState.IFITLoading -> {
                Log.w(TAG, "Displaying iFit Loading")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.EGYMLoading -> {
                Log.w(TAG, "Displaying EGYM Loading")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.YMCA360Loading -> {
                Log.w(TAG, "Displaying YMCA360 Loading")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingTrainingExercisesScreen -> {
                Log.w(TAG, "Displaying Training Exercises")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingCustomWorkoutScreen -> {
                Log.w(TAG, "Displaying Custom Workout Screen")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingOutOfOrder -> {
                Log.w(TAG, "Displaying Out of Order")
                applyBlankPanel(mLeftPanelPresentation)
                applyBlankPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingMES -> {
                Log.w(TAG, "Displaying MES")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyMatrixLogoPanel(mRightPanelPresentation)
            }

            PanelControllerState.DisplayingWorkoutSummary -> {
                Log.w(TAG, "Displaying Workout Summary")
                applyMatrixLogoPanel(mLeftPanelPresentation)
                applyHomeButtonPanel(mRightPanelPresentation)
            }

            null -> {
                Log.w(TAG, "Ignoring a request to apply the null state.")
            }
        }
    }

    /**
     * Apply the Blank panel to the given presentation. If the given presentation already has the
     * blank panel applied, nothing is changed.
     *
     * @param presentation The given presentation.
     */
    private fun applyBlankPanel(presentation: LCDPresentation) {
        val panelResourceId = R.layout.ui_blank_screen
        if (presentation.currResourceId == panelResourceId) {
            Log.d(TAG, "Blank panel already applied.")
            return
        }

        Log.d(TAG, "Applying blank panel")
        presentation.updateLCDPanel(panelResourceId)
    }

    /**
     * Apply the Matrix logo panel to the given presentation. If the given presentation already has
     * the matrix logo applied, nothing is changed.
     *
     * @param presentation The given presentation.
     */
    private fun applyMatrixLogoPanel(presentation: LCDPresentation) {
        val panelResourceId = R.layout.ui_matrix_logo;
        if (presentation.currResourceId == panelResourceId) {
            Log.d(TAG, "Matrix Logo already applied.")
            return
        }

        Log.d(TAG, "Applying matrix logo")
        presentation.updateLCDPanel(panelResourceId)
    }

    /**
     * Identifies if the workout is currently running (this is not paused, not stopped).
     */
    private val isWorkoutRunning: Boolean get() = workoutModel.isWorkoutRunning.value

    /**
     * Apply the correct workout controls panel for the given [presentation]. The following table
     * explains what will be applied by this method:
     *
     * | Machine Class Type | Left Side  | Right Side |
     * |--------------------|------------|------------|
     * | Ascent             | Incline    | Resistance |
     * | Treadmill          | Incline    | Speed      |
     * | Bike               | Resistance | Resistance |
     * | Other Class Types  | Pause/Stop | Resistance |
     *
     * @param presentation The given LCD presentation.
     */
    private fun applyWorkoutControlsPanel(presentation: LCDPresentation) {
        when (uiBaseModel.frameType) {
            FrameType.Treadmill -> {
                when (presentation.panelSide) {
                    PanelSide.LEFT -> configureWorkoutState(presentation, WorkoutPanelStat.Incline)
                    PanelSide.RIGHT -> configureWorkoutState(presentation, WorkoutPanelStat.Speed)
                }
            }
            FrameType.Ascent -> {
                when (presentation.panelSide) {
                    PanelSide.LEFT -> configureWorkoutState(presentation, WorkoutPanelStat.Incline)
                    PanelSide.RIGHT -> configureWorkoutState(presentation, WorkoutPanelStat.Resistance)
                }
            }
            FrameType.Bike -> {
                // CGC-16239 - users hitting pause accidentally, we are to show resistance both sides
                when (presentation.panelSide) {
                    PanelSide.LEFT, PanelSide.RIGHT -> configureWorkoutState(presentation, WorkoutPanelStat.Resistance)
                }
            }
            else -> {
                when (presentation.panelSide) {
                    PanelSide.LEFT -> configurePauseStop(presentation)
                    PanelSide.RIGHT -> configureWorkoutState(presentation, WorkoutPanelStat.Resistance)
                }
            }
        }
    }

    /**
     * Configure for the given workout state.
     *
     * fixme: this comment needs to be updated. It should also explain enable/disable.
     *
     * @param presentation The given presentation.
     * @param workoutPanelStat The workout panel stat being configured.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun configureWorkoutState(presentation: LCDPresentation, workoutPanelStat: WorkoutPanelStat) {
        val panelResourceId = if (presentation.panelSide == PanelSide.LEFT) R.layout.ui_workout_left else R.layout.ui_workout_right
        var initRequired = false

        // Only update the layout if it differs
        if (presentation.currResourceId != panelResourceId) {
            Log.d(TAG, "Applying the ${presentation.panelSide.name} workout controls panel layout")
            presentation.updateLCDPanel(panelResourceId)
            initRequired = true
        }

        presentation.currentView?.let { view ->
            val downButton: HapticAppCompatImageButton = view.findViewById(R.id.arrow_down)
            val upButton: HapticAppCompatImageButton = view.findViewById(R.id.arrow_up)

            /*
             * If the countdown is displaying or the workout is not running, the panels need to be
             * disabled.
             *
             * fixme: need to include any other conditionals here.
             */
            val enabled = !(displayingCountdownPopover || !isWorkoutRunning)

            // Initialize the icon as needed.
            if (initRequired) {
                (view.findViewById(R.id.icon) as ImageView).setImageResource(workoutPanelStat.iconId)

                // fixme: this likely requires a debounce.
                upButton.setOnTouchListener(HapticOnTouchListener(mastboardModel, presentation.panelSide) { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        upButton.setImageResource(R.drawable.ic_plus_grey)
                        when (workoutPanelStat) {
                            WorkoutPanelStat.Incline -> mastboardModel.inclineIncrement(true)
                            WorkoutPanelStat.Speed -> mastboardModel.speedIncrement(true)
                            WorkoutPanelStat.Resistance -> mastboardModel.resistanceIncrement(true)
                        }
                        return@HapticOnTouchListener true
                    } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        upButton.setImageResource(R.drawable.ic_plus_yellow)
                        when (workoutPanelStat) {
                            WorkoutPanelStat.Incline -> mastboardModel.inclineIncrement(false)
                            WorkoutPanelStat.Speed -> mastboardModel.speedIncrement(false)
                            WorkoutPanelStat.Resistance -> mastboardModel.resistanceIncrement(false)
                        }
                    }
                    return@HapticOnTouchListener false
                })

                // fixme: this likely requires a debounce.
                downButton.setOnTouchListener(HapticOnTouchListener(mastboardModel, presentation.panelSide) { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        downButton.setImageResource(R.drawable.ic_minus_grey)
                        when (workoutPanelStat) {
                            WorkoutPanelStat.Incline -> mastboardModel.inclineDecrement(true)
                            WorkoutPanelStat.Speed -> mastboardModel.speedDecrement(true)
                            WorkoutPanelStat.Resistance -> mastboardModel.resistanceDecrement(true)
                        }
                        /*DataMatrix.putInt(
                            HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(
                                workoutPanelStat.downKeyCode,
                                KeyPress.KeyState.KEY_DOWN
                            )
                        )*/
                        return@HapticOnTouchListener true
                    } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        downButton.setImageResource(R.drawable.ic_minus_yellow)
                        when (workoutPanelStat) {
                            WorkoutPanelStat.Incline -> mastboardModel.inclineDecrement(false)
                            WorkoutPanelStat.Speed -> mastboardModel.speedDecrement(false)
                            WorkoutPanelStat.Resistance -> mastboardModel.resistanceDecrement(false)
                        }
                        /*DataMatrix.putInt(
                            HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(
                                workoutPanelStat.downKeyCode,
                                KeyPress.KeyState.KEY_UP
                            )
                        )*/
                    }
                    return@HapticOnTouchListener false
                })
            }

            // Ensure properly enabled/disabled. This must always happen.
            upButton.isEnabled = enabled
            downButton.isEnabled = enabled
            upButton.setImageResource(if (enabled) R.drawable.ic_plus_yellow else R.drawable.ic_plus_grey)
            downButton.setImageResource(if (enabled) R.drawable.ic_minus_yellow else R.drawable.ic_minus_grey)
        } ?: Log.e(TAG, "Unable to configure the workout controls panel for @${workoutPanelStat.name} because the currentView is null.")
    }

    /**
     * fixme: Document this method. It should also explain enable/disable.
     *
     * @param presentation The given presentation.
     */
    private fun configurePauseStop(presentation: LCDPresentation) {
        val panelResourceId = R.layout.ui_workout_pause_stop_left // fixme: rename because this could be on either side.
        var initRequired = false

        // Only update the layout if it differs
        if (presentation.currResourceId != panelResourceId) {
            Log.d(TAG, "Applying the pause stop workout control panel")
            presentation.updateLCDPanel(panelResourceId)
            initRequired = true
        }

        // fixme: these buttons need to be enabled/disabled.
        presentation.currentView?.let { view ->
            val pauseButton: HapticView = view.findViewById(R.id.pause_button)
            val stopButton: HapticView = view.findViewById(R.id.stop_button)

            // Determine enable/disable state here. fixme: include any extra conditions.
            val enabled = !displayingCountdownPopover

            // Perform the initialization.
            if (initRequired) {
                // fixme: this needs to be debounced.
                pauseButton.setOnClickListener(HapticOnClickListener(mastboardModel, presentation.panelSide) {
                    // Only want to allow for pausing the workout since we are matching the physical buttons.
                    if (workoutModel.isWorkoutRunning.value)
                        workoutModel.pauseWorkout()
                })

                // fixme: this needs to be debounced.
                stopButton.setOnClickListener(HapticOnClickListener(mastboardModel, presentation.panelSide) {
                    // Match physical stop function bike / climbmill
                    workoutModel.stopWorkout()
                })
            }

            // Ensure properly enabled/disabled. This must always happen.
            pauseButton.isEnabled = enabled
            stopButton.isEnabled = enabled
        } ?: Log.e(TAG, "Unable to configure pause stop workout control because currentView is null.");
    }

    /**
     * todo: add comment here.
     */
    private fun applyGoButtonPanel(presentation: LCDPresentation) {
        if (!mastboardModel.isGoAllowed) {
            applyMatrixLogoPanel(presentation)
            return
        }
        val panelResourceId = R.layout.ui_goscreen_right
        if (presentation.currResourceId == panelResourceId) {
            Log.d(TAG, "Go Button Panel already applied.")
            return
        }

        Log.d(TAG, "Applying go button panel")
        presentation.updateLCDPanel(panelResourceId)
        presentation.currentView?.let { view ->
            val button: HapticAppCompatImageButton = view.findViewById(R.id.start_button)
            button.setOnClickListener(HapticOnClickListener(mastboardModel, presentation.panelSide) {
                mastboardModel.emulateGoPress()
            })
        }
    }

    /**
     * TODO: Apply this comment here.
     */
    private fun applyHomeButtonPanel(presentation: LCDPresentation) {
        val panelResourceId = R.layout.ui_summary_home
        if (presentation.currResourceId == panelResourceId) {
            Log.d(TAG, "Home Button Panel already applied.")
            return
        }

        Log.d(TAG, "Applying home button panel")
        presentation.updateLCDPanel(panelResourceId)
        presentation.currentView?.let { view ->
            val button: HapticView = view.findViewById(R.id.home_button)
            button.setOnClickListener(HapticOnClickListener(mastboardModel, presentation.panelSide) {
                navigationModel.launchSplashScreen()
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyLCDDebuggingPanel(presentation: LCDPresentation) {
        val panelResourceId = R.layout.handgrip_haptic_debug_testing
        if (presentation.currResourceId == panelResourceId) {
            Log.d(TAG, "LCD Debugging Panel already applied.")
            return
        }

        Log.d(TAG, "Applying lcd debugging panel to: ${presentation.panelSide.name}")
        presentation.updateLCDPanel(panelResourceId)
        presentation.currentView?.let { view ->
            val gridView: GridView = view.findViewById(R.id.grid_view)
            val touchArea: View = view.findViewById(R.id.touch_area)

            gridView.adapter = GridViewAdapter(context)
            touchArea.setOnTouchListener { _, event ->
                val x: Float = event.x
                val y: Float = event.y

                if (event.action == MotionEvent.ACTION_DOWN) {
                    val locationString = "Left: Down Touch X: $x Y: $y";
                    Toast.makeText(context, locationString, Toast.LENGTH_LONG).show() //NON-NLS
                    return@setOnTouchListener false
                } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                    val locationString = "Left: Up Touch X: $x Y: $y"
                    Toast.makeText(context, locationString, Toast.LENGTH_LONG).show() //NON-NLS
                }
                return@setOnTouchListener false
            }

            gridView.setOnItemClickListener { _, _, _, _ -> }
            gridView.isVerticalScrollBarEnabled = false
        }
    }

    /**
     * Show/Hide the debug panels.
     *
     * @param enable `true` to show the debug panels, else `false`.
     */
    fun enableDebugPanels(enable: Boolean) {
        if (enable) {
            Log.d(TAG, "Showing the debug views")
            mLeftPanelPresentation.showDebugView()
            mRightPanelPresentation.showDebugView()
        } else {
            Log.d(TAG, "Hiding the debug views")
            mLeftPanelPresentation.hideDebugView()
            mRightPanelPresentation.hideDebugView()
        }
    }

    /**
     * Publish updated touch data to the LCD panels. The touch data is only passed through to the
     * LCD presentations if it's not `null`.
     *
     * @param touchDataLeft The touch data for the left LCD.
     * @param touchDataRight The touch data for the right LCD.
     * @return true if there was a touch event, false otherwise
     */
    fun updateLastTouch(touchDataLeft: TouchData?, touchDataRight: TouchData?) : Boolean {
        var touchActivity = false
        if (touchDataLeft != null) {
            touchActivity = mLeftPanelPresentation.updateTouch(touchDataLeft)
        }
        if (touchDataRight != null) {
            touchActivity = touchActivity or mRightPanelPresentation.updateTouch(touchDataRight)
        }
        return touchActivity
    }

    private fun outputCurrentStates() {
        Log.d(TAG, """updateToggleDisplays called:
            |displayingLCDDebuggingPanels:   $displayingLCDDebuggingPanels
            |displayingMatrixDashboard:      $displayingMatrixDashboard
            |displayingConnectToWatch:       $displayingConnectToWatch
            |displayingFeaturedWorkoutSetup: $displayingFeaturedWorkoutSetup
            |displayingCountdownPopover:     $displayingCountdownPopover
            |currentState:                   $currentState
        """.trimMargin())
    }
}
