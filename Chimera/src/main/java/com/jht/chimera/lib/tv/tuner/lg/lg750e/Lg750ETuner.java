package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.DOWNLOAD_CHANNEL_XML;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPDATE_TUNER_FIRMWARE;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPLOAD_CHANNEL_XML;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.tuner.IChimeraIPTuner;
import com.jht.chimera.lib.tv.tuner.TunerBase;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// The communication protocol is defined in https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/3468886033/STB-6500+Communication
public class Lg750ETuner extends TunerBase<LgChannel> implements IChimeraIPTuner<LgChannel> {
    public static final String TAG = "Lg750ETuner";
    private final Lg750ETunerBase tuner;
    private final Lg750EVariant variant;
    private final Object defaultChannelKey;
    private final Object defaultChannelUuidKey;
    private final Object currentChannelKey;
    private final Context context;

    /**
     * Default constructor.
     *
     * @param context Context used to create the tuner.  We use this to get the settings location.
     */
    public Lg750ETuner(@NonNull Context context, @NonNull Lg750EVariant variant) {
        super(new Lg750ETunerBase(
                context,
                new File(context.getFilesDir(), variant.getSettingsDirectory())
        ));

        tuner = (Lg750ETunerBase) tuner();
        this.context = context;
        this.variant = variant;
        this.defaultChannelKey = variant.getDefaultChannelKey();
        this.defaultChannelUuidKey = variant.getDefaultChannelUuidKey();
        this.currentChannelKey = variant.getCurrentChannelKey();
    }

    /** Returns the variant this tuner was created for. */
    public Lg750EVariant getVariant() {
        return variant;
    }

    /**
     * @param channel Set the default channel index.
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public void setDefaultChannel(int channel) {
        DataMatrix.putInt(defaultChannelKey, channel);
    }

    @Override
    public void upgradeTuner(@Nullable View view, @NonNull File upgradeFile, @Nullable IOnProgress onProgress) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @return Get the default channel index.  This should be the index in the channel list
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public int getDefaultChannelInt() {
        return DataMatrix.getInt(defaultChannelKey);
    }

    @Override
    public void setDefaultChannel(@NonNull String channel) {
        DataMatrix.putString(defaultChannelUuidKey, channel);
    }

    @Override
    public String getDefaultChannel() {
        return DataMatrix.getString(defaultChannelUuidKey);
    }

    /**
     * @return A list of operations supported by the tuner.
     */
    @Override
    public TunerOperation[] getSupportedOperations(int mode) {
        return new TunerOperation[]{
                IMPORT_CHANNEL_LIST_FROM_USB,
                EXPORT_CHANNEL_LIST_TO_USB,
                IMPORT_CHANNEL_LIST_FROM_TUNER,
                UPDATE_TUNER_FIRMWARE,
                UPLOAD_CHANNEL_XML,
                DOWNLOAD_CHANNEL_XML
        };
    }

    @Override
    public void reset() {
        setChannelToDefault();

        // send message to save default channel
        tuner.saveDefaultChannel();

        // disable closed captioning
        enableClosedCaption(false);
    }

    public void clearUserSession() {
        tuner.setSoftAp("", ""); // clear the soft AP
        tuner.clearUserData(); // do last in case tuner communications drop
    }

    @Override
    public void setChannelToDefault() {
        int channelIndex = getDefaultChannelInt();
        if (channelIndex >= 0 && channelIndex < tuner().getChannelList(false).size()) {
            tuner().setChannel(tuner().getChannelList(false).get(channelIndex));
            DataMatrix.putInt(currentChannelKey, channelIndex);
        } else {
            DataMatrix.putInt(currentChannelKey, 0);
        }
    }

    private void enableClosedCaption(boolean enable) {
        // index 0 is off; index 1 is Service1 which appears to work like CC1
        tuner.setClosedCaptionIndex(enable ? Lg750ETunerBase.DEFAULT_CC_ON_INDEX : 0);
    }

    private int findChannelIndex(LgChannel channel) {
        List<LgChannel> channelList = tuner().getChannelList(false);
        for (int index = 0; index < channelList.size(); index++) {
            if (channel.uuid().equals(channelList.get(index).uuid())) {
                return index;
            }
        }
        return 0;
    }

