package com.jht.chimera.service.hal.lib.io.iocommlib;

import android.net.Uri;

public interface MCUInterface {
    void setCallback(IMCUCallback callback);
    void getOperationMode();
    void getVersion();
    void getSafeKeyState();
    void getDevicePowerState(byte source);
    void getExternalPowerState();
    void getBoot();
    void getLcbType();
    void getFanSpeed();
    void getPowerMonitorState();
    void getExtendPowerError();
    void getPowerState(byte source);
    void setWatchdog(int timeout);
    void sendKeyEvent(boolean isUp, int key_u16);
    void refresh();
    void setKeyDebounceTime(int time, int time2, int gapTime);
    void setTone(int type);
    void setFanSpeed(int speed);
    void setPower(byte source, int _switch);
    void setRS485Communication(boolean enable);
    void setQiVoltage(int voltage);
    void setERPLED(int power);
    void setLEDColor(int LEDNumber, int color);
    void enterERP();
    void enterSuspend();
    void enterReboot();
    void setDebug(boolean enable, int level);
    void updateIO(Uri uri);
    void sendIRcommand(byte[] command);
    void sendIRFrequency(short data);
    void setCECconfig(byte address);
    void sendCECcommand(byte[] command);
    void getCECresponse();
    void reboot(int delayTime);
    void powerMonitor(boolean enable, int time);
    void sendCanTx(byte[] data);
}
