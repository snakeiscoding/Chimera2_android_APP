package com.jht.chimera.ui.goscreen;

import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.SplashApp;
import com.jht.chimera.test.fixtures.SystemTestFixture;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.splashapp.SplashScreenActivity;
import com.jhtp.dapiservice.DapiEnvironment;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignInButtonTests {

    @Test @Emulator @Chimera @SplashApp
    public void testGoIsHomeSignIn() {
        try(SystemTestFixture testFixture = new SystemTestFixture()) {
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL, false);
            DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, 1964); // Need Staging or Prod 1963
            DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, DapiEnvironment.Staging.ordinal()); // Need Staging or Prod 1963
            DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, 20);
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            testFixture.startActivity(SplashScreenActivity.class);

            testFixture.waitForView("login_name_text");
            testFixture.click("buttonLogin");
            testFixture.loginUser();
            testFixture.waitForView("login_name_text");
            testFixture.checkViewGone("buttonLogin");
            sleepIgnoreInterruption(22 * 1000);
            testFixture.waitForView("buttonLogin");
        }
    }

    @Test @Emulator @Chimera @SplashApp
    public void testGoIsHomeEGYMUserLogin() {
        try(SystemTestFixture testFixture = new SystemTestFixture()) {
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL, false);
            DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, 1963); // Need Staging or Prod 1963
            DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, DapiEnvironment.Production.ordinal()); // Need Staging or Prod 1963
            DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, 30);
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            testFixture.startActivity(SplashScreenActivity.class);

            testFixture.waitForView("login_name_text");
            testFixture.click("buttonLogin");   // Click on sign in button on Go screen

            testFixture.loginEGYMUser();
            testFixture.waitForView("login_name_text");
            testFixture.checkViewGone("buttonLogin");
            sleepIgnoreInterruption(182 * 1000); // PTP Timeout is 3 minutes. This checks to make sure that timeout happens
            testFixture.waitForView("buttonLogin");
        }
    }
}