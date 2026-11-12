package com.jht.chimera.service.hal.lib.salutron;

public interface IHeartRateCallback {

    void onHeartRate(int value, int type);

    void onScanning(boolean isScanning);

    void onWorkout(boolean isWorkout);

    void isConnected(boolean isConnected, SalutronDevice.SalutronDeviceParcelable connectDevice);

    void version(String version);

    void blink(boolean isKeep);

    void stopBlink();

}
