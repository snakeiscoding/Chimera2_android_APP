package com.jht.appstore.interfaces

import java.util.UUID

data class AppStoreAppInfo(
    val name: String = "",
    val packageName: String = "",
    val iconUrl: String = "",
    val description: String = "",
    val downloadUrl: String = "",
    val permissions: List<String> = emptyList(),
    val versionCode: Long = 0L,
    var installing: Boolean = false, // for internal state
    var installed: Boolean = false // for internal state (NOTE: we could use an enum instead of installing and installed)
) {
    /** Dynamically computed UUID from package name */
    val uuid: String get() = UUID.nameUUIDFromBytes(packageName.toByteArray()).toString()
}

interface AppStoreModel {
    /** Use to load the app list */
    fun loadAppInfos(): List<AppStoreAppInfo>
    /** Use to download and install app */
    fun install(app: AppStoreAppInfo): Boolean
    /** Use to launch installed app */
    fun launch(app: AppStoreAppInfo)
    /** Use to uninstall all apps */
    fun uninstallAll()
    /** Use to update the UI */
    fun notifyAppListChanged()
    /** Use to notify processes need to update their app list */
    fun notifyNewAppConfig()
    /** Use to uninstall specified app */
    fun uninstall(app: AppStoreAppInfo): Boolean
    /** Use to check if apps persist */
    fun shouldPersistInstalledApps(): Boolean
    /** Run logic to uninstall packages that have been removed from AppStore list and update apps with different version */
    fun refreshInstalledApps(): Boolean
}
