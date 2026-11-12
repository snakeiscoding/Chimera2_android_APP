package com.jht.chimera.lib.common.http

import android.util.Log
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * A simple HTTP client to reduce boilerplate and common pitfalls of HttpUrlConnection
 */
class SimpleHttpClient: IHttpClient {

    /**
     * Sends a GET request
     * @param url the url to send the request to
     * @param body the body of the request - typically should not be used in a GET request
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun get(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("GET", url, headers, body, timeout) }

    /**
     * Sends a POST request
     * @param url the url to send the request to
     * @param body the body of the request
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun post(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("POST", url, headers, body, timeout) }

    /**
     * Mimics a PATCH request - HttpUrlConnection does not support actual PATCH requests. The server
     * must support X-HTTP-Method-Override for this to work!
     * @param url the url to send the request to
     * @param body the body of the request
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun patch(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("PATCH", url, headers, body, timeout) }

    /**
     * Sends a PUT request
     * @param url the url to send the request to
     * @param body the body of the request
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun put(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("PUT", url, headers, body, timeout) }

    /**
     * Sends a DELETE request
     * @param url the url to send the request to
     * @param body the body of the request
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun delete(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("DELETE", url, headers, body, timeout) }

    /**
     * Sends a HEAD request
     * @param url the url to send the request to
     * @param body the body of the request (typically null for HEAD requests)
     * @param headers the headers attached to the request <header name, header values>
     * @param timeout the amount of time to wait for a connection and data to be read
     * @return [SimpleHttpResponse]
     */
    override fun head(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): SimpleHttpResponse { return sendRequest("HEAD", url, headers, body, timeout) }

    private fun sendRequest(
        method: String,
        path: String,
        headers: Map<String, List<String>> = mapOf(),
        body: String? = null,
        timeout: Int = 10000
    ): SimpleHttpResponse {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(path)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
            headers.forEach { (key: String, values: List<String>) -> connection.setRequestProperty(key, values.joinToString(",")) }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }

            val statusCode = connection.responseCode
            val responseHeaders = connection.headerFields
            val inputStream = if (statusCode in 200..399) connection.inputStream else connection.errorStream
            val response = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            connection.disconnect()
            return SimpleHttpResponse(statusCode, responseHeaders, response)
        } catch (ex: Exception) {
            Log.w("SimpleHttpClient", ex.toString())
            throw ex
        } finally {
            connection?.disconnect()
        }
    }
}

data class SimpleHttpResponse(
    override val statusCode: Int,
    override val headers: Map<String, List<String>>,
    override val body: String
) : IHttpResponse