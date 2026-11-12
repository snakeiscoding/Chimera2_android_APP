@file:JvmName("HardwareIdUtil")
package com.jht.chimera.lib.common

import java.io.File

/*
    This file contains functions to attempt to collect hardware identifiers.
 */

/** Attempt to retrieve CPU serial number */
fun getCpuSerialNumber(): String? {
    // chimera1, chimera2
    getNxpId()?.let { return it }
    // parker1, vortex
    getRockChipId()?.let { return it }
    // emulator or unknown console, try to return the android serial number
    SystemPropertyUtil.get("ro.serialno")?.let { return it }
    return null
}

/* This method used a shell, but direct file API is likely more efficient
private fun getRockChipIdShell(): String? {
    return runCatching {
        val root = ExecuteAsRoot()
        root.addCommand("grep \"Serial\" /proc/cpuinfo")
        val output = StringBuilder()
        root.execute(false, true, output)
        output.split(":").getOrNull(1)?.trim()
    }.getOrNull()
}*/
private fun getRockChipId(): String? {
    return runCatching {
        val path = File("/proc/cpuinfo")
        if (path.exists()) {
            path.useLines { lines ->
                lines.forEach { line ->
                    // cpuinfo is laid out as rows of "label : text", sometimes with blank lines between sections
                    val fields = line.split(":")
                    val label = fields[0].trim()
                    if (label.startsWith("Serial")) return fields.getOrNull(1)?.trim()
                }
            }
        }
        return null
    }.getOrNull()
}
private fun getNxpId(): String? {
    return runCatching {
        var path = File("/sys/devices/soc0/soc_uid")
        if (!path.exists()) path = File("/sys/devices/soc0/serial_number") // kernel 5.10 and above
        return if (path.exists()) path.readText().trim() else null
    }.getOrNull()
}


/** Attempt to retrieve OS serial number (Requires android.permission.READ_PRIVILEGED_PHONE_STATE) */
/*@SuppressLint("MissingPermission")
fun getOsSerialNumber(): String? {
    return runCatching {
        android.os.Build.getSerial() // SSO mods to operating system might make this unreliable?
    }.getOrNull()
}*/
