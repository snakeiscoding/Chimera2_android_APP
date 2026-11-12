package com.jht.progression.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.progression.R
import com.jht.progression.ui.MmColors
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize


@Composable
fun MmBaseOptionDialog(
    open: Boolean,
    width: Dp = 800.dp.scaleSize(),
    height: Dp = 438.dp.scaleSize(),
    acceptButtonText: String = stringResource(id = R.string.yes_uc),
    acceptButtonColor: Color = MmColors.futuristicBlue,
    dismissButtonText: String = stringResource(id = R.string.no_uc),
    dismissButtonColor: Color = Color.White,
    acceptRequiresHold: Boolean = false,
    dismissOnOutsideTap: Boolean = true,
    onDismiss: () -> Unit,
    onYesClicked: () -> Unit,
    content: (@Composable () -> Unit)
) {
    MmBaseDialog(
        open = open,
        width = width,
        height = height,
        dismissOnOutsideTap = dismissOnOutsideTap,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp.scaleSize()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { content() }

            Row {
                MmThemedButton(
                    text = dismissButtonText,
                    color = dismissButtonColor,
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_button")
                )

                Spacer(modifier = Modifier.width(60.dp.scaleSize()))

                MmThemedButton(
                    text = acceptButtonText,
                    color = acceptButtonColor,
                    onClick = onYesClicked,
                    requireLongPress = acceptRequiresHold,
                    modifier = Modifier.testTag("confirm_button")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun Preview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        MmBaseOptionDialog(
            open = true,
            onDismiss = {},
            onYesClicked = {}
        ) {}
    }
}