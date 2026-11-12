package com.jht.chimera.ui.bluetooth;

import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.service.hal.lib.salutron.SalutronDevice;
import com.jht.chimera.service.hal.lib.salutron.salutronCommands;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import java.util.Arrays;

public class SalutronHeartRateDevice implements IBluetoothDevice {

    private static final String TAG = "SalutronHeartRateDevice";

    public byte[] id;
    public String name;
    public salutronCommands.DeviceType type;
    public DEVICE_STATE deviceState = DEVICE_STATE.disconnected;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private boolean connecting = false;

    private enum DEVICE_STATE {
        disconnected,
        connected
    }

    public SalutronHeartRateDevice(byte[] id, String name, salutronCommands.DeviceType type){
        this.id = id;
        this.name = name;
        this.type = type;

        if(DataMatrix.getBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL)){
            checkDevice();
        }
        Log.d(TAG, "SalutronHeartRateDevice: " + name + " Connection State "  + macAddress()  + " -> " + " " + deviceState);

    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public String macAddress() {
        return StringUtil.byteArray2Hex(id);
    }

    @Override
    public boolean connected() {
        Log.d(TAG, displayName() + " " + deviceState.toString());
        return deviceState == DEVICE_STATE.connected;
    }

    @Override
    public boolean connecting() {
        return connecting;
    }

    @Override
    public String connect(IBluetoothConnectionResult result) {
        if(type == salutronCommands.DeviceType.ANT_HR) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_ANT_HEARTRATE.name);
        }
        else if(type == salutronCommands.DeviceType.BLE) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE.name);
        }
        SalutronDevice.sendConnectDevice(name,id,type);
        connecting = true;
        mDataMatrix.createByteArrayAdapter(HALKeys.DATA_SALUTRON_CONNECTED_DEVICE_BYTES, connectedId -> {
            if (connectedId != null && Arrays.equals(connectedId, id)) {
                Log.d(TAG, displayName() + " Connection State: " + true + " " + name + " " + macAddress());
                connecting = false;
                deviceState = DEVICE_STATE.connected;
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, SalutronHeartRateDevice.this);
                DataMatrix.putString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, macAddress());
            } else if (!connecting) { // seems like we see a loss reported before connection during connecting
                // there is no connected salutron HR device OR the connected device is not this device
                Log.d(TAG, displayName() + " Connection State: " + false + " " + name + " " + macAddress());
                connecting = false;
                deviceState = DEVICE_STATE.disconnected;
                result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Lost, SalutronHeartRateDevice.this);
            }
        },false);

        mDataMatrix.createEventAdapter(HALKeys.EVENT_SALUTRON_DEVICE_CONNECTION_FAIL, event -> {
            if(type == salutronCommands.DeviceType.ANT_HR) {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_ANT_HEARTRATE_FAILED.name);
            }
            else if(type == salutronCommands.DeviceType.BLE) {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE_FAILED.name);
            }
            Log.e(TAG, "connect: " + displayName() + " fail" );
            result.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed, SalutronHeartRateDevice.this);
            connecting = false;
        });
        return null;
    }

    @Override
    public void cancelConnection() {

        if(type == salutronCommands.DeviceType.ANT_HR) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_ANT_HEARTRATE_CANCELED.name);
        }
        else if(type == salutronCommands.DeviceType.BLE) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_BLE_HEARTRATE_CANCELED.name);
        }
        connecting = false;
//        deviceState = DEVICE_STATE.disconnected;
        SalutronDevice.sendDisConnectDevice(name,id,type);
    }

    public void disconnect(boolean sendAnalytics) {
        if(sendAnalytics) {
            if(type == salutronCommands.DeviceType.ANT_HR) {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.UNPAIR_ANT_HEARTRATE.name);
                DataMatrix.putString(DAPIKeys.DATA_USER_ANTPLUS_HR_DEVICE_ID_STRING, "");
            }
            else if(type == salutronCommands.DeviceType.BLE) {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.UNPAIR_BLE_HEARTRATE.name);
                DataMatrix.putString(DAPIKeys.DATA_USER_BLE_HR_MAC_ID_STRING, "");
            }
        }
        deviceState = DEVICE_STATE.disconnected;
        Log.d(TAG, displayName() + " " + deviceState.toString());
        SalutronDevice.sendDisConnectDevice(name,id,type);
    }

    @Override
    public void close() {
        mDataMatrix.finish();
    }

    @Override
    public boolean equals(Object o) {
        SalutronHeartRateDevice item = (SalutronHeartRateDevice)o;
        return Arrays.equals(id,item.id) && name.equals(item.name);
    }

    public void checkDevice(){
        byte[] currentConnected = DataMatrix.getBytes(HALKeys.DATA_SALUTRON_CONNECTED_DEVICE_BYTES);
//        Log.d(TAG, "checkDevice: current connect " + Arrays.toString(currentConnected));
        if (currentConnected != null && Arrays.equals(currentConnected, id)) {
            deviceState = DEVICE_STATE.connected;
        }
        else
            deviceState = DEVICE_STATE.disconnected;
//        Log.d(TAG, "checkDevice: set " + displayName() + " " + deviceState);
    }

}
