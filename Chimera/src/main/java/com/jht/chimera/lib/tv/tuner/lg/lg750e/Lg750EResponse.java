package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import androidx.annotation.NonNull;

public class Lg750EResponse {
    public String version;
    public String dhcp;
    public String ip;
    public String subnet;
    public String gateway;
    public String dns1;
    public String dns2;
    public String mac;

    public Lg750EResponse() {
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
        return "Lg750EResponse{" +
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
