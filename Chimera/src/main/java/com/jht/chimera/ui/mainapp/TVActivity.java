package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SimpleArraySelectorItem;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.lib.widget.activity.HDMIActivity;
import com.jht.chimera.lib.widget.animation.AnimationHelper;
import com.jht.chimera.lib.widget.statarray.StatManager;
import com.jht.chimera.lib.widget.statbar.StatOverlay;
import com.jht.chimera.lib.widget.tv.TVAppKeypad;
import com.jht.chimera.service.hal.lib.hardware.HdmiIn;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.mainapp.tv.SKNETChannelSelectorItem;
import com.jht.chimera.ui.mainapp.tv.TVChannelSelectorItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.iptvplayer.IPTVPlayer;
import com.jht.iptvplayer.ITrack;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TVActivity extends HDMIActivity {
    @NonNls private final String TAG = "TVActivity";

    private Handler handler;
    private TVControllerBase tvController;
    private SettingKeys.TVSource source;
    private IPTVPlayer iptvPlayer;
    private final DataMatrix dataMatrix = new DataMatrix();

    private WifiManager.MulticastLock multicastLock;
    private final SelectorItemBase channelList = new SelectorItemBase(new ArrayList<>());
    private ITunerCapabilities capabilities;
    private SurfaceView tvSurface;
    private TextureView tvTexture;

    private DialogBase ccPopup = null;
    private DialogBase audioPopup = null;

    private final Point containedSize = new Point();
    private final Point fullScreenSize = new Point();

    private LocalServerSocket closedCaptionServerSocket;
    private LocalSocket closedCaptionSocket;

    private StatOverlay mStatOverlay;
    private ImageView imageViewDolbyIcon;

    @NonNls final String DDP_LIB_EVENT = "com.nex.DDP_LIB_EVENT";

    private final BroadcastReceiver dolbyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: received action = " + intent.getAction());
            if (intent.getAction().equals(DDP_LIB_EVENT)) {
                Log.d(TAG, "onReceive: received Dolby broadcast");
                imageViewDolbyIcon.setVisibility(View.VISIBLE);
                dolbyIconHideTimer.reset();
            }
        }
    };

    JHTTimer autoHideTimer = new JHTTimer(8000, () -> handler.post(this::hideUI));

    private void hideUI() {
        autoHideTimer.stop();
        AnimationHelper.hideView(findViewById(R.id.tv_user_interface), 800);
        if (ccPopup != null) {
            ccPopup.hide();
            ccPopup = null;
        }
        if (audioPopup != null) {
            audioPopup.hide();
            audioPopup = null;
        }

    }

    JHTTimer dolbyIconHideTimer = new JHTTimer(5000, () -> handler.post(this::hideDolbyIcon));

    private void hideDolbyIcon() {
        dolbyIconHideTimer.stop();
        imageViewDolbyIcon.setVisibility(View.INVISIBLE);
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv);
        handler = new Handler();

        tvSurface = findViewById(R.id.tv_player_surface);
        tvTexture = findViewById(R.id.tv_player_texture);
        mStatOverlay = findViewById(R.id.tv_stats_window);
        imageViewDolbyIcon = findViewById(R.id.dolby_icon);
        imageViewDolbyIcon.setVisibility(View.INVISIBLE);

        IntentFilter dolbyIntentFilter = new IntentFilter();
        dolbyIntentFilter.addAction(DDP_LIB_EVENT);
        this.getApplicationContext().registerReceiver(dolbyReceiver, dolbyIntentFilter);

        autoHideTimer.start();

        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> {
            findViewById(R.id.tv_player_stat_button).setVisibility(value == UiServiceKeys.WorkoutPanelState.REGULAR ? View.GONE : View.VISIBLE);
            mStatOverlay.setVisibility(View.GONE);
        });

        source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)); //NON-NLS
        tvController = TVControllerFactory.getTVController(this, source);
        if(source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.KM4) || source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2)) {
            tvController.open();
            handler.postDelayed(() -> tvController.muteOff(), 500);
            handler.postDelayed(() -> {
                if(ProductConfig.getTunerDefaultVolume(source) > 0) {
                    tvController.setTVVolume(ProductConfig.getTunerDefaultVolume(source));
                }
                tvController.setDefaultChannel();
            }, 1000);
        }
        if(source.equals(SettingKeys.TVSource.STB)) {
            tvController.open();
        }
        // for SK Net set top box
        if(source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET"))
            tvController.setDefaultChannel();

        capabilities = tvController.capabilities();
        boolean showChannelList = capabilities.channelList() && tvController.channels().size() > 0;

        // for SK Net set top box
        if(source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET"))
            showChannelList = capabilities.channelList() && tvController.sknetChannels().size() > 0;

        Log.d(TAG, "showChannelList " + showChannelList);

        findViewById(R.id.tv_keypad_buttons).setVisibility(showChannelList ? View.GONE : View.VISIBLE);
        findViewById(R.id.tv_player_channel_selector_container).setVisibility(showChannelList ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_closed_captioning).setVisibility(capabilities.cc() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_last_channel).setVisibility(capabilities.lastChannel() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_sap).setVisibility(capabilities.sap() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_remote_info).setVisibility(capabilities.info() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_remote_close_1).setVisibility(showChannelList ? View.GONE : View.VISIBLE);
        findViewById(R.id.tv_player_remote_close_2).setVisibility(!showChannelList ? View.GONE : View.VISIBLE);
        //findViewById(R.id.tv_remote_guide).setVisibility(capabilities.guide() ? View.VISIBLE : View.GONE);
        //findViewById(R.id.tv_remote_back).setVisibility(capabilities.back() ? View.VISIBLE : View.GONE);

        if (capabilities.lastChannel() || capabilities.cc() || capabilities.sap() /*|| capabilities.guide() || capabilities.back()*/) {
            findViewById(R.id.tv_player_additional_remote_buttons).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tv_player_additional_remote_buttons).setVisibility(View.GONE);
        }

        RecyclerView channels = findViewById(R.id.tv_player_channel_selector);

        // Add delay for waiting for change tvController
        // if CAB checkbox is checked, keypad doesn't support dash function
        TVAppKeypad keypad = findViewById(R.id.tv_keypad_buttons);
        handler.postDelayed(() -> {
            keypad.setOnKeyPress(tvController);
            keypad.enableDash(capabilities.dash());
        }, 1500);
        boolean finalShowChannelList = showChannelList;
        dataMatrix.createEventAdapter(SettingKeys.ProjectOne.EVENT_LAST_CHANNEL_TV, event -> {
            tvController.lastChannel();
            if (finalShowChannelList) {
                if (capabilities.hdmi()) {
                    channelList.setSelection(tvController.getIndexByChannelInfo(null));
                    if (channels.getAdapter() != null) {
                        channels.getAdapter().notifyDataSetChanged();
                    }
                } else {
                    // For IPTV
                    imageViewDolbyIcon.setVisibility(View.INVISIBLE);
                    channelList.setSelection(tvController.getIndexByChannelInfo(null));
                    if (channels.getAdapter() != null) {
                        channels.getAdapter().notifyDataSetChanged();
                    }
                    ChannelInfo channel = tvController.currentChannel();
                    play(channel.url(), channel.deinterlaced());
                }
            }
        });

        dataMatrix.createEventAdapter(SettingKeys.ProjectOne.EVENT_PREVIOUS_CHANNEL_TV, event -> {
            if(finalShowChannelList) {
                tvController.channelUp();
                if(capabilities.hdmi()) {
                    handler.postDelayed(() -> {
                        channelList.setSelection(tvController.getIndexByChannelInfo(null));
                        if(channels.getAdapter() != null) {
                            channels.getAdapter().notifyDataSetChanged();
                        }
                    }, 300);
                } else {
                    // For IPTV
                    imageViewDolbyIcon.setVisibility(View.INVISIBLE);
                    handler.postDelayed(() -> {
                        ChannelInfo channel = tvController.currentChannel();
                        if(channel != null) {
                            channelList.setSelection(tvController.getIndexByChannelInfo(null));
                            if(channels.getAdapter() != null) {
                                channels.getAdapter().notifyDataSetChanged();
                            }
                            play(channel.url(), channel.deinterlaced());
                        }
                    }, 300);

                }
            } else {
                tvController.channelDown();
            }
        });

        dataMatrix.createEventAdapter(SettingKeys.ProjectOne.EVENT_NEXT_CHANNEL_TV, event -> {
            if(finalShowChannelList) {
                tvController.channelDown();
                if(capabilities.hdmi()) {
                    handler.postDelayed(() -> {
                        channelList.setSelection(tvController.getIndexByChannelInfo(null));
                        if(channels.getAdapter() != null) {
                            channels.getAdapter().notifyDataSetChanged();
                        }
                    }, 300);
                } else {
                    // For IPTV
                    imageViewDolbyIcon.setVisibility(View.INVISIBLE);
                    handler.postDelayed(() -> {
                        ChannelInfo channel = tvController.currentChannel();
                        if(channel != null) {
                            channelList.setSelection(tvController.getIndexByChannelInfo(null));
                            if(channels.getAdapter() != null) {
                                channels.getAdapter().notifyDataSetChanged();
                            }
                            play(channel.url(), channel.deinterlaced());
                        }
                    }, 300);
                }
            } else {
                tvController.channelUp();
            }
        });

        findViewById(R.id.tv_player_channel_down).setOnClickListener(v -> {
            DataMatrix.sendEvent(SettingKeys.ProjectOne.EVENT_PREVIOUS_CHANNEL_TV);
        });

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

        findViewById(R.id.tv_player_channel_up).setOnClickListener(v -> {
            DataMatrix.sendEvent(SettingKeys.ProjectOne.EVENT_NEXT_CHANNEL_TV);
        });

        findViewById(R.id.tv_player_last_channel).setOnClickListener(v -> {
            DataMatrix.sendEvent(SettingKeys.ProjectOne.EVENT_LAST_CHANNEL_TV);
        });

        findViewById(R.id.tv_player_closed_captioning).setOnClickListener(v -> {
            if (capabilities.hdmi()) {
                tvController.toggleCC();
            } else if (iptvPlayer != null && tvController.channels().size() > 0) {
                List<ITrack> subtitileTracks = iptvPlayer.getSubtitleTracks();
                if (subtitileTracks != null && subtitileTracks.size() > 0) {
                    String[] trackNames = new String[subtitileTracks.size()];
                    for (int i = 0; i < subtitileTracks.size(); i++) {
                        trackNames[i] = subtitileTracks.get(i).name();
                    }
                    SimpleArraySelectorItem selectorItem = new SimpleArraySelectorItem(trackNames);
                    ccPopup = ProjectDialogFactory.popupSelection(v, new SelectorAdapterBase(TVActivity.this, selectorItem, item -> {
                        iptvPlayer.selectTrack(subtitileTracks.get(item.getIndex()));
                        if (ccPopup != null) {
                            ccPopup.hide();
                            ccPopup = null;
                        }
                    }), 0, 300, -2, 0, 0, DialogBase.CONSTRAIN_TO_ANCHOR.NONE);
                }
            }

        });
        findViewById(R.id.tv_player_sap).setOnClickListener(v -> {
            if (capabilities.hdmi()) {
                tvController.toggleSAP();
            } else if (iptvPlayer != null && tvController.channels().size() > 0) {

                List<ITrack> audioTracks = iptvPlayer.getAudioTracks();
                if (audioTracks != null && audioTracks.size() > 0) {
                    String[] trackNames = new String[audioTracks.size()];
                    for (int i = 0; i < audioTracks.size(); i++) {
                        trackNames[i] = audioTracks.get(i).name();
                    }
                    SimpleArraySelectorItem selectorItem = new SimpleArraySelectorItem(trackNames);
                    audioPopup = ProjectDialogFactory.popupSelection(v, new SelectorAdapterBase(TVActivity.this, selectorItem, item -> {
                        iptvPlayer.selectTrack(audioTracks.get(item.getIndex()));
                        if (audioPopup != null) {
                            audioPopup.hide();
                            audioPopup = null;
                        }
                    }), 0, 300, -2, 0, 0, DialogBase.CONSTRAIN_TO_ANCHOR.NONE);
                }
            }
        });
        findViewById(R.id.tv_remote_info).setOnClickListener(v -> {
            tvController.info();
            hideUI();
        });

        findViewById(R.id.tv_remote_guide).setOnClickListener(v -> {
            tvController.guide();
        });

        findViewById(R.id.tv_remote_back).setOnClickListener(v -> {
            tvController.back();
        });
        findViewById(R.id.tv_player_stat_button).setOnClickListener(v -> AnimationHelper.toggleVisibility(mStatOverlay));

        findViewById(R.id.tv_player_remote_close_2).setOnClickListener(v -> hideUI());

        findViewById(R.id.tv_player_remote_close_1).setOnClickListener(v -> hideUI());

        if(showChannelList) {
            // for SK Net set top box
            if(source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET")) {
                for (SKNETChannelInfo sknetChannelInfo : tvController.sknetChannels()) {
                    channelList.addItem(new SKNETChannelSelectorItem(sknetChannelInfo, null));
                }
            } else {
                for (ChannelInfo channelInfo : tvController.channels()) {
                    channelList.addItem(new TVChannelSelectorItem(channelInfo, null));
                }
            }
            channelList.setSelection(tvController.getIndexByChannelInfo(null));
//            RecyclerView channels = findViewById(R.id.tv_player_channel_selector);
            SelectorDefaultAnimations.setDefaultAnimations(this, channels);
            channels.setAdapter(new StandardSelectorItemAdapter(this, channelList, item -> {
                // for SK Net set top box
                if(source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET")) {
                    if (item instanceof SKNETChannelSelectorItem) {
                        hideUI();
                        tvController.setSknetChannel(((SKNETChannelSelectorItem) item).sknetChannelInfo());
                    }
                } else {
                    if (item instanceof TVChannelSelectorItem) {
                        if (source.equals(SettingKeys.TVSource.JP0))
                            hideUI();
                        tvController.setChannel(((TVChannelSelectorItem) item).channelInfo());
                        if (!capabilities.hdmi()) {
                            imageViewDolbyIcon.setVisibility(View.INVISIBLE);
                            play(((TVChannelSelectorItem) item).channelInfo().url(), ((TVChannelSelectorItem) item).channelInfo().deinterlaced());
                        }
                    }
                }
            }));
        }

        findViewById(R.id.tv_player_surface_container).setOnTouchListener((v, event) -> {
            if (findViewById(R.id.tv_user_interface).getVisibility() == View.GONE) {
                autoHideTimer.start();
                if(source.equals(SettingKeys.TVSource.KM4))
                    AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800, 0.5f);
                else
                    AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800);
            }
            return false;
        });

        volumeDown.setImageResource(source.equals(SettingKeys.TVSource.JP0) || (source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET")) ? R.drawable.ic_arrow_down_black_80dp : R.drawable.ic_arrow_left_black_80dp);
        volumeUp.setImageResource(source.equals(SettingKeys.TVSource.JP0) || (source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET")) ? R.drawable.ic_arrow_upward_black_80dp : R.drawable.ic_arrow_right_black_80dp);

        //if(source.equals(SettingKeys.TVSource.JP0) || (source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET"))) {
        //    findViewById(R.id.tv_player_remote_button_row).setVisibility(View.GONE);
        //    findViewById(R.id.tv_player_jp_closed_captioning).setVisibility(source.equals(SettingKeys.TVSource.JP0) ? View.VISIBLE : View.GONE);
        //} else {
        //    findViewById(R.id.tv_player_remote_button_row).setVisibility(View.VISIBLE);
        //    findViewById(R.id.tv_player_jp_closed_captioning).setVisibility(View.GONE);
        //}

        // findViewById(R.id.tv_player_jp_closed_caption_button).setOnClickListener(v -> tvController.toggleCC());

        if(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING).equals(SettingKeys.TVSource.IPTV.name())) {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifi != null) {
                multicastLock = wifi.createMulticastLock("tvmulticastlock");
                multicastLock.setReferenceCounted(true);
            }
        }


        new Thread(() -> {
            try {
                JHTTimer ccHideTimer = new JHTTimer(10000, () -> handler.post(() -> findViewById(R.id.closed_caption_text).setVisibility(View.GONE)));
                closedCaptionServerSocket = new LocalServerSocket("com.jht.vlc.closed_caption");
                byte[] buffer = new byte[4096];
                while (true) {
                    closedCaptionSocket = closedCaptionServerSocket.accept();
                    if (closedCaptionSocket == null) {
                        return;
                    }
                    int bytes;
                    StringBuilder message = new StringBuilder();
                    while ((bytes = closedCaptionSocket.getInputStream().read(buffer)) > 0) {
                        int startByte = 0;
                        for (int i = 0; i < bytes; i++) {
                            if (buffer[i] == (byte) 0) {
                                message.append(new String(buffer, startByte, i - startByte));
                                startByte = i + 1;
                                updateCC(message.toString());
                                ccHideTimer.start();
                                message.setLength(0);
                            }
                        }
                        if (startByte < bytes) {
                            message.append(new String(buffer, startByte, bytes));
                        }
                    }

                }
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }).start();

        // if lvdsValue is 1, console size is 21.5", if lvdsValue is 0, console size is 15.6"
        int lvdsSize = checkConsoleSize();
        int newHeight = (lvdsSize == 1) ? 200 : 140;
        View view = findViewById(R.id.km4_channel_info_box);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = newHeight;
        view.setLayoutParams(layoutParams);

        findViewById(R.id.km4_channel_info_box).setVisibility(source.equals(SettingKeys.TVSource.KM4) ? View.VISIBLE : View.GONE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);

        // Move user interface to right side to prevent overlap SDDTMB's OSD channel list (at left side)
        if(source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SDDTMB")) {
            ConstraintLayout tvUserInterface = findViewById(R.id.tv_user_interface);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(tvUserInterface);
            constraintSet.clear(R.id.tv_controls_layout, ConstraintSet.LEFT);
            constraintSet.connect(R.id.tv_controls_layout, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
            constraintSet.applyTo(tvUserInterface);
        }
    }

    private void updateCC(String message) {

        handler.post(() -> {
            ((TextView) findViewById(R.id.closed_caption_text)).setText(message);
            findViewById(R.id.closed_caption_text).setVisibility(View.VISIBLE);
        });
    }

    private void play(final String url, boolean deinterlace) {
        iptvPlayer.play(Uri.parse(url), false); // Removing deinterlacing feature https://jhtnaee.atlassian.net/browse/UIPTV-161.

    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        findViewById(R.id.tv_user_interface).setVisibility(View.VISIBLE);
        findViewById(R.id.tv_user_interface).setAlpha(source.equals(SettingKeys.TVSource.KM4) ? 0.5f : 1.0f);
        autoHideTimer.reset();

        TextureView vlcSurface = findViewById(R.id.vlc_player_surface);
        ITunerCapabilities capabilities = tvController.capabilities();

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
                if (findViewById(R.id.tv_user_interface).getVisibility() == View.GONE) {
                    autoHideTimer.start();
                    if(source.equals(SettingKeys.TVSource.KM4))
                        AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800, 0.5f);
                    else
                        AnimationHelper.showView(findViewById(R.id.tv_user_interface), 800);
                }
            }
        });

        mStatOverlay.setVisibility(View.GONE);
        mStatOverlay.updateStatPositions();

        boolean disableCheckResolution;
        disableCheckResolution = source.equals(SettingKeys.TVSource.STB) && tvController.getSelectedSetTopBox().contains("SK NET");
        Log.d(TAG, "TVActivity onResume: disableCheckResolution = "+ disableCheckResolution);

        if(capabilities.hdmi()) {
            setProperties(tvSurface, tvTexture, 0, () -> {
                tvController.destroy();
                tvController = TVControllerFactory.getTVController(getApplicationContext(), source);
                tvController.open();
            } );
            initHDMI();
        } else {
            vlcSurface.bringToFront();
            int iptvPlayerType = DataMatrix.getInt(SettingKeys.Experimental.DATA_IPTV_PLAYER_TYPE_INT);
            if(iptvPlayerType == SettingKeys.IPTVPlayerType.VLC.ordinal())
                Log.d(TAG, "show: iptv player type = VLC");
            else if(iptvPlayerType == SettingKeys.IPTVPlayerType.ANDROID.ordinal())
                Log.d(TAG, "show: iptv player type = ANDROID");

            ArrayList<String> options = new ArrayList<>();
            options.add("--avcodec-skiploopfilter=4"); // NON-NLS
            iptvPlayer = new IPTVPlayer(this, /*USE_VLC*/ iptvPlayerType == SettingKeys.IPTVPlayerType.VLC.ordinal() ? IPTVPlayer.PlayerMode.VLC : IPTVPlayer.PlayerMode.Android, tvSurface, vlcSurface, findViewById(R.id.tv_subtitles_surface), options, DataMatrix.getInt(SettingKeys.Experimental.DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT));
            iptvPlayer.setVolume(0.26f);
            iptvPlayer.setVlcVolume(26);
            findViewById(R.id.tv_subtitles_surface).bringToFront();

            ChannelInfo channel = tvController.currentChannel();
            if (channel != null) {
                // Note we need to change the window size when we go back and forth between full screen
                // and not full screen.  To get the size of the non full screen we have to wait for a little
                // bit otherwise the measured width and height will be 0.
                handler.postDelayed(() -> {
                    WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    if(window != null) {
                        Display display = window.getDefaultDisplay();
                        display.getRealSize(fullScreenSize);
                        containedSize.x = tvSurface.getMeasuredWidth();
                        containedSize.y = tvSurface.getMeasuredHeight();
                    }
                    play(channel.url(), channel.deinterlaced());
                    if (DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_IPTV_USE_SAP_BOOL)) {
                        iptvPlayer.browseForMedia((index, url, name, added) -> {
                            for (ChannelInfo channelInfo : tvController.channels()) {
                                if (channelInfo.name().equalsIgnoreCase(name)) {
                                    if (!added) {
                                        tvController.deleteChannel(channelInfo);
                                    }
                                    return;
                                }
                            }
                            if (added) {
                                ChannelInfo channelInfo = tvController.addChannel();
                                channelInfo.name(name);
                                channelInfo.url(url);
                            }
                        });
                    }
                }, 500);
            }
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (iptvPlayer != null) {
            iptvPlayer.destroy();
            iptvPlayer = null;
        }

        if (DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING).equals(SettingKeys.TVSource.IPTV.name())) {
            try {
                multicastLock.release();
            } catch (Exception ignored) {
            }
        }

        mStatOverlay.finish();

    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (findViewById(R.id.tv_user_interface).getVisibility() == View.VISIBLE) {
            autoHideTimer.reset();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tvController.close();
        dataMatrix.finish();

        try {
            if (closedCaptionSocket != null) {
                closedCaptionSocket.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        try {
            if (closedCaptionServerSocket != null) {
                closedCaptionServerSocket.close();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        try {
            this.getApplicationContext().unregisterReceiver(dolbyReceiver);
        } catch (Exception e){

        }
    }

    public int checkConsoleSize() {
        int lvdsValue = -1;
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
            File f = new File("/sys/bus/i2c/drivers/ep172u/0-0038/lvds");
            if (!f.exists()) {
                Log.d("kstest", "checkConsoleSize: lvds not exist");
                return 0;
            }

            new ExecuteAsRoot()
                    .addCommand("cat /sys/bus/i2c/drivers/ep172u/0-0038/lvds > /data/tmp")
                    .execute();
        } else {
            new ExecuteAsRoot()
                    .addCommand("cat /sys/class/extcon/extcon0/state > /data/tmp")
                    .execute();
        }
        FileReader fr;
        try {
            fr = new FileReader("/data/tmp");
            BufferedReader br = new BufferedReader(fr);
            while (br.ready()) {
                String s = br.readLine();
                lvdsValue = Integer.parseInt(s.substring(s.indexOf("=") + 1));
                Log.d(TAG, "checkConsoleSize: LVDS: " + lvdsValue);
            }
            fr.close();
        } catch (FileNotFoundException e) {
            Log.w(TAG, e);
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        // if lvdsValue is 1, console size is 21.5", if lvdsValue is 0, console size is 15.6"
        return lvdsValue;
    }


    @Override
    protected AudioSourceInput audioSourceInput() {
        return tvController.capabilities().hdmi() ? AudioSourceInput.HDMI : AudioSourceInput.Foreground;
    }
}
