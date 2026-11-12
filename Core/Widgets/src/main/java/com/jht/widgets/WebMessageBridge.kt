package com.jht.widgets

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.WebViewCompat

/**
 * A JS object at `window.[jsName]` with `postMessage(string)` that only pages on [allowedOrigins] can use
 *
 * [WebViewWidget] registers it with [WebViewCompat.addWebMessageListener] when the WebView supports it, so other
 * origins never see the object. Older WebViews fall back to [WebView.addJavascriptInterface] under the same name,
 * which every page can see, so messages from a page outside [allowedOrigins] are dropped. Empty [allowedOrigins]
 * registers nothing.
 *
 * @param allowedOrigins origins like `https://survey.example.com`
 * @param onMessage called on the main thread with the posted string
 * @throws IllegalArgumentException if an origin has no scheme or host
 */
class WebMessageBridge(
    val jsName: String,
    val allowedOrigins: Set<String>,
    val onMessage: (message: String?) -> Unit,
) {
    init {
        requireOrigins(allowedOrigins)
    }

    internal fun allows(url: String?): Boolean = originAllowed(url = url, allowedOrigins = allowedOrigins)
}

/** How a [WebMessageBridge] ended up registered; logged so field checks can tell which path ran */
internal enum class WebMessageBridgePath { WebMessageListener, JavascriptInterface, None }

/**
 * Registers [bridge] through the listener when [listenerSupported], else as a JS interface
 *
 * @param addListener adds the web message listener limited to the given origin rules
 * @param currentUrl the WebView's current URL, read on the main thread by the fallback
 */
internal fun registerWebMessageBridge(
    bridge: WebMessageBridge,
    listenerSupported: Boolean,
    addListener: (originRules: Set<String>) -> Unit,
    addInterface: (obj: Any, jsName: String) -> Unit,
    currentUrl: () -> String?,
): WebMessageBridgePath {
    val rules = bridge.allowedOrigins.mapNotNull(::originRuleOf).toSet()
    return when {
        rules.isEmpty() -> WebMessageBridgePath.None
        listenerSupported -> {
            addListener(rules)
            WebMessageBridgePath.WebMessageListener
        }
        else -> {
            addInterface(JavascriptInterfaceBridge(bridge = bridge, currentUrl = currentUrl), bridge.jsName)
            WebMessageBridgePath.JavascriptInterface
        }
    }
}

/**
 * `scheme://host[:port]` rule for [WebViewCompat.addWebMessageListener], or null when [url] has no scheme or host
 *
 * The default port is dropped since the rule format only allows a port that differs from it.
 */
internal fun originRuleOf(url: String?): String? {
    val origin = originOf(url) ?: return null
    val defaultPort = when {
        origin.startsWith("https://") -> ":443"
        origin.startsWith("http://") -> ":80"
        else -> ""
    }
    return origin.removeSuffix(defaultPort)
}

/** Fallback for WebViews without the listener; WebView calls [postMessage] on a binder thread */
internal class JavascriptInterfaceBridge(
    private val bridge: WebMessageBridge,
    private val currentUrl: () -> String?,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Called by the page with a string */
    @JavascriptInterface
    fun postMessage(message: String?) {
        mainHandler.post { if (bridge.allows(currentUrl())) bridge.onMessage(message) }
    }
}
