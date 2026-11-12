package com.jht.chimera.service.usbmedia;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.enums.JHTTypes;

public class UsbMediaService extends Service {
    private static final String TAG = "UsbMediaService";

    private MediaDeviceController mDeviceController;
    private BluetoothA2dp mA2dpService;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.d(TAG, "device ia attached: " + device);
                    if (mDeviceController == null) {
                        mDeviceController = new MediaDeviceController(UsbMediaService.this.getApplicationContext());
                    }
                    mDeviceController.setA2dpService(mA2dpService);
                    mDeviceController.checkDeviceType(device);
                } else if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    Log.d(TAG, "device ia detached: " + device);
                    if (mDeviceController.getDeviceType() == JHTTypes.MediaDeviceType.UMS) {
                        if (mDeviceController.getAttachedDevice() != null && device.equals(mDeviceController.getAttachedDevice())) {
                            mDeviceController.disconnect();
                            mDeviceController = null;
                            return;
                        }

                        int timeout = 50;
                        boolean isMount;
                        while(true) {
                            isMount = mDeviceController.getUSBPath().exists();
                            if(!isMount) {
                                break;
                            }

                            if(timeout == 0) {
                                break;
                            }

                            timeout--;

                            SystemClock.sleep(200);
                        }

                        if (!isMount) {
                            mDeviceController.disconnect();
                            mDeviceController = null;
                        }
                    } else {
                        if (device.equals(mDeviceController.getAttachedDevice())) {
                            mDeviceController.disconnect();
                            mDeviceController = null;
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ServiceStarter", "Starting " + this);
        init();
        Log.d("ServiceStarter", "Ending " + this);
    }

    private void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);

        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter != null) {
            mAdapter.getProfileProxy(this.getApplicationContext(), new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mA2dpService = (BluetoothA2dp) proxy;
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    mA2dpService = null;
                }
            }, BluetoothProfile.A2DP);
        }

        mDeviceController = new MediaDeviceController(UsbMediaService.this.getApplicationContext());
        mDeviceController.findDevice();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
