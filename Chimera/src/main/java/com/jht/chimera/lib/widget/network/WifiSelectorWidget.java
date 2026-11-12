package com.jht.chimera.lib.widget.network;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.R;
import com.jht.chimera.lib.widget.dialog.NetworkPasswordEntry;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;

import java.util.ArrayList;
import java.util.List;

public class WifiSelectorWidget implements NetworkController.IOnNetworkEvent {

    public interface IOnNetworkConnecting {
        void networkConnectingState(boolean connecting);
    }
    private static final String TAG = "WifiSelectorWidget";
    private StandardSelectorItemAdapter adapter;
    private SelectorItemBase networkSelectors;
    private SSID other;
    private LinearLayoutManager layoutManager;
    private final RecyclerView wifiSelector;
    private String forgottenSSID = "";
    private boolean connectingToNetwork = false;
    private boolean enteringPassword = false;

    private NetworkController networkController;

    private final Handler handler;
    private IOnNetworkConnecting onNetworkConnecting;

    private final NetworkPasswordEntry.IOnEntryComplete onPasswordEntry = new NetworkPasswordEntry.IOnEntryComplete() {

        @Override
        public void onCancel() {
            enteringPassword = false;
            refreshResults();
        }

        @Override
        public void onEntry(NetworkPasswordEntry.PasswordEntryMode mode, SSID ssidInfo, String ssid, int security, String password) {
            enteringPassword = false;
            if(mode == NetworkPasswordEntry.PasswordEntryMode.HIDDEN) {
                forgottenSSID = "";
                updateNetworkConnectingState(true);
                // Correct difference between option item index and security type for hidden network
                networkController.connectToHiddenNetwork(ssid, security + 1, password);

            } else {
                forgottenSSID = "";
                updateNetworkConnectingState(true);
                networkController.connectToNetwork(ssid, ssidInfo.scanResult.capabilities, password, ssidInfo.hidden);
            }
        }
    };

    public WifiSelectorWidget(final RecyclerView wifiSelector) {

        this.wifiSelector = wifiSelector;
        handler = new Handler();
    }

    public void setOnNetworkConnecting(IOnNetworkConnecting onNetworkConnecting) {
        this.onNetworkConnecting = onNetworkConnecting;
    }

    private void updateNetworkConnectingState(boolean connecting) {
        connectingToNetwork = connecting;
        if(onNetworkConnecting != null) {
            onNetworkConnecting.networkConnectingState(connecting);
        }
    }

    private void updateNetworkSettings(List<SSID> scanResults) {

        int active = -1;
        // Remove other.
        if(networkSelectors.numChildren() > 0) {
            networkSelectors.removeItem(networkSelectors.numChildren() - 1);
        }

        for(SSID ssid : scanResults) {
            boolean found = false;
            for(int i = 0; i < networkSelectors.numChildren(); i++) {
                ISelectorItem item = networkSelectors.getChild(i);
                if(item instanceof WifiSelectorItem) {
                    WifiSelectorItem wifiItem = (WifiSelectorItem)item;
                    if(wifiItem.ssid().scanResult.SSID.equals(ssid.scanResult.SSID)) {
                        wifiItem.ssid(ssid);
                        found = true;

                        if(!ssid.scanResult.SSID.equals(forgottenSSID) && (ssid.state == SSID.SSIDState.CONNECTED || ssid.state == SSID.SSIDState.CONNECTING)) {
                            active = i;
                        }
                        break;
                    }
                }
            }
            if(!found) {
                networkSelectors.addItem(new WifiSelectorItem(wifiSelector.getContext(), ssid));
                if(ssid.state == SSID.SSIDState.CONNECTED || ssid.state == SSID.SSIDState.CONNECTING) {
                    active = networkSelectors.numChildren() - 1;
                }
            }
        }

        networkSelectors.setSelection(active);
        networkSelectors.addItem(new WifiSelectorItem(wifiSelector.getContext(), other));
        adapter.notifyDataSetChanged();
    }

