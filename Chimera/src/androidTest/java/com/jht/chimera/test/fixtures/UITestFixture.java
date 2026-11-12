package com.jht.chimera.test.fixtures;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.util.concurrent.TimeUnit;


public class UITestFixture extends BaseTestFixture {

    /**
     * This class is useful for cases where we need to click buttons on the screen, but
     * they are third party apps and we can't get the resource id.
     *
     */
    static public class Button {
        private static final String TAG = "UITestFixture";
        private final Point point;
        private final int delaySeconds;

        public Button(int delaySeconds, Point point) {
            this.delaySeconds = delaySeconds;
            this.point = point;
        }

        private void click(UiDevice device, UITestFixture testFixture) {

            if(delaySeconds > 0) {
                sleepIgnoreInterruption(delaySeconds * 1000);
                Log.d(TAG, "Clicking on button at " + point.x + " " + point.y);
                DataMatrix.sendEvent(HALKeys.EVENT_SCREEN_TOUCH);
                testFixture.dismissCrashDialogs();
                device.click(point.x, point.y);
            }
        }

    }
    private final UiDevice device;
    private final String packageName;
    private ActivityScenario<?> scenario = null;

    public UITestFixture() {
        this.device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        packageName = InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();

        IdlingPolicies.setIdlingResourceTimeout(30, TimeUnit.SECONDS);
        dismissCrashDialogs();
    }

    public <A extends Activity> UITestFixture(@NonNull Class<A> activityClass) {
        this();
        startActivity(activityClass);
    }

    public <A extends Activity> void startActivity(@NonNull Class<A> activityClass) {
        scenario = ActivityScenario.launch(activityClass);
        scenario.onActivity( activity -> {
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            dismissCrashDialogs();
        });
    }

    public void click(Button button) {
        button.click(device, this);
    }


    public UiObject2 waitForView(String resourceId) {
        UiObject2 view = getUiObject(resourceId);
        Assert.assertNotNull("Could not find " + resourceId, view);
        return view;
    }

    public UiObject2 waitForView(String resourceId, int attempts, String message) {
        UiObject2 view = getUiObject(resourceId, attempts);
        Assert.assertNotNull("Could not find " + resourceId + ". " + message, view);
        return view;
    }

    public void checkViewGone(String resourceId) {
        device.waitForIdle(10000);
        dismissCrashDialogs();
        Assert.assertNull("Found " + resourceId, device.wait(Until.findObject(By.res(packageName, resourceId)), 3000));
    }

    public void click(String resourceId) {
        UiObject2 button = waitForView(resourceId);
        button.click();
    }

    public void setText(String resourceId, String text) {
        UiObject2 textWidget = waitForView(resourceId);
        textWidget.setText(text);
    }

    @Nullable
    private UiObject2 getUiObject(String resourceId, int attempts) {
        //device.waitForIdle(10000);
        UiObject2 textWidget = null;
        for (int i = 0; textWidget == null && i < attempts; i++) {
            dismissCrashDialogs();
            textWidget = device.wait(Until.findObject(By.res(packageName, resourceId)), 10000);
        }
        return textWidget;
    }
    @Nullable
    private UiObject2 getUiObject(String resourceId) {
        return getUiObject(resourceId, 10);
    }


    public void dismissCrashDialogs() {

        while(clickButtonIfFound("Close app") || clickButtonIfFound("wait")) {
            try {
                synchronized (this) {
                    wait(500);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    private boolean clickButtonIfFound(String buttonText) {
        UiObject closeAppButton = device.findObject(new UiSelector().textContains(buttonText));
        if (closeAppButton != null && closeAppButton.exists()) {
            try {
                closeAppButton.click();
                return true;
            } catch (UiObjectNotFoundException ignored) {
            }
        }
        return false;
    }

    public void loginUser() {
        waitForView("login_user_name_input");
        setText("login_user_name_input", "60865866310");
        setText("login_password_input", "1234");
        sleepIgnoreInterruption(100); // Need to give the UI a chance to catch up.
        click("register_button");
    }

    public void loginEGYMUser() { // Assumes you just got to the login page
        waitForView("selector_text");
        clickButtonIfFound("Custom");
        waitForView("login_user_name_input");
        setText("login_user_name_input", "egymtest001@mailinator.com");
        setText("login_password_input", "fitness123");
        sleepIgnoreInterruption(100); // Need to give the UI a chance to catch up.
        click("register_button");
    }

    public void testCheckbox(ViewInteraction viewInteraction, Object dataMatrixKey) {
        viewInteraction.check(matches(DataMatrix.getBoolean(dataMatrixKey) ? isChecked() : isNotChecked())).perform(ViewActions.click());
        sleepIgnoreInterruption(100);
        viewInteraction.check(matches(DataMatrix.getBoolean(dataMatrixKey) ? isChecked() : isNotChecked()));
    }

    public String getPackageName() {
        return packageName;
    }

    @Override
    public void close() {
        super.close();
        if(scenario != null) {
            scenario.close();
        }
    }
}
