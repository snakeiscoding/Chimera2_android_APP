package com.jht.chimera.service.workout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
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
public class TestIntervalsSpeed {

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
    public void testIntervalsSpeed() {
        try(WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {

            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.loadScript(R.raw.intervals_speed_treadmill);

            testFixture.assertHasInput("COOLDOWN_TIME_START");
            testFixture.assertHasInput("TIME_GOAL_START");
            testFixture.assertHasInput("LEVEL");
            testFixture.assertHasInput("GOAL");

            testFixture.assertHasOutput("GOAL_TYPE");
            testFixture.assertHasOutput("GOAL");
            testFixture.assertHasOutput("INCLINE");
            testFixture.assertHasOutput("SPEED");

            testFixture.updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
            testFixture.updateInput("LEVEL", 1);
            testFixture.updateInput("TIME_GOAL_START", 300000);
            testFixture.updateInput("GOAL", 18 * 1000 * 60);

            float kphConversion = SpeedConvertUtil.convertMphToKph(1);

            testFixture.addCheck(1000, "SPEED", 0.5f * kphConversion, 0.01f); // Check warmup values
            testFixture.addCheck(181000, "SPEED", 3.2f * kphConversion, 0.01f);
            testFixture.addCheck(211000, "SPEED", 1.5f * kphConversion, 0.01f);
            testFixture.addCheck(301000, "SPEED", 3.2f * kphConversion, 0.01f);
            testFixture.addCheck(331000, "SPEED", 1.5f * kphConversion, 0.01f);
            testFixture.addCheck(421000, "SPEED", 3.2f * kphConversion, 0.01f);
            testFixture.addCheck(451000, "SPEED", 1.5f * kphConversion, 0.01f);
            testFixture.addCheck(481000, "SPEED", 1.5f * kphConversion, 0.01f);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

}
