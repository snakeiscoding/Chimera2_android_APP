package com.jht.chimera.ui.bluetooth;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;
import java.util.Objects;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("MissingPermission")
public class HeadphoneDevice extends BroadcastReceiver implements IBluetoothDevice {
    @NonNls private final String TAG = "HeadphoneDevice";

    private enum DEVICE_STATE {
        disconnected,
        bonding,
        connecting,
        connected
    }

    final static int NUM_MAX_RETRY = 3;

    private final BluetoothDevice device;
    private final BluetoothA2dp a2dpService;
    private final Context context;
    private DEVICE_STATE deviceState = DEVICE_STATE.disconnected;
    private IBluetoothConnectionResult connectionCallback;
    private int numRetry;
    private boolean executingConnect = false;
    private boolean receiverRegistered = false;

    HeadphoneDevice(Context context, BluetoothA2dp a2dpService, BluetoothDevice device) {
        this.device = device;
        this.context = context;
        this.a2dpService = a2dpService;

        int connectionState = a2dpService.getConnectionState(device);
        switch(connectionState) {
            case BluetoothA2dp.STATE_DISCONNECTED:
                switch(device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        deviceState = DEVICE_STATE.disconnected;
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        deviceState = DEVICE_STATE.bonding;
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        deviceState = DEVICE_STATE.connecting;
                        break;
                }
                break;
            case BluetoothA2dp.STATE_DISCONNECTING:
                deviceState = DEVICE_STATE.disconnected;
                break;
            case BluetoothA2dp.STATE_CONNECTING:
                deviceState = DEVICE_STATE.connecting;
                break;
            case BluetoothA2dp.STATE_CONNECTED:
                deviceState = DEVICE_STATE.connected;
                break;
        }
        Log.d(TAG, "create headphone device bond state " + device.getBondState() + " connection state " + connectionState + " " + device.getName() + " " + "deviceState " + deviceState);
    }

    @Override
    public void close() {
        unregisterBluetoothNotifications();
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
        return deviceState == DEVICE_STATE.connecting || deviceState == DEVICE_STATE.bonding;
    }

    @Override
    public String connect(IBluetoothConnectionResult result) {
        DataMatrix.putString(HALKeys.ACTION_ADD_BT_HEADPHONE_STRING, device.getAddress());
        registerBluetoothNotifications();

        executingConnect = true;
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_HEADPHONES.name);
        connectionCallback = result;
        numRetry = 0;
        if(deviceState != DEVICE_STATE.connected) {
            deviceState = DEVICE_STATE.bonding;
        }

        createBondImpl(device);

