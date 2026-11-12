package com.jht.test.util

import java.net.NetworkInterface

fun getIPAddress(): String {
    return catchAll("") {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            // filters out 127.0.0.1 and inactive interfaces
            if (!networkInterface.isLoopback && networkInterface.isUp && !networkInterface.name.contains("tun") && !networkInterface.name.contains("vpn")
            ) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address.address.size == 4) {
                        return@catchAll address.hostAddress
                    }
                }
            }
        }
        ""
    }
}