package com.jht.chimera.ui;

import static com.jht.androidcommonalgorithms.util.ThreadHelper.sleepIgnoreInterruption;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.SplashAppQuickGo;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QuickGoWorkoutSmokeTest {
    private static final String SERIAL_SUFFIX = "00000000123";

    @Test
    @Emulator
    @Chimera
    @SplashAppQuickGo
    public void testQuickGo() {

        try(SystemTestFixture testFixture = new SystemTestFixture()) {
            for (FrameConfigEnum frameConfig : FrameConfigEnum.getSupportedFrames()) {
                LaunchAppUtil.launchAndroidLauncher();
                testFixture.setFrame(frameConfig);
                FrameConfigManager.getInstance().updateSettings();
                DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, 0);
                DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, ProductConfig.getConsoleType().getConsolePrefix() + SERIAL_SUFFIX);
                DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, FrameConfigManager.getInstance().getValidFramePrefix() + SERIAL_SUFFIX);
                DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, 60);
                DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, 0);
                DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, 0);
                DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, 0);
                DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
                LaunchAppUtil.launchSplashScreen();

                testFixture.click("welcome_guest_button");
                sleepIgnoreInterruption(1000);
                testFixture.click("quick_start_button");
                sleepIgnoreInterruption(5000);
                testFixture.click("right_bar_pause_button");
                testFixture.click("right_bar_stop_button");
                testFixture.click("home_button_image_view");
            }
        }

    }

}
