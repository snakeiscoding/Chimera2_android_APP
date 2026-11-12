package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.IOnProgress;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgBroadcastType;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannelLoader;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannelSort;
import com.jht.chimera.lib.tv.tuner.lg.channel.LgPolarization;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.channel.ChannelLoader;
import com.jht.serialport.serialcommunication.IOnInvalidChecksum;
import com.jht.serialport.serialcommunication.TransactionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// The communication protocol is defined in https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/3468886033/STB-6500+Communication
/**
 * Talks to the LG750E tuner through {@link Lg750EConnection} in the HAL service: commands go out
 * through a {@link Lg750ECommandSender}, and replies come back as DataMatrix keys, the same for
 * either protocol. There is no serial port, so the stream-based parts of
 * the tuner interface are no-ops.
 */
public class Lg750ETunerBase extends com.jht.serialport.peripheral.tuner.TunerBase<LgChannel> {
    public static final String TAG = "LG750E";
    public static final int DEFAULT_CC_ON_INDEX = 1;

    private static final int CHANNEL_IMPORT_TIMEOUT_MS = 5 * 60 * 1_000;
    private static final int REPLY_TIMEOUT_MS = 5 * 1_000;

    public static final String LG_CHANNEL_FILE = "channels.xml";
    private static final String LG_CHANNEL_FILE_XSD = "lg_channel_list.xsd";

    // Sends commands to the LG box.
    private final Lg750ECommandSender sender;

    // Used to manage the channel list.
    private final File settingsDir;

    // This holds the channel list for LG.
    private final List<LgChannel> channelList = new ArrayList<>();

    // This is the closed caption index
    private int closedCaptionIndex = 0;

    // In-flight state for downloadChannelsFromTuner. Cleared atomically on completion or timeout
    // so that only one thread (DataMatrix callback or timeout) can commit the result.
    private final AtomicReference<IOnProgress> channelImportProgress = new AtomicReference<>(null);
    private final List<LgChannel> channelImportBuffer = new ArrayList<>();

    /**
     * Whether or not a factory reset request is currently being sent to the tuner (the
     * RMS call is running). Cleared once RMS acknowledges the request - does NOT track
     * the tuner's actual reboot, which can take several minutes after this clears.
     * Atomic to ensure only one request is sent at a time.
     */
    private final AtomicBoolean factoryResetRequestRunning = new AtomicBoolean(false);

    /**
     * Creates a tuner that sends its commands over DataMatrix to the HAL service.
     *
     * @param context      Needed to load assets.
     * @param settingsDir  Location to find tuner settings and channel lists.
     */
    public Lg750ETunerBase(@Nullable Context context, @NonNull File settingsDir) {
        this(context, settingsDir, Lg750ECommandSender.DATA_MATRIX);
    }

    /**
     * @param context      Needed to load assets.
     * @param settingsDir  Location to find tuner settings and channel lists.
     * @param sender       Where commands for the tuner go.
     */
    public Lg750ETunerBase(@Nullable Context context, @NonNull File settingsDir, @NonNull Lg750ECommandSender sender) {
        this.sender = sender;
        this.settingsDir = settingsDir;
        //noinspection ResultOfMethodCallIgnored
        settingsDir.mkdirs();
        if (context != null) {
            FileUtils.copyAssetToFlash(context, "tuner/lg/lg_channel_list.xsd", new File(settingsDir, LG_CHANNEL_FILE_XSD));

        }
        loadSettings();
    }

    private void send(@NonNull Lg750ECommand command) {
        sender.send(command);
    }

    @Override
    protected void setChannelImpl(LgChannel channel) {
        Lg750ECommand.Tune command = Lg750ECommand.tune(channel);
        if (command != null) send(command);
    }

