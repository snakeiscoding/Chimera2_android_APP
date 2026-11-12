package com.jht.chimera.service.appstore


import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import com.jht.appstore.interfaces.AppStoreAppInfo
import com.jht.appstore.interfaces.AppStoreModel
import com.jht.appstore.service.AppStoreConfigManager
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.AppStoreKeys
import com.jht.chimera.lib.download.DownloadManager
import com.jht.chimera.lib.download.DownloadRequest
import com.jht.chimera.lib.download.IDownloadStatus
import com.jht.datamatrixlib.DataMatrix
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "AppStore"

@SuppressLint("SdCardPath")
private const val INSTALL_FOLDER = "/sdcard/appstore"


// -------------------- Installer --------------------
object AppStoreInstaller {
    private const val INSTALLED_APP_LIST_FILEPATH = "/settings/appstore/installed_app_list.txt"

    /**
     * Install the APX or XAPK
     * @param apkPath absolute local path (e.g. /sdcard/appstore/app.apk)
     * return the exit code where 0 means successful
     */
    private fun installApp(apkPath: String): Int {
        var exitCode = -1
        if (apkPath.lowercase().endsWith(".apk")) {
            Log.d(TAG, "Installing APK: $apkPath")
            val executeAsRoot = ExecuteAsRoot()
            val script = """
                pm install -r -d  $apkPath
                [ "$?" -eq 0 ] && exit 0 || exit 1
            """.trimIndent()
            executeAsRoot.addCommand(script)
            exitCode = executeAsRoot.executeWithExitCode()
            Log.d(TAG, "--> APK Exit code: $exitCode")

            // Note: can refactor script to return exit and include this in above script
            ExecuteAsRoot.run("rm -f $apkPath") // cleanup
        } else if (apkPath.lowercase().endsWith(".xapk")) {
            Log.d(TAG, "Installing XAPK: $apkPath")
            val fileName = apkPath.substringAfterLast('/')
            val unzipFolder = "$INSTALL_FOLDER/tmp_$fileName"
            val executeAsRoot = ExecuteAsRoot()

            // script logic from our update.sh for XAPK
            // if we add the logic to install the obb files, we need to make sure we remove them during uninstall
            val script = """
                set -e
                set -o pipefail
                
                rm -rf $unzipFolder 
                mkdir $unzipFolder
            
                if command -v unzip &> /dev/null; then
                    unzip $apkPath -d $unzipFolder
                else
                    busybox unzip $apkPath -d $unzipFolder
                fi
            
                # Note: Do we need to install obb file if Android folder exists like in update.sh script?
                # /sdcard/Android/obb/package doesn't appear to get removed with pm uninstall
                SESSION_ID=$(pm install-create -r -d | cut -d "[" -f2 | cut -d "]" -f1)
                INDEX=0
                for file in $unzipFolder/*.apk; do
                    pm install-write ${'$'}SESSION_ID ${'$'}INDEX ${'$'}file
                    INDEX=$((INDEX+1))
                done
                pm install-commit ${'$'}SESSION_ID
            
                [ "$?" -eq 0 ] && exit 0 || exit 1
            """.trimIndent()
            executeAsRoot.addCommand(script)
            exitCode = executeAsRoot.executeWithExitCode()
            Log.d(TAG, "--> XAPK Exit code: $exitCode")

            // Note: can refactor script to return exit and include this in above script
            ExecuteAsRoot.run("rm -rf $unzipFolder ; rm -f $apkPath") // cleanup
        }
        return exitCode
    }

