package com.jht.clubcom.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.jht.androidcommonalgorithms.network.AddressHelper
import com.jht.clubcom.interfaces.getClubComInjectionEntryPoint
import com.jht.clubcom.interfaces.ClubComOptions
import com.jht.clubcom.interfaces.ClubComModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.services.InlineConsoleService
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.util.Locale

private const val TAG = "ClubComService"

//DO NOT CHANGE THIS! This is an internet connected url we will ping to get the correct media server identifier to access club specific content
private const val EXTERNAL_URL = "http://pvsws.clubcom.com/configuration.svc/getbaseurl?model=" //NON-NLS
/** was the default url ending before */
const val MODEL_SEVENX = "7x"
/** chimera 22 */
const val MODEL_TOUCHXL = "touchXL"
/** chimera 16 */
const val MODEL_TOUCH = "touch"
/** athena */
const val MODEL_CXV = "cxv"

class ClubComConsoleService(context: Context): InlineConsoleService(context) {
    companion object {
        const val ID = TAG // UUID or unique friendly name or just use the log tag?
    }
    override val id: String = ID // do we need instance to be able to return its ID?

    @Volatile
    private var isDownloadConfigRunning = false
    private var sessionStarted = false
    private var configURL: String? = null
    private var startSessionUrl: String? = ""
    private var endSessionUrl: String? = ""

    private val systemModel: SystemModel
    private val workoutModel: WorkoutModel
    private val clubComModel: ClubComModel

    init {
        val injection = getInjectionEntryPoint(context)
        systemModel = injection.system()
        workoutModel = injection.workout()
        clubComModel = getClubComInjectionEntryPoint(context).clubCom()
    }

    override fun start() {
        systemModel.facilityID.listen {
            reset()
            getConfig()
        }
        systemModel.isInternetAvailable.listenAndTrigger { getConfig() }
        systemModel.isMachineSetup.listen { getConfig() }
        clubComModel.onStartSession.listen { startSession() }
        clubComModel.onStopSession.listen { endSession() }
        workoutModel.isWorkoutStopped.listen {
            if (it) {
                endSession()
                getConfig()
            }
        }
        workoutModel.isWorkoutRunning.listen {
            if (it) startSession()
        }
    }

    override fun stop() {
        systemModel.close()
        workoutModel.close()
        clubComModel.close()
    }

    fun reset() {
        configURL = null
        startSessionUrl = ""
        endSessionUrl = ""
        clubComModel.options.value = ClubComOptions()
    }

    private fun getConfig() {
        if (!systemModel.isMachineSetup.value) return
        if (!clubComModel.isEnabled.value) return
        if (!AddressHelper.isNetworkConnected(context)) return
        downloadConfig()
    }

    private fun downloadConfig() {
        if (isDownloadConfigRunning) return

        Thread {
            isDownloadConfigRunning = true
            // only get the config url once
            if (configURL == null) {
                val key = AddressHelper.getMACAddressStr("eth0", false) //NON-NLS
                val id = systemModel.facilityID.value
                val model = clubComModel.urlModelString
                val baseURL = "$EXTERNAL_URL$model&key=$key&partner=matrix&id=$id" //NON-NLS
                Log.d(TAG, "baseURL: $baseURL")
                configURL = getJsonString(baseURL)
            }

            if (configURL?.isNotEmpty() == true) {
                Log.d(TAG, "configURL: $configURL")

                val config = getJsonString(configURL!!)
                if (config != null) {
                    Log.d(TAG, config)
                    consumeJSON(config)
                }
            }
            isDownloadConfigRunning = false
        }.start()
    }

    private fun getJsonString(urlPath: String): String? {
        var jsonString: String? = null
        try {
            val url = URL(cleanUrl(urlPath))
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 100000
            conn.connectTimeout = 150000
            conn.doInput = true
            conn.doOutput = false
            conn.requestMethod = "GET"
            conn.connect()
            Log.d(TAG, "Connection response code: " + conn.responseCode)
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val bufferedReader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))

