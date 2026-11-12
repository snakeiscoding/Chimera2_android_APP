@file:JvmName("PanelControllerState")
package com.jht.mastboard

/**
 * All states that need to be supported by the LCD controller.
 *
 * TODO: Continue documenting these
 */
enum class PanelControllerState {
    /**
     * The splash screen is currently being displayed.
     */
    DisplayingSplashScreen,

    /**
     * The screensaver is currently being displayed.
     */
    InScreensaver,

    /**
     * First time setup is being displayed.
     */
    InFirstTimeSetup,

    /**
     * Displaying either the login screen or the register screen.
     */
    DisplayingLoginRegisterScreen,

    /**
     * Displaying the screen where Guest, Login, Register are displayed.
     *
     * fixme: What is displayed when registering...
     */
    DisplayingWelcomeScreen,
    DisplayingGoScreen,
    InWorkout,
    // fixme: Their needs to be a state for when the workout is paused.
    IFITLoading,
    EGYMLoading,
    YMCA360Loading,
    DisplayingTrainingExercisesScreen,
    DisplayingCustomWorkoutScreen,
    DisplayingOutOfOrder,
    DisplayingMES,

    /**
     * Displaying the workout summary.
     */
    DisplayingWorkoutSummary
}
