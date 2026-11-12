package com.jht.chimera.service.workout;

import android.util.Log;

import androidx.annotation.Nullable;

import com.amazonaws.util.IOUtils;
import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.chimera.test.fixtures.TestEventBase;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutcontrollerlib.parser.ScriptException;
import com.jht.workoutcontrollerlib.workoutcardio.CardioDefinitionInfo;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutControllerCardio;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
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
class WorkoutControllerTestFixture extends BaseTestFixture {

    /**
     * Used to create injection and test events.  In general we need the time of the event, the
     * variable we are checking or modifying, and the expected value or value to set.  We also
     * allow for a configurable tolerance since we are working with floating point numbers.
     */
    private static class TestEvent extends TestEventBase<Float> {
        private static final String TAG = "TestEvent";
        private final WorkoutControllerCardio controller;
        private final HashMap<String, Float> exportedValues;

        /**
         * Create an event.
         *
         * @param timing    Time of the event.
         * @param variable  Variable to check or modify.
         * @param value     Value to set or compare against.
         */
        public TestEvent(WorkoutControllerCardio controller, HashMap<String, Float> exportedValues, int timing, Object variable, float value) {
            super(timing, variable, value, 0.0f);
            this.controller = controller;
            this.exportedValues = exportedValues;
        }

        /**
         * Create an event.
         *
         * @param timing    Time of the event.
         * @param variable  Variable to check or modify.
         * @param value     Value to set or compare against.
         * @param tolerance Used to compare floating point values.
         */
        public TestEvent(WorkoutControllerCardio controller, HashMap<String, Float> exportedValues, int timing, Object variable, float value, float tolerance) {
            super(timing, variable, value, tolerance);
            this.controller = controller;
            this.exportedValues = exportedValues;
        }
        @Override
        public void inject() {
            Log.d(TAG, "inject event " + timing + " " + variable.toString() + " " + value);
            controller.updateAndValidateValue(variable.toString(), value);
        }

        @Override
        protected @Nullable
        Object getValue() {
            return exportedValues.get(variable.toString());
        }
    }

    // The current workout output state.
    private final HashMap<String, Float> exportedValues = new HashMap<>();

    // The workout controller.
    private final WorkoutControllerCardio workoutController;

    // Used to notify that the workout is complete.
    private final JHTNotification workoutEnded = new JHTNotification();

    // List of script output variables that we must feed right back to the script.  For example,
    // the constant watts outputs resistance and has that as an input so we need to update the input
    // whenever the script changes the output.  The normal Chimera code does this already.
    private final HashMap<String, String> translatedValues = new HashMap<>();

    // The loaded workout.
    private CardioDefinitionInfo definition;

    // If a check fails we store it here to better tell the user.
    private String failedEvent = null;

    /**
     * Load the database and initialize the controller.
     */
    public WorkoutControllerTestFixture() {
        exportedValues.put("PREVENT_USER_CHANGES", 0f);
        exportedValues.put("PREVENT_USER_GOAL_CHANGES", 0f);
        exportedValues.put("ALLOW_PAUSE", 1f);

        workoutController = new WorkoutControllerCardio(new WorkoutControllerCardio.ICardioWorkoutController() {
            @Override
            public void valueChangedEvent(String s, Float aFloat) {
                //Log.d("workout_test", "value changed event " + s + " " + aFloat);
                // Update the output.
                exportedValues.put(s, aFloat);

                // Check for workout state changes.
                if (s.equals(WorkoutController.WORKOUT_STATE)) {
                    if (aFloat == WorkoutController.WORKOUT_STATE_STOPPED) {
                        workoutEnded.setNotification();
                    }
                } else {
                    // See if we need to reroute the variable output back to the inputs.
                    for (Map.Entry<String, String> value : translatedValues.entrySet()) {
                        if (s.equals(value.getKey())) {
                            workoutController.updateAndValidateValue(value.getValue(), aFloat);
                        }
                    }
                }

                if(s.equals(WorkoutController.WORKOUT_TIME_MS)) {
                    failedEvent = runChecks(aFloat.intValue());
                    if(failedEvent != null) {
                        workoutEnded.setNotification();
                    }
                }
            }

            @Override
            public void allowUserGoalChangesUpdated(boolean b) {
                exportedValues.put("PREVENT_USER_GOAL_CHANGES", b ? 0f : 1f);
            }

            @Override
            public void allowUserChangesUpdated(boolean b) {
                exportedValues.put("PREVENT_USER_CHANGES", b ? 0f : 1f);
            }

            @Override
            public void allowPauseUpdated(boolean b) {
                exportedValues.put("ALLOW_PAUSE", b ? 1f : 0f);
            }

            @Override
            public void scriptExceptionOccurred() {

            }

            @Override
            public void updateComplete() {

            }

            @Override
            public void goalProgressUpdated(float goal, float goalProgress, float goalRemaining) {

            }

            @Override
            public void intervalGoalProgressUpdated(float goal, float goalProgress, float goalRemaining) {

            }
        });
    }

