package com.jht.chimera.ui.mainapp.egym;


import android.app.Instrumentation;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.chimera.test.fixtures.BaseTestFixture;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.WelcomeActivity;
import com.jht.applibrary.chameleon.AppLibraryActivity;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class tests out every Javascript API call in the EGym web interface.
 */
public class EGymWebAppInterfaceTest {
    private static final String TAG = "EGymWebAppInterfaceTest";
    private EGymWebAppInterface eGymWebAppInterface;

    // We need to start egym workouts in a looper thread.
    private HandlerThread handlerThread;
    private Handler handler;
    
    @Before
    public void init() {
        // Make sure we are at the android launcher.
        LaunchAppUtil.launchAndroidLauncher();

        // Initialize the interface and frame.
        eGymWebAppInterface = new EGymWebAppInterface(null);
        FrameConfigManager.getInstance().refreshFrame();

        // Start the handler thread needed by some tests to start the EGym workout.
        handlerThread = new HandlerThread("egymhelper");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // Make sure the system services are started.  Not necessary for all tests, but this
        // simplifies the tests below.
        DataMatrix dataMatrix = new DataMatrix();
        BaseTestFixture.startServicesBlocking(dataMatrix);
        dataMatrix.finish();
    }

    @After
    public void term() {
        LaunchAppUtil.launchAndroidLauncher();
        handlerThread.quit();

    }

    /**
     * Check the metrics JSON.  Make sure it has the correct values.
     */
    @Test @Emulator @Default @Chimera
    public void testOnMetricsUpdated() {
        testOnMetricsUpdated(true);
        testOnMetricsUpdated(false);
    }

