package com.jht.applibrary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun AppLibraryRoute(
    appLibraryViewModel: AppLibraryViewModel,
) {
    val appInfoList = remember { appLibraryViewModel.appInfoPages }
    val theme = remember { appLibraryViewModel.theme }
    val tunerConnected = remember { appLibraryViewModel.tunerConnected }
    val showTunerInitializingDialog = remember { appLibraryViewModel.showTunerInitializingDialog }

    AppLibraryScreen(
        appInfoList = appInfoList.value,
        theme = theme.value,
        ssoPrompt = appLibraryViewModel.ssoPrompt,
        tunerConnected = tunerConnected.value,
        showTunerInitializingDialog = showTunerInitializingDialog.value,
        onDismissTunerInitializingDialog = { appLibraryViewModel.dismissTunerInitializingDialog() },
        onAppClick = { appInfo ->
            if (appInfo.tunerDependent && !tunerConnected.value) {
                appLibraryViewModel.showTunerInitializingDialog()
            } else {
                appLibraryViewModel.attemptLaunchApp(appInfo)
            }
        },
        onForceLaunch = { appInfo -> appLibraryViewModel.forceLaunchApp(appInfo) },
    )
}
