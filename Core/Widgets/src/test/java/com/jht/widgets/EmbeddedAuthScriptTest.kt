package com.jht.widgets

import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedAuthScriptTest {

    private val script = buildEmbeddedAuthScript(
        accessToken = "acc\"ess",
        idToken = "id\\token",
    )

    @Test
    fun setsEmbeddedAuthWithJsonQuotedTokens() {
        assertTrue(script.contains("window.__EMBEDDED_AUTH__"))
        assertTrue(script.contains("accessToken: \"acc\\\"ess\""))
        assertTrue(script.contains("idToken: \"id\\\\token\""))
    }

    @Test
    fun dispatchesReadyEventAfterSettingAuth() {
        val auth = script.indexOf("window.__EMBEDDED_AUTH__")
        val ready = script.indexOf("dispatchEvent(new CustomEvent('embedded-auth-ready'))")
        assertTrue(auth >= 0)
        assertTrue(ready > auth)
    }

    @Test
    fun emptyTokensStayQuoted() {
        val plain = buildEmbeddedAuthScript(accessToken = "", idToken = "")
        assertTrue(plain.contains("accessToken: \"\""))
        assertTrue(plain.contains("idToken: \"\""))
    }
}