    /**
     * Check the metrics JSON.
     *
     * @param includeFinalSnapshot  True if the JSON should include the final snapshot field.
     */
    private void testOnMetricsUpdated(boolean includeFinalSnapshot) {

        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        DataMatrix.putBoolean(HALKeys.DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL, true);
        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Random random = new Random();

            float km = random.nextFloat() + random.nextInt(4);
            int elapsedTime = random.nextInt(10 * 60) + 60;
            int calories = random.nextInt(400);
            int heartRate = random.nextInt(60) + 80;
            int averageHeartRate = random.nextInt(60) + 80;
            int rpm = random.nextInt(60) + 80;
            int averageRPM = random.nextInt(60) + 80;
            int spm = random.nextInt(60) + 80;
            int averageSPM = random.nextInt(60) + 80;
            int speed = random.nextInt(10) + 2;
            int averageSpeed = random.nextInt(10) + 2;
            int resistance = random.nextInt(19) + 1;
            int averageResistance = random.nextInt(19) + 1;
            float incline = random.nextInt(15);
            float averageIncline = random.nextInt(15);
            DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, km);
            DataMatrix.putInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, elapsedTime * 1000);
            DataMatrix.putFloat(WorkoutKeys.DATA_CALORIES_FLOAT, calories);
            DataMatrix.putInt(HALKeys.DATA_DEMO_HEART_RATE_INT, heartRate);
            DataMatrix.putInt(HALKeys.DATA_HEART_RATE_INT, heartRate);
            DataMatrix.putFloat(WorkoutKeys.DATA_AVERAGE_HEART_RATE_FLOAT, averageHeartRate);
            DataMatrix.putInt(HALKeys.DATA_RPM_INT, rpm);
            DataMatrix.putInt(HALKeys.DATA_SPM_INT, spm);
            DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, speed);
            DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, speed);
            DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, incline);
            DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, resistance);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT, averageSpeed);
            DataMatrix.putFloat(WorkoutKeys.DATA_AVERAGE_INCLINE_FLOAT, averageIncline);
            DataMatrix.putFloat(WorkoutKeys.DATA_AVERAGE_RESISTANCE_FLOAT, averageResistance);
            DataMatrix.putFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT, averageRPM);
            DataMatrix.putInt(WorkoutKeys.DATA_AVERAGE_SPM_INT, averageSPM);

            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, includeFinalSnapshot ? AppIds.EGYM_UUID : AppIds.DEFAULT_APP_UUID);

            String metrics = eGymWebAppInterface.onMetricsUpdated();
            List<String> expectedKeys;


            expectedKeys = Arrays.asList(
                    "distance", "elapsedTime", "kcal", "heartRate", "averageHeartRate",
                    "heartRateStatus", "rpm", "spm", "speed", "incline", "resistance",
                    "avgSpeed", "avgIncline", "avgResistance", "avgRPM", "avgSPM", "avgKcalPerMin",
                    "minResistanceLevel", "maxResistanceLevel", "minSpeed", "maxSpeed", "minIncline", "maxIncline",
                    "macAddress", "finalSnapshot");

            try {
                JSONObject object = new JSONObject(metrics);
                for (Iterator<String> it = object.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Assert.assertTrue(key + " was not expected in onMetrics JSON", expectedKeys.contains(key));
                }
                for (String key : expectedKeys) {
                    Assert.assertTrue(key + " not found in onMetrics JSON", object.has(key));
                }

                Assert.assertEquals(object.getDouble("distance"), km, 0.01);
                Assert.assertEquals(object.getInt("elapsedTime"), elapsedTime);
                Assert.assertEquals(object.getInt("kcal"), calories);
                Assert.assertEquals(object.getInt("heartRate"), heartRate);
                Assert.assertEquals(object.getDouble("averageHeartRate"), averageHeartRate, 0.1);
                Assert.assertEquals(object.getString("heartRateStatus"), heartRate > 0 ? "VALID" : "INVALID");
                Assert.assertEquals(object.getInt("rpm"), rpm);
                Assert.assertEquals(object.getInt("spm"), spm);
                Assert.assertEquals(object.getDouble("speed"), speed, 0.1);
                Assert.assertEquals(object.getDouble("incline"), incline, 0.1);
                Assert.assertEquals(object.getInt("resistance"), resistance);
                Assert.assertEquals(object.getDouble("avgSpeed"), averageSpeed, 0.1);
                Assert.assertEquals(object.getDouble("avgIncline"), averageIncline, 0.1);
                Assert.assertEquals(object.getInt("avgResistance"), averageResistance);
                Assert.assertEquals(object.getInt("avgRPM"), averageRPM);
                Assert.assertEquals(object.getInt("avgSPM"), averageSPM);


            } catch (JSONException e) {
                Assert.fail("Error parsing the JSON object returned in the onMetrics updated\n" + metrics);
            }
        }
        finally {
            DataMatrix.putBoolean(HALKeys.DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL, false);
        }
    }

    /**
     * Modify the egym state and make sure we report it correctly.
     */
    @Test @Emulator @Default @Chimera
    public void testEGymState() {
        JHTNotification notification = new JHTNotification();
        AtomicInteger egymStateExpected = new AtomicInteger(0);
        eGymWebAppInterface.setOnEGymSateChange(state -> {
            Assert.assertEquals(egymStateExpected.get(), state.ordinal());
            notification.setNotification();
        });
        for(EGymWebAppInterface.EGymState state : EGymWebAppInterface.EGymState.values()) {
            egymStateExpected.set(state.ordinal());
            eGymWebAppInterface.setEgymState(state.name());
            notification.waitForNotification();
            Assert.assertEquals(state.name().toLowerCase(), eGymWebAppInterface.getEgymState().toLowerCase());
        }
    }

    /**
     * Modify the workout state and make sure it is reported correctly.
     */
    @Test @Emulator @Default @Chimera
    public void testEGymWorkoutState() {
        AtomicInteger egymStateExpected = new AtomicInteger(0);
        for(EGymWebAppInterface.EGymWorkoutState state : EGymWebAppInterface.EGymWorkoutState.values()) {
            egymStateExpected.set(state.ordinal());
            eGymWebAppInterface.setWorkoutState(state.name());
            Assert.assertEquals(state.name().toLowerCase(), eGymWebAppInterface.getWorkoutState().toLowerCase());
        }
    }

    /**
     * Check the machine information JSON.
     */
    @Test @Emulator @Default @Chimera
    public void testGetMachineInformation() {

        for(FrameConfigEnum frameType : FrameConfigEnum.getSupportedFrames()) {
            try (BaseTestFixture testFixture = new BaseTestFixture()) {
                DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
                testFixture.setFrame(frameType);
                FrameConfigManager.getInstance().refreshFrame();
                FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

                List<String> expectedKeys = Arrays.asList(
                        "minResistanceLevel", "maxResistanceLevel", "minSpeed", "maxSpeed", "minIncline",
                        "maxIncline", "macAddress", "rfidLocation", "version","consoleSerialNumber","frameSerialNumber", "supportsWallet");

                // this should make it so RfidReaderManager.getReaderFeatures returns false for supportWallets
                AndroidSetting.putLong(ContextUtil.getApplicationContext().getContentResolver(), "chimera_rfid_features",0L);

                String info = eGymWebAppInterface.getMachineInformation();
                try {
                    JSONObject object = new JSONObject(info);
                    for (Iterator<String> it = object.keys(); it.hasNext(); ) {
                        String key = it.next();
                        Assert.assertTrue(key + " was not expected in onMetrics JSON", expectedKeys.contains(key));
                    }
                    for (String key : expectedKeys) {
                        Assert.assertTrue(key + " not found in onMetrics JSON", object.has(key));
                    }

                    Assert.assertEquals(frameType.name(), object.getInt("minResistanceLevel"), (int) config.resistance.getMin());
                    Assert.assertEquals(frameType.name(), object.getInt("maxResistanceLevel"), (int) config.resistance.getMax());
                    Assert.assertEquals(frameType.name(), object.getDouble("minSpeed"), config.speedKph.getMin(), 0.1);
                    Assert.assertEquals(frameType.name(), object.getDouble("maxSpeed"), config.speedKph.getMax(), 0.1);
                    Assert.assertEquals(frameType.name(), object.getDouble("minIncline"), config.incline.getMin(), 0.1);
                    Assert.assertEquals(frameType.name(), object.getDouble("maxIncline"), config.incline.getMax(), 0.1);
                    Assert.assertEquals(frameType.name(), object.getString("consoleSerialNumber"), DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING));
                    Assert.assertEquals(frameType.name(), object.getString("frameSerialNumber"), DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING));
                    String rfidLocation = ProductConfig.getConsoleType().rfidLocation.name().replace("_", " ");
                    Assert.assertEquals(frameType.name(), object.getString("rfidLocation").toLowerCase(), rfidLocation.toLowerCase());
                    Assert.assertEquals(BuildConfig.VERSION_NAME, object.optString("version", ""));
                    Assert.assertEquals(frameType.name(), object.getString("consoleSerialNumber"), DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING));
                    Assert.assertEquals(frameType.name(), object.getBoolean("supportsWallet"), false);
                } catch (JSONException e) {
                    Assert.fail("Error parsing the JSON object returned in the onMachineInfo\n" + info);
                }

                Assert.assertEquals(frameType.name(), FrameConfigManager.getInstance().getFrameConfig().machineClassType.name(), eGymWebAppInterface.getMachineType());
            }
            Assert.assertEquals(DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING), eGymWebAppInterface.getMachineSerialNumber());
        }

    }

    /**
     * This sets whether the user can manipulate the resistance.  Test that and make sure
     * the system is correct.
     */
    @Test @Emulator @Default @Chimera
    public void testPedalChanges() {
        eGymWebAppInterface.disablePedalChanges();
        Assert.assertTrue(DataMatrix.getBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL));
        eGymWebAppInterface.enablePedalChanges();
        Assert.assertFalse(DataMatrix.getBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL));
    }

    /**
     * Check the various calls to get machine data.  Make sure the data reported is accurate.
     */
    @Test @Emulator @Default @Chimera
    public void testData() {

        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            Random random = new Random();
            DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, random.nextInt(19) + 1);
            Assert.assertEquals(DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), eGymWebAppInterface.getCurrentResistance());
            DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, random.nextInt(15));
            Assert.assertEquals(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), eGymWebAppInterface.getCurrentIncline(), 0.1);
            float speed = random.nextInt(15) + 1 + random.nextFloat();
            DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, speed);
            DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, speed);
            Assert.assertEquals(speed, eGymWebAppInterface.getCurrentSpeed(), 0.1);

            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
            Assert.assertEquals("METRIC", eGymWebAppInterface.getMeasurementUnit());
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, false);
            Assert.assertEquals("IMPERIAL", eGymWebAppInterface.getMeasurementUnit());


            float km = random.nextFloat() + random.nextInt(4);
            int elapsedTime = random.nextInt(10 * 60) + 60;
            int calories = random.nextInt(400);
            DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, km);
            DataMatrix.putInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, elapsedTime * 1000);
            DataMatrix.putFloat(WorkoutKeys.DATA_CALORIES_FLOAT, calories);

        }

    }

    /**
     * Simulate EGym launching its welcome screen.  Make sure this works.
     */
    @Test @Emulator @Default @Chimera
    public void testGoToWelcome() {
        eGymWebAppInterface.setWorkoutState(EGymWebAppInterface.EGymWorkoutState.RUN.name());
        eGymWebAppInterface.setEgymState(EGymWebAppInterface.EGymState.SUMMARY.name());
        eGymWebAppInterface.goToWelcomePage();
        Assert.assertEquals(EGymWebAppInterface.EGymState.SELECTION.name().toLowerCase(), eGymWebAppInterface.getEgymState().toLowerCase());
        Assert.assertEquals(EGymWebAppInterface.EGymWorkoutState.PRE.name().toLowerCase(), eGymWebAppInterface.getWorkoutState().toLowerCase());
    }

    /**
     * Make sure we can change the diplay units.
     */
    @Test @Emulator @Default @Chimera
    public void testUnit() {
        eGymWebAppInterface.setUnit("Metric");
        Assert.assertTrue(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        eGymWebAppInterface.setUnit("Imperial");
        Assert.assertFalse(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
    }

    /**
     * Make sure we can change the resistance on frames that have resistance.  Handle out of range cases.
     */
    @Test @Emulator @Default @Chimera
    public void testChangeResistance() {
        JHTNotification notification = new JHTNotification();
        AtomicInteger expectedResistance = new AtomicInteger();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createIntAdapter(HALKeys.ACTION_SET_RESISTANCE_INT, resistance -> {
            Assert.assertEquals(expectedResistance.get(), resistance.intValue());
            DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, resistance);
            notification.setNotification();
        });

        DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.EGYM_UUID);
        DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, 1);
        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Ascent_Performance);
            DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, 1);
            expectedResistance.set(2);
            eGymWebAppInterface.changeResistance(true);
            Assert.assertTrue(notification.waitForNotification(1000));
            expectedResistance.set(1);
            eGymWebAppInterface.changeResistance(false);
            Assert.assertTrue(notification.waitForNotification(1000));

            expectedResistance.set(25);
            eGymWebAppInterface.setResistanceValue(30);
            Assert.assertTrue(notification.waitForNotification(1000));


            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.DEFAULT_APP_UUID);
            DataMatrix.putInt(HALKeys.DATA_RESISTANCE_INT, 1);

            eGymWebAppInterface.changeResistance(true);
            Assert.assertFalse(notification.waitForNotification(1000));


        }
        finally {
            dataMatrix.finish();
        }
    }


    /**
     * Make sure we can change the incline on frames that have incline.  Handle out of range cases.
     */
    @Test @Emulator @Default @Chimera
    public void testChangeIncline() {

        JHTNotification notification = new JHTNotification();
        AtomicInteger expectedIncline = new AtomicInteger();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_INCLINE_FLOAT, incline -> {
            Assert.assertEquals(expectedIncline.get() / 2.0, incline, 0.1);
            DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, incline);
            notification.setNotification();
        });

        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            Log.d(TAG, "incline settings " + frameConfig.machineClassType + " " + frameConfig.incline.stepSize + " " + frameConfig.incline.getMaxModel() + " " + frameConfig.incline.getMax());
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.EGYM_UUID);
            DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 1.0f);
            expectedIncline.set(3);
            eGymWebAppInterface.changeIncline(true);
            Assert.assertTrue(notification.waitForNotification(1000));
            expectedIncline.set(2);
            eGymWebAppInterface.changeIncline(false);
            Assert.assertTrue(notification.waitForNotification(1000));


            expectedIncline.set(36);
            eGymWebAppInterface.setInclineValue(20);
            Assert.assertTrue(notification.waitForNotification(1000));
            expectedIncline.set(10);
            eGymWebAppInterface.setInclineValue(4.9);
            Assert.assertTrue(notification.waitForNotification(1000));

            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.DEFAULT_APP_UUID);
            DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 1);

            eGymWebAppInterface.changeIncline(true);
            Assert.assertFalse(notification.waitForNotification(1000));
        }
        finally {
            dataMatrix.finish();
        }
    }

    /**
     * Make sure we can change the speed on frames that have speed.  Handle out of range cases.
     */
    @Test @Emulator @Default @Chimera
    public void testChangeSpeed() {
        JHTDataNotification<Float> notification = new JHTDataNotification<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, notification::setData);

        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            FrameConfigManager.getInstance().refreshFrame();
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            Log.d(TAG, "incline settings " + frameConfig.machineClassType + " " + frameConfig.incline.stepSize + " " + frameConfig.incline.getMaxModel() + " " + frameConfig.incline.getMax());
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.EGYM_UUID);
            ThreadHelper.sleepIgnoreInterruption(500);
            eGymWebAppInterface.setSpeedValue((frameConfig.speedKph.getMax() + 10) * 10);
            checkSpeed(notification, frameConfig.speedKph.getMax());
            eGymWebAppInterface.setSpeedValue(4.9);
            checkSpeed(notification, 4.9f);

            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.DEFAULT_APP_UUID);

            eGymWebAppInterface.setSpeedValue(5.5);
            Assert.assertNull(notification.waitForData(1000));
        }
        finally {
            dataMatrix.finish();
        }
    }

    private void checkSpeed(JHTDataNotification<Float> notification, float expectedSpeed) {
        Float speed = notification.waitForData(1000);
        Assert.assertNotNull(speed);
        Assert.assertEquals(expectedSpeed, speed, 0.1);
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, speed);
    }

    /**
     * Test the scan mode.
     */
    @Test @Emulator @Default @Chimera
    public void testSetScanMode() {
        JHTDataNotification<Integer> notification = new JHTDataNotification<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createIntAdapter(HALKeys.ACTION_RFID_OPERATING_MODE_INT, notification::setData);

        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            FrameConfigManager.getInstance().refreshFrame();
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.EGYM_UUID);
            ThreadHelper.sleepIgnoreInterruption(500);

            // make sure mode 0 is set correctly
            eGymWebAppInterface.setScanMode(0);
            checkScanMode(notification, 0);

            // make sure mode 1 is set correctly
            eGymWebAppInterface.setScanMode(1);
            checkScanMode(notification, 1);

            // make sure invalid value set the default value 0
            eGymWebAppInterface.setScanMode(2);
            checkScanMode(notification, 0);

            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.DEFAULT_APP_UUID);

        }
        finally {
            dataMatrix.finish();
        }
    }

    private void checkScanMode(JHTDataNotification<Integer> notification, int expectedMode) {
        Integer value = notification.waitForData(1000);
        Assert.assertNotNull(value);
        int mode = value;
        Assert.assertEquals(expectedMode, mode);
    }

    /**
     * Make sure we handle the client secret and id correctly.
     */
    @Test @Emulator @Default @Chimera
    public void testClientSecrets() {
        Random random = new Random();

        String clientSecret = random.nextInt() + "";
        String clientId = random.nextInt() + "";
        eGymWebAppInterface.setClientSecret(clientSecret);
        eGymWebAppInterface.setClientID(clientId);
        Assert.assertEquals(clientId, eGymWebAppInterface.getClientID());
        Assert.assertEquals(clientSecret, eGymWebAppInterface.getClientSecret());
    }

    /**
     * Simulate login in progress changes and make sure these set the appropriate flags.
     */
    @Test @Emulator @Default @Chimera
    public void testLoginProgress() {
        eGymWebAppInterface.setLoginInProgress(true);
        Assert.assertTrue(eGymWebAppInterface.getIsLoginInProgress());
        eGymWebAppInterface.notifyLoginComplete();
        Assert.assertFalse(eGymWebAppInterface.getIsLoginInProgress());
    }

    /**
     * Run an EGym workout on a treadmill and make sure we can control the system.
     */
    @Test @Emulator @Default @Chimera
    public void testEGymWorkout() {

        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);


            AtomicBoolean workoutStarted = new AtomicBoolean(false);
            handler.post(() -> workoutStarted.set(eGymWebAppInterface.startEgymWorkout()));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertTrue(workoutStarted.get());

            eGymWebAppInterface.onMachineControl("speed", 10.5);
            eGymWebAppInterface.onMachineControl("incline", 5.5);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(10.5, DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT), 0.1);
            Assert.assertEquals(5.5, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            eGymWebAppInterface.onMachineControl("pause", 0.0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_PAUSE, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            eGymWebAppInterface.onMachineControl("resume", 0.0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_RUNNING, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));

            // Was going to test this, but as of now the user cannot change the goal so this doesn't work.  Not sure why it is a part of the API.
            /*
            eGymWebAppInterface.onMachineControl("seconds", 35 * 60);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(35 * 60 * 1000, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT), 0.1);
            */

            eGymWebAppInterface.onMachineControl("stop", 0.0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_STOP, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
        }
    }

    /**
     * Run an EGym workout on an elliptical and make sure we can control the system.
     */
    @Test @Emulator @Default @Chimera
    public void testEGymWorkout2() {

        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        try (BaseTestFixture testFixture = new BaseTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Elliptical_Performance);


            AtomicBoolean workoutStarted = new AtomicBoolean(false);
            handler.post(() -> workoutStarted.set(eGymWebAppInterface.startEgymWorkout()));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertTrue(workoutStarted.get());
            eGymWebAppInterface.onMachineControl("resistance", 5);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(5, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT));
            Assert.assertTrue(eGymWebAppInterface.pauseWorkout());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_PAUSE, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            Assert.assertTrue(eGymWebAppInterface.resumeWorkout());
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_RUNNING, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            Assert.assertTrue(eGymWebAppInterface.stopWorkout());
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_STOP, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
        }
        finally {
            LaunchAppUtil.launchAndroidLauncher();
        }
    }

    /**
     * Simulate showing a message in the message center from EGym.  Make sure it goes away when
     * it should.
     */
    @Test @Emulator @Default @Chimera
    public void testShowMessage() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Make sure there isn't any other message.
        while(true) {
            UiObject2 closeButton = device.findObject(By.res("com.jht.chimera:id/message_close_btn"));
            if(closeButton == null) {
                break;
            }
            closeButton.click();
            try {
                //noinspection BusyWait
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        eGymWebAppInterface.showMessage("Testing EGym Message", 5000);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
        Assert.assertNotNull(device.findObject(By.text("Testing EGym Message")));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }
        Assert.assertNull(device.findObject(By.text("Testing EGym Message")));

    }

    /**
     * Test the various activity launches that EGym can make.
     */
    @Test @Emulator @Default @Chimera
    public void testActivityLaunch() {


        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
        Instrumentation.ActivityMonitor welcomeActivityMonitor = InstrumentationRegistry.getInstrumentation().addMonitor(WelcomeActivity.class.getCanonicalName(), null, false);
        eGymWebAppInterface.showMatrixExperienceWelcomeScreen();
        Assert.assertTrue(welcomeActivityMonitor.getHits() > 0);

        Instrumentation.ActivityMonitor goActivityMonitor = InstrumentationRegistry.getInstrumentation().addMonitor(GoScreenActivity.class.getCanonicalName(), null, false);
        eGymWebAppInterface.showMatrixExperienceGoScreen();
        Assert.assertTrue(goActivityMonitor.getHits() > 0);
        LaunchAppUtil.launchAndroidLauncher();

        Instrumentation.ActivityMonitor workoutMonitor = InstrumentationRegistry.getInstrumentation().addMonitor(AppLibraryActivity.class.getCanonicalName(), null, false);

        DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.EGYM_UUID);

        handler.post(() -> eGymWebAppInterface.startEgymWorkout());
        LaunchAppUtil.launchEGym();
        try {
            Thread.sleep(6000);
        } catch (InterruptedException ignored) {
        }

        /*
        boolean fullScreen = eGymWebAppInterface.getIsFullscreen();
        eGymWebAppInterface.resizeScreen();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ignored) {
        }
        Assert.assertNotEquals(fullScreen, eGymWebAppInterface.getIsFullscreen());
*/
        eGymWebAppInterface.hideWebApp();
        Assert.assertTrue(goActivityMonitor.getHits() > 0);
        eGymWebAppInterface.stopWorkout();
        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.IDLE);

    }


    /**
     * Test user login.
     */
    @Test @Emulator @Default @Chimera
    public void testUserLogin() {
        Random random = new Random();
        float weight = random.nextInt(50) + 40;
        int age = random.nextInt(50) + 13;
        boolean isMale = random.nextBoolean();
        eGymWebAppInterface.setUserProperties(weight, age, isMale ? "male" : "female", "");
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertTrue(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(isMale, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT) == 0);
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals(DapiUser.GUEST_USER_UUID, DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));


        eGymWebAppInterface.resetStates();
        Assert.assertTrue(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertTrue(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT), DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertEquals(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT), DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT), DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals(DapiUser.GUEST_USER_UUID, DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        weight = random.nextInt(50) + 40;
        age = random.nextInt(50) + 13;
        isMale = random.nextBoolean();
        eGymWebAppInterface.setUserProperties(weight, age, isMale ? "male" : "female", "27ef5a2a-1e85-4997-a4b9-bba2054bd16d");
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertFalse(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(isMale, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT) == 0);
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals("egym-27ef5a2a-1e85-4997-a4b9-bba2054bd16d", DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        eGymWebAppInterface.resetStates();

    }


    /**
     * Test user login with new setUserInformation call instead of setUserProperties.
     */
    @Test @Emulator @Default @Chimera
    public void testUserLoginSetInformation() {
        Random random = new Random();
        float weight = random.nextInt(50) + 40;
        int age = random.nextInt(50) + 13;
        eGymWebAppInterface.setUserInformation("test1","male","imperial","27ef5a2a-1e85-4997-a4b9-bba2054bd16d",weight, age);
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertFalse(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(0, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertFalse(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals("test1",DataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING));
        Assert.assertEquals("egym-27ef5a2a-1e85-4997-a4b9-bba2054bd16d", DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        eGymWebAppInterface.resetStates();
        Assert.assertTrue(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT), DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertEquals(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT), DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL),DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        Assert.assertEquals(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT), DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals(ContextUtil.getApplicationContext().getString(R.string.guest_tc),DataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING));
        Assert.assertEquals(DapiUser.GUEST_USER_UUID, DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));


        weight = random.nextInt(50) + 40;
        age = random.nextInt(50) + 13;
        eGymWebAppInterface.setUserInformation("test2","female","metric","27ef5a2a-1e85-4997-a4b9-bba2054bd16d",weight, age);
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertFalse(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(1, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertTrue(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals("test2",DataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING));
        Assert.assertEquals("egym-27ef5a2a-1e85-4997-a4b9-bba2054bd16d", DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        eGymWebAppInterface.resetStates();

        weight = random.nextInt(50) + 40;
        age = random.nextInt(50) + 13;
        eGymWebAppInterface.setUserInformation("test2","other","metric","",weight, age);
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertTrue(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(2, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertTrue(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals("test2",DataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING));
        Assert.assertEquals(DapiUser.GUEST_USER_UUID, DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        eGymWebAppInterface.resetStates();


        weight = random.nextInt(50) + 40;
        age = random.nextInt(50) + 13;
        eGymWebAppInterface.setUserInformation("test2","random","metric","",weight, age);
        Assert.assertFalse(DataMatrix.getBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL));
        Assert.assertTrue(DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL));
        Assert.assertEquals(0, DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        Assert.assertTrue(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL));
        Assert.assertEquals(age, DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT));
        Assert.assertEquals(weight, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
        Assert.assertEquals("test2",DataMatrix.getString(DAPIKeys.DATA_USER_FIRST_NAME_STRING));
        Assert.assertEquals(DapiUser.GUEST_USER_UUID, DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING));

        eGymWebAppInterface.resetStates();

    }
}
