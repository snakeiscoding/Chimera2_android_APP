package com.jht.chimera.ui.bluetooth;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;
import java.util.Random;

public class TestBluetoothDevice implements IBluetoothDevice {

    enum TEST_MODES {
        CONNECT_SUCCESS,
        PAIRING_FAIL,
        HANG,
        CONNECTION_FAIL

    }

    enum DEVICE_MODE {
        DISCONNECTED,
        BONDING,
        BONDED,
        CONNECTING,
        CONNECTED
    }

    private String name;
    private String address;


    private TEST_MODES testMode;
    private volatile DEVICE_MODE deviceMode;
    private boolean usePin;

    TestBluetoothDevice(@NonNls String name, @NonNls String address, TEST_MODES mode, DEVICE_MODE initialDeviceMode, boolean usePin) {
        this.name = name;
        this.address = address;
        this.testMode = mode;
        this.deviceMode = initialDeviceMode;
        this.usePin = usePin;
    }


    @Override
    public String displayName() {
        return name;
    }

    @Override
    public String macAddress() {
        return address;
    }

    @Override
    public boolean connected() {
        return deviceMode == DEVICE_MODE.CONNECTED;
    }

    @Override
    public boolean connecting() {
        return deviceMode == DEVICE_MODE.CONNECTING;
    }

    @Override
    public String connect(final IBluetoothConnectionResult result) {
        String pincode;
        if(usePin) {
            Random random = new Random();
            pincode = String.format(Locale.ENGLISH, "%d", random.nextInt(8999) + 1000);
        }
        else {
            pincode = null;
        }
        deviceMode = DEVICE_MODE.BONDING;
        new Thread(() -> {
            if(testMode == TEST_MODES.HANG) {
                return;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }

            if(deviceMode == DEVICE_MODE.DISCONNECTED) {
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Canceled, TestBluetoothDevice.this);
                return;
            }
            if(testMode == TEST_MODES.PAIRING_FAIL) {
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed, TestBluetoothDevice.this);
                deviceMode = DEVICE_MODE.DISCONNECTED;
                return;
            }

            deviceMode = DEVICE_MODE.CONNECTING;

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }

            if(deviceMode == DEVICE_MODE.DISCONNECTED) {
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Canceled, TestBluetoothDevice.this);
                return;
            }

            if(testMode == TEST_MODES.CONNECTION_FAIL) {
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed, TestBluetoothDevice.this);
                deviceMode = DEVICE_MODE.DISCONNECTED;
                return;
            }
            deviceMode = DEVICE_MODE.CONNECTED;
            result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, TestBluetoothDevice.this);

        }).start();
        return pincode;
    }

    @Override
    public void cancelConnection() {
        deviceMode = DEVICE_MODE.DISCONNECTED;

    }

    @Override
    public void disconnect(boolean sendAnalytics) {
        deviceMode = DEVICE_MODE.DISCONNECTED;
    }

    @Override
    public void close() {

    }
}
