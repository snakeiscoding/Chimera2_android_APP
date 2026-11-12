package com.jht.chimera.lib.tv.mst;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.jht.androidcommonalgorithms.resources.ResourceHelper;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.firsttimesetup.Channel;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.tv.TvEvent;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.SwiftChannelInfo;
import com.jht.chimera.lib.tv.switft.SwiftManager;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import org.jetbrains.annotations.NonNls;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MstController extends TVControllerBase {
    @NonNls private static final String TAG = "MstController";
//    private CABLib cabLib;
    @NonNls private static String CHANNEL_FILE_NAME = "/TVChannel.json";
    @NonNls private static String CUSTOMCHANNEL_FILE_NAME = "/TVChannelNames.txt";
    private DataMatrix mDataMatrix = new DataMatrix();
    private Context context;
    private int currentChannelIndex;
    private int lastChannelIndex;
    @NonNls private static final String CHANNEL_BIN_FILE = "/ATSC_channels.bin";
    @NonNls private static final String ATSC_US_FIRMWARE = "MSD3553T_8MB.bin";

    private static final int KEY_0 = 9;
    private static final int KEY_1 = 10;
    private static final int KEY_2 = 11;
    private static final int KEY_3 = 12;
    private static final int KEY_4 = 13;
    private static final int KEY_5 = 14;
    private static final int KEY_6 = 15;
    private static final int KEY_7 = 16;
    private static final int KEY_8 = 17;
    private static final int KEY_9 = 18;
    /*
    private static final int KEY_LAST = 19;
    private static final int KEY_CC = 20;
    private static final int KEY_VOL_UP = 21;
    private static final int KEY_VOL_DOWN = 22;
    private static final int KEY_CH_UP = 23;
    private static final int KEY_CH_DOWN = 24;
     */
    private static final int KEY_DASH = 25;
    private static final int KEY_ENTER = 26;

    private ArrayList<ChannelInfo> channels = new ArrayList<>();
    private ArrayList<ChannelInfo> existedChannels = new ArrayList<>();

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
                return false;
            }

            @Override
            public boolean lastChannel() {
                // Because MST tuner use last channel command to do closed caption function,
                // in Chimera there is no last channel function, so we hide last channel button.
                return false;
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
                return true;
            }

            @Override
            public boolean importChannelList() {
                return true;
            }

            @Override
            public boolean menu() {
                return true;
            }

            @Override
            public boolean exit() {
                return true;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public boolean select() {
                return true;
            }

            @Override
            public boolean cabCompatible() {
                return false;
            }

            @Override
            public boolean canEditChannelName() { return false; }

            @Override
            public boolean isSetTopBox() { return false; }

            @Override
            public boolean modifyChannelList() {
                return false;
            }

            @Override
            public boolean autoRefresh() {
                return false;
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
            public boolean volume() { return true; }

        };
    }

    public MstController(Context context) {
        this.context = context;
        loadChannels();
        mDataMatrix.createParcelableAdapter(TVKeys.EVENT_PARSE_CHANNELS_PARCELABLE, TvEvent.TvParcelable.class, tvParcelable -> {
            Log.d(TAG, "[cctest] MstController: tvParcelable.length = " + tvParcelable.length);
            parseData(tvParcelable.bytes, tvParcelable.length);
        });

        currentChannelIndex = DataMatrix.getInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT);
        lastChannelIndex = DataMatrix.getInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT);
    }

    @Override
    public List<ChannelInfo> channels() {
        return channels; // null;
    }

    private List<ChannelInfo> existedChannels() {
        return existedChannels;
    }

    @Override
    public void setChannel(ChannelInfo channnel) {
        lastChannelIndex = currentChannelIndex;
        currentChannelIndex = getIndexByChannelInfo(channnel);
        super.setChannel(channnel);
    }

    @Override
    protected void setChannelImpl(ChannelInfo channelInfo) {
        super.setChannelImpl(channelInfo);
        // Do nothing.
        if (TextUtils.isEmpty(channelInfo.number())) {
            return;
        }

        DataMatrix.putInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT, currentChannelIndex);
        DataMatrix.putInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT, lastChannelIndex);

