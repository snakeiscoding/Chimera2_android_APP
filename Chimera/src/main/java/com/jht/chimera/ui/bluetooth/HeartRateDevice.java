package com.jht.chimera.ui.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import java.util.Objects;

@SuppressLint("MissingPermission")
public class HeartRateDevice implements IBluetoothDevice {

    private enum DEVICE_STATE {
        disconnected,
        connecting,
        connected
    }
    private final BluetoothDevice device;
    private final Context context;
    private DEVICE_STATE deviceState;
    private IBluetoothConnectionResult connectionCallback;
    private final DataMatrix dataMatrix;
    private boolean callbackRegistered = false;

    public HeartRateDevice(Context context, BluetoothDevice device, boolean isConnected) {
        this.device = device;
        this.context = context;
        deviceState = isConnected ? DEVICE_STATE.connected : DEVICE_STATE.disconnected;
        dataMatrix = new DataMatrix();
    }

    public void registerCallbacks() {
        if (callbackRegistered) return;
        callbackRegistered = true;

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_BLE_CONNECTED_PARCELABLE, BluetoothDevice.class, deviceReceived -> {
            if(device.getAddress().equals(deviceReceived.getAddress())) {
                deviceState = DEVICE_STATE.connected;
                if (connectionCallback != null) {
                    connectionCallback.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, HeartRateDevice.this);
                    DataMatrix.putString (DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, device.getAddress());
                }
            }
        });

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_BLE_CONNECTION_FAILED_PARCELABLE, BluetoothDevice.class, deviceReceived -> {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE_FAILED.name);
            if(device.getAddress().equals(deviceReceived.getAddress())) {
                deviceState = DEVICE_STATE.disconnected;
                if (connectionCallback != null) {
                    connectionCallback.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed, HeartRateDevice.this);
                }
            }
        });
    }

    @Override
    public void close() {
        dataMatrix.finish();
    }

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
        return deviceState == DEVICE_STATE.connected;
    }

    @Override
    public boolean connecting() {
        return deviceState == DEVICE_STATE.connecting;
    }

    @Override
    public String connect(final IBluetoothConnectionResult result) {
        registerCallbacks();
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE.name);
        DataMatrix.putParcelable(HALKeys.ACTION_CONNECT_BLE_PARCELABLE, device);

        connectionCallback = result;
        deviceState = DEVICE_STATE.connecting;

        return null;
    }

    @Override
    public void cancelConnection() {

        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE_CANCELED.name);
        if(connectionCallback != null) {
            connectionCallback.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Canceled, HeartRateDevice.this);
        }
        disconnect(false);
    }

    @Override
    public void disconnect(boolean sendAnalytics) {
        if(sendAnalytics) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.UNPAIR_BLE_HEARTRATE.name);
            DataMatrix.putString (DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, "");
        }
        deviceState = DEVICE_STATE.disconnected;
        DataMatrix.putParcelable(HALKeys.ACTION_DISCONNECT_BLE_PARCELABLE, device);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IBluetoothDevice)) return false;
        IBluetoothDevice other = (IBluetoothDevice) obj;
        return Objects.equals(this.macAddress(), other.macAddress());
    }

}