        return null;
    }

    private void connectA2DP() {
        try {
            Method connectMethod;
            connectMethod = BluetoothA2dp.class.getMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(a2dpService, device);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void cancelConnection() {
        executingConnect = false;
        disconnect(false);
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_HEADPHONES_CANCELED.name);

        if(connectionCallback != null) {
            connectionCallback.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Canceled, HeadphoneDevice.this);
            connectionCallback = null;
        }
    }

    public void disconnect(boolean sendAnalytics) {
        DataMatrix.putString(HALKeys.ACTION_REMOVE_BT_HEADPHONE_STRING, device.getAddress());
        executingConnect = false;
        if(sendAnalytics) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.UNPAIR_HEADPHONES.name);
            DataMatrix.putString (DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, "");
        }
        deviceState = DEVICE_STATE.disconnected;
        disconnect(device); // always remove bond even if disconnected
    }

    private final JHTNotification disconnectNotification = new JHTNotification();
    private void disconnect(BluetoothDevice device) {
        numRetry = NUM_MAX_RETRY; // disable the retry since we are disconnecting
        removeBondImpl(device);
    }


    private void createBondImpl(BluetoothDevice bluetoothDevice) {
        try {
            bluetoothDevice.createBond();
        } catch (Exception e) {
            Log.w(TAG, "create bond failed", e);
        }
    }

    private void removeBondImpl(BluetoothDevice bluetoothDevice) {
        try {
            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) return; // return if device is not bonded

            if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                Method cancelBondProcess;
                //noinspection JavaReflectionMemberAccess
                cancelBondProcess = BluetoothDevice.class.getMethod("cancelBondProcess");
                cancelBondProcess.invoke(bluetoothDevice);
            }

            Method removeBondMethod;
            //noinspection JavaReflectionMemberAccess
            removeBondMethod = BluetoothDevice.class.getMethod("removeBond"); // final Method removeBond = device.getClass().getMethod("removeBond"); ?
            removeBondMethod.invoke(bluetoothDevice);

        } catch (Exception e) {
            Log.w(TAG, "remove bond failed", e);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && intent.getAction() != null) {
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (bluetoothDevice != null && device != null && bluetoothDevice.getAddress().equals(device.getAddress())) {
                switch (intent.getAction()) {
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                        onBondStateChange(intent, bluetoothDevice);
                        break;
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED: // an aborted bond attempt seems to raise ACL_DISCONNECTED
                        // call onConnectionStateChanged or just clear bond to trigger onBondStateChange? both work but clear bond avoids using an extra retry count?
                        removeBondImpl(bluetoothDevice); // should trigger BOND_NONE
                        break;
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED: {
                        onConnectionStateChanged(bluetoothDevice);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IBluetoothDevice)) return false;
        IBluetoothDevice other = (IBluetoothDevice) obj;
        return Objects.equals(this.macAddress(), other.macAddress());
    }

    protected void registerBluetoothNotifications() {
        if(receiverRegistered) return;
        receiverRegistered = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        context.registerReceiver(this, intentFilter);
    }
    private void unregisterBluetoothNotifications() {
        try {
            context.unregisterReceiver(this);
        } catch( Exception ignored) {}
    }

    private void onConnectionResult(IBluetoothConnectionResult.ConnectionResult result, IBluetoothDevice btDevice) {
        if(connectionCallback != null) {
            connectionCallback.onConnectionResult(result, btDevice);
        }
    }

    private void onConnectionStateChanged(BluetoothDevice bluetoothDevice) {
        int connectState = a2dpService.getConnectionState(bluetoothDevice);
        switch(connectState) {
            case BluetoothA2dp.STATE_DISCONNECTED:
                if (executingConnect && numRetry < NUM_MAX_RETRY) {
                    numRetry++;
                    removeBondImpl(bluetoothDevice);
                    createBondImpl(bluetoothDevice);
                }
                else {
                    removeBondImpl(bluetoothDevice); // delete the bond if disconnected
                    deviceState = DEVICE_STATE.disconnected;
                    disconnectNotification.setNotification();
                    DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_HEADPHONES_FAILED.name);
                    onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Lost, HeadphoneDevice.this);
                }
                break;
            case BluetoothA2dp.STATE_DISCONNECTING:
                deviceState = DEVICE_STATE.disconnected;
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_HEADPHONES_FAILED.name);
                onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Lost, HeadphoneDevice.this);
                break;
            case BluetoothA2dp.STATE_CONNECTING:
                break;
            case BluetoothA2dp.STATE_CONNECTED:
                numRetry = NUM_MAX_RETRY;
                deviceState = DEVICE_STATE.connected;
                onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, HeadphoneDevice.this);
                DataMatrix.putString (DAPIKeys.DATA_USER_BLE_AUDIO_MAC_ID_STRING, bluetoothDevice.getAddress());
                break;
            default:
                // Note getting  -2147483648 on some connections.  Really confusing and screwing things up so let's
                // assume this is a connnection.
                numRetry = NUM_MAX_RETRY;
                deviceState = DEVICE_STATE.connected;
                onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, HeadphoneDevice.this);
                break;
        }
    }

    private void onBondStateChange(Intent intent, BluetoothDevice bluetoothDevice) {
        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
        if (state == BluetoothDevice.BOND_BONDED) {
            if(deviceState == DEVICE_STATE.bonding) { // should this block if not connecting? or permit the dropped connection to reconnect?
                deviceState = DEVICE_STATE.connecting;
                connectA2DP();
            }
        }
        else if(state == BluetoothDevice.BOND_NONE) {
            if (executingConnect && numRetry < NUM_MAX_RETRY) {
                numRetry++;
                removeBondImpl(bluetoothDevice);
                createBondImpl(bluetoothDevice);
            }
            else if(deviceState == DEVICE_STATE.connecting || deviceState == DEVICE_STATE.bonding) {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_HEADPHONES_FAILED.name);
                if(connectionCallback != null) {
                    connectionCallback.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed, HeadphoneDevice.this);
                }
                deviceState = DEVICE_STATE.disconnected;
                disconnect(bluetoothDevice);
            }
        }
    }

}
