package com.jht.widgets

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jht.data.PhoneLoginHandler
import com.jht.data.RfidLoginData
import com.jht.data.RfidLoginHandler

/** ViewModel may need to coordinate with widget, so provide handler class that can feed widget from ViewModel */
class RfidLoginWidgetHandler(
    val inProgress: (Boolean) -> Unit = {},
    val loginComplete: () -> Unit = {},
    val needToAssociate: (rfid: RfidLoginData) -> Unit = {},
    val readyForNextScan: () -> Unit = {},
) : RfidLoginHandler() {
    internal val headerId = mutableStateOf(0)
    internal val messageId = mutableStateOf(0)
    internal val allowDismiss = mutableStateOf(false)
    internal val shouldAssociateCallback = mutableStateOf<((Boolean)->Unit)?>(null)

    internal fun dismiss() {
        if (allowDismiss.value) {
            inProgress(false)
            shouldAssociateCallback.value = null
            messageId.value = 0 // close box
            readyForNextScan()
        }
    }

    private fun showInfoDialog(titleText: Int, messageText: Int, closeOnTouch: Boolean = true, confirm: ((Boolean)->Unit)? = null) {
        headerId.value = titleText
        messageId.value = messageText
        allowDismiss.value = closeOnTouch
        shouldAssociateCallback.value = confirm
    }

    override fun onLoginAttempt(isRfid: Boolean) {
        inProgress(true)
        if (isRfid)
            showInfoDialog(R.string.rfid_login_tc, R.string.rid_login_msg_pg, false)
        else
            showInfoDialog(R.string.wallet_login_tc, R.string.broadcasting_pg, false)
    }

    override fun onLoginSuccess() {
        inProgress(false)
        messageId.value = 0 // close box
        loginComplete() // proceed with logged in flow
    }

    override fun onLoginFailure(isRfid: Boolean) {
        if (isRfid)
            showInfoDialog(R.string.rfid_login_tc, R.string.login_failed_message_pg, true)
        else
            showInfoDialog(R.string.wallet_login_tc, R.string.login_failed_message_pg, true)
    }

    override fun onError(isRfid: Boolean) {
        if (isRfid)
            showInfoDialog(R.string.rfid_pairing_failed_tc, R.string.rfid_pairing_failed_msg_pg, true)
        else
            showInfoDialog(R.string.wallet_login_tc, R.string.login_failed_message_pg, true)
    }

    override fun onNewRfid(rfid: RfidLoginData) {
        showInfoDialog(R.string.rfid_tag_scanned_tc, R.string.rfid_associate_wtih_xid_msg_pg, true) { shouldAssociate ->
            if (shouldAssociate) needToAssociate(rfid)
            dismiss()
        }
    }

}


/** Provides UI for RFID login process */
@Composable
fun RfidLoginWidget(
    handler: RfidLoginWidgetHandler,
) {
    val header = if (handler.headerId.value == 0) "" else stringResource(id = handler.headerId.value)
    val message = if (handler.messageId.value == 0) "" else stringResource(id = handler.messageId.value)
    if (message.isNotEmpty()) MessageDialog(
        title = header,
        message = message,
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .aspectRatio(1.8f),
        onDismiss = { handler.dismiss() },
        okText = stringResource(id = R.string.pair_rfid_tag_tc),
        cancelText = stringResource(id = R.string.skip_tc),
        onConfirm = handler.shouldAssociateCallback.value)
}



/** ViewModel may need to coordinate with widget, so provide handler class that can feed widget from ViewModel */
class PhoneLoginWidgetHandler(
    val inProgress: (Boolean) -> Unit = {},
    val loginComplete: () -> Unit = {},
    val readyForNextScan: () -> Unit = {},
) : PhoneLoginHandler() {
    internal val headerId = mutableStateOf(0)
    internal val messageId = mutableStateOf(0)
    internal val allowDismiss = mutableStateOf(false)

    internal fun dismiss() {
        if (allowDismiss.value) {
            inProgress(false)
            messageId.value = 0 // close box
            readyForNextScan()
        }
    }

    private fun showInfoDialog(titleText: Int, messageText: Int, closeOnTouch: Boolean = true) {
        headerId.value = titleText
        messageId.value = messageText
        allowDismiss.value = closeOnTouch
    }

    override fun onLoginAttempt() {
        inProgress(true)
        showInfoDialog(R.string.phone_login_tc, R.string.phone_login_pg, false)
    }

    override fun onLoginSuccess() {
        inProgress(false)
        messageId.value = 0 // close box
        loginComplete() // proceed with logged in flow
    }

    override fun onLoginFailure() {
        showInfoDialog(R.string.phone_login_tc, R.string.login_failed_message_pg, true)
    }
}


/** Provides UI for Phone login process */
@Composable
fun PhoneLoginWidget(
    handler: PhoneLoginWidgetHandler,
) {
    val header = if (handler.headerId.value == 0) "" else stringResource(id = handler.headerId.value)
    val message = if (handler.messageId.value == 0) "" else stringResource(id = handler.messageId.value)
    if (message.isNotEmpty()) MessageDialog(
        title = header,
        message = message,
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .aspectRatio(1.8f),
        onDismiss = { handler.dismiss() })
}
