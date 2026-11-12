package com.jht.sprint8.assessment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.FrameType
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_RED
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


/**
 * Assessment Level Selection Card
 * Displays a selectable card representing an assessment level
 * (Beginner, Intermediate, Advanced, or Elite).
 * @param title The label of the assessment level (e.g., Beginner, Intermediate, Advanced, Elite).
 * @param titleColor The color used for the level title text.
 * @param textColor The color used for text within the card.
 * @param speedOrResistanceRangeTitle The heading displayed for either the speed or resistance range,
 * depending on the selected modality.
 * @param speedOrResistanceRangeSubtitle The value range displayed for speed or resistance.
 * @param inclineRange The incline range associated with the selected assessment level.
 * @param borderColor The color of the card border.
 * @param onCardClick Callback invoked when the user selects the assessment level card.
 * @param cardColors Defines the background and content colors for the card based on the assessment level.
 * @param frameType Determines whether speed or resistance data is displayed,
 * based on the current modality.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentLevelSelectionComp(
    title: String,
    titleColor: Color,
    textColor: Color,
    speedOrResistanceRangeTitle: String = "",
    speedOrResistanceRangeSubtitle: String = "",
    inclineRange: String? = "",
    borderColor: Color,
    onCardClick : () -> Unit,
    cardColors: Color,
    frameType: FrameType
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val componentTag = "AssessmentLevelSelectionComp"
    Card(
        modifier = Modifier
            .width(screenWidth * 0.2f)
            .height(screenHeight * 0.42f)
            .clickable { onCardClick() }
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("Sprint8Assessment:$title:Button"),
        colors = CardDefaults.cardColors(containerColor = cardColors),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(32.dp.computeSize())
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 42.sp.computeSize(),
                    lineHeight = 46.sp.computeSize(),
                    color = titleColor,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp.computeSize())
                        .testTag("$componentTag:Title:Text")
                )
                Spacer(modifier = Modifier.height(24.dp.computeSize()))
                Text(
                    speedOrResistanceRangeTitle,
                    fontSize = 18.sp.computeSize(),
                    lineHeight = 20.sp.computeSize(),
                    color = textColor.copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("$componentTag:SubtitleRange:Text"),
                )
                Text(
                    speedOrResistanceRangeSubtitle,
                    fontSize = 26.sp.computeSize(),
                    lineHeight = 28.sp.computeSize(),
                    color = textColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("$componentTag:SubtitleValue:Text"),
                )
                if (frameType.isTreadmill || frameType.isAscent) {
                    Spacer(modifier = Modifier.height(18.dp.computeSize()))
                    Text(
                        stringResource(R.string.incline_range_tc),
                        fontSize = 18.sp.computeSize(),
                        lineHeight = 20.sp.computeSize(),
                        color = textColor.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("$componentTag:SubtitleRange:Text"),
                    )
                    if (inclineRange != null) {
                        Text(
                            inclineRange,
                            fontSize = 26.sp.computeSize(),
                            lineHeight = 28.sp.computeSize(),
                            color = textColor,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("$componentTag:SubtitleValue:Text"),
                        )
                    }
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun AssessmentLevelSelectionCompPreview(){
    AssessmentLevelSelectionComp(
        title = stringResource(R.string.beginner_tc),
        borderColor = SPRINT8_NEW_RED,
        speedOrResistanceRangeTitle = "",
        inclineRange = "",
        onCardClick = {},
        cardColors = SPRINT8_NEW_RED.copy(alpha = 0.1f),
        textColor = Color.White,
        titleColor = Color.White,
        frameType = FrameType.Treadmill
    )
}