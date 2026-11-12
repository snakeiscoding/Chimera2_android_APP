package com.jht.chimera.lib.widget.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.jht.chimera.lib.network.MatrixNetwork;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
@SuppressLint("MissingPermission")
public class WifiConfigWrapper {
    @NonNls private static final String TAG = "WifiConfigWrapper";
    private static final int WIFI_AP_STATE_ENABLED = 13;

    private final Context mContext;
    private final WifiManager wm;

    List<WifiConfiguration> results;
    List<ScanResult> scanResults;

    public enum SECURITY_TYPE {
        WEP, WPA, OPEN
    }

    public interface IWifiConfigWrapperCallback {
        void scanFinished(List<ScanResult> wifiNetworkList);

        void connectSuccessful(String ssid);

        void networkDisconnected(String ssid);

        void authenticationFailure(String ssid);
    }

    IWifiConfigWrapperCallback mListener;

    public List<ScanResult> getWifiNetworkList() {
        return wm.getScanResults();
    }

    BroadcastReceiver scanResultStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            results = wm.getConfiguredNetworks();
            scanResults = wm.getScanResults();

            if (null != mListener) {
                mListener.scanFinished(scanResults);
            }
        }
    };

    BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SupplicantState supState;
            WifiInfo wifiInfo = wm.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();

            Log.d(TAG, "wifiStatusReceiver - onReceive: " + supState);

            if (intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
                if (null != mListener) {
                    Log.d(TAG, "Authentication Failed: " + wifiInfo.getSSID());
                    mListener.authenticationFailure(wifiInfo.getSSID());
                    return;
                }
            }

            if (supState.equals(SupplicantState.COMPLETED)) {
                if (null != mListener) {
                    Log.d(TAG, wifiInfo.toString());
                    mListener.connectSuccessful(wifiInfo.getSSID());
                }
            } else if (supState.equals(SupplicantState.DISCONNECTED)) {
                if (null != mListener) {
                    Log.d(TAG, "Disconnected: " + wifiInfo.getSSID());
                    mListener.networkDisconnected(wifiInfo.getSSID());
                }
            }
        }
    };

    public WifiConfigWrapper(Context context) {
        mContext = context;
        wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        IntentFilter scanResutlsFilter = new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(scanResultStatusReceiver, scanResutlsFilter);

        IntentFilter wifiStateChangedfilter = new IntentFilter(
                WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        wifiStateChangedfilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        wifiStateChangedfilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiStateChangedfilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStatusReceiver, wifiStateChangedfilter);
        wm.startScan();
    }

    public void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(scanResultStatusReceiver);
            mContext.unregisterReceiver(wifiStatusReceiver);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void setListener(IWifiConfigWrapperCallback listener) {
        mListener = listener;
    }

    // Start scan for wifi networks
    public void scanForNetwork() {
        wm.startScan();
    }

    public boolean isWifiConnected() {
        boolean result = false;
        WifiInfo wifiInfo = wm.getConnectionInfo();
        if (null != wifiInfo
                && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            result = true;
        }
        return result;
    }

    public int getWifiStrength(int numLevels) {
        try {
            int rssi = wm.getConnectionInfo().getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, numLevels);
            return level;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getConnectedSSID() {
        String connectedSSID = "";
        WifiInfo wifiInfo = wm.getConnectionInfo();
        if (null != wifiInfo
                && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            connectedSSID = wifiInfo.getSSID();
        }
        return connectedSSID;
    }

    public boolean isNetworkConfigured(String ssid) {
        Log.d(TAG, "Configured network list:");
        boolean result = false;
        List<WifiConfiguration> configNetworks = wm.getConfiguredNetworks();
        if (configNetworks != null) {
            for (WifiConfiguration network : configNetworks) {
                Log.d(TAG, network.SSID);
                if (network.SSID.contains(ssid)) {
                    result = true;
                }
            }
        }
        return result;
    }

    public static boolean scanResultIsOpen(ScanResult result) {
        if (result.capabilities.contains("WEP"))
            return false;
        else if (result.capabilities.contains("PSK"))
            return false;
        else if (result.capabilities.contains("EAP"))
            return false;
        return true;
    }


    public boolean isNetworkConfigured(String ssid, boolean hidden) {
        Log.d(TAG, "Configured network list:");
        boolean result = false;
        List<WifiConfiguration> configNetworks = wm.getConfiguredNetworks();
        if (configNetworks != null) {
            for (WifiConfiguration network : configNetworks) {
                Log.d(TAG, network.SSID);
                if (network.SSID.contains(ssid) && network.hiddenSSID == hidden) {
                    result = true;
                }
            }
        }
        return result;
    }


    public int numNetworks() {
        List<WifiConfiguration> configNetworks = wm.getConfiguredNetworks();
        return configNetworks == null ? 0 : configNetworks.size();
    }

    public void enable() {
        Log.d(TAG, "ENABLING WIFI");
        wm.setWifiEnabled(true);
    }

    public void disable() {
        Log.d(TAG, "DISABLING WIFI");
        wm.setWifiEnabled(false);
    }

    public boolean isEnabled() {
        return wm.isWifiEnabled();
    }

    @SuppressWarnings("deprecation")
    public void connect(String networkSSID, String capabilities,
                        String networkPass, boolean isHiddenSSID) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";

        SECURITY_TYPE security = stringToSecurityType(capabilities);

        switch (security) {
            case WEP:
                // WEP
                conf.wepKeys[0] = networkPass; // passcode must be hex and no quotes needed
                conf.wepTxKeyIndex = 0;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                break;

            case WPA:
                // WPA
                conf.preSharedKey = "\"" + networkPass + "\"";
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                break;

            case OPEN:
                // Open
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
        }

        conf.hiddenSSID = isHiddenSSID;

        wm.disconnect();
        int netId = wm.addNetwork(conf);
        boolean result = wm.enableNetwork(netId, true);
        Log.d(TAG, "wm.enableNetwork: " + result);
        if (!result) {
            mListener.authenticationFailure(networkSSID);
        } else {
            wm.saveConfiguration();
        }
    }

    public void disconnect(String networkSSID) {
        List<WifiConfiguration> configuredNetworkList = wm
                .getConfiguredNetworks();
        networkSSID = "\"" + networkSSID + "\"";

        if (configuredNetworkList != null) {
            for (WifiConfiguration wc : configuredNetworkList) {
                if ((null != wc.SSID) && (wc.SSID.compareTo(networkSSID) == 0)) {
                    wm.removeNetwork(wc.networkId);
                }
            }
        }
    }

    public List<ScanResult> getShortList(List<ScanResult> wifiNetworks) {
        List<ScanResult> shortList = new ArrayList<ScanResult>();
        for (ScanResult sr : wifiNetworks) {
            ScanResult newScanItem = sr;
            for (ScanResult sl : shortList) {
                if (sl.SSID.compareTo(sr.SSID) == 0) {
                    if (sl.level > sr.level) {
                        sl = sr;
                    }
                    newScanItem = null;
                }
            }

            if (null != newScanItem) {
                shortList.add(newScanItem);
            }
        }

        return shortList;
    }

    public List<ScanResult> getShortList() {
        return getShortList(scanResults);
    }

    public SECURITY_TYPE stringToSecurityType(String cap) {
        if (cap.contains("WPA")) {
            return SECURITY_TYPE.WPA;
        } else if (cap.contains("WEP")) {
            return SECURITY_TYPE.WEP;
        }

        return SECURITY_TYPE.OPEN;
    }

    @SuppressWarnings("deprecation")
    public void saveConfiguration() {
        wm.saveConfiguration();
    }

    public void addNPWireless() {
        final String ssid = MatrixNetwork.NpWireless.ssid;
        final String security = "WPA2-PSK"; //NON-NLS
        final String key = MatrixNetwork.NpWireless.key;

        if (!isNetworkConfigured(ssid)) {
            connect(ssid, security, key, true);
            saveConfiguration();
        }
    }

    public void turnOnWifiAccess() {
        //Turn on access point
        Thread thread = new Thread(() -> {
            try {
                // Gets state of access point
                Method getWifiApStateMethod = wm.getClass().getMethod("getWifiApState");
                int apState = (Integer) getWifiApStateMethod.invoke(wm);

                /* setWifiApEnabled was removed on Android 9 and this appears to be needed for Passport
                //If hotspot is enabled, turn off hotspot
                if (apState == WIFI_AP_STATE_ENABLED) {
                    Method setWifiApEnabled = wm.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                    setWifiApEnabled.invoke(wm, null, false);
                }
                */

                // While wifi is disabled, attempt to turn it on
                int tries = 0;
                while (!wm.isWifiEnabled()) {
                    Log.d(TAG, "ENABLING WIFI");
                    wm.setWifiEnabled(true);
                    Thread.sleep(1000);
                    tries++;
                    if (tries >= 3) break;
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }, "turnOnWifiAccess");
        thread.start();
    }

    public void removeAllWireless() {
        List<WifiConfiguration> results;

        results = wm.getConfiguredNetworks();
        if (results != null) {
            for (WifiConfiguration config : results) {
                wm.removeNetwork(config.networkId);
            }
        }
    }

    public void removeNPWireless() {
        List<WifiConfiguration> results;

        results = wm.getConfiguredNetworks();
        if (results != null) {
            for (WifiConfiguration config : results) {
                if (config.SSID.startsWith("\"npwireless")) { //NON-NLS
                    wm.removeNetwork(config.networkId);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean hasWifiHardware(Context context) {

        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (connectivityManager == null)
            return false;

        for (NetworkInfo nifo : connectivityManager.getAllNetworkInfo())
            if (nifo.getType() == ConnectivityManager.TYPE_WIFI)
                return true;

        return false;
    }
}
