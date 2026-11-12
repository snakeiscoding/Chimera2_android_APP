package com.jht.chimera.lib.tv.tuner;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.util.ExecuteAsRoot;
import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.channel.IChannel;
import com.jht.serialport.serialcommunication.TransactionManager;

import java.io.File;

public abstract class SwiftTunerBase<Channel extends IChannel> extends TunerBase<Channel> {
    private static final String TAG = "SwiftTunerBase";
    // The tuner.
    private final com.jht.serialport.peripheral.tuner.swift.SwiftTunerBase<Channel> tuner;

    // Context needed for strings.
    private final Context context;


    /**
     * Starts up the serial port interface and initializes the tuner.
     *
     * @param tuner   The main tuner object.
     * @param context Context needed for strings.
     */
    public SwiftTunerBase(Context context, com.jht.serialport.peripheral.tuner.swift.SwiftTunerBase<Channel>tuner) {
        super(tuner);
        this.tuner = tuner;
        this.context = context;
    }

    /**
     * Upgrade the firmware with the file provided.  This takes care of the progress dialog as well
     * if needed.
     * @param view                  The button used to explode the progress dialog from.  If this is automated
     *                              it can be null and there will be no dialog box.
     * @param upgradeFileLocation   The firmware file (or location if the tuner needs a location).
     * @param onProgress            Callback on progress.
     */
    @Override
    public void upgradeTuner(@Nullable View view, @NonNull File upgradeFileLocation, @Nullable IOnProgress onProgress) {

        IOnProgress progressCallback = view == null ? onProgress : initActionProgress(view, onProgress, context.getString(R.string.update_tc), context.getString(R.string.update_tuner_firmware_tc), context.getString(R.string.update_complete_tc), false);

        tuner.prepareUpdateFirmware(upgradeFileLocation, (progress, done, error) -> {
            if (done && error == null) {
                new Thread(() -> {
                    DataMatrix.sendEvent(HALKeys.EVENT_TV_FIRMWARE_UPDATING);
                    tuner.resetSerialPort(true);
                    tuner.updateFirmware(upgradeFileLocation, (p, d, e) -> {
                        if(progressCallback != null) {
                            progressCallback.progress(p, d, e);
                        }
                    });
                }).start();
            } else if(done){
                if(progressCallback != null) {
                    progressCallback.progress(100, true, error);
                }
                if(onProgress != null && onProgress != progressCallback) {
                    onProgress.progress(100, true, error);
                }
            }
        });
    }


    /**
     * Execute an operation based on the list above.  Swift tuners support importing channel lists,
     * exporting channel lists, and upgrading firmware.
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
        switch(operation) {

            case IMPORT_CHANNEL_LIST_FROM_USB:
                if(workingDir != null) {
                    importChannelListUSB(view, workingDir, onProgress);
                }
                else {
                    throw new UnsupportedOperationException("Must supply a location to import the channel list from");
                }
                break;
            case EXPORT_CHANNEL_LIST_TO_USB:
                if(workingDir != null) {
                    exportChannelListToUSB(view, workingDir, onProgress);
                }
                else {
                    throw new UnsupportedOperationException("Must supply a location to export the channel list to");
                }
                break;
            case UPDATE_TUNER_FIRMWARE:
                upgradeTuner(view, StorageUtils.getUSBPath(), onProgress);
                break;
            case EXPORT_CHANNEL_LIST_TO_TUNER:
                exportChannelListToTuner(view, onProgress);
                break;
            case IMPORT_CHANNEL_LIST_FROM_TUNER:
                importChannelListFromTuner(view, onProgress);
                break;
            default:
                super.executeOperation(view, operation, workingDir, onProgress, null);
                break;
        }

    }

    /**
     * Import the channel list from the tuner to the console
     *
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param onProgress    Callback to send progress notifications.
     */
    private void importChannelListFromTuner(@Nullable View view, @Nullable IOnProgress onProgress) {
        DataMatrix.sendEvent(HALKeys.EVENT_TV_OPERATION_START);
        tuner.downloadChannelsFromTuner(initActionProgress(view, onProgress, context.getString(R.string.import_channels_tc), context.getString(R.string.import_channels_from_tuner_tc), context.getString(R.string.import_channels_success_tc), false));
    }

