package com.jht.chimera.lib.tv.switft;


import java.util.ArrayList;

public abstract class SwiftInterface {
    static ArrayList<ISwiftCommResponse> mSwiftCallbacks = new ArrayList<>();

    abstract public void registerSwiftOnReceiveListener(ISwiftCommResponse callback);

    abstract public void unregisterSwiftOnReceiveListener(ISwiftCommResponse callback);

    public abstract boolean open(boolean updateFirmware);

    public abstract void close();

    /**********************
     * SET DATA FUNCTIONS *
     **********************/

    // this function would send Command "PON"
    abstract public void setSwiftPowerOn();

    // this function would send Command "MEM"
    abstract public void setSwiftMenu();

    // this function would send Command "001"
    abstract public void setSwiftNumber1();

    // this function would send Command "002"
    public abstract void setSwiftNumber2();

    // this function would send Command "003"
    public abstract void setSwiftNumber3();

    // this function would send Command "004"
    abstract public void setSwiftNumber4();

    // this function would send Command "005"
    public abstract void setSwiftNumber5();

    // this function would send Command "006"
    public abstract void setSwiftNumber6();

    // this function would send Command "007"
    abstract public void setSwiftNumber7();

    // this function would send Command "008"
    public abstract void setSwiftNumber8();

    // this function would send Command "009"
    public abstract void setSwiftNumber9();

    // this function would send Command "000"
    abstract public void setSwiftNumber0();

    // this function would send Command "100/."
    public abstract void setSwiftDash();

    // this function would send Command "ENT"
    public abstract void setSwiftEnter();

    // this function would send Command "CUP"
    public abstract void setSwiftChannelUp();

    // this function would send Command "CUP"
    public abstract void setSwiftChannelDown();

    // this function would send Command "ART"
    public abstract void setSwiftVolumeUp();

    // this function would send Command "ALT"
    public abstract void setSwiftVolumeDown();

    // this function would send Command "POF"
    public abstract void setSwiftPowerOff();

    // this function would send Command "MUT"
    public abstract void setSwiftMute();

    // this function would send Command "SRC"
    public abstract void setSwiftInputSource();

    // this function would send Command "EXT"
    public abstract void setSwiftExit();

    // this function would send Command "RTN"
    public abstract void setSwiftReturn();

    // this function would send Command "CCD"
    public abstract void setSwiftClosedCaption();

    // this function would send Command "RED"
    public abstract void setSwiftRed();

    // this function would send Command "GRE"
    public abstract void setSwiftGreen();

    // this function would send Command "YEL"
    public abstract void setSwiftYellow();

    // this function would send Command "BLU"
    public abstract void setSwiftBlue();

    // this function would send Command "APT"
    public abstract void setSwiftAspectRatio();

    // this function would send Command "VCP"
    public abstract void setSwiftVChip();

    // this function would send Command "EPG"
    public abstract void setSwiftEPG();

    // this function would send Command "TXT"
    public abstract void setSwiftTeletext();

    // this function would send Command "CBS"
    public abstract void setSwiftCompositeIn();

    // this function would send Command "SVO"
    public abstract void setSwiftSVideoIn();

    // this function would send Command "CMP"
    public abstract void setSwiftComponentIn();

    // this function would send Command "VGA"
    public abstract void setSwiftVGAIn();

    // this function would send Command "TNR"
    public abstract void setSwiftTunerIn();

    // this function would send Command "HDM"
    public abstract void setSwiftHDMIIn();

    // this function would send Command "FAV"
    public abstract void setSwiftFavorite();

    // this function would send Command "ADI"
    public abstract void getSwiftTunerType();

    // this function would send Command "GVR"
    public abstract void getSwiftTunerTypeVersion();

    // this function would send Command "OM1"
    public abstract void setSwiftComponent1080i();

    // this function would send Command "OM2"
    public abstract void setSwiftComponent480i();

    // this function would send Command "OM3"
    public abstract void setSwiftComponent576i();

    // this function would send Command "OM4"
    public abstract void setSwiftComponent480p();

    // this function would send Command "OM5"
    public abstract void setSwiftComponent576p();

    // this function would send Command "OM6"
    public abstract void setSwiftComponent720p();

    // this function would send Command "GOM"
    public abstract void getSwiftComponentOutput();

    // this function would send Command "OH1"
    public abstract void setSwiftHDMI720p();

    // this function would send Command "OH2"
    public abstract void setSwiftHDMI1080p();

    // this function would send Command "GOH"
    public abstract void getSwiftHDMIOutput();

    // this function would send Command "STA"
    public abstract void getSwiftStatus();

    // this function would send Command "MPC"
    public abstract void getSwiftManufacturer();

    // this function would send Command "VER"
    public abstract void getSwiftSoftwareVersion();

    // this function would send Command "MUN"
    public abstract void setSwiftMuteOn();

    // this function would send Command "MUF"
    public abstract void setSwiftMuteOff();

    // this function would send Command "GMS"
    public abstract void getSwiftMuteStatus();

    // this function would send Command "0x01 0x00 Vol"
    public abstract void setSwiftVolume(int Volume /*byte[] bytes*/);

    // this function would send Command "GVL"
    public abstract void getSwiftVolume();

    // this function would send Command "GCH"
    public abstract void getSwiftChannel();

    // this function would send Command "SCH + 4 bytes (major_H major_L minor_H minor_L)"
    public abstract void setSwiftChannel(String Channel);

    // this function would send Command "SDH + 4 bytes (major_H major_L minor_H minor_L)"
    public abstract void setSwiftDefaultChannel(String DefaultCannel);

    // this function would send Command "CLO"
    public abstract void setSwiftCloneOutDataRequire();

    // this function would send Command "LEN"
    public abstract void getSwiftCloneDataLength();

    // this function would send Command "EXS + 4 bytes (index) + 4 bytes (Length)"
    public abstract void getSwiftCloneOutData(int index, int length);

    // this function would send Command "ABC"
    public abstract void setSwiftTest1();

    // this function would send Command "123"
    public abstract void setSwiftTest2();

    // this function would send Command ""
    public abstract void setSwiftTest3(String s);

    // this function would send Command "CLI"
    public abstract void setSwiftNotifyCloneIn();

    // this function would send Command "DLH" + 4 bytes (Length)
    public abstract void setSwiftNotifyCloneDataLength(int length);

    // this function would send Command "IMS + 4 bytes (index) + 4 bytes (Length) + N bytes (Data)"
    public abstract void setSwiftSendCloneInData(int startIndex, int dataSize, byte[] data);

    // this function would send Command ""
    public abstract void getSwiftImportResult();

    // this function would send Command "ISC"
    public abstract void setSwiftInitializeScanChannel();
}
