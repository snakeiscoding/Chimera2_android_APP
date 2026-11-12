package com.jht.chimera.lib.core.documents.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Authentication request payload for CoreDocumentsApi.
 *
 * @property secret The API secret used for authentication.
 */
@Serializable
data class AuthRequest(
    @SerialName("cmapi_secret")
    val secret: String
)

/**
 * Authentication response for CoreDocumentsApi.
 *
 * @property token The TOTP token returned by the API.
 */
@Serializable
data class AuthResponse(
    @SerialName("token")
    val token: String,
)