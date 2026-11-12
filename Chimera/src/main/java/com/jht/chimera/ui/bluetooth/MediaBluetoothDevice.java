package com.jht.chimera.ui.bluetooth;

import android.os.Bundle;
import android.util.Log;

import com.jht.chimera.lib.common.bluetooth.BTSinkCommand;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.ui.mymedia.adapters.MediaItems;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gymkitmodule.comm.GsCommands;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import java.util.Arrays;

public class MediaBluetoothDevice implements IBluetoothDevice {

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

    private static final String TAG = "MediaBluetoothDevice";
    private String name;
    private byte[] addressBytes;
    private String addressString;

    private volatile DEVICE_MODE deviceMode;
    IBluetoothConnectionResult iBluetoothConnectionResult;
    private DataMatrix dataMatrix = new DataMatrix();


    MediaBluetoothDevice(String name, byte[] address) {
        this.name = name;
        this.addressBytes = address;
        addressString = setAddressString(address);
        if(name.trim().isEmpty()){
            this.name = addressString;
        }


        this.dataMatrix.createBoolAdapter(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, value -> setConnectionResult(value), false);

    }

    @Override
    public String displayName() {
        return name;
    }

    @Override
    public String macAddress() {
        return addressString;
    }

    @Override
    public boolean connected() {

        String address = StringUtil.byteArray2Hex(addressBytes);
        String connectedAddress = StringUtil.byteArray2Hex(DataMatrix.getBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES));


        Log.e(TAG, "connected(). address: " + address + ", connectedAddress: " + connectedAddress);

        return address.equalsIgnoreCase(connectedAddress);
    }

    @Override
    public boolean connecting() {
        Log.e(TAG, "MediaBluetoothDevice, deviceMode set to CONNECTING");
        return false;
    }

    @Override
    public String connect(final IBluetoothConnectionResult result) {
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_MEDIA_DEVICE.name);
        iBluetoothConnectionResult = result;

        Bundle bundle = new Bundle();
        bundle.putInt("pairMethod", GsCommands.BtPair.Value);
        bundle.putByteArray("address", addressBytes); //NON-NLS
        DataMatrix.putParcelable(BTSinkKeys.DATA_BT_SINK_PAIR_PARCELABLE, bundle);
        return null;
    }

    @Override
    public void cancelConnection() {
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.PAIR_MEDIA_DEVICE_CANCELED.name);
        DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, "");
        DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, null);
        DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, false);
        Log.e(TAG, "MediaBluetoothDevice, deviceMode set to DISCONNECTED");
        BTSinkCommand.sendCommand(GsCommands.BtCancelPairing.Value, addressBytes);
    }

    @Override
    public void disconnect(boolean sendAnalytics) {
        if(sendAnalytics) {
            DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.UNPAIR_MEDIA_DEVICE.name);
        }

        MediaItems mediaItems = DataMatrix.getParcelable(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, MediaItems.class);
        if(mediaItems != null) {
            Log.e(TAG, "about to clear in disconnect()");
            mediaItems.clear();
            dataMatrix.putParcelable(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, mediaItems);
        }

        // AVRCP - DISCONNECTED
        DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, "");
        DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, null);
        DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, false);
        Log.e(TAG, "MediaBluetoothDevice, deviceMode set to DISCONNECTED");
        BTSinkCommand.sendCommand(GsCommands.BtDeleteBond.Value, addressBytes);
    }

    @Override
    public void close() {
    }

    public byte[] getAddressBytes(){
        return addressBytes;
    }

    public void setConnectionResult(boolean connected){
        Log.e(TAG, "btConChange: in setConnectionResult of MediaBTDevice");


        String address = StringUtil.byteArray2Hex(addressBytes);
        String connectedAddress = StringUtil.byteArray2Hex(DataMatrix.getBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES));
        if (connected()) {
            Log.e(TAG, "btConChange: media device is connected");
            if (iBluetoothConnectionResult != null) {
                iBluetoothConnectionResult.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success, MediaBluetoothDevice.this);
            }
            iBluetoothConnectionResult = null;
        } else {
        }


//        if(connected){
//            deviceMode = DEVICE_MODE.CONNECTED;
//            iBluetoothConnectionResult.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Success);
//            DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, name);
//            DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, addressBytes);
//        }
//        else{
//            deviceMode = DEVICE_MODE.DISCONNECTED;
//            iBluetoothConnectionResult.onConnectionResult(IBluetoothConnectionResult.ConnectionResult.Failed);
//            DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, "");
//            DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, new byte[0]);
//        }

        Log.e(TAG, "about to set DATA_IS_BT_SINK_CONNECTED_BOOL to: " + connected);
        DataMatrix.putBoolean(HALKeys.DATA_IS_BT_SINK_CONNECTED_BOOL, connected);
    }

    private String setAddressString(byte[] bytes){
        StringBuilder sb = new StringBuilder(18);
        for (byte b : bytes) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void finish() {
        dataMatrix.finish();
    }

    public void update(MediaBluetoothDevice device) {
        Log.e(TAG, "this.name: " + this.name + ", this.addressString: " + this.addressString + ", deviceName: " + device.name);
        if(this.name.equals(this.addressString) && !device.name.isEmpty()) {
            Log.e(TAG, "this.name: " + this.name + ", this.addressString: " + this.addressString + ", deviceName: " + device.name);
            this.name = device.name;
        }
    }

    public String getName() {
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return "MediaBluetoothDevice{" +
                "this='" + this.hashCode() + '\'' +
                "name='" + name + '\'' +
                ", addressBytes=" + Arrays.toString(addressBytes) +
                ", addressString='" + addressString + '\'' +
                ", deviceMode=" + deviceMode +
                '}';
    }

    @Override
    protected void finalize() throws Throwable {
        Log.e(TAG, "before garbage collector destroys this object");
        super.finalize();
    }
}
