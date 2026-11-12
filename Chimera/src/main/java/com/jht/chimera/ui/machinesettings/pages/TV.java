package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.ProgressDialog;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxController;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.TVChannelSetupActivity;
import com.jht.chimera.ui.machinesettings.TVChannelSetupActivityNew;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.pages.tv.EngineeringPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implements the TV page gluing the page to the model layer.
 */
public class TV extends EngineeringPageBase {
    private static final String TAG = "TV";
    private TVControllerBase controller;
    private IChimeraTuner<? extends IChannel> tuner;
    private ITunerCapabilities capabilities;
    private long channelSetupClickedTime = 0; // Used for debouncing the channel setup button
    private SetTopBoxSetting currentSTB = null;
    private SetTopBoxSetting.RemoteButton currentRemoteButton = null;
    private SetTopBoxController currentController = null;
    private UserTextInput frequency;
    private UserTextInput repeats;

    private final DataMatrix dataMatrix = new DataMatrix();
    public TV() {

    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {

        View view = LayoutInflater.from(context).inflate(R.layout.tv_eng, null);
        container.addView(view);
        Handler handler = new Handler();
        view.findViewById(R.id.eng_stb_tv_import).setOnClickListener( v -> SetTopBoxController.importSTB(getContext(), new TVControllerBase.IOnProgress() {
            @Override
            public void onProgress(int progressPercentage) {

            }

            @Override
            public void onComplete(boolean success, ATSC_STATUS status) {
                handler.post(() -> {
                    if(success) {
                        ProjectDialogFactory.message(view, getContext().getString(R.string.import_settings_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        updateSTBList();
                    }
                    else {
                        ProjectDialogFactory.message(view, getContext().getString(R.string.import_settings_fail_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    }
                });
            }
        }));

        view.findViewById(R.id.eng_tv_stb_button_reset).setOnClickListener((v) -> {
            if(currentController != null && currentSTB != null) {
                SetTopBoxSetting settings = currentController.loadFromAssets(currentSTB.getFolderName());
                SetTopBoxSetting.RemoteButton originalButton = settings.remoteButtonMap.get(currentRemoteButton.buttonName);
                currentSTB.remoteButtonMap.put(originalButton.buttonName, originalButton);
                currentSTB.frequencyKhz = settings.frequencyKhz;
                currentSTB.repeats = settings.repeats;
                frequency.setText(currentSTB.frequencyKhz + "");
                repeats.setText(currentSTB.repeats + "");
                currentRemoteButton = currentSTB.remoteButtonMap.get(originalButton.buttonName);
                updateRemoteTimings(currentRemoteButton);
                currentController.saveSettings(currentSTB);
            }
        });
        view.findViewById(R.id.eng_tv_stb_button_restore).setOnClickListener((v) -> {
            if(currentController != null) {
                currentController.reloadCurrent();
                currentSTB = currentController.getCurrentSTB();
                if(currentSTB != null) {
                    updateRemoteTimings(currentRemoteButton);
                    frequency.setText(currentSTB.frequencyKhz + "");
                    repeats.setText(currentSTB.repeats + "");
                }
            }
        });
        view.findViewById(R.id.eng_tv_stb_button_save).setOnClickListener((v) -> {
            String[] newTimings = ((UserTextInput)view.findViewById(R.id.eng_tv_stb_button_new)).getText().replace(" ", "").split(",");
            int[] originalSequence = currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence;
            currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence = new int[newTimings.length];
            try {
                for (int i = 0; i < newTimings.length; i++) {
                    currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence[i] = Integer.parseInt(newTimings[i]);
                }
            }
            catch (Exception ignored) {
                DialogFactory.message(v, "Error parsing new timings.", null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence = originalSequence;
            }
            updateRemoteTimings(currentRemoteButton);
            currentController.saveSettings(currentSTB);

        });
        view.findViewById(R.id.eng_tv_stb_button_execute).setOnClickListener((v) -> {
            String[] newTimings = ((UserTextInput)view.findViewById(R.id.eng_tv_stb_button_new)).getText().replace(" ", "").split(",");
            int[] originalSequence = currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence;
            currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence = new int[newTimings.length];
            try {
                for (int i = 0; i < newTimings.length; i++) {
                    currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence[i] = Integer.parseInt(newTimings[i]);
                }
            }
            catch (Exception ignored) {
                DialogFactory.message(v, "Error parsing new timings.", null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
            currentRemoteButton.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence = originalSequence;

            byte[] message = currentRemoteButton.getIRCommand(currentSTB);
            DataMatrix.putBytes(HALKeys.ACTION_IO_SEND_IR_COMMAND_BYTES, Arrays.copyOf(message, message.length));
        });

        frequency = view.findViewById(R.id.eng_tv_stb_frequency);
        repeats = view.findViewById(R.id.eng_tv_stb_repeat);
        frequency.setOnTextChanged(text -> {
            if(currentSTB != null) {
                try {
                    currentSTB.frequencyKhz = Integer.parseInt(text.trim());
                } catch (NumberFormatException ignored) {}
            }
        });
        repeats.setOnTextChanged(text -> {
            if(currentSTB != null) {
                currentSTB.repeats = Integer.parseInt(text);
            }
        });

        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        updateUI();

        linkCheckbox(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL, R.id.eng_remote_tv, null);
        linkEditInt(SettingKeys.TV.DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT, R.id.eng_remote_tv_channel, null);
        linkEditInt(SettingKeys.TV.DATA_TV_REMOTE_TV_VOLUME_INT, R.id.eng_remote_tv_volume, null);
        //linkEditInt(SettingKeys.TV.TV_OVERSCAN_INT, R.id.eng_tv_player_over_screen_size, null);
        //linkOption(SettingKeys.TV.TV_ASPECT_RATIO, getContext().getResources().getStringArray(R.array.aspect_ratio), JHTTypes.AspectRatio.values(), R.id.eng_tv_player_aspect_ratio, null);

        SettingKeys.TVSource[] sources = SettingKeys.TVSource.getList();
        String[] tvLabels = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            tvLabels[i] = sources[i].getDisplayName();
        }
        linkOption(SettingKeys.TV.DATA_TV_SOURCE_STRING, tvLabels, sources, R.id.eng_tv_source_selection, (pos, option) -> updateUI());

        final UserTextInput defaultChannelInput = findViewById(R.id.eng_tv_player_default_channel_input_1);
        defaultChannelInput.setOnTextChanged(s -> {
            if(controller != null) {
                controller.defaultChannel(defaultChannelInput.getText());
            }
            else if(tuner != null) {
                tuner.setDefaultChannel(defaultChannelInput.getText());
            }
        });

        findViewById(R.id.eng_tv_configure).setOnClickListener(v -> {
            // Debounce this button to prevent multiple clicks from showing repeat activities
            long time = SystemClock.uptimeMillis();
            if (time - channelSetupClickedTime < 2000) return;
            channelSetupClickedTime = time;

            SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
            if(controller != null) {
                if (source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.US6) || source.equals(SettingKeys.TVSource.KM4) || source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2) || source.equals(SettingKeys.TVSource.US7))
                    controller.close();
                controller.destroy();
            }
            else if(tuner != null) {
                tuner.destroy();
            }
            if(source.useNewLib()) {
                getContext().startActivity(new Intent(getContext(), TVChannelSetupActivityNew.class));
            }
            else {
                getContext().startActivity(new Intent(getContext(), TVChannelSetupActivity.class));
            }
        });
        dataMatrix.createEventAdapter(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION, s -> {
            if(controller != null) {
                controller.loadChannels();
            }
            else if(tuner != null) {
                tuner.tuner().loadSettings();
            }
            updateDefaultChannel();
        });

        // This is specifically for IPTV.
        CheckBox sap = findViewById(R.id.eng_tv_use_sap);
        sap.setChecked(DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_IPTV_USE_SAP_BOOL));
        sap.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.TV.DATA_TV_IPTV_USE_SAP_BOOL, isChecked));
    }

    private DialogBase dialog;

    private void onTunerUpdateComplete(boolean success, ATSC_STATUS status) {
        Log.d(TAG, "Tuner update result " + status);
        if(dialog instanceof ProgressDialog) {
            if (status != ATSC_STATUS.PROCESSING || success) {
                ((ProgressDialog) dialog).onProgress().progress(100, true, getContext().getString(success ? R.string.update_complete_tc : R.string.update_failed_tc));
            }
        }
        else if(dialog instanceof SimpleMessageDialog) {
            ((SimpleMessageDialog)dialog).init(getContext().getString( success ? R.string.update_complete_tc : R.string.update_failed_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
        }
    }

    private void updateUI() {
        SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")); //NON-NLS

        if(source.useNewLib()) {
            tuner = TunerFactory.INSTANCE.getTuner(getContext(), source);
            if(tuner == null) {
                controller = TVControllerFactory.getTVController(getContext(), source);
                capabilities = controller.capabilities();
            }
            else {
                controller = null;
                capabilities = tuner.tuner().capabilities();
            }
        }
        else {
            if(tuner != null)
                tuner.destroy();
            tuner = null;
            controller = TVControllerFactory.getTVController(getContext(), source);
            capabilities = controller.capabilities();
        }

        Handler handler = new Handler();
        Button updates = findViewById(R.id.eng_tv_check_for_updates);
        updates.setVisibility(capabilities.update() ? View.VISIBLE : View.GONE);
        updates.setOnClickListener( view -> {
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
            if(controller != null) {
                if (source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.US6) || source.equals(SettingKeys.TVSource.KM4) || source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2) || source.equals(SettingKeys.TVSource.US7))
                    controller.open();
                boolean progress = controller.updateFirmware(new TVControllerBase.IOnProgress() {
                    @Override
                    public void onProgress(int progressPercentage) {
                        handler.post(() -> {
                            if (dialog instanceof ProgressDialog) {
                                ((ProgressDialog) dialog).onProgress().progress(progressPercentage, false, null);
                            }
                        });

                    }

                    @Override
                    public void onComplete(boolean success, ATSC_STATUS status) {
                        handler.post(() -> onTunerUpdateComplete(success, status));
                    }
                });
                if (progress) {
                    dialog = ProjectDialogFactory.progressDialog(view, getContext().getString(R.string.update_tc), getContext().getString(R.string.update_in_progress_tc), "", false, null, null);
                } else {
                    dialog = ProjectDialogFactory.message(view, getContext().getString(R.string.update_in_progress_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
                }
            }
            else if(tuner != null) {
                EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
                tuner.upgradeTuner(view, StorageUtils.getUSBPath(), (progress, done, error) -> {
                    if(done) {
                        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
                    }
                });
            }
        });

        switch (source) {

            case US4:
            case US8:
            case US9:
            case KM6:
            case KM7:
            case BR3:
            case JP1:
                findViewById(R.id.eng_tv_use_sap).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_settop_box_selection).setVisibility(View.GONE);
                findViewById(R.id.eng_stb_tv_import).setVisibility(View.GONE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                //findViewById(R.id.eng_tv_player_over_screen_size).setVisibility(View.VISIBLE);
                //findViewById(R.id.eng_tv_player_aspect_ratio).setVisibility(View.VISIBLE);
                DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING, "ENSEO 3300 RS232");

                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                break;
            case STB:
                findViewById(R.id.eng_tv_use_sap).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_settop_box_selection).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_stb_tv_import).setVisibility(View.VISIBLE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.VISIBLE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(DataMatrix.getBoolean(SettingKeys.TV.DATA_SHOW_STB_ADVANCED_SETTINGS_BOOL) ? View.VISIBLE : View.GONE);
                //findViewById(R.id.eng_tv_player_over_screen_size).setVisibility(View.VISIBLE);
                //findViewById(R.id.eng_tv_player_aspect_ratio).setVisibility(View.VISIBLE);
                updateSTBList();
                break;
            case US5:
            case US6:
            case KM4:
            case KM5:
            case BR2:
            case JP0:
            case CAB:
            case US7:
                findViewById(R.id.eng_tv_use_sap).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_settop_box_selection).setVisibility(View.GONE);
                findViewById(R.id.eng_stb_tv_import).setVisibility(View.GONE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                //findViewById(R.id.eng_tv_player_over_screen_size).setVisibility(View.VISIBLE);
                //findViewById(R.id.eng_tv_player_aspect_ratio).setVisibility(View.VISIBLE);
                break;
            case IPTV:
                findViewById(R.id.eng_tv_settop_box_selection).setVisibility(View.GONE);
                findViewById(R.id.eng_stb_tv_import).setVisibility(View.GONE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                //findViewById(R.id.eng_tv_player_over_screen_size).setVisibility(View.GONE);
                //findViewById(R.id.eng_tv_player_aspect_ratio).setVisibility(View.GONE);
                findViewById(R.id.eng_tv_use_sap).setVisibility(View.VISIBLE);
                findViewById(R.id.stb_tv_timing_settings).setVisibility(View.GONE);
                break;

        }
        updateDefaultChannel();

    }

    private void updateDefaultChannel() {
        SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
        // for SK NET set top box function
        if(source.equals(SettingKeys.TVSource.STB) && controller.getSelectedSetTopBox().contains("SK NET")) {
            findViewById(R.id.eng_tv_default_channel_selector_1).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_tv_player_default_channel_input_1).setVisibility(View.GONE);
            OptionSelectorItem defaultChannel = findViewById(R.id.eng_tv_default_channel_selector_1);
            String[] channelNames = new String[controller.sknetChannels().size()];
            for (int i = 0; i < controller.sknetChannels().size(); i++) {
                channelNames[i] = controller.sknetChannels().get(i).getNumber() + " - " + controller.sknetChannels().get(i).getServiceName();
            }
            defaultChannel.setOnOptionSelected(null);
            defaultChannel.setOptions(new OptionManager(channelNames, controller.defaultChannelInt()));
            defaultChannel.setOnOptionSelected((pos, option) -> controller.defaultChannel(pos));
            if (controller.sknetChannels().size() == 0) {
                defaultChannel.setText("");
            } else {
                controller.defaultChannel(controller.defaultChannelInt());
            }
        }
        else {
            if(controller != null) {
                if (capabilities.channelList() && !(capabilities.isSetTopBox() && controller.channels().isEmpty())) {
                    findViewById(R.id.eng_tv_default_channel_selector_1).setVisibility(View.VISIBLE);
                    findViewById(R.id.eng_tv_player_default_channel_input_1).setVisibility(View.GONE);
                    OptionSelectorItem defaultChannel = findViewById(R.id.eng_tv_default_channel_selector_1);
                    String[] channelNames = new String[controller.channels().size()];
                    for (int i = 0; i < controller.channels().size(); i++) {
                        channelNames[i] = controller.channels().get(i).number() + " - " + controller.channels().get(i).name();
                    }
                    defaultChannel.setOnOptionSelected(null);
                    defaultChannel.setOptions(new OptionManager(channelNames, controller.defaultChannelInt()));
                    defaultChannel.setOnOptionSelected((pos, option) -> controller.defaultChannel(pos));
                    if (controller.channels().size() == 0) {
                        defaultChannel.setText("");
                    } else {
                        controller.defaultChannel(controller.defaultChannelInt());
                    }
                } else {
                    findViewById(R.id.eng_tv_default_channel_selector_1).setVisibility(View.GONE);
                    findViewById(R.id.eng_tv_player_default_channel_input_1).setVisibility(View.VISIBLE);
                    ((UserTextInput) findViewById(R.id.eng_tv_player_default_channel_input_1)).setText(controller.defaultChannelString());
                }
            }
            else if(tuner != null) {
                if (capabilities.channelList() && !(capabilities.isSetTopBox() && tuner.tuner().getChannelList(false).isEmpty())) {
                    findViewById(R.id.eng_tv_default_channel_selector_1).setVisibility(View.VISIBLE);
                    findViewById(R.id.eng_tv_player_default_channel_input_1).setVisibility(View.GONE);
                    OptionSelectorItem defaultChannel = findViewById(R.id.eng_tv_default_channel_selector_1);
                    List<String> channelNames = new ArrayList<>();
                    for(Object channel : tuner.tuner().getChannelList(false)) {
                        if(channel instanceof IChannel) {
                            channelNames.add(((IChannel)channel).displayName(null));
                        }

                    }
                    // Resolve default by UUID so the selection matches what FTSU wrote
                    // And fall back to the INT key for devices that have not migrated yet
                    int defaultIndex = -1;
                    String storedUuid = "";
                    try { storedUuid = tuner.getDefaultChannel(); } catch (UnsupportedOperationException ignored) {}
                    if (!storedUuid.isEmpty()) {
                        List<? extends IChannel> channelListForDefault = tuner.tuner().getChannelList(false);
                        for (int i = 0; i < channelListForDefault.size(); i++) {
                            try {
                                if (storedUuid.equals(channelListForDefault.get(i).uuid())) {
                                    defaultIndex = i;
                                    break;
                                }
                            } catch (UnsupportedOperationException ignored) {}
                        }
                    }
                    if (defaultIndex < 0) defaultIndex = tuner.getDefaultChannelInt();
                    if (defaultIndex < 0 || defaultIndex >= channelNames.size()) defaultIndex = 0;
                    defaultChannel.setOnOptionSelected(null);
                    defaultChannel.setOptions(new OptionManager(channelNames.toArray(new String[0]), defaultIndex));
                    defaultChannel.setOnOptionSelected((pos, option) -> {
                        tuner.setDefaultChannel(pos);
                        List<? extends IChannel> chs = tuner.tuner().getChannelList(false);
                        if (pos >= 0 && pos < chs.size()) {
                            try { tuner.setDefaultChannel(chs.get(pos).uuid()); } catch (UnsupportedOperationException ignored) {}
                        }
                    });
                    if (tuner.tuner().getChannelList(false).size() == 0) {
                        defaultChannel.setText("");
                    }
                } else {
                    findViewById(R.id.eng_tv_default_channel_selector_1).setVisibility(View.GONE);
                    findViewById(R.id.eng_tv_player_default_channel_input_1).setVisibility(View.VISIBLE);
                    ((UserTextInput) findViewById(R.id.eng_tv_player_default_channel_input_1)).setText(tuner.getDefaultChannel());
                }
            }
        }
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
        if(tuner != null)
            tuner.destroy();
        if(controller != null) {
            controller.destroy();
        }
    }

    private void updateSTBList() {
        SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")); //NON-NLS
        if(!source.equals(SettingKeys.TVSource.STB))
            return;

        if(controller instanceof SetTopBoxController) {
            currentController = (SetTopBoxController) controller;
            currentController.reload();

            String[] list = controller.getSetTopBoxList().toArray(new String[0]);
            int optionIndex = -1;
            String currentSTB = DataMatrix.getString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING);
            for (int i = 0; i < list.length; i++) {
                if (list[i].equalsIgnoreCase(currentSTB)) {
                    optionIndex = i;
                    break;
                }
            }
            OptionSelectorItem setTopBoxOptions = findViewById(R.id.eng_tv_settop_box_selection);
            setTopBoxOptions.setOptions(new OptionManager(list, optionIndex));
            setTopBoxOptions.setOnOptionSelected((pos, option) -> {
                DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING, list[pos]);
                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                //currentController.reload();
                setTopBoxOptions.postDelayed(this::updateRemoteButtonList, 200);
            });
            if (optionIndex != -1) {
                updateRemoteButtonList();
            }
        }
    }

