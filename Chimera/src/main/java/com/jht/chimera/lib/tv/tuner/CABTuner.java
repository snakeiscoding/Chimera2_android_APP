package com.jht.chimera.lib.tv.tuner;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.io.iocommlib.IoPassthroughPort;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughDevice;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.cab.CabCommand;
import com.jht.serialport.peripheral.tuner.channel.NoChannel;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import kotlin.jvm.functions.Function0;

public class CABTuner extends TunerBase<NoChannel> {
    private final static String TAG = "CABTuner";

    // The tuner.
    private final com.jht.serialport.peripheral.tuner.cab.CABTuner cabTuner;

    // Mock serial port for testing.
    private ISerialPort serialPort = null;

    /** Creates tuner while taking into consideration if it has a dedicated serial port or uses IO passthrough */
    public static CABTuner create(Context context, Map<ITuner.REMOTE_BUTTON, Function0<CabCommand>> remoteButtonMap) {
        // constructor overrides make this difficult, so using static method to perform conditional construction
        if (ProductConfig.portMappings.cabPath() == null) {
            ISerialPort port = new IoPassthroughPort(McuPassthroughDevice.CAB);
            return new CABTuner(context, port, remoteButtonMap);
        } else {
            return new CABTuner(context, remoteButtonMap);
        }
    }

    /** Constructor using product port map */
    private CABTuner(Context context, Map<ITuner.REMOTE_BUTTON, Function0<CabCommand>> remoteButtonMap) {
        super(new com.jht.serialport.peripheral.tuner.cab.CABTuner(context, ProductConfig.portMappings.cabPath(), new File(context.getFilesDir(), "settings/us7"), remoteButtonMap));
        cabTuner = (com.jht.serialport.peripheral.tuner.cab.CABTuner)tuner();
    }

    /** Constructor using a passed serial port */
    public CABTuner(Context context, ISerialPort serialPort, Map<ITuner.REMOTE_BUTTON, Function0<CabCommand>> remoteButtonMap) {
        super(new com.jht.serialport.peripheral.tuner.cab.CABTuner(context, serialPort.getOutputStream(), new File(context.getFilesDir(), "settings/cab"), remoteButtonMap));
        try {
            serialPort.open(); // ISerialPort open throws IOException, however this constructor is used by fake serial ports
        } catch (IOException e) {
            Log.e(TAG, "CABTuner failed to open port", e);
        }
        this.serialPort = serialPort;
        cabTuner = (com.jht.serialport.peripheral.tuner.cab.CABTuner)tuner();
    }

    /**
     *
     * @return  Get the default channel as a string.  This assumes no channel list.
     * @throws UnsupportedOperationException  Only used on tuners that do not have channel lists.
     */
    @Override
    public String getDefaultChannel() {
        return DataMatrix.getString(SettingKeys.TV.DATA_TV_CAB_DEFAULT_CHANNEL_STRING);
    }

    /**
     *
     * @param channel   Set the default channel.
     * @throws UnsupportedOperationException  Only used on tuners that have a channel list.
     */
    @Override
    public void setDefaultChannel(@NonNull String channel) throws UnsupportedOperationException {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_CAB_DEFAULT_CHANNEL_STRING, channel);
    }

    @Override
    public void upgradeTuner(@Nullable View view, @NonNull File upgradeFile, @Nullable IOnProgress onProgress) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return      A list of operations supported by the tuner.
     */
    @Override
    public TunerOperation[] getSupportedOperations(int mode) {
        return null;
    }


    /**
     * Shutdown all threads related to the tuner and cleanup.
     */
    @Override
    public void destroy() {
        super.destroy();
        if(serialPort != null) {
            try {
                serialPort.close();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void reset() {
        setChannelToDefault();
    }

    @Override
    public void setChannelToDefault() {
        cabTuner.setChannel(getDefaultChannel());
    }

    @Override
    public void channelUp() {
        cabTuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_UP);
    }

    @Override
    public void channelDown() {
        cabTuner.clickRemoteButton(ITuner.REMOTE_BUTTON.CHANNEL_DOWN);
    }

    @Override
    public void setChannel(NoChannel channel) {
        cabTuner.setChannel(channel);
    }

    @Override
    public void previousChannel() {
        cabTuner.clickRemoteButton(ITuner.REMOTE_BUTTON.LAST_CHANNEL);
    }


    @Override
    public int getChannelSetupModes()  {
        return 2;
    }

    @Override
    public String channelSetupModeString(Context context, int mode)  {
        return context.getString(mode == 0 ? R.string.tuner_type_tc : R.string.cab_setup_tc);
    }

    @Override
    public int getDefaultMode()  {
        return 0;
    }

    @Override
    public SETTINGS_MODE getModeType(int mode)  {
        return mode == 0 ? SETTINGS_MODE.NO_LIST : SETTINGS_MODE.CHANNEL_LIST;
    }

    @Override
    public boolean ensureTunerMatchesDetectedHardware(SettingKeys.TVSource source, boolean force) {
        return true;
    }


    /**
     *
     * @return How the default channel is handled.  Note for CAB this is special because it may be
     * either depending on whether there is a channel list.
     */
    @Override
    public DEFAULT_CHANNEL_TYPE getDefaultChannelType() {
        return DEFAULT_CHANNEL_TYPE.CHANNEL_STRING;
    }

    @Override
    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {}
    @Override
    public void getVersion() {}
    @Override
    public void setLocale(String androidLocaleTag) {}
}
