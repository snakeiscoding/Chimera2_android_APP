
package com.jht.chimera.ui.machinesettings;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.androiduiwidgets.SimpleInputConfiguration;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SimpleSelectorItem;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.KM5Tuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.chimera.lib.tv.tuner.US4Tuner;
import com.jht.chimera.lib.tv.tuner.US5Tuner;
import com.jht.chimera.lib.tv.tuner.US6Tuner;
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500Tuner;
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500TunerBase;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgRegion;
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETuner;
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETunerBase;
import com.jht.chimera.lib.widget.activity.HDMIActivity;
import com.jht.chimera.lib.widget.tv.TVColorButtons;
import com.jht.chimera.lib.widget.tv.TVDefaultChannel;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.chimera.lib.widget.tv.TVKeypad;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferError;
import com.jht.chimera.service.systemsettings.tv_channel_manager.ChannelListTransferEvent;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.pages.tv.TVChannelSelectorItemNew;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.cab.CabCommand;
import com.jht.serialport.peripheral.tuner.channel.IChannel;
import com.jht.serialport.peripheral.tuner.channel.SimpleIPTVChannel;
import com.jht.serialport.peripheral.tuner.enseo.Enseo;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel;
import com.jht.serialport.peripheral.tuner.enseo.EnseoUDPChannel;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * This houses the Engineering screen activity which is made up of several pages that you can navigate
 * using a side menu.
 *
 */

public class TVChannelSetupActivityNew extends HDMIActivity {

    // The active TV source.
    private SettingKeys.TVSource source;

    // Secondary source for CAB.
    private SettingKeys.TVSource secondarySource;

    // The main tuner.
    private IChimeraTuner<? extends IChannel> tuner;

    // Secondary tuner for CAB
    private IChimeraTuner<? extends IChannel> secondaryTuner;

    // The active tuner that we are configuring.
    private IChimeraTuner<? extends IChannel> activeTuner;

    // Current channel setup mode.
    private int activeTunerMode = 0;

    // The number keypad to send channel commands.
    private TVKeypad keypad;

    // Extended keypad with tuner remote buttons.
    private TVExtendedControls extendedControls;

    // The color buttons.  Generally not used anymore.
    private TVColorButtons colorButtons;

    // Buttons for the channel setup mode if applicable.
    private Button channelSetupMode1;
    private Button channelSetupMode2;

    // Channel list for the active tuner.
    private RecyclerView channelList;

    // For CAB this is used to set the secondary tuner.
    private OptionSelectorItem secondaryTunerOptions;
    private ConstraintLayout commandMappingContainer;
    private OptionSelectorItem enterCommandSelector;

    // The active tuner operations that may change based on the setup mode.
    private OptionSelectorItem tunerOperations;
    private IChimeraTuner.TunerOperation[] operations = null;

    private final DataMatrix dataMatrix = new DataMatrix();

    private Handler handler;


    public TVChannelSetupActivityNew() {
        EngineeringIdleTimerManager.idleTimerManagement = new IdleTimerManagement();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        setContentView(R.layout.tv_channel_setup);
        findViewById(R.id.tv_channel_setup_back).setOnClickListener(view -> finish());
        keypad = findViewById(R.id.tv_channel_setup_keypad);
        extendedControls = findViewById(R.id.tv_channel_setup_extended_controls);
        colorButtons = findViewById(R.id.tv_channel_setup_color);

        channelSetupMode1 = findViewById(R.id.channel_setup_mode_1);
        channelSetupMode2 = findViewById(R.id.channel_setup_mode_2);
        channelList = findViewById(R.id.eng_tv_channel_list);
        tunerOperations = findViewById(R.id.tv_channel_setup_operation);
        findViewById(R.id.tv_channel_setup_operation_button).setOnClickListener(view -> executeTunerOperation());
        secondaryTunerOptions = findViewById(R.id.tv_channel_setup_secondary_tuner);
        commandMappingContainer = findViewById(R.id.command_mapping_container);
        enterCommandSelector = findViewById(R.id.enter_command_selector);

        SelectorDefaultAnimations.setDefaultAnimations(this, channelList);
    }

