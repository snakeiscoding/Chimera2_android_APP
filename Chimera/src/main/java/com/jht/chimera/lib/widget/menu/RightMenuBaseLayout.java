package com.jht.chimera.lib.widget.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.lib.widget.slider.SliderWidget;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Timer;
import java.util.TimerTask;

public abstract class RightMenuBaseLayout extends RelativeLayout {
    final int TIMEOUT_PERIOD_MS = 5000;

    private final DataMatrix mDataMatrix = new DataMatrix();
    protected SliderWidget mVolumeSlider;
    protected AppCompatImageButton mMuteButton;
    protected View mBackgroundOverlay;
    protected SelectronButton mCloseButton;
    protected Handler mHandler = new Handler();
    Timer mTimeoutTimer;

    public RightMenuBaseLayout(Context context) {
        super(context);
        initialize(context);
    }

    public RightMenuBaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public RightMenuBaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.right_menu_layout, this, true);

        mVolumeSlider = findViewById(R.id.audioSlider);
        mMuteButton = findViewById(R.id.btnMute);
        mBackgroundOverlay = findViewById(R.id.inactiveScreen);
        mCloseButton = findViewById(R.id.btnClose);
        findViewById(R.id.bt_pairing_button).setOnClickListener(v -> {
            LaunchAppUtil.launchBluetoothPairing();
            triggerCloseHandler();
        });


        mCloseButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                triggerCloseHandler();
            }
            return false;
        });

        mBackgroundOverlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                triggerCloseHandler();
            }
            return false;
        });

        // Create a custom adapter automation adapter to handle the custom timers if this is already
        // enabled.
        mDataMatrix.createBoolAdapter(TestingUtils.KEY, value -> {
            if (value) {
                stopGetInfoTimer();
            } else {
                restartGetInfoTimer();
            }
        });

        /* TODO Refactor
         * The volume observer on Parker seems to delay the change volume event causing the slider
         * to update its value later so it's like a delay update which looks weird
         * this issue doesn't exist on parker 3 this check is really bad and should be updated.
         */
        if (!ProductConfig.isRetail() || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
            mDataMatrix.createIntAdapter(HALKeys.DATA_SYSTEM_VOLUME_INT, value -> {
                if(ProductConfig.isLightCommercial()) {
                    restartGetInfoTimer();
                }
                mVolumeSlider.setValue(value, false);
                updateVolumeWidgets();
            });
        }

        mVolumeSlider.setRange(0, BuildConfig.MAX_VOLUME);
        mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL, audioDisabled -> {
            if (audioDisabled) {
                mVolumeSlider.setVisibility(View.INVISIBLE);
                mMuteButton.setVisibility(View.INVISIBLE);
            } else {
                mVolumeSlider.setVisibility(View.VISIBLE);
                mMuteButton.setVisibility(View.VISIBLE);
            }
        });

        //Set up bool adapter
        mDataMatrix.createBoolAdapter(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, muted -> updateVolumeWidgets());

        //Make sure DATA_SYSTEM_VOLUME_MUTE_BOOL defaults to false
        mDataMatrix.putBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, false);

        findViewById(R.id.audioLayout).setOnTouchListener((v, event) -> true);

        mMuteButton.setOnClickListener(v -> {
            SystemVolume.getInstance().toggleMute();
            updateVolumeWidgets();
        });

        setupVolumeSlider();
        init(context);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        restartGetInfoTimer();
        return super.dispatchTouchEvent(ev);
    }

    /**
     * The volume observer on Parker seems to delay the change volume event causing the slider to
     * update its value later so it's like a delay update which looks weird.
     * @param visibility The new visibility of the window.
     */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (ProductConfig.isParker() && !ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
            if (visibility == VISIBLE) {
                // just initialize the volume from the music stream
                AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                int musicStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mVolumeSlider.setValue(musicStreamVolume);
            }
        }
    }

    /**
     * @param changedView The view whose visibility changed. May be
     *                    {@code this} or an ancestor view.
     * @param visibility The new visibility, one of {@link #VISIBLE},
     *                   {@link #INVISIBLE} or {@link #GONE}.
     */
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            ((Button)findViewById(R.id.bt_pairing_button)).setText(R.string.device_pairing_tc);
            updateVolumeWidgets();
            restartGetInfoTimer();
        }
    }

    /**
     * Restart timer for closing the settings menu. If {@link TestingUtils#isAutomationEnabled()}
     * this does nothing.
     */
    private void restartGetInfoTimer() {
        // If automation is enabled, then ignore all clock interactions.
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting RightMenuBaseLayout#restartGetInfoTimer() because automation is enabled.");
            return;
        }

        stopGetInfoTimer();
        mTimeoutTimer = new Timer();
        mTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                triggerCloseHandler();
            }
        }, TIMEOUT_PERIOD_MS);
    }

    /**
     * Stop the timer for closing the settings menu. If {@link TestingUtils#isAutomationEnabled()}
     * this does nothing.
     */
    private void stopGetInfoTimer() {
        // If automation is enabled, then ignore all clock interactions.
        if (mTimeoutTimer != null) {
            mTimeoutTimer.cancel();
            mTimeoutTimer.purge();
            mTimeoutTimer = null;
        }
    }

    void triggerCloseHandler() {
        stopGetInfoTimer();
        mHandler.post(this::onCloseHandler);
    }

    /**
     * Sets up listener for volume slider. Sets volume to whatever the slider is changing to. Updates the mute icon as needed.
     */
    void setupVolumeSlider() {
        mVolumeSlider.setOnValueChangedListener(value -> {
            SystemVolume.getInstance().setVolume((int) value);
            updateVolumeWidgets();
        });
    }

    /**
     * Changes mute volume based on DM key for MUTE_BOOL
     */
    void updateVolumeWidgets() {
        if (DataMatrix.getBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL)) {
            mMuteButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_volume_mute_white));
        } else {
            mMuteButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_volume_white));
        }
    }

    abstract protected void init(Context context);

    abstract protected void onCloseHandler();
}
