package com.jht.splashapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.liberror.EC;
import com.jht.liberror.ErrorCodeManager;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends RestringActivity {
    private static final String TAG = "SplashScreenActivity";

    private Animation mSpinner;
    private JHTTimer mViewPressTimer;
    // this logic should live in a service, but for now make it static so activity lifecycle does not mess with it
    private static final SplashScreenLogic splashScreenLogic = new SplashScreenLogic();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mSpinner = AnimationUtils.loadAnimation(this, R.anim.spinning_clockwise);
        // Lock orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onResume() {
        super.onResume();

        // Ensure any active error codes are being displayed according to splash screen requirements
        int minimumWaitTimeMS = displayActiveErrorCode() ? 5000 : 0;

        // This object has a running state that will skip if it is already running.
        // We have tried figuring out the activity events for showing vs already up, but have found edge cases in practice.
        splashScreenLogic.runLogic(minimumWaitTimeMS);

        // spinner animation
        mSpinner.reset();
        Intent intent = getIntent();
        if (intent == null || intent.getBooleanExtra("animations", true)) {
            findViewById(R.id.busyIndicator).startAnimation(mSpinner);
        }

        // product dashboard
        mViewPressTimer = new JHTTimer(3000, splashScreenLogic::showSettingsDialog);
        findViewById(R.id.productLogo).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mViewPressTimer.start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                mViewPressTimer.stop();
            }
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        splashScreenLogic.onSplashPause();
        mSpinner.cancel();
        if (mViewPressTimer != null) {
            mViewPressTimer.stop();
        }
    }

    /**
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * This method is responsible for ensuring the active error code is displayed properly. The
     * logic determining whether the error code is "self-documented" at this time. :|
     * @return true if error code needs to be displayed
     */
    private boolean displayActiveErrorCode() {
        final TextView errorCodeTitle = findViewById(R.id.error_code_title);
        final TextView errorCodeText = findViewById(R.id.error_code_text);
        final TextView errorCodeMessageText = findViewById(R.id.error_code_message_text);
        boolean showError = false;

        // RGC-3515 retail should not show error codes to end users, only log them for techs
        if (!ProductConfig.isRetail() && ErrorCodeManager.hasActiveErrorCode()) {
            final EC activeErrorCode = ErrorCodeManager.activeErrorCode();
            // If this error cannot be ignored, handle it.
            if (activeErrorCode.getCode() != -1 && !SystemUtils.inDemoMode() && !activeErrorCode.canIgnore()) {
                errorCodeText.setText(String.format(Locale.ENGLISH, "%04X", activeErrorCode.getCode()));
                errorCodeMessageText.setText(activeErrorCode.getName());
                showError = true;
            }
        }
        // Apply the correct visibility to the error code elements.
        final int visibility = showError ? View.VISIBLE : View.GONE;
        errorCodeTitle.setVisibility(visibility);
        errorCodeText.setVisibility(visibility);
        errorCodeMessageText.setVisibility(visibility);
        return showError;
    }
}
