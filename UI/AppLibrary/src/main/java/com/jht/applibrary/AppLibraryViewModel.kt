package com.jht.applibrary

import androidx.compose.runtime.mutableStateOf
import com.jht.data.AppInfo
import com.jht.interfaces.models.AppModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class AppLibraryViewModel @Inject constructor(
    val uiBaseModel: UIBaseModel,
    val appModel: AppModel,
    val navigationModel: NavigationModel,
    val systemModel: SystemModel,
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val appInfoPages = mutableStateOf(listOf<AppInfo>())
    val ssoPrompt = mutableStateOf<AppInfo?>(null)
    val tunerConnected = mutableStateOf(true)
    val showTunerInitializingDialog = mutableStateOf(false)

    fun showTunerInitializingDialog() { showTunerInitializingDialog.value = true }
    fun dismissTunerInitializingDialog() { showTunerInitializingDialog.value = false }


    override fun create() {
        appModel.appsAvailableInWorkout.listenAndTrigger { appInfoPages.value = it }
        systemModel.tvTunerConnected.listenAndTrigger { tunerConnected.value = it }
    }

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        appModel.refreshAppList() // refresh the list in case the callbacks did not refresh the list
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        uiBaseModel.close()
        navigationModel.close()
    }

    override fun destroy() {
        // AppStore bug where the DataMatrix callback doesn't register on resume so moving cleanup to ondestroy
        // TODO We need to re-visit logic to understand what start doesn't register the callbacks on resume
        appModel.close()
    }

    /** Attempts to launch app, but checks SSO first */
    fun attemptLaunchApp(appInfo: AppInfo) {
        if (appInfo.locked) {
            // UI should provide a prompt
        } else if (navigationModel.isAppWaitingOnSso(appInfo.uuid)) {
            ssoPrompt.value = appInfo
        } else {
            navigationModel.launchApp(appInfo.uuid)
        }
    }
    /** Launches app without any checks */
    fun forceLaunchApp(appInfo: AppInfo) {
        navigationModel.launchApp(appInfo.uuid)
    }

}
