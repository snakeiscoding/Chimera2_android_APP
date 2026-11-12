package com.jht.chimera.lib.widget.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.jht.chimera.lib.common.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 */
class NetworkController {

    public interface IOnNetworkEvent {
        void onResult(String ssid, boolean success);
        void onScanUpdated(List<SSID> scanResults);
    }

    private static final String TAG = "NetworkController";
    private final WifiManager wm;
    private final List<SSID> scanResults = new ArrayList<>();
    private final HandlerThread handlerThread;
    private final Handler handler;
    private String currentSSID = "";
    private boolean forgetNetwork = false;

    public void refreshResults() {
        handler.post(() -> {
            for (SSID ssid : scanResults) {
                if (ssid.state == SSID.SSIDState.CONNECTED) {
                    Log.e(TAG, "NetworkController --> PRIOR CASE. ssid state is connected WITH " + ssid.scanResult.SSID);
                }
            }
            WifiInfo wifiInfo = wm.getConnectionInfo();
            if(wifiInfo != null) {
                Log.e(TAG, "NetworkController --> wifiInfo: " + wifiInfo.getSSID() + ", state: " + wifiInfo.getSupplicantState().name());

                boolean[] oldScanFound = new boolean[scanResults.size()];

                List<ScanResult> results = wm.getScanResults();
                List<WifiConfiguration> configuration = wm.getConfiguredNetworks();
                for (ScanResult result : results) {
                    boolean found = false;
                    if (result.SSID == null || result.SSID.equals("")) {
                        continue;
                    }

                    for (int i = 0; i < scanResults.size(); i++) {
                        SSID ssid = scanResults.get(i);
                        if (result.SSID.equals(ssid.scanResult.SSID)) {
                            found = true;
                            // Check to see if we need to use the new value or the old value.
                            if (oldScanFound.length <= i || oldScanFound[i]) {
                                // This is our second time seeing this SSID.
                                if (ssid.scanResult.level <= result.level) {
                                    // This is the same SSID, but the signal is weaker.
                                    // Use the original scan.
                                    continue;
                                }
                            }
                            setSSIDState(wifiInfo, ssid);

                            // This is either the first time seeing this in the current scan
                            // or the signal is stronger so use this scan result.
                            if (oldScanFound.length > i) {
                                // Mark this scan as having been found.
                                oldScanFound[i] = true;
                            }

                            ssid.scanResult = result;
                            ssid.open =
                                    !result.capabilities.contains("WEP") && !result.capabilities.contains("PSK") && !result.capabilities.contains("EAP");
                            ssid.hidden = false;
                            ssid.configured = false;
                            for (WifiConfiguration wifiConfiguration : configuration) {

                                if (wifiConfiguration.SSID.contains(result.SSID)) {
                                    ssid.hidden = wifiConfiguration.hiddenSSID;
                                    ssid.configured = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    if (!found) {
                        SSID ssid = new SSID();
                        ssid.scanResult = result;
                        ssid.open =
                                !result.capabilities.contains("WEP") && !result.capabilities.contains("PSK") && !result.capabilities.contains("EAP");
                        ssid.hidden = false;

                        for (WifiConfiguration wifiConfiguration : configuration) {
                            if (wifiConfiguration.SSID.contains(result.SSID)) {
                                ssid.hidden = wifiConfiguration.hiddenSSID;
                                ssid.configured = true;
                                break;
                            }
                        }
                        setSSIDState(wifiInfo, ssid);
                        scanResults.add(ssid);
                    }
                }

                // Remove old scans that are no longer seen.  Go backwards as we are removing
                // from a list and this will prevent us from changing the index.
                for (int i = oldScanFound.length; i > 0; i--) {
                    if (!oldScanFound[i - 1]) {
                        scanResults.remove(i - 1);
                    }
                }

                for (SSID ssid : scanResults) {
                    if (ssid.state == SSID.SSIDState.CONNECTED) {
                        Log.e(TAG, "NetworkController --> ssid state is connected WITH " + ssid.scanResult.SSID);
                    }
                }
                onNetworkEvent.onScanUpdated(scanResults);
            }
        });
    }

    private final BroadcastReceiver onScanResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "NetworkController --> onScanResult onReceive - action: " + intent.getAction());
            Log.d(TAG, "onScanResult onReceive");
            refreshResults();
            handler.postDelayed(wm::startScan, 5000);
        }
    };

    private void setSSIDState(WifiInfo wifiInfo, SSID ssid) {

        if(wifiInfo != null && wifiInfo.getSSID().equals("\"" + ssid.scanResult.SSID + "\"")) {
            switch(wifiInfo.getSupplicantState()) {
                case ASSOCIATED:
                case ASSOCIATING:
                case AUTHENTICATING:
                case FOUR_WAY_HANDSHAKE:
                case GROUP_HANDSHAKE:
                case SCANNING:
                    ssid.state = SSID.SSIDState.CONNECTING;
                    currentSSID = ssid.scanResult.SSID;
                    break;
                case COMPLETED:
                    Log.e(TAG, "setSSIDState CONNECTED, ssid: " + ssid.scanResult.SSID);
                    ssid.state = SSID.SSIDState.CONNECTED;
                    currentSSID = ssid.scanResult.SSID;
                    break;
                default:
                    ssid.state = SSID.SSIDState.IDLE;
                    break;
            }
        } else {
            ssid.state = SSID.SSIDState.IDLE;
        }
        Log.d(TAG, "setSSIDState state = " + ssid.scanResult.SSID + " " + ssid.state);
    }

    private final BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "wifiStatusReceiver - onReceive: " + intent.getAction());

