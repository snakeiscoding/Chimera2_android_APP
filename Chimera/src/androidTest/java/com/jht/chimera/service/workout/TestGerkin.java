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
public class TestGerkin {

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
    public void testGerkin() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {

            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.loadScript(R.raw.gerkin_fitness_test);

            testFixture.assertHasInput("COOLDOWN_TIME_START");
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
            testFixture.updateInput("SPEED_IN", 0.8f);
            testFixture.updateInput("INCLINE_IN", 0.0f);
            testFixture.updateInput("GOAL", 18 * 1000 * 60);
            testFixture.updateInput("WATTS", 250);
            testFixture.updateInput("WEIGHT", 90);
            testFixture.updateInput("HEART_RATE", 108);

            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 20, 0.01f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 60, 0.01f);

            testFixture.injectInput(12 * 60 * 1000, "HEART_RATE", 180);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

}
