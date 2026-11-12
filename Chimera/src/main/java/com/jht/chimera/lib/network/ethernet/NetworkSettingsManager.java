package com.jht.chimera.lib.network.ethernet;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.jht.chimera.lib.common.context.ContextUtil;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"MissingPermission", "unchecked", "JavaReflectionMemberAccess"})
public class NetworkSettingsManager {
    @NonNls private final String TAG = "NetworkSettingsManager";

    private Class<?> classEthernetManager;
    private Method methodGetConfiguration;
    private Method methodSetConfiguration;

    private Constructor<?> constructorIpConfiguration;
    private Object[] enumIpAssignment;
    private Object[] enumProxySettings;
    private Method methodGetIpAssignment;
    private Method methodGetStaticIpConfiguration;
    private Field fieldProxySettings;
    private Field fieldHttpProxy;

    private Constructor<?> constructorStaticIPConfiguration;
    private Field fieldIpAddress;
    private Field fieldGateway;
    private Field fieldDnsServers;
    private Field fieldDomains;

    private Method methodIsIPv4Default;
    private Method methodHasGateway;


    Method methodGetIpConfiguration;
    Method methodSetIPConfiguration;


    private Constructor<?> constructorLinkAddress;

    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
    public NetworkSettingsManager() {
        try {
            classEthernetManager = Class.forName("android.net.EthernetManager");
            methodGetConfiguration = classEthernetManager.getDeclaredMethod("getConfiguration", String.class);
            //Method methodGetAvailableInterfaces = classEthernetManager.getDeclaredMethod("getAvailableInterfaces");

            Class<?> classIpConfiguration = Class.forName("android.net.IpConfiguration");
            enumIpAssignment = Class.forName("android.net.IpConfiguration$IpAssignment").getEnumConstants();
            enumProxySettings = Class.forName("android.net.IpConfiguration$ProxySettings").getEnumConstants();
            methodGetIpAssignment = classIpConfiguration.getDeclaredMethod("getIpAssignment");
            methodGetStaticIpConfiguration = classIpConfiguration.getDeclaredMethod("getStaticIpConfiguration");
            fieldProxySettings = classIpConfiguration.getDeclaredField("proxySettings");
            fieldHttpProxy = classIpConfiguration.getDeclaredField("httpProxy");

            Class<?> classStaticIpConfiguration = Class.forName("android.net.StaticIpConfiguration");
            constructorStaticIPConfiguration = classStaticIpConfiguration.getConstructor();
            fieldIpAddress = classStaticIpConfiguration.getDeclaredField("ipAddress");
            fieldGateway = classStaticIpConfiguration.getDeclaredField("gateway");
            fieldDnsServers = classStaticIpConfiguration.getDeclaredField("dnsServers");
            fieldDomains = classStaticIpConfiguration.getDeclaredField("domains");

            methodIsIPv4Default = RouteInfo.class.getDeclaredMethod("isIPv4Default");
            methodIsIPv4Default.setAccessible(true);

            methodHasGateway = RouteInfo.class.getDeclaredMethod("hasGateway");
            methodHasGateway.setAccessible(true);

            constructorLinkAddress = LinkAddress.class.getConstructor(InetAddress.class, int.class);
            constructorLinkAddress.setAccessible(true);

            methodGetIpConfiguration = WifiConfiguration.class.getDeclaredMethod("getIpConfiguration");
            methodSetIPConfiguration = WifiConfiguration.class.getDeclaredMethod("setIpConfiguration", classIpConfiguration);
            methodSetConfiguration = classEthernetManager.getDeclaredMethod("setConfiguration", String.class, classIpConfiguration);
            constructorIpConfiguration = classIpConfiguration.getConstructor(Class.forName("android.net.IpConfiguration$IpAssignment"),
                    Class.forName("android.net.IpConfiguration$ProxySettings"), classStaticIpConfiguration, ProxyInfo.class);

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public SystemNetworkSettings getEthernetSettings(Context context) {
        try {
            //EthernetManager manager = context.getSystemService(EthernetManager.class);
            Object manager = context.getSystemService(classEthernetManager);
            ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
            SystemNetworkSettings settings = new SystemNetworkSettings();

            if (manager == null || connectivityManager == null) {
                return null;
            }

            /*
            //String[] ifaces = manager.getAvailableInterfaces();
            String[] ifaces = (String[]) methodGetAvailableInterfaces.invoke(manager);
            if (ifaces == null || ifaces.length == 0) {
                return null;
            }
            */

            //IpConfiguration configuration = manager.getConfiguration(ifaces[0]);
            ///Object configuration = methodGetConfiguration.invoke(manager, ifaces[0]);
            Object configuration = methodGetConfiguration.invoke(manager, "eth0");

            if (configuration == null) {
                return null;
            }

            Network network = getFirstEthernet(connectivityManager);
            if (network == null) {
                return null;
            }

            // Get the link properties used to look up the IP settings.
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            if (linkProperties == null) {
                return null;
            }

            //boolean dhcp = configuration.getIpAssignment() == IpConfiguration.IpAssignment.DHCP || configuration.getIpAssignment() == IpConfiguration.IpAssignment.UNASSIGNED;
            boolean dhcp = methodGetIpAssignment.invoke(configuration) == enumIpAssignment[1] || methodGetIpAssignment.invoke(configuration) == enumIpAssignment[2];
            settings.setDhcp(dhcp);
            if (dhcp) {
                // This is DHCP.  Grab all of the IP settings from the link properties.
                for (LinkAddress addr : linkProperties.getLinkAddresses()) {
                    settings.setIPAddress(addr);
                }

                // Find IPv4 default gateway.
                for (RouteInfo routeInfo : linkProperties.getRoutes()) {
                    //if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
                    if ((Boolean) methodIsIPv4Default.invoke(routeInfo) && (Boolean) methodHasGateway.invoke(routeInfo)) {
                        settings.setGateway(routeInfo.getGateway());
                        break;
                    }
                }

                // Find all (IPv4 and IPv6) DNS addresses.
                List<InetAddress> dnsServers = linkProperties.getDnsServers();
                if (dnsServers != null) {
                    for (InetAddress dnsServer : dnsServers) {
                        settings.addDNSServer(dnsServer);
                    }
                }
            } else {
                // Get the static configuration.
                //StaticIpConfiguration staticIpConfiguration = configuration.getStaticIpConfiguration();
                Object staticIpConfiguration = methodGetStaticIpConfiguration.invoke(configuration);
                if (staticIpConfiguration != null) {
                    //settings.setIPAddress(staticIpConfiguration.ipAddress);
                    settings.setIPAddress((LinkAddress) fieldIpAddress.get(staticIpConfiguration));
                    //settings.setGateway((InetfieldGateway.get(staticIpConfiguration));
                    settings.setGateway((InetAddress) fieldGateway.get(staticIpConfiguration));
                    //for (InetAddress dnsServer : staticIpConfiguration.dnsServers) {
                    for (InetAddress dnsServer : (ArrayList<InetAddress>) fieldDnsServers.get(staticIpConfiguration)) {
                        settings.addDNSServer(dnsServer);
                    }
                    //settings.setDomains(staticIpConfiguration.domains);
                    settings.setDomains((String) fieldDomains.get(staticIpConfiguration));
                }
            }

            // Check for proxy settings.
            //if (configuration.proxySettings == IpConfiguration.ProxySettings.PAC) {
            if (fieldProxySettings.get(configuration) == enumProxySettings[3]) {
                //settings.setProxyPAC(configuration.httpProxy.getPacFileUrl());
                ProxyInfo httpProxy = (ProxyInfo) fieldHttpProxy.get(configuration);
                settings.setProxyPAC(httpProxy.getPacFileUrl());
                //} else if (configuration.proxySettings == IpConfiguration.ProxySettings.STATIC) {
            } else if (fieldProxySettings.get(configuration) == enumProxySettings[1]) {
                //settings.setProxySettings(configuration.httpProxy.getHost(), configuration.httpProxy.getPort());
                ProxyInfo httpProxy = (ProxyInfo) fieldHttpProxy.get(configuration);
                settings.setProxySettings(httpProxy.getHost(), httpProxy.getPort());
                //String[] exclusionArray = configuration.httpProxy.getExclusionList();
                String[] exclusionArray = httpProxy.getExclusionList();
                if (exclusionArray != null) {
                    for (String s : exclusionArray) {
                        settings.addProxyExclude(s);
                    }
                }
            }

            return settings;
        } catch (Exception e) {
            Log.e(TAG, "==> getEthernetSettings error", e);
        }

        return null;
    }

    public void setEthernetSettings(Context context, SystemNetworkSettings settings) {
        try {
            //EthernetManager manager = context.getSystemService(EthernetManager.class);
            Object manager = context.getSystemService(classEthernetManager);
            Log.d(TAG, "Received new Ethernet IP settings: " + settings.toString());

            if (manager == null) {
                Log.d(TAG, "Cannot get Ethernet manager");
                return;
            }

            //String[] ifaces = manager.getAvailableInterfaces();
            /*
            String[] ifaces = (String[]) methodGetAvailableInterfaces.invoke(manager);
            if (ifaces == null || ifaces.length == 0) {
                return;
            }
            */

            //IpConfiguration.IpAssignment ipAssignment = settings.isDhcp() ? IpConfiguration.IpAssignment.DHCP : IpConfiguration.IpAssignment.STATIC;
            Object ipAssignment = settings.isDhcp() ? enumIpAssignment[1] : enumIpAssignment[0];
            //StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            Object staticIpConfiguration = constructorStaticIPConfiguration.newInstance();
            if (!settings.isDhcp()) {
                //staticIpConfiguration.ipAddress = new LinkAddress(settings.getIpAddress(), settings.getPrefixLength());
                fieldIpAddress.set(staticIpConfiguration, constructorLinkAddress.newInstance(settings.getIpAddress(), settings.getPrefixLength()));
                //staticIpConfiguration.gateway = settings.getGateway();
                fieldGateway.set(staticIpConfiguration, settings.getGateway());
                //staticIpConfiguration.dnsServers.addAll(settings.getDnsServers());
                ArrayList<InetAddress> dnsServers = (ArrayList<InetAddress>) fieldDnsServers.get(staticIpConfiguration);
                dnsServers.addAll(settings.getDnsServers());
                //staticIpConfiguration.domains = settings.getDomains();
                fieldDomains.set(staticIpConfiguration, settings.getDomains());
            }

            // String host, int port, List<String> exclList   Uri pacUri
            ProxyInfo proxyInfo = null;
            //IpConfiguration.ProxySettings proxySettings;
            Object proxySettings;
            if (settings.getPacUri() != null) {
                //proxySettings = IpConfiguration.ProxySettings.PAC;
                proxySettings = enumProxySettings[3];
                proxyInfo = ProxyInfo.buildPacProxy(settings.getPacUri());
            } else if (settings.isUseProxy()) {
                //proxySettings = IpConfiguration.ProxySettings.STATIC;
                proxySettings = enumProxySettings[1];
                proxyInfo = ProxyInfo.buildDirectProxy(settings.getHost(), settings.getPort(), settings.getExclList());
            } else {
                //proxySettings = IpConfiguration.ProxySettings.NONE;
                proxySettings = enumProxySettings[0];
            }

            Log.d(TAG, "ipAssignment: " + ipAssignment);
            Log.d(TAG, "staticIpConfiguration: " + staticIpConfiguration);
            Log.d(TAG, "proxySettings: " + proxySettings);
            Log.d(TAG, "proxyInfo: " + proxyInfo);


            //manager.setConfiguration(ifaces[0], new IpConfiguration(ipAssignment, proxySettings, staticIpConfiguration, proxyInfo));
            //methodSetConfiguration.invoke(manager, ifaces[0], constructorIpConfiguration.newInstance(ipAssignment, proxySettings, staticIpConfiguration, proxyInfo));
            methodSetConfiguration.invoke(manager, "eth0", constructorIpConfiguration.newInstance(ipAssignment, proxySettings, staticIpConfiguration, proxyInfo));
        } catch (Exception e) {
            Log.e(TAG, "==> setEthernetSettings error", e);
        }
    }

    public SystemNetworkSettings getWifiSettings(Context context) {
        try {
            WifiManager manager = context.getSystemService(WifiManager.class);
            SystemNetworkSettings settings = new SystemNetworkSettings();
            ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);

            // See if we have access to the system services.
            if (manager == null || connectivityManager == null) {
                return null;
            }

            // Get the current WiFi network.  If null we aren't connected.
            Network network = getFirstWifi(connectivityManager);
            if (network == null) {
                return null;
            }

            // Get the link properties used to look up the IP settings.
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            if (linkProperties == null) {
                return null;
            }

            // Get the current configuration for the current wifi.  We need this to get the static IP settings.

            WifiConfiguration wifiConfiguration = getCurrentWiFiConfiguration(ContextUtil.getApplicationContext());
//            WifiConfiguration wifiConfiguration = getWifiConfiguration(manager, network);

            if (wifiConfiguration == null) {
                return null;
            }

            // Get the network static ip and proxy configuration.
            //IpConfiguration configuration = wifiConfiguration.getIpConfiguration();
            Object configuration = methodGetIpConfiguration.invoke(wifiConfiguration);
            if (configuration == null) {
                return null;
            }

            // See if this is dhcp.
            //boolean dhcp = configuration.getIpAssignment() == IpConfiguration.IpAssignment.DHCP || configuration.getIpAssignment() == IpConfiguration.IpAssignment.UNASSIGNED;
            boolean dhcp = methodGetIpAssignment.invoke(configuration) == enumIpAssignment[1] || methodGetIpAssignment.invoke(configuration) == enumIpAssignment[2];
            settings.setDhcp(dhcp);
            if (dhcp) {
                // This is DHCP.  Grab all of the IP settings from the link properties.
                for (LinkAddress addr : linkProperties.getLinkAddresses()) {
                    settings.setIPAddress(addr);
                }

                // Find IPv4 default gateway.
                for (RouteInfo routeInfo : linkProperties.getRoutes()) {

                    //if (routeInfo.isIPv4Default() && routeInfo.hasGateway()) {
                    if ((Boolean) methodIsIPv4Default.invoke(routeInfo) && (Boolean) methodHasGateway.invoke(routeInfo)) {
                        settings.setGateway(routeInfo.getGateway());
                        break;
                    }
                }

                // Find all (IPv4 and IPv6) DNS addresses.
                List<InetAddress> dnsServers = linkProperties.getDnsServers();
                if (dnsServers != null) {
                    for (InetAddress dnsServer : dnsServers) {
                        settings.addDNSServer(dnsServer);
                    }
                }
            } else {
                // Get the static configuration.
                //StaticIpConfiguration staticIpConfiguration = configuration.getStaticIpConfiguration();
                Object staticIpConfiguration = methodGetStaticIpConfiguration.invoke(configuration);
                if (staticIpConfiguration != null) {
                    //settings.setIPAddress(staticIpConfiguration.ipAddress);
                    settings.setIPAddress((LinkAddress) fieldIpAddress.get(staticIpConfiguration));
                    //settings.setGateway((InetfieldGateway.get(staticIpConfiguration));
                    settings.setGateway((InetAddress) fieldGateway.get(staticIpConfiguration));
                    //for (InetAddress dnsServer : staticIpConfiguration.dnsServers) {
                    for (InetAddress dnsServer : (ArrayList<InetAddress>) fieldDnsServers.get(staticIpConfiguration)) {
                        settings.addDNSServer(dnsServer);
                    }
                    //settings.setDomains(staticIpConfiguration.domains);
                    settings.setDomains((String) fieldDomains.get(staticIpConfiguration));
                }
            }

            // Check for proxy settings.
            //if (configuration.proxySettings == IpConfiguration.ProxySettings.PAC) {
            if (fieldProxySettings.get(configuration) == enumProxySettings[3]) {
                //settings.setProxyPAC(configuration.httpProxy.getPacFileUrl());
                ProxyInfo httpProxy = (ProxyInfo) fieldHttpProxy.get(configuration);
                settings.setProxyPAC(httpProxy.getPacFileUrl());
                //} else if (configuration.proxySettings == IpConfiguration.ProxySettings.STATIC) {
            } else if (fieldProxySettings.get(configuration) == enumProxySettings[1]) {
                //settings.setProxySettings(configuration.httpProxy.getHost(), configuration.httpProxy.getPort());
                ProxyInfo httpProxy = (ProxyInfo) fieldHttpProxy.get(configuration);
                settings.setProxySettings(httpProxy.getHost(), httpProxy.getPort());
                //String[] exclusionArray = configuration.httpProxy.getExclusionList();
                String[] exclusionArray = httpProxy.getExclusionList();
                if (exclusionArray != null) {
                    for (String s : exclusionArray) {
                        settings.addProxyExclude(s);
                    }
                }
            }

            return settings;
        } catch (Exception e) {
            Log.e(TAG, "==> getWifiSettings error", e);
        }
        return null;
    }

    public static WifiConfiguration getCurrentWiFiConfiguration(Context context) {
        WifiConfiguration wifiConf = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
                if (configuredNetworks != null) {
                    for (WifiConfiguration conf : configuredNetworks) {
                        if (conf.networkId == connectionInfo.getNetworkId()) {
                            wifiConf = conf;
                            break;
                        }
                    }
                }

            }
        }
        return wifiConf;
    }

//This is method is buggy. Will cause the ip address to not update properly.
//    private WifiConfiguration getWifiConfiguration(WifiManager wifiManager, Network network) {
//        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
//        if (configuredNetworks != null) {
//            for (WifiConfiguration configuredNetwork : configuredNetworks) {
//                return configuredNetwork; // return the first network
//                // this doesn't seem to work (status is sometimes 2)
//                //if (configuredNetwork.status == 0) {
//                //    return configuredNetwork;
//                //}
//            }
//        }
//        return null;
//    }

    private Network getFirstEthernet(ConnectivityManager manager) {
        final Network[] networks = manager.getAllNetworks();
        for (final Network network : networks) {
            NetworkInfo networkInfo = manager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return network;
            }
        }
        return null;
    }


    private Network getFirstWifi(ConnectivityManager manager) {
        final Network[] networks = manager.getAllNetworks();
        for (final Network network : networks) {
            NetworkInfo networkInfo = manager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return network;
            }
        }
        return null;
    }


}

