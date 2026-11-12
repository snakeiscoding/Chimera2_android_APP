package com.jht.chimera.ui.ifit;

import android.graphics.Point;

import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Locale;


/**
 * Here we want to make sure to start the system.  Sometimes we may want to set the frame too.
 * This class helps simplify the tests.
 */
public class IFitTestFixture extends SystemTestFixture {

    private IFitTestSettings testSettings;

    IFitTestFixture(FrameConfigEnum frame) {
        super(frame);
        init(new IFitTestSettings());
    }

    IFitTestFixture(FrameConfigEnum frame, int getStartedSeconds, int guestWorkoutSelectionSeconds, int workoutSelectionDelaySeconds, int startWorkoutDelaySeconds, int acceptDelaySeconds, int endWarmupTimeSeconds, int workoutTimeSeconds) {
        super(frame);
        init(new IFitTestSettings(getStartedSeconds, guestWorkoutSelectionSeconds, workoutSelectionDelaySeconds, startWorkoutDelaySeconds, acceptDelaySeconds, endWarmupTimeSeconds, workoutTimeSeconds));
    }

    private void init(IFitTestSettings testSettings) {
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_BOOL, true);
        DataMatrix.putBoolean(SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL, true);
        DataMatrix.putString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_ATHENA_STRING, String.format(Locale.ENGLISH, "%d,%d,%d", WorkoutIds.IFIT, WorkoutIds.IFIT, WorkoutIds.IFIT));
        DataMatrix.putString(SettingKeys.Workout.DATA_FEATURED_WORKOUT_NON_ATHENA_STRING, String.format(Locale.ENGLISH, "%d,%d,%d", WorkoutIds.IFIT, WorkoutIds.IFIT, WorkoutIds.IFIT));

        this.testSettings = testSettings;
    }

    void clickGetStarted() {
        click(
                new UITestFixture.Button(
                        testSettings.getStartedDelaySeconds(),
                        new Point(
                                getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitWorkoutGetStartedX),
                                getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitWorkoutGetStartedY)
                        )
                ));
    }

    void clickStartWorkout() {
        click(
            new UITestFixture.Button(
                testSettings.startWorkoutDelaySeconds(),
                new Point(
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitStartWorkoutX),
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitStartWorkoutY)
                )
        ));
    }


    void clickIfitGuestWorkouts() {
        click(
                new UITestFixture.Button(
                        testSettings.guestWorkoutSelection(),
                        new Point(
                                getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitGuestWorkoutX),
                                getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitGuestWorkoutY)
                        )
                ));
    }

    void clickSelectWorkout() {
        click(
            new UITestFixture.Button(
                testSettings.workoutSelectionDelaySeconds(),
                new Point(
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitWorkoutSelectionX),
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitWorkoutSelectionY)
                )
        ));
    }

    void clickAcceptWorkout() {
        click(
            new UITestFixture.Button(
                testSettings.acceptDelaySeconds(),
                new Point(
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitAccepWorkoutX),
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitAccepWorkoutY)
                )
            ));
    }

    void clickEndWarmup() {
        click(
            new UITestFixture.Button(
                testSettings.endWarmupTimeSeconds(),
                new Point(
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitEndWarmupX),
                    getTestContext().getResources().getInteger(com.jht.chimera.test.R.integer.ifitEndWarmupY)
                )
            ));
    }

    void waitForWorkoutEnd() {
        if(testSettings.endWarmupTimeSeconds() <= 0) {
            sleepIgnoreInterruption((testSettings.workoutTimeSeconds() + 3 * 60) * 1000);
        }
        else {
            sleepIgnoreInterruption(testSettings.workoutTimeSeconds() * 1000);
        }
    }

}
