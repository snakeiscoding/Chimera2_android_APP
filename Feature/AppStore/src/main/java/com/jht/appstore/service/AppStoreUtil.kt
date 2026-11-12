package com.jht.appstore.service


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.DAPIEnvironment
import com.jht.appstore.interfaces.AppStoreAppInfo
import com.jht.appstore.service.AppStoreParser.fromJsonToAppList
import com.jht.util.ContextHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.util.concurrent.ConcurrentHashMap


private const val TAG = "AppStore"
private const val defaultLocale = "en_us"

/** Get the current locale (ie en_US) */
@SuppressLint("ObsoleteSdkInt")
private fun getCurrentLocale(context: Context?): String {
    if (context == null) return defaultLocale
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        context.resources.configuration.locale
    }
    return "${locale.language}_${locale.country}".lowercase()
}

/** Get the localized string for the current locale in map, then default, then empty string. */
private fun Map<String, String>?.getLocalizedString(): String {
    if (this.isNullOrEmpty()) return ""
    val context = ContextHelper.context
    val currentLocale = getCurrentLocale(context)
    // Try to get text for current locale, then default, then empty string
    return this[currentLocale]
        ?: this[defaultLocale]
        ?: ""
}


@Serializable
private data class DetailedAppInfo(
    @SerialName("name") val nameMap: Map<String, String> = emptyMap(),
    @SerialName("packageName") val packageName: String = "",
    @SerialName("iconUrl") val iconUrl: String = "",
    @SerialName("description") val descriptionMap: Map<String, String> = emptyMap(),
    @SerialName("downloadUrl") val downloadUrl: String = "",
    @SerialName("permissions") val permissions: List<String> = emptyList(),
    @SerialName("versionCode") val versionCode: Long = 0L,
)

object AppStoreParser {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true   // tolerate extra fields
        isLenient = true           // tolerate relaxed JSON
        encodeDefaults = true      // use defaults if missing
        allowTrailingComma = true
    }

    fun fromJsonToAppList(jsonString: String): List<AppStoreAppInfo> {
        return try {
            val detailedList = json.decodeFromString<List<DetailedAppInfo>>(jsonString)
                .map { app ->
                    app.copy(
                        nameMap = app.nameMap.mapKeys { it.key.lowercase() },
                        descriptionMap = app.descriptionMap.mapKeys { it.key.lowercase() },
                    )
                }
                .distinctBy { it.packageName } // make sure the package name is unique

            // convert DetailAppInfo to AppStoreAppInfo which has the localized name and description
            detailedList.map { app ->
                AppStoreAppInfo(
                    name = app.nameMap.getLocalizedString(),
                    packageName = app.packageName,
                    iconUrl = app.iconUrl,
                    description = app.descriptionMap.getLocalizedString(),
                    downloadUrl = app.downloadUrl,
                    permissions = app.permissions,
                    versionCode = app.versionCode,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}", e)
            emptyList()
        }
    }
}

// TODO we need a helper class somewhere to do this since this will be a common function across modules
private fun mapDapiEnv(env: String): DAPIEnvironment {
    return DAPIEnvironment.entries.find { it.name.equals(env, true) } ?: DAPIEnvironment.production
}


object AppStoreConfigManager {
    @SuppressLint("SdCardPath")
    private const val DEBUG_JSON_PATH = "/sdcard/debug_app_store_list.json"
    private const val SETTING_FILEPATH = "/settings/appstore/app_store_list.json"
    private var cachedApps: List<AppStoreAppInfo>? = null

    /**
     * Download the AppStore config file
     * return true if a valid json was downloaded
     */
    suspend fun downloadConfig(clubId: Int, consoleType : String, dapiEnvironment: String): Boolean {
        val appStoreApi = AppStoreApi(AppStoreClient())
        val appStoreJson = appStoreApi.getAppStoreList(clubId, consoleType, mapDapiEnv(dapiEnvironment))
        appStoreJson?.let {
            if (loadAppListFromString(it).isNotEmpty()) {
                saveAppList(it)
                Log.d(TAG, "--> new app list downloaded")
                return true
            }
        }
        return false
    }

