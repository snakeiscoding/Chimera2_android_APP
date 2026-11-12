package com.jht.chimera.lib.fleet

import android.content.Context
import android.util.Base64
import android.util.Log
import com.jht.oauthlib.auth.ConsoleTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class FleetProvisioningClient(
    private val context: Context,
    private val deployment: Int,
    private val iotEndpoint: String,
    private val templateName: String,
    private val apiBaseUrl: String? = null
) {
    companion object {
        private const val TAG = "FleetProvisioningClient"
    }

    fun <T> retry(times: Int, delayMs: Long = 0, block: () -> T): T {
        var lastError: Exception? = null

        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Attempt ${attempt + 1} failed", e)
                if (attempt < times - 1 && delayMs > 0)  Thread.sleep(delayMs)
            }
        }

        throw lastError ?: RuntimeException("Retry failed")
    }

    suspend fun provisionDevice(
        bridge: SgmBridgeInterface,
        consoleUuid: String,
        cpuId: String,
        retries: Int = 3,
        delayMs: Long = 1000
    ): Result<FleetDeviceIdentity> {
        var result: Result<FleetDeviceIdentity> = Result.failure(Exception("Provisioning failed"))

        repeat(retries) { attempt ->
            result = provisionDeviceOnce(bridge, consoleUuid, cpuId)
            if (result.isSuccess) return result
            Log.e(TAG, "Provision attempt ${attempt + 1} failed", result.exceptionOrNull())
            if (attempt < retries - 1) delay(delayMs)
        }

        return result
    }

    suspend fun provisionDeviceOnce(
        bridge: SgmBridgeInterface,
        consoleUuid: String,
        cpuId: String
    ): Result<FleetDeviceIdentity> = withContext(Dispatchers.IO) {
        try {
            /**
             * Reference this sequence diagram for details on the flow being implemented here:
             * https://jhtnaee.atlassian.net/wiki/spaces/SA/pages/6266978337/ADR-009+Fleet+Provisioning+for+JHT+Machines#Appendix%3A-Detailed-Cryptography-Sequence-Diagram
             */

            // Phase 2: Fleet Provisioning by Claim (MQTT)
            Log.d(TAG, "Starting fleet provisioning for $consoleUuid:$cpuId")
            val identity = when (val result = IoTProvisioningService(
                endpoint = iotEndpoint,
                claimCertPem = bridge.getCertPem(),
                claimKeyPem = bridge.getKeyPem(),
                templateName = templateName
            ).provision(consoleUuid, cpuId)) {
                is FleetProvisioningResult.Success -> result.identity
                is FleetProvisioningResult.Error -> return@withContext Result.failure(result.cause)
            }

            // Phase 4: Post-Provisioning (Register with JUMP DCR and key staging)
            Log.d(TAG, "Registering device via API")
            val credentials = retry(times = 3, delayMs = 2000) {
                KeyPipelineClient(deployment, baseUrl = apiBaseUrl).register(
                    thingName = identity.thingName,
                    ecdhPublicKey = Base64.encodeToString(bridge.getPublicKey(), Base64.NO_WRAP),
                    attestationSignature = Base64.encodeToString(bridge.getKeySignature(), Base64.NO_WRAP)
                ).getOrThrow()
            }

            Result.success(FleetDeviceIdentity(
                deployment = deployment,
                thingName = identity.thingName,
                certificatePem = identity.certificatePem,
                privateKeyPem = identity.privateKey,
                oauthClientId = credentials.clientId,
                oauthClientSecret = credentials.clientSecret,
                oauthScope = credentials.scope,
                sgmPublicKey = Base64.encodeToString(bridge.getPublicKey(), Base64.NO_WRAP)
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Device provisioning failed", e)
            Result.failure(e)
        }
    }
}
