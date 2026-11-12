package com.jht.chimera.service.hal.lib.salutron;

import com.jht.serialport.SerialPort;

public abstract class SalutronInterface {
    abstract public void registerOnReceiveListener(IHeartRateCallback callback);

    abstract public void unregisterOnReceiveListener(IHeartRateCallback callback);

    abstract public boolean start(SerialPort device);

    abstract public void stop();
}
