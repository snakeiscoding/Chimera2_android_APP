package com.jht.chimera.lib.tv.settopbox;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.resources.ResourceHelper;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.hdmi.HDMIResolution;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.chimera.service.hal.lib.hardware.IrDriver;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SetTopBoxController extends TVControllerBase {
    @NonNls private static final String TAG = "SetTopBoxController";

    public enum SET_TOP_BOX_COMMANDS {
        zero,
        one,
        two,
        three,
        four,
        five,
        six,
        seven,
        eight,
        nine,
        dash,
        enter,
        select,
        channelup,
        channeldown,
        channellast,
        togglecc,
        togglesap,
        exit,
        red,
        green,
        yellow,
        blue,
        info,
        menu,
        left,
        right,
        up,
        down,
        wait,
        setup,
        mute,
        CMD_SCAN,
        CMD_CHANNEL,
        CMD_GET_CHANNELS, // SKNET get channel
        CMD_REFRESH_CHANNELS, // Refresh channel list
        CMD_GET_DEVICE_INFO, // get device info for SKNET STB
        CMD_DEFAULT_CHANNEL, // Set default channel for SKNET STB
        CMD_STOP_SCAN, // Stop scanning for SKNET STB
        CMD_SAVE_SCAN_RESULT, // Save scan result for SKNET STB
        CMD_SET_AUDIO_INDEX, // Set audio index for SKNET STB
        CMD_GET_SCAN_STATUS, // get scan status for SKNET STB
        CMD_GET_CURRENT_STATUS, // get status of current channel for SKNET STB
        CMD_REBOOT, // reboot for SKNET STB
        guide,
        back,
        poweron
    }

    protected static final String STB_FILEPATH = "settings/tv_settings/set_top_boxes"; //NON-NLS
    private static final String STB_SETTING_FILENAME = "settings.json"; //NON-NLS

    private final DataMatrix dataMatrix = new DataMatrix();
    private final List<SetTopBoxSetting> setTopBoxes = new ArrayList<>();
    protected SetTopBoxSetting enseoRS232;  // Note this is special as it is handled in the ProIPTV.
    private SetTopBoxSetting currentSTB;
    protected Context context;
    private STBComPortManager stbComPortManager = null;
    private final List<ChannelInfo> channels = new ArrayList<>();
    private int currentChannelIndex;
    private int lastChannelIndex;

    private List<SKNETChannelInfo> sknetChannels = new ArrayList<>();
    private boolean isSKNet = false;
    private boolean isLG6500RS232 = false;

    private IrDriver irDriver;

    private ReceivingRadio type = ReceivingRadio.DTB;
    private final Object lock = new Object();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private IOnProgress onSknetProgress;
    @SuppressLint("SdCardPath")
    @NonNls private final static String SKNET_FILENAME = "/data/data/com.jht.chimera/files/settings/tv_settings/set_top_boxes/SKNET_channels.json";
    @NonNls private static final String sknet_channels_filename = "SKNET_channels.json";
    @NonNls private static final String sknet_channels_path = "/settings/tv_settings/set_top_boxes/";
    protected String channelFile() {
        return "rs232ChannelConfig.xml"; //NON-NLS
    }

    @Override
    public ITunerCapabilities capabilities() {
        return new ITunerCapabilities() {
            @Override
            public boolean channelList() {
                if(isSKNet)
                    return true;
                else
                    return false;
            }

            @Override
            public boolean cc() {
                return hasCommand(SET_TOP_BOX_COMMANDS.togglecc);
            }

            @Override
            public boolean sap() {
                return hasCommand(SET_TOP_BOX_COMMANDS.togglesap);
            }

            @Override
            public boolean lastChannel() {
                return hasCommand(SET_TOP_BOX_COMMANDS.channellast);
            }

            @Override
            public boolean info() {
                return hasCommand(SET_TOP_BOX_COMMANDS.info);
            }

            @Override
            public boolean colorButtons() {
                return hasCommand(SET_TOP_BOX_COMMANDS.red) || hasCommand(SET_TOP_BOX_COMMANDS.green) || hasCommand(SET_TOP_BOX_COMMANDS.blue) || hasCommand(SET_TOP_BOX_COMMANDS.yellow);
            }

            @Override
            public boolean hdmi() {
                return true;
            }

            @Override
            public boolean hasIPScanList() {
                return false;
            }

            @Override
            public boolean keypad() {
                return true;
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
                return hasCommand(SET_TOP_BOX_COMMANDS.menu);
            }

            @Override
            public boolean exit() {
                return hasCommand(SET_TOP_BOX_COMMANDS.exit);
            }

            @Override
            public boolean enter() {
                return hasCommand(SET_TOP_BOX_COMMANDS.select) || hasCommand(SET_TOP_BOX_COMMANDS.enter);
            }

            @Override
            public boolean select() {
                return hasCommand(SET_TOP_BOX_COMMANDS.select) || hasCommand(SET_TOP_BOX_COMMANDS.enter);
            }

            @Override
            public boolean cabCompatible() {
                return false;
            }

            @Override
            public boolean canEditChannelName() {
                return !isSKNet;
            }

            @Override
            public boolean isSetTopBox() {
                return true;
            }

            @Override
            public boolean modifyChannelList() {
                return !isSKNet;
            }

            @Override
            public boolean autoRefresh() {
                return false;
            }

            @Override
            public boolean dash() { return hasCommand(SET_TOP_BOX_COMMANDS.dash); }

            @Override
            public boolean guide() { return hasCommand(SET_TOP_BOX_COMMANDS.guide); }

            @Override
            public boolean canSkipChannel() { return false; }

            @Override
            public boolean setup() { return hasCommand(SET_TOP_BOX_COMMANDS.setup); }

            @Override
            public boolean back() { return hasCommand(SET_TOP_BOX_COMMANDS.back); }

            @Override
            public boolean input() { return false; }

            @Override
            public boolean volume() { return false; }
        };
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            three();
        }
    };

    public SetTopBoxController(Context context) {
        this.context = context;


        currentChannelIndex = DataMatrix.getInt(TVKeys.DATA_STB_CURRENT_CHANNEL_INT);
        lastChannelIndex = DataMatrix.getInt(TVKeys.DATA_STB_PREVIOUS_CHANNEL_INT);

        // Load the set top box settings.
        reload();
        loadCurrentSetTopBox();
        loadChannels();

        context.registerReceiver(receiver, new IntentFilter("com.jht.direct.tv.test"));

    }


    /**
     * Loads all of the set top box settings.  We have to load them all to get the names for the
     * list in the Machine Settings page.  We search in the assets first and then in the files
     * directory.  Anything in the files directory overrides the assets.
     */
    public void reload() {

        List<String> setTopBoxLocations = new ArrayList<>();
        setTopBoxes.clear();

        // Find all of the set top box configurations in the assets.
        try {
            AssetManager assetManager = context.getAssets();
            String[] list = assetManager.list(STB_FILEPATH);
            setTopBoxLocations.addAll(Arrays.asList(list));
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        // Find all of the set top box configurations in the files directory.
        File file = new File(context.getFilesDir(), STB_FILEPATH);
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File folder : files) {
                if (folder.isDirectory() && !setTopBoxLocations.contains(folder.getName())) {
                    setTopBoxLocations.add(folder.getName());
                }
            }
        }

        // Load the set top box settings.
        for(String setTopBox : setTopBoxLocations) {
            SetTopBoxSetting setTopBoxSetting = loadSTB(setTopBox);
            if(setTopBoxSetting != null) {
                if(setTopBox.equalsIgnoreCase("enseo_3300_rs232")) {
                    enseoRS232 = setTopBoxSetting;
                }
                else {
                    setTopBoxes.add(setTopBoxSetting);
                }
            }
        }
    }

    public void reloadCurrent() {
        loadSetTopBox(loadSTB(currentSTB.getFolderName()));
    }

    protected void loadCurrentSetTopBox() {

        // Grab the current selection and open the com port.
        dataMatrix.createStringAdapter(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING, this::loadSetTopBox, false);
        loadSetTopBox(DataMatrix.getString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_SELECTION_STRING));
    }

    public boolean loadSetTopBox(String name) {
        for(SetTopBoxSetting stb : setTopBoxes) {
            if(stb.setTopBoxName.equalsIgnoreCase(name)) {
                loadSetTopBox(stb);
                return true;
            }
        }
        return false;
    }

    protected void loadSetTopBox(SetTopBoxSetting stb) {
        currentSTB = stb;
        if(stbComPortManager != null) {
            stbComPortManager.close();
        }
        stbComPortManager = new STBComPortManager(currentSTB);

        isSKNet = currentSTB.setTopBoxName.contains("SK NET");
        isLG6500RS232 = currentSTB.setTopBoxName.contains("LG 6500 RS232");

        if (currentSTB.useIR) {
            String lircPath = ProductConfig.portMappings.irLircPath();
            if (lircPath != null) {
                initLircDriver(lircPath);
            } else {
                DataMatrix.putInt(HALKeys.DATA_IO_SET_IR_FREQUENCY_KHZ_INT, currentSTB.frequencyKhz);
            }
        }
        Log.d(TAG, "loadSetTopBox: isSKNet = " + isSKNet + ", isLG6500RS232 = " + isLG6500RS232);
    }

    private void initLircDriver(String lircPath) {
        if (irDriver != null) {
            irDriver.close();
        }

        irDriver = new IrDriver(lircPath);
        if (irDriver.open()) {
            int frequency = (currentSTB != null && currentSTB.frequencyKhz >= 0) ? currentSTB.frequencyKhz * 1000 : IrDriver.IR_FREQUENCY_DEFAULT;
            boolean ok = irDriver.configure(frequency, IrDriver.IR_DUTY_DEFAULT, IrDriver.LIRC_MODE_PULSE);
            if (ok) {
                Log.d(TAG, "initLircDriver: Set IR parameters success via IrDriver, freq = " + frequency);
            } else {
                Log.e(TAG, "initLircDriver: Set IR parameters failed via IrDriver");
            }
        } else {
            Log.e(TAG, "initLircDriver: Failed to open IrDriver");
        }
    }

    @Nullable
    public SetTopBoxSetting loadFromAssets(@NonNull String name) {
        String path = STB_FILEPATH + "/" + name + "/" + STB_SETTING_FILENAME;
        File file = new File(context.getFilesDir(), path);
        JSONObject object = ResourceHelper.getJSONFromAsset(context, path);

        if (object != null) {
            return new SetTopBoxSetting(name, object);
        }
        return null;
    }

    public void saveSettings(SetTopBoxSetting settings) {

        String path = STB_FILEPATH + "/" + settings.getFolderName() + "/" + STB_SETTING_FILENAME;
        File file = new File(context.getFilesDir(), path);
        try {
            FileUtils.writeToFile(settings.toJSON().toString(4), file);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    /**
     * Load the set top box settings.  First look in the files directory and then the assets.
     * @param name  The set top box name.
     * @return      The set top box settings if we find them.  Null otherwise.  Should never return
     *              null.
     */
    @Nullable
    private SetTopBoxSetting loadSTB(@NonNull String name) {
        String path = STB_FILEPATH + "/" + name + "/" + STB_SETTING_FILENAME;
        File file = new File(context.getFilesDir(), path);
        JSONObject object;
        if(file.exists()) {

            object = ResourceHelper.loadJSONFromFile(file);
            if (object == null) {
                object = ResourceHelper.getJSONFromAsset(context, path);
            }
        } else {
            object = ResourceHelper.getJSONFromAsset(context, path);
        }

        if (object != null) {
            return new SetTopBoxSetting(name, object);
        }
        return null;

    }


    @Override
    public List<String> getSetTopBoxList() {
        List<String> names = new ArrayList<>();
        for(SetTopBoxSetting setTopBoxSetting : setTopBoxes) {
            names.add(setTopBoxSetting.setTopBoxName);
        }
        return names;
    }

    @Override
    public String getSelectedSetTopBox() {
        if (currentSTB != null)
            return currentSTB.setTopBoxName;
        return "";
    }

    @Override
    public List<ChannelInfo> channels() {
        return channels;
    }

    @Override
    public List<SKNETChannelInfo> sknetChannels() {
        return sknetChannels;
    }

    @Override
    public void zero() {
        executeCommand(SET_TOP_BOX_COMMANDS.zero);
    }

    @Override
    public void one() {
        executeCommand(SET_TOP_BOX_COMMANDS.one);
    }

    @Override
    public void two() {
        executeCommand(SET_TOP_BOX_COMMANDS.two);
    }

    @Override
    public void three() {
        executeCommand(SET_TOP_BOX_COMMANDS.three);
    }

    @Override
    public void four() {
        executeCommand(SET_TOP_BOX_COMMANDS.four);
    }

    @Override
    public void five() {
        executeCommand(SET_TOP_BOX_COMMANDS.five);
    }

    @Override
    public void six() {
        executeCommand(SET_TOP_BOX_COMMANDS.six);
    }

    @Override
    public void seven() {
        executeCommand(SET_TOP_BOX_COMMANDS.seven);
    }

    @Override
    public void eight() {
        executeCommand(SET_TOP_BOX_COMMANDS.eight);
    }

    @Override
    public void nine() {
        executeCommand(SET_TOP_BOX_COMMANDS.nine);
    }

    @Override
    public void dash() {
        executeCommand(SET_TOP_BOX_COMMANDS.dash);
    }

    @Override
    public void enter() {
        if(hasCommand(SET_TOP_BOX_COMMANDS.enter)) {
            executeCommand(SET_TOP_BOX_COMMANDS.enter);
        }
        else if(hasCommand(SET_TOP_BOX_COMMANDS.select)) {
            executeCommand(SET_TOP_BOX_COMMANDS.select);
        }
    }

    @Override
    public void select() {
        if(hasCommand(SET_TOP_BOX_COMMANDS.select)) {
            executeCommand(SET_TOP_BOX_COMMANDS.select);
        }
        else if(hasCommand(SET_TOP_BOX_COMMANDS.enter)) {
            executeCommand(SET_TOP_BOX_COMMANDS.enter);
        }
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
        else {
            executeCommand(SET_TOP_BOX_COMMANDS.channelup);
        }
    }


    /**
     * Set the current channel.
     * @param channelInfo  The current channel
     */
    @Override
    protected void setChannelImpl(ChannelInfo channelInfo) {
        super.setChannelImpl(channelInfo);

        // Do nothing.
        if (channelInfo == null || TextUtils.isEmpty(channelInfo.number())) {
            return;
        }

        lastChannelIndex = currentChannelIndex;
        currentChannelIndex = getIndexByChannelInfo(channelInfo);

        DataMatrix.putInt(TVKeys.DATA_STB_CURRENT_CHANNEL_INT, currentChannelIndex);
        DataMatrix.putInt(TVKeys.DATA_STB_PREVIOUS_CHANNEL_INT, lastChannelIndex);


        for (int i = 0; i < channelInfo.number().length(); i++) {
            char value = channelInfo.number().charAt(i);
            switch(value) {
                case '-':
                case '.':
                    executeCommand(SET_TOP_BOX_COMMANDS.dash);break;
                case '0': executeCommand(SET_TOP_BOX_COMMANDS.zero); break;
                case '1': executeCommand(SET_TOP_BOX_COMMANDS.one); break;
                case '2': executeCommand(SET_TOP_BOX_COMMANDS.two); break;
                case '3': executeCommand(SET_TOP_BOX_COMMANDS.three); break;
                case '4': executeCommand(SET_TOP_BOX_COMMANDS.four); break;
                case '5': executeCommand(SET_TOP_BOX_COMMANDS.five); break;
                case '6': executeCommand(SET_TOP_BOX_COMMANDS.six); break;
                case '7': executeCommand(SET_TOP_BOX_COMMANDS.seven); break;
                case '8': executeCommand(SET_TOP_BOX_COMMANDS.eight); break;
                case '9': executeCommand(SET_TOP_BOX_COMMANDS.nine); break;
            }

        }
        executeCommand(SET_TOP_BOX_COMMANDS.enter);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

    }

    /**
     * Set the current channel.
     * @param sknetChannelInfo  The current channel
     */
    @Override
    public void setSknetChannel(SKNETChannelInfo sknetChannelInfo) {
        // Do nothing.
        if (sknetChannelInfo == null || TextUtils.isEmpty(sknetChannelInfo.getNumber())) {
            return;
        }

        lastChannelIndex = currentChannelIndex;
        currentChannelIndex = getIndexBySknetChannelInfo(sknetChannelInfo);

        DataMatrix.putInt(TVKeys.DATA_STB_CURRENT_CHANNEL_INT, currentChannelIndex);
        DataMatrix.putInt(TVKeys.DATA_STB_PREVIOUS_CHANNEL_INT, lastChannelIndex);

        playService(sknetChannelInfo.getServiceId());
    }

    @Override
    public void channelDown() {

        if(channels.size() > 0) {
            if(currentChannelIndex > -1) {
                int nextChannel = currentChannelIndex == 0 ? channels.size() - 1 : currentChannelIndex - 1;
                setChannel(channels.get(nextChannel));
            }
        }
        else {
            executeCommand(SET_TOP_BOX_COMMANDS.channeldown);
        }
    }

    @Override
    public void lastChannel() {

        if(channels.size() > 0) {
            if (lastChannelIndex != currentChannelIndex) {
                setChannel(channels.get(lastChannelIndex));
            }
        }
        else {
            executeCommand(SET_TOP_BOX_COMMANDS.channellast);
        }
    }

    @Override
    public void toggleCC() {
        executeCommand(SET_TOP_BOX_COMMANDS.togglecc);
    }

    @Override
    public void toggleSAP() {
        executeCommand(SET_TOP_BOX_COMMANDS.togglesap);
    }

    @Override
    public void reset() {
        String defaultChannel;
        defaultChannel = DataMatrix.getString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING);
        enter();
        for(int i = 0; i < defaultChannel.length(); i++) {
            switch (defaultChannel.charAt(i)) {
                case '1': one(); break;
                case '2': two(); break;
                case '3': three(); break;
                case '4': four(); break;
                case '5': five(); break;
                case '6': six(); break;
                case '7': seven(); break;
                case '8': eight(); break;
                case '9': nine(); break;
                case '0': zero(); break;
                case '-': dash(); break;
                case '.': dash(); break;
            }
        }
        enter();
        currentChannelIndex = lastChannelIndex = DataMatrix.getInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT);
        DataMatrix.putInt(TVKeys.DATA_STB_CURRENT_CHANNEL_INT, currentChannelIndex);
        DataMatrix.putInt(TVKeys.DATA_STB_PREVIOUS_CHANNEL_INT, currentChannelIndex);
    }

    @Nullable
    @Override
    public ChannelInfo currentChannel() {
        if(channels.size() > 0 && currentChannelIndex >= 0 && currentChannelIndex < channels.size()) {
            return channels.get(currentChannelIndex);
        }
        return null;
    }


    @Override
    public void close() {
        executeCommand(SET_TOP_BOX_COMMANDS.exit);
    }

    @Override
    public void setDefaultChannel() {
        if(isSKNet) {
            int defaultChannelIndex = DataMatrix.getInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT);
            if(defaultChannelIndex < 0) return;
            if(sknetChannels().size() > 0 && defaultChannelIndex < sknetChannels().size()) {
                SKNETChannelInfo sknetChannelInfo = sknetChannels().get(defaultChannelIndex);
                setSknetChannel(sknetChannelInfo);
            }
        }
    }

    @Override
    public void red() {
        executeCommand(SET_TOP_BOX_COMMANDS.red);
    }

    @Override
    public void green() {
        executeCommand(SET_TOP_BOX_COMMANDS.green);
    }

    @Override
    public void yellow() {
        executeCommand(SET_TOP_BOX_COMMANDS.yellow);
    }

    @Override
    public void blue() {
        executeCommand(SET_TOP_BOX_COMMANDS.blue);
    }

    @Override
    public void info() {
        executeCommand(SET_TOP_BOX_COMMANDS.info);
    }

    @Override
    public void guide() {
        executeCommand(SET_TOP_BOX_COMMANDS.guide);
    }

    @Override
    public void back() {
        executeCommand(SET_TOP_BOX_COMMANDS.back);
    }

    @Override
    public void cornerButtonOperation(TVExtendedControls.CORNER_BUTTON_OPERATION operation) {
        switch (operation) {

            case MENU:
                executeCommand(SET_TOP_BOX_COMMANDS.menu);
                break;
            case SETUP:
                executeCommand(SET_TOP_BOX_COMMANDS.setup);
                break;
            case EXIT:
                executeCommand(SET_TOP_BOX_COMMANDS.exit);
                break;
            case BACK:
                executeCommand(SET_TOP_BOX_COMMANDS.back);
                break;
            case INFO:
                executeCommand(SET_TOP_BOX_COMMANDS.info);
                break;
            case CC:
                executeCommand(SET_TOP_BOX_COMMANDS.togglecc);
                break;
            case GUIDE:
                executeCommand(SET_TOP_BOX_COMMANDS.guide);
                break;
        }
    }

    @Override
    public void left() {
        executeCommand(SET_TOP_BOX_COMMANDS.left);
    }

    @Override
    public void right() {
        executeCommand(SET_TOP_BOX_COMMANDS.right);
    }

    @Override
    public void up() {
        executeCommand(SET_TOP_BOX_COMMANDS.up);
    }

    @Override
    public void down() {
        executeCommand(SET_TOP_BOX_COMMANDS.down);
    }

    @Override
    public String defaultChannelString() { return DataMatrix.getString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING); }

    @Override
    public int defaultChannelInt() { return DataMatrix.getInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT); }

    @Override
    public void defaultChannel(String channel) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING, channel);
        for(int i = 0; i < channels.size(); i++) {
            if(channels.get(i).number().equals(channel)) {
                DataMatrix.putInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT, i);
                break;
            }
        }
    }

    @Override
    public void defaultChannel(int channel) {
        DataMatrix.putInt(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_INT, channel);
        if(channel >= 0 && channel < channels.size()) {
            DataMatrix.putString(SettingKeys.TV.DATA_TV_SET_TOP_BOX_DEFAULT_CHANNEL_STRING, channels.get(channel).number());
        }

    }

    public static void importSTB(Context context, IOnProgress stbOnProgress) {
        new Thread(() -> {
            File setTopBoxFile = new File(StorageUtils.getUSBPath(), "stb.zip");
            File setTopBoxFileMD5 = new File(StorageUtils.getUSBPath(), "stb.md5");
            File flashPath = new File(context.getFilesDir(), STB_FILEPATH);
            String expectedMD5 = "";
            if(setTopBoxFile.exists() && setTopBoxFileMD5.exists()) {
                try(FileInputStream fin = new FileInputStream(setTopBoxFileMD5)) {
                    //noinspection CharsetObjectCanBeUsed
                    try(BufferedReader streamReader = new BufferedReader(new InputStreamReader(fin, "UTF-8"))) {
                        expectedMD5 = streamReader.readLine();
                    }
                } catch (FileNotFoundException ignored) {
                } catch (IOException ignored) {
                }
                String md5 = FileUtils.computeMd5(setTopBoxFile);
                if(expectedMD5.equals("") || md5.equals(expectedMD5)) {

                    FileUtils.delete(flashPath);
                    //noinspection ResultOfMethodCallIgnored
                    flashPath.mkdirs();
                    ZipFile zipFile;
                    try {
                        zipFile = new ZipFile(setTopBoxFile);
                        zipFile.extractAll(flashPath.getAbsolutePath());

                    } catch (ZipException ignored) {
                    }
                    ExecuteAsRoot.fixFilesInPackage(context.getPackageName());
                    stbOnProgress.onComplete(true, ATSC_STATUS.ACKOK);
                    return;
                }
            }
            stbOnProgress.onComplete(false, ATSC_STATUS.ACKOK);

        }).start();

    }


    /**
     * Import the channels from the USB and store it on the USB and tuner.
     */
    @Override
    public boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode) {
        new Thread(() -> {
            if(mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH_TO_TUNER || mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH) {
                File srcFile = new File(StorageUtils.getUSBPath(), channelFile());
                if (srcFile.exists()) {
                    File destFolder = new File(context.getFilesDir(), STB_FILEPATH);
                    File destFile = new File(destFolder, channelFile());
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
        File srcFolder = new File(context.getFilesDir(), STB_FILEPATH);
        File udisk = StorageUtils.getUSBPath();
        boolean retVal = FileUtils.copyFile(new File(srcFolder, channelFile()), new File(udisk, channelFile()));
        ExecuteAsRoot.runSyncCommand();
        return retVal;
    }


    /**
     * Import the channels from the USB and store it on the USB and tuner.
     */
    @Override
    public boolean exportChannels(IOnProgress onProgress) {
        if(isSKNet) {
            onSknetProgress = onProgress;
            startChannelScan();
            return true;
        }
        else {
            new Thread(() -> onProgress.onComplete(exportChannelsUSB(), ATSC_STATUS.ACKOK)).start();
            return false;
        }
    }

    protected void startTransaction() {
        stbComPortManager.startTransaction();
    }

    protected void waitForTransactionComplete(int timeout) {
        if(!stbComPortManager.waitForTransactionComplete(timeout)) {
            cancelOperation();
        }
    }

    protected void forceTransactionComplete() {
        stbComPortManager.forceTransactionComplete();
    }

    protected void executeCommand(SET_TOP_BOX_COMMANDS command) {
        if (irDriver != null && sendIRCommand(command)) {
            // send IR success
        } else {
            executeCommand(command, 1);
        }
    }

    protected void executeCommand(SET_TOP_BOX_COMMANDS command, int count) {
        if (currentSTB != null) {
            SetTopBoxSetting.RemoteButton remoteButton = findButton(command.name());
            if (remoteButton != null && stbComPortManager != null) {
                for (int i = 0; i < count; i++) {
                    if (!isSKNet)
                        stbComPortManager.sendCommand(remoteButton, currentSTB);
                }
            }
        }
    }

    @Nullable
    private SetTopBoxSetting.RemoteButton findButton(@NonNull String name) {
        if (currentSTB == null) return null;
        SetTopBoxSetting.RemoteButton button = currentSTB.remoteButtonMap.get(name);
        if (button != null) return button;
        for (Map.Entry<String, SetTopBoxSetting.RemoteButton> entry : currentSTB.remoteButtonMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    protected boolean sendIRCommand(SET_TOP_BOX_COMMANDS command) {
        if (currentSTB != null) {
            String cmdName = command.name();
            Log.d(TAG, "sendIRCommand: command = " + cmdName);
            SetTopBoxSetting.RemoteButton remoteButton = findButton(cmdName);
            int[] timings = getCombinedTimings(remoteButton);

            if (timings != null) {
                Log.d(TAG, "sendIRCommand: found data for " + cmdName + ", length = " + timings.length);
                sendIRSignal(timings);
                return true;
            } else {
                Log.w(TAG, "sendIRCommand: no data sequences for " + cmdName);
            }
        }
        return false;
    }

    @Nullable
    private int[] getCombinedTimings(@Nullable SetTopBoxSetting.RemoteButton remoteButton) {
        if (remoteButton == null || remoteButton.pulseSequences == null) return null;

        int[] combinedTimings = Arrays.stream(remoteButton.pulseSequences)
                .filter(seq -> seq != null && seq.sequence != null && seq.sequence.length > 0)
                .flatMapToInt(seq -> Arrays.stream(seq.sequence))
                .toArray();

        return combinedTimings.length > 0 ? combinedTimings : null;
    }

    private void sendIRSignal(int[] timings) {
        if (irDriver == null || !irDriver.isOpen()) {
            Log.e(TAG, "IrDriver is not valid or not open");
            return;
        }
        int repeats = currentSTB != null ? currentSTB.repeats : 0;
        irDriver.queueWrite(timings, repeats);
    }


    protected void poweron() {
        if(hasCommand(SET_TOP_BOX_COMMANDS.poweron)) {
            executeCommand(SET_TOP_BOX_COMMANDS.poweron);
        }
    }

    protected boolean hasCommand(SET_TOP_BOX_COMMANDS command) {
        return currentSTB != null && currentSTB.remoteButtonMap.get(command.name()) != null;
    }

    protected ChannelInfo parseChannel(XmlPullParser parser, int index) {
        return new ChannelInfo(channelType(),  new File(context.getFilesDir(), STB_FILEPATH).getAbsolutePath(), parser, index);
    }

    // FIXME - Unclosed Streams possible.
    @Override
    public void loadChannels() {
        if (isSKNet) {
            if (sknetChannels != null) {
                sknetChannels.clear();
                sknetChannels = null;
            }

            sknetChannels = new ArrayList<>();

            try {
                FileReader fr = new FileReader(SKNET_FILENAME);
                BufferedReader br = new BufferedReader(fr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();

                String contents = sb.toString();
                try {
                    JSONObject object = new JSONObject(contents);

                    int result = 0;

                    if (object.has("Result")) {
                        result = object.getInt("Result");
                        Log.d(TAG, "result = " + result);
                    }

                    if (result != ErrorGetChannels.OK.getValue())
                        return;

                    if (object.has("Type")) {
                        type = ReceivingRadio.values()[object.getInt("Type")];
                    }
                    
                    if (object.has("Channels")) {
                        JSONArray array = object.getJSONArray("Channels");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject objChannel = array.getJSONObject(i);
                            String frequency = String.valueOf(objChannel.getInt("Frequency"));
                            String tsId = String.valueOf(objChannel.getInt("TSID"));
                            String serviceId = String.valueOf(objChannel.getInt("ServiceID"));
                            String serviceName = objChannel.getString("ServiceName");
                            String channelNo = objChannel.getString("ChannelNo");
                            sknetChannels.add(new SKNETChannelInfo(frequency, tsId, serviceId, serviceName, channelNo, false));
                        }
                        DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_CHANNEL_LIST);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONObject construct failed.", e);
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "SKNET_channels.json is not exist.", e);
            } catch (IOException e) {
                Log.e(TAG, "readLine failed.", e);
            }
        } else {
            channels.clear();
            File file = new File(context.getFilesDir(), STB_FILEPATH + "/" + channelFile());
            if (file.exists()) {
                XmlPullParserFactory pullParserFactory;
                try {
                    pullParserFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = pullParserFactory.newPullParser();

                    try (InputStream inputStream = new FileInputStream(file)) {
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                        parser.setInput(inputStream, null);

                        int eventType = parser.getEventType();
                        int index = 1;

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            String name;
                            switch (eventType) {
                                case XmlPullParser.START_DOCUMENT:
                                    break;
                                case XmlPullParser.START_TAG:

                                    name = parser.getName();
                                    if (name.equals("Channel")) //NON-NLS NON-NLS
                                    {
                                        channels.add(parseChannel(parser, index));
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
            }
        }
    }

    @Override
    public void saveChannels(boolean toUsb) {
        File folder = new File(context.getFilesDir(), STB_FILEPATH);
        File file = new File(folder, channelFile());
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
    public ChannelInfo addChannel() {
        if(channels.size() > 0) {
            ChannelInfo lastChannel = channels.get(channels.size() - 1);
            int channelNumber = channels.size() + 1;
            ChannelInfo newChannel = new ChannelInfo(lastChannel);
            newChannel.number(channelNumber + "");
            channels.add(newChannel);
            return newChannel;
        }
        ChannelInfo channelInfo = new ChannelInfo(channelType(), "1", "", null);
        channels.add(channelInfo);
        saveChannels(false);
        setChannel(channelInfo);
        return channelInfo;
    }

    @Override
    public void deleteChannel(ChannelInfo channel) {
        int index = getIndexByChannelInfo(channel);
        if(index >= 0 && index < channels.size()) {
            channels.remove(index);
            saveChannels(false);
            int defaultChannelIndex = defaultChannelInt();
            if(defaultChannelIndex >= index && defaultChannelIndex > 0) {
                defaultChannel(defaultChannelIndex - 1);
            }
            if(currentChannelIndex >= index && channels.size() > 0) {
                if(currentChannelIndex > 0) {
                    setChannel(channels.get(currentChannelIndex - 1));
                }
            }
        }
    }

    @Override
    public void open() {
        if(stbComPortManager != null) {
            stbComPortManager.open();
        }
    }


    @Override
    public void exit() {
        executeCommand(SET_TOP_BOX_COMMANDS.exit);
    }

    @Override
    public void syncChannels() {

    }

    @Override
    public void muteOff() {

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

    public int getIndexBySknetChannelInfo(SKNETChannelInfo currentChannel) {
        if(currentChannel == null) {
            return currentChannelIndex;
        }
        for(int i = 0; i < sknetChannels.size(); i++) {
            if(currentChannel.equals(sknetChannels().get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void destroy() {
        if(stbComPortManager != null)
            stbComPortManager.close();
        if (irDriver != null) {
            irDriver.close();
            irDriver = null;
        }
        dataMatrix.finish();
        try {
            context.unregisterReceiver(receiver);
        }
        catch(Exception ignored) {}
    }


    @Override
    public ChannelInfo.ChannelType channelType() {
        return ChannelInfo.ChannelType.REGULAR_TV_USER_ENTERED;
    }

    private void loadSknetChannels() {
        if (sknetChannels != null) {
            sknetChannels.clear();
            sknetChannels = null;
        }

        sknetChannels = new ArrayList<>();

        try {
            FileReader fr = new FileReader(SKNET_FILENAME);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            parseResponse(SET_TOP_BOX_COMMANDS.CMD_GET_CHANNELS, sb.toString());
        } catch (FileNotFoundException e) {
            Log.d(TAG, "SKNET_channels.json is not exist.", e);
        } catch (IOException e) {
            Log.e(TAG, "readLine failed.", e);
        }
    }

    private void getCommand(final SetTopBoxController.SET_TOP_BOX_COMMANDS command, final @NonNls String request) {
        new Thread(() -> {
            synchronized(lock) {
                URL url;
                HttpURLConnection connection = null;

                Log.d(TAG, "getCommand run: command = " + command + ", request = " + request);

                try {
                    url = new URL(request);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(15000);
                    connection.connect();

                    int response = connection.getResponseCode();
                    switch(response) {
                        case HttpURLConnection.HTTP_OK:
                        case HttpURLConnection.HTTP_CREATED:
                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            br.close();
                            Log.d(TAG, "command: " + command + " response = " + sb.toString());
                            if (command == SET_TOP_BOX_COMMANDS.CMD_GET_CHANNELS) {
                                File folder = new File(context.getFilesDir(), sknet_channels_path);
                                //noinspection ResultOfMethodCallIgnored
                                folder.mkdirs();
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ignored) {
                                }
                                File file = new File(context.getFilesDir(), sknet_channels_path + sknet_channels_filename);
                                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                                BufferedWriter bw = new BufferedWriter(fw);
                                bw.write(sb.toString());
                                bw.close();
                                loadSknetChannels();
                            } else {
                                parseResponse(command, sb.toString());
                            }
                            break;
                        default:
                            break;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.d(TAG, "getCommand run: Exception 1 = " + e);
                    if(onSknetProgress != null)
                        onSknetProgress.onComplete(false, ATSC_STATUS.NETWORKERROR);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "getCommand run: Exception 2 = " + e);
                    if(onSknetProgress != null)
                        onSknetProgress.onComplete(false, ATSC_STATUS.NETWORKERROR);
                } finally {
                    if (connection != null) {
                        connection.disconnect();

                    }
                }
            }
        }).start();
    }

    /*
    private void getDeviceInfo() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_GET_DEVICE_INFO, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/get_device_info");
    }
     */

    private void playService(final String serviceId) {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_CHANNEL, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/play_service?type=" + type.getValue() + "&service_id=" + serviceId);
    }

    private void getChannelList() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_GET_CHANNELS, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/get_channels?type=" + type.getValue());
    }

    private void startChannelScan() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_SCAN, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/start_channel_scan?type=" + type.getValue());
    }

    private void getScanStatus() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_GET_SCAN_STATUS, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/get_scan_status");
    }

    private void stopChannelScan() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_STOP_SCAN, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/stop_channel_scan");
    }

    private void saveScanResult() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_SAVE_SCAN_RESULT, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/save_scan_result?update=1");
    }

    /*
    private void setAudioIndex(int index) {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_SET_AUDIO_INDEX, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/set_audio_index?index=" + index);
    }

    private void getCurrentStatus() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_GET_CURRENT_STATUS, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/get_current_status?type=" + type.getValue());
    }

    private void reboot() {
        getCommand(SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_REBOOT, "http://" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_ADDRESS_STRING) + ":" + DataMatrix.getString(SettingKeys.TV.DATA_TV_SKNET_TARGET_PORT_STRING) + "/reboot");
    }
     */

    private void parseResponse(SetTopBoxController.SET_TOP_BOX_COMMANDS command, String contents) {
        try {
            JSONObject object = new JSONObject(contents);

            int result = 0;

            if (object.has("Result")) {
                result = object.getInt("Result");
                Log.d(TAG, "result = " + result);
            }

            if (result != ErrorGetChannels.OK.getValue())
                return;

            if (command == SET_TOP_BOX_COMMANDS.CMD_STOP_SCAN && result == 0) {
                saveScanResult();
            } else if(command == SET_TOP_BOX_COMMANDS.CMD_SAVE_SCAN_RESULT && result == 0) {
                handler.postDelayed(this::getChannelList, 1000);
            } else if(command == SET_TOP_BOX_COMMANDS.CMD_GET_CHANNELS && result == 0) {
                handler.postDelayed(() -> {
                   if(onSknetProgress != null)
                       onSknetProgress.onComplete(true, ATSC_STATUS.EXPORTSUCCESS);
                }, 1000);
             }

            if (command == SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_SCAN) {
                handler.post(this::getScanStatus);
//				startTimer();
            } else if (command == SetTopBoxController.SET_TOP_BOX_COMMANDS.CMD_GET_DEVICE_INFO) {
                SKNETDeviceInfo deviceInfo = new SKNETDeviceInfo();
                deviceInfo.setModel(object.getString("Model"));
                deviceInfo.setVersion(object.getString("Version"));
                deviceInfo.setMac(object.getString("MAC"));
                deviceInfo.setBcas(object.getString("B-CAS"));
                deviceInfo.setMaintenanceMode(object.getInt("MaintenanceMode"));
// TODO
//                if (callback != null) {
//                    callback.onDeviceInfo(deviceInfo);
//                }
                return;
            }

            if (object.has("Type")) {
                type = ReceivingRadio.values()[object.getInt("Type")];
            }
            
            if (object.has("Progress")) {
                Log.d(TAG, "get scan status");
                int progress = object.getInt("Progress");
                Log.d(TAG, "parseResponse: progress = " + progress);
                if (progress < 100) {
                    handler.post(this::getScanStatus);
                } else {
                    if(onSknetProgress != null)
                        onSknetProgress.onComplete(true, ATSC_STATUS.EXPORTSUCCESS);
                    handler.postDelayed(this::stopChannelScan, 1000);
                }
                if(onSknetProgress != null)
                    onSknetProgress.onProgress(progress);
                return;
            }

            if (object.has("Channels")) {
                JSONArray array = object.getJSONArray("Channels");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject objChannel = array.getJSONObject(i);
                    String frequency = String.valueOf(objChannel.getInt("Frequency"));
                    String tsId = String.valueOf(objChannel.getInt("TSID"));
                    String serviceId = String.valueOf(objChannel.getInt("ServiceID"));
                    String serviceName = objChannel.getString("ServiceName");
                    String channelNo = objChannel.getString("ChannelNo");
                    sknetChannels.add(new SKNETChannelInfo(frequency, tsId, serviceId, serviceName, channelNo, false));
                }
                DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_CHANNEL_LIST);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONObject construct failed.", e);
        }
    }

    @Override
    public boolean sknetGetChannels(IOnProgress onProgress) {
        onSknetProgress = onProgress;
        getChannelList();
        return false;
    }



    @Override
    public List<HDMIResolution> supportedResolutions() {
        return null;
    }

    @Override
    public int splashResource() {
        return 0;
    }

    @Nullable
    @Override
    public SetTopBoxSetting getCurrentSTB() {
        if(currentSTB != null)
            return currentSTB;
        return null;
    }

    @Override
    public void executeIscProcess() {

    }

    @Override
    public void executeSTBCommand(SetTopBoxSetting.RemoteButton remoteButton) {
        if (currentSTB != null && remoteButton != null) {
            if (irDriver != null) {
                int[] timings = getCombinedTimings(remoteButton);
                if (timings != null) {
                    sendIRSignal(timings);
                    return;
                }
            }

            // If it is not Vortex or there is no IR data, use the original control method
            if (stbComPortManager != null) {
                Log.d(TAG, "executeSTBCommand: remoteButton.buttonName = " + remoteButton.buttonName);
                stbComPortManager.sendCommand(remoteButton, currentSTB);
            }
        }
    }
}
