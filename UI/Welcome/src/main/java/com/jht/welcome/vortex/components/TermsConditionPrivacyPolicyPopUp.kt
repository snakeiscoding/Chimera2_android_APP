package com.jht.welcome.vortex.components


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Popup
import com.jht.ui.clickableNoEffect
import com.jht.ui.largerThan16
import com.jht.widgets.BlurBackgroundOverlay
import com.jht.widgets.LegalDocument
import com.jht.widgets.LegalDocumentContent

@Composable
fun TermsConditionPrivacyPolicyPopUp(
    onBack: () -> Unit,
    onClose: () -> Unit,
    showPrivacyPolicy: MutableState<Boolean>,
    showTermsAndConditions: MutableState<Boolean>
) {
    if (showPrivacyPolicy.value || showTermsAndConditions.value) {
        // dismiss whichever document is currently showing
        val dismiss = {
            showPrivacyPolicy.value = false
            showTermsAndConditions.value = false
        }
        Popup(
            onDismissRequest = dismiss,
            alignment = Alignment.Center

        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoEffect(onClick = dismiss),
                contentAlignment = Alignment.Center
            ) {
                LegalDocumentPopUpContent(
                    onBack = onBack,
                    onClose = onClose,
                    document = if (showPrivacyPolicy.value) {
                        LegalDocument.PRIVACY_POLICY
                    } else {
                        LegalDocument.TERMS_OF_SERVICE
                    }
                )
            }
        }
    }
}

@Composable
private fun LegalDocumentPopUpContent(
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    document: LegalDocument
) {
    BlurBackgroundOverlay(
        modifier = Modifier
            .fillMaxWidth(if (largerThan16()) 0.6f else 0.6f)
            .fillMaxHeight(if (largerThan16()) 0.95f else 0.95f)
            // swallow clicks inside the dialog so tapping the document text doesn't
            // fall through to the outer dismiss handler (the old WebView consumed
            // touches itself; MarkdownText's TextView does not)
            .clickableNoEffect(onClick = { /* do nothing */ }),
        showCloseButton = false,
        showBackButton = true,
        onCloseButtonClick = onClose,
        onBackButtonClick = onBack
    ) {
        Column {
            LegalDocumentContent(
                document = document,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(6f)
            )
        }
    }
}
