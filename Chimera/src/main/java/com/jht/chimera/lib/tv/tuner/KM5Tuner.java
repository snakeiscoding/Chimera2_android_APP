package com.jht.chimera.lib.tv.tuner;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_TUNER;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.IMPORT_CHANNEL_LIST_FROM_USB;
import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.UPDATE_TUNER_FIRMWARE;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.mock.MockSerialPort;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.SerialPort;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;

import java.io.File;

public class KM5Tuner extends SwiftTunerBase<SwiftChannel> {


    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    public KM5Tuner(@NonNull Context context) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.KM5Tuner(context, ProductConfig.portMappings.swiftPath(), null, new File(context.getFilesDir(), "settings/km5")));
    }

    public KM5Tuner(@NonNull Context context, @NonNull SerialPort serialPort) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.KM5Tuner(context, serialPort, null, new File(context.getFilesDir(), "settings/km5")));
    }
    /**
     * Default constructor.
     * @param context   Context used to create the tuner.  We use this to get the settings location.
     */
    @SuppressLint("VisibleForTests")
    public KM5Tuner(Context context, MockSerialPort serialPort) {
        super(context, new com.jht.serialport.peripheral.tuner.swift.KM5Tuner(context, new File(context.getFilesDir(), "settings/km5")));
        serialPort.open();
        tuner().restart(serialPort.getInputStream(), serialPort.getOutputStream(), null);
    }
    /**
     *
     * @param channel   Set the default channel index.
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public void setDefaultChannel(int channel) {
        DataMatrix.putInt(SettingKeys.TV.DATA_TV_SWIFT_DEFAULT_CHANNEL_INT, channel);
    }

    /**
     *
     * @return Get the default channel index.  This should be the index in the channel list
     */
    // TODO: CGC-22432 Remove once UUID migration is complete
    @Override
    public int getDefaultChannelInt() { return DataMatrix.getInt(SettingKeys.TV.DATA_TV_SWIFT_DEFAULT_CHANNEL_INT); }

    @Override
    public void setDefaultChannel(@NonNull String channel) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SWIFT_DEFAULT_CHANNEL_UUID_STRING, channel);
    }

    @Override
    public String getDefaultChannel() {
        return DataMatrix.getString(SettingKeys.TV.DATA_TV_SWIFT_DEFAULT_CHANNEL_UUID_STRING);
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
                IMPORT_CHANNEL_LIST_FROM_TUNER,
                EXPORT_CHANNEL_LIST_TO_TUNER,
                UPDATE_TUNER_FIRMWARE
        };
    }

    @Override
    public void getVersion() {}
    @Override
    public void setLocale(String androidLocaleTag) {}

}