    /**
     * Download a single file and install it, then delete it.
     * @param url remote URL (use app.downloadUrl)
     * @param outFile absolute local path (e.g. /sdcard/appstore/app.apk)
     * return true if install was successful
     */
    private fun downloadAndInstall(url: String, outFile: String): Boolean {
        // make sure install folder exists
        File(INSTALL_FOLDER).apply { if (!exists()) mkdirs() }

        val downloadManager = DownloadManager()
        val request = DownloadRequest(outFile, url, "")
        val latch = CountDownLatch(1)
        val result = AtomicBoolean(false)

        val callback = IDownloadStatus { request, status, errorCode ->
            try {
                // TODO ignoring MD5 since it doesn't match etag? Figure if we need to refactor DownloadManager to add logic to skip MD5 check
                if (status == IDownloadStatus.STATUS_CODE.SUCCESS || status == IDownloadStatus.STATUS_CODE.MD5_ERROR) {
                    if (installApp(outFile) == 0) result.set(true)
                } else {
                    Log.d(TAG, "Download failed: status=$status error=$errorCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during install: ${e.message}", e)
            } finally {
                latch.countDown()
            }
        }

        downloadManager.download(request, callback)

        // wait for download to finish
        try {
            latch.await()
        } catch (_: InterruptedException) {
        }

        return result.get()
    }

    /** Download and install a single app.
     * @param app app to install
     * return true if successful
     * */
    @SuppressLint("SdCardPath")
    fun installApp(app: AppStoreAppInfo): Boolean {
        try {
            val fileName = app.downloadUrl.substringAfterLast('/')
            val ok = downloadAndInstall(app.downloadUrl, "$INSTALL_FOLDER/$fileName")
            if (ok) recordInstalledApp(app.packageName) // record app so it can be uninstalled
            return ok
        } catch (e: Exception) {
            Log.e(TAG, "Error installing app: ${e.message}", e)
            return false
        }
    }

    /**
     * Uninstall the app and remove the record after that.
     * @param app app to uninstall
     * @return true if uninstall app successfully.
     * */
    fun uninstall(app: AppStoreAppInfo): Boolean {
        ExecuteAsRoot.run("pm uninstall ${app.packageName}")
        try {
            val context = ContextUtil.getApplicationContext()
            val installedListFile = File(context?.filesDir, INSTALLED_APP_LIST_FILEPATH)
            if (installedListFile.exists()) {
                installedListFile.readLines().filterNot { it == app.packageName }.let {
                    installedListFile.writeText(it.joinToString(
                        separator = "\n",
                        postfix = "\n"))
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /** Record the installed app to a file in case the console is rebooted during a workout.
     * This list will be used to uninstall all apps.
     * @param packageName package name of the app
     * */
    private fun recordInstalledApp(packageName: String) {
        try {
            val context = ContextUtil.getApplicationContext()
            val file = File(context?.filesDir, INSTALLED_APP_LIST_FILEPATH)

            // Ensure parent directory and file exists
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()

            if (!file.readLines().any { it.trim() == packageName }) {
                file.appendText("$packageName\n")
                Log.d(TAG, "Recorded installed app: $packageName")
            } else {
                Log.d(TAG, "App already recorded: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording installed app: ${e.message}", e)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            val pm = ContextUtil.getApplicationContext()?.packageManager
            pm?.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Uninstall all apps in the list and clean temp install folder. */
    fun uninstallAll(appList: List<AppStoreAppInfo>) {
        val context = ContextUtil.getApplicationContext()
        val installedListFile = File(context?.filesDir, INSTALLED_APP_LIST_FILEPATH)

        if (installedListFile.exists()) {
            try {
                val packages = installedListFile.readLines().filter { it.isNotBlank() }

                for (pkg in packages) {
                    Log.d(TAG, "Uninstalling $pkg")
                    ExecuteAsRoot.run("pm uninstall $pkg")
                }

                // Delete the entire file after all uninstalls
                installedListFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error uninstalling from installed list: ${e.message}", e)
            }
        }

        // just in case of weird edge case, uninstall any app from the list if it's installed
        for (app in appList) {
            if (isAppInstalled(app.packageName)) {
                Log.d(TAG, "-->Uninstalling ${app.packageName}")
                ExecuteAsRoot.run("pm uninstall ${app.packageName}")
            }
        }

        // make sure all the install files are deleted to handle reboots during a workout
        ExecuteAsRoot.run("rm -rf $INSTALL_FOLDER/*")
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun getVersionCode(packageName: String): Long {
        return try {
            val pm = ContextUtil.getApplicationContext()!!.packageManager
            val pi = pm.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
        } catch (e: Exception) {
            Log.e(TAG, "getInstalledVersionCode error for $packageName: ${e.message}", e)
            0L
        }
    }

    /** Uninstall packages that have been removed from AppStore list and update apps with different version */
    fun refreshInstalledApps(appList: List<AppStoreAppInfo>): Boolean {
        var hasAppBeenUpdated = false
        try {
            val context = ContextUtil.getApplicationContext()
            val file = File(context?.filesDir, INSTALLED_APP_LIST_FILEPATH)

            if (!file.exists()) return false
            if (appList.isEmpty()) return false

            // Read recorded packages
            val recordedPackages = file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val appByPkg = appList.associateBy { it.packageName }
            val validPackages = appByPkg.keys

            // Create list of AppStoreAppInfo objects for removal
            val toRemove = recordedPackages
                .filterNot { it in validPackages }
                .map { pkg -> AppStoreAppInfo(packageName = pkg) }
            for (app in toRemove) {
                uninstall(app)
                Log.d(TAG, "Removed installed app: ${app.packageName}")
            }

            // Create list of AppStoreAppInfo objects for check for update
            val toUpdate = recordedPackages
                .filter { it in validPackages }
                .mapNotNull { pkg -> appByPkg[pkg] }
            val appsToReinstall = mutableListOf<AppStoreAppInfo>()
            // Remove first so it's gone from the workout
            for (app in toUpdate) {
                val apkVersionCode = getVersionCode(app.packageName)
                if (apkVersionCode == 0L || app.versionCode == 0L) continue // skip if no valid version is found
                if (apkVersionCode != app.versionCode) {
                    uninstall(app)
                    appsToReinstall.add(app)
                    Log.d(TAG, "Remove app: ${app.packageName} ${app.versionCode} to new $apkVersionCode")
                }
            }
            // Install the new version of app
            for (app in appsToReinstall) {
                installApp(app)
                Log.d(TAG, "Update app: ${app.packageName}")
                hasAppBeenUpdated = true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshInstalledApps: ${e.message}", e)
        }
        return hasAppBeenUpdated
    }
}


@Singleton
class MatrixAppStoreModel @Inject constructor() : AppStoreModel {
    override fun loadAppInfos(): List<AppStoreAppInfo> {
        return AppStoreConfigManager.getAppList()
    }

    override fun install(app: AppStoreAppInfo): Boolean {
        return AppStoreInstaller.installApp(app);
    }

    override fun launch(app: AppStoreAppInfo) {
        LaunchAppUtil.launchApp(ContextUtil.getApplicationContext(), app.uuid, null)
    }

    override fun uninstallAll() {
        AppStoreInstaller.uninstallAll(AppStoreConfigManager.getAppList())
    }

    override fun notifyAppListChanged() {
        DataMatrix.sendEvent(AppStoreKeys.EVENT_APP_LIST_CHANGED)
    }

    override fun notifyNewAppConfig() {
        DataMatrix.sendEvent(AppStoreKeys.EVENT_NEW_APP_LIST)
    }

    override fun uninstall(app: AppStoreAppInfo): Boolean {
        return AppStoreInstaller.uninstall(app)
    }

    override fun shouldPersistInstalledApps(): Boolean {
        return ProductConfig.isRetail()
    }

    override fun refreshInstalledApps(): Boolean {
        return AppStoreInstaller.refreshInstalledApps(AppStoreConfigManager.getAppList())
    }
}



