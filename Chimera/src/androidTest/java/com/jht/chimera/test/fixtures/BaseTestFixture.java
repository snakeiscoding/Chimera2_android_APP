package com.jht.chimera.test.fixtures;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.chimera.service.ConsoleServicesHelper;
import com.jht.chimera.test.espresso.SystemAnimationHelper;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This test fixture helps with storing and then restoring the system settings keys.  This
 * implements closeable so you can use try with resources to make sure the settings are reset at the
 * end of the test.
 *
 * In addition there are functions to help you set the frame, start all services, and setup checks
 * that are based on timing.
 *
 * Finally there are some utility functions to sleep ignoring interrupts, get strings from the
 * target context and copy an asset from the test context to the flash of the target context.
 *
 */
public class BaseTestFixture implements Closeable {


    // List of events to check the workout state.  This will be sorted.
    private final List<TestEventBase<?>> testCheckEvents = new ArrayList<>();

    // List of events used to modify workout inputs.
    private final List<TestEventBase<?>> testInjectEvents = new ArrayList<>();

    // Protect the event list.
    private final Object eventLock = new Object();

    private final SystemAnimationHelper systemAnimationHelper = new SystemAnimationHelper();

    private final SettingsCache settingsCache = new SettingsCache();

    private final Context targetContext;
    private final Context testContext;

    /**
     * Makes sure the target and test contexts are setup correctly.
     */
    public BaseTestFixture() {
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation());
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        Assert.assertNotNull(targetContext);
        Assert.assertNotNull(testContext);
    }


    /**
     * Start the services.  Make sure the services are started.
     *
     * @param dataMatrix    Used to make sure the services have started.
     */
    public void startServices(@NonNull DataMatrix dataMatrix) {
        startServicesBlocking(dataMatrix);
        dataMatrix.finish();
    }

    static public void startServicesBlocking(DataMatrix dataMatrix) {
        if (!servicesRunning(dataMatrix)) {
            try (ConsoleServicesHelper servicesHelper = new ConsoleServicesHelper()) {
                ConsoleServicesHelper.startServices();
                servicesHelper.waitForServices(0);
            }
        }
    }
    static public boolean servicesRunning(DataMatrix dataMatrix) {
        JHTNotification halHeartBeat = new JHTNotification();
        dataMatrix.createEventAdapter(HALKeys.EVENT_HAL_HEARTBEAT_RESPONSE, event -> halHeartBeat.setNotification());
        DataMatrix.sendEvent(HALKeys.EVENT_HAL_HEARTBEAT_CHECK);
        return halHeartBeat.waitForNotification(3000);
    }

    /**
     * Start the test.  Make sure events are sorted.
     */
    public void startTest() {
        Collections.sort(testCheckEvents);
        Collections.sort(testInjectEvents);
    }

    /**
     * Helper function used to set the frame.
     * @param config    The frame to make active.
     */
    public void setFrame(@NonNull FrameConfigEnum config) {
        FrameConfigManager.getInstance().setFrame(config);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Creates an event to check during a test.
     * @param event     The event to add.
     */
    public void addCheck(TestEventBase<?> event) {
        testCheckEvents.add(event);
    }

    /**
     * Adds an injection event to the test.
     * @param event     The event to add.
     */
    public void injectInput(TestEventBase<?> event) {
        testInjectEvents.add(event);
    }

    /**
     * Loop through all the checks and injections and do the operation if needed.
     * @param timing    The current timer in the test.
     * @return          If a check fails this returns an error string otherwise it returns null.
     */
    @Nullable public String runChecks(int timing) {
        synchronized (eventLock) {
            // See if it is time to update a workout input.  Make sure to remove it if we do.
            for (TestEventBase<?> event : getActiveEvents(testInjectEvents, timing)) {
                event.inject();
                testInjectEvents.remove(event);
            }

            for (TestEventBase<?> event : getActiveEvents(testCheckEvents, timing)) {
                String failedCheck = event.check();
                if(failedCheck != null) {
                    return failedCheck;
                }
                testCheckEvents.remove(event);
            }
            return null;
        }
    }

    @NonNull private List<TestEventBase<?>> getActiveEvents(List<TestEventBase<?>> events, int timing) {
        List<TestEventBase<?>> eventsFound = new ArrayList<>();
        for (TestEventBase<?> event : events) {
            if (event.timing() <= timing) {
                eventsFound.add(event);
            }
            else {
                return eventsFound;
            }
        }
        return eventsFound;
    }

    /**
     * Run through remaining checks.  Timing doesn't matter.  We assume these are end of test
     * checks.
     *
     */
    public void runFinalCheck() {
        for(TestEventBase<?> event : testCheckEvents) {
            String failedCheck = event.check();
            Assert.assertNull(failedCheck, failedCheck);
        }
        reset();
    }

    /**
     * Reset the test events.
     */
    public void reset() {
        testCheckEvents.clear();
        testInjectEvents.clear();
    }

    /**
     * End the test.  Restore all the settings.
     */
    @Override
    public void close() {
        settingsCache.restore();
        systemAnimationHelper.restore();
    }

    /**
     *
     * @param resourceId    The string resource to get.
     * @return              Returns a string corresponding to the resource input.
     */
    public String getTargetString(int resourceId) {
        return targetContext.getString(resourceId);
    }

    /**
     * Copies an asset from the test context to the target context files directory.
     * @param assetPath  The path where to find the asset.
     * @param flashPath  The path where to place the asset.
     */
    public void copyAssetToFlash(@NonNull String assetPath, String flashPath) {
        File flashFile = new File(targetContext.getFilesDir(), flashPath);
        if(flashFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            flashFile.delete();
        }
        FileUtils.copyAssetToFlash(testContext, assetPath, flashFile);
    }

    /**
     *
     * @return  The target context for the test.
     */
    public Context getTargetContext() { return targetContext; }


    /**
     *
     * @return  The context for the test.
     */
    public Context getTestContext() { return testContext; }


    /**
     * Sleep ignoring any interruption.  If we are interrupted just return.
     * @param ms    The length of time to sleep.
     */
    public static void sleepIgnoreInterruption(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    public void resetChimeraApps() {
        KillProcessHelper killProcessHelper = new KillProcessHelper();
        killProcessHelper.addPackageToKill("com.jht.mainapp", true);
        killProcessHelper.addPackageToKill("com.jht.machinesettings", true);
        killProcessHelper.addPackageToKill("com.jht.machinesetup", true);
        killProcessHelper.addPackageToKill("com.jht.egym", true);
        if(ProductConfig.isCommercial()) {
            killProcessHelper.addPackagesToClear("com.ifit.standalone", false);
        }
        killProcessHelper.kill();
    }
}
