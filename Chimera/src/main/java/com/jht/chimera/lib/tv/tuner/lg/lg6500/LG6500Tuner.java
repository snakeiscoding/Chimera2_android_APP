package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.DOWNLOAD_CHANNEL_XML;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPDATE_TUNER_FIRMWARE;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPLOAD_CHANNEL_XML;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.tuner.IChimeraIPTuner;
import com.jht.chimera.lib.tv.tuner.TunerBase;
import com.jht.chimera.lib.tv.tuner.US4Tuner;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Note: LG6500 channel list uses the same channel class for Enseo
// The communication protocol is defined in https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/3468886033/STB-6500+Communication
public class LG6500Tuner extends TunerBase<EnseoChannel> implements IChimeraIPTuner<EnseoChannel> {
    public static final String TAG = "LG6500Tuner";
    private final LG6500TunerBase tuner;
    private final Object defaultChannelKey;
    private final Object defaultChannelUuidKey;
    private final Object currentChannelKey;
    private final Context context;
    private ISerialPort serialPort;

    /**
     * Default constructor.
     *
     * @param context Context used to create the tuner.  We use this to get the settings location.
     */
    public LG6500Tuner(@NonNull Context context, @NonNull LG6500Variant lg6500Variant) {
        super(new LG6500TunerBase(
                context,
                new File(context.getFilesDir(), lg6500Variant.getSettingsDirectory())
        ));

        try {
            serialPort = LG6500SerialPort.getInstance();
            LG6500SerialPort.getInstance().open();
        } catch (IOException e) {
            Log.w(TAG, "Failed opening serial port", e);
        }

        tuner = (LG6500TunerBase) tuner();
        tuner.restart(serialPort.getInputStream(), serialPort.getOutputStream(), null);
        this.context = context;
        this.defaultChannelKey = lg6500Variant.getDefaultChannelKey();
        this.defaultChannelUuidKey = lg6500Variant.getDefaultChannelUuidKey();
        this.currentChannelKey = lg6500Variant.getCurrentChannelKey();
    }

    /**
     * Default constructor.
     *
     * @param context Context used to create the tuner.  We use this to get the settings location.
     */
    @SuppressLint("VisibleForTests")
    public LG6500Tuner(Context context, ISerialPort serialPort, LG6500Variant lg6500Variant) {
        super(new LG6500TunerBase(
                context,
                new File(context.getFilesDir(), lg6500Variant.getSettingsDirectory())
        ));

        try {
            this.serialPort = serialPort;
            serialPort.open();
        } catch (IOException e) {
            Log.e(TAG, "Failed opening serial port: ", e);
        }

        tuner = (LG6500TunerBase) tuner();
        tuner.restart(serialPort.getInputStream(), serialPort.getOutputStream(), null);
        this.context = context;
        this.defaultChannelKey = lg6500Variant.getDefaultChannelKey();
        this.defaultChannelUuidKey = lg6500Variant.getDefaultChannelUuidKey();
        this.currentChannelKey = lg6500Variant.getCurrentChannelKey();
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
        tuner.setClosedCaptionIndex(enable ? LG6500TunerBase.DEFAULT_CC_ON_INDEX : 0);
    }

    private int findChannelIndex(EnseoChannel channel) {
        List<EnseoChannel> channelList = tuner().getChannelList(false);
        for (int index = 0; index < channelList.size(); index++) {
            if (channel.toJSON().toString().equals(channelList.get(index).toJSON().toString())) {
                return index;
            }
        }
        return 0;
    }

    private void channelDelta(int delta) {
        List<EnseoChannel> channelList = tuner().getChannelList(false);
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
    public void setChannel(EnseoChannel channel) {
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
     * Shutdown all threads related to the tuner and cleanup.
     */
    @Override
    public void destroy() {
        super.destroy();
        try {
            serialPort.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed closing serial port: ", e);
        }
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
            default:
                throw new UnsupportedOperationException("Tuner does not implement " + operation);
        }
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
    public void resetSerialPort(boolean update) {
        tuner().destroy();
        LG6500SerialPort.getInstance().reset();
        tuner().restart(LG6500SerialPort.getInstance().getInputStream(), LG6500SerialPort.getInstance().getOutputStream(), null);
    }

    @Override
    public void reboot() {
        tuner.reboot();
    }

    @Override
    public void getVersion() {
        tuner.getVersion();
    }

    @Override
    public void setLocale(String androidLocaleTag) {
        tuner.setLocale(androidLocaleTag);
    }

    @Override
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
