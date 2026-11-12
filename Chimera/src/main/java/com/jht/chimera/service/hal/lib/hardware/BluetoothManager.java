package com.jht.chimera.service.hal.lib.hardware;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private DataMatrix dataMatrix;

    public static BluetoothManager getInstance() {
        return BluetoothManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final BluetoothManager INSTANCE = new BluetoothManager();
    }

    private BluetoothManager() {
        dataMatrix = new DataMatrix();
    }

    public synchronized void start() {
        stop();
        dataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, string -> {
            updateBluetoothName();
        }, false);

        dataMatrix.createStringAdapter(SettingKeys.Hardware.DATA_SALUTRON_BLE_NAME_STRING, string -> {
            Log.d(TAG, "got name " + string);
            updateBluetoothName();
        });

    }


    public void updateBluetoothName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null) {
            String bleName = DataMatrix.getString(SettingKeys.Hardware.DATA_SALUTRON_BLE_NAME_STRING);
            Log.d(TAG, "got name 2" + bleName);
            //FTMS can take up to 29 characters otherwise the advertisement packet will fail to send.
            if (bleName.isEmpty()) {
                String consoleSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
                if (consoleSerial == null || consoleSerial.isEmpty()) {
                    consoleSerial = "MATRIX"; // default ble name but this case should never happen //NON-NLS
                }
                if(consoleSerial.length() >= 30){
                    adapter.setName(consoleSerial.substring(0, 29));
                } else {
                    adapter.setName(consoleSerial);
                }
            } else {
                if(bleName.length() >= 30){
                    adapter.setName(bleName.substring(0, 29));
                } else {
                    adapter.setName(bleName);
                }

            }
        }
    }

    public synchronized void stop() {
        dataMatrix.finish();
    }
}
