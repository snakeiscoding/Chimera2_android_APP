package com.jht.chimera.ui.bluetooth;

public interface IBluetoothConnectionResult {
    enum ConnectionResult {
        Success,
        Failed,
        Canceled,
        Lost
    }
    void onConnectionResult(ConnectionResult result, IBluetoothDevice btDevice);
}
