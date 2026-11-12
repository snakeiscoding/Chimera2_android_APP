package com.jht.progression.ui.setup.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmBaseDialog
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

/**
 * Dialog displayed for non-workout events when clicked
 */
@Composable
fun NonWorkoutEventDialog(
    open: Boolean,
    iconId: Int,
    titleTextId: Int,
    labelTextId: Int,
    onDismiss: () -> Unit,
) {
    MmBaseDialog(
        open = open,
        width = 549.dp.scaleSize(),
        height = 323.dp.scaleSize(),
        dismissOnOutsideTap = true,
        onDismiss = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight().padding(horizontal = 60.dp.scaleSize())
        ) {
            Image(
                painter = painterResource(iconId),
                colorFilter = ColorFilter.tint(MmColors.futuristicBlue),
                contentDescription = "icon",
                modifier = Modifier.padding(bottom = 15.dp.scaleSize())
            )

            Text(
                text = stringResource(titleTextId),
                textAlign = TextAlign.Center,
                fontSize = 36.sp.scaleSize(),
                fontWeight = FontWeight.Bold,
                style = oxaniumStyle,
                modifier = Modifier.testTag("dialog_title").padding(bottom = 25.dp.scaleSize())
            )

            Text(
                text = stringResource(labelTextId),
                textAlign = TextAlign.Center,
                fontSize = 21.sp.scaleSize(),
                fontWeight = FontWeight.Medium,
                style = oxaniumStyle,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.testTag("dialog_label")
            )

        }
    }
}

@AllDevicePreviews
@Composable
private fun Preview() {
    NonWorkoutEventDialog(
        open = true,
        iconId = R.drawable.ic_rest_mm,
        titleTextId = R.string.rest_tc,
        labelTextId = R.string.rest_message_pg
    ) { }
}