package com.jht.welcome.vortex

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.welcome.R

@Composable
fun getLoginTypeUiData(loginData: LoginData): LoginTypeUiData {
    return when(loginData.type) {
        LoginType.Xid -> LoginTypeUiData(name = stringResource(R.string.xid_tc), type = LoginType.Xid, src = R.drawable.ic_xid_monochrome)
        LoginType.PhoneNumber -> LoginTypeUiData(name = stringResource(R.string.phone_tc), type = LoginType.PhoneNumber, src = R.drawable.ic_phone, tint = Color.White)
        LoginType.Email -> LoginTypeUiData(name = stringResource(R.string.email_tc), type = LoginType.Email, src = R.drawable.ic_mail, tint = Color.White)
        LoginType.QrCode -> LoginTypeUiData(name = stringResource(R.string.qr_code_tc), type = LoginType.QrCode, src = R.drawable.ic_qr_code, tint = Color.White)
        LoginType.Barcode -> LoginTypeUiData(name = stringResource(R.string.barcode_tc), type = LoginType.Barcode, src = R.drawable.ic_qr_code, tint = Color.White)
        LoginType.Wallet -> LoginTypeUiData(name = stringResource(R.string.wallet_tc), type = LoginType.Wallet, src = R.drawable.ic_wallet, tint = Color.White)
        LoginType.Custom -> {
            // do we need to do any length checking for fit?
            val label = loginData.label.ifEmpty { stringResource(R.string.custom_no_ellipes_tc) }
            LoginTypeUiData(name = label, type = LoginType.Custom, src = R.drawable.ic_note_pencil, tint = Color.White)
        }
        else -> LoginTypeUiData(name = stringResource(R.string.login_uc), type = loginData.type, src = R.drawable.ic_login, tint = Color.White)
    }
}

data class LoginTypeUiData(
    val name: String,
    val type: LoginType,
    val src: Int,
    val tint: Color = Color.White
)

/**
 * Returns the primary (default) login, which is the first non-Guest entry in the list.
 * The list is expected to already be ordered with the primary login method first.
 */
fun List<LoginData>.primaryLogin(): LoginData? = firstOrNull { it.type != LoginType.Guest }