    /**
     * Export the channel list from the console to the tuner.
     *
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param onProgress    Callback to send progress notifications.
     */
    private void exportChannelListToTuner(@Nullable View view, @Nullable IOnProgress onProgress) {
        DataMatrix.sendEvent(HALKeys.EVENT_TV_OPERATION_START);
        tuner.uploadChannelsToTuner(initActionProgress(view, onProgress, context.getString(R.string.export_channels_tc), context.getString(R.string.export_channels_to_tuner), context.getString(R.string.export_channels_success_tc), false));
    }



    /**
     * Import the channel list from the USB to console and then to tuner.
     *
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param src           The location to find the channel list.
     * @param onProgress    Callback to send progress notifications.
     */
    private void importChannelListUSB(@Nullable View view, @NonNull File src, @Nullable IOnProgress onProgress) {
        onProgress = initActionProgress(view, onProgress, context.getString(R.string.import_channels_tc), context.getString(R.string.import_channel_list_from_usb_tc), context.getString(R.string.import_channels_success_tc), false);
        if(tuner.importChannelList(src)) {
            onProgress.progress(0, false, context.getString(R.string.export_channels_to_tuner));
            tuner.uploadChannelsToTuner(onProgress);
        }
        else {
            onProgress.progress(100, true, context.getString(R.string.import_channels_failed_pg));
        }
    }

