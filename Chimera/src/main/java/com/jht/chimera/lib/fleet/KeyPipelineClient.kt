package com.jht.chimera.lib.fleet

import android.util.Log
import com.jht.chimera.lib.common.http.IHttpClient
import com.jht.chimera.lib.common.http.SimpleHttpClient
import com.jht.oauthlib.microservices.FleetService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KeyPipelineClient(
    private val deployment: Int,
    private val baseUrl: String? = null,
    private val httpClient: IHttpClient = SimpleHttpClient()
) {
    companion object {
        private const val TAG = "KeyPipelineClient"
        private const val TIMEOUT_MS = 30_000
        private val json = Json { ignoreUnknownKeys = true }
    }

    private fun resolveBaseUrl(): String {
        return baseUrl ?: FleetService.getBaseUrl(deployment)
    }

    fun register(
        thingName: String,
        ecdhPublicKey: String,
        attestationSignature: String
    ): Result<RegisterResponse> {
        return try {
            val url = "${resolveBaseUrl()}/v1/register"
            val body = buildJsonObject {
                put("thingName", thingName)
                if (ecdhPublicKey.isNotEmpty()) put("ecdhPublicKey", ecdhPublicKey)
                if (attestationSignature.isNotEmpty()) put("attestationSignature", attestationSignature)
            }.toString()

            val headers = mapOf("Content-Type" to listOf("application/json"))
            val response = httpClient.post(url, body, headers, TIMEOUT_MS)

            if (response.statusCode == 200) {
                Result.success(json.decodeFromString<RegisterResponse>(response.body))
            } else {
                val error = try {
                    json.decodeFromString<FleetApiError>(response.body)
                } catch (_: Exception) { null }
                Result.failure(
                    FleetApiException(
                        error?.error?.code ?: "HTTP_ERROR",
                        error?.error?.message ?: "HTTP ${response.statusCode}",
                        response.statusCode
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register failed", e)
            Result.failure(e)
        }
    }

    fun deliverKey(partnerId: String, keyId: String, bearerToken: String): Result<EciesEnvelope> {
        return try {
            val url = "${resolveBaseUrl()}/v1/keys/$partnerId/$keyId"
            val headers = mapOf(
                "Authorization" to listOf(bearerToken),
                "Content-Type" to listOf("application/json")
            )

            val response = httpClient.post(url, null, headers, TIMEOUT_MS)

            when (response.statusCode) {
                200 -> Result.success(json.decodeFromString<EciesEnvelope>(response.body))
                401 -> Result.failure(FleetApiException("UNAUTHORIZED", "Invalid or expired token", 401))
                403 -> Result.failure(FleetApiException("FORBIDDEN", "Missing required scope", 403))
                409 -> Result.failure(FleetApiException("DEVICE_NOT_READY", "Device ECDH key exchange not complete", 409))
                else -> {
                    val error = try {
                        json.decodeFromString<FleetApiError>(response.body)
                    } catch (_: Exception) { null }
                    Result.failure(
                        FleetApiException(
                            error?.error?.code ?: "UNKNOWN",
                            error?.error?.message ?: "HTTP ${response.statusCode}",
                            response.statusCode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Key delivery failed for $partnerId/$keyId", e)
            Result.failure(e)
        }
    }

    fun deliverWalletKeys(facilityId: Int, bearerToken: String): Result<WalletKeysResponse> {
        return try {
            val url = "${resolveBaseUrl()}/v1/wallet-keys?facilityId=$facilityId"
            val headers = mapOf(
                "Authorization" to listOf(bearerToken)
            )

            val response = httpClient.get(url, null, headers, TIMEOUT_MS)

            when (response.statusCode) {
                200 -> Result.success(json.decodeFromString<WalletKeysResponse>(response.body))
                401 -> Result.failure(FleetApiException("UNAUTHORIZED", "Invalid or expired token", 401))
                403 -> Result.failure(FleetApiException("FORBIDDEN", "Missing required scope", 403))
                409 -> Result.failure(FleetApiException("DEVICE_NOT_READY", "Device ECDH key exchange not complete", 409))
                else -> {
                    val error = try {
                        json.decodeFromString<FleetApiError>(response.body)
                    } catch (_: Exception) { null }
                    Result.failure(
                        FleetApiException(
                            error?.error?.code ?: "UNKNOWN",
                            error?.error?.message ?: "HTTP ${response.statusCode}",
                            response.statusCode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wallet key delivery failed for facilityId=$facilityId", e)
            Result.failure(e)
        }
    }

    fun listKeys(partnerId: String, bearerToken: String): Result<KeyListResponse> {
        return try {
            val url = "${resolveBaseUrl()}/v1/keys/$partnerId"
            val headers = mapOf("Authorization" to listOf(bearerToken))

            val response = httpClient.get(url, null, headers, TIMEOUT_MS)

            if (response.statusCode == 200) {
                Result.success(json.decodeFromString<KeyListResponse>(response.body))
            } else {
                Result.failure(FleetApiException("HTTP_ERROR", "HTTP ${response.statusCode}", response.statusCode))
            }
        } catch (e: Exception) {
            Log.e(TAG, "List keys failed for $partnerId", e)
            Result.failure(e)
        }
    }
}

class FleetApiException(
    val code: String,
    override val message: String,
    val statusCode: Int
) : Exception(message)
