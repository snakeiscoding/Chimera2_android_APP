package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.common.callback.hr.HeartRateMeasurementDataCallback;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

// Version of BLEHeartRateReader that uses the nordic BLE library
@SuppressLint("MissingPermission")
class BLEHeartRateReader2 implements IHeartRateReader {
    private static final String HEART_RATE_CHARACTERISTIC = "00002a37-0000-1000-8000-00805f9b34fb"; //NON-NLS
    private static final String HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb"; //NON-NLS
    private static final int CONNECT_TIMEOUT_MS = 30000; // 30 sec.

    private BluetoothDevice currentDevice = null; // could be normal BLE or MyZone device!
    private HeartRateConnection currentConnection = null; // at the moment only allow one connection, otherwise could have map of device address and connection
    private final MyZoneReader myZoneReader;

    private volatile int heartRate;
    private volatile HeartRateSource heartRateSource;
    private final DataMatrix dataMatrix = new DataMatrix();
    private final Context context;
    private final IHeartRateUpdated heartRateUpdated;

    // This reads the heart rate every second while we are connected.
    private final JHTTimer bleTimer = new PeriodicTimer(0, 1000, () -> {
        if (currentConnection != null) {
            currentConnection.queueHrRead();
        }
    });

    private boolean isCurrentDevice(BluetoothDevice device) {
        return currentDevice != null && currentDevice.getAddress().equalsIgnoreCase(device.getAddress());
    }

    BLEHeartRateReader2(Context context, IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        this.context = context;

        myZoneReader = new MyZoneReader(context, new IHeartRateUpdated() {
            @Override
            public void heartRateUpdated() {
                HeartRateSource source = myZoneReader.hasConnection() ? HeartRateSource.MyZone : HeartRateSource.BLE;
                setBLEHeartRate(myZoneReader.heartRate(), source);
            }
        });

        dataMatrix.createParcelableAdapter(HALKeys.ACTION_CONNECT_BLE_PARCELABLE, BluetoothDevice.class, this::connect, false);
        dataMatrix.createParcelableAdapter(HALKeys.ACTION_DISCONNECT_BLE_PARCELABLE, BluetoothDevice.class, this::disconnect, false);
    }

    private void connect(BluetoothDevice device) {
        if (isCurrentDevice(device)) return; // already connected
        releaseConnection(true); // make sure any previous connection is cleaned up before creating a new one
        currentDevice = device;
        currentConnection = new HeartRateConnection(context);
        currentConnection.connect(device).useAutoConnect(false).retry(3).timeout(CONNECT_TIMEOUT_MS).enqueue();
    }

