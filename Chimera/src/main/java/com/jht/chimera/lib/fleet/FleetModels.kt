package com.jht.chimera.lib.fleet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EciesEnvelope(
    @SerialName("ephemeralPublicKey") val ephemeralPublicKey: String,
    @SerialName("ciphertext") val ciphertext: String,
    @SerialName("authTag") val authTag: String,
    // Apple wallet config
    @SerialName("passTypeId") val passTypeId: String? = null,
    // Google wallet config
    @SerialName("collectorId") val collectorId: Long? = null,
    @SerialName("keyVersion") val keyVersion: Int? = null,
    @SerialName("passTypeFilter") val passTypeFilter: Int? = null
)

@Serializable
data class KeyListResponse(
    @SerialName("partnerId") val partnerId: String,
    @SerialName("keys") val keys: List<String>
)

@Serializable
data class FleetApiError(
    @SerialName("error") val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String
)

@Serializable
data class RegisterResponse(
    @SerialName("clientId") val clientId: String,
    @SerialName("clientSecret") val clientSecret: String,
    @SerialName("scope") val scope: String
)

data class FleetDeviceIdentity(
    val deployment: Int,
    val thingName: String,
    val certificatePem: String,
    val privateKeyPem: String,
    val oauthClientId: String,
    val oauthClientSecret: String,
    val oauthScope: String,
    val sgmPublicKey: String
)

enum class WalletSlot {
    APPLE,
    GOOGLE
}

@Serializable
data class WalletKeysResponse(
    @SerialName("apple") val apple: EciesEnvelope? = null,
    @SerialName("google") val google: EciesEnvelope? = null
)
