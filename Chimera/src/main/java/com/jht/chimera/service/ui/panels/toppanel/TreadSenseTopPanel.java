package com.jht.chimera.service.ui.panels.toppanel;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.service.ui.panels.IPanel;
import com.jht.chimera.service.ui.panels.PanelIds;
import com.jht.chimera.service.ui.panels.WorkoutPanelManager;
import com.jht.chimera.service.ui.panels.WorkoutPanelMode;
import com.jht.datamatrixlib.DataMatrix;

public class TreadSenseTopPanel extends LinearLayout implements IPanel {
    private int mCountdown = 10; // Should be set by whatever
    private CountDownTimer mTimer = null;
    private TextView mTreadSenseText;
    private final Handler mHandler = new Handler();
    private final DataMatrix mDataMatrix = new DataMatrix();

    public TreadSenseTopPanel(Context context) {
        super(context);
        initialize(context);
    }

    public TreadSenseTopPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public TreadSenseTopPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.treadsense_layout, this, true);

        mTreadSenseText = findViewById(R.id.tread_sense_text_view);
        //mCountdown = 10;
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_TREAD_SENSE_COUNTDOWN_SEC_INT, value -> mCountdown = value);

        mDataMatrix.createEventAdapter(UiServiceKeys.ACTION_HIDE_TREAD_SENSE_EVENT, s -> {
            stopTimer();
            WorkoutPanelManager.getInstance().showPanel(PanelIds.TREAD_SENSE_PANEL, false);
        });

        mDataMatrix.createEventAdapter(UiServiceKeys.ACTION_SHOW_TREAD_SENSE_EVENT, key -> {
            startTimer();
            WorkoutPanelManager.getInstance().showPanel(PanelIds.TREAD_SENSE_PANEL, true);
        });
    }
    @Override
    public void dispose() {
        mDataMatrix.finish();
        stopTimer();
    }

    private void startTimer() {
        stopTimer();
        mTimer = new CountDownTimer(mCountdown * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = Math.round((float) millisUntilFinished / 1000f);
                String warning = StringLookup.get(R.string.tread_sense_warning_tc);
                String tsWarning = warning.replace("%1", String.valueOf(seconds));
                mHandler.post(() -> mTreadSenseText.setText(tsWarning));
            }

            @Override
            public void onFinish() {
                DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
            }
        };
        mTimer.start();
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public void panelAdded() {
        // Do nothing
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
        // Do nothing
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        boolean isFullscreen = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN;
        if (isFullscreen) {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        } else {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            int width = (int) getResources().getDimension(R.dimen.app_container_width);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        }
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