            Log.e(TAG, "wifiStatusReceiver onReceive");
            handler.post(() -> {

                SupplicantState supState;

                if (intent.getAction() != null && intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

                    supState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                    WifiInfo wifiInfo = wm.getConnectionInfo();

                    if (intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING || SupplicantState.DISCONNECTED.equals(supState)) {
                        Log.e(TAG, "NetworkController --> wifiStatusReceiver onReceive - action: " + intent.getAction());

//                        for(SSID ssid : scanResults) {
//                            if(ssid.scanResult.SSID.equals(currentSSID)) {
//                                ssid.state = SSID.SSIDState.IDLE;
//                                break;
//                            }
//                        }

                        //cases:
                        //1. removeNetwork (forget network) - you have to be connected to the network first
                        //a. make ssid IDLE
                        //b. set currentSSID to empty
                        //c. onScanUpdated to update UI
                        //d. scanNetworks
                        //2. getting to the network setup page with no networks connected
                        //
                        //3. disconnecting to connect to another network
                        //4. bad password - when you enter a bad password
                        //a. not connected to any network
                        //b. already connected to a network

                        if(intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING ) {
                            Log.e(TAG, "NetworkController --> wifiStatusReceiver onReceive - authentication error");

                            onNetworkEvent.onResult(currentSSID, false);
                        } else if(forgetNetwork){

                            Log.e(TAG, "NetworkController --> wifiStatusReceiver onReceive - forget network");
                            forgetNetwork = false;

                        } else {
                            if( wifiInfo.getSSID().contains(currentSSID) ) {
                                Log.e(TAG, "NetworkController --> wifiStatusReceiver onReceive - disconnect case");
                                onNetworkEvent.onResult(currentSSID, false);
                            }
                        }

                        //currentSSID = "";
                        scanNetworks();
                        //onNetworkEvent.onScanUpdated(scanResults);
                    }
                    else if (SupplicantState.COMPLETED.equals(supState)) {
                        Log.e(TAG, "NetworkController --> wifiStatusReceiver onReceive - COMPLETED");
                        for(SSID ssid : scanResults) {
                            if(ssid.scanResult.SSID.equals(currentSSID)) {
                                ssid.state = SSID.SSIDState.CONNECTED;
                                Log.e(TAG, "NetworkController --> current ssid = " + currentSSID);
                                break;
                            }
                        }
                        scanNetworks();
                        onNetworkEvent.onScanUpdated(scanResults);
                        //onNetworkEvent.onResult(currentSSID, true);
                        currentSSID = "";

                    }

                }
                // onNetworkEvent.onScanUpdated(scanResults);
                //onNetworkEvent.onScanUpdated(scanResults);
            });
        }
    };


    private WifiConfigWrapper.SECURITY_TYPE stringToSecurityType(String cap) {
        if (cap.contains("WPA")) {
            return WifiConfigWrapper.SECURITY_TYPE.WPA;
        } else if (cap.contains("WEP")) {
            return WifiConfigWrapper.SECURITY_TYPE.WEP;
        }

        return WifiConfigWrapper.SECURITY_TYPE.OPEN;
    }

    private final IOnNetworkEvent onNetworkEvent;
    private final Context context;


    NetworkController(Context context, IOnNetworkEvent onNetworkEvent) {

        this.onNetworkEvent = onNetworkEvent;
        this.context = context;

        WifiManagerHelper.TurnWifiOn(context);

        wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        handlerThread = new HandlerThread("jht_wifi_scanner");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        context.registerReceiver(wifiStatusReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION), null, handler);

    }


    void clearResults() {
        scanResults.clear();
    }


    private boolean scanning = false;
    void scanNetworks() {
        if(!scanning) {
            wm.startScan();
            context.registerReceiver(onScanResult, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
        scanning = true;
    }

    void stopScanningNetworks() {
        if(scanning) {
            context.unregisterReceiver(onScanResult);
        }
        scanning = false;
    }



    void connectToNetwork(String ssid, String capabilities, String password, boolean isHidden) {

        currentSSID = ssid;
        new Thread(() -> {
            try {
                stopScanningNetworks();

                for(SSID ssid2 : scanResults) {
                    if(ssid2.scanResult.SSID.equals(ssid)) {
                        ssid2.state = SSID.SSIDState.CONNECTING;
                    }

                    if(ssid2.state == SSID.SSIDState.CONNECTED) {
                        ssid2.state = SSID.SSIDState.IDLE;
                        Log.e(TAG, "there is already a connected network");
                    }
                }
                onNetworkEvent.onScanUpdated(scanResults);

                // If this is already configured try to use that network.
                for(WifiConfiguration configuration : wm.getConfiguredNetworks()) {
                    Log.e(TAG, "NetworkController --> connectToNetwork - configured sid: " + configuration.SSID);
                    if(configuration.SSID != null && configuration.SSID.equals("\"" + ssid + "\"")) {
                        Log.e(TAG, "NetworkController --> connectToNetwork - this is already configured and id to connect is: " + configuration.networkId);
                        wm.disconnect();
                        wm.enableNetwork(configuration.networkId, true);
                        wm.reconnect();
                        return;
                    }
                }

                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + ssid + "\"";

                WifiConfigWrapper.SECURITY_TYPE security = stringToSecurityType(capabilities);

                switch (security) {
                    case WEP:
                        conf.wepKeys[0] = processPasswordForWep(password);
                        conf.wepTxKeyIndex = 0;
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                        break;

                    case WPA:
                        // WPA
                        conf.preSharedKey = "\"" + password + "\"";
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                        break;

                    case OPEN:
                        // Open
                        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        break;
                }

                conf.hiddenSSID = isHidden;

                Log.e(TAG, "about to disconnect");
                wm.disconnect();
                int netId = wm.addNetwork(conf);
                Log.e(TAG, "NetworkController --> connectToNetwork - about to enable network with id: " + netId);

                if(netId == -1) {
                    // Error with the configuration.  Unable to connect.
                    onNetworkEvent.onResult(currentSSID, false);
                }
                else {
                    wm.enableNetwork(netId, true);
                }
                wm.reconnect();
            }
            catch(Exception ex) {
                Log.w(TAG, ex);
            }
        }).start();
    }


    void connectToHiddenNetwork(String ssid, int security, String password) {
        handler.post(() -> {

            stopScanningNetworks();

            WifiConfiguration conf = getHiddenWifiConfiguration(ssid, password, security);
            if(conf == null) {
                onNetworkEvent.onResult(ssid, false);
                return;
            }

            wm.disconnect();
            int netId = wm.addNetwork(conf);
            wm.enableNetwork(netId, true);
            wm.reconnect();
        });
    }

    private WifiConfiguration getConfiguration(String ssid) {
        List<WifiConfiguration> networks = wm.getConfiguredNetworks();
        // If all the networks have been removed for some reason, existingConfigs is null, and
        // have to check for it before the 'for' loop to avoid a crash when manually adding a
        // wifi network.
        if( networks != null )
        {
            for (WifiConfiguration network : networks)
            {
                if (network.SSID.equals("\"" + ssid + "\""))
                {
                    return network;
                }
            }
        }
        return null;
    }

    private WifiConfiguration getHiddenWifiConfiguration(String ssid, String password, int securityType) {
        String TAG = "add wifi config"; //NON-NLS
        Log.i(TAG, "SSID:" + ssid + ",password:" + password);

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        WifiConfiguration currentConfig = getConfiguration(ssid);
        if(currentConfig != null) {
            wm.removeNetwork(currentConfig.networkId);
        }

        switch(securityType) {
            case 1: // WIFICIPHER_NOPASS
                config.wepKeys[0] = "";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case 2: // WIFICIPHER_WEP
                Log.i(TAG, "Type =2.");
                config.hiddenSSID = true;
                config.wepKeys[0] = processPasswordForWep(password);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case 3: // WIFICIPHER_WPA
            case 4: // WPA2 PSK
                config.preSharedKey = "\"" + password + "\"";
                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // WPA
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // WPA2
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.status = WifiConfiguration.Status.ENABLED;
                break;
            default:
                return null;
        }
        return config;
    }

    void forgetNetwork(String ssid) {
        handler.post( () -> {
            stopScanningNetworks();
            List<WifiConfiguration> configuredNetworkList = wm.getConfiguredNetworks();
            String ssidQuoted = "\"" + ssid + "\"";

            for(SSID ssid2 : scanResults) {
                if(ssid2.scanResult.SSID.equals(ssid)) {
                    ssid2.state = SSID.SSIDState.IDLE;
                    onNetworkEvent.onScanUpdated(scanResults);
                }
            }


            if (configuredNetworkList != null) {
                for (WifiConfiguration wc : configuredNetworkList) {
                    if ((null != wc.SSID) && (wc.SSID.compareTo(ssidQuoted) == 0)) {
                        forgetNetwork = true;
                        wm.removeNetwork(wc.networkId);
                        break;
                    }
                }
            }
        });
    }

    boolean isConfigured(String ssid) {
        List<WifiConfiguration> configuredNetworkList = wm.getConfiguredNetworks();
        String ssidQuoted = "\"" + ssid + "\"";

        if (configuredNetworkList != null) {
            for (WifiConfiguration wc : configuredNetworkList) {
                if ((null != wc.SSID) && (wc.SSID.compareTo(ssidQuoted) == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    void destroy() {
        try {
            context.unregisterReceiver(wifiStatusReceiver); // will throw IllegalArgumentException if not registered
        }
        catch (Exception ignored) {}
        handlerThread.quit();
    }

    // Maybe we need to add a checkbox to entry dialog for HEX vs ASCII password?
    // If ASCII password is all hex chars, this will screw up unless the user adds their own quotes?
    String processPasswordForWep(String value) {
        if (value == null) return null;
        if (value.startsWith("\"") && value.endsWith("\"")) return value; // already quoted
        if (StringUtil.isHexadecimal(value)) return value; // HEX password
        return "\"" + value + "\""; // ASCII password
    }

}
