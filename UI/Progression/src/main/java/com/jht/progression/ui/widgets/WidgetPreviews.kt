package com.jht.progression.ui.widgets

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
private fun ProgressionSBoxPreview() {
    MaterialTheme {
        var state by remember {
            mutableStateOf(true)
        }
        Surface (
            color = Color.Transparent,
        ) {
            MmThemedButton(
                text = "NEXT",
                onClick = {
                    state = !state
                },
                modifier = Modifier.width(if(state) 150.dp else 38.dp)
            )
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
private fun StatsBoxPreview() {
    MaterialTheme {
        val stats = HashMap<String, String>()
        stats["MILE PACE"] = "00:12:37"
        stats["RECOVERY HR"] = "160 BPM"
        stats["5K PACE"] = "00:38:19"
        StatBoxWidget {
            StatBoxContent(stats = stats)
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
private fun TutorialTipPreview() {
    MaterialTheme {
        TutorialTipBox(
            title = "INTERVALS",
            tipBody = "Please follow the instructions in the middle of the screen. Here you will see either: walk, jog, or run.",
            tipsTotal = 2,
            tipCur = 1
        )
    }
}