    /**
     * Export the channel list from console to USB.  If the console doesn't have one, try to get it from
     * the USB.
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param dest          The destination to send the channel list.
     * @param onProgress    Callback to send progress notifications.
     */
    private void exportChannelListToUSB(@Nullable View view, @NonNull File dest, @Nullable IOnProgress onProgress) {
        IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.export_channels_tc), context.getString(R.string.export_channel_list_to_usb_tc), context.getString(R.string.export_channels_success_tc), false);

        if(!tuner.exportChannelList(dest)) {
            if(tuner.getChannelList(true).size() == 0) {
                if (operationProgress != null) {
                    operationProgress.progress(0, false, context.getString(R.string.import_channels_from_tuner_tc));
                }
                tuner.downloadChannelsFromTuner((progress, done, error) -> {
                    if (!done && onProgress != null) {
                        if (operationProgress != null) {
                            operationProgress.progress(progress, done, error);
                        }
                    } else if (done) {
                        if (operationProgress != null) {
                            operationProgress.progress(0, false, context.getString(R.string.export_channel_list_to_usb_tc));
                        }
                        if (tuner.exportChannelList(StorageUtils.getUSBPath())) {
                            if (operationProgress != null) {
                                ExecuteAsRoot.executeCommand("sync");
                                operationProgress.progress(100, true, context.getString(R.string.export_channels_success_tc));
                            }
                        } else {
                            if (operationProgress != null) {
                                operationProgress.progress(100, true, context.getString(R.string.export_channels_failed_pg));
                            }
                        }
                    }
                });
            }
            else {
                if (operationProgress != null) {
                    operationProgress.progress(100, true, context.getString(R.string.export_channels_failed_pg));
                }
            }
        }
        else {
            ExecuteAsRoot.executeCommand("sync");
            operationProgress.progress(100, true, context.getString(R.string.export_channels_success_tc));
        }
    }

    /**
     * Reset the tuner changing the channel to the default channel.
     */
    @Override
    public void reset() {
        setChannelToDefault();
    }

    @Override
    public void setChannelToDefault() {
        int channelIndex = getDefaultChannelInt();
        if(channelIndex >= 0 && channelIndex < tuner().getChannelList(false).size()) {
            tuner().setChannel(tuner().getChannelList(false).get(channelIndex));
            DataMatrix.putInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT, channelIndex);
            DataMatrix.putInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT, -1);
        }
        else {
            DataMatrix.putInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT, -1);
            DataMatrix.putInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT, -1);
        }
    }

    /**
     * Execute the channel up operation.  Use this so we can keep track of the previous and current channels.
     */
    public void channelUp() {
        if(tuner.getChannelList(false).size() == 0) {
            tuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_UP);
            return;
        }
        else if(tuner.getChannelList(false).size() < 2) {
            return;
        }
        int index = getCurrentChannelIndex() + 1;
        if(index >= tuner.getChannelList(false).size()) {
            index = 0;
        }
        setChannel(tuner.getChannelList(false).get(index));

    }

    /**
     * Execute the channel down operation.  Use this so we can keep track of the previous and current channels.
     */
    public void channelDown() {
        if(tuner.getChannelList(false).size() == 0) {
            tuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_DOWN);
            return;
        }
        else if(tuner.getChannelList(false).size() < 2) {
            return;
        }
        int index = getCurrentChannelIndex() - 1;
        if(index == -1) {
            index = tuner.getChannelList(false).size() - 1;
        }
        setChannel(tuner.getChannelList(false).get(index));

    }

    /**
     * Set the current channel.  Call this instead of the tuner function so that we can keep track of the current and previous channels.
     * @param channel   The channel to set.
     */
    public void setChannel(Channel channel) {
        Log.d(TAG, "set channel " + channel.toString());
        int index = tuner.getChannelList(false).indexOf(channel);
        if(index >= 0) {
            DataMatrix.putInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT, getCurrentChannelIndex());
            DataMatrix.putInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT, index);
            tuner.setChannel(channel);
        }
    }

    /**
     * For tuners with a channel list this returns the index of the current channel in the list.
     * @return  The current channel
     */
    public int getCurrentChannelIndex() {
        return DataMatrix.getInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT);
    }
    /**
     * Get the previous channel.  We only need this internally.
     *
     * @return  The previous channel
     */
    private int getLastChannelIndex() {
        return DataMatrix.getInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT);
    }

    /**
     * Execute the previous channel operation.  Do this instead of calling directly into the tuner
     * so we can keep the previous and current channels up to date.
     */
    public void previousChannel() {
        if(tuner.getChannelList(false).size() == 0) {
            tuner.clickRemoteButton(ITuner.REMOTE_BUTTON.LAST_CHANNEL);
            return;
        }
        int lastChannel = getLastChannelIndex();
        int currentChannel = getCurrentChannelIndex();

        if(lastChannel >= 0 && lastChannel < tuner().getChannelList(false).size() && lastChannel != currentChannel) {
            setChannel(tuner.getChannelList(false).get(lastChannel));
        }
    }

    /**
     *
     * @return How the default channel is handled.  Note for CAB this is special because it may be
     * either depending on whether there is a channel list.
     */
    @Override
    public DEFAULT_CHANNEL_TYPE getDefaultChannelType() {
        return DEFAULT_CHANNEL_TYPE.CHANNEL_INDEX;
    }


    @Override
    public boolean ensureTunerMatchesDetectedHardware(SettingKeys.TVSource source, boolean force) {
        JHTDataNotification<TunerProperties> tunerPropertiesNotification = new JHTDataNotification<>();
        tuner().loadProperties(tunerPropertiesNotification::setData);
        TunerProperties tunerProperties = tunerPropertiesNotification.waitForData(5100);
        return updateTVWithDetectedSource(source, force, tunerProperties);
    }

    private boolean updateTVWithDetectedSource(SettingKeys.TVSource source, boolean force, TunerProperties tunerProperties) {
        boolean sourceDetected = false;
        DataMatrix.putInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT, 0);
        if (tunerProperties != null && tunerProperties.tunerType() != null && !tunerProperties.tunerType().equals(TunerProperties.TUNER_TYPE.UNKNOWN)) {
            SettingKeys.TVSource detectedSource = SettingKeys.TVSource.getSource(tunerProperties.tunerType());
            sourceDetected = force || !detectedSource.equals(SettingKeys.TVSource.KM4);
            if(sourceDetected) {
                Log.d(TAG, "Detected tuner " + detectedSource + " was set to " + source);
                updateTunerProperties(tunerProperties);
                if (source.followsSwiftProtocol() && !source.equals(detectedSource)) {
                    // Update to the detected source.
                    Log.d(TAG, "Updating tuner to tuner " + detectedSource + " was set to " + source);
                    DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, detectedSource.toString());
                }
            }
        }
        return sourceDetected;
    }

    private void updateTunerProperties(TunerProperties tunerProperties) {
        Log.d(TAG, "Tuner type detected " + tunerProperties.tunerType());
        DataMatrix.putInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT, tunerProperties.tunerType().ordinal());
        if(tunerProperties.version() != null) {
            DataMatrix.putString(TVKeys.DATA_TUNER_VERSION_STRING, tunerProperties.version());
        }
        TransactionManager transactionManager = tuner().startTransaction();
        tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.MUTE_OFF);
        resetTunerVolume(ProductConfig.getTunerDefaultVolume(SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))));
        transactionManager.waitForTransactionComplete(10000);
        reset();
    }

    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {
        tuner.setTunerOutput(resolution);
    }
}
