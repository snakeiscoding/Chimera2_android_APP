package com.jht.chimera.ui.machinesettings

import android.content.Context
import android.util.Pair
import com.jht.chimera.R
import com.jht.data.LoginType

/** Class helper for MES Asset management page */
class AssetManagementLogins(
    context: Context,
) {
    /** Supported login types for primary login method setting */
    private val possiblePrimaryMethods = arrayOf(
        // need user facing translated labels mapped to internal login types
        Pair(context.getString(R.string.btn_xid_login_tc), LoginType.Xid),
        Pair(context.getString(R.string.barcode_login_tc), LoginType.Barcode),
        Pair(context.getString(R.string.phone_number_tc), LoginType.PhoneNumber),
        Pair(context.getString(R.string.email_login_tc), LoginType.Email),
        Pair(context.getString(R.string.custom_login_tc), LoginType.Custom),
        Pair(context.getString(R.string.wallet_login_tc), LoginType.Wallet),
        Pair(context.getString(R.string.qr_code_tc), LoginType.QrCode),
    )
    fun getLabels(): Array<String> = possiblePrimaryMethods.map { it.first }.toTypedArray()
    fun getIndexFromType(loginTypeValue: String?, defaultIndex: Int = 0): Int = getIndexFromType(
        LoginType.fromValue(loginTypeValue), defaultIndex)
    fun getIndexFromType(loginType: LoginType?, defaultIndex: Int = 0): Int {
        val found = possiblePrimaryMethods.indexOfFirst { it.second == loginType }
        return if (found < 0) defaultIndex else found
    }
    fun getTypeFromIndex(index: Int): LoginType = possiblePrimaryMethods.getOrNull(index)?.second ?: possiblePrimaryMethods.first().second
}