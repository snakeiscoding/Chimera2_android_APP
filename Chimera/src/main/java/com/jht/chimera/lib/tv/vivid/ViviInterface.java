package com.jht.chimera.lib.tv.vivid;


import com.jht.serialport.SerialPort;

import java.util.ArrayList;

public abstract class ViviInterface {
    static ArrayList<IViviCommResponse> mViviCallbacks = new ArrayList<>();

    abstract public void registerViviOnReceiveListener(IViviCommResponse callback);

    abstract public void unregisterViviOnReceiveListener(IViviCommResponse callback);

    abstract public boolean start(SerialPort device/*, String path, int baudRate*/);

    abstract public void stop();

    /**********************
     * SET DATA FUNCTIONS *
     **********************/

    // this function would send Command "PON"
    abstract public void setViviPowerOn();

    // this function would send Command "MEM"
    abstract public void setViviMenu();

    // this function would send Command "001"
    abstract public void setViviNumber1();

    // this function would send Command "002"
    public abstract void setViviNumber2();

    // this function would send Command "003"
    public abstract void setViviNumber3();

    // this function would send Command "004"
    abstract public void setViviNumber4();

    // this function would send Command "005"
    public abstract void setViviNumber5();

    // this function would send Command "006"
    public abstract void setViviNumber6();

    // this function would send Command "007"
    abstract public void setViviNumber7();

    // this function would send Command "008"
    public abstract void setViviNumber8();

    // this function would send Command "009"
    public abstract void setViviNumber9();

    // this function would send Command "000"
    abstract public void setViviNumber0();

    // this function would send Command "100/."
    public abstract void setViviDash();

    // this function would send Command "ENT"
    public abstract void setViviEnter();

    // this function would send Command "CUP"
    public abstract void setViviChannelUp();

    // this function would send Command "CUP"
    public abstract void setViviChannelDown();

    // this function would send Command "ART"
    public abstract void setViviVolumeUp();

    // this function would send Command "ALT"
    public abstract void setViviVolumeDown();

    // this function would send Command "POF"
    public abstract void setViviPowerOff();

    // this function would send Command "MUT"
    public abstract void setViviMute();

    // this function would send Command "SRC"
    public abstract void setViviInputSource();

    // this function would send Command "EXT"
    public abstract void setViviExit();

    // this function would send Command "RTN"
    public abstract void setViviReturn();

    // this function would send Command "CCD"
    public abstract void setViviClosedCaption();

    // this function would send Command "RED"
    public abstract void setViviRed();

    // this function would send Command "GRE"
    public abstract void setViviGreen();

    // this function would send Command "YEL"
    public abstract void setViviYellow();

    // this function would send Command "BLU"
    public abstract void setViviBlue();

    // this function would send Command "APT"
    public abstract void setViviAspectRatio();

    // this function would send Command "VCP"
    public abstract void setViviVChip();

    // this function would send Command "EPG"
    public abstract void setViviEPG();

    // this function would send Command "TXT"
    public abstract void setViviTeletext();

    // this function would send Command "CBS"
    public abstract void setViviCompositeIn();

    // this function would send Command "SVO"
    public abstract void setViviSVideoIn();

    // this function would send Command "CMP"
    public abstract void setViviComponentIn();

    // this function would send Command "VGA"
    public abstract void setViviVGAIn();

    // this function would send Command "TNR"
    public abstract void setViviTunerIn();

    // this function would send Command "HDM"
    public abstract void setViviHDMIIn();

    // this function would send Command "FAV"
    public abstract void setViviFavorite();

    // this function would send Command "ADI"
    public abstract void getViviTunerType();

    // this function would send Command "OM1"
    public abstract void setViviComponent1080i();

    // this function would send Command "OM2"
    public abstract void setViviComponent480i();

    // this function would send Command "OM3"
    public abstract void setViviComponent576i();

    // this function would send Command "OM4"
    public abstract void setViviComponent480p();

    // this function would send Command "OM5"
    public abstract void setViviComponent576p();

    // this function would send Command "OM6"
    public abstract void setViviComponent720p();

    // this function would send Command "GOM"
    public abstract void getViviComponentOutput();

    // this function would send Command "OH1"
    public abstract void setViviHDMI720p();

    // this function would send Command "OH2"
    public abstract void setViviHDMI1080p();

    // this function would send Command "GOH"
    public abstract void getViviHDMIOutput();

    // this function would send Command "STA"
    public abstract void getViviStatus();

    // this function would send Command "MPC"
    public abstract void getViviManufacturer();

    // this function would send Command "VER"
    public abstract void getViviSoftwareVersion();
}
