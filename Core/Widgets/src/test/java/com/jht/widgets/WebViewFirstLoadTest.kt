package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class WebViewFirstLoadTest {
    private val failures = mutableListOf<Pair<Int, String>>()
    private val options = WebViewWidgetOptions(
        onFirstLoadFailed = { code, description -> failures += code to description },
    )

    @Test
    fun `main frame network error before any page finishes is a first load failure`() {
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        assertEquals(listOf(-2 to "host lookup"), failures)
    }

    @Test
    fun `first load failure fires once`() {
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        options.reportNetworkError(isForMainFrame = true, errorCode = -6, description = "connect")
        assertEquals(listOf(-2 to "host lookup"), failures)
    }

    @Test
    fun `network error after the first page finished is not a first load failure`() {
        options.reportPageFinished(url = "https://survey.example.com")
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        assertEquals(emptyList<Pair<Int, String>>(), failures)
    }

    @Test
    fun `about blank finishing does not count as the first page`() {
        options.reportPageFinished(url = "about:blank")
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        assertEquals(listOf(-2 to "host lookup"), failures)
    }

    @Test
    fun `subresource network error is not a first load failure`() {
        options.reportNetworkError(isForMainFrame = false, errorCode = -6, description = "connect")
        assertEquals(emptyList<Pair<Int, String>>(), failures)
    }

    @Test
    fun `http status errors are never a first load failure`() {
        // a Cloudflare challenge answers 403 or 503 then reloads itself
        options.reportHttpError(isForMainFrame = true, statusCode = 403, reasonPhrase = "Forbidden")
        options.reportHttpError(isForMainFrame = true, statusCode = 503, reasonPhrase = "Unavailable")
        options.reportHttpError(isForMainFrame = true, statusCode = 429, reasonPhrase = "Too Many Requests")
        assertEquals(emptyList<Pair<Int, String>>(), failures)
    }

    @Test
    fun `webview creation failure is reported and yields no view`() {
        val boom = IllegalStateException("no webview provider")
        var reported: Throwable? = null
        val handled = WebViewWidgetOptions(onCreateFailed = { reported = it })
        assertNull(handled.createWebView { throw boom })
        assertSame(boom, reported)
    }

    @Test
    fun `webview creation failure still throws by default`() {
        assertThrows(IllegalStateException::class.java) {
            WebViewWidgetOptions().createWebView<Any> { throw IllegalStateException("no webview provider") }
        }
    }

    @Test
    fun `successful webview creation returns the view`() {
        val view = Any()
        assertSame(view, WebViewWidgetOptions(onCreateFailed = {}).createWebView { view })
    }

    @Test
    fun `permissions are auto granted by default`() {
        val defaults = WebViewWidgetOptions()
        assertEquals(true, defaults.allowsGeolocation())
        assertEquals(listOf(PROTECTED_MEDIA_ID), defaults.permissionsToGrant().toList())
    }

    @Test
    fun `auto grants can be turned off`() {
        val locked = WebViewWidgetOptions(autoGrantPermissions = false)
        assertEquals(false, locked.allowsGeolocation())
        assertEquals(emptyList<String>(), locked.permissionsToGrant().toList())
    }

    private companion object {
        // PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        const val PROTECTED_MEDIA_ID = "android.webkit.resource.PROTECTED_MEDIA_ID"
    }
}
