package com.jht.widgets

import android.app.Application.getProcessName
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/*
    This file contains an activity to present a closable web browser.
    Originally created for call-to-action message center messages. It could evolve into a replacement for old web browser activity.
 */

private const val TAG = "WebBrowserCompose"
private const val KeyUrlString = "com.jht.WebBrowserCompose.url"
private const val KeyOptionParcelable = "com.jht.WebBrowserCompose.options"

/** Class to package browser options */
@Parcelize
class WebBrowserComposeOptions(
    val idleTimeoutMs: Int = 0,
) : Parcelable

/** Launch a WebBrowserCompose */
fun launchWebBrowserCompose(
    context: Context,
    url: String,
    options: WebBrowserComposeOptions = WebBrowserComposeOptions(),
) {
    WebBrowserComposeActivity::class.java.canonicalName?.let {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.component = ComponentName(context.packageName, it)
        intent.putExtra(KeyUrlString, url)
        intent.putExtra(KeyOptionParcelable, options)
        context.startActivity(intent)
    }
}

/** Host activity for WebBrowserCompose */
@AndroidEntryPoint
class WebBrowserComposeActivity : RestringThemedActivity() {
    override val themedViewModel: WebBrowserComposeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val processName = getProcessName()
        // Check if the current process is not the main application process
        if (!packageName.equals(processName)) {
            // CommonRoutines.setWebViewDataDirectory did this for old web activity, avoids multiprocess data exception
            // (if WebView is already initialized, this can throw)
            runCatching { WebView.setDataDirectorySuffix(processName) }
        }
        setContent {
            WebBrowserComposeCompose(themedViewModel)
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        themedViewModel.onNewIntent(intent)
    }
}

/** Host view model for WebBrowserCompose */
@HiltViewModel
class WebBrowserComposeViewModel @Inject constructor(
    val uiBaseModel: UIBaseModel,
    val systemModel: SystemModel,
    val idleTimerModel: IdleTimerModel,
    val savedStateHandle: SavedStateHandle, // can provide launch intent extras
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val url = mutableStateOf("")
    val webViewOptions = WebViewWidgetOptions(onError = ::onWebError)
    private var webBrowserComposeOptions = WebBrowserComposeOptions()

    override fun create() {
        systemModel.onSplashReset.listen(::onClose)
    }
    override fun destroy() {
        systemModel.close()
    }
    override fun start() {
        stop() // onNewIntent flow could cause re-entrance
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        url.value = savedStateHandle.get<String>(KeyUrlString) ?: ""
        webBrowserComposeOptions = savedStateHandle.get<WebBrowserComposeOptions>(KeyOptionParcelable) ?: WebBrowserComposeOptions()
        // a regular timeout just to close the web browser should be safe in and out of workout
        idleTimerModel.addIdleTimer("WebAutoClose", webBrowserComposeOptions.idleTimeoutMs) { // this is smart enough to only apply if time >0
            onClose()
        }
        idleTimerModel.startIdleTimers()
    }
    override fun stop() {
        uiBaseModel.close()
        idleTimerModel.close()
    }
    fun onNewIntent(intent: Intent?) {
        // feels like there should be some way to make savedStateHandle update when the intent changes, but have not found it yet
        intent?.extras?.getString(KeyUrlString)?.let {
            savedStateHandle[KeyUrlString] = it
        }
        intent?.extras?.getParcelable<WebBrowserComposeOptions>(KeyOptionParcelable)?.let {
            savedStateHandle[KeyOptionParcelable] = it
        }
        // Activity.onResume will fire which calls start()
    }

    fun onWebError(errorCode: Int, errorDescription: String) {
        Log.w(TAG, "Web error $errorCode '$errorDescription'")
    }

    fun resetIdleTimers() {
        // typically it is not necessary to manually reset idle as the system is resetting on hardware events
        // however if we need to kick based on software event or other reason
        idleTimerModel.resetIdleTimers()
    }

    fun onBack() {
        webViewOptions.goBack()
    }
    fun onForward() {
        webViewOptions.goForward()
    }
    fun onRefresh() {
        webViewOptions.refresh()
    }
    fun onClose() {
        closeParent()
    }
}

/** Main compose tree for WebBrowserCompose */
@Composable
private fun WebBrowserComposeCompose(
    vm: WebBrowserComposeViewModel,
) {
    DynamicTheme(vm.theme.value) {
        Column(
            Modifier
                .background(DynamicTheme.colors.background1)
                .fillMaxSize()
        ) {
            // Controls row
            Row(
                Modifier
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 24.dp)
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                // Nav buttons
                BrowserButton(onClick = vm::onBack, iconId = R.drawable.ic_back_button_white)
                BrowserButton(onClick = vm::onForward, iconId = R.drawable.ic_forward_button_white)
                BrowserButton(onClick = vm::onRefresh, iconId = R.drawable.ic_refresh_white)
                // center mass
                Spacer(Modifier.weight(1f))
                // CLOSE button
                BrowserButton(onClick = vm::onClose, iconId = R.drawable.ic_close)
            }
            // Web view
            WebViewWidget(
                url = vm.url.value,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                options = vm.webViewOptions,
            )
        }
    }
}

@Composable
private fun BrowserButton(
    modifier: Modifier = Modifier,
    iconId: Int,
    onClick: () -> Unit = {},
) {
    ThemeFlatButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            modifier = modifier.size(40.dp),
            painter = painterResource(id = iconId),
            tint = Color.White,
            contentDescription = null,
        )
    }
}
