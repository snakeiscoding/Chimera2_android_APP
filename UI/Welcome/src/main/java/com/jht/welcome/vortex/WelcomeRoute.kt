package com.jht.welcome.vortex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jht.welcome.WelcomeViewModel

@Composable
fun WelcomeRoute(
    welcomeViewModel: WelcomeViewModel,
) {

    val theme = remember { welcomeViewModel.theme }
    val backgroundUri = remember { welcomeViewModel.backgroundUri}
    val videoUriList = remember { welcomeViewModel.customVideoList }
    val message = remember { welcomeViewModel.message }
    val loginState = remember { welcomeViewModel.uiState }
    val networkAvailable = remember { welcomeViewModel.networkAvailable }
    val loginList = remember { welcomeViewModel.loginList }
    val registerList = remember { welcomeViewModel.registerList }
    val selectedTabKey = remember { welcomeViewModel.selectedTabKey }
    val currentLanguageIndex = remember { welcomeViewModel.currentLanguageIndex }
    val rfidHandler = remember { welcomeViewModel.rfidHandler }
    val screenTimedOut = remember { welcomeViewModel.screenTimedOut }

    WelcomeScreen(
        theme = theme.value,
        uiState = loginState.value,
        networkAvailable = networkAvailable,
        loginList = loginList.value,
        onLogin = { data -> welcomeViewModel.attemptLogin(data) },
        registerList = registerList.value,
        onRegister = { data -> welcomeViewModel.attemptRegister(data) },
        selectedTabKey = selectedTabKey.value,
        onSelectTabKey = { key -> welcomeViewModel.onSelectTabKey(key) },
        languageList = welcomeViewModel.systemModel.languageList,
        selectedLanguageIndex = currentLanguageIndex.value,
        onLanguageSelected = { language -> welcomeViewModel.systemModel.setLanguage(language) },
        resetIdle = { welcomeViewModel.resetIdle() }, // basically to allow screen saver wake from non-hardware touch things like scrcpy
        backgroundUri = backgroundUri,
        videoUriList = videoUriList,
        adList = welcomeViewModel.advertisingList.value,
        dismissAd = welcomeViewModel::dismissAdvert,
        message = message.value,
        rfidHandler = rfidHandler,
        screenTimedOut = screenTimedOut.value,
        showDebugDialog = { welcomeViewModel.showDebugDialog() },
        onMediaError = welcomeViewModel::onMediaError,
        qrUiState = welcomeViewModel.qrUiState,
        isRfidIconVisible = welcomeViewModel.needsRfidIcon(),
    )
}