    @Override
    public void onPause() {
        super.onPause();
        setVolume(AudioSelectManager.getMasterVolume());
        tuner.resetTunerVolume(ProductConfig.getTunerDefaultVolume(source));
        tuner.destroy();
        if(secondaryTuner != null) {
            secondaryTuner.resetTunerVolume(ProductConfig.getTunerDefaultVolume(secondarySource));
            secondaryTuner.destroy();
        }
        dataMatrix.finish();
    }


    @Override
    public void onResume() {

        // Get the tv tuner(s).
        source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")); //NON-NLS
        activeTuner = tuner = TunerFactory.INSTANCE.getTuner(this, source);
        secondarySource = null;
        secondaryTuner = null;
        if(tuner != null) {

            // If CAB we need to get the secondary tuner.
            if(source.equals(SettingKeys.TVSource.CAB)) {
                secondarySource = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_CAB_TV_SOURCE_STRING)); //NON-NLS
                List<SettingKeys.TVSource> availableTuners = ProductConfig.getAvailableTuners();
                if (availableTuners.contains(secondarySource)) {
                    secondaryTuner = secondarySource.equals(SettingKeys.TVSource.CAB) ? tuner : TunerFactory.INSTANCE.getTuner(this, secondarySource);
                } else {
                    secondarySource = source;
                    secondaryTuner = tuner;
                }
                int secondaryTunerSelection = 0;
                SettingKeys.TVSource[] sources = SettingKeys.TVSource.getCABList();
                String[] secondaryLabels = new String[sources.length];
                for (int i = 0; i < sources.length; i++) {
                    secondaryLabels[i] = sources[i].displayNameAlt();
                    if(sources[i].equals(secondarySource)) {
                        secondaryTunerSelection = i;
                    }
                }
                secondaryTunerOptions.setOptions(new OptionManager(secondaryLabels, secondaryTunerSelection));
                secondaryTunerOptions.setOnOptionSelected((pos, option) -> {
                    secondarySource = sources[pos];
                    DataMatrix.putString(SettingKeys.TV.DATA_CAB_TV_SOURCE_STRING, secondarySource.toString());
                    secondaryTuner = sources[pos].equals(SettingKeys.TVSource.CAB) ? tuner : TunerFactory.INSTANCE.getTuner(this, secondarySource);
                    activeTuner = secondaryTuner;
                    initUI(0);
                });
                activeTuner = secondaryTuner;
            }

            // Get the capabilities.
            ITunerCapabilities capabilities = tuner.tuner().capabilities();

            // Turn on the camera if we are HDMI.
            if (capabilities.hdmi()) {
                setProperties(findViewById(R.id.eng_tv_player_surface), findViewById(R.id.eng_tv_player_texture), source.equals(SettingKeys.TVSource.US4) ? 250 : 0, () -> tuner.resetSerialPort(false));
                initHDMI();
            }

