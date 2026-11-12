package com.jht.appstore.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.jht.appstore.interfaces.AppStoreAppInfo
import com.jht.appstore.interfaces.AppStoreModel
import com.jht.appstore.service.AppInstallCoordinator
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@Immutable
data class AppItemUiState(
    val app: AppStoreAppInfo,
    val installing: Boolean = false,
    val installed: Boolean = false,
    val installFailed: Boolean = false
)

data class AppStoreUiState(
    val items: List<AppItemUiState> = emptyList(),
)

@HiltViewModel
class AppStoreViewModel @Inject constructor(
    private val appStoreModel: AppStoreModel
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(AppStoreUiState())
    val uiState: StateFlow<AppStoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = appStoreModel.loadAppInfos().map {
                AppItemUiState(
                    app = it,
                    installing = it.installing,
                    installed = it.installed
                )
            }
            _uiState.value = AppStoreUiState(items = items)

            // if any apps are installing, join the coroutine so UI state can update
            items.forEach { if (it.installing) onInstall(it.app) }
        }
    }

    private fun updateAppState(packageName: String, transform: (AppItemUiState) -> AppItemUiState) {
        _uiState.update { state ->
            val index = state.items.indexOfFirst { it.app.packageName == packageName }
            if (index == -1) return@update state
            val newList = state.items.toMutableList()
            newList[index] = transform(newList[index])
            state.copy(items = newList)
        }
    }

    fun onInstall(app: AppStoreAppInfo) = viewModelScope.launch {
        updateAppState(app.packageName) {
            it.copy(installing = true)
        }

        val ok = try {
            AppInstallCoordinator.launchOrJoin(app) {
                val result = appStoreModel.install(app)
                if (result) appStoreModel.notifyAppListChanged()
                result
            }.await()
        } catch (_: CancellationException) {
            // UI scope canceled; install still continues in IO scope
            return@launch
        }

        updateAppState(app.packageName) {
            it.copy(
                installing = false,
                installed = ok,
                installFailed = !ok
            )
        }
    }

    fun onLaunch(app: AppStoreAppInfo) = viewModelScope.launch {
        appStoreModel.launch(app)
    }

    fun onUninstall(app: AppStoreAppInfo) = viewModelScope.launch {
        updateAppState(app.packageName) {
            it.copy(installing = true)
        }
        val ok = try {
            AppInstallCoordinator.uninstall(app) {
                val result = appStoreModel.uninstall(app)
                if (result) appStoreModel.notifyAppListChanged()
                result
            }.await()
        } catch (_: CancellationException) {
            return@launch
        }

        updateAppState(app.packageName) {
            it.copy(
                installing = false,
                installed = !ok,
                installFailed = false
            )
        }
    }

    fun showDeleteIcon() : Boolean = appStoreModel.shouldPersistInstalledApps()

}

