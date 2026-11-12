package com.jht.chimera.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.jht.chimera.test.espresso.drawables.DrawableChecks.withDrawable;

import android.content.Context;
import android.provider.Settings;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MainApp;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.chimera.ui.mainapp.WelcomeActivity;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WelcomeScreenTest {


    @Test @Emulator @Chimera @MainApp
    public void testSamsungBadge() {

        try(UITestFixture ignored = new UITestFixture(WelcomeActivity.class)) {

            // When disconnected make sure badge shows, indicator does not, and that the user can touch the
            // badge to bring up the dialog.
            DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
            onView(withId(R.id.samsung_icon_indicator)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.gymkit_badge)).check(matches(isDisplayed()));
            onView(withId(R.id.samsung_badge)).check(matches(isDisplayed())).perform(click());
            onView(withId(R.id.watch_dialog_close_button)).inRoot(isSystemAlertWindow()).check(matches(isDisplayed())).perform(click());

            // Make sure the badge goes away and the indicator is in the correct state when connecting.
            DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Connecting.ordinal());
            onView(withId(R.id.samsung_badge)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.gymkit_badge)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.samsung_icon_indicator)).check(matches(isDisplayed())).check(withDrawable(R.drawable.ic_icon_samsung_connecting, true));

            // Make sure the badge is still not there and the indicator is in the correct state when connected
            DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Connected.ordinal());
            onView(withId(R.id.samsung_icon_indicator)).check(matches(isDisplayed())).check(withDrawable(R.drawable.ic_icon_samsung_connected, true));
            onView(withId(R.id.samsung_badge)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            onView(withId(R.id.gymkit_badge)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

            // Reset to the disconnected state and make sure UI is still good.
            DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
            onView(withId(R.id.samsung_badge)).check(matches(isDisplayed()));
            onView(withId(R.id.gymkit_badge)).check(matches(isDisplayed()));
            onView(withId(R.id.samsung_icon_indicator)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }
    }

    @Test @Emulator @Chimera @MainApp
    public void testGoScreenLaunch() {
        try(UITestFixture ignored = new UITestFixture(WelcomeActivity.class)) {
            onView(withId(R.id.welcome_guest_button)).perform(click());
            onView(withId(R.id.quick_start_button)).check(matches(isDisplayed()));
        }

    }

    @Test @Emulator @Chimera @MainApp
    public void testScreensaverFunction() {
        // Used to allow the screensaver function to work
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, 5);
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT, SettingKeys.AssetManagementConstants.SCREEN_BRIGHTNESS_AFTER_TIMEOUT_0_MIN);

        try(UITestFixture testFixture = new UITestFixture(WelcomeActivity.class)) {
            SystemBrightness.setSystemBrightness(testFixture.getTargetContext().getContentResolver(), 100);

            try { // Wait for screen to start and dim (set to 5 seconds hide)
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check that we show the touch icon
            onView(withId(R.id.welcome_touch_icon)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

            Context context = getInstrumentation().getTargetContext();
            int currentBrightness = -1;
            try {
                currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            // Check that the brightness is reduced according to what it should be
            assertThat(currentBrightness, new IsEqual<>(SystemBrightness.getSystemBrightness(DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT))));

            // Wait ten seconds, touch icon goes off after ten seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check that we show/hide the touch icon
            onView(withId(R.id.welcome_touch_icon)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }
    }
}