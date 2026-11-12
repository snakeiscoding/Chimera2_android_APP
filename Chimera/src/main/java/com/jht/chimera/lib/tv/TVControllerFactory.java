package com.jht.chimera.lib.tv;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.iptv.IPTVController;
import com.jht.chimera.lib.tv.mst.MstController;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxController;
import com.jht.chimera.lib.tv.settopbox.SetTopBoxSetting;
import com.jht.chimera.lib.tv.switft.SwiftController;
import com.jht.chimera.lib.tv.vivid.VividController;
import com.jht.chimera.lib.widget.tv.TVExtendedControls;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

import java.util.ArrayList;
import java.util.List;

public class TVControllerFactory {
    private static final String TAG = "TVControllerFactory";
    private static TVControllerBase testController = new TVControllerBase() {
        private ArrayList<ChannelInfo> channels = new ArrayList<ChannelInfo>() {
            {
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 1", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 2", "rtsp://192.168.213.233:8554/chimera", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 4", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 5", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 6", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 7", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 8", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
                add(new ChannelInfo(ChannelInfo.ChannelType.UNENCRYPTED_IPTV, "Testing 9", "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov", null)); //NON-NLS
            }
        };

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
        public int getIndexByChannelInfo(ChannelInfo currentChannel) {
            return 0;
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
                public boolean guide() {
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
                public boolean dash() {
                    return true;
                }

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

        @Override
        public List<ChannelInfo> channels() {
            return channels;
        }


        @Override
        public void zero() {
            Log.d(TAG, "Pressed 0");
        }

        @Override
        public void one() {
            Log.d(TAG, "Pressed 1");
        }

        @Override
        public void two() {
            Log.d(TAG, "Pressed 2");
        }

        @Override
        public void three() {
            Log.d(TAG, "Pressed 3");
        }

        @Override
        public void four() {
            Log.d(TAG, "Pressed 4");
        }

        @Override
        public void five() {
            Log.d(TAG, "Pressed 5");
        }

        @Override
        public void six() {
            Log.d(TAG, "Pressed 6");
        }

        @Override
        public void seven() {
            Log.d(TAG, "Pressed 7");
        }

        @Override
        public void eight() {
            Log.d(TAG, "Pressed 8");
        }

        @Override
        public void nine() {
            Log.d(TAG, "Pressed 9");
        }

        @Override
        public void dash() {
            Log.d(TAG, "Pressed Dash");
        }

        @Override
        public void enter() {
            Log.d(TAG, "Pressed Enter");
        }

        @Override
        public void select() {
            Log.d(TAG, "Pressed select");
        }

        @Override
        public void info() {

        }

        @Override
        public void guide() {

        }

        @Override
        public void back() {

        }

        @Override
        public void red() {

        }

        @Override
        public void blue() {

        }

        @Override
        public void yellow() {

        }

        @Override
        public void green() {

        }

        @Override
        public void channelUp() {
            Log.d(TAG, "Pressed Channel Up");
        }

        @Override
        public void channelDown() {
            Log.d(TAG, "Pressed Channel Down");
        }

        @Override
        public void lastChannel() {
            Log.d(TAG, "Pressed Last Channel");
        }

        @Override
        public void toggleCC() {
            Log.d(TAG, "Pressed CC");
        }

        @Override
        public void toggleSAP() {
            Log.d(TAG, "Pressed SAP");
        }

        @Override
        public void reset() {
            Log.d(TAG, "Reset");
        }

        @Override
        public ChannelInfo currentChannel() {
            return channels.get(0);
        }

        @Override
        public boolean importChannels(IOnProgress onProgress, IMPORT_CHANNEL_MODE mode) {
            return false;
        }

        @Override
        public void open() { Log.d(TAG, "Open"); }


        @Override
        public void cornerButtonOperation(TVExtendedControls.CORNER_BUTTON_OPERATION operation) {

        }

        @Override
        public void left() {
            Log.d(TAG, "left");
        }

        @Override
        public void right() {
            Log.d(TAG, "right");
        }

        @Override
        public void up() {
            Log.d(TAG, "up");
        }

        @Override
        public void down() {
            Log.d(TAG, "down");
        }

        @Override
        public void close() {Log.d(TAG, "close");}

        @Override
        public void setDefaultChannel() {

        }

    };
    static public @NonNull TVControllerBase getTVController(Context context, SettingKeys.TVSource tvSource) {
        Log.d(TAG, "getTVController: tvSource = " + tvSource.toString());
        switch (tvSource) {
            case US5:
            case US6:
    	    case US7:
            case BR2:
                return new SwiftController(context);
            case KM4:
                return new VividController();
            case JP0:
                return new MstController(context);
            case IPTV:
                return new IPTVController(context);
            case STB:
                return new SetTopBoxController(context);
        }
        return testController;
    }
}