    /**
     * Add a workout check.
     * @param timing        Time of the check.
     * @param variable      Variable to check.
     * @param value         Expected value.
     */
    public void addCheck(int timing, String variable, float value) {
        addCheck(new TestEvent(workoutController, exportedValues, timing, variable, value));
    }

    /**
     * Add a workout check.
     * @param timing        Time of the check.
     * @param variable      Variable to check.
     * @param value         Expected value.
     * @param tolerance     Tolerance for floating point check.
     */
    public void addCheck(int timing, String variable, float value, float tolerance) {
        addCheck(new TestEvent(workoutController, exportedValues, timing, variable, value, tolerance));
    }

    /**
     * Update a workout input.
     *
     * @param timing        The time of the injection.
     * @param variable      The variable to update.
     * @param value         The value to update the variable to.
     */
    public void injectInput(int timing, String variable, float value) {
        injectInput(new TestEvent(workoutController, exportedValues, timing, variable, value));
    }

    /**
     * Load the script.
     *
     * @param scriptId           The workout to load.
     * @throws ScriptException      Thrown if there is an error loading the script.
     */
    public void loadScript(int scriptId) throws ScriptException {

        String script = getScriptString(scriptId);
        // Log.d("workout_test_script", "script: " + script);
        Assert.assertNotNull(script);
        definition = WorkoutControllerCardio.loadScript(script);
    }

    /**
     * Gets the script from the resources
     * @param id The ID for the script file
     * @return The script from the resources or null
     */
    private String getScriptString(int id) {
        try {
            InputStream inputStream = ContextUtil.getApplicationContext().getResources().openRawResource(id);
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads the script defined by the passed in string.
     *
     * @param workoutScriptString The workout to load
     * @throws ScriptException Thrown if there is an error loading the script
     */
    public void loadScriptString(String workoutScriptString) throws ScriptException {
        Assert.assertNotNull(workoutScriptString);
        definition = WorkoutControllerCardio.loadScript(workoutScriptString);
    }

    /**
     * Make sure the workout input exists.  Smoke test the workout.
     *
     * @param input     The input to check.
     */
    public void assertHasInput(String input) {
        Assert.assertTrue(definition.getInputs().contains(input));
    }

    /**
     * Make sure the workout output exists.  Smoke test the workout.
     *
     * @param output     The output to check.
     */
    public void assertHasOutput(String output) {
        Assert.assertTrue(definition.getExports().contains(output));
    }

    /**
     * Run the workout, wait for it to complete, check to see if there were failures.
     *
     * @throws ScriptException  Thrown if there is an error when parsing the script.
     */
    public void runWorkout() throws ScriptException {
        startTest();

        // Run the workout
        workoutController.changeExecutionSpeedMultiplier(100);
        workoutController.startWorkout(definition);
        workoutEnded.waitForNotification();


        // Check for failures.
        Assert.assertNull(failedEvent, failedEvent);
        runFinalCheck();

    }

    /**
     * Translate an output to the input.
     * @param source        The output to translate.
     * @param dest          The workout input to update.
     */
    public void addTranslatedValue(String source, String dest) {
        translatedValues.put(source, dest);
    }

    /**
     * Update a workout input.  Generally used before starting a workout.
     *
     * @param name      The input name.
     * @param value     The input value.
     */
    public void updateInput(String name, float value) {
        workoutController.updateAndValidateValue(name, value);
    }

}
