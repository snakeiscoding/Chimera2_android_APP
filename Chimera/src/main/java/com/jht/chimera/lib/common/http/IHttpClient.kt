package com.jht.chimera.lib.common.http

interface IHttpClient {
    fun get(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse

    fun post(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse

    fun patch(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse

    fun put(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse

    fun delete(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse

    fun head(
        url: String,
        body: String? = null,
        headers: Map<String, List<String>> = mapOf(),
        timeout: Int = 10000
    ): IHttpResponse
}

interface IHttpResponse {
    val statusCode: Int
    val headers: Map<String, List<String>>
    val body: String
}