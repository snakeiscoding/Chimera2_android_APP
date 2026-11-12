package com.jht.appstore.interfaces

class MockAppStoreModel(val appList: List<AppStoreAppInfo> = emptyList()) : AppStoreModel {

    override fun loadAppInfos(): List<AppStoreAppInfo> {
        return appList
    }

    override fun install(app: AppStoreAppInfo): Boolean {
        println("Not yet implemented")
        return true
    }

    override fun launch(app: AppStoreAppInfo) {
        println("Not yet implemented")
    }

    override fun uninstallAll() {
        println("Not yet implemented")
    }

    override fun notifyAppListChanged() {
        println("Not yet implemented")
    }

    override fun notifyNewAppConfig() {
        println("Not yet implemented")
    }

    override fun uninstall(app: AppStoreAppInfo): Boolean {
        println("Not yet implemented")
        return true
    }

    override fun shouldPersistInstalledApps(): Boolean {
        println("Not yet implemented")
        return true
    }

    override fun refreshInstalledApps(): Boolean {
        println("Not yet implemented")
        return false
    }
}