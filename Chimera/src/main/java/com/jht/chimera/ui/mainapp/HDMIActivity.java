package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.widget.animation.AnimationHelper;
import com.jht.chimera.lib.widget.statarray.StatManager;
import com.jht.chimera.lib.widget.statbar.StatOverlay;
import com.jht.datamatrixlib.DataMatrix;


@SuppressLint("NewApi")
public class HDMIActivity extends com.jht.chimera.lib.widget.activity.HDMIActivity {

    private Handler handler;
    private final DataMatrix dataMatrix = new DataMatrix();

    private StatOverlay mStatOverlay;
    private View hdmiInstruction;

    JHTTimer autoHideTimer = new JHTTimer(5000, () -> handler.post(this::hideUI));

    private int imageCounter = 0;
    PeriodicTimer updateInstructionTimer = new PeriodicTimer(0, 1000, () -> handler.post(() -> {
        if (hdmiInstruction.getVisibility() != View.VISIBLE) return;

        if (ProductConfig.isLightCommercial()) {
            imageCounter++;
            if (imageCounter % 5 == 0) {
                hdmiInstruction.setBackgroundResource(R.drawable.hdmi_in_01); // show for 5 sec
            } else if (imageCounter % 7 == 0) {
                hdmiInstruction.setBackgroundResource(R.drawable.hdmi_in_02); // show for 2 sec
                imageCounter = 0;
            }
        }
    }));

    private void hideUI() {
        autoHideTimer.stop();
        AnimationHelper.hideView(findViewById(R.id.tv_user_interface), 800);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hdmi);
        handler = new Handler();
        mStatOverlay = findViewById(R.id.tv_stats_window);
        hdmiInstruction = findViewById(R.id.hdmi_instruction);

        if (ProductConfig.isRetail()) {
            findViewById(R.id.dialog_container).setVisibility(View.VISIBLE); // "No HDMI Detected" box
        }

        findViewById(R.id.tv_player_stat_button).setOnClickListener(v -> AnimationHelper.toggleVisibility(mStatOverlay));
        findViewById(R.id.tv_player_surface_container).setOnTouchListener((v, event) -> {
            if (findViewById(R.id.tv_user_interface).getVisibility() == View.GONE) {
                autoHideTimer.start();
                AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800);
            }
            return false;
        });

    }

    @Override
    public void onResume() {

        autoHideTimer.start();
        findViewById(R.id.tv_user_interface).setVisibility(View.VISIBLE);

        setProperties(findViewById(R.id.tv_player_surface), findViewById(R.id.tv_player_texture), 0, null);
        initHDMI();
        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);

        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> {
            findViewById(R.id.tv_player_stat_button).setVisibility(value == UiServiceKeys.WorkoutPanelState.REGULAR ? View.GONE : View.VISIBLE);
            mStatOverlay.setVisibility(View.GONE);
        });

        mStatOverlay.start(new StatManager.IStatManagerNotifications() {
            @Override
            public void columnOpened() {
                interactionOccurred();
            }

            @Override
            public void columnClosed() {
                interactionOccurred();
            }

            private void interactionOccurred() {
                if (findViewById(R.id.tv_user_interface).getVisibility() == View.GONE) {
                    autoHideTimer.start();
                    AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800);
                }
            }
        });

        mStatOverlay.setVisibility(View.GONE);
        mStatOverlay.updateStatPositions();

        onConnectionChange();

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
        mStatOverlay.finish();
        updateInstructionTimer.stop();
        autoHideTimer.stop();
        dataMatrix.finish();

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (findViewById(R.id.tv_user_interface).getVisibility() == View.VISIBLE) {
            autoHideTimer.reset();
        }
    }


    @Override
    protected int hdmiSource() {
        return ProductConfig.getHdmiInput(null);
    }

    @Override
    protected void onConnectionChange() {
        handler.postDelayed(() -> {
            if (connected()) {
                hdmiInstruction.setVisibility(View.GONE);
            } else {
                if (hdmiInstruction.getVisibility() != View.VISIBLE) {
                    imageCounter = 0;
                    if (ProductConfig.isLightCommercial()) {
                        hdmiInstruction.setBackgroundResource(R.drawable.hdmi_in_02);
                    }
                    updateInstructionTimer.start();
                    hdmiInstruction.setVisibility(View.VISIBLE);
                }
            }
        }, 3000);
    }

}