package com.jht.chimera.service.hal.lib.util

import android.util.Log
import com.jht.chimera.lib.common.SystemPropertyUtil
import java.net.InetAddress

object NtpServerUtils {

    private const val TAG = "NtpServerUtils"
    private const val PROP_NTP_SERVER_LIST = "debug.jht.ntp.server_list"

    fun getNtpServersOverDHCP(): List<InetAddress> {
        val ipStringList = SystemPropertyUtil.get(PROP_NTP_SERVER_LIST, "")
        if (ipStringList.isBlank()) return emptyList()
        Log.d(TAG, "getNtpServersOverDHCP: $ipStringList")

        return ipStringList.split(",")
            .map { it.trim() }
            .filter { isValidIpv4(it) }
            .mapNotNull { ip ->
                try {
                    InetAddress.getByAddress(parseIpv4(ip))
                } catch (_: Exception) {
                    null
                }
            }
    }

    private fun isValidIpv4(ip: String): Boolean {
        val octets = ip.split(".")
        if (octets.size != 4) return false

        return octets.all { octet ->
            octet.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private fun parseIpv4(ip: String): ByteArray {
        val octets = ip.split(".")
        return ByteArray(4) { i ->
            octets[i].toInt().toByte()
        }
    }
}