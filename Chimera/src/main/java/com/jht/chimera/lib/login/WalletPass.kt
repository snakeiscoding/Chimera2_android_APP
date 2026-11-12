package com.jht.chimera.lib.login

import android.os.Parcelable
import com.jht.rfidlib.WalletType
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class WalletPass(
    @JvmField val pass: String,
    @JvmField val passType: WalletType,
    @JvmField val issuerId: String,
) : Parcelable

object WalletPassCreator {

    const val KEY_XID = "xid"
    const val KEY_PASSCODE = "passcode"
    const val KEY_RFID = "rfid"
    const val KEY_TYPE = "uid"
    const val KEY_CUSTOM = "custom"
    const val KEY_PASSWORD = "pass"
    const val KEY_BARCODE = "barcode"
    const val KEY_PHONE = "phone"
    const val KEY_EMAIL = "u"
    const val KEY_EMAIL_PASSWORD = "p"

    fun generateXidPass(xid: String, passcode: String): String {
        return JSONObject().apply {
            put(KEY_XID, xid)
            put(KEY_PASSCODE, passcode)
        }.toString()
    }

    fun generateRfidPass(tagUid: String, rfStandard: String): String {
        return JSONObject().apply {
            put(KEY_RFID, tagUid)
            put(KEY_TYPE, rfStandard)
        }.toString()
    }

    fun generateCustomPass(xid: String, password: String): String {
        return JSONObject().apply {
            put(KEY_CUSTOM, xid)
            put(KEY_PASSWORD, password)
        }.toString()
    }

    fun generateBarcodePass(xid: String): String {
        return JSONObject().apply {
            put(KEY_BARCODE, xid)
        }.toString()
    }

    fun generatePhonePass(xid: String, password: String): String {
        return JSONObject().apply {
            put(KEY_PHONE, xid)
            put(KEY_PASSWORD, password)
        }.toString()
    }

    fun generateEmailPass(email: String, password: String): String {
        return JSONObject().apply {
            put(KEY_EMAIL, email)
            put(KEY_EMAIL_PASSWORD, password)
        }.toString()
    }
}