//        SwiftManager.getInstance().setSwiftChannel(channelInfo.number());


        for (int i = 0; i < channelInfo.number().length(); i++) {
            char value = channelInfo.number().charAt(i);
            if (value == '-' || value == '.') {
                sendControl(KEY_DASH);
            } else {
                sendControl(Integer.parseInt(String.valueOf(value)) + KEY_0);
            }

            try {
                // Note 350 is needed otherwise the channel change during closed captioning will fail on Swift info 1.3.
                Thread.sleep(350);
            } catch (InterruptedException ignored) {
            }
        }


        sendControl(KEY_ENTER);

/*
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

 */
    }

    @Override
    public void zero() {
        SwiftManager.getInstance().setSwiftNumber0();
    }

    @Override
    public void one() {
        SwiftManager.getInstance().setSwiftNumber1();
    }

    @Override
    public void two() {
        SwiftManager.getInstance().setSwiftNumber2();
    }

    @Override
    public void three() {
        SwiftManager.getInstance().setSwiftNumber3();
    }

    @Override
    public void four() {
        SwiftManager.getInstance().setSwiftNumber4();
    }

    @Override
    public void five() {
        SwiftManager.getInstance().setSwiftNumber5();
    }

    @Override
    public void six() {
        SwiftManager.getInstance().setSwiftNumber6();
    }

    @Override
    public void seven() {
        SwiftManager.getInstance().setSwiftNumber7();
    }

    @Override
    public void eight() {
        SwiftManager.getInstance().setSwiftNumber8();
    }

    @Override
    public void nine() {
        SwiftManager.getInstance().setSwiftNumber9();
    }

    @Override
    public void dash() {
        SwiftManager.getInstance().setSwiftDash();
    }

    @Override
    public void enter() {
        SwiftManager.getInstance().setSwiftEnter();
    }

    @Override
    public void select() {
        SwiftManager.getInstance().setSwiftEnter();
    }

    @Override
    public void info() { SwiftManager.getInstance().setSwiftRed();  }

    @Override
    public void guide() {
    }

    @Override
    public void back() {
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
        } else {
            up();
        }
    }

    @Override
    public void channelDown() {
        if(channels.size() > 0) {
            if(currentChannelIndex > -1) {
                int nextChannel = currentChannelIndex == 0 ? channels.size() - 1 : currentChannelIndex - 1;
                setChannel(channels.get(nextChannel));
            }
        } else {
            down();
        }
    }

    @Override
    public void lastChannel() {
        if(channels.size() > 0) {
            if (lastChannelIndex != currentChannelIndex) {
                setChannel(channels.get(lastChannelIndex));
            }
        }
    }

    @Override
    public void toggleCC() {
        // Because MST tuner use last channel command to do closed caption function,
        // so we send last channel command when user press closed caption button.
        SwiftManager.getInstance().setSwiftReturn();
    }

    @Override
    public void toggleSAP() {
        SwiftManager.getInstance().setSwiftAudioChannelSwitch();
    }

    @Override
    public void reset() {
        Log.d(TAG, "[cctest] reset: ");
        int defaultChannel = DataMatrix.getInt(SettingKeys.TV.DATA_TV_MST_DEFAULT_CHANNEL_INT);
        if(defaultChannel >= 0 && channels.size() > 0 && defaultChannel < channels.size()) {
            setChannel(channels.get(defaultChannel));
            currentChannelIndex = DataMatrix.getInt(SettingKeys.TV.DATA_TV_MST_DEFAULT_CHANNEL_INT);
            lastChannelIndex = currentChannelIndex;
            DataMatrix.putInt(TVKeys.DATA_SWIFT_PREVIOUS_CHANNEL_INDEX_INT, lastChannelIndex);
            DataMatrix.putInt(TVKeys.DATA_SWIFT_CURRENT_CHANNEL_INDEX_INT, currentChannelIndex);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        SwiftManager.getInstance().setSwiftMuteOff();
//        SwiftManager.getInstance().getSwiftTunerType();
//        SwiftManager.getInstance().getSwiftManufacturer();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        SwiftManager.getInstance().getSwiftSoftwareVersion();
    }

    @Override
    public void setDefaultChannel() {
        Log.d(TAG, "[cctest] setDefaultChannel: ");
        int defaultChannel = DataMatrix.getInt(SettingKeys.TV.DATA_TV_MST_DEFAULT_CHANNEL_INT);
        if(channels.size() > 0 && defaultChannel < channels.size()) {
            setChannel(channels.get(defaultChannel));
        }
    }

    @Override
    public ChannelInfo currentChannel() {
        return currentChannelIndex >= 0 && currentChannelIndex < channels.size() ? channels.get(currentChannelIndex) : null;
    }

    @Override
    public boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode) {
        // NOTE: This function should copy over files from USB and put them in the settings folder.  It should then put
        // push the files to the tuner.  At the end call loadChannels to refresh the UI.

        // Make sure the swift folder exists.
        File swiftSettingsFolder = new File(context.getFilesDir(), "/settings/swift");
        //noinspection ResultOfMethodCallIgnored
        swiftSettingsFolder.mkdirs();

        if(mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH_TO_TUNER || mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH) {
            File usbPath = new File(StorageUtils.getUSBPath().getAbsolutePath());
            FileUtils.copyFile(new File(usbPath + CHANNEL_FILE_NAME), new File(swiftSettingsFolder, CHANNEL_FILE_NAME));
            FileUtils.copyFile(new File(usbPath + CUSTOMCHANNEL_FILE_NAME), new File(swiftSettingsFolder, CUSTOMCHANNEL_FILE_NAME));
            FileUtils.copyFile(new File(usbPath + CHANNEL_BIN_FILE), new File(swiftSettingsFolder, CHANNEL_BIN_FILE));
            ExecuteAsRoot.runSyncCommand();
            loadChannelNameFromUSB();
            loadChannels();
            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
        }

        if(mode == IMPORT_CHANNEL_MODE.USB_TO_FLASH_TO_TUNER || mode == IMPORT_CHANNEL_MODE.FLASH_TO_TUNER) {
            File tunerChannelFile = new File(swiftSettingsFolder, CHANNEL_BIN_FILE);
            if (tunerChannelFile.exists()) {
                SwiftManager.getInstance().importChannels(tunerChannelFile.getAbsolutePath(), onProgress);
                return true;
            }
        }

        onProgress.onComplete(true, ATSC_STATUS.IMPORTSUCCESS);
        return false;

    }

    @Override
    public void stopImport() {
        // TODO:
    }

    @Override
    public boolean exportChannelsUSB() {

        boolean success;
        File swiftSettingsFolder = new File(context.getFilesDir(), "/settings/swift");
        //noinspection ResultOfMethodCallIgnored
        swiftSettingsFolder.mkdirs();
        File usbPath = new File(StorageUtils.getUSBPath().getAbsolutePath());
        Log.d(TAG, "exportChannelsUSB: swiftSettingsFolder = " + swiftSettingsFolder.getPath() + ", usbPath = " + usbPath.getPath());
        success = FileUtils.copyFile(new File(swiftSettingsFolder, CHANNEL_FILE_NAME), new File(usbPath, CHANNEL_FILE_NAME));
        success &= FileUtils.copyFile(new File(swiftSettingsFolder, CUSTOMCHANNEL_FILE_NAME), new File(usbPath, CUSTOMCHANNEL_FILE_NAME));
        success &= FileUtils.copyFile(new File(swiftSettingsFolder, CHANNEL_BIN_FILE), new File(usbPath, CHANNEL_BIN_FILE));
        ExecuteAsRoot.runSyncCommand();
        return success;
    }

    @Override
    public boolean exportChannels(IOnProgress onProgress) {
        SwiftManager.getInstance().exportChannels(context.getFilesDir() + "/settings/swift/" + CHANNEL_BIN_FILE, onProgress); //NON-NLS
        return true;
    }

    @Override
    public void stopExport() {
        // TODO:
    }


    @Override
    public int defaultChannelInt() { return DataMatrix.getInt(SettingKeys.TV.DATA_TV_MST_DEFAULT_CHANNEL_INT); }


    @Override
    public void defaultChannel(int channel) {
        DataMatrix.putInt(SettingKeys.TV.DATA_TV_MST_DEFAULT_CHANNEL_INT, channel);
    }

    @Override
    public boolean updateFirmware(IOnProgress onProgress) {
        String fileName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + ATSC_US_FIRMWARE;
        SwiftManager.getInstance().updateFirmware(fileName, onProgress);
        return true;
    }

    @Override
    public void close() {
        Log.d(TAG, "[cctest] close: ");
        SwiftManager.getInstance().close();
        mDataMatrix.finish();
    }

    @Override
    public void open() {
        Log.d(TAG, "[cctest] open: ");
        SwiftManager.getInstance().open(false);

    }

    @Override
    public void reboot() {
        Log.d(TAG, "[cctest] reboot: ");
        SwiftManager.getInstance().reboot();
    }

    @Override
    public void cornerButtonOperation(TVExtendedControls.CORNER_BUTTON_OPERATION operation) {
        switch (operation) {
            case MENU:
                SwiftManager.getInstance().setSwiftMenu();
                break;
            case EXIT:
                SwiftManager.getInstance().setSwiftExit();
                break;
            case INFO:
                SwiftManager.getInstance().setSwiftRed();
                break;
            case CC:
                SwiftManager.getInstance().setSwiftClosedCaption();
                break;
        }
    }

    @Override
    public void left() { SwiftManager.getInstance().setSwiftVolumeDown(); }
    @Override
    public void right() { SwiftManager.getInstance().setSwiftVolumeUp(); }
    @Override
    public void up() { SwiftManager.getInstance().setSwiftChannelUp(); }
    @Override
    public void down() { SwiftManager.getInstance().setSwiftChannelDown();  }

    @Override
    public void red() {
        SwiftManager.getInstance().setSwiftRed();
    }
    @Override
    public void green() {
        SwiftManager.getInstance().setSwiftGreen();
    }
    @Override
    public void yellow() {
        SwiftManager.getInstance().setSwiftYellow();
    }
    @Override
    public void blue() {
        SwiftManager.getInstance().setSwiftBlue();
    }

    private void parseData(byte[] data, int length) {
        StringBuilder information = new StringBuilder();
        int mTotalChannelCount;
        ArrayList<SwiftChannelInfo> mChannelInfo = new ArrayList<>();

        mTotalChannelCount = data[12] * 256 + data[13];
        Log.w(TAG, "Parse(), get(12) = " + data[12] + ", get(13) = " + data[13] + ", mTotalChannelCount = " + mTotalChannelCount);
        byte[] majorChanNo = new byte[2];
        byte[] minorChanNo = new byte[2];
        byte[] chName = new byte[33];
        char serviceType = 0;
        char isFav = 0;
        char isSkip = 0;

        // Get each channel information
        int index = 0;
        int channelNameIndex = 0;
        int totalParsedChannelCount = 0;
        for (int i = 14; i < length; i++) {

            if (index == 0) {
                majorChanNo[0] = data[i];
            } else if (index == 1) {
                majorChanNo[1] = data[i];
            } else if (index == 2) {
                minorChanNo[0] = data[i];
            } else if (index == 3) {
                minorChanNo[1] = data[i];
            } else if (index >= 4 && index <= 36) {
                chName[channelNameIndex] = (data[i]);
                channelNameIndex++;
            } else if (index == 37) {
                serviceType = (char) (data[i]);
            } else if (index == 38){
                isFav = (char) (data[i]);
            }else if (index == 39){
                isSkip = (char) (data[i]);
            }

            index ++;

            if(index == 40)
            {
                // add parsed channel info
                SwiftChannelInfo channelInfo = new SwiftChannelInfo();
                channelInfo.setMajorChannelNo(majorChanNo);
                channelInfo.setMinorChannelNo(minorChanNo);
                channelInfo.setChannelName(chName);
                channelInfo.setServiceType(serviceType);
                channelInfo.setIsFav(isFav);
                channelInfo.setIsSkip(isSkip);

                mChannelInfo.add(channelInfo);

                // Clear all local parameters
                majorChanNo = new byte [2];
                minorChanNo = new byte [2];
                chName      = new byte [33];
                serviceType = 0;
                isFav = 0;
                isSkip = 0;

                Log.w(TAG, "Parse() AFTER mChannelInfo.size() = " + mChannelInfo.size());

                index = 0;
                channelNameIndex = 0;

                totalParsedChannelCount ++;

                // all channels are pasted
                if(totalParsedChannelCount >= mTotalChannelCount)
                    break;
            }
        }

        information.append(String.format(Locale.ENGLISH, "Total Channel Count = %d, mChannelInfo.size() = %d\n", mTotalChannelCount, mChannelInfo.size()));

        //Channel ch;
        ChannelInfo ch;

//        tvSettings.swiftTunerSettings.channelList.clear(true);
        channels().clear();

        for(int i=0; i<mChannelInfo.size(); i++){
            information.append("\n");
            information.append(String.format(Locale.ENGLISH, "Channel Number %d:\n", i + 1));
            information.append(String.format(Locale.ENGLISH, "majorChanNo = %s\n", Strings.bytesToHex(mChannelInfo.get(i).getMajorChanNo())));
            information.append(String.format(Locale.ENGLISH, "minorChanNo = %s\n", Strings.bytesToHex(mChannelInfo.get(i).getMinorChanNo())));
            information.append(String.format(Locale.ENGLISH, "chName = %s\n", String.valueOf(mChannelInfo.get(i).getChannelName())));
            information.append(String.format(Locale.ENGLISH, "serviceType = 0x%02X\n", (int) mChannelInfo.get(i).getServiceType()));
            information.append(String.format(Locale.ENGLISH, "isFav = 0x%02X\n", (int) mChannelInfo.get(i).getIsFav()));
            information.append(String.format(Locale.ENGLISH, "isSkip = 0x%02X\n", (int) mChannelInfo.get(i).getIsSkip()));

            //Log.w(TAG, "parseData: mChannelInfo.get(i).getMajorChanNo().toString() = " + mChannelInfo.get(i).getMajorChanNo().toString());

            ch = new ChannelInfo(ChannelInfo.ChannelType.REGULAR_TV, "", "", null);
            ch.numberReadOnly(true);
            int intMajorNumber = bytesToInt(mChannelInfo.get(i).getMajorChanNo());
            int intMinorNumber = bytesToInt(mChannelInfo.get(i).getMinorChanNo());
            String channelNumber;
            if(intMinorNumber == 0)
                channelNumber = String.format(Locale.ENGLISH, "%d", intMajorNumber);
            else
                channelNumber = String.format(Locale.ENGLISH, "%d-%d", intMajorNumber, intMinorNumber);

            Log.w(TAG, "parseData: intMajorNumber = " + intMajorNumber + ", intMinorNumber = " +intMinorNumber + ", channelNumber = " + channelNumber);

            ch.number(channelNumber);
            ch.majorNumber(Integer.toString(intMajorNumber));
            ch.minorNumber(Integer.toString(intMinorNumber));
            ch.name(String.valueOf(mChannelInfo.get(i).getChannelName()));
            ch.serviceType((int)mChannelInfo.get(i).getServiceType());
            ch.isFavorite((int)mChannelInfo.get(i).getIsFav());
            ch.isSkip((int)mChannelInfo.get(i).getIsSkip());

            ch.name(checkExistedChannelNames(ch));
            if(ch.isSkip() == 0)
                channels().add(ch);

        }

        Log.w(TAG, "Parse: result = " + information);

        Log.d(TAG, "ccctest parseData");

        saveChannels(true);

        mChannelInfo.size();
    }

    private static int bytesToInt(byte[] data) {
        if(data.length < 2) return 0;

        int intTmp = (unsignedByteToInt(data[1])
                + (unsignedByteToInt(data[0]) << 8));
        Log.w(TAG, "bytesToInt: intTmp = " + intTmp + ", data[0] = " + data[0] + ", data[1] = " + data[1] + ", datasize = " + data.length);

        return intTmp;
    }

    private static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }


    private String checkExistedChannelNames(ChannelInfo channel) {
        String originalChannelName = channel.name().replace("\0", "");
        String newChannelName = originalChannelName;
        if (existedChannels().size() > 0) {
            for (int i = 0; i < existedChannels().size(); i++) {
                Log.d(TAG, "[cctest] checkExistedChannelNames: i = " + i + ", existed number = " + existedChannels().get(i).number() + ", new number =" + channel.number());
                if (existedChannels().get(i).number().equals(channel.number())) {
                    // If the tuner name is blank then use the name you have already stored in the channel name file.
                    if (originalChannelName.isEmpty()) {
                        Log.d(TAG, "[cctest] checkExistedChannelNames: NULL, name = " + existedChannels().get(i).name());
                        newChannelName = existedChannels().get(i).name();
                    }
                    // If the tuner name is not blank then override what you have.
                    else {
                        Log.d(TAG, "[cctest] checkExistedChannelNames: NOT NULL, name = " + originalChannelName);
                        newChannelName = originalChannelName;
                    }
                } else {
                    newChannelName = originalChannelName;
                }
            }
        }
        return newChannelName;
    }

    @Override
    public synchronized void saveChannels(boolean toUsb) {

        File fileFolder = new File(context.getFilesDir(), "/settings/swift");
        File file = new File(fileFolder, CHANNEL_FILE_NAME);

        try {
            //noinspection ResultOfMethodCallIgnored
            fileFolder.mkdirs();

            JSONObject swiftInfo = new JSONObject();
            //Add channels to list
            JSONArray channelList = new JSONArray();
            swiftInfo.put("channels", channelList);

            for(ChannelInfo channel : channels()) {
                channelList.put(channel.toJSON());

                if(channel.logo() != null) {
                    try(FileOutputStream out = new FileOutputStream(new File(fileFolder, channel.number() + "_" + channel.name() + ".png"))) {
                        channel.logo().compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    catch (IOException ignored) {}
                }
            }
            try (FileWriter fileWriter = new FileWriter(file)) {

                fileWriter.write(swiftInfo.toString(4));
                fileWriter.flush();

            }
            catch (IOException ignored) {}

        } catch (JSONException ignored) {
        }

        // Save custom names.
        File f = new File(context.getFilesDir(), "/settings/swift/" + CUSTOMCHANNEL_FILE_NAME);
        try(FileOutputStream fout = new FileOutputStream(f)) {
            for(ChannelInfo channel : channels) {
                String channelName = channel.name().replace("\0", "");
                fout.write(channelName.getBytes() /*channel.name().getBytes()*/);
                fout.write("\n".getBytes());
                new Handler().postDelayed(this::refreshExistedChannelNames, 1000);
            }
        } catch (FileNotFoundException ignored) {
        } catch (IOException ignored) {
        }
        ExecuteAsRoot.runSyncCommand();

        if(toUsb) {
            FileUtils.copyFile(new File(context.getFilesDir(), "/settings/swift/" + CHANNEL_FILE_NAME), new File(StorageUtils.getUSBPath().getAbsolutePath(), CHANNEL_FILE_NAME));
            FileUtils.copyFile(new File(context.getFilesDir(), "/settings/swift/" + CUSTOMCHANNEL_FILE_NAME), new File(StorageUtils.getUSBPath().getAbsolutePath(), CUSTOMCHANNEL_FILE_NAME));
            FileUtils.copyFile(new File(context.getFilesDir(), "/settings/swift/" + CHANNEL_BIN_FILE), new File(StorageUtils.getUSBPath().getAbsolutePath(), CHANNEL_BIN_FILE));
            ExecuteAsRoot.runSyncCommand();
        }
    }

    @Override
    public void loadChannels() {

        File fileFolder = new File(context.getFilesDir(), "/settings/swift");
        File file = new File(fileFolder, CHANNEL_FILE_NAME);
        if(file.exists()) {
            channels().clear();
            existedChannels().clear();
            JSONObject channels = ResourceHelper.loadJSONFromFile(file);
            try {
                if(channels != null) {
                    JSONArray channelList = channels.getJSONArray("channels");
                    for (int i = 0; channelList != null && i < channelList.length(); i++) {

                        JSONObject channel = channelList.getJSONObject(i);
                        if (channel != null) {
                            ChannelInfo info = new ChannelInfo(ChannelInfo.ChannelType.REGULAR_TV, channel);
                            info.numberReadOnly(true);
                            File logoFile = new File(fileFolder, info.number() + "_" + info.name() + ".png");
                            if (logoFile.exists()) {
                                info.logo(BitmapFactory.decodeFile(logoFile.getAbsolutePath()));
                            }
                            channels().add(info);
                            existedChannels().add(info);
                        }
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        loadChannelNames();


    }

    private void loadChannelNames() {
        File file = new File(context.getFilesDir(), "/settings/swift/" + CUSTOMCHANNEL_FILE_NAME);
        if(file.exists()) {
            try(FileInputStream fin = new FileInputStream(file)) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(fin))) {
                    for(ChannelInfo channel : channels) {
                        String line = reader.readLine();
                        if(line == null) { break; }
                        channel.name(line);
                    }
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException ignored) {
            }

            refreshExistedChannelNames();
        }
    }

    private void refreshExistedChannelNames() {
        File file = new File(context.getFilesDir(), "/settings/swift/" + CUSTOMCHANNEL_FILE_NAME);
        if(file.exists()) {
            try(FileInputStream fin = new FileInputStream(file)) {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(fin))) {
                    for(ChannelInfo channel : existedChannels) {
                        String line = reader.readLine();
                        if(line == null) { break; }
                        channel.name(line);
                    }
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException ignored) {
            }
        }
    }

    private boolean loadChannelNameFromUSB() {
        Log.d(TAG, "[cctest] loadChannelNameFromUSB: ");

        if(!FileUtils.copyFile(new File(StorageUtils.getUSBPath().getAbsolutePath(), CUSTOMCHANNEL_FILE_NAME), new File(context.getFilesDir(), "/settings/swift/" + CUSTOMCHANNEL_FILE_NAME))) {
            return false;
        }
        ExecuteAsRoot.runSyncCommand();
        return true;
    }

    @Override
    public void syncChannels() {
        Log.d(TAG, "[cctest] syncChannels: ");
        if(!loadChannelNameFromUSB()) {
            // if source file is not existed, return and show sync failed message
            Log.d(TAG, "syncChannels: source file is not existed!");
            return;
        }
        loadChannels();
    }

    @Override
    public void muteOff() {
        SwiftManager.getInstance().setSwiftMuteOff();
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

    private void sendControl(int id) {
        try {
            switch (id) {
                case KEY_DASH:
                    dash();
                    break;
                case KEY_ENTER:
                    enter();
                    break;
                case KEY_0:
                    zero();
                    break;
                case KEY_1:
                    one();
                    break;
                case KEY_2:
                    two();
                    break;
                case KEY_3:
                    three();
                    break;
                case KEY_4:
                    four();
                    break;
                case KEY_5:
                    five();
                    break;
                case KEY_6:
                    six();
                    break;
                case KEY_7:
                    seven();
                    break;
                case KEY_8:
                    eight();
                    break;
                case KEY_9:
                    nine();
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void exit() {
        SwiftManager.getInstance().setSwiftExit();
    }

    @Override
    public int getIndexByChannelInfo(ChannelInfo channelInfo) {
        if(channelInfo == null) {
            return currentChannelIndex;
        }

        for(int i = 0; i < channels().size(); i++)
        {
            if(channelInfo.equals(channels().get(i)))
                return i;
        }
        return -1;
    }


    @Override
    public void destroy() {
        Log.d(TAG, "[cctest] destroy: ");
        SwiftManager.getInstance().close();
        mDataMatrix.finish();
        super.destroy();
    }

    @Override
    public void setTVVolume(int volume) {
        SwiftManager.getInstance().setSwiftVolume(volume);
    }

}
