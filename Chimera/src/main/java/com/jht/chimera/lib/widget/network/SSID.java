package com.jht.chimera.lib.widget.network;


import android.net.wifi.ScanResult;


/**
 * The user model contains all user based configuration items.
 */
public class SSID {

    public enum SSIDState {
        IDLE,
        CONNECTING,
        CONNECTED
    }

    // This is the Uri of the content provider for this model.

    public ScanResult scanResult;
    public SSIDState state;
    public boolean open;
    public boolean hidden;
    public int signalStrength;
    public boolean configured;



}
