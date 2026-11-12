package com.jht.clubcom.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jht.clubcom.interfaces.ClubComModel
import com.jht.clubcom.interfaces.getClubComInjectionEntryPoint
import com.jht.widgets.WebViewWidget
import com.jht.widgets.WebViewWidgetOptions
import com.jht.widgets.helpers.LifeCycleOnPause

/*
    Widgets for use with Club Com services
 */
private const val TAG = "ClubComUi"

/** When Club Com is enabled, this widget presents the welcome UI for Club Com */
@Composable
fun ClubComWelcomeUi(
    modifier: Modifier = Modifier,
    screenSaverMode: Boolean = false,
    alternativeContent: @Composable () -> Unit ={},
) {
    val context = LocalContext.current
    val state = remember { ClubComWelcomeState(context) }
    state.onScreenSaver(screenSaverMode)
    // life cycle pause fires before dispose
    LifeCycleOnPause {
        state.onPause()
    }
    DisposableEffect(Unit) {
        onDispose { state.close() }
    }
    if (state.shown.value && state.url.value.isNotEmpty()) {
        // do we need black background or does the default transparent work?
        WebViewWidget(
            url = state.url.value,
            modifier = modifier,
            options = WebViewWidgetOptions(
                allowUserInteraction = false,
                onError = state::onError,
            )
        )
    } else {
        alternativeContent()
    }
}

private class ClubComWelcomeState(context: Context) : AutoCloseable {
    val shown = mutableStateOf(false)
    val url = mutableStateOf("")
    private val clubComModel: ClubComModel = getClubComInjectionEntryPoint(context).clubCom()
    init {
        clubComModel.isEnabled.listenAndTrigger {
            shown.value = it
        }
        clubComModel.options.listenAndTrigger {
            url.value = it.welcomeUrl?.url ?: ""
        }
    }
    fun onScreenSaver(isScreenSaverShowing: Boolean) {
        if (isScreenSaverShowing) clubComModel.startSession()
        else clubComModel.stopSession()
    }
    fun onError(errorCode: Int, errorDescription: String) {
        Log.w(TAG, "onError $errorCode '$errorDescription'")
        url.value = "about:blank"
    }
    fun onPause() {
        Log.d(TAG, "onPause")
        // should see chromium info log mention "onHidden event received"
        url.value = "javascript:onHidden()" // old code onPause calls clubComWebView.loadUrl("javascript:onHidden()");
    }
    override fun close() {
        Log.d(TAG, "close")
        url.value = "about:blank" // unload page
        clubComModel.close()
    }
}
