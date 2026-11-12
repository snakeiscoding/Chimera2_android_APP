package com.jht.progression.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

@Composable
fun MmTimeOptionDialog(
    open: Boolean,
    titleText: String,
    bodyText: String,
    timeText: String,
    icon: Painter? = null,
    width: Dp = 820.dp.scaleSize(),
    height: Dp = 509.dp.scaleSize(),
    acceptButtonText: String = stringResource(id = R.string.yes_uc),
    acceptButtonColor: Color = MmColors.futuristicBlue,
    dismissButtonText: String = stringResource(id = R.string.no_uc),
    dismissButtonColor: Color = Color.White,
    acceptRequiresHold: Boolean = false,
    dismissOnOutsideTap: Boolean = true,
    onDismiss: () -> Unit,
    onYesClicked: () -> Unit
) {
    MmBaseOptionDialog(
        open = open,
        width = width,
        height = height,
        acceptButtonText = acceptButtonText,
        acceptButtonColor = acceptButtonColor,
        dismissButtonText = dismissButtonText,
        dismissButtonColor = dismissButtonColor,
        acceptRequiresHold = acceptRequiresHold,
        dismissOnOutsideTap = dismissOnOutsideTap,
        onDismiss = onDismiss,
        onYesClicked = onYesClicked
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Image(
                    modifier = Modifier.size(60.dp.scaleSize()),
                    painter = icon,
                    contentDescription = "dialog icon",
                )
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = 30.dp.scaleSize(), vertical = 15.dp.scaleSize())
                    .testTag("dialog_title"),
                text = titleText,
                color = Color.White,
                fontSize = 36.sp.scaleSize(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = oxaniumStyle,
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 20.dp.scaleSize(), vertical = 10.dp.scaleSize())
                    .testTag("dialog_body_message"),
                text = bodyText,
                color = Color.White,
                fontSize = 21.sp.scaleSize(),
                textAlign = TextAlign.Center,
                style = oxaniumStyle,
                lineHeight = 31.sp.scaleSize()
            )
            Text(
                modifier = Modifier
                    .padding(top = 26.dp.scaleSize())
                    .testTag("dialog_time_text"),
                text = timeText,
                color = MmColors.futuristicBlue,
                fontSize = 48.sp.scaleSize(),
                fontWeight = FontWeight.Bold,
                style = oxaniumStyle,
            )
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
        MmTimeOptionDialog(
            open = true,
            icon = painterResource(id = R.drawable.ic_stopwatch_outline),
            titleText = stringResource(id = R.string.adjust_goal_time_tc),
            bodyText = stringResource(id = R.string.goal_time_adjusted_pg),
            timeText = "52:45",
            acceptButtonText = stringResource(id = R.string.accept_uc),
            dismissButtonText = stringResource(id = R.string.reject_uc),
            onDismiss = {},
            onYesClicked = {}
        )
    }
}