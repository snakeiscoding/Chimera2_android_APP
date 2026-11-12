package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewPageScriptTest {
    private val options = WebViewWidgetOptions(
        pageFinishedScript = { "window.ready = true;" },
        runScriptOnAnyOrigin = true,
    )

    @Test
    fun `script runs on any page when opted in`() {
        assertEquals("window.ready = true;", options.pageFinishedScriptFor("https://survey.example.com"))
        assertEquals("window.ready = true;", options.pageFinishedScriptFor("http://other.example.org/x"))
    }

    @Test
    fun `empty allowed origins run no script`() {
        val closed = WebViewWidgetOptions(pageFinishedScript = { "x" })
        assertNull(closed.pageFinishedScriptFor("https://survey.example.com"))
    }

    @Test
    fun `allowed origin without a scheme or host is rejected`() {
        listOf("survey.example.com", "not a url", "https://", "/survey").forEach { origin ->
            assertThrows(IllegalArgumentException::class.java) {
                WebViewWidgetOptions(pageFinishedScript = { "x" }, allowedOrigins = setOf(origin))
            }
        }
    }

    @Test
    fun `script skips about blank`() {
        assertNull(options.pageFinishedScriptFor("about:blank"))
    }

    @Test
    fun `default options run no script`() {
        assertNull(WebViewWidgetOptions().pageFinishedScriptFor("https://example.com"))
    }

    @Test
    fun `touch listener only installed when interaction or zoom is off`() {
        assertTrue(WebViewWidgetOptions().needsTouchFilter())
        assertFalse(WebViewWidgetOptions(zoomEnabled = true).needsTouchFilter())
        assertTrue(WebViewWidgetOptions(zoomEnabled = true, allowUserInteraction = false).needsTouchFilter())
        assertTrue(WebViewWidgetOptions(zoomEnabled = false).needsTouchFilter())
    }

    @Test
    fun `script runs on an allowed origin`() {
        val gated = WebViewWidgetOptions(pageFinishedScript = { "x" }, allowedOrigins = setOf("https://survey.example.com"))
        assertEquals("x", gated.pageFinishedScriptFor("https://survey.example.com/page?id=1"))
    }

    @Test
    fun `script skips an origin not in the allowlist`() {
        val gated = WebViewWidgetOptions(pageFinishedScript = { "x" }, allowedOrigins = setOf("https://survey.example.com"))
        assertNull(gated.pageFinishedScriptFor("https://evil.example.com/survey.example.com"))
        assertNull(gated.pageFinishedScriptFor("http://survey.example.com"))
        assertNull(gated.pageFinishedScriptFor("https://survey.example.com:8443"))
        assertNull(gated.pageFinishedScriptFor("https://survey.example.com.evil.com"))
    }

    @Test
    fun `script skips unparseable or null urls when gated`() {
        val gated = WebViewWidgetOptions(pageFinishedScript = { "x" }, allowedOrigins = setOf("https://survey.example.com"))
        assertNull(gated.pageFinishedScriptFor(null))
        assertNull(gated.pageFinishedScriptFor("not a url"))
    }

    @Test
    fun `origin match ignores host case and explicit default port`() {
        val gated = WebViewWidgetOptions(pageFinishedScript = { "x" }, allowedOrigins = setOf("https://survey.example.com"))
        assertEquals("x", gated.pageFinishedScriptFor("https://Survey.Example.com:443/a"))
    }

    @Test
    fun `script is read again on every page load`() {
        var token = "a"
        val fresh = WebViewWidgetOptions(pageFinishedScript = { token }, allowedOrigins = setOf("https://survey.example.com"))
        assertEquals("a", fresh.pageFinishedScriptFor("https://survey.example.com"))
        token = "b"
        assertEquals("b", fresh.pageFinishedScriptFor("https://survey.example.com"))
    }
}
