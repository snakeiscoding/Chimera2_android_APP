package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import androidx.annotation.NonNull;

public class LG6500Response {
    public String version;
    public String dhcp;
    public String ip;
    public String subnet;
    public String gateway;
    public String dns1;
    public String dns2;
    public String mac;

    public LG6500Response() {
        reset();
    }

    public void reset() {
        version = "N/A";
        dhcp = "";
        ip = "";
        subnet = "";
        gateway = "";
        dns1 = "";
        dns2 = "";
        mac = "";
    }

    @NonNull
    @Override
    public String toString() {
        return "LG6500Response{" +
                "version='" + version + '\'' +
                ", dhcp='" + dhcp + '\'' +
                ", ip='" + ip + '\'' +
                ", subnet='" + subnet + '\'' +
                ", gateway='" + gateway + '\'' +
                ", dns1='" + dns1 + '\'' +
                ", dns2='" + dns2 + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}
