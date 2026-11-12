package com.jht.chimera.lib.tv;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.tv.hdmi.HDMIResolution;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.chimera.lib.widget.tv.TVAppKeypad;
import com.jht.chimera.lib.widget.tv.TVColorButtons;
import com.jht.chimera.lib.widget.tv.TVControls;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.chimera.lib.widget.tv.TVKeypad;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import java.util.LinkedList;
import java.util.List;

public abstract class TVControllerBase implements TVKeypad.ITVKeypad, TVAppKeypad.ITVKeypad, TVExtendedControls.ITVExtendedControls, TVControls.ITVControls, TVColorButtons.ITVColorControls {

    private ChannelInfo currentChannel = null;
    private final Object currentChannelSync = new Object();
    private JHTNotification notification = new JHTNotification();
    private volatile boolean stopThread = false;


    public TVControllerBase() {
        new Thread(() -> {
            ChannelInfo channelInfo;
            ChannelInfo previousChannel = null;
            while(!stopThread) {
                notification.waitForNotification();
                if(stopThread) {
                    return;
                }
                synchronized (currentChannelSync) {
                    channelInfo = currentChannel == null ? null : new ChannelInfo(currentChannel);
                }
                if(channelInfo != null)
                    setChannelImpl(channelInfo);
            }
        }).start();
    }

    protected void setChannelImpl(ChannelInfo channel) {
        if(onChannelChanged != null) {
            onChannelChanged.onChannelChanged(channel);
        }
    }

    public abstract void exit();

    public abstract void syncChannels();

    public abstract void muteOff();

    public abstract void setSknetChannel(SKNETChannelInfo sknetChannelInfo);

    public abstract SetTopBoxSetting getCurrentSTB();

    public abstract void executeIscProcess();

    public interface IOnChannelChanged {
        void onChannelChanged(ChannelInfo channelInfo);
    }

    public abstract int getIndexByChannelInfo(ChannelInfo currentChannel);

    public interface IOnProgress {
        void onProgress(int progressPercentage);
        void onComplete(boolean success, ATSC_STATUS status);
    }

    public interface IOnSignalStrength {
        void onSignalStrength(int strength);
    }

    /**
     * Get the capabilities of the tuner for display purposes.
     * @return  The tuner capabilities.
     */
    public abstract ITunerCapabilities capabilities();

    /**
     * Get the channel list.
     * @return
     */
    public abstract List<ChannelInfo> channels();

    /**
     * Set the current channel.
     * @param channelInfo  The current channel
     */
    public void setChannel(ChannelInfo channelInfo) {
        synchronized (currentChannelSync) {
            if(channelInfo == null) {
                this.currentChannel = null;
            }
            else {
                this.currentChannel = new ChannelInfo(channelInfo);
            }
        }
        notification.setNotification();
    }

    /**
     * Get the set top box list.
     * @return
     */
    public List<String> getSetTopBoxList() {return new LinkedList<>();}


    /**
     * Get the selected set top box.
     * @return
     */
    public String getSelectedSetTopBox() {return  "";}

    /**
     * Set the current channel to the default channel.
     */
    public abstract void reset();

    /**
     * This will close any serial port communication.
     */
    public abstract void close();


    public abstract void setDefaultChannel();

    public void setTVVolume(int volume) {}

    /**
     *
     * @return  The current channel being displayed.
     */
    public abstract ChannelInfo currentChannel();

    public enum IMPORT_CHANNEL_MODE {
        USB_TO_FLASH,
        FLASH_TO_TUNER,
        USB_TO_FLASH_TO_TUNER
    }
    /**
     * Import the channels from the USB and store it on the flash and tuner.
     */
    public abstract boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode);

    /**
     * Stop importing the channels from the USB and store it on the USB and tuner.
     */
    public void stopImport() {}


    /**
     * Export the channels from the tuner to the console and then send it to USB.
     */
    public boolean exportChannels(IOnProgress onProgress) {
        return false;
    }

    /**
     * Export the channels from the console to USB.  Do not grab it from the tuner.
     */
    public boolean exportChannelsUSB() {
        return false;
    }

    /**
     * Stop exporting the channels to the USB and store it on the USB and tuner.
     */
    public void stopExport() {}

    public String defaultChannelString() { return ""; }
    public int defaultChannelInt() { return 0; }
    public void defaultChannel(String channel) {}
    public void defaultChannel(int channel) {}

    /**
     * Set the callback to be called when the signal strength changes.  By default do nothing for
     * the tuners that don't have signal strength;
     *
     * @param onSignalStrength  The callback for the signal strength.
     */
    public void setOnSignalStrength(IOnSignalStrength onSignalStrength) {

    }

    public ChannelInfo addChannel() {
        return null;
    }

    public void deleteChannel(ChannelInfo channel) {

    }

    public void deleteChannel(SKNETChannelInfo channel) {

    }

    public abstract void open();

    public void saveChannels(boolean toUsb) {

    }

    public void reboot() {

    }

    public void loadChannels() {}

    private IOnChannelChanged onChannelChanged;
    public void setOnChannelChanged(IOnChannelChanged onChannelChanged) {
        this.onChannelChanged = onChannelChanged;
    }

    /**
     *
     * @param onProgress Callback to handle progress updates.
     * @return  True if we can provide progress updates.
     */
    public boolean updateFirmware(IOnProgress onProgress) {
        return false;
    }

    protected void volumeUp() {
        SystemVolume.getInstance().increaseVolume();
    }

    protected void volumeDown() {
        SystemVolume.getInstance().decreaseVolume();
    }

    public void destroy() {
        stopThread = true;
        notification.setNotification();
    }

    public ChannelInfo.ChannelType channelType() {
        return ChannelInfo.ChannelType.REGULAR_TV;
    }

    public void initialize() {}
    public void scan() {}
    public void enableLegacy() {}
    public void cancelOperation() {}
    public boolean sknetGetChannels(IOnProgress onProgress) {
        return false;
    }
    public List<SKNETChannelInfo> sknetChannels() {
        return null;
    }
    public void executeSTBCommand(SetTopBoxSetting.RemoteButton remoteButton) {}

    public List<HDMIResolution> supportedResolutions() { return null; }
    public int splashResource() { return 0; }
}

