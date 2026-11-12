package com.jht.chimera.ui.bluetooth;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import com.jht.chimera.R;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
@SuppressLint({"DiscouragedPrivateApi", "PrivateApi", "MissingPermission"})
public class HeadphoneScanner extends BroadcastReceiver implements IBluetoothScanner {
    @NonNls private final String TAG = "HeadphoneScanner";
    private final BluetoothAdapter adapter;
    private boolean isScanning = false;
    private boolean startScanWhenA2DPServiceConnected = false;
    private final Context context;
    private BluetoothA2dp a2dpService;
    private IBluetoothScanResult scanResult;

    public HeadphoneScanner(Context context) {
        this(context, false);
    }

    public HeadphoneScanner(Context context, boolean clearDevices) {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
        if (adapter == null) return;

        Log.i(TAG, "HeadphoneScanner getProfileProxy " + Application.getProcessName());
        adapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                a2dpService = (BluetoothA2dp)proxy;
                if(clearDevices) {
                    clearDevices();
                }
                if (startScanWhenA2DPServiceConnected) {
                    scan(scanResult);
                    startScanWhenA2DPServiceConnected = false;
                }
            }
            @Override
            public void onServiceDisconnected(int profile) {
                a2dpService = null;
            }
        }, BluetoothProfile.A2DP);
    }

    @Override
    public BluetoothScannerType scannerType() {
        return BluetoothScannerType.HeadphoneScanner;
    }

    /**
     *
     * @return  The name as a resource id that we will show for this scanner list.
     */
    @Override
    public int name() {
        return R.string.speakers_headphones_tc;
    }

    /**
     *
     * @return  The icon as a resource id that we will show for this scanner list.
     */
    @Override
    public int icon() {
        return R.drawable.bt_pairing_headphones;
    }

    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult  Callback to let the UI know the results.
     */
    @Override
    public void scan(IBluetoothScanResult scanResult) {
        this.scanResult = scanResult;

        // Check to see if the service has connected yet.  If not then wait to start scan
        if (a2dpService != null) {
            if (!isScanning && adapter != null) {
                isScanning = true;
                adapter.enable();

                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    HeadphoneDevice headphoneDevice = new HeadphoneDevice(context, a2dpService, device);
                    headphoneDevice.registerBluetoothNotifications(); // already connected device so register for connection events
                    scanResult.deviceFound(headphoneDevice, null);
                }

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); // in case connected device does not show in the scan
                intentFilter.addAction("android.bluetooth.device.action.SDP_RECORD");
                context.registerReceiver(this, intentFilter);

                adapter.startDiscovery();
            }
        } else {
            startScanWhenA2DPServiceConnected = true;
        }
    }

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    @Override
    public void stopScan() {
        if(isScanning) {
            isScanning = false;
            scanResult = null;
            try {
                adapter.cancelDiscovery();
            } catch (Exception ignored) {}
            try {
                context.unregisterReceiver(this);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) switch (intent.getAction()) {
            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED: {
                // in case connected device does not show in the scan
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int connectState = a2dpService.getConnectionState(device);
                if (connectState == BluetoothA2dp.STATE_CONNECTED) {
                    reportDeviceIfHeadphone(device);
                }
                break;
            }
            case BluetoothDevice.ACTION_FOUND: {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                reportDeviceIfHeadphone(device);
                break;
            }
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                if (isScanning) {
                    adapter.startDiscovery();
                }
                break;
            }
            case "android.bluetooth.device.action.SDP_RECORD": { //NON-NLS
                // check if the device has the specified uuid
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    //intent.getIntExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", 0) == 0) {
                    Log.d(TAG, "Bluetooth device SDP " + device.getName() + ", " + device.getAddress() + ", " + intent.getIntExtra("android.bluetooth.device.extra.SDP_SEARCH_STATUS", 0) + " " + intent.hasExtra("android.bluetooth.device.extra.SDP_RECORD"));
                    reportDeviceIfHeadphone(device);
                }
                break;
            }
        }
    }

    private void reportDeviceIfHeadphone(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null || scanResult == null) return;

        // Note most documentation will tell you to call fetchUuidsWithSdp().  We cannot just call getUUID as we
        // used to with Android 4.3.  However that function is broken and you will always get null for the UUID.
        // This is because you must pair first which is pointless because we are trying to filter the list of devices
        // to pair.  Instead we use reflection to utilize the sdpSearch which does exactly what we need.

        ParcelUuid[] uuid = bluetoothDevice.getUuids();
        if(uuid != null) {
            try {
                Class<?> cls = Class.forName("android.bluetooth.BluetoothUuid");
                Method containAnyUuidMethod = cls.getDeclaredMethod("containsAnyUuid", ParcelUuid[].class, ParcelUuid[].class);
                if ((Boolean)containAnyUuidMethod.invoke(cls, uuid, HEADSET_UUIDS)) {
                    Log.d(TAG, "Bluetooth device has a headset UUID ");
                    scanResult.deviceFound(new HeadphoneDevice(context, a2dpService, bluetoothDevice), null);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        } else {
            BluetoothClass btClass = bluetoothDevice.getBluetoothClass();
            if (btClass != null) { // if class found in device.
                try {
                    Method doesClassMatchMethod = BluetoothClass.class.getDeclaredMethod("doesClassMatch", int.class);
                    boolean isA2dpDevice = (Boolean) doesClassMatchMethod.invoke(btClass, PROFILE_A2DP);
                    boolean isHeadsetDevice = (Boolean) doesClassMatchMethod.invoke(btClass, PROFILE_HEADSET);
                    if ((isA2dpDevice || isHeadsetDevice) && scanResult != null) {
                        Log.d(TAG, "Bluetooth device has a headset profile " + isA2dpDevice + " " + isHeadsetDevice);
                        scanResult.deviceFound(new HeadphoneDevice(context, a2dpService, bluetoothDevice), null);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        }
        /*try {
            Method m = device.getClass().getDeclaredMethod("sdpSearch", ParcelUuid.class);
            m.invoke(device, ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB"));
            m.invoke(device, ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB"));
        } catch (Exception e) {
            Log.w(TAG, e);
        }*/
    }


    private static final ParcelUuid[] HEADSET_UUIDS = {
            ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB"), 	// HSP
            ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB")	// Handsfree
    };
    private static final int PROFILE_A2DP = 1;
    private static final int PROFILE_HEADSET = 0;


    @Override
    public void clearDevices() {
        try {
            if(a2dpService == null) {
                return;
            }
            List<BluetoothDevice> connectedDevices = a2dpService.getConnectedDevices();

            for(BluetoothDevice device : connectedDevices) {
                new HeadphoneDevice(context, a2dpService, device).disconnect(false);
            }
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if(!connectedDevices.contains(device)) {
                    try {
                        Method removeBondMethod;
                        //noinspection JavaReflectionMemberAccess
                        removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
                        removeBondMethod.invoke(device);
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                }
            }
        }
        catch(Exception ignored) {}
    }

    @Override
    public void close() {
        if (adapter != null) {
            Log.i(TAG, "HeadphoneScanner closeProfileProxy | " + Application.getProcessName());
            adapter.closeProfileProxy(BluetoothProfile.A2DP,a2dpService);
        }
    }

}
