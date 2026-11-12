package com.jht.chimera.lib.network;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.network.ethernet.NetworkSettingsImportExport;
import com.jht.chimera.lib.network.ethernet.NetworkSettingsManager;
import com.jht.chimera.lib.network.ethernet.SystemNetworkSettings;
import com.jht.chimera.lib.widget.network.WifiSelectorWidget;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.dialogs.EngDialogFactory;
import com.jht.chimera.ui.machinesettings.dialogs.NetworkSettingItem;
import com.jht.common.CmdProcessHelper;
import com.jht.datamatrixlib.DataMatrix;

import java.net.InetAddress;
import java.util.List;

/**
 * A widget that has all the necessary buttons for network setup. Shared between first time setup
 * and engineering screen.
 */
public class NetworkSetupWidget extends ConstraintLayout {
    private static final String TAG = "NetworkSetupWidget";

    private WifiSelectorWidget wifiSelectorWidget = null;
    private static final int CONNECT_TIMEOUT = 15000; // 15 seconds

    private SystemNetworkSettings wifiSettings = null;
    private SystemNetworkSettings ethernetSettings = null;
    private Handler handler;

    private WifiManager wifiManager;

    private Button buttonConnectAutomatic;
    private ImageView automaticConnectStatus;
    private MatrixNetwork autoConnectNetwork = null;

    private IndicatorStatus connectStatus = IndicatorStatus.None;
    private JHTTimer connectTimer = new JHTTimer();

    private boolean wifiEnabled = true;

    private enum IndicatorStatus {
        None,
        Checking,
        Success,
        Failure
    }