    private void channelDelta(int delta) {
        List<LgChannel> channelList = tuner().getChannelList(false);
        if (channelList != null && !channelList.isEmpty()) {
            int index = getCurrentChannelIndex();
            index += delta;
            if (index >= channelList.size()) index = 0;
            if (index < 0) index = channelList.size() - 1;
            setChannel(channelList.get(index));
        }
        else if (delta > 0) {
            tuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_UP);
        }
        else if (delta < 0) {
            tuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_DOWN);
        }
    }

    @Override
    public void channelUp() {
        channelDelta(1);
    }

    @Override
    public void channelDown() {
        channelDelta(-1);
    }

    @Override
    public void setChannel(LgChannel channel) {
        tuner().setChannel(channel);
        DataMatrix.putInt(currentChannelKey, findChannelIndex(channel));
    }

    /**
     * For tuners with a channel list this returns the index of the current channel in the list.
     *
     * @return The current channel
     */
    public int getCurrentChannelIndex() {
        return DataMatrix.getInt(currentChannelKey);
    }

    @Override
    public void previousChannel() {
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
     * Execute an operation.
     *
     * @param view       The button used to explode the progress dialog from.  If this is automated
     *                   it can be null and there will be no dialog box.
     * @param operation  The operation to perform.
     * @param workingDir The working directory where you will find files needed by the operation.
     * @param onProgress Callback for the operation progress.
     * @param onConfirm Callback for refreshing the page after downloading channel file
     * @throws UnsupportedOperationException Not all tuners support all operations.
     */
    @Override
    public void executeOperation(@Nullable View view, @NonNull TunerOperation operation, @Nullable File workingDir, @Nullable IOnProgress onProgress, @Nullable DialogBase.IOnConfirm onConfirm) throws UnsupportedOperationException {
        switch (operation) {
            case IMPORT_CHANNEL_LIST_FROM_USB:
                importChannelListFromUSB(view, workingDir, onProgress);
                break;
            case EXPORT_CHANNEL_LIST_TO_USB:
                boolean result = tuner.exportChannelList(workingDir);
                ExecuteAsRoot.runSyncCommand();
                if (view != null) {
                    DialogFactory.message(view, context.getString(result ? R.string.export_channels_success_tc : R.string.export_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }
                break;
            case UPDATE_TUNER_FIRMWARE:
                tuner.reboot();
                break;
            case EXPORT_CHANNEL_LIST_TO_TUNER:
                // return successful so Site Survey export works since there is nothing to export
                if (onProgress != null) {
                    onProgress.progress(100, true, null);
                }
                break;
            case IMPORT_CHANNEL_LIST_FROM_TUNER:
                importChannelListFromTuner(view, onProgress);
                break;
            default:
                throw new UnsupportedOperationException("Tuner does not implement " + operation);
        }
    }

    private void importChannelListFromTuner(@Nullable View view, @Nullable IOnProgress onProgress) {
        DataMatrix.sendEvent(HALKeys.EVENT_TV_OPERATION_START);
        tuner.downloadChannelsFromTuner(initActionProgress(view, onProgress,
                context.getString(R.string.import_channels_tc),
                context.getString(R.string.import_channels_from_tuner_tc),
                context.getString(R.string.import_channels_success_tc),
                false));
    }

    private void importChannelListFromUSB(@Nullable View view, @Nullable File src, @Nullable IOnProgress onProgress) {
        if (tuner.importChannelList(src)) {
            IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.import_from_usb_tc), context.getString(R.string.import_from_usb_tc), context.getString(R.string.import_channels_success_tc), true);
            tuner.loadSettings();
            operationProgress.progress(100, true, null);
        } else {
            if (view != null) {
                DialogFactory.message(view, context.getString(R.string.import_channels_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
            if (onProgress != null) {
                onProgress.progress(100, true, null);
            }
        }
    }

    @Override
    public void updateChannelEnabledStatus(LgChannel channel, boolean enable) {
        super.updateChannelEnabledStatus(channel, enable);
        tuner().saveSettings();
    }

    /**
     * Does nothing: there is no port to reset, because the HAL service owns the tuner connection.
     */
    @Override
    public void resetSerialPort(boolean update) {
    }

    @Override
    public void reboot() {
        tuner.reboot();
    }

    public void getVersion() {
        tuner.getVersion();
    }

    @Override
    public void setLocale(String androidLocaleTag) {
        tuner.setLocale(androidLocaleTag);
    }

    /**
     * Attempts to change the LG750E tuner's video output resolution. Note that this will cause the STB
     * to reboot (unless the specified resolution is the current resolution).
     *
     * @param resolution The output resolution the STB will be set to
     */
    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {
        tuner.setTunerOutputResolution(resolution);
    }

    public CompletableFuture<String> getIpAddress() {
        return tuner.getIpAddress();
    }

    public CompletableFuture<String> getMacAddress() {
        return tuner.getMacAddress();
    }

}
