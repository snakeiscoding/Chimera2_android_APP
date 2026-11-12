package com.jht.chimera.service.systemsettings;

import static com.jht.androidcommonalgorithms.network.AddressHelper.getIPAddress;
import static com.jht.chimera.service.systemsettings.SystemSettings.getConsoleName;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.net.DatagramPacket;
import java.net.MulticastSocket;

class ListenForBroadcast {
    
    private final Context context;
    private final JHTNotification notification = new JHTNotification();
    private int lastNonce = 0;
    private final byte[] receiveBuffer = new byte[4095];
    private MulticastSocket ms = null;

    ListenForBroadcast(Context context, DataMatrix dataMatrix) {
        this.context = context;
        dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, b -> checkThread(), false);
        dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, b -> checkThread(), false);
        dataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, status -> checkThread(), false);
        new Thread(this::listen).start();
        notification.setNotification();
    }

    private boolean shouldBeRunning() {
        return
            DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL) && (
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL) ||
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL)
            );
    }

    private void checkThread() {
        if (shouldBeRunning()) {
            notification.setNotification();
        } else if (null != ms) {
            ms.close();
        }
    }

    private void listenForBroadcast() {
        WifiManager.MulticastLock multicastLock = null;
        try {
            // Get the multicast lock.
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            multicastLock = wifi.createMulticastLock("multicastLock2");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();

            // Create the multicast interface.
            ms = new MulticastSocket(1600);
            DatagramPacket dp = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            String receivedData = "";
            while (!receivedData.equals(SystemSettings.EXIT)) {
                // Get the packet.
                ms.receive(dp);

                // Read the data.
                receivedData = new String(receiveBuffer, 0, dp.getLength());
                if (!receivedData.contains(SystemSettings.EXIT) && !receivedData.contains("Address")) {
                    parseIncomingMessage(receivedData);
                }
            }

        } catch (Exception ignored) {
        } finally {
            // Release multicast lock.
            if (ms != null) {
                ms.close();
                ms = null;
            }
            if (multicastLock != null) {
                multicastLock.release();
            }
        }
    }

    public void listen() {
        while (notification.waitForNotification()) {
            if (shouldBeRunning()) {
                listenForBroadcast();
            }
            notification.reset();
        }
    }

    private void parseIncomingMessage(final String xml) {
        SettingsAvailMessage settings = new SettingsAvailMessage(xml);
        if(!getIPAddress().equalsIgnoreCase(settings.host) &&
                settings.console.equalsIgnoreCase(getConsoleName().toLowerCase()) &&
                lastNonce != settings.nonce)
        {
            lastNonce = settings.nonce;
            new ExportClient(context, settings.host, settings.settingsPort);
        }
    }

}
