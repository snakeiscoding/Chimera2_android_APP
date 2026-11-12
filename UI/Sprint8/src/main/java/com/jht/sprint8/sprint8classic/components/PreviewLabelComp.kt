package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.FrameType
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays a preview label component showing key workout parameters.
 * @param modifier Modifier applied to the root composable for layout and styling.
 * @param title Title text displayed at the top of the label.
 * @param speedOrResistanceValue String representing the speed or resistance value.
 * @param inclineValue String representing the incline value.
 * @param backgroundColor Background color of the label component.
 * @param frameType Defines the visual frame or border style applied to the component.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PreviewLabelComp(
    modifier: Modifier,
    title: String,
    speedOrResistanceValue: String,
    inclineValue: String,
    backgroundColor: Color,
    frameType: FrameType
) {
    val componentTag = "Sprint8Preview"
    Row(modifier = modifier.wrapContentSize()) {
        Box(
            modifier = modifier
                .size(30.dp.computeSize())
                .background(color = backgroundColor, shape = RoundedCornerShape(8.dp.computeSize()))
                .semantics {
                    testTagsAsResourceId = true
                }
        )
        Column(
            modifier = modifier
                .wrapContentSize()
                .padding(start = 8.dp.computeSize())
        ) {
            Text(
                text = title,
                fontSize = 16.sp.computeSize(),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.SansSerif,
                color = DynamicTheme.colors.text1,
                modifier = modifier.testTag("$componentTag:Title:Text"),

                )
            Text(
                text = speedOrResistanceValue,
                fontSize = 16.sp.computeSize(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.SansSerif,
                color = DynamicTheme.colors.text1,
                modifier = modifier.testTag("$componentTag:$title:Value:Text"),
                )
            if (frameType.isTreadmill || frameType.isAscent) {
                Text(
                    text = inclineValue,
                    fontSize = 16.sp.computeSize(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.SansSerif,
                    color = DynamicTheme.colors.text1,
                    modifier = modifier.testTag("$componentTag:$title:Incline:Value:Text"),
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun PreviewLabelCompPreview(){
    PreviewLabelComp(
        modifier = Modifier,
        title = stringResource(R.string.title_tc),
        speedOrResistanceValue = "9",
        inclineValue = "9",
        backgroundColor = Color.Green,
        frameType = FrameType.Treadmill
    )
}