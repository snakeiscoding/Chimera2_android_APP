package com.jht.chimera.ui.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import com.jht.chimera.R;

public class BleDevice implements IBluetoothDevice {

    private final BluetoothDevice device;
    private final Context context;

    public BleDevice(Context context, BluetoothDevice device) {
        this.device = device;
        this.context = context;
    }

    @Override
    public void close() {}

    @Override
    public String displayName() {
        String name = device.getName();
        if( TextUtils.isEmpty(name) || !TextUtils.isGraphic(name) ) {
            name = device.getAddress();
        }
        if( TextUtils.isEmpty(name) || !TextUtils.isGraphic(name) ) {
            return context.getResources().getString(R.string.anonymous_tc);
        }
        return name;
    }

    @Override
    public String macAddress() {
        return device.getAddress();
    }

    @Override
    public boolean connected() {
        return false;
    }

    @Override
    public boolean connecting() {
        return false;
    }

    @Override
    public String connect(final IBluetoothConnectionResult result) {
        return null;
    }

    @Override
    public void cancelConnection() {
    }

    @Override
    public void disconnect(boolean sendAnalytics) {

    }
}
