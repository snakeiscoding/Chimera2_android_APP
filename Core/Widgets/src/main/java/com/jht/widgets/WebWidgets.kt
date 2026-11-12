package com.jht.widgets

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.net.URI

private const val TAG = "WebViewWidget"

/*
    File for Web related widgets
 */

/* keyboard can cover entries, needs further investigation
// some ideas? https://github.com/FRbrt/Compose-Keyboard-Covers-TextField-Demo/blob/main/KeyboardHidesTextField/app/src/main/java/edu/keyboardhidestextfield/DialogWithTextFieldContent.kt
@Composable
fun WebPopup(
    url: String,
    overrideUserAgent: String = "",
    onDismissRequest: () -> Unit,
) {
    // PopupFullscreen with focus would be above the keyboard
    DialogFullscreen(
        onDismissRequest,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(disabledBackground)
                .clickableNoEffect(onClick = onDismissRequest),
            contentAlignment = Alignment.Center,
        ) {
            WebViewWidget(
                url,
                Modifier.fillMaxSize(0.9f),
                overrideUserAgent,
            )
        }
    }
}*/


/**
 * Options for customizing [WebViewWidget] look or behavior
 *
 * @param onError fires for any failed request, including subresources
 * @param pageFinishedScript builds the JS evaluated after each page finishes loading, called on every load so
 *   it can return fresh values like refreshed tokens; empty runs nothing
 * @param onMainFrameError fires only when the page itself fails: a network error or an HTTP status >= 400
 *   on the main frame. Subresource failures don't trigger it.
 * @param onFirstLoadFailed fires at most once, when the main frame hits a network error before any page has
 *   finished loading. HTTP status errors and later navigations never trigger it, so a challenge page that
 *   answers 403 or 503 and then reloads itself is not treated as a failure.
 * @param onMainFrameHttpError fires when the main frame answers with an HTTP status >= 400, on any load.
 *   Network errors never trigger it.
 * @param onCreateFailed called if creating the [WebView] throws, which has happened on Parker hardware. The
 *   widget then shows nothing. The default rethrows.
 * @param autoGrantPermissions true grants every page geolocation and protected media (DRM) access without
 *   asking; false denies both. Read once when the WebView is created.
 * @param zoomEnabled false blocks pinch zoom and hides the on-screen +/- control. Read once when the WebView is created.
 * @param allowedOrigins origins like `https://survey.example.com` that [pageFinishedScript] may run on. Empty runs
 *   the script nowhere, so a redirect can't hand secrets in it to another site.
 * @param runScriptOnAnyOrigin explicit opt-in to run [pageFinishedScript] on every page, ignoring [allowedOrigins].
 *   Only for scripts that carry nothing sensitive.
 * @param webMessageBridges JS objects limited to their own origins, see [WebMessageBridge]. Read once when the
 *   WebView is created.
 * @throws IllegalArgumentException if an [allowedOrigins] entry has no scheme or host
 */
