package com.jht.chimera.lib.tv.tuner;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.ENABLE_LEGACY;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.INITIALIZE_TUNER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.enseo.Enseo;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel;

import java.io.File;
import java.io.IOException;

public class US4Tuner extends TunerBase<EnseoChannel> {

    private static class US4SerialPort extends SerialPort {
        private static class US4SerialPortConfiguration extends SerialPortConfiguration {
            US4SerialPortConfiguration() {
                baudRate = SerialPortConfiguration.BaudRate.B115200;
                CREAD = true;
                CLOCAL = true;
                csize = SerialPortConfiguration.CSIZE.CS8;
                makeRaw = true;
                NOCTTY = true;
                TRACE_DEBUG=false;
            }
        }
        private static final String deviceName = ProductConfig.portMappings.us4Path();

        private static class SingletonHolder {
            private static final US4SerialPort INSTANCE = new US4SerialPort();
        }

        public US4SerialPort() {
            super(deviceName, new US4SerialPortConfiguration());
        }

        static US4SerialPort getInstance() {
            return SingletonHolder.INSTANCE;
        }

        public void reset() {
            close();
            try {
                open();
            } catch (IOException ignored) {}
        }

    }

    private final Enseo tuner;
    private final Context context;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void checkSettingsFile(Context context) {
        File oldLocation = new File(context.getFilesDir(), "settings/tv_settings/set_top_boxes/rs232ChannelConfig.xml");
        File newLocation = new File(context.getFilesDir(), "settings/us4/channels.xml");
        if(oldLocation.exists() && !newLocation.exists()) {
            // We most likely had an upgrade and need to update the settings file into the new
            // location.  Also port the default channel setting.
            File settingsDir = newLocation.getParentFile();
            if(settingsDir != null) {
                settingsDir.mkdirs();
                FileUtils.writeToFile(
                        FileUtils.readFileToString(oldLocation).replace("RS232Channels", "IPTVChannels"), newLocation);
                oldLocation.delete();
            }
            DataMatrix.putInt(SettingKeys.TV.DATA_TV_US4_DEFAULT_CHANNEL_INT, DataMatrix.getInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT));
        }
    }

    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    public US4Tuner(@NonNull Context context) {
        super(new com.jht.serialport.peripheral.tuner.enseo.Enseo(context, new File(context.getFilesDir(), "settings/us4")));
        try {
            US4SerialPort.getInstance().open();
        } catch (IOException ignored) {
        }
        tuner = (Enseo) tuner();
        tuner.restart(US4SerialPort.getInstance().getInputStream(), US4SerialPort.getInstance().getOutputStream(), null);
        this.context = context;
    }
    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    @SuppressLint("VisibleForTests")
    public US4Tuner(Context context, ISerialPort serialPort) {
        super(new com.jht.serialport.peripheral.tuner.enseo.Enseo(context, new File(context.getFilesDir(), "settings/us4")));
        try {
            serialPort.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tuner = (Enseo) tuner();
        tuner.restart(serialPort.getInputStream(), serialPort.getOutputStream(), null);
        this.context = context;
    }
    /**
     *
     * @param channel   Set the default channel index.
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public void setDefaultChannel(int channel) {
        DataMatrix.putInt(SettingKeys.TV.DATA_TV_US4_DEFAULT_CHANNEL_INT, channel);
    }

    @Override
    public void upgradeTuner(@Nullable View view, @NonNull File upgradeFile, @Nullable IOnProgress onProgress) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return Get the default channel index.  This should be the index in the channel list
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public int getDefaultChannelInt() {
        return DataMatrix.getInt(SettingKeys.TV.DATA_TV_US4_DEFAULT_CHANNEL_INT);
    }

    @Override
    public void setDefaultChannel(@NonNull String channel) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_US4_DEFAULT_CHANNEL_UUID_STRING, channel);
    }

    @Override
    public String getDefaultChannel() {
        return DataMatrix.getString(SettingKeys.TV.DATA_TV_US4_DEFAULT_CHANNEL_UUID_STRING);
    }

    /**
     *
     * @return      A list of operations supported by the tuner.
     */
    @Override
    public TunerOperation[] getSupportedOperations(int mode) {
        return new TunerOperation[] {
                IMPORT_CHANNEL_LIST_FROM_USB,
                EXPORT_CHANNEL_LIST_TO_USB,
                EXPORT_CHANNEL_LIST_TO_TUNER,
                ENABLE_LEGACY,
                INITIALIZE_TUNER
        };
    }

    @Override
    public void reset() {
        setChannelToDefault();
    }

    @Override
    public void setChannelToDefault() {
        int channelIndex = getDefaultChannelInt();
        if(channelIndex >= 0 && channelIndex < tuner().getChannelList(false).size()) {
            tuner().setChannel(tuner().getChannelList(false).get(channelIndex));
            DataMatrix.putInt(TVKeys.DATA_US4_CURRENT_CHANNEL_INT, channelIndex);
            DataMatrix.putInt(TVKeys.DATA_US4_PREVIOUS_CHANNEL_INT, -1);
        }
        else {
            DataMatrix.putInt(TVKeys.DATA_US4_CURRENT_CHANNEL_INT, -1);
            DataMatrix.putInt(TVKeys.DATA_US4_PREVIOUS_CHANNEL_INT, -1);
        }
    }

    @Override
    public void channelUp() {
        tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_UP);
    }

    @Override
    public void channelDown() {
        tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_DOWN);
    }

    @Override
    public void setChannel(EnseoChannel channel) {
        tuner().setChannel(channel);
    }

    /**
     * For tuners with a channel list this returns the index of the current channel in the list.
     * @return  The current channel
     */
    public int getCurrentChannelIndex() {
        return DataMatrix.getInt(TVKeys.DATA_US4_CURRENT_CHANNEL_INT);
    }
    /**
     * Get the previous channel.  We only need this internally.
     *
     * @return  The previous channel
     */
    private int getLastChannelIndex() {
        return DataMatrix.getInt(TVKeys.DATA_US4_PREVIOUS_CHANNEL_INT);
    }

    @Override
    public void previousChannel() {
        int lastChannel = getLastChannelIndex();
        int currentChannel = getCurrentChannelIndex();

        if(lastChannel >= 0 && lastChannel < tuner().getChannelList(false).size() && lastChannel != currentChannel) {
            setChannel(tuner().getChannelList(false).get(lastChannel));
        }
    }

    @Override
    public DEFAULT_CHANNEL_TYPE getDefaultChannelType() {
        return DEFAULT_CHANNEL_TYPE.CHANNEL_INDEX;
    }

    @Override
    public boolean ensureTunerMatchesDetectedHardware(SettingKeys.TVSource source, boolean force) {
        return false;
    }


    /**
     * Shutdown all threads related to the tuner and cleanup.
     */
    @Override
    public void destroy() {
        super.destroy();
        US4SerialPort.getInstance().close();
    }


    /**
     * Execute an operation.
     *
     * @param view          The button used to explode the progress dialog from.  If this is automated
     *                      it can be null and there will be no dialog box.
     * @param operation     The operation to perform.
     * @param workingDir    The working directory where you will find files needed by the operation.
     * @param onProgress    Callback for the operation progress.
     * @throws UnsupportedOperationException    Not all tuners support all operations.
     */
    @Override
    public void executeOperation(@Nullable View view, @NonNull TunerOperation operation, @Nullable File workingDir, @Nullable IOnProgress onProgress, @Nullable DialogBase.IOnConfirm onConfirm) throws UnsupportedOperationException  {
        switch (operation) {
            case INITIALIZE_TUNER:
                tuner.initialize(initActionProgress(view, onProgress, context.getString(R.string.initialize_tc), context.getString(R.string.initializing_please_wait_pg), context.getString(R.string.complete_tc), false), ProductConfig.getTunerDefaultVolume(SettingKeys.TVSource.US4));
                break;
            case ENABLE_LEGACY:
                tuner.enableLegacy(initActionProgress(view, onProgress, context.getString(R.string.enseo_enable_legacy_tc), context.getString(R.string.enseo_enable_legacy_message_pg), context.getString(R.string.complete_tc), false), ProductConfig.getTunerDefaultVolume(SettingKeys.TVSource.US4));
                break;
            case IMPORT_CHANNEL_LIST_FROM_USB:
                importChannelListFromUSB(view, workingDir, onProgress);
                break;
            case EXPORT_CHANNEL_LIST_TO_USB:
                boolean result = tuner.exportChannelList(workingDir);
                ExecuteAsRoot.runSyncCommand();
                if(view != null) {
                    DialogFactory.message(view, context.getString(result ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }
                break;
            case EXPORT_CHANNEL_LIST_TO_TUNER:
                IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.export_channels_tc), context.getString(R.string.export_channels_to_tuner), context.getString(R.string.import_channels_success_tc), true);
                tuner.loadSettings();
                tuner.uploadChannelsToTuner(operationProgress);
                break;
            default:
                throw new UnsupportedOperationException("Tuner does not implement " + operation);
        }
    }

    private void importChannelListFromUSB(@Nullable View view, @Nullable File src, @Nullable IOnProgress onProgress) {
        if(tuner.importChannelList(src)) {
            IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.import_from_usb_tc), context.getString(R.string.export_channels_to_tuner), context.getString(R.string.import_channels_success_tc), true);
            tuner.loadSettings();
            tuner.uploadChannelsToTuner(operationProgress);
        }
        else {
            if(view != null) {
                DialogFactory.message(view, context.getString(R.string.import_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
            if(onProgress != null) {
                onProgress.progress(100, true, null);
            }
        }
    }


    @Override
    public void resetSerialPort(boolean update) {
        tuner().destroy();
        US4SerialPort.getInstance().reset();
        tuner().restart(US4SerialPort.getInstance().getInputStream(), US4SerialPort.getInstance().getOutputStream(), null);
    }

    @Override
    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {}
    @Override
    public void getVersion() {}
    @Override
    public void setLocale(String androidLocaleTag) {}
}