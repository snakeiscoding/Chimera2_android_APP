package com.jht.chimera.service.workout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.R;
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
public class TestAirForce {

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
    public void testAirForceScript() {
        try(ArmedServiceFitnessTestFixture testFixture = new ArmedServiceFitnessTestFixture(R.raw.air_force_fitness_test, 2.41402f)) {

            testFixture.runTest(30, 660, 42.0f, 50.0f);
            testFixture.runTest(30, 815, 36.0f, 42.0f);

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

}