    public void show() {

        networkController = new NetworkController(wifiSelector.getContext(), this);
        other = new SSID();
        other.scanResult = null;


        networkSelectors = new SelectorItemBase(new ArrayList<>());
        adapter = new StandardSelectorItemAdapter(wifiSelector.getContext(), networkSelectors, item -> {
            if(item instanceof WifiSelectorItem) {
                final WifiSelectorItem wifiItem = (WifiSelectorItem)item;

                View view = layoutManager.findViewByPosition((item.getIndex()));
                if(view == null) view = wifiSelector;
                if(wifiItem.ssid().scanResult == null) {
                    enteringPassword = true;
                    ProjectDialogFactory.networkPassword(view, other, NetworkPasswordEntry.PasswordEntryMode.HIDDEN, onPasswordEntry);
                } else {
                    if(networkController.isConfigured(wifiItem.ssid().scanResult.SSID)) {
                        switch (wifiItem.ssid().state) {
                            case IDLE:
                                forgottenSSID = "";
                                updateNetworkConnectingState(true);
                                networkController.connectToNetwork(wifiItem.ssid().scanResult.SSID, wifiItem.ssid().scanResult.capabilities, "", wifiItem.ssid().hidden);
                                break;
                            case CONNECTING:
                            case CONNECTED:
                                // Forget network.
                                ProjectDialogFactory.confirm(wifiSelector.getRootView(), wifiSelector.getContext().getResources().getString(R.string.do_you_want_to_forget_network), () -> {

                                    networkController.forgetNetwork(wifiItem.ssid().scanResult.SSID);
                                    forgottenSSID = wifiItem.ssid().scanResult.SSID;
                                    networkSelectors.setSelection(-1);
                                    adapter.notifyDataSetChanged();
                                }, null);
                                break;
                        }
                    }
                    else if(wifiItem.ssid().open) {
                        // Wifi is open. Connect.
                        forgottenSSID = "";
                        updateNetworkConnectingState(true);
                        networkController.connectToNetwork(wifiItem.ssid().scanResult.SSID, wifiItem.ssid().scanResult.capabilities, "", wifiItem.ssid().hidden);
                    }
                    else if(wifiItem.ssid().scanResult.SSID.startsWith("npwireless")) {
                        forgottenSSID = "";
                        updateNetworkConnectingState(true);
                        networkController.connectToNetwork(wifiItem.ssid().scanResult.SSID, wifiItem.ssid().scanResult.capabilities, "QvnDjg8PZodeN34LZSpl", false); //NON-NLS
                    }
                    else if(wifiItem.ssid().scanResult.SSID.startsWith("MXWireless")) {
                        forgottenSSID = "";
                        updateNetworkConnectingState(true);
                        networkController.connectToNetwork(wifiItem.ssid().scanResult.SSID, wifiItem.ssid().scanResult.capabilities, "FuWxzbbocfOt", false); //NON-NLS
                    }
                    else if(wifiItem.ssid().scanResult.capabilities.contains("WEP")) {
                        enteringPassword = true;
                        ProjectDialogFactory.networkPassword(view, wifiItem.ssid(), NetworkPasswordEntry.PasswordEntryMode.WEP, onPasswordEntry);
                    }
                    else {
                        enteringPassword = true;
                        ProjectDialogFactory.networkPassword(view, wifiItem.ssid(), NetworkPasswordEntry.PasswordEntryMode.WPA, onPasswordEntry);
                    }
                }
            }
        });


        layoutManager = SelectorDefaultAnimations.setDefaultAnimations(wifiSelector.getContext(), wifiSelector);
        wifiSelector.setAdapter(adapter);

        updateNetworkSettings(new ArrayList<>());
        networkController.clearResults();
        networkController.scanNetworks();

    }

    public void refreshResults() {
        if(!connectingToNetwork && !enteringPassword) {
            networkController.refreshResults();
        }
    }


    public void hide() {
        networkController.stopScanningNetworks();
        networkController.destroy();
    }

    private SimpleMessageDialog failedToConnectDialog = null;

    @Override
    public void onResult(String ssid, boolean success) {
        boolean isConnecting = this.connectingToNetwork;
        updateNetworkConnectingState(false);
        if(!success && ssid != null && !ssid.isEmpty()) {

            new Thread(() -> {

                if(failedToConnectDialog == null) {
                    JHTNotification notification = new JHTNotification();
                    // Show error dialog.
                    if(isConnecting) {
                        handler.post(() -> {
                            failedToConnectDialog = ProjectDialogFactory.message(wifiSelector.getRootView(), wifiSelector.getContext().getResources().getString(R.string.failed_to_connect_pg), () -> failedToConnectDialog = null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                            notification.setNotification();
                        });
                        notification.waitForNotification();
                    }
                    handler.post(() -> {
                        networkController.forgetNetwork(ssid);
                        forgottenSSID = ssid;
                    });
                }
            }).start();
        }

    }

    @Override
    public void onScanUpdated(List<SSID> scanResults) {
        Log.e(TAG, "onScanUpdated");

        JHTNotification notification = new JHTNotification();
        // Show error dialog.
        handler.post(() -> {
            updateNetworkSettings(scanResults);
            notification.setNotification();
        });
        notification.waitForNotification();
    }
}
