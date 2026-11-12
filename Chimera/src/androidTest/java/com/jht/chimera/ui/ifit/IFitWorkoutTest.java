package com.jht.chimera.ui.ifit;

import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.hardware.NotAutomated;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.runner.IFITTest;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IFitWorkoutTest {

    @Before
    public void setup() {
        LaunchAppUtil.launchAndroidLauncher();
    }

    @Test
    @Chimera
    @IFITTest
    @NotAutomated
    public void testIFitScreenTimeout() {
        try(SystemTestFixture testFixture = new SystemTestFixture(FrameConfigEnum.CardioSpinBike)) {
            setupMachineForIFit();
            setScreenTimeout();
            launchIFit(testFixture);
            sleepIgnoreInterruption(30000); // Wait for screen to timeout.
            testFixture.waitForView("welcome_guest_button");
            LaunchAppUtil.launchAndroidLauncher();
        }
    }

    private void launchIFit(SystemTestFixture testFixture) {
        LaunchAppUtil.launchSplashScreen();
        testFixture.waitForView("welcome_guest_icon");
        testFixture.monitorLogForCrashes();
        testFixture.click("welcome_guest_icon");
        testFixture.click("start_button");
    }

    private void setScreenTimeout() {
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, 0);
        DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, 20);
    }

    @Test
    @Chimera
    @IFITTest
    @NotAutomated
    public void testIFitWorkout() {
        runIfitWorkoutTest();
    }

    @Test
    @Chimera
    @IFITTest
    @NotAutomated
    public void testIFitNoWarmup60SecondWorkout() {
        try (
            IFitTestFixture testFixture =
                new IFitTestFixture(
                    FrameConfigEnum.CardioSpinBike,
                    35,
                    20,
                    20,
                    10,
                    10,
                    -1,
                    60
                )
        ) {
            runIFitTest(testFixture);
        }
    }

    private void runIfitWorkoutTest() {
        try (IFitTestFixture testFixture = new IFitTestFixture(FrameConfigEnum.CardioSpinBike)) {
            runIFitTest(testFixture);
        }
    }

    private void runIFitTest(IFitTestFixture testFixture) {
        setupMachineForIFit();
        launchChimera(testFixture);
        testFixture.monitorLogForCrashes(new String[] {"Play audio/video player"});
        testFixture.click("welcome_guest_button");
        //loginUser(testFixture);
        runIFitWorkout(testFixture);
        endIFitWorkout(testFixture);
        LaunchAppUtil.launchAndroidLauncher();
    }

    private void endIFitWorkout(IFitTestFixture testFixture) {
        testFixture.click("right_bar_pause_button");
        testFixture.click("right_bar_stop_button");
        testFixture.click("home_button");
        testFixture.waitForView("welcome_guest_button");
    }

    private void runIFitWorkout(IFitTestFixture testFixture) {
        testFixture.waitForView("start_button");
        // sleepIgnoreInterruption(5000); // Need to give SSO a chance to complete.
        testFixture.click("start_button");
        testFixture.clickGetStarted();
        testFixture.clickIfitGuestWorkouts();
        testFixture.clickSelectWorkout();
        testFixture.clickStartWorkout();
        testFixture.clickAcceptWorkout();
        testFixture.clickEndWarmup();
        testFixture.waitForWorkoutEnd();
    }

    private void loginUser(IFitTestFixture testFixture) {
        testFixture.click("welcome_login_icon");
        testFixture.setText("login_user_name_input", "60865866310");
        testFixture.setText("login_password_input", "1234");
        sleepIgnoreInterruption(100); // Need to give the UI a chance to catch up.
        testFixture.click("register_button");
    }

    private void launchChimera(IFitTestFixture testFixture) {
        LaunchAppUtil.launchSplashScreen();
        testFixture.waitForView("welcome_login_icon");
    }

    private void setupMachineForIFit() {
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, 0);
        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
        DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, DAPIKeys.Constants.DAPI_DEPLOYMENT_STAGING);
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, true);
    }


}
