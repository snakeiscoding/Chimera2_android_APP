package com.jht.chimera.service.workout;

import android.os.Parcelable;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workout.workoutlist.WorkoutEntry;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScript;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScripts;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.chimera.test.fixtures.DataMatrixTestAction;
import com.jht.chimera.test.fixtures.DataMatrixTestEvent;
import com.jht.chimera.test.fixtures.DataMatrixTestEventParcelable;
import com.jht.chimera.test.fixtures.DataMatrixTestEventStringList;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * WARNING ----- This seems to only work for one workout run.  Do not have multiple workout runs within a
 *               test function.  The UI gets in the way and throws things off.  There may be some other
 *               bug there or way around that, but haven't dug in too far.
 * This helps setup tests for workout controller scripts.  Use this to load a script and then check it.
 * This works primarily by allowing the test code to check workout states and inject workout inputs
 * at a given time in the workout.  This way we can run it fast and there is no need to have a bunch of
 * callbacks.  So basically to use this do the following:
 *      Load the script.
 *      Validate inputs and outputs exist. (assertInputExists, assertOutputExists)
 *      Setup checks and injections.    (addCheck, injectInput)
 *      Setup workout parameters        (udpateInput)
 *      Start workout.                  This waits for the workout to complete.
 *
 *      Note if a check fails in the middle of the workout we stop and print a message to the user with
 *      the failure.
 */
class WorkoutControllerServiceTestFixture extends BaseTestFixture {

    private final List<InitialCondition> initialConditions = new ArrayList<>();

    // If a check fails we store it here to better tell the user.
    private String failedCheck = null;

    private final DataMatrix dataMatrix;


    /**
     * Load the database and initialize the controller.
     */
    public WorkoutControllerServiceTestFixture(DataMatrix dataMatrix) {
        this.dataMatrix = dataMatrix;
        startServices(dataMatrix);

        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT, 10f);
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL, true);
    }

    public void runWorkout(int workoutId, String appId) {
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, ms -> {
            if(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
                failedCheck = runChecks(ms);
                if(failedCheck != null) {
                    DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
                }
                
            }
        });

        JHTNotification workoutEndNotification = new JHTNotification();

        WorkoutScripts workoutScripts = new WorkoutScripts();
        WorkoutScript script = workoutScripts.getWorkoutScript(20);
        Assert.assertNotNull(script);

        WorkoutEntry entry = new WorkoutEntry(workoutId, appId, script.scriptString);
        DataMatrix.putParcelable(WorkoutKeys.ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE, entry);

        // Sort the events so we can easily check them.

        WorkoutLaunchData workoutLaunchData = new WorkoutLaunchData(initialConditions, new WorkoutSettingOverrides(), false);
        DataMatrix.putParcelable(WorkoutKeys.ACTION_LAUNCH_WORKOUT_PARCELABLE, workoutLaunchData);
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, state -> {
            if(state == WorkoutKeys.Constants.RUN_STATE_STOP) {
                workoutEndNotification.setNotification();
            }
        }, false);

        Assert.assertTrue(workoutEndNotification.waitForNotification(5 * 60 * 1000));
        dataMatrix.finish();

        // Check for failures.
        if(failedCheck != null) {
            Assert.fail(failedCheck);
        }
        runFinalCheck();
    }


    /**
     * Add a workout check.
     * @param timing        Time of the check.
     * @param variable      Variable to check.
     * @param value         Expected value.
     */
    public void addCheck(int timing, Object variable, float value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, float value, float tolerance) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value, tolerance));
    }
    public void addCheck(int timing, Object variable, int value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, long value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, double value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, double value, float tolerance) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value, tolerance));
    }
    public void addCheck(int timing, Object variable, String value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, byte[] value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, boolean value) {
        addCheck(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, List<String> value) {
        addCheck(new DataMatrixTestEventStringList(timing, variable, value));
    }
    public void addCheck(int timing, Object variable, Parcelable value) {
        addCheck(new DataMatrixTestEventParcelable<>(timing, variable, value));
    }

    /**
     * Update a workout input.
     *
     * @param timing        The time of the injection.
     * @param variable      The variable to update.
     * @param value         The value to update the variable to.
     */
    public void injectInput(int timing, Object variable, float value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, float value, float tolerance) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value, tolerance));
    }
    public void injectInput(int timing, Object variable, int value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, long value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, double value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, double value, float tolerance) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value, tolerance));
    }
    public void injectInput(int timing, Object variable, String value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, byte[] value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, Parcelable value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, boolean value) {
        injectInput(new DataMatrixTestEvent<>(timing, variable, value));
    }
    public void injectInput(int timing, Object variable, List<String> value) {
        injectInput(new DataMatrixTestEventStringList(timing, variable, value));
    }
    public void injectInput(int timing, Object variable) {
        injectInput(new DataMatrixTestAction(timing, variable));
    }

    /**
     */
    public void addInitialCondition(String key, float value) {
        initialConditions.add(new InitialCondition(key, value));
    }

    @Override
    public void close() {
        super.close();
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT, 1f);

    }

}
