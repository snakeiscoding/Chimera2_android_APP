package com.jht.progression.ui.setup.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

@Composable
fun NonWorkoutScreen(
    event: IEvent,
    onBackPressed: () -> Unit,
    onSkipEvent: (event: IEvent) -> Unit,
    onLaunchVaWorkout: () -> Unit
) {
    MmSetupFrame(onBackPressed = onBackPressed) {
        NonWorkoutScreenContent(event = event, onSkipEvent, onLaunchVaWorkout)
    }
}

@Composable
private fun NonWorkoutScreenContent(
    event: IEvent,
    onSkipEvent: (event: IEvent) -> Unit,
    onLaunchVaWorkout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("Non workout Screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val eventData = remember(event) { getEventData(event) }

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp.scaleSize())) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("rest_event_title"),
                text = stringResource(id = eventData.titleId),
                fontSize = 36.sp.scaleSize(),
                color = Color.White,
                style = oxaniumStyle,
            )

            MmThemedButton(
                modifier = Modifier
                    .padding(end = 70.dp.scaleSize())
                    .width(125.dp.scaleSize())
                    .height(50.dp.scaleSize())
                    .align(Alignment.CenterEnd)
                    .testTag("skip_button"),
                text = stringResource(id = R.string.skip_uc),
                verticalTextPadding = 10.dp.scaleSize(),
                onClick = { onSkipEvent(event) }
            )
        }

        Image(
            modifier = Modifier
                .size(140.dp.scaleSize())
                .padding(bottom = 50.dp.scaleSize())
                .testTag("rest_event_icon"),
            painter = painterResource(id = eventData.iconId),
            colorFilter = ColorFilter.tint(MmColors.futuristicBlue),
            contentDescription = "icon"
        )

        Text(
            modifier = Modifier
                .padding(horizontal = 300.dp.scaleSize())
                .padding(bottom = 30.dp.scaleSize())
                .testTag("rest_event_subtext1"),
            text = stringResource(id = eventData.subtextId),
            fontSize = 21.sp.scaleSize(),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = oxaniumStyle,
            lineHeight = 31.sp.scaleSize()
        )

        Text(
            modifier = Modifier
                .padding(horizontal = 250.dp.scaleSize())
                .padding(bottom = 50.dp.scaleSize())
                .testTag("rest_event_subtext2"),
            text = stringResource(id = eventData.subtext2Id),
            fontSize = 21.sp.scaleSize(),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = oxaniumStyle,
        )

        MmThemedButton(
            text = stringResource(id = R.string.workout_uc),
            color = MmColors.futuristicBlue,
            onClick = onLaunchVaWorkout,
            modifier = Modifier.testTag("workout_button")
        )
    }
}

private data class EventData(
    val iconId: Int,
    val titleId: Int,
    val subtextId: Int,
    val subtext2Id: Int
)

private fun getEventData(event: IEvent): EventData {
    return when (event.label) {
        EventLabel.REST -> EventData(
            iconId = R.drawable.ic_rest_mm,
            titleId = R.string.rest_uc,
            subtextId = R.string.rest_message_pg,
            subtext2Id = R.string.rest_message_2_pg
        )
        else -> EventData(
            iconId = R.drawable.ic_cross_weight_mm,
            titleId = R.string.cross_uc,
            subtextId = R.string.cross_message_pg,
            subtext2Id = R.string.cross_message_2_pg
        )
    }
}

@Composable
@AllDevicePreviews
private fun Preview() {
    val event: IEvent = NonWorkoutEvent(0, 0, 0, false, EventLabel.REST)
    NonWorkoutScreen(
        event = event,
        onBackPressed = {},
        onSkipEvent = {},
        onLaunchVaWorkout = {}
    )
}