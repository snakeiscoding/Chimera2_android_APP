package com.jht.widgets

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WebMessageBridgeTest {

    private val received = mutableListOf<String?>()
    private val bridge = WebMessageBridge(
        jsName = "ChimeraSurveyNative",
        allowedOrigins = setOf("https://survey.example.com"),
        onMessage = { received += it },
    )

    private class Calls {
        val listenerRules = mutableListOf<Set<String>>()
        val interfaces = mutableListOf<Pair<Any, String>>()
    }

    private fun register(bridge: WebMessageBridge, listenerSupported: Boolean, calls: Calls) =
        registerWebMessageBridge(
            bridge = bridge,
            listenerSupported = listenerSupported,
            addListener = { rules -> calls.listenerRules += rules },
            addInterface = { obj, name -> calls.interfaces += obj to name },
            currentUrl = { "https://survey.example.com/a" },
        )

    @Test
    fun `uses the web message listener limited to the bridge origins when supported`() {
        val calls = Calls()
        assertEquals(WebMessageBridgePath.WebMessageListener, register(bridge, listenerSupported = true, calls = calls))
        assertEquals(listOf(setOf("https://survey.example.com")), calls.listenerRules)
        assertTrue(calls.interfaces.isEmpty())
    }

    @Test
    fun `falls back to a javascript interface under the same name when unsupported`() {
        val calls = Calls()
        assertEquals(WebMessageBridgePath.JavascriptInterface, register(bridge, listenerSupported = false, calls = calls))
        assertTrue(calls.listenerRules.isEmpty())
        assertEquals(listOf("ChimeraSurveyNative"), calls.interfaces.map { it.second })
    }

    @Test
    fun `empty origins register nothing`() {
        val calls = Calls()
        val closed = WebMessageBridge(jsName = "x", allowedOrigins = emptySet(), onMessage = {})
        assertEquals(WebMessageBridgePath.None, register(closed, listenerSupported = true, calls = calls))
        assertEquals(WebMessageBridgePath.None, register(closed, listenerSupported = false, calls = calls))
        assertTrue(calls.listenerRules.isEmpty())
        assertTrue(calls.interfaces.isEmpty())
    }

    @Test
    fun `origin without a scheme or host is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            WebMessageBridge(jsName = "x", allowedOrigins = setOf("survey.example.com"), onMessage = {})
        }
    }

    @Test
    fun `origin rules drop the default port and keep others`() {
        assertEquals("https://survey.example.com", originRuleOf("https://Survey.Example.com:443/path?q=1"))
        assertEquals("http://survey.example.com", originRuleOf("http://survey.example.com:80"))
        assertEquals("https://survey.example.com:8443", originRuleOf("https://survey.example.com:8443"))
    }

    @Test
    fun `fallback delivers on the main looper from the allowed origin`() {
        val fallback = JavascriptInterfaceBridge(bridge = bridge, currentUrl = { "https://survey.example.com/after" })
        // WebView calls the interface on a binder thread
        val worker = Thread { fallback.postMessage("hi") }
        worker.start()
        worker.join()
        assertTrue(received.isEmpty())

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf<String?>("hi"), received)
    }

    @Test
    fun `fallback drops messages from pages outside the allowed origins`() {
        val fallback = JavascriptInterfaceBridge(bridge = bridge, currentUrl = { "https://evil.example.com/survey.example.com" })
        fallback.postMessage("hi")
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(received.isEmpty())
    }
}
