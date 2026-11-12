package com.jht.welcome.vortex.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.welcome.R
import com.lightspark.composeqr.QrCodeView

@Composable
fun QrLogin(
    modifier: Modifier = Modifier,
    qrUri: String? = null,
    userCode: String? = null,
    verificationUri: String? = null,
    showQr: Boolean,
    onShowQrClicked: () -> Unit,
    onQrDismissed: () -> Unit = {},
) {
    val showQrLatest by rememberUpdatedState(showQr)
    val onQrDismissedLatest by rememberUpdatedState(onQrDismissed)
    DisposableEffect(Unit) {
        onDispose { if (showQrLatest) onQrDismissedLatest() }
    }
    if (!showQr) {
        QrLoginButton(
            modifier = modifier,
            onClick = onShowQrClicked,
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            Text(
                text = stringResource(R.string.qr_code_login_tc),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = .03.sp,
                color = Color(0xFFf5f5f5),
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.scan_qr_code_to_login_pg),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFdcdcdc),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.02.sp
            )
            Spacer(modifier = Modifier.size(24.dp))
            // TODO: display fallback for users who can't scan (in data but not designs)
            Box(
                modifier = Modifier
                    .background(
                        if (qrUri != null) {
                            Color.White
                        } else {
                            Color.Transparent
                        }
                    )
                    .size(145.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                qrUri?.let { data ->
                    QrCodeView(
                        data = data,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

private const val PREVIEW_QR_URI = "https://example.com/qr-preview?user_code=X7Y9-Z2"

@Preview
@Composable
private fun PreviewQrLoginButtonState() {
    DynamicTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            QrLogin(qrUri = PREVIEW_QR_URI, showQr = false, onShowQrClicked = {})
        }
    }
}

@Preview
@Composable
private fun PreviewQrLoginQrCodeState() {
    DynamicTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            QrLogin(
                qrUri = PREVIEW_QR_URI,
                userCode = "X7Y9-Z2",
                verificationUri = "https://example.com/qr-preview",
                showQr = true,
                onShowQrClicked = {},
            )
        }
    }
}