                val sb = StringBuilder()
                var inputLine: String?
                while ((bufferedReader.readLine().also { inputLine = it }) != null) {
                    sb.append(inputLine)
                }
                bufferedReader.close()
                jsonString = decodeText(sb.toString())
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return jsonString
    }

    private fun cleanUrl(url: String): String {
        return url.replace("\"", "")
    }

    private fun decodeText(text: String): String {
        var result = text
        try {
            result = URLDecoder.decode(text, "UTF-8")
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return result
    }

    private fun consumeJSON(json: String) {
        try {
            val jsonSerialize = Json {
                ignoreUnknownKeys = true // just skip unknown keys instead of throwing
                isLenient = true // does not have to be strict format json
            }
            val options = jsonSerialize.decodeFromString<ClubComOptions>(json)

            //get welcome URL
            val welcomeUrl = options.welcomeUrl?.url
            if (welcomeUrl?.isNotEmpty() == true) {
                Log.d(TAG, "welcome url: $welcomeUrl")
                //get IP from welcome URL
                try {
                    val serverIP = URL(welcomeUrl).host
                    Log.d(TAG, "ipUrl host: $serverIP")
                } catch (e: MalformedURLException) {
                    Log.w(TAG, e)
                }
            }

            Log.d(TAG, "apps length: " + options.apps?.size)
            if (options.startSessionUrl?.isNotEmpty() == true) {
                startSessionUrl = options.startSessionUrl
                Log.d(TAG, "startsessionurl: $startSessionUrl")
            }
            if (options.endSessionUrl?.isNotEmpty() == true) {
                endSessionUrl = options.endSessionUrl
                Log.d(TAG, "endsessionurl: $endSessionUrl")
            }

            //get AppList
            if (options.apps != null) {
                options.apps.forEach { app ->
                    getImage(app.imageUrl, app.id)
                }
            } else {
                Log.d(TAG, "Apps JSON is null no new apps created")
            }

            //get Preroll List
            if (options.preRollUrls != null) {
                Log.d(TAG, "${options.preRollUrls.size} preroll entries found")
                val preRollUrl = options.preRollUrls.firstOrNull()?.url
                if (preRollUrl?.isNotEmpty() == true) {
                    Log.d(TAG, "Preroll Url: $preRollUrl")
                }
            } else {
                Log.d(TAG, "Prerolls JSON object is null so prerolls are not assigned")
            }
            clubComModel.options.value = options
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    private fun startSession() {
        if (sessionStarted) {
            Log.d(TAG, "session has started cannot start new session")
            return
        }
        if (startSessionUrl == null || startSessionUrl == "") {
            Log.d(TAG, "ClubCom start session URL was not provided")
            return
        }
        Thread {
            try {
                Log.d(TAG, "Calling startSession()")
                val url = URL(startSessionUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.readTimeout = 100000
                conn.connectTimeout = 150000
                conn.requestMethod = "GET"
                conn.connect()
                Log.d(TAG, "Connection response code: " + conn.responseCode)
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "START SESSION -- CONNECTION HTTP OK RESPONSE")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }.start()
        sessionStarted = true
    }

    private fun endSession() {
        if (!sessionStarted) {
            Log.d(TAG, "The session has not started so it cannot be ended")
            return
        }
        if (endSessionUrl == null || endSessionUrl == "") {
            Log.d(TAG, "ClubCom end session URL was not provided")
            return
        }
        Thread {
            try {
                Log.d(TAG, "Calling endSession()")
                val url = URL(endSessionUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.readTimeout = 100000
                conn.connectTimeout = 150000
                conn.requestMethod = "GET"
                conn.connect()
                Log.d(TAG, "Connection response code: " + conn.responseCode)
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "END SESSION -- CONNECTION HTTP OK RESPONSE")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }.start()
        sessionStarted = false
    }

    private fun getImage(imageUrl: String, uid: String): String {
        // Should this be built without package name hardcoded? such as> File(context.filesDir, "settings/apps/bitmaps/")
        val folderName = "/data/data/com.jht.chimera/files/settings/apps/bitmaps/" //NON-NLS
        File(folderName).mkdirs()
        val fileName = folderName + uid.lowercase(Locale.getDefault()) + ".png" //NON-NLS

        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val image = BitmapFactory.decodeStream(connection.inputStream)
            Log.d(TAG, "File downloaded: $fileName")
            storeImage(image, fileName)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return fileName
    }

    private fun storeImage(image: Bitmap, filepath: String) {
        val file = File(filepath)
        try {
            val fos = FileOutputStream(file.absolutePath)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            Log.d(TAG, "File Stored " + file.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

}
