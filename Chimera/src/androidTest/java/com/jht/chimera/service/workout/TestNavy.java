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
public class TestNavy {

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


    @Test @Emulator @Default @Chimera
    public void testNavyScript() {
        try(ArmedServiceFitnessTestFixture testFixture = new ArmedServiceFitnessTestFixture(R.raw.navy_fitness_test, 2.41402f)) {
            testFixture.runTest(30, 659, 8.0f, 11.0f, 180, 0);
            testFixture.runTest(30, 659, 9.0f, 12.0f, 180, 1);
        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }

    }

}