    private void updateRemoteButtonList() {
        List<String> buttonNames = new ArrayList<>();
        OptionSelectorItem remoteButtonOptions = findViewById(R.id.eng_tv_stb_remote_button);
        currentSTB = currentController.getCurrentSTB();
        if(currentSTB != null) {
            frequency.setText(currentSTB.frequencyKhz + "");
            repeats.setText(currentSTB.repeats + "");
            for (Map.Entry<String, SetTopBoxSetting.RemoteButton> buttons : currentSTB.remoteButtonMap.entrySet()) {
                buttonNames.add(buttons.getKey());
            }
            remoteButtonOptions.setOptions(new OptionManager(buttonNames.toArray(new String[0]), 0));

            remoteButtonOptions.setOnOptionSelected((pos, option) -> updateRemoteTimings(currentSTB.remoteButtonMap.get(buttonNames.get(pos))));
            updateRemoteTimings(currentSTB.remoteButtonMap.get(buttonNames.get(0)));
        }
    }

    private void updateRemoteTimings(SetTopBoxSetting.RemoteButton button) {
        currentRemoteButton = button;
        UserTextInput original = findViewById(R.id.eng_tv_stb_button_original);
        UserTextInput newSequence = findViewById(R.id.eng_tv_stb_button_new);
        StringBuilder stringBuilder = new StringBuilder();
        boolean firstTime = true;
        for(int sequence : button.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence) {
            if(!firstTime) {
                stringBuilder.append(", ");
            }
            firstTime = false;
            stringBuilder.append(sequence);
        }
        original.setText(stringBuilder.toString());
        newSequence.setText(stringBuilder.toString());
    }
}
