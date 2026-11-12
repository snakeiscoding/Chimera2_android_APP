package com.jht.chimera.service.doore

import android.util.Log
import com.jht.chimera.lib.common.http.IHttpClient
import com.jht.chimera.lib.common.http.SimpleHttpClient
import org.json.JSONArray

/**
 * Class for DOORE API calls. Please reference the linked Requirements page.
 * @see <a href="https://jhtnaee.atlassian
 * .net/wiki/spaces/CSR/pages/4532305963/DOORE+-+In+Progress">DOORE Requirements</a>
 * @param version   desired SW version
 * @param url   manifest for desired version
 * @param softwareType  name of platform
 */
class DooreApi {
    private var httpClient: IHttpClient = SimpleHttpClient()

    /**
     * Determines URL that should be used to retrieve software versions from the API call.
     *
     * @param softwareType  flavor name
     * @return URL string
     */
    fun softwareTypeUrl(softwareType: String): String {
        var baseInvokeUrl = "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/"
        baseInvokeUrl = when (softwareType) {
            "parker" -> "${baseInvokeUrl}parker"
            "chimera" -> "${baseInvokeUrl}chimera"
            "lcgui" -> "${baseInvokeUrl}lowcostgui"
            "projectone" -> "${baseInvokeUrl}projectone"
            else -> "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/"
        }
        return baseInvokeUrl
    }

    /**
     * Call to get the list of currently available builds on AWS based on softwareType.
     *
     * @param softwareType  parker, chimera, lowcostgui, and projectone are supported at this time
     * @return  list of available SW versions on DOORE
     */
    fun retrieveSoftwareOptions(softwareType: String): List<String> {
        val response = httpClient.get(
            url = softwareTypeUrl(softwareType),
            timeout = 10000
        )
        if (response.statusCode == 200 || response.statusCode == 201) {
            val nightlyReleaseJson: String = response.body.trimIndent()
            val nightlyReleaseJsonArray = JSONArray(nightlyReleaseJson)
            val nightlyReleasesList = mutableListOf<String>()
            for (i in 0 until nightlyReleaseJsonArray.length()) {
                nightlyReleasesList.add(nightlyReleaseJsonArray.getString(i))
            }
            return nightlyReleasesList
        } else {
            throw DooreApiException("Retrieving $softwareType options from DOORE failed: ${response.statusCode}")
        }
    }

    class DooreApiException(message: String): Exception (message)
}