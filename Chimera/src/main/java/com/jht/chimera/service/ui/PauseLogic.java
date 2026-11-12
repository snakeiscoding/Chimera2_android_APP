package com.jht.chimera.service.ui;

import android.os.CountDownTimer;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.datamatrixlib.DataMatrix;

/**
 * This was buried in the right panel. Need to use it in a new panel.
 * Maybe should be in workout controller service? Can we also reduce DM keys?
 */
public class PauseLogic {
    private int PAUSE_TIMER_COUNTDOWN_MS = 300000; // 5 minutes
    private CountDownTimer mCountDownTimer;
    
    /**
     * Method that listens for workout state changes and then runs logic to start/stop
     * the pause timer based on that state.
     * Needs to hook into an adapter for WorkoutKeys.DATA_RUN_STATE_INT
     */
    public void processWorkoutRunState(int workoutRunState) {
        if (workoutRunState == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
            startPauseTimer();
        } else if (workoutRunState == WorkoutKeys.Constants.RUN_STATE_STOP || workoutRunState == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
            resetPauseTimer();
        }
        // I guess we ignore the state of RUN_STATE_SWITCHING
    }

    private void startPauseTimer() {
        resetPauseTimer();
        PAUSE_TIMER_COUNTDOWN_MS = DataMatrix.getInt(SettingKeys.Workout.DATA_PAUSE_TIME_MIN_INT) * 60 * 1000;

        // gymkit pause time is 2 minute
        if (DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != HALKeys.STATUS_GYM_KIT_DISCONNECTED) {
            PAUSE_TIMER_COUNTDOWN_MS = 2 * 60 * 1000;
        }

        if (AppIds.IFIT_UUID.equals(DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING))) {
            // ifit pause timer is hardcoded the time is dependent upon the ifit version / market.
            PAUSE_TIMER_COUNTDOWN_MS = (ProductConfig.isRetail() ? 10 : 2) * 60 * 1000;
        }
        DataMatrix.putInt(UiServiceKeys.DATA_PAUSE_TIME_INT, PAUSE_TIMER_COUNTDOWN_MS / 1000);

        mCountDownTimer = new CountDownTimer(PAUSE_TIMER_COUNTDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String timeRemaining = TimeFormat.formatTime(millisUntilFinished);
                DataMatrix.putInt(UiServiceKeys.DATA_PAUSE_TIME_REMAINING_INT, (int)millisUntilFinished / 1000);
                DataMatrix.putString(UiServiceKeys.DATA_PAUSE_TIMER_STRING, timeRemaining);
            }

            @Override
            public void onFinish() {
                pauseFinish();
            }
        };
        mCountDownTimer.start();
    }

    private void resetPauseTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
            DataMatrix.putString(UiServiceKeys.DATA_PAUSE_TIMER_STRING, TimeFormat.formatTime(PAUSE_TIMER_COUNTDOWN_MS));
        }
    }

    protected void pauseFinish() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
    }
}
