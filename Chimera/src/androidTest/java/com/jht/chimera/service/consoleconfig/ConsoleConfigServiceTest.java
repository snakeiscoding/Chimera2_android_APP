package com.jht.chimera.service.consoleconfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.chimera.test.annotations.runner.MainApp;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Here we can test sending settings to the console config service.  Basically we pass it XML
 * and make sure the Data Matrix keys are correct.  Also we can test malformed XML.
 *
 * Currently this tests the following settings.
 *      Enable MFI
 *      Enable Gymkit
 */
public class ConsoleConfigServiceTest {


    /**
     * Tests the console skin function in the console configuration.  We setup the XML so that it
     * downloads the zip file from the assets.  The file contains only one text file to check.  The
     * test cases are as follows.
     *
     * 1. Grab the console config and make sure the skin is downloaded to the correct place.
     * 2. Retry test 1.  Should result in a 304 so skin is not downloaded.
     * 3. Download config, but version matches the last version.  Should not download.
     * 4. Download config with versions not matching.  Should download config.
     *
     * @throws IOException  General IO exception which should fail the test.
     */
    @Test @Emulator @MainApp @Chimera
    public void testConsoleSkin() throws IOException {
        try(ConsoleConfigServiceTestFixture testFixture = new ConsoleConfigServiceTestFixture()) {
            testFixture.runSkinTest("0", "consoleconfig/skin_test.xml", true);
            testFixture.runSkinTest("0", "consoleconfig/skin_test.xml", false);
            testFixture.runSkinTest("2", "consoleconfig/skin_test2.xml", false);
            testFixture.runSkinTest("2", "consoleconfig/skin_test3.xml", true);
        }
    }

    /**
     * For this test we make iFit the featured workout.  We then load a configuration with iFit
     * enabled and see if that is the featured workout.  After that we load a configuration with
     * iFit disabled and make sure Sprint 8 is the featured workout.
     *
     * amended to test more remote configs than just iFit
     *
     * @throws IOException  General IO exception which should fail the test.
     */
    @Test @Emulator @MainApp @Chimera
    public void testIFITSettings() throws IOException {
        try(ConsoleConfigServiceTestFixture testFixture = new ConsoleConfigServiceTestFixture()) {
            //testFixture.disableIFIT();
            //testFixture.makeIFITTheFeaturedWorkout();
            testFixture.testConfig("consoleconfig/ifit_enabled.xml");
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_ASCENT_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL));
            Assert.assertEquals("test.url/321", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
            Assert.assertEquals("test.url/321", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING));


            testFixture.testConfig("consoleconfig/ifit_disabled.xml");
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL));
            Assert.assertEquals("test.url/123", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
            Assert.assertEquals("test.url/123", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING));
        }
    }

    /**
     * Test to make sure the remote config settings we send are being received, and that they populate
     * the appropriate datamatrix keys
     *
     * @throws IOException  General IO exception which should fail the test.
     */
    @Test @Emulator @MainApp @Chimera
    public void testRemoteConfigSettings() throws IOException {
        try(ConsoleConfigServiceTestFixture testFixture = new ConsoleConfigServiceTestFixture()) {
            testFixture.testConfig("consoleconfig/remoteConfigTestA.xml");
            Assert.assertEquals("ml.test.url/123", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL));
            Assert.assertEquals("test.url/123", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
            Assert.assertEquals("test.url/123", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL));
            Assert.assertEquals("production", DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING));
            Assert.assertNotEquals("not prod", DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_PTP_BOOL));
            assertTrue(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_RFID_BOOL));

            testFixture.testConfig("consoleconfig/remoteConfigTestB.xml");
            Assert.assertEquals("ml.test.url/456", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL));
            Assert.assertNotEquals("test.url/321", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
            Assert.assertNotEquals("test.url/321", DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL));
            Assert.assertEquals("development", DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING));
            Assert.assertNotEquals("not dev", DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_URLS_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_PTP_BOOL));
            assertFalse(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_RFID_BOOL));

        }
    }

    /**
     * Test the MFI and gymkit options in the console configuration service.
     *
     * @throws IOException  Thrown if there is an error opening the configuration XML from the assets.
     */
    @Test @Emulator @Default @Chimera
    public void testMFISettings() throws IOException {
        try(ConsoleConfigServiceTestFixture testFixture = new ConsoleConfigServiceTestFixture()) {
            // Enable the settings before running the test.
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL, true);

            testFixture.runAppleTest("consoleconfig/apple_disable.xml", false, false);
            testFixture.runAppleTest("consoleconfig/apple_enable.xml", true, true);
            testFixture.runAppleTest("consoleconfig/gymkit_disable.xml", true, false);
        }
    }

    @Test @Emulator @Default @Chimera
    public void testPersistentLog() throws IOException {
        try(ConsoleConfigServiceTestFixture testFixture = new ConsoleConfigServiceTestFixture()) {
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, false);
            testFixture.runPersistentTest("consoleconfig/persistent_log_enable.xml", true);
            testFixture.runPersistentTest("consoleconfig/persistent_log_disable.xml", false);
        }
    }

}
