package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize

@Composable
fun PrivacyPolicyPopUp(
    showPrivacyPolicy: MutableState<Boolean>,
    onAccept: () -> Unit = {},
) {
    if (showPrivacyPolicy.value) {
        Popup(
            onDismissRequest = {
                showPrivacyPolicy.value = false
            },
            alignment = Alignment.Center

        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .clickableNoEffect(onClick = { showPrivacyPolicy.value = false }),
                contentAlignment = Alignment.Center
            ){
                PrivacyPolicy(showPrivacyPolicy, onAccept)
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun preview() {
    val showPP = remember {
        mutableStateOf(true)
    }
    PrivacyPolicy(showPP)
}

@Composable
private fun PrivacyPolicy(
    showPP: MutableState<Boolean>,
    onAccept: () -> Unit = {},
) {
    var isPolicyLoaded by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .clickableNoEffect(
            onClick = {/* do nothing, so clicking inside the window doesn't dismiss it */ },
        )
        .fillMaxHeight(.9f)
        .fillMaxWidth(.7f),
        contentAlignment = Alignment.Center
    ) {
        Column {
            LegalDocumentContent(
                document = LegalDocument.PRIVACY_POLICY,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(6f),
                onLoadedChanged = { isPolicyLoaded = it }
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(DynamicTheme.colors.gray1),
                contentAlignment = Alignment.Center
            ) {
                ThemeFlatButton(
                    "Accept",
                    onClick = {
                        onAccept()
                        showPP.value = false
                    },
                    isPrimary = true,
                    // don't allow accepting a policy the user hasn't been able to see
                    enabled = isPolicyLoaded,
                    modifier = Modifier
                        .fillMaxWidth(.4f)
                        .fillMaxHeight()
                        .padding(10.dp.computeSize())
                )
            }
        }
    }
}
