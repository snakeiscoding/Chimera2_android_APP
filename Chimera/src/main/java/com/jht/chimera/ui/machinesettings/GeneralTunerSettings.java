package com.jht.chimera.ui.machinesettings;


import com.jht.chimera.lib.tv.ChannelInfo;

import java.util.LinkedList;
import java.util.List;


/**
 * TODO THIS IS A TEMPORARY PORT SO CODE COMPILES.
 */

public class GeneralTunerSettings {


    public List<ChannelInfo> channelList = new LinkedList<>();
    public String defaultChannel;
    public String firmwareVersion;
    public JHTTypes.AspectRatio currentAspectRatio;
    public int overScanSize;
    public int exportPercentage;
    public int importPercentage;
    public int updatePercentage;
    public JHTTypes.ATSC_STATUS atscStatus;

}
