package com.jht.chimera.lib.network;

// This currently only works with WPA2, will need to add security type if needed later
public enum MatrixNetwork {
    MxWireless("MXWireless", "FuWxzbbocfOt"), //gitleaks:allow
    NpWireless("npwireless", "QvnDjg8PZodeN34LZSpl"); //gitleaks:allow

    public String ssid;
    public String key;

    MatrixNetwork(String ssid, String key) {
        this.ssid = ssid;
        this.key = key;
    }
}
