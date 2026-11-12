package com.jht.chimera.service.hal.lib.hardware;

import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;

public class HdmiIn {
    private static HdmiIn sInstance;

    static {
        System.loadLibrary("hdmiin");
    }

    public static HdmiIn getInstance() {
        if (null == sInstance) {
            sInstance = new HdmiIn();
        }

        return sInstance;
    }

    private HdmiIn(){}

    public int switchPort(int port) { // possible values of 1,2,3 ? think we need a smarter way of handling these?
        if (port < 0) return 0; // not sure what is considered valid? is there an ALL-off option?
        // Parker Port Path mappings have been added to ProductConfig.java instead of being hard coded.
        switch (ProductConfig.hardware) {
            case Parker:
            case Parker_3:
                return switchPort1(ProductConfig.portMappings.hdmiI2CPath(), port);
            case Vortex:
            case VortexLite:
                // reading "/sys/class/hdmirx/hdmirx/port_switch" will also provide the which inputs have connections as "p0=plugout p1=plugin p2=plugout"
                ExecuteAsRoot.run("echo p" + (port-1) + " > /sys/class/hdmirx/hdmirx/port_switch"); // p0 p1 p2
                return 0;
        }
        return 0; // if hardware is not applicable, just return success
    }

    private native int switchPort1(String path, int port); // parker1 did some low level i2c interactions
    public native int setVideoDelay(int value); // this a parker1 driver, do we move parker2 version here, keep in audio select manager, or move all of them here?
}
