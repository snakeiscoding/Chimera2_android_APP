package com.jht.progression.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.progression.R

@Composable
fun TutorialTipBox(
    modifier: Modifier = Modifier,
    title: String,
    tipBody: String,
    tipsTotal: Int,
    tipCur: Int
) {
    Layout(
        content = {
            Image(
                painter = painterResource(id = R.drawable.progression_tutorial_tip_box),
                contentDescription = "progression_tip_box",
                contentScale = ContentScale.FillBounds,
                modifier = modifier
                    .layoutId("tipBox")
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .layoutId("tipTitle")
                    .padding(horizontal = 10.dp)
            )
            Text(
                text = tipBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .layoutId("tipBody")
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "[$tipCur/$tipsTotal]",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.layoutId("tipCounter")
            )
        }
    ) { measurables, constraints ->
        val imagePlaceable = measurables.first { it.layoutId == "tipBox" }.measure(constraints)
        val textConstraints = constraints.copy(
            maxWidth = (imagePlaceable.width * 0.95).toInt(),
            maxHeight = (imagePlaceable.height * 0.55).toInt()
        )
        val tipTitlePlaceable = measurables.first { it.layoutId == "tipTitle" }.measure(textConstraints)
        val tipBodyPlaceable = measurables.first { it.layoutId == "tipBody" }.measure(textConstraints)
        val tipCounterPlaceable = measurables.first { it.layoutId == "tipCounter"}.measure(textConstraints)
        layout(width = imagePlaceable.width, height = imagePlaceable.height) {
            imagePlaceable.placeRelative(0, 0)
            tipTitlePlaceable.placeRelative(0, (imagePlaceable.height * 0.05).toInt())
            tipBodyPlaceable.placeRelative(0, (imagePlaceable.height * 0.23).toInt())
            tipCounterPlaceable.placeRelative((imagePlaceable.width * 0.8).toInt(), (imagePlaceable.height * 0.8).toInt())
        }
    }
}

@Preview(device = Devices.AUTOMOTIVE_1024p)
@Composable
private fun tutorialTipBoxPreview() {
    MaterialTheme {
        TutorialTipBox(
            title = "INTERVALS",
            tipBody = "Please follow the instructions in the middle of the screen. Here you will see either: walk, jog, or run.",
            tipsTotal = 2,
            tipCur = 1
        )
    }
}