    /** Load and parse the JSON file. Returns cached data if already loaded. */
    private fun loadAppList(): List<AppStoreAppInfo> {
        cachedApps?.let { return it }
        val context = ContextHelper.context
        val file = File(context?.filesDir, SETTING_FILEPATH)
        if (!file.exists()) {
            Log.e(TAG, "JSON file not found at: ${file.absoluteFile}")
            return emptyList()
        }

        return try {
            FileReader(file).use { reader ->
                val json = reader.readText()
                val appList = fromJsonToAppList(json)

                // load debug json for ability to add more to app store
                val debugJson = File(DEBUG_JSON_PATH)
                var finalAppList = if (debugJson.exists()) {
                    val debugAppList = fromJsonToAppList(debugJson.readText())
                    appList + debugAppList
                } else {
                    appList
                }

                // update the installed state if the app is actually installed
                context?.packageManager.let { pm ->
                    finalAppList = finalAppList.map { app ->
                        try {
                            pm?.getPackageInfo(app.packageName, 0)
                            app.copy(installed = true)
                        } catch (_: Exception) {
                            app
                        }
                    }
                }

                cachedApps = finalAppList
                finalAppList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}", e)
            emptyList()
        }
    }

    /** Load and parse the JSON string.
     * return list of AppStoreAppInfo
     */
    fun loadAppListFromString(json: String): List<AppStoreAppInfo> {
        return try {
            fromJsonToAppList(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON: ${e.message}", e)
            emptyList()
        }
    }

    /** Save the AppStore json to setting */
    fun saveAppList(json: String) {
        val context = ContextHelper.context
        val path = File(context?.filesDir, SETTING_FILEPATH)
        path.parentFile?.let { parent -> if (!parent.exists()) parent.mkdirs() }
        path.writeText(json)
    }

    /** Returns the list of apps currently loaded. */
    fun getAppList(): List<AppStoreAppInfo> = cachedApps ?: loadAppList()

    /** Reset the cached list */
    fun reset() {
        cachedApps = null
    }

}

// -------------------- Coordinator --------------------
object AppInstallCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobMap = ConcurrentHashMap<String, Deferred<Boolean>>()
    private const val NUM_SIMULTANEOUS_INSTALLS = 1 // change this to number of concurrent installs
    private val semaphore = Semaphore(NUM_SIMULTANEOUS_INSTALLS)

    // Observable set of active installs by key (packageName)
    private val activeKeys = MutableStateFlow<Set<String>>(emptySet())
    private fun addActive(key: String) = activeKeys.update { it + key }
    private fun removeActive(key: String) = activeKeys.update { it - key }

    /** Start (or join) an install for this app. */
    fun launchOrJoin(app: AppStoreAppInfo, installFunction: suspend () -> Boolean): Deferred<Boolean> {
        val key = app.packageName

        // get the app from the manager to update the app object install states
        val managedApp = AppStoreConfigManager.getAppList().find { it.packageName == app.packageName }

        // run/join async task in IO scope so it doesn't end if view model is canceled
        return activeJobMap.computeIfAbsent(key) {
            scope.async {
                managedApp?.installing = true
                addActive(key)
                try {
                    // queue installs to reduce stress on console
                    semaphore.withPermit {
                        val ok = installFunction()
                        managedApp?.installed = ok
                        ok
                    }
                } finally {
                    managedApp?.installing = false
                    removeActive(key)
                    activeJobMap.remove(key)
                }
            }
        }
    }

    /**
     * Start an uninstall for the app.
     * */
    fun uninstall(app: AppStoreAppInfo, uninstall: suspend () -> Boolean): Deferred<Boolean> {
        val key = app.packageName
        val managedApp = AppStoreConfigManager.getAppList().find { it.packageName == app.packageName }
        return activeJobMap.computeIfAbsent(key) {
            scope.async {
                managedApp?.installing = true
                addActive(key)
                try {
                    semaphore.withPermit {
                        val ok = uninstall()
                        managedApp?.installed = !ok
                        ok
                    }
                } finally {
                    managedApp?.installing = false
                    removeActive(key)
                    activeJobMap.remove(key)
                }
            }
        }
    }

    fun isRunning(app: AppStoreAppInfo) = activeJobMap.containsKey(app.packageName)
    fun cancel(app: AppStoreAppInfo) {
        activeJobMap.remove(app.packageName)?.cancel()
    }

}
