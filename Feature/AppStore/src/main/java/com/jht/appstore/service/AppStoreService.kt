package com.jht.appstore.service

import android.content.Context
import com.jht.appstore.interfaces.AppStoreModel
import com.jht.appstore.interfaces.getAppStoreInjectionEntryPoint
import com.jht.interfaces.models.SystemModel
import com.jht.services.InlineConsoleService
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "AppStoreService"

class AppStoreService(context: Context) : InlineConsoleService(context) {
    companion object {
        const val ID = TAG // UUID or unique friendly name or just use the log tag?
    }

    override val id: String = ID // do we need instance to be able to return its ID?

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val systemModel: SystemModel
    private val appStoreModel: AppStoreModel

    init {
        val injection = getInjectionEntryPoint(context)
        systemModel = injection.system()
        appStoreModel = getAppStoreInjectionEntryPoint(context).appStore() // Should this follow the same pattern as the system injection?
    }

    // Currently only retail needs to persist installed apps. Add more to the list if needed.
    private fun shouldPersistInstalledApps(): Boolean {
        return appStoreModel.shouldPersistInstalledApps()
    }

    private fun cleanupAppStore() {
        // if we're not persisting apps, uninstall them
        if (!shouldPersistInstalledApps()) appStoreModel.uninstallAll()
    }

    override fun start() {
        // get config handlers
        systemModel.facilityID.listen { getConfig() }
        systemModel.dapiEnvironment.listen { getConfig() }
        systemModel.isInternetAvailable.listen { getConfig() }
        systemModel.isMachineSetup.listen { getConfig() }

        // splash reset handler
        systemModel.onSplashReset.listen { cleanupAppStore() }

        // factory reset handler
        systemModel.onFactoryReset.listen { appStoreModel.uninstallAll() }

        // make sure to cleanup before starting
        cleanupAppStore();

        // Attempt to only call getConfig
        getConfig()
    }

    override fun stop() {
        serviceScope.cancel() // this prevents coroutines from running again
        systemModel.close()
    }

    private fun getConfig() {
        if (!systemModel.isMachineSetup.value) return
        if (!systemModel.isInternetAvailable.value) return

        serviceScope.launch {
            val ok = AppStoreConfigManager.downloadConfig(systemModel.facilityID.value,
                systemModel.appStoreFilterName, systemModel.dapiEnvironment.value)

            // if there was a new AppStore config, notify other processes
            if (ok) appStoreModel.notifyNewAppConfig()

            // if we're persisting apps, refresh them
            if (shouldPersistInstalledApps()) {
                if (appStoreModel.refreshInstalledApps()) {
                    appStoreModel.notifyAppListChanged()
                }
            }
        }

    }

}
