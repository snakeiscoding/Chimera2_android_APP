package com.jht.chimera.lib.tv;

import android.graphics.Bitmap;

public class TVSettings {

    // GeneralTunerSettings 2???? swift and vivid
    public Channel[] channelList2;
    public String defaultChannel;
    public String firmwareVersion;
    public int currentAspectRatio; // enum: fill, auto
    public int overScanSize;
    public int exportPercentage;
    public int importPercentage;
    public int updatePercentage;
    public ATSC_STATUS_TODO atscStatus;

    // IPTVSettings
    public IPTVChannel[] channelList;
    public String defaultChannel4;
    public int interfaceMetric;
    public boolean useSapChannelList;

    // SetTopBoxSettings
    public String[] setTopBoxList;
    public String currentSetTopBox;

    // CABSettings
    public String defaultChannel3;
    public int currentAspectRatio2; // enum: fill, auto
    public int overScanSize2;

    //RemoteTVSettings
    public boolean enabled;
    public int defaultChannel2;
    public int numChannels;
    public int currentChannel;

    public boolean needSetDefaultChannel;
    public boolean needRecreateHDMI;
    public boolean needSendMuteOff;
    public int currentPosition;
    public int lastPosition;
    /*
public TVSourceField currentTVSource;
    public GeneralTunerSettings swiftTunerSettings;
    public GeneralTunerSettings vividTunerSettings;
    public IPTVSettings ipTVSettings;
    public SetTopBoxSettings setTopBoxSettings;
    public CABSettings cabSettings; // default channel
    public RemoteTVSettings remoteTVSettings; // default channel, num channels
    public boolean needSetDefaultChannel;
    public boolean needRecreateHdmi;
    public boolean needSendMuteOff;
    public int currentPosition;
    public int lastPosition;
     */
    class IPTVChannel  {

        public int channel;
        public String name;
        public String protocol;
        public String address;
        public String port;
        public String path;
        public String ip;
        public String sourceType;

        public String description;
        public Bitmap icon;
        public boolean enabled;
        public boolean isDefault;
        public boolean deinterlace;
    }
    
    class Channel {

        public String channelNumber;
        public String channelName;
        public Bitmap logo;
        public double signalStrength;
        public String sourceType; // "digital, analog, qam ..."

        public String majorNumber;
        public String minorNumber;
        public int serviceType;
        public int isFavorite;
        public int isSkip;
    }

    public enum ATSC_STATUS_TODO {
        PROCESSING,
        ACKOK,
        TIMEOUTFAIL,
        CHECKSUMERROR,
        OVERINDEXERROR,
        TIMEOUTREPLY,
        REPLYFAIL,
        IMPORTSUCCESS,
        IMPORTFAIL,
        UPGRADESUCCESS,
        UPGRADEFAIL,
        LEAVEUPGRADEMODE,
        EXPORTSUCCESS,
        EXPORTFAIL,
        PERMISSIONDENIED,
        FILENOTEXISTED,
        None;

        public int getValue() {
            return ordinal();
        }
    }
}
