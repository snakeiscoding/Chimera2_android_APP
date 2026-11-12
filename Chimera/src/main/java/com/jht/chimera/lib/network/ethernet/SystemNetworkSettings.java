package com.jht.chimera.lib.network.ethernet;

import android.content.Intent;
import android.net.LinkAddress;
import android.net.Uri;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SystemNetworkSettings {

    private boolean dhcp;
    private InetAddress ipAddress;
    private Inet6Address ipv6Address;
    private int prefixLength;
    private InetAddress gateway;
    private List<InetAddress> dnsServers;

    private boolean useProxy;
    private Uri pacUri;
    private String host;
    private int port;
    private ArrayList<String> exclList;
    private String domains = "";

    public SystemNetworkSettings() {
        dhcp = true;
        ipAddress = null;
        ipv6Address = null;
        prefixLength = 24;
        gateway = null;
        useProxy = false;
        dnsServers = new ArrayList<>();
        pacUri = null;
        host = "";
        port = 0;
        exclList = new ArrayList<>();
        domains = "";
    }

    public SystemNetworkSettings(Intent intent) {
        this();
        fromIntent(intent);
    }

    public void setDhcp(boolean dhcp) {
        this.dhcp = dhcp;
    }

    public void setIPAddress(String ipAddress, int prefixLength) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ipAddress);
        if (address instanceof Inet6Address) {
            ipv6Address = (Inet6Address) address;
        } else {
            this.ipAddress = address;
        }
        this.prefixLength = prefixLength;
    }

    public void setIPAddress(LinkAddress address) {
        if (address.getAddress() instanceof Inet4Address) {
            ipAddress = address.getAddress();
            prefixLength = address.getPrefixLength();

        } else if (address.getAddress() instanceof Inet6Address) {
            ipv6Address = (Inet6Address) address.getAddress();
        }
    }

    public void setGateway(String gateway) throws UnknownHostException {
        if(gateway.isEmpty()) {
           this.gateway = null;
        } else {
            this.gateway = InetAddress.getByName(gateway);
        }
    }

    public void setGateway(InetAddress gateway) {
        this.gateway = gateway;
    }

    public void addDNSServer(String dnsServer) throws UnknownHostException {
        dnsServers.add(InetAddress.getByName(dnsServer));
    }

    public void addDNSServer(InetAddress dnsServer) {
        dnsServers.add(dnsServer);
    }

    public void setProxyPAC(Uri pacUri) {
        useProxy = true;
        this.pacUri = pacUri;
    }

    public void setProxySettings(String host, int port) {
        useProxy = true;
        this.host = host;
        this.port = port;
    }

    public void addProxyExclude(String host) {
        exclList.add(host);
    }

    public void setDomains(String domains) {
        if(domains != null) {
            this.domains = domains;
        }
        else {
            this.domains = "";
        }
    }

    public void addToIntent(Intent intent) {
        intent.putExtra("dhcp", dhcp);
        if (ipAddress != null) {
            intent.putExtra("ipAddress", ipAddress);
        }
        if (ipv6Address != null) {
            intent.putExtra("ipv6Address", ipv6Address);
        }
        intent.putExtra("prefixLength", prefixLength);
        if (gateway != null) {
            intent.putExtra("gateway", gateway);
        }
        intent.putExtra("numDNSServers", dnsServers.size());
        for (int i = 0; i < dnsServers.size(); i++) {
            intent.putExtra("dnsServer" + i, dnsServers.get(i));
        }
        intent.putExtra("domains", domains);

        intent.putExtra("useProxy", useProxy);
        if (pacUri != null) {
            intent.putExtra("pacUri", pacUri);
        }
        intent.putExtra("host", host);
        intent.putExtra("port", port);
        intent.putExtra("exclList", exclList);

    }

    public void fromIntent(Intent intent) {
        dhcp = intent.getBooleanExtra("dhcp", true);
        if (intent.hasExtra("ipAddress")) {
            ipAddress = (InetAddress) intent.getSerializableExtra("ipAddress");
        }
        if (intent.hasExtra("ipv6Address")) {
            ipv6Address = (Inet6Address) intent.getSerializableExtra("ipv6Address");
        }
        prefixLength = intent.getIntExtra("prefixLength", 24);
        if (intent.hasExtra("gateway")) {
            gateway = (InetAddress) intent.getSerializableExtra("gateway");
        }
        int numDNSServers = intent.getIntExtra("numDNSServers", 0);
        for (int i = 0; i < numDNSServers; i++) {
            dnsServers.add((InetAddress) intent.getSerializableExtra("dnsServer" + i)); //NON-NLS
        }
        useProxy = intent.getBooleanExtra("useProxy", false);
        if (intent.hasExtra("pacUri")) {
            pacUri = intent.getParcelableExtra("packUri");
        }
        host = intent.getStringExtra("host");
        port = intent.getIntExtra("port", 0);
        exclList = intent.getStringArrayListExtra("exclList");
        domains = intent.getStringExtra("domains");
        if(domains == null) {
            domains = "";
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\tUse DHCP: ").append(dhcp).append("\n");
        if (ipAddress != null) {
            builder.append("\tIP Address: ").append(ipAddress.getHostAddress()).append("\n");
        }
        if (ipv6Address != null) {
            builder.append("\tIPv6 Address: ").append(ipv6Address.getHostAddress()).append("\n");
        }
        builder.append("Prefix Length: ").append(prefixLength).append("\n");
        if (gateway != null) {
            builder.append("\tGateway Address: ").append(gateway.getHostAddress()).append("\n");
        }
        if (dnsServers != null) {
            builder.append("\tDNS Server(s):\n");
            for (InetAddress dns : dnsServers) {
                builder.append("\t\t").append(dns.getHostAddress()).append("\n");
            }
        }
        builder.append("\tDomains: ").append(domains).append("\n");
        builder.append("\tUse Proxy: ").append(useProxy).append("\n");
        if (pacUri != null) {
            builder.append("\tPAC URI: ").append(pacUri).append("\n");
        }
        builder.append("\tProxy Host: ").append(host).append("\n");
        builder.append("\tProxy Port: ").append(port).append("\n");
        if (exclList != null) {
            builder.append("\tProxy Exclude List:\n");
            for (String exclude : exclList) {
                builder.append("\t\t").append(exclude).append("\n");
            }
        }
        return builder.toString();
    }

    public boolean isDhcp() {
        return dhcp;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Inet6Address getIpv6Address() {
        return ipv6Address;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public InetAddress getGateway() {
        return gateway;
    }

    public List<InetAddress> getDnsServers() {
        return dnsServers;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public Uri getPacUri() {
        return pacUri;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ArrayList<String> getExclList() {
        return exclList;
    }

    public String getDomains() {
        return domains;
    }


}
