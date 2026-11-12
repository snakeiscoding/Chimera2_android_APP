package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.TestTrace;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.channel.ChannelLoader;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel;
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannelLoader;
import com.jht.serialport.peripheral.tuner.enseo.EnseoRTPChannel;
import com.jht.serialport.peripheral.tuner.enseo.EnseoUDPChannel;
import com.jht.serialport.serialcommunication.IOnInvalidChecksum;
import com.jht.serialport.serialcommunication.StringPacket;
import com.jht.serialport.serialcommunication.TransactionManager;
import com.jht.serialport.serialcommunication.WriteManager;
import com.jht.serialport.serialcommunication.WriteProperties;
import com.jht.serialport.serialcommunication.parser.StringParser;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Note: LG6500 channel list uses the same channel class for Enseo
// The communication protocol is defined in https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/3468886033/STB-6500+Communication
public class LG6500TunerBase extends com.jht.serialport.peripheral.tuner.TunerBase<EnseoChannel> {
    // RS232 Max length of EU model is 255 (bytes) and US model is 32 (bytes)
    // Since the US model only has 32 bytes buffer, we need to sleep between sending messages so the messages are not concatenated
    public static final String TAG = "LG6500";
    public static final int SLEEP_TIME = 50;
    public static final int DEFAULT_CC_ON_INDEX = 1;

    private static final LG6500Response lg6500Response = new LG6500Response();

    public static final String LG_CHANNEL_FILE = "channels.xml";
    private static final String LG_CHANNEL_FILE_XSD = "ip_list.xsd";

    // These are the commands send for the various buttons.
    private static final String[] remoteButtons = {
            "01:48\n", // ZERO
            "01:49\n", // ONE
            "01:50\n", // TWO
            "01:51\n", // THREE
            "01:52\n", // FOUR
            "01:53\n", // FIVE
            "01:54\n", // SIX
            "01:55\n", // SEVEN
            "01:56\n", // EIGHT
            "01:57\n", // NINE
            null, // DASH
            "01:427\n", // CHANNEL_UP
            "01:428\n", // CHANNEL_DOWN
            "01:711\n", // LAST_CHANNEL
            "01:447\n", // VOLUME_UP
            "01:448\n", // VOLUME_DOWN
            null, // MUTE_ON
            null, // MUTE_OFF
            "01:449\n", // MUTE
            "01:1008\n", // TOGGLE_CC
            "01:38\n", // UP
            "01:40\n", // DOWN
            "01:37\n", // LEFT
            "01:39\n", // RIGHT
            "01:13\n", // ENTER
            "01:13\n", // SELECT
            null, // SETUP
            "01:18\n", // MENU
            "01:409\n", // POWER
            null, // WAIT
            null, // POWER_OFF
            "01:1001\n", // EXIT
            "01:457\n", // INFO
            "01:403\n", // RED
            "01:404\n", // GREEN
            "01:405\n", // YELLOW
            "01:406\n", // BLUE
            "01:723\n", // SAP
            "01:461\n", // BACK
            null, // RETURN
            "01:458\n", // GUIDE
            null, // PAGE_UP
            null, // PAGE_DOWN,
            null // SOURCE
    };

    // Write manager used to send messages to the LG box.
    private WriteManager writeManager;

    // Used to parse incoming data.
    private StringParser parser;

    // Used to manage the channel list.
    private final File settingsDir;

    // This holds the channel list for LG.
    private final List<EnseoChannel> channelList = new ArrayList<>();

    // This is the closed caption index
    private int closedCaptionIndex = 0;

    /**
     * Starts up read and write threads to communicate with LG.
     *
     * @param context      Needed to load assets.
     * @param settingsDir  Location to find tuner settings and channel lists.
     */
    public LG6500TunerBase(@Nullable Context context, @NonNull File settingsDir) {
        this.settingsDir = settingsDir;
        //noinspection ResultOfMethodCallIgnored
        settingsDir.mkdirs();
        if (context != null) {
            // Note: Using the same xsd as us4 tuner. Do we want to create a LG6500 ip_list.xsd in artifact?
            FileUtils.copyAssetToFlash(context, "tuner/us4/ip_list.xsd", new File(settingsDir, LG_CHANNEL_FILE_XSD));

        }
        loadSettings();
        // Do not read streams before port is open! call this after port is open
        //restart(inputStream, outputStream, null);
    }

