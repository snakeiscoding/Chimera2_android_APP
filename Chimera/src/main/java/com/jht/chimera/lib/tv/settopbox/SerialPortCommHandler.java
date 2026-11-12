package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import java.io.IOException;

public abstract class SerialPortCommHandler implements ICommHandler {
    private static final String TAG = "SerialPortCommHandler";
    private SerialPort serialPort;

    @Override
    public void open(SetTopBoxSetting setTopBoxSetting) {
        close();
        SerialPortConfiguration conf = new SerialPortConfiguration();
        conf.baudRate = SerialPortConfiguration.BaudRate.B115200;
        for(SerialPortConfiguration.BaudRate rate : SerialPortConfiguration.BaudRate.values()) {
            if(rate.getValue() == setTopBoxSetting.baudRate) {
                conf.baudRate = rate;
                break;
            }
        }
        Log.d(TAG, "open device " + setTopBoxSetting.serialPortDevice + " " + conf.baudRate);
        conf.CREAD = true;
        conf.CLOCAL = true;
        conf.csize = SerialPortConfiguration.CSIZE.CS8;
        conf.makeRaw = true;
        conf.NOCTTY = true;
        conf.TRACE_DEBUG=false;
        serialPort = new SerialPort(setTopBoxSetting.serialPortDevice, conf);
        try {
            serialPort.open();
        } catch (IOException ignored) {
            Log.d(TAG, "error opening device " + setTopBoxSetting.serialPortDevice, ignored);
        }
    }

    @Override
    public void close() {
        if(serialPort != null) {
            serialPort.close();
        }
    }

    @Override
    public void sendData(byte[] buffer, int size) {
        try {

            Log.d(TAG, "Write " + size + " " + StringUtil.byteArray2Hex(buffer));
            serialPort.getOutputStream().write(buffer, 0, size);
        } catch (IOException ignored) {
        }
    }

    @Override
    public int readData(byte[] buffer) {
        try {
            return serialPort.getInputStream().read(buffer);
        } catch (IOException ignored) {
        }
        return -1;
    }
}
