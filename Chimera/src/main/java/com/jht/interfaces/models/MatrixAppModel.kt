package com.jht.interfaces.models


import com.jht.chimera.lib.common.datamatrixkeys.AppStoreKeys
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.workoutapps.AppIconManager
import com.jht.chimera.lib.workoutapps.AppInfo.Category
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.ui.machinesettings.JHTTypes
import com.jht.data.AppInfo
import com.jht.data.AppInfoCategory
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener


class MatrixAppModel : AppModel() {
    private val manager: CallbackManager = CallbackManager()

    override fun close() {
        manager.cancelAllListeners()
    }

    private val appListChangeListener =
        AppListChangeListener(manager) {
            AppIconManager.getInstance().visibleApps.map {
                AppInfo(
                    title = it.getAppName(),
                    uuid = it.uuid,
                    icon = it.getAppIcon(),
                    locked = it.appInfo.state == JHTTypes.FEATURE_STATE.FEATURE_DISABLED,
                    category = it.appInfo.category.toAppInfoCategory(),
                    tunerDependent = it.appInfo.tunerDependent,
                )
            }
        }

    override val appsAvailableInWorkout: DataValueListener<List<AppInfo>> = appListChangeListener

    override fun refreshAppList() {
        appListChangeListener.refreshAppList()
    }
}

private fun Category.toAppInfoCategory(): AppInfoCategory {
    return when (this) {
        Category.Workout -> AppInfoCategory.Workout
        Category.Entertainment -> AppInfoCategory.Entertainment
        Category.System -> AppInfoCategory.System
        Category.Integration -> AppInfoCategory.Integration
        Category.FullIntegration -> AppInfoCategory.FullIntegration
        else -> AppInfoCategory.Workout
    }
}


/** Class to handle situations were system may need to change the list of app icons */
private class AppListChangeListener(
    private val manager: CallbackManager,
    private val getList: () -> List<AppInfo>,
) : DataValueListener<List<AppInfo>> {
    private var initialized = false
    private val callbacks: MutableSet<(List<AppInfo>) -> Unit> = mutableSetOf()
    private fun updateList() {
        val currentList = value // compute list once for all callbacks
        callbacks.forEach { it(currentList) }
    }

    init {
        initializeIfNeeded()
        // only need to attach this once per construction
        manager.cancelAllEventCallbacks.add {
            initialized = false
        }
    }
    private fun initializeIfNeeded() {
        if (!initialized) {
            callbacks.clear()
            // listener for changes that impact available app list
            manager.addStringListener(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, { updateList() }, false) // show conditions based on starting app
            manager.addBoolListener(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, { updateList() }, false) // VA icon can depend on available content
            manager.addEventListener(AppStoreKeys.EVENT_APP_LIST_CHANGED) {
                refreshAppList()
            } // app list changed
            // geolocation probably does not change enough to be worth including?
            initialized = true
        }
    }

    fun refreshAppList() {
        // App Store - refresh list
        AppManager.getInstance().reset()
        AppIconManager.getInstance().reset()
        updateList()
    }

    override val value: List<AppInfo>
        get() = getList()
    override fun listen(onChange: (List<AppInfo>) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
    }
    override fun listenAndTrigger(onChange: (List<AppInfo>) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
        onChange(value)
    }
}

