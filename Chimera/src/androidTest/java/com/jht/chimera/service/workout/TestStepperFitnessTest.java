package com.jht.chimera.service.workout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.parser.ScriptException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class TestStepperFitnessTest {

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
    public void testStepperFitnessTest() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {

            testFixture.setFrame(FrameConfigEnum.Stepper_Endurance);
            testFixture.loadScript(R.raw.stepper_fitness_test);

            testFixture.assertHasInput("COOLDOWN_TIME_START");
            testFixture.assertHasInput("SPM");
            testFixture.assertHasInput("GENDER");
            testFixture.assertHasInput("AGE");

            testFixture.assertHasOutput("GOAL_TYPE");
            testFixture.assertHasOutput("GOAL");
            testFixture.assertHasOutput("TEST_SCORE_MALE");
            testFixture.assertHasOutput("TEST_SCORE_FEMALE");
            testFixture.assertHasOutput("ALLOW_PAUSE");

            testFixture.updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
            testFixture.updateInput("GENDER", 2);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("SPM", 70);

            testFixture.updateInput("HEART_RATE", 108);

            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 2, 0.01f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 5, 0.01f);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

}
