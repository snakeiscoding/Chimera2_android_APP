package com.jht.chimera.service.hal.lib.hardware;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

public class BluetoothHeadphoneManager {

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", BluetoothAdapter.ERROR);
            if (state == BluetoothA2dp.STATE_CONNECTED) {
                DataMatrix.putBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, true);
            } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                DataMatrix.putBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, false);
            }

        }
    };

    public static BluetoothHeadphoneManager getInstance() {
        return BluetoothHeadphoneManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final BluetoothHeadphoneManager INSTANCE = new BluetoothHeadphoneManager();
    }

    private BluetoothHeadphoneManager() { }

    public synchronized void start() {
        Context context = ContextUtil.getApplicationContext();
        if(context != null) {

            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            DataMatrix.putBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, false);

            // Check to see if we currently have BT headphones connected.
            if (adapter != null) {
                adapter.getProfileProxy(context.getApplicationContext(), new BluetoothProfile.ServiceListener() {
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        for (BluetoothDevice d : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
                            if (d != null && proxy.getConnectionState(d) == BluetoothProfile.STATE_CONNECTED) {
                                DataMatrix.putBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, true);
                                break;
                            }
                        }
                    }

                    public void onServiceDisconnected(int profile) {
                    }
                }, 2);
            }

            context.registerReceiver(btReceiver, new IntentFilter("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"));
        }
    }


    public synchronized void stop() {

        Context context = ContextUtil.getApplicationContext();
        if(context != null) {
            context.unregisterReceiver(btReceiver);
        }
    }
}
