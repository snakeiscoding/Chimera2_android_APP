package com.jht.chimera.service.doore

import android.util.Log
import kotlinx.coroutines.Runnable

/**
 * Holds and runs the logic to make the API call for retrieving the Nightly Build options from AWS.
 * Uses the supplied callback to return said options.
 * Please reference the linked Requirements page.
 * @see <a href="https://jhtnaee.atlassian.net/wiki/spaces/CSR/pages/4532305963/DOORE+-+In+Progress">DOORE Requirements</a>
 */
class NightlyBuildsWorker(
    private val nightlyBuildsApi: DooreApi,
    private val softwareType: String,
    private val callback: (options: List<String>) -> Unit,
): Runnable {
    companion object {
        const val TAG = "NightlyBuildsWorker"
    }

    /** Makes API call, and returns a callback of versions available for the given softwareType.
     * It will catch DooreApiException if an error occurs, and log the result. If another error
     * occurs, it will be caught as a generic Exception. */
    override fun run() {
        try {
            val options = nightlyBuildsApi.retrieveSoftwareOptions(softwareType)
            callback(options)
        } catch (dooreApiException: DooreApi.DooreApiException) {
            Log.e(TAG, "Error from Doore API: $dooreApiException")
        }
    }
}