    /**
     * @return Returns the capabilities for the tuner.  Useful for determining what buttons to display to the user
     */
    @Override
    public ITunerCapabilities capabilities() {
        return new Lg750ECapabilities();
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
     * Triggers a channel list fetch via DataMatrix. Lg750EConnection observes the start event and
     * sends getChannelList (classic command 41) over the WebSocket. The tuner replies with one channel
     * object per message and then an end marker; the connection collects them and delivers the
     * complete list as a JSON array string via EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING, which this
     * method parses. Both protocols deliver the same channel fields.
     *
     * WebSocket is required — the STB's RS-232C API caps replies at 32 bytes which cannot carry
     * the ~300-byte per-channel JSON objects.
     *
     * @param onProgress Callback used to provide progress updates.
     */
    @Override
    public void downloadChannelsFromTuner(IOnProgress onProgress) {
        if (channelImportProgress.get() != null) {
            Log.w(TAG, "downloadChannelsFromTuner: import already in progress, ignoring");
            if (onProgress != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        onProgress.progress(100, false, "Import already in progress"));
            }
            return;
        }
        Log.d(TAG, "downloadChannelsFromTuner: started");
        channelImportBuffer.clear();
        channelImportProgress.set(onProgress);

        DataMatrix localDm = new DataMatrix();

        localDm.createStringAdapter(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING, result -> {
            localDm.finish();
            Log.d(TAG, "downloadChannelsFromTuner: received import result");
            try {
                JSONArray arr = new JSONArray(result);
                for (int i = 0; i < arr.length(); i++) {
                    LgChannel ch = parseRfChannelFromJson(arr.getJSONObject(i));
                    if (ch != null) {
                        channelImportBuffer.add(ch);
                    }
                }
                Log.d(TAG, "downloadChannelsFromTuner: parsed " + channelImportBuffer.size() + " channels");
            } catch (Exception e) {
                Log.e(TAG, "downloadChannelsFromTuner: failed to parse channel result", e);
            }
            finishChannelImport(!channelImportBuffer.isEmpty(), channelImportBuffer.isEmpty() ? "No channels found" : null);
        }, false);

        localDm.createStringAdapter(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING, error -> {
            localDm.finish();
            Log.e(TAG, "downloadChannelsFromTuner: error from tuner: " + error);
            finishChannelImport(false, "Channel import error: " + error);
        }, false);

        Log.d(TAG, "downloadChannelsFromTuner: sending ACTION_LG750E_CHANNEL_IMPORT_START");
        DataMatrix.sendEvent(TVKeys.ACTION_LG750E_CHANNEL_IMPORT_START);

        new Thread(() -> {
            ThreadHelper.sleepIgnoreInterruption(CHANNEL_IMPORT_TIMEOUT_MS);
            if (channelImportProgress.get() != null) {
                Log.w(TAG, "downloadChannelsFromTuner: timed out waiting for STB response");
                localDm.finish();
                finishChannelImport(false, "Channel import timed out");
            }
        }, "LG750E-channel-import-timeout").start();
    }

    /**
     * Triggers a factory reset on the tuner via RMS (Remote Manager Service), with an
     * optional listener for status updates and the final result. Callback is invoked
     * on a background thread.
     *
     * A fresh Lg750ERmsClient is created per call and closed on completion. If a reset
     * is already in progress, this is a no-op and the listener's onComplete is called
     * immediately with success=false. This only guards against overlapping requests, not
     * the tuner's actual reset, which can take upwards of 15mins after onComplete fires.
     */
    public void factoryResetTuner(@Nullable Lg750ERmsClient.ResetListener listener) {
        if (!factoryResetRequestRunning.compareAndSet(false, true)) {
            Log.w(TAG, "factoryResetTuner: reset already in progress, ignoring");
            if (listener != null) listener.onComplete(false, "Factory reset already in progress");
            return;
        }

        Lg750ERmsClient client = new Lg750ERmsClient();
        client.factoryReset(new Lg750ERmsClient.ResetListener() {
            @Override
            public void onStatus(String message) {
                if (listener != null) listener.onStatus(message);
            }

            @Override
            public void onComplete(boolean success, @Nullable String errorMessage) {
                if (listener != null) listener.onComplete(success, errorMessage);
                client.close();
                factoryResetRequestRunning.set(false);
            }
        });
    }

