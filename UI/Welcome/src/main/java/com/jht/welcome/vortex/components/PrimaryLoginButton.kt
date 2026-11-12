package com.jht.welcome.vortex.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.welcome.R

@Composable
fun PrimaryLoginButton(
    icon: Painter,
    titleText: String,
    subtitleText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = DynamicTheme.colors.background1),
        shape = DynamicTheme.corners.medium.makeCornerShape(),
        border = BorderStroke(1.dp, Color(0xFFababac)),
        contentPadding = PaddingValues(24.dp),
    ) {
        val color = Color.White
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titleText,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                letterSpacing = .02.sp
            )
            Text(
                text = subtitleText,
                color = Color(0xFFdcdcdc),
                fontSize = 16.sp,
                letterSpacing = 0.02.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.caret_right),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun WalletLoginButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PrimaryLoginButton(
        icon = painterResource(R.drawable.wallet_scan_rfid),
        titleText = stringResource(R.string.wallet_login_tc),
        subtitleText = stringResource(R.string.login_with_apple_or_google_wallet_tc),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun QrLoginButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PrimaryLoginButton(
        icon = painterResource(R.drawable.ic_qr_code),
        titleText = stringResource(R.string.qr_code_login_tc),
        subtitleText = stringResource(R.string.use_your_phone_to_login_pg),
        modifier = modifier,
        onClick = onClick,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewPrimaryLoginButton() {
    DynamicTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            PrimaryLoginButton(
                icon = painterResource(R.drawable.ic_login),
                titleText = "Primary Login",
                subtitleText = "Generic primary login button preview",
                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = {},
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewWalletLoginButton() {
    DynamicTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            WalletLoginButton(
                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = {},
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewQrLoginButton() {
    DynamicTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            QrLoginButton(
                modifier = Modifier.fillMaxWidth(0.5f),
                onClick = {},
            )
        }
    }
}