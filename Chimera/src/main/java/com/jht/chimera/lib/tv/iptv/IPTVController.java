package com.jht.chimera.lib.tv.iptv;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import org.jetbrains.annotations.NonNls;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * XML looks like the following.
 * <Channel name="name" ip="rtsp://..." description="description" icon="icon_file" enabled="true" deinterlace="true" chnumber="0" />
 */
public class IPTVController extends TVControllerBase {
    @NonNls private final String TAG = "IPTVController";
    private List<ChannelInfo> channels = new ArrayList<>();
    private static final String channelFile = "iptvchannels.xml"; //NON-NLS
    private static final String path = "/settings/iptv/"; //NON-NLS
    private Context context;
    private int currentChannelIndex;
    private int previousChannelIndex;


    @Override
    public void exit() {

    }

    @Override
    public void syncChannels() {

    }

    @Override
    public void muteOff() {

    }

    @Override
    public void setSknetChannel(SKNETChannelInfo sknetChannelInfo) {

    }

    @Override
    public SetTopBoxSetting getCurrentSTB() {
        return null;
    }

    @Override
    public void executeIscProcess() {

    }

    @Override
    public boolean sknetGetChannels(IOnProgress onProgress) {
        return false;
    }

    @Override
    public int getIndexByChannelInfo(ChannelInfo currentChannel) {
        if(currentChannel == null) {
            return currentChannelIndex;
        }
        for(int i = 0; i < channels.size(); i++) {
            if(currentChannel.equals(channels().get(i))) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public ITunerCapabilities capabilities() {
        return new ITunerCapabilities() {
            @Override
            public boolean channelList() {
                return true;
            }

            @Override
            public boolean cc() {
                return true;
            }

            @Override
            public boolean sap() {
                return true;
            }

            @Override
            public boolean lastChannel() {
                return true;
            }

            @Override
            public boolean info() {
                return false;
            }

            @Override
            public boolean colorButtons() {
                return false;
            }

            @Override
            public boolean hdmi() {
                return false;
            }

            @Override
            public boolean hasIPScanList() {
                return false;
            }

            @Override
            public boolean keypad() {
                return false;
            }

            @Override
            public boolean update() {
                return false;
            }

            @Override
            public boolean importChannelList() {
                return false;
            }

            @Override
            public boolean menu() {
                return false;
            }

            @Override
            public boolean exit() {
                return false;
            }

            @Override
            public boolean enter() {
                return false;
            }

            @Override
            public boolean select() {
                return false;
            }

            @Override
            public boolean cabCompatible() { return false; }

            @Override
            public boolean canEditChannelName() { return true; }

            @Override
            public boolean isSetTopBox() { return false; }

            @Override
            public boolean modifyChannelList() {
                return true;
            }

            @Override
            public boolean autoRefresh() {
                return true;
            }

            @Override
            public boolean dash() { return keypad(); }

            @Override
            public boolean guide() { return false; }

            @Override
            public boolean canSkipChannel() { return false; }

            @Override
            public boolean setup() { return false; }

            @Override
            public boolean back() { return false; }

            @Override
            public boolean input() { return false; }

            @Override
            public boolean volume() { return false; }
        };
    }

    public IPTVController(Context context) {
        this.context = context;

        try {
            currentChannelIndex = DataMatrix.getInt(TVKeys.DATA_IPTV_CURRENT_CHANNEL_INT);
            previousChannelIndex = DataMatrix.getInt(TVKeys.DATA_IPTV_PREVIOUS_CHANNEL_INT);

            Log.d(TAG, "currentChannelIndex " + currentChannelIndex);
        }
        catch(Exception ex) {
            reset();
        }
        loadChannels();
    }

    @Override
    public List<ChannelInfo> channels() {
        return channels;
    }

    @Override
    public void setChannel(ChannelInfo channnel) {
        previousChannelIndex = currentChannelIndex;
        currentChannelIndex = getIndexByChannelInfo(channnel);
        super.setChannel(channnel);
    }

    @Override
    protected void setChannelImpl(ChannelInfo channelInfo) {
        super.setChannelImpl(channelInfo);

        if (TextUtils.isEmpty(channelInfo.number())) {
            return;
        }

        DataMatrix.putInt(TVKeys.DATA_IPTV_CURRENT_CHANNEL_INT, currentChannelIndex);
        DataMatrix.putInt(TVKeys.DATA_IPTV_PREVIOUS_CHANNEL_INT, previousChannelIndex);
    }

    @Override
    public void zero() {
        
    }

    @Override
    public void one() {
    }

    @Override
    public void two() {
        
    }

    @Override
    public void three() {
        
    }

    @Override
    public void four() {
        
    }

    @Override
    public void five() {
        
    }

    @Override
    public void six() {
        
    }

    @Override
    public void seven() {
        
    }

    @Override
    public void eight() {
        
    }

    @Override
    public void nine() {
        
    }

    @Override
    public void dash() {
        
    }

    @Override
    public void enter() {
        
    }

    @Override
    public void select() {

    }

    @Override
    public void channelUp() {
        if(channels.size() > 0) {
            if(currentChannelIndex >= 0) {
                int nextChannel = currentChannelIndex + 1;
                if (nextChannel >= channels.size()) {
                    setChannel(channels.get(0));
                } else {
                    setChannel(channels.get(nextChannel));
                }
            }
        }
    }

    @Override
    public void channelDown() {
        if(channels.size() > 0) {
            if(currentChannelIndex > -1) {
                int nextChannel = currentChannelIndex == 0 ? channels.size() - 1 : currentChannelIndex - 1;
                setChannel(channels.get(nextChannel));
            }
        }
    }

    @Override
    public void lastChannel() {
        if(channels.size() > 0) {
            if (previousChannelIndex != currentChannelIndex) {
                setChannel(channels.get(previousChannelIndex));
            }
        }
    }

    @Override
    public void toggleCC() {
    }

    @Override
    public void toggleSAP() {
    }

    @Override
    public void reset() {
        String defaultUuid = defaultChannelString();
        int targetIndex = 0;

        if (!TextUtils.isEmpty(defaultUuid)) {
            for (int i = 0; i < channels.size(); i++) {
                if (defaultUuid.equals(channels.get(i).uuid())) {
                    targetIndex = i;
                    break;
                }
            }
        }

        Log.d(TAG, "reset() targeting index " + targetIndex);
        currentChannelIndex = targetIndex;
        DataMatrix.putInt(TVKeys.DATA_IPTV_CURRENT_CHANNEL_INT, targetIndex);
        DataMatrix.putInt(TVKeys.DATA_IPTV_PREVIOUS_CHANNEL_INT, targetIndex);
    }

    @Override
    public ChannelInfo currentChannel() {
        if(channels.size() > 0 && currentChannelIndex >= 0 && currentChannelIndex < channels.size()) {
            return channels.get(currentChannelIndex);
        }
        return null;
    }

    @Override
    public void close() {
        
    }

    @Override
    public void setDefaultChannel() {
        ChannelInfo current = currentChannel();
        if (current != null) {
            defaultChannel(current.uuid());
        }
    }

    @Override
    public void red() {}

    @Override
    public void green() {}

    @Override
    public void yellow() {}

    @Override
    public void blue() {}

    @Override
    public void info() {}

    @Override
    public void guide() {

    }

    @Override
    public void back() {

    }


    @Override
    public void cornerButtonOperation(TVExtendedControls.CORNER_BUTTON_OPERATION operation) {
        if (operation == TVExtendedControls.CORNER_BUTTON_OPERATION.CC) {
            toggleCC();
        }
    }

    @Override
    public void left() {
        volumeDown();
    }

    @Override
    public void right() {
        volumeUp();
    }

    @Override
    public void up() {
        if(currentChannelIndex < channels.size() - 1) {
            setChannel(channels.get(currentChannelIndex + 1));
        }
        else if(currentChannelIndex == channels.size() - 1) {
            setChannel(channels.get(currentChannelIndex));
        }
    }

    @Override
    public void down() {
        if(currentChannelIndex > 0 ) {
            setChannel(channels.get(currentChannelIndex - 1));
        }
        else if(currentChannelIndex == channels.size() - 1) {
            setChannel(channels.get(currentChannelIndex));
        }
    }

    @Override
    public ChannelInfo addChannel() {
        if(channels.size() > 0) {
            ChannelInfo lastChannel = channels.get(channels.size() - 1);
            int channelNumber = channels.size() + 1;
            ChannelInfo newChannel = new ChannelInfo(lastChannel);
            newChannel.number(channelNumber + "");
            channels.add(newChannel);
            return newChannel;
        }
        ChannelInfo channelInfo = new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "1", "", null);
        channels.add(channelInfo);
        setChannel(channelInfo);
        return channelInfo;
    }

    @Override
    public void deleteChannel(ChannelInfo channel) {
        int index = getIndexByChannelInfo(channel);
        if(index >= 0 && index < channels.size()) {
            // If deleting the current default, clear it so reset() falls back to index 0.
            if (channel.uuid().equals(defaultChannelString())) {
                defaultChannel("");
            }
            channels.remove(index);
            saveChannels(false);
            if(currentChannelIndex >= index && channels.size() > 0) {
                if(currentChannelIndex > 0) {
                    setChannel(channels.get(currentChannelIndex - 1));
                }
            }
        }
    }

    @Override
    public void open() {

    }


    @Override
    public String defaultChannelString() {
        return DataMatrix.getString(SettingKeys.TV.DATA_TV_IPTV_DEFAULT_CHANNEL_UUID_STRING);
    }

    @Override
    public void defaultChannel(String uuid) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_IPTV_DEFAULT_CHANNEL_UUID_STRING, uuid);
    }

    @Override
    public int defaultChannelInt() {
        String defaultUuid = defaultChannelString();
        if (!TextUtils.isEmpty(defaultUuid)) {
            for (int i = 0; i < channels.size(); i++) {
                if (defaultUuid.equals(channels.get(i).uuid())) {
                    return i;
                }
            }
        }
        // Migration fallback, no UUID stored, use the legacy integer index.
        int legacyIndex = DataMatrix.getInt(SettingKeys.TV.DATA_TV_IPTV_DEFAULT_CHANNEL_INT);
        if (legacyIndex > 0 && legacyIndex < channels.size()) {
            return legacyIndex;
        }
        return 0;
    }

    @Override
    public void defaultChannel(int channel) {
        if (channel >= 0 && channel < channels.size()) {
            defaultChannel(channels.get(channel).uuid());
        }
    }

    @Override
    public void loadChannels() {
        channels.clear();
        File file = new File(context.getFilesDir(), path + channelFile);
        if(file.exists()) {
            XmlPullParserFactory pullParserFactory;
            try {
                pullParserFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = pullParserFactory.newPullParser();

                try(InputStream inputStream = new FileInputStream(file)) {
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(inputStream, null);

                    int eventType = parser.getEventType();
                    int index = 1;

                    while (eventType != XmlPullParser.END_DOCUMENT)
                    {
                        String name;
                        switch (eventType)
                        {
                            case XmlPullParser.START_DOCUMENT:
                                break;
                            case XmlPullParser.START_TAG:

                            name = parser.getName();
                            if (name.equals("Channel"))
                            {
                                channels.add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, new File(context.getFilesDir(), path).getAbsolutePath(), parser, index));
                                index++;
                            }

                                break;
                            case XmlPullParser.END_TAG:
                                break;
                        }

                        eventType = parser.next();
                    }
                } catch (Exception ex) {
                    Log.w(TAG, ex);
                }
            } catch (XmlPullParserException parserEx) {
                Log.w(TAG, parserEx);
            }
            Collections.sort(channels, new ChannelInfo.ChannelInfoSort());
            // Persist UUIDs so newly assigned ones survive across sessions.
            saveChannels(false);
        }

        // TODO This logic needs to get reviewed. It should technically need to be only run once or if the channel list changes
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) updateRoutingTableAsync();
    }

    private static final ExecutorService routeExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "routeTable"));
    private void updateRoutingTableAsync() {
        // task should run one at a time but multiples could still queue
        final List<ChannelInfo> snapshot = new ArrayList<>(channels);
        routeExecutor.execute(() -> {
            try {
                updateRoutingTable(snapshot);
            } catch (Exception e) {
                Log.e(TAG, "updateRoutingTable crash", e);
            }
        });
    }

    /** updates the routing table based on channel list (DO NOT RUN ON MAIN THREAD) */
    private void updateRoutingTable(List<ChannelInfo> snapshot) {
        // use a set for the command list to deduplicate the commands
        Set<String> commandList = new LinkedHashSet<>();
        for (ChannelInfo channel : snapshot) {
            String url = channel.url();
            if (url == null) continue;

            ChannelInfo.URLDecode d = new ChannelInfo.URLDecode(url);
            if (d.protocol().contains("HTTP")) continue;
            String host = d.address();
            if (host.isEmpty()) continue;

            String ip;
            try {
                // note: fast for numeric addresses but longer host name DNS look ups could add up! Host name look up without network could also stall?
                ip = InetAddress.getByName(host).getHostAddress();
            } catch (Exception e) {
                Log.w(TAG, "updateRoutingTable: unresolved " + channel, e);
                continue;
            }

            String[] octets = ip.split("\\.");
            if (octets.length < 3) continue;   // IPv6 or garbage
            int first;
            try { first = Integer.parseInt(octets[0]); } catch (NumberFormatException e) { continue; }
            if (first < 224 || first > 239) {          // not 224.0.0.0/4
                Log.w(TAG, "updateRoutingTable: skipping non-multicast " + ip);
                continue;
            }
            commandList.add("ip route add " + octets[0] + "." + octets[1] + "." + octets[2] + ".0/24 dev eth0 proto static scope link table wlan0");
        }
        // Should we be removing the old routes too? reboot probably clears them on console
        ExecuteAsRoot shell = new ExecuteAsRoot();
        for (String command : commandList) {
            Log.d(TAG, "updateRoutingTable: " + command);
            shell.addCommand(command);
        }
        shell.execute();
    }


    /**
     * Import the channels from the USB and store it on the USB and tuner.
     */
    @Override
    public boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode) {
        new Thread(() -> {

            if(mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH_TO_TUNER || mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH) {
                File srcFile = new File(StorageUtils.getUSBPath(), channelFile);
                if (srcFile.exists()) {
                    File destFolder = new File(context.getFilesDir(), path);
                    File destFile = new File(destFolder, channelFile);
                    //noinspection ResultOfMethodCallIgnored
                    destFolder.mkdirs();

                    ExecuteAsRoot exec = new ExecuteAsRoot();
                    exec.addCommand("cp -f " + srcFile.getAbsolutePath() + " " + destFile.getAbsolutePath());
                    exec.execute();
                    ExecuteAsRoot.fixFilesInPackage(context.getPackageName());
                    loadChannels();

                    onProgress.onComplete(true, ATSC_STATUS.ACKOK);
                } else {
                    onProgress.onComplete(false, ATSC_STATUS.ACKOK);
                }
            }
            else {
                onProgress.onComplete(false, ATSC_STATUS.ACKOK);
            }
        }).start();
        return false;
    }

    @Override
    public boolean exportChannelsUSB() {

        File udisk = StorageUtils.getUSBPath();
        File srcFolder = new File(context.getFilesDir(), path);

        boolean retVal = FileUtils.copyFile(new File(srcFolder, channelFile), new File(udisk, channelFile));
        ExecuteAsRoot.runSyncCommand();
        return retVal;
    }

    /**
     * Import the channels from the USB and store it on the USB and tuner.
     */
    @Override
    public boolean exportChannels(IOnProgress onProgress) {
        new Thread(() -> onProgress.onComplete(exportChannelsUSB(), ATSC_STATUS.ACKOK)).start();
        return false;
    }

    @Override
    public void saveChannels(boolean toUsb) {
        File folder = new File(context.getFilesDir(), path);
        File file = new File(folder, channelFile);
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();

        Collections.sort(channels, new ChannelInfo.ChannelInfoSort());


        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();


        try(FileOutputStream fout = new FileOutputStream(file)) {

			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "IPTVChannels");
			serializer.startTag("", "Channels");
			for (ChannelInfo ch : channels)
			{
			    ch.toXml(serializer);
			}
			serializer.endTag("", "Channels");
			serializer.endTag("", "IPTVChannels");
			serializer.endDocument();

            fout.write(writer.toString().getBytes());
            fout.flush();
        }
        catch (Exception e)
        {
            Log.w(TAG, e);
        }

    }

    @Override
    public ChannelInfo.ChannelType channelType() {
        return ChannelInfo.ChannelType.UNENCRYPTED_IPTV;
    }

    @Override
    public void scan() {

    }

}
