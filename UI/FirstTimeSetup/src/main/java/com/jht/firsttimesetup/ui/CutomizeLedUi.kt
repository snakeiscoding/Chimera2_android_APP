package com.jht.firsttimesetup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize
import com.jht.widgets.colorpicker.ColorPickerPopup

/**
 * This view allows users to pick their preferred LED color
 * @param [colorsToDisplay] initial set of colors for users to choose (not color picker)
 * @param [selectedColorIndex] index of that corresponds to [colorsToDisplay]
 * @param [onPreselectedColorClicked] will set the selected color based on [colorsToDisplay] and
 * takes the [selectedColorIndex] as a parameter
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomLedColorCard(
    modifier: Modifier = Modifier,
    colorsToDisplay: IntArray = intArrayOf(
        0xFF3651e8.toInt(),
        0xFF2e89eb.toInt(),
        0xFF3596b3.toInt(),
        0xFF05b387.toInt(),
        0xFFea902e.toInt(),
        0xFFee2d20.toInt(),
        0xFFf9008b.toInt(),
        0xFF7c27f6.toInt(),
        0xFF511cdf.toInt(),
    ),
    selectedColorIndex: Int = 0,
    onPreselectedColorClicked: (index: Int, color: Int) -> Unit = { index, color -> }, // updates color to one of the preselects
    onColorSelected: (color: Int) -> Unit = {},
    ) {

    var customSelectedColor by remember { mutableIntStateOf(0) }

    var showColorPicker = remember { mutableStateOf(false) }

    ColorPickerPopup(
        color = selectedColorIndex,
        colorPickerVisible = showColorPicker,
        onColorChanged = { color -> onColorSelected(color) }
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
        modifier = modifier
            .fillMaxWidth(.5f)
            .fillMaxHeight(.7f)
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp), horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.led_color_tc),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                    .testTag("FTSU:LedColor:Title:Text")

            )

            Spacer(Modifier.height(16.dp.computeSize()))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {

                colorsToDisplay.forEachIndexed { index, colorHex ->
                    Box(modifier = Modifier
                        .testTag("FTSU:LedColor:$index:Button")
                        .align(Alignment.CenterVertically)
                        .clickable {
                            onPreselectedColorClicked(index,colorHex)
                        }
                        .size(
                            40.dp
                        )
                        .clip(CircleShape)
                        .background(color = Color(colorHex))
                        .border(
                            2.dp,
                            if (index == selectedColorIndex) Color.White else Color.Transparent,
                            CircleShape
                        ), contentAlignment = Alignment.Center, content = {})
                }

                CustomColorWheelIcon(
                    modifier = Modifier.testTag("FTSU:LedColor:ColorWheel:Button").size(40.dp), onClick = { showColorPicker.value = true }

                )
            }
        }
    }
}

@Preview
@Composable
private fun CustomLedColorCardPreview() {
    CustomLedColorCard(
        modifier = Modifier.size(800.dp), colorsToDisplay = intArrayOf(
            0xFF3651e8.toInt(),
            0xFF2e89eb.toInt(),
            0xFF3596b3.toInt(),
            0xFF05b387.toInt(),
            0xFFea902e.toInt(),
            0xFFee2d20.toInt(),
            0xFFf9008b.toInt(),
            0xFF7c27f6.toInt(),
            0xFF511cdf.toInt(),
        )
    )
}

/**
 * Custom icon for color wheel with dropper
 * @param [onClick] -> handles click event for icon
 */
@Composable
fun CustomColorWheelIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    val customWheelGradient = Brush.sweepGradient(
        colors = listOf(
            Color(0xff8423ff),
            Color(0xff4091f3),
            Color(0xffe99d12),
            Color(0xffea3c13),
            Color(0xfff31699),
            Color(0xff8423ff)
        )
    )

    Box(
        modifier = modifier.clickable {
            onClick()
        }) {

        Image(
            painter = painterResource(R.drawable.wheel_spinner),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(alpha = 0.99f)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(customWheelGradient, blendMode = BlendMode.SrcAtop)
                    }
                })

        Image(
            painter = painterResource(R.drawable.dropper),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd)
        )

    }

}

@Preview
@Composable
private fun CustomWheelIcon(
    modifier: Modifier = Modifier,
) {
    CustomColorWheelIcon()
}
