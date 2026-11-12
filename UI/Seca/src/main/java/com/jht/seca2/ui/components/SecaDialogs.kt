@file:OptIn(ExperimentalComposeUiApi::class)

package com.jht.seca2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.jht.seca.R
import com.jht.seca2.ui.SecaColors
import com.jht.seca2.ui.titleToBody
import com.jht.theme.DynamicTheme
import com.jht.ui.clickableNoEffect
import com.jht.widgets.AutoSizeText

@Composable
internal fun ExitDialog(
    shown: Boolean,
    onCancel: () -> Unit,
    onExit: () -> Unit,
) {
    if (shown) {
        Popup {
            ExitDialogContent(onCancel, onExit)
        }
    }
}

@Composable
private fun ExitDialogContent(
    onCancel: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .background(SecaColors.neutralGray14.copy(alpha = 0.6f))
            .clickableNoEffect(onClick = onCancel)
            .testTag("seca:ExitDialogContent:Outer:Box")
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = SecaColors.neutralGray2,
                    shape = RoundedCornerShape(10.dp)
                )
                .align(Alignment.Center)
                .clickableNoEffect{}
                .testTag("seca:ExitDialogContent:Inner:Box")
                .padding(DynamicTheme.padding.spacing11)
        ) {
            // Width is the same across all screen sizes
            Column(
                modifier = Modifier.width(644.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoSizeText(
                    text = stringResource(R.string.sure_you_want_to_stop_now_tc),
                    color = SecaColors.titleText,
                    style = MaterialTheme.typography.headlineLarge,
                    maxFontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    modifier = Modifier
                        .padding(bottom = DynamicTheme.padding.titleToBody)
                        .testTag("seca:ExitDialogContent:Title:Text")
                )

                Text(
                    text = stringResource(R.string.exit_survey_message_pg),
                    color = SecaColors.bodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = DynamicTheme.padding.spacing11)
                        .testTag("seca:ExitDialogContent:Paragraph:Text")
                )

                Row {
                    SecaBlankButton(
                        text = stringResource(R.string.cancel_tc),
                        onClick = onCancel,
                        modifier = Modifier.padding(end = DynamicTheme.padding.spacing8)
                            .testTag("seca:ExitDialogContent:Cancel:SecaBlankButton")
                    )

                    SecaButton(
                        text = stringResource(R.string.exit_tc),
                        onClick = onExit,
                        modifier = Modifier
                            .testTag("seca:ExitDialogContent:Exit:SecaButton")
                    )
                }
            }
        }
    }
}