    @Override
    protected void setChannelImpl(EnseoChannel channel) {
        // don't call the super.setChannelImpl(channel); but just override function use protocol definition
        if (channel instanceof EnseoRTPChannel) {
            EnseoRTPChannel rtpChannel = (EnseoRTPChannel) channel;
            writeManager.sendMessage(new StringPacket(String.format("05:%s:%s\n", rtpChannel.address(), rtpChannel.port())));
        }
        else if (channel instanceof EnseoUDPChannel) {
            EnseoUDPChannel udpChannel = (EnseoUDPChannel) channel;
            writeManager.sendMessage(new StringPacket(String.format("04:%s:%s\n", udpChannel.address(), udpChannel.port())));
        }
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    /**
     * @return Returns the capabilities for the tuner.  Useful for determining what buttons to display to the user
     */
    @Override
    public ITunerCapabilities capabilities() {
        return new LG6500Capabilities();
    }

    /**
     * Used to upload channels from the system to the tuner.
     *
     * @param onProgress Callback used to provide progress updates.
     */
    @Override
    public void uploadChannelsToTuner(IOnProgress onProgress) {
    }

    /**
     * Used to download channels from the tuner if applicable.
     *
     * @param onProgress Callback used to provide progress updates.
     */
    @Override
    public void downloadChannelsFromTuner(IOnProgress onProgress) {
        throw new UnsupportedOperationException("Cannot download channels from the LG box");
    }

    /**
     * Loads the settings.  Typically this is the channel list.
     */
    @Override
    public void loadSettings() {
        channelList.clear();
        File file = new File(settingsDir, LG_CHANNEL_FILE);
        if (file.exists()) {
            new EnseoChannelLoader().loadList(new File(settingsDir, LG_CHANNEL_FILE_XSD), file, channelList);
            channelList.sort(new LG6500ChannelInfoSort());
        }
    }

    /**
     * Save the settings.  Typically this is the channel list.
     */
    @Override
    public void saveSettings() {
        channelList.sort(new LG6500ChannelInfoSort());
        File file = new File(settingsDir, LG_CHANNEL_FILE);
        new EnseoChannelLoader().saveList(file, channelList);
    }

    /**
     * @return Used to get the current channel list.
     */
    @Override
    public List<EnseoChannel> getChannelList(boolean includeAll) {
        return channelList;
    }

    /**
     * Shutdown all communication threads.
     */
    @Override
    public void destroy() {
        super.destroy();
        if (writeManager != null) writeManager.destroy();
        if (parser != null) parser.destroy();
        ThreadHelper.sleepIgnoreInterruption(1100);
    }

    /**
     * Restart the interface.
     *
     * @param inputStream       The stream to read messages from the tuner.
     * @param outputStream      The stream to write messages to the tuner.
     * @param onInvalidChecksum Callback when we have an invalid checksum.
     */
    @Override
    public void restart(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, @Nullable IOnInvalidChecksum onInvalidChecksum) {
        if (writeManager != null) writeManager.destroy();
        if (parser != null) parser.destroy();
        writeManager = new WriteManager(outputStream, new WriteProperties(0, 0, 1, TestTrace.instance().inTest() ? 0 : 200, TestTrace.instance().inTest() ? 0 : 200));
        parser = new StringParser(inputStream, 4096, (message) -> {
            //Log.d(TAG, "message: " + message);
            try {
                if (message != null && message.contains("=")) {
                    String[] tokens = message.replace("\n", "").split("=");
                    String key = tokens[0];
                    String value = tokens[1];

                    switch (key) {
                        case "version":
                            lg6500Response.version = value;
                            DataMatrix.putString(TVKeys.DATA_TUNER_VERSION_STRING, value);
                            break;

                        case "dhcp":
                            lg6500Response.dhcp = value;
                            break;

                        case "ip":
                            lg6500Response.ip = value;
                            DataMatrix.putString(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING, value);
                            break;

                        case "subnet":
                            lg6500Response.subnet = value;
                            break;

                        case "gateway":
                            lg6500Response.gateway = value;
                            break;

                        case "dns1":
                            lg6500Response.dns1 = value;
                            break;

                        case "dns2":
                            lg6500Response.dns2 = value;
                            break;
                        case "mac":
                            lg6500Response.mac = value;
                            DataMatrix.putString(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, value);
                            break;
                        case "model":
                            DataMatrix.putString(TVKeys.EVENT_TUNER_MODEL_STRING, value);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error Message: ", e);
            }
        }, "\n");


        restart();
    }

    /**
     * Reset the serial port.  We don't own a serial port in this interface so we throw an unsupported
     * operation.
     *
     * @param update True if we are about ot update the firmware.
     */
    @Override
    public void resetSerialPort(boolean update) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pull in the channel config.
     *
     * @param src The location where the settings files are.
     * @return True if we found a channel file in the directory given.
     */
    @Override
    public boolean importChannelList(File src) {
        File importFile = new File(src, LG_CHANNEL_FILE);
        String xsd = LG_CHANNEL_FILE_XSD;
        if (importFile.exists() && ChannelLoader.validateListFile(new File(settingsDir, xsd), importFile)) {
            return FileUtils.writeToFile(
                    FileUtils.readFileToString(importFile), new File(settingsDir, LG_CHANNEL_FILE));
        }
        return false;
    }

    /**
     * Move the channel list file to the directory input.
     *
     * @param dest The location where the settings files need to be exported to.
     * @return True if the export is successful.
     */
    @Override
    public boolean exportChannelList(File dest) {
        return FileUtils.copyFile(new File(settingsDir, LG_CHANNEL_FILE), new File(dest, LG_CHANNEL_FILE));
    }

    /**
     * Handles remote presses.  Generally we can act as a virtual remote for the tuner.
     *
     * @param button The button pressed
     */
    @Override
    public void clickRemoteButton(REMOTE_BUTTON button) {
        //Log.d(TAG, "clickRemoteButton: " + button);

        // hijack and handle the CC button
        if (button == REMOTE_BUTTON.TOGGLE_CC) {
            // toggle
            setClosedCaptionIndex(closedCaptionIndex == 0 ? DEFAULT_CC_ON_INDEX : 0);
            // show UI to indicate CC level
            DataMatrix.putBoolean(TVKeys.ACTION_SHOW_CLOSED_CAPTION_STATUS_BOOL, closedCaptionIndex == DEFAULT_CC_ON_INDEX);
            // skip sending the button command
            return;
        }

        clickRemoteButton(button, 1);

        // show the custom LG setting dialog box
        if (button == REMOTE_BUTTON.SETUP) {
            lg6500Response.reset();

            // send get version command
            getVersion();
            // send get ip command part 1
            getIpAddress();
            // send get ip command part 2
            writeManager.sendMessage(new StringPacket(String.format("07:02\n")));
            SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
            // send get mac address command
            getMacAddress();

            LG6500TunerSettings dialog = new LG6500TunerSettings(ContextUtil.getApplicationContext(), lg6500Response, writeManager);
            dialog.show();
        }
    }

    /**
     * Handles remote presses.  Generally we can act as a virtual remote for the tuner.
     *
     * @param button The button to press.
     * @param count  The number of times to push the button.
     */
    void clickRemoteButton(REMOTE_BUTTON button, int count) {
        if (remoteButtons[button.ordinal()] == null) return;
        for (int i = 0; i < count; i++) {
            writeManager.sendMessage(new StringPacket(remoteButtons[button.ordinal()]));
            onRemoteButton(button);
            SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
        }
    }

    /**
     * Cancel a transaction that is in progress.
     */
    @Override
    public void cancelTransaction() {
    }

    /**
     * We do not currently support this function, but might be able to in the future.
     *
     * @param channel The channel.
     */
    @Override
    public void sendChannelEnableStatus(EnseoChannel channel) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TransactionManager startTransaction() {
        return writeManager.startTransaction();
    }


    /**
     * Used to set the tuner volume.  The range can be different across tuners.
     */
    @Override
    public void setTunerVolume(double volume) {
        //Log.d(TAG, "setTunerVolume: " + volume);
        writeManager.sendMessage(new StringPacket(String.format("02:%d\n", (int) volume)));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    public void setClosedCaptionIndex(int index) {
        closedCaptionIndex = index;
        writeManager.sendMessage(new StringPacket(String.format("08:%d\n", closedCaptionIndex)));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    public void reboot() {
        writeManager.sendMessage(new StringPacket(String.format("00\n")));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    public CompletableFuture<String> getVersion() {
        CompletableFuture<String> versionFuture = new CompletableFuture<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createStringAdapter(TVKeys.DATA_TUNER_VERSION_STRING, versionFuture::complete, false);

        writeManager.sendMessage(new StringPacket(String.format("06\n")));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated

        CompletableFuture.runAsync(() -> {
            try { versionFuture.get(5, TimeUnit.SECONDS); }
            catch (Exception e) { versionFuture.completeExceptionally(e); }
            finally { dataMatrix.finish(); }
        });

        return versionFuture;
    }

    public void setLocale(String androidLocaleTag) {
        writeManager.sendMessage(new StringPacket(String.format("36:" + androidLocaleTag + "\n"))); // TODO vortex - finalize new commands
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    public void saveDefaultChannel() {
        writeManager.sendMessage(new StringPacket(String.format("11\n")));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
    }

    /**
     * Attempts to change the video output resolution of the LG6500 STB. Note that this will
     * cause the STB to reboot (unless the specified resolution is the current resolution).
     *
     * @param resolution The output resolution the STB will be set to
     */
    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {
        int resolutionId;
        switch (resolution) {
            case OUTPUT_RESOLUTION_720p:
                resolutionId = 1;
                break;
            case OUTPUT_RESOLUTION_1080p:
                resolutionId = 4;
                break;
            default: return;
        }
        writeManager.sendMessage(new StringPacket(String.format("28:%d\n", resolutionId)));
        SystemClock.sleep(SLEEP_TIME);
    }

    public CompletableFuture<String> getIpAddress() {
        CompletableFuture<String> ipAddressFuture = new CompletableFuture<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createStringAdapter(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING, ipAddressFuture::complete, false);

        // Request the IP address from tuner
        writeManager.sendMessage(new StringPacket(String.format("07:01\n")));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated

        CompletableFuture.runAsync(() -> {
            try { ipAddressFuture.get(5, TimeUnit.SECONDS); }
            catch (Exception e) { ipAddressFuture.completeExceptionally(e); }
            finally { dataMatrix.finish(); }
        });

        return ipAddressFuture;
    }

    public CompletableFuture<String> getMacAddress() {
        CompletableFuture<String> macAddressFuture = new CompletableFuture<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createStringAdapter(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, macAddressFuture::complete, false);

        writeManager.sendMessage(new StringPacket(String.format("27\n")));
        SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated

        CompletableFuture.runAsync(() -> {
            try { macAddressFuture.get(5, TimeUnit.SECONDS); }
            catch (Exception e) { macAddressFuture.completeExceptionally(e); }
            finally { dataMatrix.finish(); }
        });

        return macAddressFuture;
    }

    public CompletableFuture<String> getModel() {
        CompletableFuture<String> modelFuture = new CompletableFuture<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createStringAdapter(TVKeys.EVENT_TUNER_MODEL_STRING, modelFuture::complete, false);

        writeManager.sendMessage(new StringPacket(String.format("29\n")));
        SystemClock.sleep(SLEEP_TIME);

        CompletableFuture.runAsync(() -> {
            try { modelFuture.get(5, TimeUnit.SECONDS); }
            catch (Exception e) { modelFuture.completeExceptionally(e); }
            finally { dataMatrix.finish(); }
        });

        return modelFuture;
    }
}
