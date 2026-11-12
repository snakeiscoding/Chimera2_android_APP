package com.jht.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Response from /oauth/device-authorization per the JUMP spec. */
@Parcelize
data class QrAuthorizationResponse(
    /** Opaque code used by the console when polling the token endpoint. */
    val deviceCode: String,
    /** Short code shown to the user as a fallback when QR scanning isn't available. */
    val userCode: String,
    /** Base URI where the user completes login on a secondary device. */
    val verificationUri: String,
    /** Full URI including the user code; encoded into the displayed QR. */
    val verificationUriComplete: String,
    /** Device code lifetime, in seconds. JUMP defaults to 180 for console flows. */
    val expiresIn: Int,
    /** Minimum polling interval, in seconds. */
    val interval: Int,
) : Parcelable
