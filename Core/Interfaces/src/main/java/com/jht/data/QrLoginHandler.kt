package com.jht.data

/**
 * Callbacks for a QR-login session driven by [com.jht.interfaces.models.LoginModel.qrStartSession].
 */
interface QrLoginHandler {
    /** QR is ready for display. */
    fun onCodeReady(response: QrAuthorizationResponse)
    /** The device code expired before the user completed the sign in. */
    fun onExpired()
    /** Error during the device-auth call, token polling, or downstream sign-in. */
    fun onError()
    /** Token polling and downstream sign-in both succeeded; the user is now logged in. */
    fun onLoginSuccess()
}
