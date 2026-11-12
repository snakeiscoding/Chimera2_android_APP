package com.jht.chimera.lib.tv.tuner;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_IP_LIST_TO_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_IP_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPDATE_TUNER_FIRMWARE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.SerialPort;
import com.jht.serialport.peripheral.tuner.swift.SwiftProIdiomChannel;

import org.junit.Assert;

import java.io.File;

public class US7Tuner extends SwiftTunerBase<SwiftProIdiomChannel> {

    // The tuner.
    private final com.jht.serialport.peripheral.tuner.swift.US7Tuner us7Tuner;

    // Context needed for strings.
    private final Context context;

    // Mock serial port for testing.
    private MockSerialPort serialPort = null;

    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    public US7Tuner(Context context) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.US7Tuner(context, ProductConfig.portMappings.swiftPath(), null, new File(context.getFilesDir(), "settings/us7")));
        us7Tuner = (com.jht.serialport.peripheral.tuner.swift.US7Tuner)tuner();
        this.context = context;
    }

    public US7Tuner(Context context, SerialPort serialPort) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.US7Tuner(context, serialPort, null, new File(context.getFilesDir(), "settings/us7")));
        us7Tuner = (com.jht.serialport.peripheral.tuner.swift.US7Tuner)tuner();
        this.context = context;
    }

    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    @SuppressLint("VisibleForTests")
    public US7Tuner(Context context, MockSerialPort serialPort) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.US7Tuner(context, new File(context.getFilesDir(), "settings/us7")));
        serialPort.open();
        this.serialPort = serialPort;
        us7Tuner = (com.jht.serialport.peripheral.tuner.swift.US7Tuner)tuner();
        us7Tuner.restart(serialPort.getInputStream(), serialPort.getOutputStream(), null);
        this.context = context;
    }


    /**
     *
     * @param channel   Set the default channel index.
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public void setDefaultChannel(int channel) {
        DataMatrix.putInt(SettingKeys.TV.DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_INT, channel);
    }

    /**
     *
     * @return Get the default channel index.  This should be the index in the channel list
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public int getDefaultChannelInt() { return DataMatrix.getInt(SettingKeys.TV.DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_INT); }

    @Override
    public void setDefaultChannel(@NonNull String channel) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_UUID_STRING, channel);
    }

    @Override
    public String getDefaultChannel() {
        return DataMatrix.getString(SettingKeys.TV.DATA_TV_SWIFT_US7_DEFAULT_CHANNEL_UUID_STRING);
    }

    /**
     * The US7 tuner has additional operations for managing the IP scan list.
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
            case IMPORT_IP_LIST_FROM_USB:
                Assert.assertNotNull("Coding Error: Must supply a location to import the IP scan list from USB", workingDir);
                importIPListUSB(view, workingDir, onProgress);
                break;
            case EXPORT_IP_LIST_TO_TUNER:
                exportIPListToTuner(view, onProgress);
                break;
            default:
                super.executeOperation(view, operation, workingDir, onProgress, null);
                break;

        }

    }

    /**
     *
     * @return      A list of operations supported by the tuner.
     */
    @Override
    public TunerOperation[] getSupportedOperations(int mode) {
        if (mode == 0) {
            return new TunerOperation[] {
                    IMPORT_IP_LIST_FROM_USB,
                    EXPORT_IP_LIST_TO_TUNER,
                    UPDATE_TUNER_FIRMWARE
            };
        }
        return new TunerOperation[] {
                IMPORT_CHANNEL_LIST_FROM_USB,
                EXPORT_CHANNEL_LIST_TO_USB,
                IMPORT_CHANNEL_LIST_FROM_TUNER,
                EXPORT_CHANNEL_LIST_TO_TUNER,
                UPDATE_TUNER_FIRMWARE
        };
    }

    /**
     * Import the IP scan list from USB (or elsewhere).
     *
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param src           The location to find the IP scan list.
     * @param onProgress    Callback to send progress notifications.
     */
    private void importIPListUSB(@Nullable View view, @NonNull File src, @Nullable IOnProgress onProgress) {

        if(us7Tuner.importIPList(src)) {
            IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.import_from_usb_tc), context.getString(R.string.import_ip_list_from_usb_tc), context.getString(R.string.import_channels_success_tc), false);
            if(operationProgress != onProgress) {
                operationProgress.progress(0, false, context.getString(R.string.export_ip_list_to_tuner_tc));
                us7Tuner.loadSettings();
                us7Tuner.uploadIPList((progress, done, error) -> {
                    if(done && error == null) {
                        // Move on to importing the channel list from the tuner.
                        operationProgress.progress(0, false, context.getString(R.string.import_channels_from_tuner_tc));
                        us7Tuner.downloadChannelsFromTuner(operationProgress);
                    }
                    else {
                        operationProgress.progress(progress, done, error);
                    }
                }, true);
            }
            else {
                us7Tuner.uploadIPList(onProgress, true);
            }
        }
        else {
            if(view != null) {
                DialogFactory.message(view, context.getString(R.string.ip_list_not_found_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
            if(onProgress != null) {
                onProgress.progress(100, true, null);
            }
        }
    }

    /**
     * Import the IP scan list from USB (or elsewhere).
     *
     * @param view          The view that started the operation.  If null don't show dialog box.
     * @param onProgress    Callback to send progress notifications.
     */
    private void exportIPListToTuner(@Nullable View view, @Nullable IOnProgress onProgress) {
        IOnProgress operationProgress = initActionProgress(view, onProgress, context.getString(R.string.export_channels_tc), context.getString(R.string.export_ip_list_to_tuner_tc), context.getString(R.string.export_channels_success_tc), false);
        if(operationProgress != onProgress) {
            us7Tuner.loadSettings();
            us7Tuner.uploadIPList((progress, done, error) -> {
                operationProgress.progress(progress, false, error);
                if(done) {
                    // Move on to importing the channel list from the tuner.
                    operationProgress.progress(0, false, context.getString(R.string.import_channels_from_tuner_tc));
                    us7Tuner.downloadChannelsFromTuner(operationProgress);
                }

            }, true);
        }
        else {
            us7Tuner.uploadIPList(onProgress, true);
        }

    }

    /**
     * Shutdown all threads related to the tuner and cleanup.
     */
    @Override
    public void destroy() {
        super.destroy();
        if(serialPort != null) {
            serialPort.close();
        }
    }

    @Override
    public int getChannelSetupModes()  {
        return 2;
    }

    @Override
    public String channelSetupModeString(Context context, int mode)  {
        return context.getString(mode == 0 ? R.string.ip_mode_tc : R.string.channel_mode_tc);
    }

    @Override
    public int getDefaultMode()  {
        return tuner().getChannelList(true).isEmpty() ? 0 : 1;
    }

    @Override
    public SETTINGS_MODE getModeType(int mode)  {
        return mode == 0 ? SETTINGS_MODE.IP_SCAN_LIST : SETTINGS_MODE.CHANNEL_LIST;
    }
    @Override
    public void getVersion() {}
    @Override
    public void setLocale(String androidLocaleTag) {}
}
