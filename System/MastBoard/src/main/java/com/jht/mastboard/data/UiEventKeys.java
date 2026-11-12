package com.jht.mastboard.data;

public class UiEventKeys {
    public enum Key {
        DATA_UI_EVENT_STRING
    }

    public enum EVENT {
        /**
         * Publish when the splash screen is being displayed.
         */
        SPLASH_SCREEN,
        /**
         * Publish when the screensaver is displayed.
         */
        SCREENSAVER_SCREEN,
        /**
         * Publish when displaying the welcome screen.
         */
        WELCOME_SCREEN,
        /**
         * Publish when displaying the go screen.
         */
        GO_SCREEN,
        /**
         * Publish when displaying the training exercises screen.
         */
        TRAINING_EXERCISES_SCREEN,
        /**
         * Publish when displaying the custom workouts screen.
         */
        CUSTOM_WORKOUTS_SCREEN,
        OPEN_FEATURE_WORKOUT_SETUP,
        CLOSE_FEATURE_WORKOUT_SETUP,
        /**
         * Publish when opening matrix dashboard popup.
         */
        OPEN_MATRIX_DASHBOARD,
        /**
         * Publish when closing matrix dashboard popup.
         */
        CLOSE_MATRIX_DASHBOARD,
        /**
         * Publish when opening the connect to watch dialog.
         */
        OPEN_CONNECT_TO_WATCH,
        /**
         * Publish when closing the connect to watch dialog.
         */
        CLOSE_CONNECT_TO_WATCH,
        /**
         * Publish when displaying the login/register page.
         */
        LOGIN_REGISTER,
        /**
         * Publish when the countdown popover is being displayed.
         */
        OPEN_COUNTDOWN_POPOVER,
        /**
         * Publish when the countdown popover is being no longer displayed.
         */
        CLOSE_COUNTDOWN_POPOVER,
        /**
         * Publish when you are in a workout.
         */
        WORKOUT,
        /**
         * The E-Stop needs to be shown.
         */
        SHOW_ESTOP,
        /**
         * The E-Stop is being closed.
         */
        CLOSE_ESTOP,
        /**
         * TODO: Determine if this is when going to MES?
         *
         * This is shown when MES is displayed.
         *
         * fixme: I need to rename this to something easier.
         */
        MES,
        /**
         * Publish when displaying First Time Setup.
         */
        FIRST_TIME_SETUP,
        /**
         * Publish when displaying Workout Summary.
         */
        WORKOUT_SUMMARY,
        /**
         * Publish when displaying EGYM Loading.
         */
        EGYM_LOADING,
        /**
         * Publish when displaying YMCA360 Loading.
         */
        YMCA360_LOADING,
        /**
         * Publish when displaying iFit loading.
         */
        IFIT_LOADING,
        OUT_OF_ORDER_SCREEN,
        //==========================================================================================
        // FIXME: I think these can be removed because I don't understand why they exist.
        //==========================================================================================
        /**
         * FIXME: This is being publishing, do I need to do anything about this?
         */
        UPDATE
    }
    static EVENT[] allEvents = EVENT.values();

    public static EVENT getEventFromString(String eventType) {
        if (eventType == null) return null;
        for (EVENT type : allEvents) {
            if (type.toString().equalsIgnoreCase(eventType)) {
                return type;
            }
        }
        return null;
    }
}
