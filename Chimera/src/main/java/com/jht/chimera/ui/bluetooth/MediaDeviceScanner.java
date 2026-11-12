package com.jht.chimera.ui.bluetooth;

import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.bluetooth.BTSinkDevice;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gymkitmodule.comm.GsCommands;

import java.util.LinkedList;
import java.util.List;

public class MediaDeviceScanner implements IBluetoothScanner {
    private static final String TAG = "MediaDeviceScanner";
    DataMatrix dataMatrix = new DataMatrix();
    IBluetoothScanResult scanResult;
    List<MediaBluetoothDevice> deviceList = new LinkedList<>();

    public MediaDeviceScanner( ){
    }

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.MediaDeviceScanner;
    }

    /**
     *
     * @return  The name as a resource id that we will show for this scanner list.
     */
    @Override
    public int name() {
        return R.string.media_players_tc;
    }

    /**
     *
     * @return  The icon as a resource id that we will show for this scanner list.
     */
    @Override
    public int icon() {
        return R.drawable.bt_pairing_media_player;
    }

    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult  Callback to let the UI know the results.
     */
    @Override
    public void scan(IBluetoothScanResult scanResult) {

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_BT_SINK_DEVICE_FOUND_PARCELABLE, BTSinkDevice.BtDeviceParcelable.class, deviceParcelable -> {
            if(deviceParcelable.name != null) {
                if(scanResult != null) {
                    Log.e(TAG, "about to add MediaBluetoothDevice from MediaDeviceScanner with name: " + deviceParcelable.name);
                    MediaBluetoothDevice mediaBluetoothDevice = new MediaBluetoothDevice(deviceParcelable.name, deviceParcelable.bytes);
                    deviceList.add(mediaBluetoothDevice);
                    scanResult.deviceFound(mediaBluetoothDevice, null);
                }

                Log.d(TAG, "MediaDeviceScanner: "+deviceParcelable.name);
            }
        });


        //DataMatrix.putInt(HALKeys.ACTION_START_BT_SINK_SCAN_INT, 30);

        this.scanResult = scanResult;
    }

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    @Override
    public void stopScan() {
        Log.e(TAG, "about to stop media scanner");
        DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.BtStopScan.Value);
        dataMatrix.finish();
    }

    @Override
    public void clearDevices() {
        // TODO: Implement function.
        for (MediaBluetoothDevice device : deviceList) {
            device.finish();
        }
        deviceList.clear();
    }

    @Override
    public void close() {

    }
}
