package com.jht.chimera.lib.common.http

class MockHttpClient: IHttpClient {
    private var callbacks = mutableMapOf<Pair<HttpMethod, String>, (MockRequest) -> MockResponse>()

    private fun responseFor(method: HttpMethod, url: String, mockRequest: MockRequest): MockResponse =
        callbacks[Pair(method, url)]?.invoke(mockRequest) ?: throw IllegalStateException("No callback set for method $method at url $url")

    fun on(method: HttpMethod, url: String, callback: (MockRequest) -> MockResponse) {
        callbacks[Pair(method, url)] = callback
    }

    override fun get(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.GET, url, MockRequest(url, body, headers, timeout))

    override fun post(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.POST, url, MockRequest(url, body, headers, timeout))

    override fun patch(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.PATCH, url, MockRequest(url, body, headers, timeout))

    override fun put(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.PUT, url, MockRequest(url, body, headers, timeout))

    override fun delete(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.DELETE, url, MockRequest(url, body, headers, timeout))

    override fun head(
        url: String,
        body: String?,
        headers: Map<String, List<String>>,
        timeout: Int
    ): MockResponse = responseFor(HttpMethod.HEAD, url, MockRequest(url, body, headers, timeout))

    data class MockRequest(
        val url: String,
        val body: String?,
        val headers: Map<String, List<String>>,
        val timeout: Int
    )

    data class MockResponse(
        override val statusCode: Int,
        override val headers: Map<String, List<String>>,
        override val body: String
    ) : IHttpResponse

    enum class HttpMethod {
        GET,
        POST,
        PATCH,
        PUT,
        DELETE,
        HEAD
    }
}

