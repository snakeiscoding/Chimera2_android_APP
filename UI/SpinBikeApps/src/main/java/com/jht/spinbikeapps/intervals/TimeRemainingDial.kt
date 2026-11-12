package com.jht.spinbikeapps.intervals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.resources.robotoFonts
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.ui.Device22inPreviews

private val timeRemainingTextStyle = TextStyle(
    fontSize = 120.sp,
    color = TargetTrainingColors.BTN_PRIMARY,
    fontWeight = FontWeight.Bold,
    fontFamily = robotoFonts,
)

private val timeRemainingValueStyle = TextStyle(
    fontSize = 20.sp,
    fontWeight = FontWeight.Medium,
    color = TargetTrainingColors.TIME_REMAINING_LABEL_TEXT,
    fontFamily = robotoFonts,
)

@Composable
internal fun TimeRemainingDial(
    timerTextLabel: String = "",
    mainText: String = "",
    accentColor: Color = TargetTrainingColors.BTN_PRIMARY,
) {
    Column(
        modifier = Modifier.wrapContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = accentColor,
                modifier = Modifier
                    .blur(
                        radius = 50.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
                    .width(442.dp)
                    .height(30.dp)
                    .alpha(.3f),
            ) {}
            Text(
                text = mainText,
                modifier = Modifier
                    .height(120.dp)
                    .width(627.dp),
                style = timeRemainingTextStyle.copy(color = accentColor),
                textAlign = TextAlign.Center,
            )
        }

        //TODO SRTM verify this once data's connected and time countdown graphic is drawn
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = timerTextLabel,
            style = timeRemainingValueStyle,
        )
    }
}

//region Previews
@Composable
@Device22inPreviews
private fun PreviewIntervalsCountdown() {
    DynamicTheme(darkDynamicTheme) {
        TimeRemainingDial(
            mainText = "5.0",
        )
    }
}

@Composable
@Device22inPreviews
private fun PreviewIntervalsCountdownRest() {
    DynamicTheme(darkDynamicTheme) {
        TimeRemainingDial(
            mainText = "5.0",
            accentColor = TargetTrainingColors.GREEN_1
        )
    }
}
