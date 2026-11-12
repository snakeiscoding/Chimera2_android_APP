package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.network.ethernet.NetworkSettingsManager;
import com.jht.chimera.lib.network.ethernet.SystemNetworkSettings;

import org.jetbrains.annotations.NonNls;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */

public class NetworkIPSettings extends DialogBase {
    @NonNls
    private final String TAG = "NetworkIPSettings";

    private SystemNetworkSettings settings;

    private CheckBox dhcp;
    private UserTextInput ipAddress;
    private OptionSelectorItem subnetMask;
    private UserTextInput gateway;
    private RecyclerView dnsServers;
    private ImageButton dnsServerAdd;
    private UserTextInput domain;
    private CheckBox proxy;
    private UserTextInput pacURL;
    private UserTextInput host;
    private UserTextInput port;
    private RecyclerView excludeList;
    private ImageButton excludeListAdd;

    private SelectorAdapterBase dnsAdapter;
    private SelectorAdapterBase excludesAdapter;
    SelectorItemBase dnsList;
    SelectorItemBase excludes;


    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public NetworkIPSettings(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public NetworkIPSettings(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public NetworkIPSettings(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dhcp = findViewById(R.id.network_ip_settings_dialog_dhcp);
        ipAddress = findViewById(R.id.network_ip_settings_dialog_ip_address);
        subnetMask = findViewById(R.id.network_ip_settings_dialog_subnet_mask);
        gateway = findViewById(R.id.network_ip_settings_dialog_ip_gateway);
        dnsServers = findViewById(R.id.network_ip_settings_dialog_dnslist);
        dnsServerAdd = findViewById(R.id.network_ip_settings_dialog_add_dns);
        domain = findViewById(R.id.network_ip_settings_dialog_ip_domain);
        proxy = findViewById(R.id.network_ip_settings_dialog_proxy);
        pacURL = findViewById(R.id.network_ip_settings_dialog_proxy_pac);
        host = findViewById(R.id.network_ip_settings_dialog_proxy_host);
        port = findViewById(R.id.network_ip_settings_dialog_proxy_port);
        excludeList = findViewById(R.id.network_ip_settings_dialog_exclude_list);
        excludeListAdd = findViewById(R.id.network_ip_settings_dialog_add_exclude);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.network_ip_settings;
    }

    /**
     * Initialize the dialog.
     */
    public void init(final SystemNetworkSettings settings) {
        this.settings = settings;
        resetUI();

        dnsServerAdd.setOnClickListener(v -> {
            dnsList.addItem(new URLSelectorItem("", dhcp.isChecked()));
            dnsAdapter.notifyDataSetChanged();
        });

        excludeListAdd.setOnClickListener(v -> {
            excludes.addItem(new URLSelectorItem("", !proxy.isChecked()));
            excludesAdapter.notifyDataSetChanged();
        });

        dhcp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (int i = 0; i < dnsList.numChildren(); i++) {
                ISelectorItem item = dnsList.getChild(i);
                if (item instanceof URLSelectorItem) {
                    ((URLSelectorItem) item).readOnly = isChecked;
                }
            }
            dnsAdapter.notifyDataSetChanged();
            dnsServerAdd.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            ipAddress.setReadOnly(isChecked);
            subnetMask.setReadOnly(isChecked);
            domain.setReadOnly(isChecked);
            gateway.setReadOnly(isChecked);
        });

        proxy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (int i = 0; i < excludes.numChildren(); i++) {
                ISelectorItem item = excludes.getChild(i);
                if (item instanceof URLSelectorItem) {
                    ((URLSelectorItem) item).readOnly = !isChecked;
                }
            }
            excludesAdapter.notifyDataSetChanged();
            excludeListAdd.setVisibility(!isChecked ? View.GONE : View.VISIBLE);
            pacURL.setReadOnly(!isChecked);
            host.setReadOnly(!isChecked);
            port.setReadOnly(!isChecked);
        });

        findViewById(R.id.network_ip_settings_save).setOnClickListener(v -> {
            SystemNetworkSettings newSettings = new SystemNetworkSettings();
            hide();
            newSettings.setDhcp(dhcp.isChecked());
            if (!dhcp.isChecked()) {
                try {
                    newSettings.setIPAddress(ipAddress.getText(), subnetMask.getSelectedPosition() + 8);
                    newSettings.setGateway(gateway.getText());
                    for (int i = 0; i < dnsList.numChildren(); i++) {
                        String server = dnsList.getChild(i).toString();
                        if (!server.equals("")) {
                            newSettings.addDNSServer(server);
                        }
                    }
                } catch (UnknownHostException e) {
                    // TODO: Show a message.
                    Log.w(TAG, e);
                }
            }
            if (proxy.isChecked()) {
                String url = pacURL.getText();
                if (url != null && !url.equals("")) {
                    newSettings.setProxyPAC(Uri.parse(url));
                } else {
                    newSettings.setProxySettings(host.getText(), Integer.parseInt(port.getText()));

                    for (int i = 0; i < excludes.numChildren(); i++) {
                        String exclude = excludes.getChild(i).toString();
                        if (!exclude.equals("")) {
                            newSettings.addProxyExclude(exclude);
                        }
                    }
                }
            }

            NetworkSettingsManager networkSettingsManager = new NetworkSettingsManager();
            networkSettingsManager.setEthernetSettings(getContext(), newSettings);


        });

        findViewById(R.id.network_ip_settings_cancel).setOnClickListener(v -> hide());
        findViewById(R.id.network_ip_settings_reset).setOnClickListener(v -> resetUI());
    }

    private void resetUI() {

        dnsList = new SelectorItemBase(null);
        for (InetAddress address : settings.getDnsServers()) {
            dnsList.addItem(new URLSelectorItem(address.getHostAddress(), settings.isDhcp()));
        }
        SelectorDefaultAnimations.setDefaultAnimations(getContext(), dnsServers);
        dnsAdapter = new SelectorAdapterBase(getContext(), dnsList, null);
        dnsServers.setAdapter(dnsAdapter);
        dnsServerAdd.setVisibility(settings.isDhcp() ? View.GONE : View.VISIBLE);

        excludes = new SelectorItemBase(null);
        for (String excl : settings.getExclList()) {
            excludes.addItem(new URLSelectorItem(excl, !settings.isUseProxy()));
        }
        SelectorDefaultAnimations.setDefaultAnimations(getContext(), excludeList);
        excludesAdapter = new SelectorAdapterBase(getContext(), excludes, null);
        excludeList.setAdapter(excludesAdapter);
        excludeListAdd.setVisibility(!settings.isUseProxy() ? View.GONE : View.VISIBLE);

        dhcp.setChecked(settings.isDhcp());
        if (settings.getIpAddress() != null) {
            ipAddress.setText(settings.getIpAddress().getHostAddress());
        } else {
            ipAddress.setText("");
        }
        ipAddress.setReadOnly(settings.isDhcp());

        subnetMask.selectItem(settings.getPrefixLength() - 8);
        subnetMask.setReadOnly(settings.isDhcp());

        if (settings.getGateway() != null) {
            gateway.setText(settings.getGateway().getHostAddress());
        } else {
            gateway.setText("");
        }
        gateway.setReadOnly(settings.isDhcp());

        domain.setText(settings.getDomains());
        domain.setReadOnly(settings.isDhcp());

        proxy.setChecked(settings.isUseProxy());

        if (settings.getPacUri() != null) {
            pacURL.setText(settings.getPacUri().toString());
        }
        pacURL.setReadOnly(!settings.isUseProxy());
        if (settings.getPacUri() == null) {
            pacURL.setText("");
        } else {
            pacURL.setText(settings.getPacUri().toString());
        }

        host.setText(settings.getHost());
        host.setReadOnly(!settings.isUseProxy());

        port.setText(settings.getPort() + "");
        port.setReadOnly(!settings.isUseProxy());
    }


}