    private final BroadcastReceiver connectivityChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (connectStatus == IndicatorStatus.Checking) {
                    // Check if this is a new connection, set status correctly if we are waiting
                    Bundle extras = intent.getExtras();
                    boolean noConnectivity = false;
                    boolean failover = false;
                    if (extras != null) {
                        if (extras.containsKey(ConnectivityManager.EXTRA_IS_FAILOVER)) {
                            failover = extras.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
                        }
                        if (extras.containsKey(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
                            noConnectivity = extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
                        }
                    }

                    if (!failover && !noConnectivity) {
                        setConnectStatus(IndicatorStatus.Success);
                        connectTimer.stop();
                    }
                }

                Log.e(TAG, "networkBug: connectivityChange onReceive");
                Log.e(TAG, "networkFix: NetworkSetup --> connectivityChange onReceive - action: " + intent.getAction());
                refreshNetworkSettings();
                if (wifiSelectorWidget != null) wifiSelectorWidget.refreshResults();
            } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                if(ProductConfig.isCommercial()) {
                    processWifiScanResults(scanResults);
                } else {
                    if (wifiSelectorWidget != null)  wifiSelectorWidget.refreshResults();
                }
            }
        }
    };

    public NetworkSetupWidget(Context context) {
        super(context);
        init(context);
    }

    public NetworkSetupWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NetworkSetupWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.network_setup_widget, this, true);

        HandlerThread handlerThread = new HandlerThread("jht_network_setup");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());

        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        SelectorDefaultAnimations.setDefaultAnimations(getContext(), findViewById(R.id.network_settings_values));

        Button networkSettingsButton = findViewById(R.id.enter_wifi_settings);
        networkSettingsButton.setOnClickListener(v -> {
            connectTimer.stop();
            setConnectStatus(IndicatorStatus.None);
            Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            intent.putExtra(":settings:show_fragment_as_subsetting", true); // this will hide the Multi Panel of WiFi Settings
            context.startActivity(intent);
        });


        automaticConnectStatus = findViewById(R.id.connect_to_matrix_wifi_status);
        buttonConnectAutomatic = findViewById(R.id.connect_to_matrix_wifi);
        buttonConnectAutomatic.setEnabled(false); // Start this disabled, we enable it when we find a Matrix wifi network
        buttonConnectAutomatic.setOnClickListener(v -> {
            connectToMatrixWifi();
        });

        findViewById(R.id.eng_network_settings_eth_advanced).setOnClickListener(v -> EngDialogFactory.ipSettings(v, ethernetSettings));

        findViewById(R.id.eng_export_wifi).setOnClickListener(v -> {
            boolean exportWifiConfigStoreSuccessful = NetworkSettingsImportExport.usbExportWifiConfig();
            if (exportWifiConfigStoreSuccessful) {
                DialogFactory.message(v, getContext().getString(R.string.export_settings_disk_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            } else {
                DialogFactory.message(v, getContext().getString(R.string.export_settings_disk_fail_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
        });

        findViewById(R.id.eng_import_wifi).setOnClickListener(v -> {
            boolean importWifiConfigStoreSuccessful = NetworkSettingsImportExport.usbImportWifiConfig();
            if (importWifiConfigStoreSuccessful) {
                DialogFactory.message(v, getContext().getString(R.string.import_settings_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);

                if (context != null) {
                    Log.d(TAG, "DISABLING WIFI");
                    wifiManager.setWifiEnabled(false);
                    handler.postDelayed(() -> {
                        Log.d(TAG, "ENABLING WIFI");
                        wifiManager.setWifiEnabled(true);
                        // send broadcast which will trigger WifiConfigManager loadFromStore
                        Intent intent;
                        if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
                            context.sendBroadcast(new Intent("com.jht.import.wifi.settings"));
                        }
                        else {
                            intent = new Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED);
                            context.sendBroadcast(intent);
                        }
                    },2000);


                }
            } else {
                DialogFactory.message(v, getContext().getString(R.string.import_settings_fail_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
        });


        Button bandwidthTest = findViewById(R.id.eng_network_settings_bandwidth_test);
        if(bandwidthTest != null) {
            bandwidthTest.setOnClickListener(v -> DataMatrix.sendEvent(LoggerKeys.ACTION_SHOW_BANDWIDTH_TEST));
        }

        UserTextInput userTextInput = findViewById(R.id.eng_bandwidth_web_page);
        if (userTextInput != null) {
            userTextInput.setText(DataMatrix.getString(SettingKeys.Experimental.DATA_BANDWIDTH_TEST_URL_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.Experimental.DATA_BANDWIDTH_TEST_URL_STRING, s));
        }

        if(ProductConfig.isRetail()) {
            if(((Activity) context).getClass().getSimpleName().contains("MachineSetup")) {
                findViewById(R.id.eng_network_setup_network_settings).setVisibility(GONE);
            }
            wifiSelectorWidget = new WifiSelectorWidget(findViewById(R.id.eng_network_wireless_selection));
            wifiSelectorWidget.setOnNetworkConnecting(connecting -> {
                try {
                    if(connecting) {
                        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
                    } else {
                        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
                    }
                } catch(Exception e) { Log.e(TAG, "setOnNetworkConnecting " + connecting, e); }
            });
            wifiSelectorWidget.show();
            Button advanced = findViewById(R.id.eng_network_settings_wifi_advanced);
            if(((Activity) context).getClass().getSimpleName().contains("MachineSetup")) {
                advanced.setVisibility(GONE);
            } else {
                advanced.setOnClickListener(view -> EngDialogFactory.ipSettings(view, wifiSettings));
            }
        }
    }

    /**
     * Runs logic that should be run when the widget is shown
     */
    public void start() {
        buttonConnectAutomatic.setEnabled(false); // Disable this here to make sure it's not enabled when scanning

        refreshNetworkSettings();

        // Disable settings so that the back button in the settings takes us back to our app
        try {
            CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
            cmdProcessHelper.addCommand("pm disable com.android.settings/.Settings");
            cmdProcessHelper.execute();
        } catch (InterruptedException e) {
            Log.e(TAG, "Did not disable settings correctly!");
            e.printStackTrace();
        }

        try {
            IntentFilter connectivityFilter = new IntentFilter();
            connectivityFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            connectivityFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            getContext().registerReceiver(connectivityChange, connectivityFilter, null, handler);
            wifiManager.startScan(); // Start a scan, we'll enable the auto connect button if we find a Matrix network
        } catch (Exception ignored) {

        }
    }

    /**
     * Runs logic to stop the widget
     */
    public void finish() {
        connectTimer.stop();

        // Enable the settings we disabled when showing the screen
        try {
            CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
            cmdProcessHelper.addCommand("pm enable com.android.settings/.Settings");
            cmdProcessHelper.execute();
        } catch (InterruptedException e) {
            Log.e(TAG, "Did not enable settings correctly!");
            e.printStackTrace();
        }

        try {
            if (wifiSelectorWidget != null) wifiSelectorWidget.hide();
            getContext().unregisterReceiver(connectivityChange);
        } catch (Exception ignored) {
        }
    }

    private void setConnectStatus(IndicatorStatus status) {
        ((Activity) getContext()).runOnUiThread((Runnable) () -> {
            connectStatus = status;
            switch (status) {
                case None:
                    automaticConnectStatus.clearAnimation();
                    automaticConnectStatus.clearAnimation();
                    automaticConnectStatus.setImageDrawable(null);
                    buttonConnectAutomatic.setEnabled(true);
                    break;
                case Checking:
                    Animation rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.spinning_clockwise);
                    automaticConnectStatus.setImageResource(R.drawable.ic_spinner);
                    automaticConnectStatus.startAnimation(rotateAnimation);
                    buttonConnectAutomatic.setEnabled(false);
                    break;
                case Success:
                    automaticConnectStatus.clearAnimation();
                    automaticConnectStatus.setImageResource(R.drawable.ic_check_green_80dp);
                    buttonConnectAutomatic.setEnabled(true);
                    break;
                case Failure:
                    automaticConnectStatus.clearAnimation();
                    automaticConnectStatus.setImageResource(R.drawable.tv_x);
                    buttonConnectAutomatic.setEnabled(true);
                    break;
                default:
                    // Do nothing
                    break;
            }
        });
    }

    /**
     * Try to find one of our networks we want to automatically connect to. Enable the auto
     * connect button if we find something.
     *
     * @param results The wifi scan results
     */
    private void processWifiScanResults(List<ScanResult> results) {
        for (ScanResult result : results) {
            for (MatrixNetwork network : MatrixNetwork.values()) {
                if (result.SSID.equals(network.ssid)) {
                    autoConnectNetwork = network;
                    ((Activity) getContext()).runOnUiThread(() -> buttonConnectAutomatic.setEnabled(true));
                    return;
                }
            }
        }
    }

    private void connectToMatrixWifi() {
        if (autoConnectNetwork == null) return;

        connectTimer.setOnTimer(() -> setConnectStatus(IndicatorStatus.Failure));
        connectTimer.start(CONNECT_TIMEOUT);

        setConnectStatus(IndicatorStatus.Checking);

        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + autoConnectNetwork.ssid + "\"";
        wifiConfiguration.preSharedKey = "\"" + autoConnectNetwork.key + "\"";
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiManager.addNetwork(wifiConfiguration);

        @SuppressLint("MissingPermission")
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + autoConnectNetwork.ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

    private void refreshNetworkSettings() {
        NetworkSettingsManager networkSettingsManager = new NetworkSettingsManager();
        wifiSettings = networkSettingsManager.getWifiSettings(getContext());
        ethernetSettings = networkSettingsManager.getEthernetSettings(getContext());
        if (ethernetSettings == null) {
            ethernetSettings = new SystemNetworkSettings();
        }
        if (wifiSettings == null) {
            wifiSettings = new SystemNetworkSettings();
        }
        updateNetworkSettings();
    }

    private void updateNetworkSettings() {
        String[] subnetMasks = getContext().getResources().getStringArray(R.array.subnet_addresses_options);
        SelectorItemBase settingsRoot = new SelectorItemBase(null);
        settingsRoot.addItem(new NetworkSettingItem("", getContext().getResources().getString(R.string.wifi_network_tc), getContext().getResources().getString(R.string.ethernet_tc)));
        settingsRoot.addItem(new NetworkSettingItem(
                getContext().getResources().getString(R.string.mac_address_tc),
                AddressHelper.getMACAddressStr("wlan0", true), //NON-NLS
                AddressHelper.getMACAddressStr("eth0", true))); //NON-NLS
        settingsRoot.addItem(new NetworkSettingItem(
                getContext().getResources().getString(R.string.ip_address_tc),
                wifiSettings == null ? "" : wifiSettings.getIpAddress() == null ? "" : wifiSettings.getIpAddress().getHostAddress(),
                ethernetSettings == null ? "" : ethernetSettings.getIpAddress() == null ? "" : ethernetSettings.getIpAddress().getHostAddress()));
        settingsRoot.addItem(new NetworkSettingItem(
                getContext().getResources().getString(R.string.subnet_mask_tc),
                wifiSettings == null ? "" : subnetMasks[wifiSettings.getPrefixLength() - 8],
                ethernetSettings == null ? "" : subnetMasks[ethernetSettings.getPrefixLength() - 8]));
        settingsRoot.addItem(new NetworkSettingItem(
                getContext().getResources().getString(R.string.gateway_tc),
                wifiSettings == null ? "" : wifiSettings.getGateway() == null ? "" : wifiSettings.getGateway().getHostAddress(),
                ethernetSettings == null ? "" : ethernetSettings.getGateway() == null ? "" : ethernetSettings.getGateway().getHostAddress()));
        StringBuilder wifiDNS = new StringBuilder();
        StringBuilder ethernetDNS = new StringBuilder();
        if (wifiSettings != null && wifiSettings.getDnsServers() != null) {
            boolean newline = false;
            for (InetAddress address : wifiSettings.getDnsServers()) {
                if (newline) {
                    wifiDNS.append("\n");
                }
                wifiDNS.append(address.getHostAddress());
                newline = true;
            }
        }
        if (ethernetSettings != null) {
            boolean newline = false;
            for (InetAddress address : ethernetSettings.getDnsServers()) {
                if (newline) {
                    ethernetDNS.append("\n");
                }
                ethernetDNS.append(address.getHostAddress());
                newline = true;
            }
        }
        settingsRoot.addItem(new NetworkSettingItem("DNS", wifiDNS.toString(), ethernetDNS.toString())); // NON-NLS
        SelectorAdapterBase selectorAdapterBase = new SelectorAdapterBase(getContext(), settingsRoot, null);


        ((Activity) getContext()).runOnUiThread(() -> {
            if(wifiEnabled) {
                ((RecyclerView) findViewById(R.id.network_settings_values)).setAdapter(selectorAdapterBase);
                findViewById(R.id.eng_network_settings_eth_advanced).setEnabled(true);
            }
        });
    }

    public void enableButtons(boolean enabled){
        wifiEnabled = enabled;

        findViewById(R.id.enter_wifi_settings).setEnabled(enabled);
        findViewById(R.id.eng_import_wifi).setEnabled(enabled);
        findViewById(R.id.eng_export_wifi).setEnabled(enabled);
        findViewById(R.id.eng_network_settings_eth_advanced).setEnabled(enabled);
    }
}
