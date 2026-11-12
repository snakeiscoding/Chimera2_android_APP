package com.jht.chimera.lib.tv.tuner.lg.lg750e

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Client for RMS (Remote Manager Service), an HTTP API exposed by the LG750E tuner
 * on port 10000, outside of the tuner's web app.
 *
 * Currently only factory reset is implemented, but RMS exposes a broader set of
 * commands (see docs below) - additional RMS operations can be added to this class
 * as needed using rmsGet(). Some operations, such as factory reset, require
 * enableSecureConnection() first.
 *
 * RMS docs: http://<TUNER_IP>:10000/doc/
 *
 * Requires the console's IP to already be registered as the tuner's trusted RMS IP
 * via the tuner web app - RMS rejects requests otherwise.
 */
class Lg750ERmsClient(
    private val httpClient: HttpClient = HttpClient(Android) {
        install(HttpTimeout)
    }
) : AutoCloseable {
    companion object {
        private const val TAG = "LG750E-RMS"

        private const val RMS_PORT = 10000
        private const val RMS_BASE_PATH = "/rms"

        // The tuner's IP on the Vortex network. The app statically assigns this IP to
        // the tuner during network setup, so it should always be reachable - the subnet
        // scan fallback exists only as a safety net in case that assignment was ever
        // missed or changed outside the normal flow.
        private const val DEFAULT_TUNER_IP = "192.168.3.2"
        private const val CONSOLE_SUBNET_PREFIX = "192.168.3."
        private const val SUBNET_SCAN_START = 2
        private const val SUBNET_SCAN_END = 254
        private const val SUBNET_SCAN_THREADS = 32

        private const val DISCOVERY_TIMEOUT_MS = 1000L
        private const val HTTP_TIMEOUT_MS = 5000L
    }

    // caps subnet scan concurrency at SUBNET_SCAN_THREADS
    private val subnetScanDispatcher = Dispatchers.IO.limitedParallelism(SUBNET_SCAN_THREADS)

    // log failures since factoryReset() is fire-and-forget with no listener required
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "factoryReset: unhandled exception", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    /**
     * Status/result callback. onStatus may be called multiple times as the reset
     * progresses; onComplete is called exactly once at the end. Both are invoked on
     * a background thread - callers must post to the UI thread themselves if needed.
     */
    interface ResetListener {
        fun onStatus(message: String)
        fun onComplete(success: Boolean, errorMessage: String?)
    }

    /**
     * Kicks off a factory reset asynchronously. Safe to call from the UI thread;
     * all network work happens on a background thread.
     *
     * @param listener optional callback for status/result; may be null to fire-and-forget
     */
    fun factoryReset(listener: ResetListener? = null) {
        scope.launch { factoryResetSuspending(listener) }
    }

    private suspend fun factoryResetSuspending(listener: ResetListener?) {
        listener?.onStatus("Locating tuner...")
        val tunerIp = resolveTunerIp()
        if (tunerIp == null) {
            Log.e(TAG, "factoryReset: could not locate tuner on network")
            listener?.onComplete(false, "Could not locate tuner on network")
            return
        }
        Log.d(TAG, "factoryReset: using tuner IP $tunerIp")

        listener?.onStatus("Enabling secure connection...")
        if (!enableSecureConnection(tunerIp)) {
            Log.e(TAG, "factoryReset: failed to enable secure connection on $tunerIp")
            listener?.onComplete(false, "Failed to enable secure connection")
            return
        }

        listener?.onStatus("Sending factory reset command...")
        val success = resetTuner(tunerIp)
        if (success) {
            Log.d(TAG, "factoryReset: reset command accepted, tuner rebooting")
        } else {
            Log.e(TAG, "factoryReset: reset command failed")
        }
        listener?.onComplete(success, if (success) null else "Factory reset request failed")
    }

    /**
     * Returns a reachable tuner IP, or null if none found.
     * Tries the known static Vortex IP first, falls back to scanning the subnet
     * in parallel in case the tuner's static IP was never applied or has changed.
     */
    private suspend fun resolveTunerIp(): String? {
        if (isRmsServiceAvailable(DEFAULT_TUNER_IP)) return DEFAULT_TUNER_IP

        Log.d(TAG, "resolveTunerIp: RMS unavailable at $DEFAULT_TUNER_IP, scanning subnet")
        return scanSubnetForTuner()
    }

    /**
     * Scans the console's local subnet in parallel for a host running RMS.
     * Returns the first matching IP in scan order, or null if none respond.
     */
    private suspend fun scanSubnetForTuner(): String? = coroutineScope {
        val deferredResults = (SUBNET_SCAN_START..SUBNET_SCAN_END)
            .map { "$CONSOLE_SUBNET_PREFIX$it" }
            .filter { it != DEFAULT_TUNER_IP } // already tried
            .map { candidate ->
                async(subnetScanDispatcher) {
                    if (isRmsServiceAvailable(candidate)) candidate else null
                }
            }

        // Return the first matching IP in scan order.
        val result = deferredResults.firstNotNullOfOrNull { it.await() }
        deferredResults.forEach { it.cancel() } // stop the rest once we have a match
        result
    }

    private suspend fun isRmsServiceAvailable(ip: String): Boolean {
        return try {
            val url = "http://$ip:$RMS_PORT$RMS_BASE_PATH/service/ping"
            val response: HttpResponse = httpClient.get(url) {
                timeout { requestTimeoutMillis = DISCOVERY_TIMEOUT_MS }
            }
            response.status.value == 200
        } catch (e: CancellationException) {
            throw e // don't swallow cancellation from the scan above
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun enableSecureConnection(tunerIp: String): Boolean {
        val url = "http://$tunerIp:$RMS_PORT$RMS_BASE_PATH" +
                "/service/configuration/set/useSecureConnection?value=1"
        return rmsGet(url)
    }

    private suspend fun resetTuner(tunerIp: String): Boolean {
        val url = "http://$tunerIp:$RMS_PORT$RMS_BASE_PATH" +
                "/control/device/reset?exceptNetwork=false&exceptServer=false" +
                "&exceptCountry=false&reboot=true"
        return rmsGet(url)
    }

    /**
     * Sends a GET request and returns true if RMS reports success.
     * 200 = executed. 202 = accepted but not yet executed (expected for reboot=true).
     */
    private suspend fun rmsGet(url: String): Boolean {
        return try {
            val response = httpClient.get(url) {
                timeout { requestTimeoutMillis = HTTP_TIMEOUT_MS }
            }
            val statusCode = response.status.value
            val body = response.bodyAsText()
            Log.d(TAG, "rmsGet: $url -> $statusCode $body")
            statusCode == 200 || statusCode == 202
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "rmsGet failed: $url", e)
            false
        }
    }

    override fun close() {
        scope.cancel()
        httpClient.close()
    }
}