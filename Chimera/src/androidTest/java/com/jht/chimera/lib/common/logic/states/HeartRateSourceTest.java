package com.jht.chimera.lib.common.logic.states;


import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.service.hal.lib.hardware.heartrate.HeartRateManager;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;


public class HeartRateSourceTest {
    @Rule
    public final ServiceTestRule serviceRule = ServiceTestRule.withTimeout(60L, TimeUnit.SECONDS);

    private HeartRateManager heartRateManager;
    private DataMatrix mDataMatrix = new DataMatrix();

    @Before
    public void setup() {


//        // Create the service Intent.
//        Intent serviceIntent =
//                new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
//                        HALService.class);
//
//        // Data can be passed to the service via the Intent.
////        serviceIntent.putExtra(HALService.SEED_KEY, 42L);
//
//        // Bind the service and grab a reference to the binder.
//        IBinder binder = serviceRule.bindService(serviceIntent);
//
//        // Get the reference to the service, or you can call
//        // public methods on the binder directly.
//        HALService service =
//                ((HALService.HalBinder) binder).getService();
//
//        // Verify that the service is working correctly.
//        Assert.assertEquals(service.getKey(), 23);


        heartRateManager = HeartRateManager.getInstance();
        heartRateManager.start(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    @After
    public void tearDown() {
        DataMatrix.putInt(HALKeys.DATA_DEMO_HEART_RATE_INT, 0);
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_BLE_HEART_RATE_INT, 0);
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, 0);
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT, 0);
        mDataMatrix.finish();
    }

    @Test @Emulator @Default @Chimera
    public void testHeartRatePriority() {
        DataMatrix.putInt(HALKeys.DATA_DEMO_HEART_RATE_INT, 40);
        sleep(100);
        heartRateManager.simulateSalutronHeartRate(55);
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT, 70);
        sleep(100);
        Assert.assertEquals(70, heartRateManager.getHeartRate());

        DataMatrix.putInt(HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT, 0);
        sleep(100);
        Assert.assertEquals(55, heartRateManager.getHeartRate());

        heartRateManager.simulateSalutronHeartRate(0);
        sleep(100);
        Assert.assertEquals(40, heartRateManager.getHeartRate());
    }
}
