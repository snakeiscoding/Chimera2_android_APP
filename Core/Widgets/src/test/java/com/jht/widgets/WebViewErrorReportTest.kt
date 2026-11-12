package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewErrorReportTest {
    private val errors = mutableListOf<Pair<Int, String>>()
    private val mainFrameErrors = mutableListOf<Pair<Int, String>>()
    private val options = WebViewWidgetOptions(
        onError = { code, description -> errors += code to description },
        onMainFrameError = { code, description -> mainFrameErrors += code to description },
    )

    @Test
    fun `main frame network error fires both callbacks`() {
        options.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        assertEquals(listOf(-2 to "host lookup"), errors)
        assertEquals(listOf(-2 to "host lookup"), mainFrameErrors)
    }

    @Test
    fun `subresource network error fires only onError`() {
        options.reportNetworkError(isForMainFrame = false, errorCode = -6, description = "connect")
        assertEquals(listOf(-6 to "connect"), errors)
        assertEquals(emptyList<Pair<Int, String>>(), mainFrameErrors)
    }

    @Test
    fun `main frame http status under 400 is not an error`() {
        options.reportHttpError(isForMainFrame = true, statusCode = 399, reasonPhrase = "")
        assertEquals(emptyList<Pair<Int, String>>(), mainFrameErrors)
    }

    @Test
    fun `main frame http status 400 and over fires onMainFrameError`() {
        options.reportHttpError(isForMainFrame = true, statusCode = 400, reasonPhrase = "Bad Request")
        options.reportHttpError(isForMainFrame = true, statusCode = 503, reasonPhrase = "Unavailable")
        assertEquals(listOf(400 to "Bad Request", 503 to "Unavailable"), mainFrameErrors)
    }

    @Test
    fun `http errors leave onError alone so existing callers see no new calls`() {
        options.reportHttpError(isForMainFrame = true, statusCode = 404, reasonPhrase = "Not Found")
        assertEquals(emptyList<Pair<Int, String>>(), errors)
    }

    @Test
    fun `subresource http error is ignored`() {
        options.reportHttpError(isForMainFrame = false, statusCode = 404, reasonPhrase = "Not Found")
        assertEquals(emptyList<Pair<Int, String>>(), mainFrameErrors)
    }

    @Test
    fun `onMainFrameHttpError only sees main frame http status 400 and over`() {
        val httpErrors = mutableListOf<Pair<Int, String>>()
        val httpOptions = WebViewWidgetOptions(onMainFrameHttpError = { code, reason -> httpErrors += code to reason })
        httpOptions.reportNetworkError(isForMainFrame = true, errorCode = -2, description = "host lookup")
        httpOptions.reportHttpError(isForMainFrame = true, statusCode = 399, reasonPhrase = "")
        httpOptions.reportHttpError(isForMainFrame = false, statusCode = 500, reasonPhrase = "Server Error")
        httpOptions.reportHttpError(isForMainFrame = true, statusCode = 404, reasonPhrase = "Not Found")
        assertEquals(listOf(404 to "Not Found"), httpErrors)
    }
}
