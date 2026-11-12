package com.jht.chimera.service.workout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workout.workoutlist.WorkoutEntry;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScript;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScripts;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutcontrollerlib.parser.ScriptException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This tests the constant watts workout script.  The tests include the following
 *      Workout Script Tests
 *          Make sure script has expected inputs and outputs.
 *          Test with LCB Watts control.
 *          Test with Software Watts control.
 *      Workout Controller Service Tests
 *          Make sure the workout controller runs the workout correctly for each frame type that
 *          has a constant watts app.
 *
 *
 * Other considerations:
 *      Resistance is clamped in the workout controller service.  We do not need to test out of
 *          range resistance values or constant watts values.  The script should not see them
 *          as inputs and the outputs are checked by the higher level code.
 */
public class TestConstantWatts {
    private static final String TAG = "TestConstantWatts";

    @Before
    public void setUp() {
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getContext());
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getTargetContext());
        DataMatrix.putBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL, true);
        
    }

    @After
    public void teardown() {
        DataMatrix.putBoolean(MainAppKeys.DATA_RUNNING_INSTRUMENTATION_TEST_BOOL, false);
    }

    /**
     * Do a basic test making sure the script has the expected inputs and outputs
     */
    @Test @Emulator @Default @Chimera
    public void testConstantWattsScript() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {

            testFixture.loadScript(R.raw.constant_watts_resistance);

            testFixture.assertHasInput("WATTS");
            testFixture.assertHasInput("TIME_GOAL_START");
            testFixture.assertHasInput("TARGET_WATTS_START");
            testFixture.assertHasInput("TARGET_WATTS");
            testFixture.assertHasInput("COOLDOWN_TIME_START");
            testFixture.assertHasInput("RESISTANCE_IN");
            testFixture.assertHasInput("LCB_CONTROLS_WATTS");

            testFixture.assertHasOutput("RESISTANCE");
            testFixture.assertHasOutput("INCLINE");
            testFixture.assertHasOutput("GOAL");
            testFixture.assertHasOutput("GOAL_TYPE");
            testFixture.assertHasOutput("TARGET_WATTS");
            testFixture.assertHasOutput("PREVENT_USER_GOAL_CHANGES");
            testFixture.assertHasOutput("PREVENT_USER_CHANGES");
            testFixture.assertHasOutput("CONTROL_MODE");

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("TARGET_WATTS", 300);
            testFixture.updateInput("WATTS", 300);
            testFixture.updateInput("TARGET_WATTS_START", 300);
            testFixture.updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
            testFixture.updateInput("TIME_GOAL_START", 5 * 60 * 1000);
            testFixture.updateInput("RESISTANCE_IN", 1);
            testFixture.updateInput("LCB_CONTROLS_WATTS", 1);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    /**
     * Test the constant watts programs on machines where the LCB controls the resistance.
     * In this test the user should be able to control resistance in warm up and cool down.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsLCBControl() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.loadScript(R.raw.constant_watts_resistance);

            // Make sure the warmup has the correct state.
            testFixture.addCheck(30000, WorkoutController.STAGE, 0.0f);
            testFixture.addCheck(30000, "CONTROL_MODE", 0);
            testFixture.addCheck(30000, "PREVENT_USER_CHANGES", 0);
            testFixture.addCheck(30000, "RESISTANCE", 1);

            // Make sure the workout has the correct state.
            testFixture.addCheck(3 * 60 * 1000, WorkoutController.STAGE, 1.0f);
            testFixture.addCheck(3 * 60 * 1000, "CONTROL_MODE", 1);
            testFixture.addCheck(3 * 60 * 1000, "PREVENT_USER_CHANGES", 1);
            testFixture.addCheck(3 * 60 * 1000 + 1000, "RESISTANCE", 1);

            // Make sure cool down has the correct state.
            testFixture.addCheck(7 * 60 * 1000 + 30000, WorkoutController.STAGE, 2.0f);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "CONTROL_MODE", 0);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "RESISTANCE", 1);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "PREVENT_USER_CHANGES", 0);


            // We could increase resistance here, but the script does nothing with it in this mode.

            // Setup the workout.
            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("TARGET_WATTS", 300);
            testFixture.updateInput("WATTS", 300);
            testFixture.updateInput("TARGET_WATTS_START", 300);
            testFixture.updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
            testFixture.updateInput("TIME_GOAL_START", 5 * 60 * 1000);
            testFixture.updateInput("RESISTANCE_IN", 1);
            testFixture.updateInput("LCB_CONTROLS_WATTS", 1);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    /**
     * Test out the constant watts workout on machines where we control the watts in software.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsSoftwareControl() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.loadScript(R.raw.constant_watts_resistance);

            // For this test we need to reroute resistance values from the script back to it.
            // The main code does this automatically.  Also note that the software will clamp the
            // resistance output which makes sure the resistance in is not above the range.
            // No need to test this edge case.
            testFixture.addTranslatedValue("RESISTANCE", "RESISTANCE_IN");

            // Make sure the warmup has the correct state.
            testFixture.addCheck(30000, WorkoutController.STAGE, 0.0f);
            testFixture.addCheck(30000, "CONTROL_MODE", 0);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "PREVENT_USER_CHANGES", 0);
            testFixture.addCheck(30000, "RESISTANCE", 1);

            // Make sure the workout has the correct state.
            testFixture.addCheck(3 * 60 * 1000, WorkoutController.STAGE, 1.0f);
            testFixture.addCheck(3 * 60 * 1000, "CONTROL_MODE", 0);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "PREVENT_USER_CHANGES", 0);
            testFixture.addCheck(3 * 60 * 1000 + 1000, "RESISTANCE", 1);

            // Make sure the cool down has the correct state.
            testFixture.addCheck(7 * 60 * 1000 + 30000, WorkoutController.STAGE, 2.0f);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "CONTROL_MODE", 0);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "RESISTANCE", 1);
            testFixture.addCheck(7 * 60 * 1000 + 30000, "PREVENT_USER_CHANGES", 0);

            // Increase the resistance in the warmup making sure this is properly accounted for in the workout.
            testFixture.injectInput(31000, "RESISTANCE_IN", 2);


            // Test out the workout by adjusting watts and make sure the resistance tracks per the script.
            int time = 3 * 60 * 1000 + 10000;
            testFixture.injectInput(time, "TARGET_WATTS", 300);
            testFixture.injectInput(time, "WATTS", 240);
            time += 1000;
            testFixture.addCheck(time, "RESISTANCE", 5);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 8);
            testFixture.injectInput(time, "WATTS", 260);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 10);
            testFixture.injectInput(time, "WATTS", 280);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 11);
            testFixture.injectInput(time, "WATTS", 305);
            time += 6000;
            testFixture.addCheck(time, "RESISTANCE", 11);
            testFixture.injectInput(time, "WATTS", 315);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 10);
            testFixture.injectInput(time, "WATTS", 335);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 8);
            testFixture.injectInput(time, "WATTS", 355);
            time += 2000;
            testFixture.addCheck(time, "RESISTANCE", 5);

            // Setup the workout.
            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("TARGET_WATTS", 300);
            testFixture.updateInput("WATTS", 300);
            testFixture.updateInput("TARGET_WATTS_START", 300);
            testFixture.updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
            testFixture.updateInput("TIME_GOAL_START", 5 * 60 * 1000);
            testFixture.updateInput("RESISTANCE_IN", 1);
            testFixture.updateInput("LCB_CONTROLS_WATTS", 0);

            // Run the workout.
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerSpinBike() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.CardioSpinBike);
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerBike() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.HybridBike_Performance);
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerAscent() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.Ascent_Performance);
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerEP() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.Elliptical_Performance);
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerStepper() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.Stepper_Endurance);
        }
    }

    /**
     * Test running the workout via the workout controller service.
     */
    @Test @Emulator @Default @Chimera
    public void testWattsWorkoutControllerLifestyle() {
        try (WorkoutControllerServiceTestFixture testFixture = new WorkoutControllerServiceTestFixture(new DataMatrix())){
            testWattsWorkoutController(testFixture, FrameConfigEnum.Elliptical_Lifestyle);
        }
    }


    /**
     * This runs a workout controller constant watts test for a given frame.
     * @param testFixture       Test fixture used to run the test.
     * @param frame             Frame to test the constant watts program on.
     */
    private void testWattsWorkoutController(WorkoutControllerServiceTestFixture testFixture, FrameConfigEnum frame) {

        LaunchAppUtil.launchAndroidLauncher();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testFixture.setFrame(frame);

        // Get the constant watts script.
        WorkoutScripts workoutScripts = new WorkoutScripts();
        WorkoutScript script = workoutScripts.getWorkoutScript(20);
        Assert.assertNotNull(script);

        WorkoutEntry entry = new WorkoutEntry(20, "ConstantWatts", script.scriptString);
        DataMatrix.putParcelable(WorkoutKeys.ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE, entry);

        testFixture.addInitialCondition("AGE", 30);
        testFixture.addInitialCondition("TARGET_WATTS_START", 300);
        testFixture.addInitialCondition("TIME_GOAL_START", 5);
        testFixture.addInitialCondition("RESISTANCE_IN", 1);

        testFixture.addCheck(30000, WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, 0);
        testFixture.injectInput(30000, WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, 4);
        testFixture.addCheck(32000, HALKeys.DATA_RESISTANCE_INT, 4);

        if(frame.createFrameConfig().LCB_CAN_CONTROL_WATTS) {
            testFixture.addCheck(4 * 60 * 1000 + 1000, WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, 1);
            testFixture.injectInput(4 * 60 * 1000 + 1000, WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, 4);
            testFixture.addCheck(4 * 60 * 1000 + 2000, HALKeys.DATA_RESISTANCE_INT, 1);
        }
        else {
            testFixture.addCheck(4 * 60 * 1000 + 1000, WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, 0);
            testFixture.addCheck(4 * 60 * 1000 + 1000, HALKeys.DATA_RESISTANCE_INT, 25);
            testFixture.injectInput(4 * 60 * 1000 + 1000, WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, 4);
            testFixture.addCheck(4 * 60 * 1000 + 3000, HALKeys.DATA_RESISTANCE_INT, 25);
        }

        testFixture.addCheck(8 * 60 * 1000 + 2000, WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, 0);
        testFixture.injectInput(8 * 60 * 1000 + 2000, WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, 5);
        testFixture.addCheck(8 * 60 * 1000 + 4000, HALKeys.DATA_RESISTANCE_INT, 5);

        testFixture.runWorkout(20, AppIds.CONSTANT_WATTS_UUID);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
