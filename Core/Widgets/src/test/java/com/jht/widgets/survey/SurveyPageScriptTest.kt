package com.jht.widgets.survey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SurveyPageScriptTest {

    private val script = buildSurveyPageScript(accessToken = "access", idToken = "id")

    private fun options(
        url: String = "https://dev-matrixworkouts.jfit.co/survey/after-exercise?exercise_id=manual%3A1",
        accessToken: () -> String = { "access" },
        onMessage: (SurveyBridgeMessage) -> Unit = {},
        onLoadFailed: (Int, String) -> Unit = { _, _ -> },
        onCreateFailed: (Throwable) -> Unit = {},
    ) = mlSurveyWebViewOptions(
        url = url,
        accessToken = accessToken,
        idToken = { "id" },
        onMessage = onMessage,
        onLoadFailed = onLoadFailed,
        onCreateFailed = onCreateFailed,
    )

    @Test
    fun locksViewportAndInjectsAuth() {
        assertTrue(script.contains("user-scalable=no"))
        assertTrue(script.contains("accessToken: \"access\""))
        assertTrue(script.contains("idToken: \"id\""))
    }

    @Test
    fun assignsBridgeBeforeDispatchingReady() {
        val bridge = script.indexOf("window.ApolloSurveyBridge")
        val ready = script.indexOf("dispatchEvent(new CustomEvent('embedded-auth-ready'))")
        assertTrue(bridge >= 0)
        assertTrue(ready > bridge)
    }

    @Test
    fun shimWrapsNativeBridgeAndStringifiesObjects() {
        assertTrue(script.contains("window.$SURVEY_BRIDGE_NAME"))
        assertTrue(script.contains("typeof m === 'string' ? m : JSON.stringify(m)"))
    }

    @Test
    fun shimDoesNotThrowWhenTheNativeObjectIsMissing() {
        assertTrue(APOLLO_BRIDGE_SHIM.contains("if (!native) return;"))
    }

    @Test
    fun viewportLockCreatesMetaWhenPageHasNone() {
        assertTrue(VIEWPORT_LOCK_SCRIPT.contains("createElement('meta')"))
        assertTrue(VIEWPORT_LOCK_SCRIPT.contains("meta[name=viewport]"))
        assertTrue(VIEWPORT_LOCK_SCRIPT.contains("maximum-scale=1"))
    }

    @Test
    fun allowedOriginsIsTheSurveyPageOrigin() {
        assertEquals(setOf("https://survey.example.com:443"), surveyAllowedOrigins("https://survey.example.com/pace?lang=en"))
    }

    @Test
    fun allowedOriginsEmptyForBadUrlSoNothingRuns() {
        assertTrue(surveyAllowedOrigins("not a url").isEmpty())
        assertNull(options(url = "not a url").pageFinishedScriptFor("not a url"))
    }

    @Test
    fun optionsTurnZoomOffAndLimitScriptAndBridgeToTheSurveyOrigin() {
        val options = options()
        assertFalse(options.zoomEnabled)
        assertFalse(options.runScriptOnAnyOrigin)
        assertFalse(options.autoGrantPermissions)
        assertEquals(setOf("https://dev-matrixworkouts.jfit.co:443"), options.allowedOrigins)
        assertTrue(options.pageFinishedScriptFor("https://dev-matrixworkouts.jfit.co/survey/after-exercise")!!.contains("ApolloSurveyBridge"))
        assertNull(options.pageFinishedScriptFor("https://evil.example.com/"))
        val bridge = options.webMessageBridges.single()
        assertEquals(SURVEY_BRIDGE_NAME, bridge.jsName)
        assertEquals(options.allowedOrigins, bridge.allowedOrigins)
    }

    @Test
    fun tokensAreReadOnEveryLoad() {
        var token = "a"
        val options = options(accessToken = { token })
        val url = "https://dev-matrixworkouts.jfit.co/survey"
        assertTrue(options.pageFinishedScriptFor(url)!!.contains("accessToken: \"a\""))
        token = "b"
        assertTrue(options.pageFinishedScriptFor(url)!!.contains("accessToken: \"b\""))
    }

    @Test
    fun bridgeParsesMessagesBeforeHandingThemOn() {
        val received = mutableListOf<SurveyBridgeMessage>()
        options(onMessage = { received += it }).webMessageBridges.single().onMessage("""{"type":"survey-complete"}""")
        assertEquals(listOf<SurveyBridgeMessage>(SurveyBridgeMessage.Complete), received)
    }

    @Test
    fun firstLoadNetworkFailureIsReported() {
        val failures = mutableListOf<Int>()
        val options = options(onLoadFailed = { code, _ -> failures += code })
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "net::ERR_NAME_NOT_RESOLVED")
        assertEquals(listOf(-2), failures)
    }

    @Test
    fun networkFailureAfterThePageLoadedIsNotReported() {
        val failures = mutableListOf<Int>()
        val options = options(onLoadFailed = { code, _ -> failures += code })
        options.reportPageFinished("https://dev-matrixworkouts.jfit.co/survey")
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "net::ERR_NAME_NOT_RESOLVED")
        assertEquals(emptyList<Int>(), failures)
    }

    @Test
    fun mainFrameHttpErrorIsReported() {
        val failures = mutableListOf<Int>()
        val options = options(onLoadFailed = { code, _ -> failures += code })
        options.reportPageFinished("https://dev-matrixworkouts.jfit.co/survey")
        options.reportHttpError(isForMainFrame = true, statusCode = 400, reasonPhrase = "Bad Request")
        options.reportHttpError(isForMainFrame = true, statusCode = 503, reasonPhrase = "Service Unavailable")
        assertEquals(listOf(400, 503), failures)
    }

    @Test
    fun httpBelow400AndSubresourceErrorsAreNotReported() {
        val failures = mutableListOf<Int>()
        val options = options(onLoadFailed = { code, _ -> failures += code })
        options.reportHttpError(isForMainFrame = true, statusCode = 399, reasonPhrase = "")
        options.reportHttpError(isForMainFrame = false, statusCode = 500, reasonPhrase = "Server Error")
        assertEquals(emptyList<Int>(), failures)
    }

    @Test
    fun createFailureIsHandedToTheCaller() {
        val errors = mutableListOf<Throwable>()
        val boom = IllegalStateException("no webview provider")
        assertNull(options(onCreateFailed = { errors += it }).createWebView<Any> { throw boom })
        assertEquals(listOf<Throwable>(boom), errors)
    }
}
