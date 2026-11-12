package com.jht.welcome

import androidx.compose.runtime.Immutable

@Immutable
data class QrUiState(
    val qrUri: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val qrShown: Boolean = false,
    val onShowQrClicked: () -> Unit = {},
    val onQrDismissed: () -> Unit = {},
)
