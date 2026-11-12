package com.jht.chimera.service.ui.panels;

import static com.jht.progression.MmProgramConstantsKt.PROGRESSION_APP_UUID;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataMatrix;

/**
 * The countdown for starting a workout.
 * <p>
 * Created by chris.foyer on 9/1/2017.
 */
public class CountdownPanel extends FrameLayout implements IPanel {
    public TextView countdownText;
    private final View countdownBackground;
    private TextView quoteText; //This text view is only used for Matrix Method

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean shouldBeep;
    private final Context context;

    public CountdownPanel(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.countdown_layout, this, true);
        countdownBackground = findViewById(R.id.countdown_background);
        countdownText = findViewById(R.id.countdown_text_view);
        quoteText = findViewById(R.id.quote_text_view);
        this.context = context;
    }

    /**
     * This function is setup for Matrix Method during the countdown time
     */
    private void handleProgressionUiStart() {
        String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);

        if (appId.equals(PROGRESSION_APP_UUID)) {
            if (quoteText != null) {
                quoteText.setText(R.string.are_you_ready_uc);
                quoteText.setVisibility(VISIBLE);
                countdownText.setTextColor(0xFF00DBFF);
                countdownBackground.setVisibility(VISIBLE);
                //setting these programmatically for progression so that other areas using the countdown panel are not affected
                Typeface oxaniumBoldTypeface = ResourcesCompat.getFont(context, R.font.oxanium_bold);
                quoteText.setTextSize(48);
                quoteText.setTypeface(oxaniumBoldTypeface);
                countdownText.setTextSize(300);
                countdownText.setTypeface(oxaniumBoldTypeface);
            }
        }
        else {
            quoteText.setVisibility(INVISIBLE);
            countdownText.setTextColor(Color.WHITE);
        }
    }

    /**
     * This function is to undo progression specific changes for Matrix Method during the countdown time, after the countdown finishes
     */
    private void handleProgressionUiStop() {
        String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);

        if (appId.equals(PROGRESSION_APP_UUID) && quoteText != null) {
            quoteText.setTextSize(50);
            quoteText.setTypeface(null);
            countdownText.setTextSize(400);
            countdownText.setTypeface(null);
        }
    }

    /**
     * Starts a countdown dialog from 3 to 1
     */
    public void startCountdown() {
        handleProgressionUiStart();
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OPEN_COUNTDOWN_POPOVER.name());
        countdownText.setText("");
        countdownBackground.setVisibility(VISIBLE);
        shouldBeep = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_COUNTDOWN_BEEP_BOOL); // this will not change during the countdown

        long now = SystemClock.uptimeMillis();
        mHandler.postAtTime(mCountdown3Runnable, now + 1000);
        mHandler.postAtTime(mCountdown2Runnable, now + 2000);
        mHandler.postAtTime(mCountdown1Runnable, now + 3000);
        mHandler.postAtTime(() -> {
            countdownText.setText("");
            countdownBackground.setVisibility(INVISIBLE);
            quoteText.setVisibility(INVISIBLE);
            WorkoutPanelManager.getInstance().showPanel(PanelIds.COUNTDOWN_PANEL, false);
            DataMatrix.putBoolean(UiServiceKeys.ACTION_START_WORKOUT_UI_READY_BOOL, true); // should this be in UI service?
        }, now + 4000);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(3000);
        alphaAnimation.setStartOffset(1000);
        countdownBackground.startAnimation(alphaAnimation);
    }

    public void stopCountdown() {
        handleProgressionUiStop();
        mHandler.removeCallbacksAndMessages(null);
        countdownText.setText("");
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CLOSE_COUNTDOWN_POPOVER.name());
    }

    private final Runnable mCountdown3Runnable = () -> {
        countdownText.setText("3");
        if (shouldBeep) DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
    };

    private final Runnable mCountdown2Runnable = () -> {
        countdownText.setText("2");
        if (shouldBeep) DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
    };

    private final Runnable mCountdown1Runnable = () -> {
        countdownText.setText("1");
        if (shouldBeep) DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
    };

    @Override
    public void panelAdded() {
        mHandler.postDelayed(this::startCountdown, 100); // do not slow down the panel manager, just queue the count start
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
        stopCountdown();
    }

    @Override
    public void dispose() {}

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        int width = WindowManager.LayoutParams.MATCH_PARENT;
        int height = WindowManager.LayoutParams.MATCH_PARENT;

        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, height,
                ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        viewParams.gravity = Gravity.NO_GRAVITY;
        viewParams.x = 0;
        viewParams.y = 0;
        return viewParams;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }

}
