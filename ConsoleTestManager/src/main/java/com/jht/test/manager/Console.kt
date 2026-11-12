package com.jht.test.manager

import com.jht.test.util.SystemCommandHelper
import com.jht.test.util.readFileToString
import com.jht.test.util.simpleSleep
import java.io.File
import java.util.*

class Console (private val ipAddress: String, val parallel: Boolean = false) {

    private val adbCommand = if(parallel) { "adb -s $ipAddress" } else { "adb" }
    private val adbCommandLite = "adb"

    fun updateUpdateManager(): Boolean {
        // This function is only necessary because the update manager does not have the ability to update
        // with RSCU over FTP.  This function could most likely be deleted in the future.
        println("Updating update manager on $ipAddress")
        disconnect()
        if (connect() && notInUpdate()) {
            if (SystemCommandHelper().execute(
                    "$adbCommand install -r -d UpdateService-chimera_main-matrix-chimera-debug.apk",
                    60000
                ).result != 0
            ) {
                disconnect()
                println("Failed to install update manager on $ipAddress")
                return false
            }
            simpleSleep(2000)
            SystemCommandHelper().execute("$adbCommand shell sync", 1000)
            SystemCommandHelper().execute("$adbCommand shell reboot", 1000)
            println("Updated update manager on $ipAddress")
            disconnect()
            return true
        }
        println("Did not update $ipAddress.  Failed to connect or in update.")
        return false
    }

    fun update(): Boolean {
        println("Need to run RSCU on $ipAddress")
        disconnect()
        if (connect()) {
            val result = SystemCommandHelper().execute(
                String.format(
                    Locale.ENGLISH,
                    "$adbCommand shell am broadcast -a com.jht.automatedupdate --ez rscu true --es rscu_version \"%s\" --es rscu_location \"%s\" --es rscu_md5 \"%s\"",
                    File("rscu_version.txt").readFileToString(),
                    File("update.debug.xml.rscu_location.txt").readFileToString(),
                    File("update.debug.xml.rscu_md5.txt").readFileToString()
                ), 5000
            )
            disconnect()
            println("RSCU result on $ipAddress -> ${result.result}")
            return result.result == 0
        }
        return false
    }

    fun disconnect() {
        if(parallel) {
            SystemCommandHelper().execute("$adbCommandLite disconnect $ipAddress", 5000)
        }
        else {
            SystemCommandHelper().execute("$adbCommandLite disconnect", 5000)
        }
    }

    fun connect(): Boolean {
        if (isConnected()) {
            return root()
        }
        val result = SystemCommandHelper().execute("$adbCommandLite connect $ipAddress", 5000)
        if (result.result == 0 || result.output.contains("connected to")) {
            return root()
        }
        return false
    }

    private fun root() : Boolean {
        val result = SystemCommandHelper().execute("$adbCommand shell whoami", 5000)
        if(result.result != 0) {
            disconnect()
            return false
        }
        if(!result.output.contains("root")) {
            SystemCommandHelper().execute("$adbCommand root", 5000)
            simpleSleep(2000)
            SystemCommandHelper().execute("$adbCommandLite connect $ipAddress", 5000)
        }
        return true
    }


    fun checkChimeraVersion(version: String) : Boolean {
        val result = SystemCommandHelper().execute("$adbCommand shell dumpsys package com.jht.chimera")
        for(line in result.output.lines()) {
            if(line.contains("versionName")) {
                println(line)
                return line.trim().trimEnd().endsWith(version)
            }
        }
        return false
    }

    private fun isConnected(): Boolean {
        val result = SystemCommandHelper().execute("$adbCommandLite devices", 5000)
        return result.result == 0 && result.output.contains(ipAddress)
    }

    private fun notInUpdate(): Boolean {
        return SystemCommandHelper().execute("$adbCommand shell grep -e \"UpdateInProgress.*false\" /data/data/com.jht.updateservice/shared_prefs/UpdateManagerPreferences.xml", 5000).result == 0
    }

    private fun chimeraHasFocus(): Boolean {
        val result = SystemCommandHelper().execute("$adbCommand shell dumpsys window windows")
        if(result.result == 0) {
            for(line in result.output.lines()) {
                if(line.contains("mCurrentFocus")) {
                    return line.contains("com.jht.chimera")
                }
            }
        }
        return false
    }

    fun updateComplete(): Boolean {
        println("Checking update complete on $ipAddress")
        return connect() && notInUpdate() && chimeraHasFocus()
    }

}