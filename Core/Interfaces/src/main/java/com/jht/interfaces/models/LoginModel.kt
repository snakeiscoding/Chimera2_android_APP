package com.jht.interfaces.models

import android.graphics.Bitmap
import com.jht.data.LoginData
import com.jht.data.QrLoginHandler
import com.jht.data.RegisterData
import com.jht.data.RfidLoginHandler
import com.jht.data.RfidOperatingMode
import com.jht.interfaces.BaseModel

/**
 * Provides a model for login data and operations
 */
abstract class LoginModel : BaseModel {
    /** Signals back to the provider that a guest has been selected by the consumer */
    fun guestSelected() { attemptLogin(null) {} }

    /** Retrieve a list of available login options (default method is presumed to be first in the list) */
    abstract fun getLoginOptions() : List<LoginData>

    /** Commands the system to attempt logging in with provided data. NULL data will be treated as a guest user. A result of zero is considered success */
    abstract fun attemptLogin(data: LoginData?, onResult: (Int) -> Unit) // callback currently receives DapiResult, do we need a generic wrapper ?

    /** Retrieve a list of available registration options */
    abstract fun getRegisterOptions() : List<RegisterData>

    /** Commands the system to attempt register user with provided data. A result of zero is considered success */
    abstract fun attemptRegister(data: RegisterData, onResult: (Int) -> Unit) // callback currently receives DapiResult, do we need a generic wrapper ?

    /** Start an RFID session with the given handler. There can be only one session at a time. New session will replace any previous. */
    abstract fun rfidStartSession(handler: RfidLoginHandler)

    /** Stops the last RFID session */
    abstract fun rfidStopSession()

    /** Set operating mode for RFID session */
    abstract fun rfidSetMode(rfidOperatingMode: RfidOperatingMode)

    /** Log user out */
    abstract fun logout()

    /** Generate Wallet QR code */
    open suspend fun generateWalletQR(passData: String, username: String) : Bitmap? { return null }

    /** Start a QR session with a handler for callbacks. */
    open fun qrStartSession(handler: QrLoginHandler) {}

    /** Stop the active QR-login session, cancelling any in-flight calls or polling. */
    open fun qrStopSession() {}
}