    private void onConnection(BluetoothDevice device) {
        // connect BLE event could be for something else
        if (!isCurrentDevice(device)) return;
        // Notify others that a GATT device connection has been made.
        DataMatrix.putParcelable(HALKeys.EVENT_BLE_CONNECTED_PARCELABLE, device);
        DataMatrix.putString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING, device.getAddress());
        setBLEHeartRate(0, HeartRateSource.BLE);
        if(!DataMatrix.getBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL)) {
            myZoneReader.connectToMyZoneDevice(device, ()->{
                // if device connected via MyZone, drop the initial connection
                bleTimer.stop();
                // previous code also did an explicit disconnect, is that needed? do we need alternate disconnection event handling for that?
                releaseConnection(false); // MyZone reader should take over, so clean up initial connection
            });
        }
        bleTimer.start("BleHrTimer");
    }

    private void disconnect(BluetoothDevice device) {
        // disconnect BLE DM event could be for something else
        if (!isCurrentDevice(device)) return;
        bleTimer.stop();
        releaseConnection(true); // do we need to explicitly queue a disconnect request or just close connection?
        if(currentDevice != null) {
            DataMatrix.putParcelable(HALKeys.EVENT_BLE_CONNECTION_FAILED_PARCELABLE, currentDevice);
        }
        DataMatrix.putString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING, "");
        myZoneReader.disconnect();
        setBLEHeartRate(0, HeartRateSource.None);
        currentDevice = null;
    }

    @Override
    public void close() {
        dataMatrix.finish();
        releaseConnection(false);
    }

    private void releaseConnection(boolean fullDisconnect) {
        if (currentConnection != null) {
            if (fullDisconnect) currentConnection.terminateConnectionAsync();
            else currentConnection.close(); // there are OS resources that we must release
            currentConnection = null;
        }
    }

    @Override
    public synchronized int heartRate() {
        return heartRate;
    }

    @Override
    public synchronized boolean blankState() { return false; }

    @Override
    public HeartRateSource getSource() {
        return heartRateSource;
    }

    @Override
    public boolean hasConnection() {
        String address = DataMatrix.getString(HALKeys.DATA_BLE_HEART_RATE_ADDRESS_STRING);
        boolean ble = address != null && !address.isEmpty();
        boolean myZone = myZoneReader != null && myZoneReader.hasConnection();
        return ble || myZone;
    }

    public void setBLEHeartRate(int heartRate, HeartRateSource source) {
        this.heartRate = heartRate;
        this.heartRateSource = source;
        heartRateUpdated.heartRateUpdated();
    }


    // Handler for BLE connection
    private class HeartRateConnection extends BleManager implements ConnectionObserver {
        protected BluetoothGattCharacteristic hrCharacter = null;

        public HeartRateConnection(@NonNull Context context) {
            super(context);
            setConnectionObserver(this);
        }

        //region ConnectionObserver
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {}
        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            BLEHeartRateReader2.this.onConnection(device);
        }
        @Override
        public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {}
        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {}
        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {}
        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) { // "reason" constants are defined on ConnectionObserver
            // closing the connection for hand over to MyZone reader could trigger this callback and we do not want to force disconnect in that case
            if (myZoneReader == null || !myZoneReader.hasConnection()) BLEHeartRateReader2.this.disconnect(device);
        }
        //endregion ConnectionObserver

        public void queueHrRead() {
            if (hrCharacter != null && (hrCharacter.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                readCharacteristic(hrCharacter).with(updateHrCallback).enqueue();
            }
        }
        public void terminateConnectionAsync() {
            if (hrCharacter != null) disableNotifications(hrCharacter).enqueue();
            removeBond().enqueue();
            disconnect().enqueue(); // disconnect should internally call close for this connection
        }

        protected HeartRateMeasurementDataCallback updateHrCallback = new HeartRateMeasurementDataCallback() {
            @Override
            public void onHeartRateMeasurementReceived(@NonNull BluetoothDevice device, int heartRate, @Nullable Boolean contactDetected, @Nullable Integer energyExpanded, @Nullable List<Integer> rrIntervals) {
                // Update machine data.
                setBLEHeartRate(heartRate, HeartRateSource.BLE);
            }
        };

        @Override
        public void log(int priority, @NonNull String message) {
            //Log.println(priority, TAG, message)
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            BluetoothGattService service = gatt.getService(UUID.fromString(HEART_RATE_SERVICE));
            if (service != null) {
                hrCharacter = service.getCharacteristic(UUID.fromString(HEART_RATE_CHARACTERISTIC));
                if (hrCharacter != null) {
                    // should probably at least support READ or NOTIFY (many emulators only do NOTIFY)
                    int properties = hrCharacter.getProperties();
                    return ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0 ||
                            (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
                }
            }
            return false;
        }

        @Override
        protected void onServicesInvalidated() {
            hrCharacter = null;
        }

        @Override
        protected void initialize() {
            if (hrCharacter != null && (hrCharacter.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                setNotificationCallback(hrCharacter).with(updateHrCallback);
                enableNotifications(hrCharacter).enqueue();
            }
        }
    } // END CLASS: HeartRateConnection
}
