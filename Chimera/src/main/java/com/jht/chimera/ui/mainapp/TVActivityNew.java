package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.chimera.lib.widget.activity.HDMIActivity;
import com.jht.chimera.lib.widget.animation.AnimationHelper;
import com.jht.chimera.lib.widget.statarray.StatManager;
import com.jht.chimera.lib.widget.statbar.StatOverlay;
import com.jht.chimera.lib.widget.tv.TVControls;
import com.jht.chimera.lib.widget.tv.TVKeypad;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Locale;

public class TVActivityNew extends HDMIActivity {
    private static final String TAG = "TVActivityNew";
    // TODO: Review vision 600 hdmi.
    private Handler handler;
    private IChimeraTuner<? extends IChannel> tuner;
    private SettingKeys.TVSource source;
    private final DataMatrix dataMatrix = new DataMatrix();

    private ITunerCapabilities capabilities;
    private SurfaceView tvSurface;
    private TextureView tvTexture;
    private TVControls controls;

    private StatOverlay mStatOverlay;

    JHTTimer autoHideTimer = new JHTTimer(8000, () -> handler.post(this::hideUI));

    private boolean isShowingUI() {
        return findViewById(R.id.tv_user_interface).getVisibility() == View.VISIBLE;
    }
    private void showUI() {
        if (!isShowingUI()) {
            autoHideTimer.start();
            AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800);
        }
    }
    private void hideUI() {
        autoHideTimer.stop();
        if (isShowingUI()) AnimationHelper.hideView(findViewById(R.id.tv_user_interface), 800);
        sendExitToLg();
    }
    private void sendExitToLg() {
        // send the exit button for US8/US9
        if (source.isLG()) {
            tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.EXIT);
        }
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_new);
        handler = new Handler();

        tvSurface = findViewById(R.id.tv_player_surface);
        tvTexture = findViewById(R.id.tv_player_texture);
        mStatOverlay = findViewById(R.id.tv_stats_window);
        TVKeypad keypad = findViewById(R.id.tv_keypad_buttons);
        controls = findViewById(R.id.tv_controls);

        autoHideTimer.start();

        source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")); //NON-NLS
        tuner = TunerFactory.INSTANCE.getTuner(this, source);
        Assert.assertNotNull(tuner);
        capabilities = tuner.tuner().capabilities();

        boolean showChannelList = capabilities.channelList() && tuner.tuner().getChannelList(false).size() > 0;


        keypad.setVisibility(showChannelList ? View.GONE : View.VISIBLE);
        keypad.setTuner(tuner);
        keypad.enableDash(capabilities.dash());
        controls.setTuner(tuner, null, null);

        // update the constraint for the controls layout if needed
        // Future improvement: create some tuner setting for left/right control alignment
        if (source == SettingKeys.TVSource.US8 || source == SettingKeys.TVSource.KM6) { // so far only LG65 needs overlay on the right side
            ConstraintLayout tvUserInterface = findViewById(R.id.tv_user_interface);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(tvUserInterface);
            constraintSet.clear(R.id.tv_controls_layout, ConstraintSet.LEFT);
            constraintSet.connect(R.id.tv_controls_layout, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
            constraintSet.applyTo(tvUserInterface);
        }

        tuner.tuner().onRemoteButton(remote_button -> {
            if(remote_button.equals(ITuner.REMOTE_BUTTON.INFO)) {
                hideUI();
            }
            // TODO:
/*
            channelList.setSelection(tvController.getIndexByChannelInfo(null));
            if(channels.getAdapter() != null) {
                channels.getAdapter().notifyDataSetChanged();
            }
            play(channel.url(), channel.deinterlaced());

 */
        });

        findViewById(R.id.tv_player_channel_selector_container).setVisibility(showChannelList ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_remote_close_1).setVisibility(showChannelList ? View.GONE : View.VISIBLE);
        findViewById(R.id.tv_player_remote_close_2).setVisibility(!showChannelList ? View.GONE : View.VISIBLE);
        findViewById(R.id.tv_player_stat_button).setOnClickListener(v -> AnimationHelper.toggleVisibility(mStatOverlay));
        findViewById(R.id.tv_player_remote_close_2).setOnClickListener(v -> hideUI());
        findViewById(R.id.tv_player_remote_close_1).setOnClickListener(v -> hideUI());


        AppCompatImageButton volumeUp = findViewById(R.id.tv_player_volume_up);
        AppCompatImageButton volumeDown = findViewById(R.id.tv_player_volume_down);

        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL)) {
            volumeUp.setEnabled(false);
            volumeDown.setEnabled(false);
        } else {
            volumeUp.setOnClickListener(v -> {
                int volume = DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT);
                if (volume < 30) {
                    DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume + 1);
                }
            });

            volumeDown.setOnClickListener(v -> {
                int volume = DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT);
                if (volume > 0) {
                    DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume - 1);
                }
            });
        }


        if(showChannelList) {
            tuner.setChannelToDefault();

            SelectorItemBase channelList = new SelectorItemBase(new ArrayList<>());
            RecyclerView channels = findViewById(R.id.tv_player_channel_selector);
            tuner.createChannelSelectorItems(channelList, null, false, false);
            channelList.setSelection(tuner.getCurrentChannelIndex());

            SelectorDefaultAnimations.setDefaultAnimations(this, channels);
            StandardSelectorItemAdapter adapter = new StandardSelectorItemAdapter(this, channelList, null);
            channels.setAdapter(adapter);
            controls.setTuner(tuner, channels, channelList);
            channels.scrollToPosition(tuner.getCurrentChannelIndex());

        }

        findViewById(R.id.tv_player_surface_container).setOnTouchListener((v, event) -> {
            showUI();
            return false;
        });

        findViewById(R.id.km4_channel_info_box).setVisibility(source.equals(SettingKeys.TVSource.KM4) ? View.VISIBLE : View.GONE);

    }


    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        findViewById(R.id.tv_user_interface).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_user_interface).setAlpha(1.0f);
        autoHideTimer.reset();
        sendExitToLg(); // to exit LG apps if needed

        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);

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
                showUI();
            }
        });

        mStatOverlay.setVisibility(View.GONE);
        mStatOverlay.updateStatPositions();


        if(capabilities.hdmi()) {
            setProperties(tvSurface, tvTexture, source.equals(SettingKeys.TVSource.US4) ? 250 : 0, () -> tuner.resetSerialPort(false));
            initHDMI();
        }


        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> {
            findViewById(R.id.tv_player_stat_button).setVisibility(value == UiServiceKeys.WorkoutPanelState.REGULAR ? View.GONE : View.VISIBLE);
            mStatOverlay.setVisibility(View.GONE);
        });
        dataMatrix.createBoolAdapter(TVKeys.ACTION_SHOW_CLOSED_CAPTION_STATUS_BOOL, value -> {
            controls.setClosedCaptionIcon(value);
        });
        try {
            Locale locale = getResources().getConfiguration().getLocales().get(0);
            tuner.setLocale(locale.toLanguageTag());
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve locale", e);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideUI();
        dataMatrix.finish();
        mStatOverlay.finish();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (isShowingUI()) {
            autoHideTimer.reset();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tuner.destroy();
    }

    @Override
    protected AudioSourceInput audioSourceInput() {
        return tuner.tuner().capabilities().hdmi() ? AudioSourceInput.HDMI : AudioSourceInput.Foreground;
    }

}