            // Initialize the channel setup modes.
            if(tuner.getChannelSetupModes() == 1) {
                channelSetupMode1.setVisibility(View.GONE);
                channelSetupMode2.setVisibility(View.GONE);
                initUI(0);
            }
            else if(tuner.getChannelSetupModes() == 2) {
                channelSetupMode1.setVisibility(View.VISIBLE);
                channelSetupMode2.setVisibility(View.VISIBLE);
                channelSetupMode1.setText(tuner.channelSetupModeString(this, 0));
                channelSetupMode2.setText(tuner.channelSetupModeString(this, 1));
                channelSetupMode1.setOnClickListener(view -> {
                    if(!channelSetupMode1.isActivated()) {
                        if(secondaryTuner != null) {
                            activeTuner = secondaryTuner;
                        }
                        initUI(0);
                    }
                });
                channelSetupMode2.setOnClickListener(view -> {
                    if(!channelSetupMode2.isActivated()) {
                        activeTuner = tuner;
                        initUI(1);
                    }
                });
                initUI(tuner.getDefaultMode());

            }
            else {
                // Should not get here.
                throw new UnsupportedOperationException();
            }


        }

        // Init the system volume.
        SimpleInputConfiguration volumeConfiguration = new SimpleInputConfiguration();
        PillBox systemVolume = findViewById(R.id.system_volume);
        volumeConfiguration.min(0);
        volumeConfiguration.max(30);

        volumeConfiguration.value(AudioSelectManager.getMasterVolume());
        volumeConfiguration.label(getString(R.string.master_volume_tc));
        systemVolume.initParameterSettings(volumeConfiguration);
        systemVolume.setiValueSelectorInterface((v, s) -> setVolume((int)v));

        dataMatrix.createEventAdapter(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION, s -> ((TVDefaultChannel) findViewById(R.id.tv_setup_default_channel)).reset());

        dataMatrix.createParcelableAdapter(
            ImportExportKeys.EVENT_FINISHED_TV_CHANNEL_TRANSFER_PARCELABLE,
            ChannelListTransferEvent.class,
            this::onChannelListTransferFinished
        , false);

        // Place this at the end to make sure we have updated the tuners for the audio controller.
        super.onResume();
    }

    private void initUI(int tunerModeIndex) {
        activeTunerMode = tunerModeIndex;

        secondaryTunerOptions.setVisibility(source.equals(SettingKeys.TVSource.CAB) && tunerModeIndex == 0 ? View.VISIBLE : View.GONE);

        // Setup CAB remote mapping UI
        if (source.equals(SettingKeys.TVSource.CAB) && tunerModeIndex == 1) {
            String[] cabEnterCommandOptions = CabCommand.EnterCommand.Companion.asList()
                    .stream()
                    .map(CabCommand.EnterCommand::getDisplayName)
                    .toArray(String[]::new);
            enterCommandSelector.setOptions(new OptionManager(cabEnterCommandOptions));
            CabCommand.EnterCommand enterCommand = CabCommand.EnterCommand.Companion.fromDisplayName(DataMatrix.getString(SettingKeys.TV.DATA_CAB_ENTER_COMMAND_STRING));
            int enterCommandIndex = enterCommand != null ? CabCommand.EnterCommand.Companion.asList().indexOf(enterCommand) : 0;
            enterCommandSelector.selectItem(enterCommandIndex > -1 ? enterCommandIndex : 0);
            enterCommandSelector.setOnOptionSelected((pos, selectedEnterCommandOption) -> DataMatrix.putString(SettingKeys.TV.DATA_CAB_ENTER_COMMAND_STRING, selectedEnterCommandOption));
            commandMappingContainer.setVisibility(View.VISIBLE);
        } else {
            commandMappingContainer.setVisibility(View.GONE);
        }

        // Setup the mode buttons.
        channelSetupMode1.setActivated(tunerModeIndex == 0);
        channelSetupMode2.setActivated(tunerModeIndex == 1);

        // Load the operations.
        operations = activeTuner.getSupportedOperations(tunerModeIndex);

        if(operations != null && operations.length > 0) {
            String[] operationLabels = new String[operations.length];
            for (int i = 0; i < operations.length; i++) {
                operationLabels[i] = getString(operations[i].labelResId());
            }
            tunerOperations.setVisibility(View.VISIBLE);
            findViewById(R.id.tv_channel_setup_operation_button).setVisibility(View.VISIBLE);
            tunerOperations.setOptions(new OptionManager(operationLabels, 0));
        }
        else {
            tunerOperations.setVisibility(View.GONE);
            findViewById(R.id.tv_channel_setup_operation_button).setVisibility(View.GONE);
        }

        if(source.equals(SettingKeys.TVSource.CAB) && tunerModeIndex == 0) {
            // Interface changes based on whether we bypass the tuner or have a secondary tuner.
            if(secondaryTuner != null) {
                if(secondarySource.equals(SettingKeys.TVSource.CAB)) {
                    // Straight HDMI.  There is nothing really to do on this page.
                    keypad.setVisibility(View.GONE);
                    extendedControls.setVisibility(View.GONE);
                    colorButtons.setVisibility(View.GONE);
                    findViewById(R.id.tv_setup_default_channel).setVisibility(View.GONE);
                    findViewById(R.id.tv_tuner_volume).setVisibility(View.GONE);
                    findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.GONE);
                }
                else {
                    // We have a secondary tuner.  Update the interface per that tuner.
                    setupUI(secondaryTuner);
                }
            }
            else {
                setupUI(tuner);
            }
        }
        else {
            setupUI(tuner);
        }
    }

    private void setExtendedControlCornerButtons() {
        if (source == SettingKeys.TVSource.US4) {
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.TOP_LEFT, TVExtendedControls.CORNER_BUTTON_OPERATION.MENU);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.TOP_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.SETUP);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_LEFT, TVExtendedControls.CORNER_BUTTON_OPERATION.INFO);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.CC);
        }
        if (source.isLG()) {
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.TOP_LEFT, TVExtendedControls.CORNER_BUTTON_OPERATION.MENU);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.TOP_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.EXIT);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_LEFT, TVExtendedControls.CORNER_BUTTON_OPERATION.SETUP);
            extendedControls.setCornerButton(TVExtendedControls.CORNER_BUTTONS.BOTTOM_RIGHT, TVExtendedControls.CORNER_BUTTON_OPERATION.CC);
        }
    }

    private void setupUI(IChimeraTuner<?> tuner) {
        ITunerCapabilities capabilities = tuner.tuner().capabilities();
        keypad.enableDash(capabilities.dash());
        keypad.setVisibility(capabilities.keypad() ? View.VISIBLE : View.GONE);
        keypad.setTuner(tuner);
        setExtendedControlCornerButtons();
        extendedControls.setTuner(tuner);
        extendedControls.setVisibility(View.VISIBLE);
        colorButtons.setTuner(tuner);
        colorButtons.setVisibility(capabilities.colorButtons() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_setup_default_channel).setVisibility(View.VISIBLE);
        ((TVDefaultChannel) findViewById(R.id.tv_setup_default_channel)).setTuner(tuner);

        findViewById(R.id.tv_tuner_volume).setVisibility(View.VISIBLE);
        findViewById(R.id.cab_settings_container).setVisibility(View.GONE);

        updateChannelList(tuner);

        // Get the tuner properties.  If volume is 0 retry.  Do the retry three times.
        PillBox tunerVolume = findViewById(R.id.tv_tuner_volume);
        if(capabilities.volume()) {
            SimpleInputConfiguration tunerVolumeConfiguration = new SimpleInputConfiguration();
            tunerVolumeConfiguration.min(0);
            tunerVolumeConfiguration.max(100);
            tunerVolumeConfiguration.label(getString(R.string.tv_volume_tc));
            tunerVolume.initParameterSettings(tunerVolumeConfiguration);
            SettingKeys.TVSource activeSource = (source.equals(SettingKeys.TVSource.CAB) && !secondarySource.equals(SettingKeys.TVSource.CAB)) ? secondarySource : source;
            if(ProductConfig.getTunerDefaultVolume(activeSource) > 0) {
                tunerVolume.setValue(ProductConfig.getTunerDefaultVolume(activeSource));
            }
            tunerVolume.setiValueSelectorInterface((v, s) -> tuner.tuner().setTunerVolume(v));
            tunerVolume.setEnabled(true);
        }
        else {
            tunerVolume.setVisibility(View.INVISIBLE);
        }
    }

    private void updateChannelList(IChimeraTuner<?> tuner) {
        handler.post(() -> {
            IChimeraTuner.SETTINGS_MODE mode = tuner.getModeType(activeTunerMode);
            switch (mode) {
                case IP_SCAN_LIST:
                    updateIPListAdapter();
                    findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.VISIBLE);
                    break;
                case CHANNEL_LIST:
                    if(tuner.tuner().capabilities().channelList()) {
                        findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.VISIBLE);
                        loadChannelList(tuner);
                    }
                    else {
                        findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.GONE);
                    }
                    break;
                case NO_LIST:
                    findViewById(R.id.eng_tv_channel_list_container).setVisibility(View.GONE);
                    break;
            }

        });
    }


    private void updateIPListAdapter() {
        SimpleSelectorItem channels = new SimpleSelectorItem("", new ArrayList<>()); //NON-NLS
        for(SimpleIPTVChannel channel : tuner.tuner().getIPTVList()) {
            channels.addItem(new TVChannelSelectorItemNew<>(null, channel, ((action,item) -> {
                if(action == TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_EDIT) {
                    ProjectDialogFactory.editSimpleIPTVChannelDialog(channelList, channel, tuner.tuner().getIPTVList(), () -> {
                        tuner.tuner().saveSettings();
                        updateIPListAdapter();
                    });
                }
                else if(action == TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_DELETE) {
                    tuner.tuner().getIPTVList().remove(channel);
                    tuner.tuner().saveSettings();
                    updateIPListAdapter();
                }
            })));
        }
        channels.addItem(new TVChannelSelectorItemNew<SimpleIPTVChannel>(null, null, ((action, item) -> {
            if(action == TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_EDIT) {
                SimpleIPTVChannel ch = new SimpleIPTVChannel("", "");
                ProjectDialogFactory.editSimpleIPTVChannelDialog(channelList, ch, tuner.tuner().getIPTVList(), () -> {
                    tuner.tuner().getIPTVList().add(ch);
                    tuner.tuner().saveSettings();
                    updateIPListAdapter();
                });
            }
        })));

        channelList.setAdapter(new StandardSelectorItemAdapter(this, channels, null));
    }

    private void loadChannelList(IChimeraTuner<? extends IChannel> channelListTuner) {

        SelectorItemBase channels = new SelectorItemBase(new ArrayList<>()); //NON-NLS
        channelListTuner.createChannelSelectorItems(channels, ((action, item) -> {
            if(action == TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_EDIT) {
                if((tuner instanceof US6Tuner || tuner instanceof US5Tuner || tuner instanceof KM5Tuner) && item.channel() instanceof SwiftChannel) {
                    ProjectDialogFactory.editChannelDialog(channelList, (SwiftChannel)item.channel(), () -> {
                        StandardSelectorItemAdapter adapter = (StandardSelectorItemAdapter) channelList.getAdapter();
                        if(adapter != null) {
                            adapter.notifyItemChanged(item.getIndex());
                            ((TVDefaultChannel) findViewById(R.id.tv_setup_default_channel)).setTuner(tuner);
                            tuner.tuner().saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                        }
                    });
                }
                else if(tuner instanceof US4Tuner) {
                    Enseo enseo = (Enseo) tuner.tuner();
                    List<EnseoChannel> us4Channels = enseo.getChannelList(true);
                    if(item.channel() == null) {
                        EnseoChannel newChannel;
                        if(us4Channels.isEmpty()) {
                            newChannel = new EnseoUDPChannel("" , "");
                        }
                        else {
                            newChannel = enseo.getChannelList(true).get(enseo.getChannelList(true).size() - 1).createCopy();
                        }
                        ProjectDialogFactory.editEnseoChannelDialog(channelList, newChannel, false, us4Channels, enseoChannel -> {
                            us4Channels.add(enseoChannel);
                            enseo.saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }
                    else if(item.channel() instanceof EnseoChannel){
                        ProjectDialogFactory.editEnseoChannelDialog(channelList, (EnseoChannel) item.channel(), false, us4Channels, enseoChannel -> {
                            us4Channels.set(item.getIndex(), enseoChannel);
                            tuner.tuner().saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }

                }
                else if(tuner instanceof LG6500Tuner) {
                    // Note: LG6500 channel list uses the same channel class for Enseo
                    LG6500TunerBase lg6500TunerBase = (LG6500TunerBase) tuner.tuner();
                    List<EnseoChannel> lg6500Channels = lg6500TunerBase.getChannelList(true);
                    if(item.channel() == null) {
                        EnseoChannel newChannel;
                        if(lg6500Channels.isEmpty()) {
                            newChannel = new EnseoUDPChannel("" , "");
                        }
                        else {
                            newChannel = lg6500TunerBase.getChannelList(true).get(lg6500TunerBase.getChannelList(true).size() - 1).createCopy();
                        }
                        ProjectDialogFactory.editEnseoChannelDialog(channelList, newChannel, false, lg6500Channels, enseoChannel -> {
                            lg6500Channels.add(enseoChannel);
                            lg6500TunerBase.saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }
                    else if(item.channel() instanceof EnseoChannel){
                        ProjectDialogFactory.editEnseoChannelDialog(channelList, (EnseoChannel) item.channel(), false, lg6500Channels, enseoChannel -> {
                            lg6500Channels.set(item.getIndex(), enseoChannel);
                            tuner.tuner().saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }
                }
                else if(tuner instanceof Lg750ETuner) {
                    Lg750ETuner lg750ETuner = (Lg750ETuner) tuner;
                    Lg750ETunerBase lg750ETunerBase = (Lg750ETunerBase) tuner.tuner();
                    List<LgChannel> lg750EChannels = lg750ETunerBase.getChannelList(true);
                    List<LgChannel.ChannelType> supportedTypes = lg750ETuner.getVariant().getSupportedChannelTypes();
                    LgRegion region = lg750ETuner.getVariant().getChannelRegion();
                    if(item.channel() == null) {
                        LgChannel newChannel = lg750EChannels.isEmpty()
                                ? LgChannel.createDefault(supportedTypes.isEmpty() ? LgChannel.ChannelType.IP_CLASS_2 : supportedTypes.get(0))
                                : lg750EChannels.get(lg750EChannels.size() - 1).createCopy();
                        ProjectDialogFactory.editLgChannelDialog(channelList, newChannel, supportedTypes, region, lg750EChannels, lgChannel -> {
                            lg750EChannels.add(lgChannel);
                            lg750ETunerBase.saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }
                    else if(item.channel() instanceof LgChannel) {
                        ProjectDialogFactory.editLgChannelDialog(channelList, (LgChannel) item.channel(), supportedTypes, region, lg750EChannels, lgChannel -> {
                            lg750EChannels.set(item.getIndex(), lgChannel);
                            tuner.tuner().saveSettings();
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                            loadChannelList(tuner);
                        });
                    }
                }
            }
            else if(action == TVChannelSelectorItemNew.CHANNEL_ACTIONS.CHANNEL_DELETE) {
                // Adjust the default before reloading the UI using UUID comparison
                try {
                    String deletedUuid = item.channel().uuid();
                    String defaultUuid = channelListTuner.getDefaultChannel();
                    if (deletedUuid.equals(defaultUuid)) {
                        List<? extends IChannel> remaining = channelListTuner.tuner().getChannelList(false);
                        IChannel newDefault = null;
                        if (item.getIndex() > 0 && !remaining.isEmpty()) {
                            newDefault = remaining.get(item.getIndex() - 1);
                        } else if (!remaining.isEmpty()) {
                            newDefault = remaining.get(0);
                        }
                        if (newDefault != null) {
                            channelListTuner.setDefaultChannel(newDefault.uuid());
                        } else {
                            channelListTuner.setDefaultChannel("");
                            channelListTuner.setDefaultChannel(-1);
                        }
                    }
                } catch (UnsupportedOperationException ignored) {}
                loadChannelList(channelListTuner);
                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
            }
        }),     true, channelListTuner.tuner().capabilities().canSkipChannel());

        channelList.setAdapter(new StandardSelectorItemAdapter(this, channels, null));
    }

    private boolean hideTunerOutput() {
        return source.followsSwiftProtocol();
    }

    private void executeTunerOperation() {
        if(operations != null && tunerOperations.getSelectedPosition() >= 0 && tunerOperations.getSelectedPosition() < operations.length) {
            IChimeraTuner.TunerOperation op = operations[tunerOperations.getSelectedPosition()];
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
            IChimeraTuner<?> activeTuner = source.equals(SettingKeys.TVSource.CAB) && activeTunerMode == 0 ? secondaryTuner : tuner;
            if(hideTunerOutput()) {
                findViewById(R.id.tv_surface_overlay).setVisibility(View.VISIBLE);
                closeCamera();
            }
            switch (op) {
                case DOWNLOAD_CHANNEL_XML:
                    DataMatrix.sendEvent(ImportExportKeys.ACTION_IMPORT_TV_CHANNELS_INTERNET);
                    break;
                case UPLOAD_CHANNEL_XML:
                    DataMatrix.sendEvent(ImportExportKeys.ACTION_EXPORT_TV_CHANNELS_INTERNET);
                    break;
                default:
                    activeTuner.executeOperation(findViewById(R.id.tv_channel_setup_operation_button), op, StorageUtils.getUSBPath(), (progress, done, error) -> {
                        if (done) {
                            if (op.isChannelListImport()) { resetDefaultChannelToFirst(activeTuner); }
                            onExecuteTunerOperationComplete();
                        }
                    }, null);
            }
        }
    }

    private void onChannelListTransferFinished(ChannelListTransferEvent event) {
        boolean isImport = event.isImport();
        boolean transferSuccessful = event instanceof ChannelListTransferEvent.TransferSuccessful;
        String message = getChannelListTransferMessage(event);

        DialogFactory.message(
                findViewById(R.id.tv_channel_setup_operation_button),
                message,
                () -> {
                    if (isImport && transferSuccessful) {
                        activeTuner.tuner().loadSettings();
                        resetDefaultChannelToFirst(activeTuner);
                        onExecuteTunerOperationComplete();
                    }
                },
                SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK
        );
    }

    private String getChannelListTransferMessage(ChannelListTransferEvent event) {
        boolean isImport = event.isImport();

        if (event instanceof ChannelListTransferEvent.TransferSuccessful) {
            return isImport ? getString(R.string.tv_channel_list_download_success) : getString(R.string.tv_channel_list_upload_success);
        }

        if (event instanceof ChannelListTransferEvent.TransferFailed) {
            ChannelListTransferError error = ((ChannelListTransferEvent.TransferFailed) event).getError();
            if (error instanceof ChannelListTransferError.ChannelListNotFound) {
                return getString(R.string.unable_to_find_channel_list_on_internet_pg);
            }
            else if (error instanceof ChannelListTransferError.ChannelListAlreadyUpToDate) {
                return getString(R.string.tv_channel_list_download_not_needed);
            }
            else if (error instanceof ChannelListTransferError.UnsupportedTvSource) {
                return getString(R.string.tv_source_not_supported_pg);
            }
            else if (error instanceof ChannelListTransferError.AuthenticationError) {
                return getString(R.string.authentication_error_pg);
            }
            else if (error instanceof ChannelListTransferError.NetworkError) {
                return getString(R.string.network_error_pg);
            }
            else {
                return isImport ? getString(R.string.tv_channel_list_download_failure) : getString(R.string.tv_channel_list_upload_failure);
            }
        }

        return getString(R.string.unexpected_error_pg);
    }

    /**
     * Resets the default channel to the first visible channel in the tuner's channel list.
     * Should be called after a channel list import so the stale "pre-import" default is not silently
     * applied to a new channel set.
     */
    private void resetDefaultChannelToFirst(IChimeraTuner<?> tuner) {
        List<? extends IChannel> channels = tuner.tuner().getChannelList(false);
        if (!channels.isEmpty()) {
            IChannel first = channels.get(0);
            try {
                tuner.setDefaultChannel(first.uuid());
            } catch (UnsupportedOperationException ignored) {}
        }
    }

    private void onExecuteTunerOperationComplete() {
        DataMatrix.sendEvent(HALKeys.EVENT_TV_OPERATION_DONE);
        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
        updateChannelList(activeTuner);
        ((TVDefaultChannel) findViewById(R.id.tv_setup_default_channel)).setTuner(tuner);

        // In case of update grab the version again.
        new Thread(() -> {
            // Wait a little bit as we may be resetting the tuner properties.
            ThreadHelper.sleepIgnoreInterruption(5000);
            try {
                activeTuner.tuner().loadProperties((tunerProperties -> {
                    if(tunerProperties != null && tunerProperties.version() != null) {
                        DataMatrix.putString(TVKeys.DATA_TUNER_VERSION_STRING, tunerProperties.version());
                    }
                }));
            } catch (UnsupportedOperationException ignored) {
            }

            if(hideTunerOutput()) {
                handler.post(() -> {
                    findViewById(R.id.tv_surface_overlay).setVisibility(View.GONE);
                    openCamera();
                });
            }
        }).start();
    }

    @Override
    protected AudioSourceInput audioSourceInput() {
        return tuner.tuner().capabilities().hdmi() ? AudioSourceInput.HDMI : AudioSourceInput.Foreground;
    }

}



