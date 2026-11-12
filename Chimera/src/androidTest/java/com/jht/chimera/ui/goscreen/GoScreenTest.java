package com.jht.chimera.ui.goscreen;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.jht.chimera.test.fixtures.BaseTestFixture.sleepIgnoreInterruption;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.MainApp;
import com.jht.chimera.test.fixtures.UITestFixture;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GoScreenTest {

    @Test @Emulator @Chimera @MainApp
    public void testSigninButton() {
        try(UITestFixture ignored = new UITestFixture(GoScreenActivity.class)) {
            onView(withId(R.id.buttonLogin));
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            DataMatrix.putString(DAPIKeys.DATA_USER_FIRST_NAME_STRING, "button should show");
            sleepIgnoreInterruption(500);
            onView(withId(R.id.buttonLogin)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
            DataMatrix.putString(DAPIKeys.DATA_USER_FIRST_NAME_STRING, "button should not show");
            sleepIgnoreInterruption(500);
            onView(withId(R.id.buttonLogin)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        }
    }
}