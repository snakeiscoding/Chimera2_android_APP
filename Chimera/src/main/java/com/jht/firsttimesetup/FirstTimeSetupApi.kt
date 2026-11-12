package com.jht.firsttimesetup

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path

/**
 * API client for First time setup related calls
 * @param [client] -> http client
 */
class FirstTimeSetupApi(
    private val client: FtsuClient
) {

    /**
     * Search for facility for first time setup
     * @param [facilityID] -> facility to look for
     */
    suspend fun searchFacilityId(facilityID: String): FacilitySearchResponse? {
        client.use {
            return client.getClient().get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = client.getHost()
                    path("console/facility/$facilityID")
                    headers {
                        append(HttpHeaders.Accept, "application/json")
                    }
                }
            }.body()
        }
    }
}