    /**
     * Same as {@link #factoryResetTuner(Lg750ERmsClient.ResetListener)}, reporting only
     * the final success/failure result. Callback is invoked on a background thread.
     */
    public void factoryResetTunerWithResult(@NonNull java.util.function.Consumer<Boolean> onResult) {
        factoryResetTuner(new Lg750ERmsClient.ResetListener() {
            @Override
            public void onStatus(String message) {
                // not used — caller only cares about the final result
            }

            @Override
            public void onComplete(boolean success, @Nullable String errorMessage) {
                onResult.accept(success);
            }
        });
    }

    @Nullable
    private LgChannel parseRfChannelFromJson(JSONObject ch) {
        try {
            LgBroadcastType.RfBroadcast broadcastType = LgBroadcastType.RfBroadcast.Companion.fromName(ch.optString("rfBroadcastType"));
            String name = ch.optString("name", "");
            String number = ch.optString("number", "");
            String major = ch.optString("majorNumber", "");
            String minor = ch.optString("minorNumber", "");
            String frequency = ch.optString("frequency", "");
            String programNumber = ch.optString("programNumber", "");
            String logicalNumber = ch.optString("logicalNumber", "");
            String plpId = ch.optString("plpId", "");
            String satelliteId = ch.optString("satelliteId", "");
            String polarization = ch.optString("polarization", "");

            String rfClassRaw = ch.optString("rfClass", "");
            LgChannel.ChannelType channelType = LgChannel.ChannelType.getEntries().stream()
                    .filter(type -> type.getIdentifiers().stream().anyMatch(id -> id.equalsIgnoreCase(rfClassRaw)))
                    .findFirst()
                    .orElse(LgChannel.ChannelType.RF_CLASS_1);

            LgChannel channel;
            switch (channelType) {
                case RF_CLASS_4:
                    channel = new LgChannel.RfChannel.Class4(broadcastType, frequency, programNumber, satelliteId,
                            LgPolarization.Companion.fromName(polarization));
                    break;
                case RF_CLASS_6:
                    channel = new LgChannel.RfChannel.Class6(broadcastType, major, minor, plpId);
                    break;
                case RF_CLASS_5:
                    channel = new LgChannel.RfChannel.Class5(broadcastType, frequency, programNumber, plpId);
                    break;
                case RF_CLASS_3:
                    channel = new LgChannel.RfChannel.Class3(broadcastType, major, minor);
                    break;
                case RF_CLASS_2:
                    channel = new LgChannel.RfChannel.Class2(broadcastType, frequency, programNumber);
                    break;
                default:
                    channel = new LgChannel.RfChannel.Class1(broadcastType, logicalNumber);
                    break;
            }
            channel.name(name);
            channel.number(number);
            return channel;
        } catch (Exception e) {
            Log.w(TAG, "parseRfChannelFromJson: failed", e);
            return null;
        }
    }

    /**
     * Completes a channel list import. Only the first caller wins (AtomicReference CAS). If both
     * the DataMatrix callback and the timeout thread attempt completion, the second returns
     * immediately, preventing duplicate updates and notifications.
     *
     * On success, replaces the current channel list and saves it to disk. File I/O happens on
     * the calling thread; the progress callback is always delivered on the UI thread because
     * progress dialogs require it.
     *
     * On failure, the existing channel list and saved settings are unchanged.
     */
    private void finishChannelImport(boolean success, @Nullable String errorMessage) {
        Log.d(TAG, "finishChannelImport: success=" + success + " error=" + errorMessage);
        IOnProgress progress = channelImportProgress.getAndSet(null);
        if (progress == null) {
            Log.w(TAG, "finishChannelImport: progress already cleared (duplicate completion), ignoring");
            return;
        }
        if (success && !channelImportBuffer.isEmpty()) {
            channelList.clear();
            channelList.addAll(channelImportBuffer);
            Log.d(TAG, "finishChannelImport: saved " + channelList.size() + " channels");
            saveSettings();
        }
        channelImportBuffer.clear();
        Log.d(TAG, "finishChannelImport: delivering progress callback on UI thread");
        new Handler(Looper.getMainLooper()).post(() -> progress.progress(100, true, errorMessage));
    }

