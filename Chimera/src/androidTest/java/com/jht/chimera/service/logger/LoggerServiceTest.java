package com.jht.chimera.service.logger;

import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.runner.SplashApp;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiEnvironment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

@RunWith(AndroidJUnit4.class)
public class LoggerServiceTest {

    @Before
    public void setup() {
        LaunchAppUtil.launchAndroidLauncher();
    }

    @Test
    @Chimera
    @SplashApp
    public void testPersistentAlarm() {
        try(SystemTestFixture ignored = new SystemTestFixture()) {
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, false);
            DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, DapiEnvironment.Production.ordinal());
            sleepIgnoreInterruption(5000);
            Calendar fakeStartTime = Calendar.getInstance();
            fakeStartTime.add(Calendar.DAY_OF_MONTH, -14);
            fakeStartTime.add(Calendar.SECOND, 7);
            DataMatrix.putLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG, fakeStartTime.getTime().getTime());
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, true);
            sleepIgnoreInterruption(4000);
            assertTrue(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));
            sleepIgnoreInterruption(4000);
            assertFalse(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));
        }
    }

    @Test
    @Chimera
    @SplashApp
    public void testPersistentAlarm2() {
        try(SystemTestFixture ignored = new SystemTestFixture()) {
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, false);
            DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, DapiEnvironment.Production.ordinal());
            Calendar fakeStartTime = Calendar.getInstance();
            fakeStartTime.add(Calendar.DAY_OF_MONTH, -14);
            fakeStartTime.add(Calendar.SECOND, -5);
            DataMatrix.putLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG, fakeStartTime.getTime().getTime());
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, true);
            sleepIgnoreInterruption(3000);
            assertFalse(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));
        }
    }

}
