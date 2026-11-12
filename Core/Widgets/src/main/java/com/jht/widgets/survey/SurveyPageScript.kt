package com.jht.widgets.survey

import com.jht.widgets.WebMessageBridge
import com.jht.widgets.WebViewWidgetOptions
import com.jht.widgets.buildEmbeddedAuthScript
import com.jht.widgets.originOf

/**
 * [WebViewWidgetOptions] for the ML post workout survey page
 *
 * Zoom is off, and the page script and the Apollo bridge only run on the survey's own origin, so a
 * redirect never gets the JUMP tokens or the bridge.
 *
 * @param url the full survey URL, see [MlPostWorkoutSurvey.buildUrl]
 * @param accessToken read on every page load so a reload picks up refreshed tokens
 * @param idToken read on every page load, like [accessToken]
 * @param onMessage called on the main thread for every bridge message, already parsed
 * @param onLoadFailed called for a network error before the first page loads, or an HTTP status >= 400
 *   on the main frame at any point
 */
fun mlSurveyWebViewOptions(
    url: String,
    accessToken: () -> String,
    idToken: () -> String,
    onMessage: (SurveyBridgeMessage) -> Unit,
    onLoadFailed: (errorCode: Int, errorDescription: String) -> Unit,
    onCreateFailed: (error: Throwable) -> Unit,
): WebViewWidgetOptions {
    val origins = surveyAllowedOrigins(url)
    return WebViewWidgetOptions(
        pageFinishedScript = { buildSurveyPageScript(accessToken = accessToken(), idToken = idToken()) },
        zoomEnabled = false,
        allowedOrigins = origins,
        onFirstLoadFailed = onLoadFailed,
        onMainFrameHttpError = onLoadFailed,
        onCreateFailed = onCreateFailed,
        autoGrantPermissions = false,
        webMessageBridges = listOf(
            WebMessageBridge(jsName = SURVEY_BRIDGE_NAME, allowedOrigins = origins) { onMessage(SurveyBridgeMessage.parse(it)) },
        ),
    )
}

/**
 * Builds the JS run after each survey page load: viewport lock, Apollo bridge shim, then JUMP auth
 *
 * The shim goes before the auth script so `window.ApolloSurveyBridge` exists when the page
 * handles `embedded-auth-ready`.
 */
internal fun buildSurveyPageScript(accessToken: String, idToken: String): String =
    VIEWPORT_LOCK_SCRIPT + APOLLO_BRIDGE_SHIM + buildEmbeddedAuthScript(accessToken = accessToken, idToken = idToken)

/** Origins the survey page script and bridge may run on; empty for a URL with no origin, so nothing runs */
internal fun surveyAllowedOrigins(url: String): Set<String> = setOfNotNull(originOf(url))

/** JS name of the native survey bridge, the same for the web message listener and the JS interface fallback */
internal const val SURVEY_BRIDGE_NAME = "ChimeraSurveyNative"

/**
 * Wraps the native bridge as `window.ApolloSurveyBridge`, stringifying objects since both bridge paths take strings
 *
 * Skips the wrap when the native object is missing so the page sees no bridge rather than a script error.
 */
internal const val APOLLO_BRIDGE_SHIM =
    "(function() {\n" +
        "  var native = window.$SURVEY_BRIDGE_NAME;\n" +
        "  if (!native) return;\n" +
        "  window.ApolloSurveyBridge = {\n" +
        "    postMessage: function(m) { native.postMessage(typeof m === 'string' ? m : JSON.stringify(m)); }\n" +
        "  };\n" +
        "})();"

/**
 * JS that pins the page viewport to scale 1 with user scaling off
 *
 * Backup for the touch filter in [com.jht.widgets.WebViewWidget]; creates the viewport meta if the page has none.
 */
internal const val VIEWPORT_LOCK_SCRIPT =
    "(function() {\n" +
        "  var meta = document.querySelector('meta[name=viewport]');\n" +
        "  if (!meta) {\n" +
        "    meta = document.createElement('meta');\n" +
        "    meta.name = 'viewport';\n" +
        "    document.head.appendChild(meta);\n" +
        "  }\n" +
        "  meta.content = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no';\n" +
        "})();"