class WebViewWidgetOptions(
    val allowUserInteraction: Boolean = true,
    val overrideUserAgent: String = "",
    val onError: (errorCode: Int, errorDescription: String) -> Unit = {_,_->},
    val pageFinishedScript: () -> String = { "" },
    val onMainFrameError: (errorCode: Int, errorDescription: String) -> Unit = {_,_->},
    val zoomEnabled: Boolean = false,
    val allowedOrigins: Set<String> = emptySet(),
    val runScriptOnAnyOrigin: Boolean = false,
    val onFirstLoadFailed: (errorCode: Int, errorDescription: String) -> Unit = {_,_->},
    val onCreateFailed: (error: Throwable) -> Unit = { throw it },
    val autoGrantPermissions: Boolean = true,
    val webMessageBridges: List<WebMessageBridge> = emptyList(),
    val onMainFrameHttpError: (statusCode: Int, reasonPhrase: String) -> Unit = {_,_->},
) {
    init {
        requireOrigins(allowedOrigins)
    }

    // WebViewClient callbacks all arrive on the UI thread
    private var firstLoadSettled = false

    private val _events = Channel<WebViewWidgetCommands>(Channel.BUFFERED)
    internal val events = _events.receiveAsFlow()

    fun goBack() {
        _events.trySend(WebViewWidgetCommands.Back)
    }
    fun goForward() {
        _events.trySend(WebViewWidgetCommands.Forward)
    }
    fun refresh() {
        _events.trySend(WebViewWidgetCommands.Refresh)
    }
    fun stop() {
        _events.trySend(WebViewWidgetCommands.Stop)
    }
    fun clearHistory() {
        _events.trySend(WebViewWidgetCommands.ClearHistory)
    }
    fun clearCache() {
        _events.trySend(WebViewWidgetCommands.ClearCache)
    }
    fun clearFormData() {
        _events.trySend(WebViewWidgetCommands.ClearFormData)
    }

    internal fun zoomSettings() = WebViewZoomSettings(
        supportZoom = zoomEnabled,
        builtInZoomControls = zoomEnabled,
        displayZoomControls = zoomEnabled,
    )

    internal fun needsTouchFilter() = !allowUserInteraction || !zoomEnabled

    /** Script to run for a finished page, or null when there is nothing to run or the origin isn't allowed */
    internal fun pageFinishedScriptFor(url: String?): String? {
        if (url == "about:blank") return null
        if (!runScriptOnAnyOrigin && !originAllowed(url = url, allowedOrigins = allowedOrigins)) return null
        return pageFinishedScript().ifEmpty { null }
    }

    internal fun reportNetworkError(isForMainFrame: Boolean, errorCode: Int, description: String) {
        onError(errorCode, description)
        if (!isForMainFrame) return
        onMainFrameError(errorCode, description)
        if (!firstLoadSettled) {
            firstLoadSettled = true
            onFirstLoadFailed(errorCode, description)
        }
    }

    internal fun reportPageFinished(url: String?) {
        if (url != "about:blank") firstLoadSettled = true
    }

    /** Runs [create], handing a throw to [onCreateFailed] and returning null instead */
    internal fun <T> createWebView(create: () -> T): T? =
        runCatching(create).onFailure(onCreateFailed).getOrNull()

    internal fun allowsGeolocation() = autoGrantPermissions

    internal fun permissionsToGrant(): Array<String> =
        if (autoGrantPermissions) arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID) else emptyArray()

    /** onError stays network-only so existing callers don't start getting HTTP status codes */
    internal fun reportHttpError(isForMainFrame: Boolean, statusCode: Int, reasonPhrase: String) {
        if (!isForMainFrame || statusCode < 400) return
        onMainFrameError(statusCode, reasonPhrase)
        onMainFrameHttpError(statusCode, reasonPhrase)
    }
}
/** Zoom related [WebSettings] values, split out so the mapping is testable without a WebView */
internal data class WebViewZoomSettings(
    val supportZoom: Boolean,
    val builtInZoomControls: Boolean,
    val displayZoomControls: Boolean,
)

/**
 * Decide whether the WebView touch listener consumes an event
 *
 * With zoom off, any multi-pointer event is eaten so pages whose viewport allows scaling can't pinch zoom.
 */
internal fun blockTouch(pointerCount: Int, actionMasked: Int, zoomEnabled: Boolean, allowUserInteraction: Boolean): Boolean {
    if (!allowUserInteraction) return true
    if (zoomEnabled) return false
    return pointerCount >= 2 || actionMasked == MotionEvent.ACTION_POINTER_DOWN
}

