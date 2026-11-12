package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import java.io.IOException;

class LG6500SerialPort extends SerialPort {
    private static class LG6500SerialPortConfiguration extends SerialPortConfiguration {
        LG6500SerialPortConfiguration() {
            baudRate = BaudRate.B115200;
            CREAD = true;
            CLOCAL = true;
            csize = CSIZE.CS8;
            makeRaw = true;
            NOCTTY = true;
            TRACE_DEBUG = false;
        }
    }

    // LG6500 STB uses the same uart as us4 tuner
    private static final String deviceName = ProductConfig.portMappings.us4Path();

    private static class SingletonHolder {
        private static final LG6500SerialPort INSTANCE = new LG6500SerialPort();
    }

    public LG6500SerialPort() {
        super(deviceName, new LG6500SerialPortConfiguration());
    }

    static LG6500SerialPort getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void reset() {
        close();
        try {
            open();
        } catch (IOException ignored) {
        }
    }
}
