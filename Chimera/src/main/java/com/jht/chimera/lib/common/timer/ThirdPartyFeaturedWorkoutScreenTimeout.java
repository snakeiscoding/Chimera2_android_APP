package com.jht.chimera.lib.common.timer;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Class to timeout third party applications such as iFit when in the workout selection or workout
 * summary state.
 */
public class ThirdPartyFeaturedWorkoutScreenTimeout {
    private enum SYSTEM_STATE {
        WORKOUT_SELECTION,
        WORKOUT,
        WORKOUT_SUMMARY,
    }
    private final IdleTimerManagement idleTimerManagement = new IdleTimerManagement();

    private SYSTEM_STATE systemState = SYSTEM_STATE.WORKOUT_SELECTION;
    // the post workout survey sits between workout stop and the summary, so the summary timeout waits for it
    private boolean heldForSurvey = false;
    private String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);

    public void reset() {
        heldForSurvey = false;
        systemState = SYSTEM_STATE.WORKOUT_SELECTION;
        updateTimerState();
    }

    /**
     * Handles any updates to the workout state from the system and applies that update to our
     * time out logic.
     * Needs to hook into an adapter for WorkoutKeys.DATA_RUN_STATE_INT
     * @param runState - the new workout state to run our logic against
     */
    public void processWorkoutRunState(int runState) {
        if(runState != WorkoutKeys.Constants.RUN_STATE_STOP) {
            if (systemState != SYSTEM_STATE.WORKOUT) heldForSurvey = false; // a new workout drops any leftover hold
            systemState = SYSTEM_STATE.WORKOUT;
            updateTimerState();
        }
        else if(systemState == SYSTEM_STATE.WORKOUT) {
            systemState = SYSTEM_STATE.WORKOUT_SUMMARY;
            updateTimerState();
        }
    }

    /**
     * Handles when the workoutAppUUID is updated by the system.  (e.g. iFit is launched, we need
     * to know that we are currently using ifit and update the timeout logic)
     * Needs to hook into an adapter for WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING
     * @param workoutAppUUID - the new appId to update our class variable with
     */
    public void processWorkoutAppUUID(String workoutAppUUID) {
        appId = workoutAppUUID;
        updateTimerState();
    }

    /**
     * Stops the post workout timeout while the post workout survey shows, so idling on the survey never
     * reaches splash. Holds until {@link #restartAfterSurvey()}, {@link #reset()} or a new workout.
     */
    public void holdForSurvey() {
        heldForSurvey = true;
        updateTimerState();
    }

    /** Releases the survey hold and gives the summary a fresh, full timeout */
    public void restartAfterSurvey() {
        heldForSurvey = false;
        updateTimerState();
    }

    private void updateTimerState() {
        // Full integrations own their entire user flow including post-workout, so never force them back to splash
        AppInfo appInfo = AppManager.getInstance().getApp(appId);
        boolean isFullIntegration = appInfo != null && appInfo.category == AppInfo.Category.FullIntegration;

        boolean inThirdPartyWorkoutSelection = systemState.equals(SYSTEM_STATE.WORKOUT_SELECTION) &&
                (AppIds.IFIT_UUID.equals(appId) || AppIds.YMCA_360_FEATURED_UUID.equals(appId));

        boolean inSummary = systemState.equals(SYSTEM_STATE.WORKOUT_SUMMARY) && !heldForSurvey;

        if (!isFullIntegration && (inThirdPartyWorkoutSelection || inSummary)) {
            startIdleTimer();
        } else {
            idleTimerManagement.clearTimers();
        }
    }

    private void startIdleTimer() {
        if (ProductConfig.isParker()) return; // until strength workouts run as workouts, parker can't timeout ifit
        int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);

        idleTimerManagement.clearTimers();
        if (screenTimeout > 0) {
            idleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                LaunchAppUtil.launchSplashScreen();
                idleTimerManagement.clearTimers();
            }));
        }
        idleTimerManagement.monitorTimers();
    }

    /**
     * This will handle logic for timing out 3rd party applications when a passive workout
     * has been selected by the user. (e.g. iFit strength workouts)
     * Needs to hook into an adapter for WorkoutKeys.ACTION_PASSIVE_WORKOUT_BOOL
     *
     * @param starting - denotes if a passive workout is starting (true) or stopping (false)
     */
    public void handlePassiveWorkouts(boolean starting) {
        // this would usually be triggered while view is third party and panels are IDLE.
        if (AppIds.IFIT_UUID.equals(appId)) {
            if (starting) {
                idleTimerManagement.clearTimers();
            } else {
                startIdleTimer();
            }
        }
    }
}