    /**
     * Loads the settings.  Typically this is the channel list.
     */
    @Override
    public void loadSettings() {
        channelList.clear();
        File file = new File(settingsDir, LG_CHANNEL_FILE);
        if (file.exists() && file.length() > 0) {
            try {
                new LgChannelLoader().loadList(new File(settingsDir, LG_CHANNEL_FILE_XSD), file, channelList);
            } catch (Exception e) {
                Log.w(TAG, "parse threw exception: " + e.getMessage(), e);
            }
            channelList.sort(new LgChannelSort());
            if (channelList.isEmpty()) {
                // Non-empty file that parses to 0 channels is in an incompatible format (e.g. a
                // legacy LG6500/Enseo XML written before the LG750E schema was in place). Log the
                // file header for diagnosis, then delete so the next CORE sync can replace it.
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    String header = new String(bytes, 0, Math.min(bytes.length, 512), java.nio.charset.StandardCharsets.UTF_8);
                    Log.w(TAG, "incompatible channels.xml (" + bytes.length + " bytes, 0 channels) file header: " + header);
                } catch (IOException e) {
                    Log.w(TAG, "incompatible channels.xml could not read for diagnosis: " + e.getMessage());
                }
                Log.w(TAG, "deleting incompatible channels.xml");
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /**
     * Save the settings.  Typically this is the channel list.
     */
    @Override
    public void saveSettings() {
        channelList.sort(new LgChannelSort());
        File file = new File(settingsDir, LG_CHANNEL_FILE);
        new LgChannelLoader().saveList(file, channelList);
        ExecuteAsRoot.runSyncCommand();
    }

    /**
     * @return Used to get the current channel list.
     */
    @Override
    public List<LgChannel> getChannelList(boolean includeAll) {
        if(includeAll) {
            return channelList;
        }
        else {
            List<LgChannel> activeChannels = new ArrayList<>();
            for(LgChannel channel : channelList) {
                if(channel.enabled()) {
                    activeChannels.add(channel);
                }
            }
            return activeChannels;
        }
    }

    /**
     * Restarts the channel-change thread. The streams are unused: commands go through the
     * {@link Lg750ECommandSender} and replies arrive over DataMatrix.
     */
    @Override
    public void restart(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, @Nullable IOnInvalidChecksum onInvalidChecksum) {
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
            Context context = ContextUtil.getApplicationContext();
            Handler ui = new Handler(Looper.getMainLooper());
            Lg750EResponse settings = new Lg750EResponse();

            // show dialog immediately, then have callback refresh it when ready
            Lg750ETunerSettings dialog = new Lg750ETunerSettings(context, this);
            dialog.show();

            // The tuner answers command 07 with dhcp, subnet, gateway and dns lines alongside ip, so
            // listen for those before getIpAddress() sends it.
            DataMatrix networkDm = new DataMatrix();
            networkDm.createStringAdapter(TVKeys.EVENT_TUNER_DHCP_STRING, value -> settings.dhcp = value, false);
            networkDm.createStringAdapter(TVKeys.EVENT_TUNER_SUBNET_STRING, value -> settings.subnet = value, false);
            networkDm.createStringAdapter(TVKeys.EVENT_TUNER_GATEWAY_STRING, value -> settings.gateway = value, false);
            networkDm.createStringAdapter(TVKeys.EVENT_TUNER_DNS1_STRING, value -> settings.dns1 = value, false);
            networkDm.createStringAdapter(TVKeys.EVENT_TUNER_DNS2_STRING, value -> settings.dns2 = value, false);

            // send get version command
            CompletableFuture<Void> versionFuture = getVersion().thenAccept(value -> settings.version = value);
            // send get ip command
            CompletableFuture<Void> ipFuture = getIpAddress().thenAccept(value -> settings.ip = value);
            // send get mac address command
            CompletableFuture<Void> macFuture = getMacAddress().thenAccept(value -> settings.mac = value);

            // we cannot block the ui thread waiting on futures because futures rely on DM callbacks that queue on ui thread
            CompletableFuture.allOf(versionFuture, ipFuture, macFuture)
                    .whenComplete((r, e) -> ui.post(() -> {
                        networkDm.finish();
                        dialog.refresh(settings);
                    }));
        }
    }

    /**
     * Handles remote presses.  Generally we can act as a virtual remote for the tuner.
     *
     * @param button The button to press.
     * @param count  The number of times to push the button.
     */
    void clickRemoteButton(REMOTE_BUTTON button, int count) {
        Lg750EKey key = Lg750EKey.forRemoteButton(button);
        if (key == null) return;
        for (int i = 0; i < count; i++) {
            send(new Lg750ECommand.KeyPress(key, 1));
            onRemoteButton(button);
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
    public void sendChannelEnableStatus(LgChannel channel) throws UnsupportedOperationException {
        // Do nothing, throwing UnsupportedTunerOperation will cause issues with enabling/disabling channels
    }

    /**
     * Always null: commands are sent immediately and none of them is acknowledged, so there is
     * nothing to track. Callers wait for a fixed time instead.
     */
    @Nullable
    @Override
    public TransactionManager startTransaction() {
        return null;
    }


    /**
     * Used to set the tuner volume.  The range can be different across tuners.
     */
    @Override
    public void setTunerVolume(double volume) {
        //Log.d(TAG, "setTunerVolume: " + volume);
        send(new Lg750ECommand.SetVolume((int) volume));
    }

    public void setClosedCaptionIndex(int index) {
        closedCaptionIndex = index;
        send(new Lg750ECommand.SetClosedCaption(closedCaptionIndex));
    }

    public void reboot() {
        send(Lg750ECommand.Reboot.INSTANCE);
    }
    public void setSoftAp(String ssid, String pw) {
        send(new Lg750ECommand.SetSoftAp(ssid, pw));
    }
    public void clearUserData() {
        send(Lg750ECommand.ClearUserData.INSTANCE);
    }

    public void enableDhcp() {
        send(Lg750ECommand.EnableDhcp.INSTANCE);
    }

    public void setStaticNetwork(String ip, String subnetMask, String gateway, String dns1, String dns2) {
        send(new Lg750ECommand.SetStaticNetwork(ip, subnetMask, gateway, dns1, dns2));
    }

    public void setLocale(String androidLocaleTag) {
        send(new Lg750ECommand.SetLocale(androidLocaleTag));
    }

    public void saveDefaultChannel() {
        send(Lg750ECommand.SaveDefaultChannel.INSTANCE);
    }

    /**
     * Attempts to change the video output resolution of the LG750E STB. Note that this will
     * cause the STB to reboot (unless the specified resolution is the current resolution).
     *
     * @param resolution The output resolution the STB will be set to
     */
    public void setTunerOutputResolution(@NonNull TunerProperties.OUTPUT_RESOLUTION resolution) {
        switch (resolution) {
            case OUTPUT_RESOLUTION_720p:
            case OUTPUT_RESOLUTION_1080p:
            case OUTPUT_RESOLUTION_2160p:
                break; // allowed
            default:
                return; // unsupported resolution
        }
        send(new Lg750ECommand.SetResolution(resolution.name()));
    }

    /**
     * Sends {@code command} and completes with the next value published to {@code replyKey}, or
     * exceptionally after {@link #REPLY_TIMEOUT_MS}. The listener is registered before the command
     * goes out so a fast reply is not missed.
     */
    private CompletableFuture<String> request(@NonNull TVKeys replyKey, @NonNull Lg750ECommand.Query command) {
        CompletableFuture<String> reply = new CompletableFuture<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createStringAdapter(replyKey, reply::complete, false);

        send(command);

        CompletableFuture.runAsync(() -> {
            try { reply.get(REPLY_TIMEOUT_MS, TimeUnit.MILLISECONDS); }
            catch (Exception e) { reply.completeExceptionally(e); }
            finally { dataMatrix.finish(); }
        });

        return reply;
    }

    public CompletableFuture<String> getVersion() {
        return request(TVKeys.DATA_TUNER_VERSION_STRING, Lg750ECommand.GetVersion.INSTANCE);
    }

    public CompletableFuture<String> getIpAddress() {
        return request(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING, Lg750ECommand.GetNetwork.INSTANCE);
    }

    public CompletableFuture<String> getMacAddress() {
        return request(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, Lg750ECommand.GetMac.INSTANCE);
    }

    public CompletableFuture<String> getModel() {
        return request(TVKeys.EVENT_TUNER_MODEL_STRING, Lg750ECommand.GetModel.INSTANCE);
    }
}