/** Normalized `scheme://host:port` for [url], or null when it has no scheme or host */
internal fun originOf(url: String?): String? {
    val uri = runCatching { URI(url ?: return null) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    val host = uri.host?.lowercase() ?: return null
    val port = when {
        uri.port != -1 -> uri.port
        scheme == "https" -> 443
        scheme == "http" -> 80
        else -> -1
    }
    return "$scheme://$host:$port"
}

/** True when [url] has an origin and it matches one of [allowedOrigins] */
internal fun originAllowed(url: String?, allowedOrigins: Set<String>): Boolean {
    val origin = originOf(url) ?: return false
    return allowedOrigins.any { originOf(it) == origin }
}

/** @throws IllegalArgumentException for an origin without a scheme or host, which could never match */
internal fun requireOrigins(origins: Set<String>) {
    origins.forEach { require(originOf(it) != null) { "Origin needs a scheme and host: '$it'" } } //NON-NLS
}

internal enum class WebViewWidgetCommands {
    Back, Forward, Refresh, Stop, ClearHistory, ClearCache, ClearFormData,
}

/** A compose wrapper for [WebView] handling the typical boilerplate code */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility", "JavascriptInterface")
@Composable
fun WebViewWidget(
    url: String,
    modifier: Modifier = Modifier,
    options: WebViewWidgetOptions = WebViewWidgetOptions(),
) {
    // Fun note: webView will still be running the loaded page when paused unless you unload it with something like "about:blank" or call webView.onPause?
    val context = LocalContext.current
    val webView = remember {
        // Putting this in ScrollView doesn't seem to gain anything, leaving scrolling up to WebView itself
        options.createWebView {
            WebView(context).apply {
                //clipToOutline = true
                //isScrollContainer = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = CustomWebViewClient(options)
                webChromeClient = CustomWebChromeClient(options)
                settings.apply {
                    if (options.overrideUserAgent.isNotEmpty()) userAgentString = options.overrideUserAgent
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    val zoom = options.zoomSettings()
                    setSupportZoom(zoom.supportZoom)
                    builtInZoomControls = zoom.builtInZoomControls
                    displayZoomControls = zoom.displayZoomControls
                    allowFileAccess = true
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT) // defer to parent background while being created
                if (options.needsTouchFilter()) {
                    setOnTouchListener { view, event ->
                        val block = blockTouch(
                            pointerCount = event.pointerCount,
                            actionMasked = event.actionMasked,
                            zoomEnabled = options.zoomEnabled,
                            allowUserInteraction = options.allowUserInteraction,
                        )
                        // the first finger's gesture already reached the page, cancel it so a slider drag doesn't stick
                        if (block && options.allowUserInteraction && event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                            view.onTouchEvent(cancel)
                            cancel.recycle()
                        }
                        block
                    }
                }
                val listenerSupported = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
                options.webMessageBridges.forEach { bridge ->
                    val path = registerWebMessageBridge(
                        bridge = bridge,
                        listenerSupported = listenerSupported,
                        addListener = { rules ->
                            WebViewCompat.addWebMessageListener(this, bridge.jsName, rules) { _, message, _, _, _ ->
                                bridge.onMessage(message.data)
                            }
                        },
                        addInterface = { obj, name -> addJavascriptInterface(obj, name) },
                        currentUrl = { this@apply.url },
                    )
                    Log.i(TAG, "Bridge ${bridge.jsName} registered via $path") //NON-NLS
                }
            }
        }
    } ?: return

    // loading from AndroidView.update reloaded the page on every recomposition
    LaunchedEffect(url) {
        webView.loadUrl(url)
        webView.requestFocus()
    }

    LaunchedEffect(Unit) {
        options.events.collect { event ->
            when (event) {
                WebViewWidgetCommands.Back -> webView.goBack()
                WebViewWidgetCommands.Forward -> webView.goForward()
                WebViewWidgetCommands.Refresh -> webView.reload()
                WebViewWidgetCommands.Stop -> webView.stopLoading()
                WebViewWidgetCommands.ClearHistory -> webView.clearHistory()
                WebViewWidgetCommands.ClearCache -> webView.clearCache(true)
                WebViewWidgetCommands.ClearFormData -> webView.clearFormData()
            }
        }
    }

    AndroidView(
        modifier = modifier.clipToBounds(), // chimera1 error page seems to overflow composable bounds, so force clip
        factory = { webView },
    )
}

/** Customized WebChromeClient */
private class CustomWebChromeClient(
    val options: WebViewWidgetOptions,
) : WebChromeClient() {
    // allow device ID for DRM
    override fun onPermissionRequest(request: PermissionRequest?) {
        val permissions = options.permissionsToGrant()
        if (permissions.isEmpty()) request?.deny() else request?.grant(permissions)
    }
    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, options.allowsGeolocation(), false)
    }
}

