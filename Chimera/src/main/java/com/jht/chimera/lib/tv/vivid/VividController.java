package com.jht.chimera.lib.tv.vivid;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.jht.androidcommonalgorithms.util.CmdProcessHelper;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.SwiftManager;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VividController extends TVControllerBase {
    @NonNls private static final String TAG = "VividController";
    private List<ChannelInfo> channels = new ArrayList<>();

    @NonNls private static final String PATH_TO_KM4 = "/data/local/km4";
    @NonNls private static final String FIRMWARENAME = "MERGE.bin";
    @NonNls private static final String TUNERNAME = "km4";
    @NonNls private static final String CHANNELLISTNAME = "CUS_DATABASE.bin";
    @NonNls private static final String PATH_TO_USB_DISK = StorageUtils.getUdiskPath();
    @NonNls private static final String VIVID_MOUNT_PATH = ProductConfig.portMappings.vividMetricsPath();

    private static final int KEY_UP = 1;
    private static final int KEY_DOWN = 2;
    private static final int KEY_LEFT = 3;
    private static final int KEY_RIGHT = 4;
    private static final int KEY_OK = 5;
    private static final int KEY_MENU = 6;
    private static final int KEY_SCAN = 7;
    private static final int KEY_EXIT = 8;
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
    private static final int KEY_LAST = 19;
    private static final int KEY_CC = 20;
    private static final int KEY_VOL_UP = 21;
    private static final int KEY_VOL_DOWN = 22;
    private static final int KEY_CH_UP = 23;
    private static final int KEY_CH_DOWN = 24;
    private static final int KEY_DASH = 25;
    private static final int KEY_ENTER = 26;

    @Override
    public void exit() {
        SwiftManager.getInstance().setSwiftExit();
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
    public boolean sknetGetChannels(IOnProgress onProgress) {
        return false;
    }

    @Override
    public int getIndexByChannelInfo(ChannelInfo currentChannel) {
        return 0;
    }


    @Override
    public ITunerCapabilities capabilities() {
        return new ITunerCapabilities() {
            @Override
            public boolean channelList() {
                return false;
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
                return true;
            }

            @Override
            public boolean info() {
                return true;
            }

            @Override
            public boolean colorButtons() {
                return true;
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
            public boolean cabCompatible() { return true; }

            @Override
            public boolean canEditChannelName() { return true; }

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

    public VividController() {
    }

    @Override
    public List<ChannelInfo> channels() {
        return channels;
    }

    @Override
    protected void setChannelImpl(ChannelInfo channelInfo) {
        super.setChannelImpl(channelInfo);
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
    public void channelUp() {
        SwiftManager.getInstance().setSwiftChannelUp();
    }

    @Override
    public void channelDown() {
        SwiftManager.getInstance().setSwiftChannelDown();
    }

    @Override
    public void lastChannel() {
        SwiftManager.getInstance().setSwiftReturn();
    }

    @Override
    public void toggleCC() {
        SwiftManager.getInstance().setSwiftClosedCaption();
    }

    @Override
    public void toggleSAP() {
    }

    @Override
    public void reset() {

        String defaultChannel = DataMatrix.getString(SettingKeys.TV.DATA_TV_VIVID_DEFAULT_CHANNEL_STRING);
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
    }

    @Override
    public ChannelInfo currentChannel() {
        return null;
    }

    @Override
    public boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode) {

        Log.d(TAG, "[km4] importChannels:");

        new Thread(() -> {

            File file = new File (PATH_TO_USB_DISK + "/" + TUNERNAME + "/" + CHANNELLISTNAME);

            if (file.exists()) {

                SwiftManager.getInstance().getSwiftUSBHandler();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }

                ExecuteAsRoot cmds = new ExecuteAsRoot();

                cmds.addCommand("mkdir " + PATH_TO_KM4);
                cmds.addCommand("mount -t vfat -o rw " + VIVID_MOUNT_PATH + " " + PATH_TO_KM4);
                cmds.addCommand("sleep 3");
                cmds.addCommand("cp -rf " + PATH_TO_USB_DISK + "/" + TUNERNAME + "/" + CHANNELLISTNAME + " " + PATH_TO_KM4 + "/");
                cmds.addCommand("sleep 3");
                cmds.addCommand("umount " + PATH_TO_KM4);
                cmds.execute();

                onProgress.onComplete(true, ATSC_STATUS.ACKOK);

            } else {
                onProgress.onComplete(false, ATSC_STATUS.FILENOTEXISTED);
            }

        }).start();
        return false;
    }

    @Override
    public void stopImport() {

    }


    @Override
    public boolean exportChannelsUSB() {
        return true;
    }

    @Override
    public boolean exportChannels(IOnProgress onProgress) {

        Log.d(TAG, "[km4] exportChannels:");

        new Thread(() -> {

            if (checkUsbDisk(true)) {

                SwiftManager.getInstance().getSwiftUSBHandler();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }

                ExecuteAsRoot cmds = new ExecuteAsRoot();

                cmds.addCommand("mkdir " + PATH_TO_KM4);
                cmds.addCommand("mount -t vfat -o rw " + VIVID_MOUNT_PATH + " " + PATH_TO_KM4);
                cmds.addCommand("sleep 1");
                cmds.addCommand("cp -rf " + PATH_TO_KM4 + "/" + CHANNELLISTNAME + " " + PATH_TO_USB_DISK + "/" + TUNERNAME + "/");
                cmds.addCommand("sleep 2");
                cmds.addCommand("sync");
                cmds.addCommand("sleep 1");
                cmds.addCommand("umount " + PATH_TO_KM4);
                cmds.addCommand("sleep 1");
                Log.d(TAG, "[km4] exportChannels: cmds1.execute();");
                cmds.execute();
            }

        }).start();

        new Handler().postDelayed(() -> {
            File dest = new File(PATH_TO_USB_DISK,TUNERNAME + "/" + CHANNELLISTNAME);
            Log.d(TAG, "[km4] run: File = " + dest.getPath());
            if(dest.exists()) {
                Log.d(TAG, "[km4] run: File length = " + dest.length());
                if(dest.length() > 0)
                    onProgress.onComplete(true, ATSC_STATUS.ACKOK);
                else
                    onProgress.onComplete(false, ATSC_STATUS.EXPORTFAIL);
            } else {
                onProgress.onComplete(false, ATSC_STATUS.EXPORTFAIL);
            }
        }, 10000);

        return false;
    }

    @Override
    public void stopExport() {

    }


    @Override
    public boolean updateFirmware(IOnProgress onProgress) {

        Log.d(TAG, "[km4] updateFirmware:");

        new Thread(() -> {

            File file = new File (PATH_TO_USB_DISK + "/" + TUNERNAME + "/" + FIRMWARENAME);

            if (file.exists()) {

                SwiftManager.getInstance().getSwiftUSBHandler();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }

                ExecuteAsRoot cmds = new ExecuteAsRoot();

                cmds.addCommand("mkdir " + PATH_TO_KM4);
                cmds.addCommand("mount -t vfat -o rw " + VIVID_MOUNT_PATH + " " + PATH_TO_KM4);
                cmds.addCommand("sleep 3");
                cmds.addCommand("rm -rf " + PATH_TO_KM4 + "/" + FIRMWARENAME);
                cmds.addCommand("sleep 1");
                cmds.addCommand("cp -rf " + PATH_TO_USB_DISK + "/" + TUNERNAME + "/" + FIRMWARENAME + " /data/local/km4");
                cmds.addCommand("sleep 2");
                cmds.addCommand("sync");
                cmds.addCommand("sleep 1");
                cmds.addCommand("umount " + PATH_TO_KM4);

                cmds.execute();

                onProgress.onComplete(true, ATSC_STATUS.ACKOK);

            } else {
                onProgress.onComplete(false, ATSC_STATUS.FILENOTEXISTED);
            }

        }).start();
        return false;
    }

    @Override
    public void close() {
        SwiftManager.getInstance().close();
    }

    @Override
    public void setDefaultChannel() {
        String defaultChannel = DataMatrix.getString(SettingKeys.TV.DATA_TV_VIVID_DEFAULT_CHANNEL_STRING);
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
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignore) {
            }
        }
        enter();
    }

    @Override
    public void red() {
        // Do nothing.
        SwiftManager.getInstance().setSwiftRed();
    }

    @Override
    public void green() {
        // Do nothing.
        SwiftManager.getInstance().setSwiftGreen();
    }

    @Override
    public void yellow() {
        // Do nothing.
        SwiftManager.getInstance().setSwiftYellow();
    }

    @Override
    public void blue() {
        // Do nothing.
        SwiftManager.getInstance().setSwiftBlue();
    }

    @Override
    public void info() {
        // Do nothing.
        SwiftManager.getInstance().setSwiftRed();
    }

    @Override
    public void guide() {

    }

    @Override
    public void back() {

    }

    @Override
    public void left() {
        SwiftManager.getInstance().setSwiftVolumeDown();
    }

    @Override
    public void right() {
        SwiftManager.getInstance().setSwiftVolumeUp();
    }

    @Override
    public void up() {
        SwiftManager.getInstance().setSwiftChannelUp();
    }

    @Override
    public void down() {
        SwiftManager.getInstance().setSwiftChannelDown();
    }


    @Override
    public String defaultChannelString() { return DataMatrix.getString(SettingKeys.TV.DATA_TV_VIVID_DEFAULT_CHANNEL_STRING); }


    @Override
    public void defaultChannel(String channel) {
        DataMatrix.putString(SettingKeys.TV.DATA_TV_VIVID_DEFAULT_CHANNEL_STRING, channel);
    }

    @Override
    public void open() {
        SwiftManager.getInstance().open(false);
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

    private void sendControl(int id) {

        Log.d(TAG, "[channel play] sendControl: id = " + id);
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

    private boolean checkUsbDisk(boolean create) {
        File dest = new File(PATH_TO_USB_DISK + "/" + TUNERNAME);

        Log.d(TAG, "[km4] checkUsbDisk: create = " + create);
        
        if (!dest.exists()) {
            Log.d(TAG, "[km4] checkUsbDisk: gate 1");
            if (create) {
                CmdProcessHelper cmd = new CmdProcessHelper();
                cmd.addCommand("mkdir " + PATH_TO_USB_DISK + "/" + TUNERNAME); //NON-NLS
                cmd.execute();
                Log.d(TAG, "[km4] checkUsbDisk: gate 2, cmd = " + cmd.toString());

                return checkUsbDisk(false);
            } else {
                Log.d(TAG, "[km4] checkUsbDisk: gate 3 return false");

                return false;
            }
        }
        Log.d(TAG, "[km4] checkUsbDisk: gate 4 return true");

        return true;
    }

    @Override
    public void destroy() {
        SwiftManager.getInstance().close();
    }

    @Override
    public void setTVVolume(int volume) {
        SwiftManager.getInstance().setSwiftVolume(volume);
    }

    @Override
    public void executeIscProcess() {
        Log.d(TAG, "[km4] executeIscProcess: send ISC command");
        SwiftManager.getInstance().setSwiftInitializeScanChannel();
        SystemClock.sleep(12000);

        Log.d(TAG, "[km4] executeIscProcess: after send ISC command, start to control OSD");
        SwiftManager.getInstance().setSwiftVolumeUp();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftChannelDown();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftChannelDown();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftEnter();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftEnter();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftChannelDown();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftVolumeUp();
        SystemClock.sleep(2000);
        SwiftManager.getInstance().setSwiftEnter();
        SystemClock.sleep(2000);

        Log.d(TAG, "[km4] executeIscProcess: after control OSD, start to scan channel and wait 10 minutes for completing scan");
        SystemClock.sleep(600000);
        Log.d(TAG, "[km4] executeIscProcess: scan complete, send left to complete all ISC process");
        SwiftManager.getInstance().setSwiftVolumeDown();

        SystemClock.sleep(2000);
    }
}
