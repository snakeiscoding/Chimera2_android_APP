package com.jht.chimera.service.hal.lib.hardware;


import static com.jht.androidcommonalgorithms.util.ThreadHelper.sleepIgnoreInterruption;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.service.hal.lib.hardware.heartrate.HeartRateManager;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MainApp;
import com.jht.chimera.test.fixtures.SystemTestFixture;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TestHeartRateManager {

    @Test
    @Chimera
    @Emulator
    @MainApp
    public void testHandgrip() {
        try(SystemTestFixture testFixture = new SystemTestFixture()) {
            ParkerHRDriverSim hrSimulator = new ParkerHRDriverSim();
            HeartRateManager.getInstance().start(testFixture.getTargetContext(), hrSimulator, false);
            hrSimulator.demoHandgripHeartRate = 75;
            sleepIgnoreInterruption(1100);
            assertEquals(75, HeartRateManager.getInstance().getHeartRate());
            hrSimulator.demoHandgripHeartRate = 85;
            sleepIgnoreInterruption(1100);
            assertEquals(85, HeartRateManager.getInstance().getHeartRate());
            hrSimulator.demoPolarHeartRate = 66;
            sleepIgnoreInterruption(1100);
            assertEquals(66, HeartRateManager.getInstance().getHeartRate());
        }
    }
}