/** Customized WebViewClient */
private class CustomWebViewClient(
    val options: WebViewWidgetOptions,
) : WebViewClient() {
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        view?.setBackgroundColor(android.graphics.Color.WHITE) // builtin error pages assume a white background
        val code = error?.errorCode ?: 0 // should we use ERROR_UNKNOWN or just zero as default error code?
        val description = error?.description?.toString() ?: ""
        options.reportNetworkError(isForMainFrame = request?.isForMainFrame == true, errorCode = code, description = description)
    }
    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        super.onReceivedHttpError(view, request, errorResponse)
        options.reportHttpError(
            isForMainFrame = request?.isForMainFrame == true,
            statusCode = errorResponse?.statusCode ?: 0,
            reasonPhrase = errorResponse?.reasonPhrase ?: "",
        )
    }
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        // on emulator you can see through some pages (should this be an emulator check? or do we care if emulator is see-through?)
        val color = if (url == "about:blank") android.graphics.Color.TRANSPARENT else android.graphics.Color.WHITE
        view?.setBackgroundColor(color)
        options.reportPageFinished(url)
        options.pageFinishedScriptFor(url)?.let { view?.evaluateJavascript(it, null) }
    }
    /*override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
    }*/
    /*override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }*/
}

/* Attempt to gain more control over keyboard function. Seems like emulator does not shift focus in WebView quite right, but on console it seems fine. Leaving this here in case it needs to be looked at again.
private class CustomWebView(context: Context) : WebView(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection? {
        // this is called each time keyboard connects to the WebView
        //val baseConnection = super.onCreateInputConnection(outAttrs) // the super can apparently return null?
        //val inputConnection = baseConnection?.let { MyConnection(baseConnection, this) } // use InputConnectionWrapper? but doesn't seem to scroll right, maybe base is not creating soon enough?
        val inputConnection = MyConnection(this)
        val overrideIME = false // don't think this is necessary, unless we want to force NEXT ACTION? but if field is multiline you probably want the enter button instead
        if (overrideIME && outAttrs != null) {
            // remove other IME_ACTION_*
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_ACTION_GO.inv()
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_ACTION_SEARCH.inv()
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_ACTION_SEND.inv()
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_ACTION_DONE.inv()
            outAttrs.imeOptions = outAttrs.imeOptions and EditorInfo.IME_ACTION_NONE.inv()
            // add IME_ACTION_NEXT instead
            outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_ACTION_NEXT
            //outAttrs.inputType = EditorInfo.TYPE_NULL
        }
        // forcing focus or scroll here did not seem very effective?
        return inputConnection
    }
    fun keyPress(keycode: Int) {
        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keycode))
        dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keycode))
    }

    private class MyConnection(private val webView: CustomWebView) : BaseInputConnection(webView, false) {
        // BaseInputConnection seems a little different than the one produced by WebView super, but not sure where it comes from.
        // Android source shows it comes from view delegate but InputConnection only lists subclasses of this and the wrapper?
        override fun performEditorAction(action: Int): Boolean {
            when (action) {
                EditorInfo.IME_ACTION_NEXT -> {
                    webView.keyPress(KeyEvent.KEYCODE_TAB) // treat next action as TAB for web forms?
                    //webView.evaluateJavascript("")
                }
            }
            return super.performEditorAction(action) // true
        }
    }
}*/
