package com.jht.widgets.helpers

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import com.jht.widgets.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale


/** Class to use in composable to get system stat updates */
class SystemStats(val context: Context) {
    val memoryUsage = MutableStateFlow("")
    val cpuUsage = MutableStateFlow("")
    val sdCardUsage = MutableStateFlow("")
    val storageUsage = MutableStateFlow("")
    val cpuTemp = MutableStateFlow("")
    val wifiSignal = MutableStateFlow("")
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private var previousStats: ProcessorStat? = null
    private var currentStats: ProcessorStat? = null
    fun start() {
        if (job != null) return // anything we check on the scope to see if anything already running?
        job = scope.launch {
            // old code just got storage stuff on start
            storageUsage.value = updateStorageUse()
            sdCardUsage.value = updateSdCardUse()
            while (isActive) {
                delay(1000)

                // we could either buffer these in locals then set mutable states inside withContext(Dispatchers.Main) or use MutableStateFlow
                // https://stackoverflow.com/a/72677252
                cpuTemp.value = updateCpuTemp()
                memoryUsage.value = updateMemoryUse()
                cpuUsage.value = updateCpuUse()
                wifiSignal.value = updateWifiSignal()
            }
        } // END launch job
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun updateCpuTemp(): String {
        try {
            BufferedReader(
                InputStreamReader(Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp").inputStream)
            ).use { reader -> return (reader.readLine().toInt() / 1000).toString() + " \u2103" }
        } catch (_: Exception) { return "" }
    }

    private fun updateMemoryUse(): String {
        val mi = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(mi)
        val percentage = ((mi.totalMem - mi.availMem) * 100 / mi.totalMem).toInt()
        return String.format(Locale.ENGLISH, "%d%s", percentage, "%")
    }

    private fun updateCpuUse(): String {
        if (previousStats == null) {
            previousStats = ProcessorStat()
            currentStats = ProcessorStat()
            previousStats?.getStats()
            return "--"
        } else {
            currentStats!!.getStats()
            val resultString = String.format(Locale.ENGLISH, "%d%s", currentStats!!.getCPUUsage(previousStats!!), "%")
            val temp: ProcessorStat? = previousStats
            previousStats = currentStats
            currentStats = temp
            return resultString
        }
    }

    private fun updateWifiSignal(): String {
        // Update wifi signal strength.
        var signalStrength = "--"
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiManager != null) {
            val dbm: Int = wifiManager.getConnectionInfo().getRssi()
            if (dbm != -127) {
                signalStrength = "$dbm dBm " //NON-NLS
                if (dbm >= -35 && dbm <= 0) {
                    signalStrength += "(" + context.resources.getString(R.string.excellent_signal_tc) + ")" //NON-NLS
                } else if (dbm >= -55 && dbm <= -36) {
                    signalStrength += "(" + context.resources.getString(R.string.good_signal_tc) + ")" //NON-NLS
                } else if (dbm >= -70 && dbm <= -56) {
                    signalStrength += "(" + context.resources.getString(R.string.fair_signal_tc) + ")" //NON-NLS
                } else if (dbm >= -200 && dbm <= -71) {
                    signalStrength += "(" + context.resources.getString(R.string.poor_signal_tc) + ")" //NON-NLS
                }
            }
        }
        return signalStrength
    }

    private fun updateStorageUse(): String {
        // Get the free space on /data.
        var returnString = ""
        try {
            BufferedReader(
                InputStreamReader(Runtime.getRuntime().exec("df /data").inputStream)
            ).use {reader->
                reader.readLine()
                val storageLine = reader.readLine()
                val parts = storageLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size >= 5) {
                    returnString = parts[4]
                }
            }
        } catch (_: Exception) { }
        return returnString
    }

    private fun updateSdCardUse(): String {
        // Get the free space on SD card
        var returnString = ""
        try {
            BufferedReader(
                InputStreamReader(Runtime.getRuntime().exec("df /storage/sdcard").inputStream)
            ).use {reader->
                reader.readLine()
                val storageLine: String = reader.readLine()
                val parts = storageLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size == 6) {
                    returnString = parts[4]
                }
            }
        } catch (_: Exception) { }
        return returnString
    }
}


/**
 * This is used to read the CPU stats from /proc/stat.  We can take two objects and figure out
 * what the actual CPU usage is in percentage.
 * COPIED OUT OF Chimera/src/main/java/com/jht/chimera/lib/widget/dialog/SettingsDlg.java
 */
private class ProcessorStat {
    // These are the CPU usage times read from /proc/stat.
    private var user: Long = 0
    private var nice: Long = 0
    private var system: Long = 0

    /**
     * @return The idle CPU time.
     */
    private var idleTime: Long = 0
    private var iowait: Long = 0
    private var irq: Long = 0
    private var softirq: Long = 0
    val totalTime: Long
        /**
         * @return The total CPU time adding up all fields.
         */
        get() = user + nice + system + idleTime + iowait + irq + softirq

    /**
     * @param previousStat The previous cpu usage stat reading.
     * @return The amount of time the processor spent doing work from the previous stat.
     */
    fun getCPUUsage(previousStat: ProcessorStat): Int {
        val time = totalTime - previousStat.totalTime
        val idle = idleTime - previousStat.idleTime
        return if (time == 0L || idle == 0L) 0 else ((time - idle) * 100 / time).toInt()
    }

    /**
     * Reads the /proc/stat and stores the value in the object.
     */
    fun getStats() {
        try {
            BufferedReader(
                InputStreamReader(Runtime.getRuntime().exec("cat /proc/stat").inputStream)
            ).use {reader->
                while (true) {
                    val cpuLine = reader.readLine()
                    if (cpuLine.startsWith("cpu")) { //NON-NLS
                        val parts = cpuLine.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray() //NON-NLS
                        if (parts.size >= 8) {
                            user = parts[1].toLong()
                            nice = parts[2].toLong()
                            system = parts[3].toLong()
                            idleTime = parts[4].toLong()
                            iowait = parts[5].toLong()
                            irq = parts[6].toLong()
                            softirq = parts[7].toLong()
                        }
                        return
                    }
                }
            }
        } catch (ignored: Exception) { }
    }
}
