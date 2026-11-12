package com.jht.mock.interfaces.models

import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.RegisterData
import com.jht.data.RfidLoginHandler
import com.jht.data.RfidOperatingMode
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldFunction
import com.jht.interfaces.models.LoginModel

// Give typical options to aid in previews
private val MockStandardLogins = listOf(
    LoginData("Xid", "", LoginType.Xid, null, listOf(
        UserFieldDigits(UserFieldFunction.LoginId, "XID", "Enter XID", 4, 14),
        UserFieldDigits(UserFieldFunction.Password, "Passcode", "Enter Passcode", 1 ,4),
    )),
    LoginData("Guest", "", LoginType.Guest)
)

class MockLoginModel(
    private val mockLoginOptions: List<LoginData> = MockStandardLogins,
) : LoginModel() {
    override fun getLoginOptions(): List<LoginData> {
        return mockLoginOptions
    }

    override fun attemptLogin(data: LoginData?, onResult: (Int) -> Unit) {
        //do nothing
        onResult(0)
    }

    override fun getRegisterOptions(): List<RegisterData> {
        return listOf()
    }

    override fun attemptRegister(data: RegisterData, onResult: (Int) -> Unit) {
        //do nothing
        onResult(0)
    }

    override fun rfidStartSession(handler: RfidLoginHandler) {
        //do nothing
    }

    override fun rfidStopSession() {
        //do nothing
    }

    override fun rfidSetMode(rfidOperatingMode: RfidOperatingMode) {
        //do nothing
    }

    override fun logout() {
        //do nothing
    }

    override fun close() {
        //do nothing
    }
}