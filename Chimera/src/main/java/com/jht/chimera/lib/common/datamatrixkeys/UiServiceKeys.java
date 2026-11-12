package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum UiServiceKeys {
    // Show/hide all panels depending on boolean sent
    DATA_WORKOUT_PANEL_STATE_INT(new DataDefault<>(WorkoutPanelState.IDLE)), // see UiServiceKeys.WorkoutPanelState for values
    ACTION_SET_WORKOUT_PANEL_STATE_INT, // see UiServiceKeys.WorkoutPanelState for values (USE THIS TO SET OUTSIDE OF SERVICE)
    ACTION_SET_WORKOUT_PANEL_BEHAVIOR_PARCELABLE, // see PanelBehavior class, used with DATA_WORKOUT_PANEL_STATE_INT

    ACTION_LAUNCH_UI_PARCELABLE, // see LaunchScreenData, used for controlled launching of UI
    ACTION_LAUNCH_APP_PARCELABLE, // see LaunchAppData, used for controlled launching of Apps (1st and 3rd party)
    EVENT_LAUNCHING_APP_UUID_STRING, // event triggered with app uuid before launching app
    EVENT_UI_RESUMED_PARCELABLE, // Passes Intent of resumed activity
    EVENT_UI_PAUSED_PARCELABLE, // Passes Intent of paused activity
    ACTION_LAUNCH_UI_PANEL_STRING, //displays the UI panel based on the panel id string
    EVENT_APP_BAR_REFRESH_BTN_CLICK,
    ACTION_START_WORKOUT_UI_READY_BOOL, // true if success, false if UI failed (should it be parcelable with transaction ID?)
    ACTION_SHOW_DIGITAL_SAFETY_PIN_PROMPT_BOOL, // event or bool? do we need a hide action?
    DATA_UI_OVERLAY_VIEWPORT_PARCELABLE(new ParcelableDataDefault()), // android.graphics.Rect representing the screen area not covered by overlays or empty if no overlays (only panel management should set this)

    /**
     *  The overlay panel is just a black screen that covers whatever the activity is doing.
     *  It’s useful to hide poor transitions.  - Chris Foyer 2022
     **/
    ACTION_SHOW_OVERLAY_PANEL_MS_INT,
    ACTION_SHOW_OVERLAY_PANEL_BOOL(new DataDefault<>(false)),

    //*** UI Service specific ***//

    // This key is the currently selected distance unit. The distance unit determines what the other units do
    DATA_DISPLAY_METRIC_VALUES_BOOL(new DataDefault<>(false)),

    ACTION_LEFT_KEYPAD_CONFIRMED_VALUE_FLOAT,
    ACTION_RIGHT_KEYPAD_CONFIRMED_VALUE_FLOAT,

    ACTION_SHOW_MESSAGE_PARCELABLE,
    ACTION_REMOVE_MESSAGE_BY_TAG_STRING,

    DATA_PAUSE_TIME_REMAINING_INT(new DataDefault<>(0)),
    DATA_PAUSE_TIME_INT(new DataDefault<>(0)),
    DATA_PAUSE_TIMER_STRING(new DataDefault<>("")),

    // Tracks the current selected index of the stats so we can match them between VA and the UI Service.
    DATA_COL_1_STAT_SELECTED_ORDER_INT(new DataDefault<>(0)),
    DATA_COL_2_STAT_SELECTED_ORDER_INT(new DataDefault<>(0)),
    DATA_COL_3_STAT_SELECTED_ORDER_INT(new DataDefault<>(0)),
    DATA_COL_4_STAT_SELECTED_ORDER_INT(new DataDefault<>(0)),
    DATA_COL_5_STAT_SELECTED_ORDER_INT(new DataDefault<>(0)),

    ACTION_DISABLE_SAFETY_SCREENS_BOOL,

    ACTION_SHOW_TREAD_SENSE_EVENT,
    ACTION_HIDE_TREAD_SENSE_EVENT,

    EVENT_HIDE_KEYBOARD,
    EVENT_STOP_SIDE_BAR_INCREMENT, // Stops the side bar from incrementing after lock screen or audio panel is displayed

    DATA_HOME_BUTTON_ENABLED_BOOL(new DataDefault<>(true)),

    /**
     * allows the app that is currently running to decide which panels stick.  e.g. The simple app
     * has top, left, and right panels and normally when you press the pause button all of the
     * panels pop up, but we only want 3/4.
     * Basically makes setWorkoutPanelSet(int state) in WorkoutPanelManager.java stay the same
     **/
    DATA_APP_HANDLES_PAUSE_BOOL(new DataDefault<>(false)),
    DATA_SHOW_ERROR_PANEL_BOOL(new DataDefault<>(false)),

    // Adds a panel to the UI service. Called from the MatrixUiServiceModel.
    ACTION_ADD_UI_PANEL_PARCELABLE,
    ACTION_SEND_PANEL_DATA_PARCELABLE,
    DATA_IFIT_WORKOUT_STARTED_BOOL(new DataDefault<>(false)),

    DATA_FEATURED_WORKOUT_1_INT(new DataDefault<>(-1)),
    DATA_FEATURED_WORKOUT_2_INT(new DataDefault<>(-1)),
    DATA_FEATURED_WORKOUT_3_INT(new DataDefault<>(-1)),
    DATA_FEATURED_WORKOUT_FULLSCREEN_BOOL(new DataDefault<>(false)),

    EVENT_FEATURED_WORKOUT_PRESS_INT,
    ACTION_SHOW_DEBUG_PANEL_EVENT;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (UiServiceKeys s : UiServiceKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    UiServiceKeys(){
        this.DataDefault = null;
    }

    UiServiceKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    /**
     * This tracks the common phases or states that workout panels go through for an app.
     * This state is applied to a panel behavior to display the correct set of panels for an app.
     */
    public static final class WorkoutPanelState {
        public static final int IDLE = 1; // panel set when workout not active
        public static final int REGULAR = 2; // panel set when workout is active, but not fullscreen
        public static final int FULLSCREEN = 3; // panel set when workout is active and fullscreen
        public static final int WORKOUT_SUMMARY = 4; // panel set when workout not active
    }

}
