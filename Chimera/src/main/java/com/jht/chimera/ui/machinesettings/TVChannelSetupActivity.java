
package com.jht.chimera.ui.machinesettings;


import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.ProgressDialog;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SimpleSelectorItem;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxController;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.activity.HDMIActivity;
import com.jht.chimera.lib.widget.tv.TVColorButtons;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.chimera.lib.widget.tv.TVKeypad;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.pages.tv.SKNETChannelSelectorItem;
import com.jht.chimera.ui.machinesettings.pages.tv.TVChannelSelectorItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.iptvplayer.IPTVPlayer;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This houses the Engineering screen activity which is made up of several pages that you can navigate
 * using a side menu.
 */

public class TVChannelSetupActivity extends HDMIActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "TVChannelSetupActivity";
    private TVControllerBase controller;
    private SettingKeys.TVSource source;
    private ITunerCapabilities capabilities;
    private final Handler handler = new Handler();
    private RecyclerView channelList;
    private final SimpleSelectorItem channels = new SimpleSelectorItem("", new ArrayList<>()); //NON-NLS
    private DialogBase dialog;
    private WifiManager.MulticastLock multicastLock;
    private final DataMatrix dataMatrix = new DataMatrix();
    private IPTVPlayer iptvPlayer;
    private int stbCommandIndex = 0;
    private String stbCommandName = "";




    public TVChannelSetupActivity() {
        EngineeringIdleTimerManager.idleTimerManagement = new IdleTimerManagement();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_settings);

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("tvmulticastlock");
        multicastLock.setReferenceCounted(true);

        findViewById(R.id.tv_channel_setup_back).setOnClickListener(v -> finish());


    }

    @Override
    public void onPause() {
        super.onPause();
        if (source.equals(SettingKeys.TVSource.STB) || source.equals(SettingKeys.TVSource.US4) || source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.US6) || source.equals(SettingKeys.TVSource.KM4) || source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2) || source.equals(SettingKeys.TVSource.US7))
            controller.destroy();
        try {
            multicastLock.release();
        } catch (Exception ignored) {
        }
        if (iptvPlayer != null) {
            iptvPlayer.destroy();
            iptvPlayer = null;
        }


        dataMatrix.finish();

    }

    private SimpleMessageDialog currentDialog;

    @Override
    public void onResume() {

        source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
        controller = TVControllerFactory.getTVController(this, source);
        capabilities = controller.capabilities();

        if(source.equals(SettingKeys.TVSource.STB) || source.equals(SettingKeys.TVSource.US4) ||source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.US6) || source.equals(SettingKeys.TVSource.KM4)|| source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2) || source.equals(SettingKeys.TVSource.US7)) {
            controller.open();
        }

        controller.reset();

        ImageView enseoSplash = findViewById(R.id.enseo_splash);
        SurfaceView tvSurface = findViewById(R.id.eng_tv_player_surface);
        TextureView textureViewTv = findViewById(R.id.eng_tv_player_texture);

        if(controller.splashResource() != 0) {
            // R.drawable.enseo, R.drawable.directv_splash
            enseoSplash.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), controller.splashResource()));
            enseoSplash.setVisibility(View.VISIBLE);
        } else {
            enseoSplash.setVisibility(View.GONE);
        }

        setProperties(tvSurface, textureViewTv, source == SettingKeys.TVSource.US4 ? 2500 : 0, () -> {
            controller.destroy();
            controller = TVControllerFactory.getTVController(getApplicationContext(), source);
            controller.open();
        });

        SurfaceView surfaceView = findViewById(R.id.eng_tv_player_surface);
        TextureView textureView = findViewById(R.id.eng_vlc_player_surface);
        if(capabilities.hdmi()) {
            initHDMI();
        }
        else {
            textureView.bringToFront();
            try {
                System.loadLibrary("c++_shared");
                ArrayList<String> options = new ArrayList<>();
                options.add("--avcodec-skiploopfilter=4"); // NON-NLS
                iptvPlayer = new IPTVPlayer(getApplicationContext(), IPTVPlayer.PlayerMode.VLC, surfaceView, textureView, findViewById(R.id.eng_tv_subtitles_surface), options, DataMatrix.getInt(SettingKeys.Experimental.DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT));
                iptvPlayer.setVolume(0.26f);
                iptvPlayer.setVlcVolume(26);
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "VLC native libraries not available, IPTV playback disabled", e);
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize IPTV player", e);
            }
        }

        TVKeypad keypad = findViewById(R.id.tv_settings_eng_keypad);
        TVExtendedControls extendedControls = findViewById(R.id.tv_settings_eng_extended_controls);
        Button importChannelsUSB = findViewById(R.id.eng_tv_import_channels_usb);
        Button exportChannelsUSB = findViewById(R.id.eng_tv_export_channels_usb);
        Button importChannelsTuner = findViewById(R.id.eng_tv_import_from_tuner);
        Button upgradeKm4 = findViewById(R.id.eng_tv_upgrade_km4);
        Button sknetScan = findViewById(R.id.eng_tv_sknet_scan);
        Button sknetGetChannels = findViewById(R.id.eng_tv_sknet_get_channels);
        TVColorButtons colorButtons = findViewById(R.id.tv_channel_setup_color);

        channelList = findViewById(R.id.eng_tv_channel_list);

        Log.d(TAG, "Showing channel buttons ? " + capabilities.channelList() + " " + source);
        importChannelsUSB.setVisibility(capabilities.channelList() || source.equals(SettingKeys.TVSource.KM4) ? View.VISIBLE : View.GONE);
        exportChannelsUSB.setVisibility(capabilities.channelList() ? View.VISIBLE : View.GONE);
        importChannelsTuner.setVisibility(capabilities.importChannelList() ? View.VISIBLE : View.GONE);
        upgradeKm4.setVisibility(source.equals(SettingKeys.TVSource.KM4) ? View.VISIBLE : View.GONE);

        keypad.enableDash(capabilities.dash());
        keypad.setOnKeyPress(controller);
        initExtendedControls(extendedControls);
        keypad.setVisibility(source.equals(SettingKeys.TVSource.IPTV) || source.equals(SettingKeys.TVSource.US4)  ? View.GONE : View.VISIBLE);
        colorButtons.setOnKeyPress(controller);
        colorButtons.setVisibility(capabilities.colorButtons() ? View.VISIBLE : View.GONE);

        importChannelsUSB.setOnClickListener(this::importChannels);
        exportChannelsUSB.setOnClickListener(this::exportChannelsUSB);
        importChannelsTuner.setOnClickListener(this::exportChannels);
        upgradeKm4.setOnClickListener(this::upgradeKM4);

        UserTextInput frequency = findViewById(R.id.eng_tv_stb_frequency);
        UserTextInput repeats = findViewById(R.id.eng_tv_stb_repeat);

        if(capabilities.channelList()) {
            SelectorDefaultAnimations.setDefaultAnimations(this, channelList);
            findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.VISIBLE);
            updateAdapter();
            if(controller.currentChannel() != null && !capabilities.hdmi()) {
                handler.postDelayed(() -> play(controller.currentChannel().url(), controller.currentChannel().deinterlaced()), 1000);
                //play(controller.currentChannel().url());
            }

        }
        else {
            if(source.equals(SettingKeys.TVSource.KM4)) {
                findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.INVISIBLE);
                findViewById(R.id.eng_tv_default_channel_container).setVisibility(View.INVISIBLE);
            }
            else {
                findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_default_channel_container).setVisibility(View.GONE);
            }
            updateDefaultChannel();
        }

        // for SK NET set top box function
        if(source.equals(SettingKeys.TVSource.STB)) {
            Log.d(TAG, "onResume: controller.getSelectedSetTopBox() = " + controller.getSelectedSetTopBox());
            if(controller.getSelectedSetTopBox().contains("SK NET")) {
                Log.d(TAG, "onResume: IS SK NET STB");
                findViewById(R.id.tv_settings_eng_keypad).setVisibility(View.GONE);
                findViewById(R.id.tv_settings_eng_extended_controls).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_import_channels_usb).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_export_channels_usb).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_sknet_get_channels).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tv_sknet_scan).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tv_sknet_target_address).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tv_sknet_target_port).setVisibility(View.VISIBLE);

                {
                    UserTextInput userTextInput = findViewById(R.id.eng_tv_sknet_target_address);
                    userTextInput.setText("" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING));
                    userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING, s));
                }

                {
                    UserTextInput userTextInput = findViewById(R.id.eng_tv_sknet_target_port);
                    userTextInput.setText("" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING));
                    userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING, s));
                }
            } else {
                Log.d(TAG, "onResume: NOT SK NET STB");
                findViewById(R.id.tv_settings_eng_keypad).setVisibility(View.VISIBLE);
                findViewById(R.id.tv_settings_eng_extended_controls).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tv_import_channels_usb).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_export_channels_usb).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_sknet_get_channels).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_sknet_scan).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_sknet_target_address).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_sknet_target_port).setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.eng_tv_sknet_get_channels).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_sknet_scan).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_sknet_target_address).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_sknet_target_port).setVisibility(View.GONE);
        }

        sknetScan.setOnClickListener(this::sknetScanChannels);
        sknetGetChannels.setOnClickListener(this::sknetGetChannels);

        // After SKNet get channels complete, send event to update channel list and default channel options
        dataMatrix.createEventAdapter(TVKeys.EVENT_UPDATE_CHANNEL_LIST, s -> {
            Log.d(TAG, "show: receive EVENT_UPDATE_CHANNEL_LIST");
            updateAdapter();
            updateDefaultChannel();
        });

        final UserTextInput defaultChannelInput = findViewById(R.id.eng_tv_player_default_channel_input);
        defaultChannelInput.setOnTextChanged(s -> controller.defaultChannel(defaultChannelInput.getText()));

        if(!capabilities.hdmi()) {
            // This is IPTV.
            controller.setOnChannelChanged(channelInfo -> {
                if(channelInfo != null && channelInfo.name() != null && !channelInfo.name().isEmpty()) {
                    play(channelInfo.url(), channelInfo.deinterlaced());
                }
            });
        }


        findViewById(R.id.eng_tv_enseo_legacy).setOnClickListener(view -> {
            if(DataMatrix.getBoolean(SettingKeys.TV.DATA_ENSEO_LEGACY_MODE_INITIALIZED_BOOL)) {
                ProjectDialogFactory.confirm(view, getResources().getString(R.string.enseo_legacy_confirm_pg), () -> {
                    new Thread(() -> {
                        controller.enableLegacy();
                        handler.post(() -> {
                            if(currentDialog != null) {
                                currentDialog.init(getString(R.string.enseo_initiailzed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                currentDialog = null;
                            }
                        });
                    }).start();
                    currentDialog = ProjectDialogFactory.message(view, getString(R.string.enseo_enable_legacy_message_pg), () -> {
                        currentDialog = null;
                        controller.cancelOperation();
                    }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.CANCEL);
                }, null);
            }
            else {
                new Thread(() -> {
                    controller.enableLegacy();
                    handler.post(() -> {
                        if(currentDialog != null) {
                            currentDialog.init(getString(R.string.enseo_initiailzed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                            currentDialog = null;
                        }
                    });
                }).start();
                currentDialog = ProjectDialogFactory.message(view, getString(R.string.enseo_enable_legacy_message_pg), () -> {
                    currentDialog = null;
                    controller.cancelOperation();
                }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.CANCEL);
                DataMatrix.putBoolean(SettingKeys.TV.DATA_ENSEO_LEGACY_MODE_INITIALIZED_BOOL, true);
                findViewById(R.id.eng_tv_enseo_initialize).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tv_enseo_complete).setVisibility(View.VISIBLE);
            }
        });

        if(source.equals(SettingKeys.TVSource.US4)) {

            findViewById(R.id.eng_tv_enseo_legacy).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_tv_enseo_initialize).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_tv_enseo_complete).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_stb_command_list_container).setVisibility(showSTBCommandList() ? View.VISIBLE : View.GONE);

        }
        else if(source.equals(SettingKeys.TVSource.IPTV)) {
            findViewById(R.id.eng_tv_enseo_legacy).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_enseo_initialize).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_enseo_complete).setVisibility(View.GONE);
            findViewById(R.id.eng_stb_command_list_container).setVisibility(showSTBCommandList() ? View.VISIBLE : View.GONE);
        }
        else {
            findViewById(R.id.eng_tv_enseo_legacy).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_enseo_initialize).setVisibility(View.GONE);
            findViewById(R.id.eng_tv_enseo_complete).setVisibility(View.GONE);
            findViewById(R.id.eng_stb_command_list_container).setVisibility(showSTBCommandList() ? View.VISIBLE : View.GONE);

        }

        OptionSelectorItem setTopBoxCommandOptions = findViewById(R.id.eng_stb_command_option);
        Map<String, SetTopBoxSetting.RemoteButton> remoteButtonMap = new HashMap<>();
        ArrayList<String> commandlist = new ArrayList<>();
        SetTopBoxSetting currentSTB = controller.getCurrentSTB();

        if(currentSTB != null)
            remoteButtonMap = controller.getCurrentSTB().remoteButtonMap;

        if(currentSTB != null) {
            Log.d(TAG, "onResume: currentSTB name = " + currentSTB.setTopBoxName);
            Log.d(TAG, "onResume: remoteButtonMap size = " + remoteButtonMap.size());

            commandlist.clear();
            for(Map.Entry<String, SetTopBoxSetting.RemoteButton> entry : remoteButtonMap.entrySet()) {
                commandlist.add(entry.getKey());
            }
        } else {
            Log.d(TAG, "onResume: currentSTB == null");
        }

        if(commandlist.size() > 0) {
            String[] commandNames = new String[commandlist.size()];
            commandNames = commandlist.toArray(commandNames);
            setTopBoxCommandOptions.setOptions(new OptionManager(commandNames, 0));
            stbCommandIndex = 0;
            stbCommandName = commandNames[stbCommandIndex];

            String[] finalCommandNames = commandNames;
            setTopBoxCommandOptions.setOnOptionSelected((pos, option) -> {
                stbCommandIndex = pos;
                stbCommandName = finalCommandNames[stbCommandIndex];
            });
        }

        Map<String, SetTopBoxSetting.RemoteButton> finalRemoteButtonMap = remoteButtonMap;
        findViewById(R.id.eng_stb_command_go).setOnClickListener(view -> {
            if(currentSTB != null) {
                Log.d(TAG, "GO setOnClickListener: stbCommandName = " + stbCommandName);
                controller.executeSTBCommand(finalRemoteButtonMap.get(stbCommandName));
            }
        });

        findViewById(R.id.eng_tv_enseo_initialize).setOnClickListener(view -> {
            new Thread(() -> {
                controller.initialize();
                handler.post(() -> {
                    if(currentDialog != null) {
                        currentDialog.init(getString(R.string.enseo_initiailzed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        currentDialog = null;
                    }
                });

            }).start();
            currentDialog = ProjectDialogFactory.message(view, getString(R.string.initializing_please_wait_pg), () -> {
                currentDialog = null;
                controller.cancelOperation();
            }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.CANCEL);

        });

        findViewById(R.id.eng_tv_enseo_complete).setOnClickListener(view -> {
            controller.importChannels(new TVControllerBase.IOnProgress() {
                @Override
                public void onProgress(int progressPercentage) {

                }

                @Override
                public void onComplete(boolean success, ATSC_STATUS status) {
                    handler.post( () -> {
                        if(currentDialog != null) {
                            currentDialog.init(getString(R.string.export_settings_network_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        }
                        else {
                            ProjectDialogFactory.message(view, getString(R.string.export_settings_network_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        }
                        currentDialog = null;
                    });
                }
            }, TVControllerBase.IMPORT_CHANNEL_MODE.FLASH_TO_TUNER);
            currentDialog = ProjectDialogFactory.message(view, getString(R.string.enseo_channel_export_pg), () -> {
                currentDialog = null;
                controller.cancelOperation();
            }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.CANCEL);
        });

        // Place this at the end to make sure we have updated the tuners for the audio controller.
        super.onResume();
    }

    private void initExtendedControls(TVExtendedControls extendedControls) {
        extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.CC);
        if(source.equals(SettingKeys.TVSource.STB)) {
            if(capabilities.guide() && capabilities.back()) {
                extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.BACK);
            }
        }
        extendedControls.setOnKeyPress(controller);
        extendedControls.setVisibility(source.equals(SettingKeys.TVSource.CAB) || source.equals(SettingKeys.TVSource.US4)  || source.equals(SettingKeys.TVSource.IPTV) ? View.GONE : View.VISIBLE);
        extendedControls.showButtons(capabilities);
    }


    private void importChannels(View view) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        TVControllerBase.IMPORT_CHANNEL_MODE mode = TVControllerBase.IMPORT_CHANNEL_MODE.USB_TO_FLASH_TO_TUNER;
        if (source.equals(SettingKeys.TVSource.US4)) {
            mode = TVControllerBase.IMPORT_CHANNEL_MODE.USB_TO_FLASH;
        }
        boolean progress = controller.importChannels(new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        ((ProgressDialog)dialog).onProgress().progress(progressPercentage, false, null);
                    }
                    if(capabilities.isSetTopBox() && controller instanceof SetTopBoxController) {
                        ((SetTopBoxController)controller).reload();
                        updateAdapter();
                    }
                });

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        if(status != ATSC_STATUS.PROCESSING || success) {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, getString(success ? R.string.import_channels_success_tc : R.string.import_channels_failed_pg));
                        }
                    }
                    else if(dialog instanceof SimpleMessageDialog) {
                        ((SimpleMessageDialog)dialog).init(getString( success ? R.string.import_channels_success_tc : R.string.import_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                    updateAdapter();
                });
            }
        }, mode);
        if(progress) {
            dialog = ProjectDialogFactory.progressDialog(view, getString(R.string.import_channels_tc), getString(R.string.import_in_progress_tc), "", false, null, null);
        }
        else {
            dialog = ProjectDialogFactory.message(view, getString(R.string.import_channels_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        }

    }

    private void exportChannelsUSB(View view) {
        if(controller.exportChannelsUSB()) {
            ProjectDialogFactory.message(view,  getString(R.string.export_channels_success_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
        }
        else {
            ProjectDialogFactory.message(view,  getString(R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
        }
    }

    private void exportChannels(View view) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        boolean progress = controller.exportChannels(new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        ((ProgressDialog)dialog).onProgress().progress(progressPercentage, false, null);
                    }
                });

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    Log.d(TAG, "Channel export result " + status);
                    if(dialog instanceof ProgressDialog) {
                        if (status != ATSC_STATUS.PROCESSING || success) {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, getString(success ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg));
                            // If tv support channel list (e.g. SwiftInfo), send event to tell TV page to update default channel option field
                            if(success && capabilities.channelList()) {
                                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            }
                        }
                    }
                    else if(dialog instanceof SimpleMessageDialog) {
                        ((SimpleMessageDialog)dialog).init(getString( success ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                    updateAdapter();
                });
            }
        });

        if(progress) {
            dialog = ProjectDialogFactory.progressDialog(view, getString(R.string.export_channels_tc), getString(R.string.export_in_progress_tc), "", false, null, null);
        }
        else {
            dialog = ProjectDialogFactory.message(view, getString(R.string.export_channels_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        }
    }

    private void upgradeKM4(View view) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        boolean progress = controller.updateFirmware(new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        ((ProgressDialog)dialog).onProgress().progress(progressPercentage, false, null);
                    }
                });

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        if(success || status != ATSC_STATUS.PROCESSING) {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, success ? getString(R.string.update_complete_tc) : R.string.update_failed_tc);
                        }
                    }
                    else if(dialog instanceof SimpleMessageDialog) {
                        ((SimpleMessageDialog)dialog).init(success ? getString(R.string.km4_update_complete_pg) : getString(R.string.update_failed_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                });
            }
        });
        if(progress) {
            dialog = ProjectDialogFactory.progressDialog(view, getString(R.string.update_tc), getString(R.string.update_in_progress_tc), "", false, null, null);
        }
        else {
            dialog = ProjectDialogFactory.message(view, getString(R.string.update_tuner_firmware_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        }
    }

    private void sknetScanChannels(View view) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        boolean progress = controller.exportChannels(new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {
                handler.post(() -> {
                    if(dialog instanceof ProgressDialog) {
                        ((ProgressDialog)dialog).onProgress().progress(progressPercentage, false, null);
                    }
                });

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    Log.d(TAG, "Channel export result " + status);
                    if(dialog instanceof ProgressDialog) {
                        if (status == ATSC_STATUS.PROCESSING) {
                            if(success) {
                                ((ProgressDialog) dialog).onProgress().progress(100, true, getString(R.string.import_channels_success_tc));
                            }
                        } else if(status == ATSC_STATUS.NETWORKERROR) {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, success ? getString(R.string.import_channels_success_tc) : getString(R.string.network_unavailable_tc));
                        } else {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, success ? getString(R.string.import_channels_success_tc) : getString(R.string.import_channels_failed_pg));
                            // If tv support channel list (e.g. SwiftInfo), send event to tell TV page to update default channel option field
                            if (success && capabilities.channelList()) {
                                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            }
                        }
                    }
                    else if(dialog instanceof SimpleMessageDialog) {
                        ((SimpleMessageDialog)dialog).init(getString( success ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                    updateAdapter();
                });
            }
        });

        if(progress) {
            dialog = ProjectDialogFactory.progressDialog(view, getString(R.string.export_channels_tc), getString(R.string.export_in_progress_tc), "", false, null, null);
        }
        else {
            dialog = ProjectDialogFactory.message(view, getString(R.string.export_channels_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        }
    }

    private void sknetGetChannels(View view) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        Log.d(TAG, "TVChannelSetupActivity call sknetGetChannels() ");
        boolean progress = controller.sknetGetChannels(new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {
                handler.post(() -> {
//                    if(dialog instanceof ProgressDialog) {
//                        ((ProgressDialog)dialog).updateProgress(progressPercentage, false);
//                    }
                });

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    Log.d(TAG, "Channel export result " + status);
                    if(dialog instanceof ProgressDialog) {
                        if (status == ATSC_STATUS.PROCESSING) {
                            if(success) {
                                ((ProgressDialog) dialog).onProgress().progress(100, true, getString(R.string.export_channels_success_tc));
                            }
                        } else if(status == ATSC_STATUS.NETWORKERROR) {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, success ? getString(R.string.export_channels_success_tc) : getString(R.string.network_unavailable_tc));
                        } else {
                            ((ProgressDialog) dialog).onProgress().progress(100, true, success ? getString(R.string.export_channels_success_tc) : getString(R.string.export_channels_failed_pg));
                            // If tv support channel list (e.g. SwiftInfo), send event to tell TV page to update default channel option field
                            if(success && capabilities.channelList()) {
                                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            }
                        }
                    }
                    else if(dialog instanceof SimpleMessageDialog) {
                        ((SimpleMessageDialog)dialog).init(getString( success ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                    updateAdapter();
                });
            }
        });

        if(progress) {
            dialog = ProjectDialogFactory.progressDialog(view, getString(R.string.export_channels_tc), getString(R.string.export_in_progress_tc), "", false, null, null);
        }
        else {
            dialog = ProjectDialogFactory.message(view, getString(R.string.export_channels_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        }
    }

    // CGC-22432: In the event this activity needs duplicate channel prevention
    // please refer to the functionality being added to TVChannelSetupActivityNew
    private TVChannelSelectorItem createChannel(ChannelInfo channel) {
        return new TVChannelSelectorItem(channel, null, item -> {
            if(controller.channelType().equals(ChannelInfo.ChannelType.REGULAR_TV)) {
                ProjectDialogFactory.editChannelDialog(channelList, channel, () -> {
                    updateAdapter();
                    controller.saveChannels(false);
                    DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                });
            }
            else if(controller.channelType().equals(ChannelInfo.ChannelType.REGULAR_TV_USER_ENTERED)) {
                ProjectDialogFactory.editChannelDialog2(channelList, channel, () -> {
                    updateAdapter();
                    controller.saveChannels(false);
                    DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                });
            }
            else {
                ProjectDialogFactory.editIPTVChannelDialog(channelList, channel, controller.channels(), () -> {
                    updateAdapter();
                    controller.saveChannels(false);
                    DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                });
            }
        }, capabilities.modifyChannelList(), item -> {
            controller.deleteChannel(item.channelInfo());
            updateAdapter();
            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
        }, capabilities.canEditChannelName());
    }

    private SKNETChannelSelectorItem createChannel(SKNETChannelInfo channel) {
        return new SKNETChannelSelectorItem(channel, null, item -> updateAdapter(), capabilities.modifyChannelList(), item -> {
            controller.deleteChannel(item.sknetChannelInfo());
            updateAdapter();
            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
        }, capabilities.canEditChannelName());
    }


    private void updateAdapter() {
        channels.removeAllChildren();

        // for SK NET set top box function
        if(source.equals(SettingKeys.TVSource.STB) && controller.getSelectedSetTopBox().contains("SK NET")) {
            for (SKNETChannelInfo channelInfo : controller.sknetChannels()) {
                channels.addItem(createChannel(channelInfo));
            }
            int index = controller.getIndexByChannelInfo(controller.currentChannel());
            if(index >= 0 && index < controller.channels().size()) {
                channels.setSelection(index);
            }
        } else {
            for (ChannelInfo channelInfo : controller.channels()) {
                channels.addItem(createChannel(channelInfo));
            }
            int index = controller.getIndexByChannelInfo(controller.currentChannel());
            if(index >= 0 && index < controller.channels().size()) {
                channels.setSelection(index);
            }
        }

        // CGC-22432: TODO: add duplicate channel prevention here to match TVChannelSetupActivityNew
        if(capabilities.modifyChannelList()) {
            channels.addItem(new TVChannelSelectorItem(null, null, item -> {
                ChannelInfo newChannel = channels.numChildren() > 1 ? new ChannelInfo(((TVChannelSelectorItem)channels.getChild(channels.numChildren() - 2)).channelInfo()) : new ChannelInfo(controller.channelType(), "", "", null);
                if(controller.channelType() == ChannelInfo.ChannelType.REGULAR_TV) {
                    ProjectDialogFactory.editChannelDialog(channelList, newChannel, () -> {
                        controller.addChannel().copyFrom(newChannel);
                        updateAdapter();
                        controller.saveChannels(false);
                        DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                    });
                }
                else if(controller.channelType().equals(ChannelInfo.ChannelType.REGULAR_TV_USER_ENTERED)) {
                    ProjectDialogFactory.editChannelDialog2(channelList, newChannel, () -> {
                        controller.addChannel().copyFrom(newChannel);
                        updateAdapter();
                        controller.saveChannels(false);
                    });
                }
                else {
                    ProjectDialogFactory.editIPTVChannelDialog(channelList, newChannel, controller.channels(), () -> {
                        controller.addChannel().copyFrom(newChannel);
                        updateAdapter();
                        controller.saveChannels(false);
                        DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                    });
                }
            }, false, null, capabilities.canEditChannelName()));
        }
        channelList.setAdapter(new StandardSelectorItemAdapter(this, channels, item -> {
            if(item instanceof TVChannelSelectorItem) {
                if(capabilities.hdmi()) {
                    controller.setChannel(((TVChannelSelectorItem) item).channelInfo());
                }
                else {
                    play(((TVChannelSelectorItem)item).channelInfo().url(), ((TVChannelSelectorItem)item).channelInfo().deinterlaced());
                }
            }
            else if(item instanceof SKNETChannelSelectorItem) {
                if(capabilities.hdmi()) {
                    controller.setSknetChannel(((SKNETChannelSelectorItem) item).sknetChannelInfo());
                }
            }
        }));
        updateDefaultChannel();

    }

    private void play(final String url, boolean deinterlace) {
        if (iptvPlayer == null) {
            Log.w(TAG, "IPTV player not available, skipping playback");
            return;
        }
        try {
            iptvPlayer.play(Uri.parse(url), false); // Removing deinterlacing feature https://jhtnaee.atlassian.net/browse/UIPTV-161.
        } catch (Exception e) {
            Log.e(TAG, "Failed to play channel", e);
        }
    }


    private void updateDefaultChannel() {

        UserTextInput defaultChannelInput = findViewById(R.id.eng_tv_player_default_channel_input);
        OptionSelectorItem defaultChannelSelection = findViewById(R.id.eng_tv_default_channel_selector);

        // for SK NET set top box function
        if(source.equals(SettingKeys.TVSource.STB) && controller.getSelectedSetTopBox().contains("SK NET")) {
            defaultChannelInput.setVisibility(View.GONE);
            defaultChannelSelection.setVisibility(View.VISIBLE);

            String[] channelNames = new String[controller.sknetChannels().size()];
            for (int i = 0; i < controller.sknetChannels().size(); i++) {
                channelNames[i] = controller.sknetChannels().get(i).getNumber() + " - " + controller.sknetChannels().get(i).getServiceName();
            }
            defaultChannelSelection.setOnOptionSelected(null);
            defaultChannelSelection.setOptions(new OptionManager(channelNames, controller.defaultChannelInt()));
            defaultChannelSelection.setOnOptionSelected((pos, option) -> {
                controller.defaultChannel(pos);
                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
            });
            if (controller.sknetChannels().size() == 0) {
                defaultChannelSelection.setText("");
            } else {
                controller.defaultChannel(controller.defaultChannelInt());
            }
        }
        else {
            if (controller.capabilities().channelList() && !(controller.capabilities().isSetTopBox() && controller.channels().isEmpty())) {
                defaultChannelInput.setVisibility(View.GONE);
                defaultChannelSelection.setVisibility(View.VISIBLE);

                String[] channelNames = new String[controller.channels().size()];
                for (int i = 0; i < controller.channels().size(); i++) {
                    channelNames[i] = controller.channels().get(i).number() + " - " + controller.channels().get(i).name();
                }
                defaultChannelSelection.setOnOptionSelected(null);
                defaultChannelSelection.setOptions(new OptionManager(channelNames, controller.defaultChannelInt()));
                defaultChannelSelection.setOnOptionSelected((pos, option) -> {
                    controller.defaultChannel(pos);
                    DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                });
                if (controller.channels().size() == 0) {
                    defaultChannelSelection.setText("");
                } else {
                    controller.defaultChannel(controller.defaultChannelInt());
                }
            } else {

                defaultChannelInput.setVisibility(View.VISIBLE);
                defaultChannelSelection.setVisibility(View.GONE);
                defaultChannelInput.setText(controller.defaultChannelString());
                defaultChannelInput.setOnTextChanged(s -> controller.defaultChannel(defaultChannelInput.getText()));
            }
        }
    }

    private boolean showSTBCommandList()
    {
        boolean result = false;
        if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_SHOW_STB_COMMAND_LIST_BOOL)) {
            if (source.equals(SettingKeys.TVSource.US4)) {
                result = true;
            } else if (source.equals(SettingKeys.TVSource.STB)) {
                if(!controller.getSelectedSetTopBox().contains("SK NET"))
                    result = true;
            }
        }
        return result;
    }

    @Override
    protected AudioSourceInput audioSourceInput() {
        return capabilities.hdmi() ? AudioSourceInput.HDMI : AudioSourceInput.Foreground;
    }
}



