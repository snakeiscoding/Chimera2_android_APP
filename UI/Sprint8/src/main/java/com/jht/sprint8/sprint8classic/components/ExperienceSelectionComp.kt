package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays a selectable experience card with a letter identifier, title,
 * and subtitle, styled with gradient backgrounds and borders.
 *
 * @param letter Short identifier (usually a single letter) displayed prominently.
 * @param title Main title text describing the experience option.
 * @param subTitle Supporting text providing additional details.
 * @param borderColor Solid color used for the card border.
 * @param gradientColors List of colors used for the card background gradient.
 * @param borderGradientColor List of colors used for the border gradient effect.
 * @param screenHeight Screen height used to calculate responsive dimensions.
 * @param screenWidth Screen width used to calculate responsive dimensions.
 * @param modifier Modifier applied to the root composable for layout, styling,
 * and interaction behavior.
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ExperienceSelectionComp(
    letter: String, title: String,
    subTitle: String,
    borderColor: Color,
    gradientColors: List<Color>,
    borderGradientColor: List<Color>,
    screenHeight: Dp,
    screenWidth: Dp,
    modifier: Modifier
) {
    val componentTag = "Sprint8ExperienceSelect"
    Card(
        modifier = modifier.width(screenWidth * 0.177f)
            .height(screenHeight * 0.407f)
            .semantics {
                testTagsAsResourceId = true
            },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = borderColor.copy(0.1f)),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.wrapContentSize()) {
                Canvas(modifier = Modifier.size(screenWidth * 0.180f, screenHeight * 0.180f)) {
                    val outerRadius = size.minDimension / 2
                    val ringThickness = 16.dp.toPx()
                    val strokeWidth = 2.dp.toPx()

                    drawCircle(
                        color = borderColor.copy(alpha = 0.2f),
                        radius = outerRadius - ringThickness / 2,
                        style = Stroke(width = ringThickness)
                    )

                    drawCircle(
                        brush = Brush.verticalGradient(borderGradientColor),
                        radius = outerRadius - strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = borderColor.copy(alpha = 0.005f),
                        radius = outerRadius - ringThickness
                    )

                    drawCircle(
                        brush = Brush.verticalGradient(borderGradientColor),
                        radius = outerRadius - ringThickness + strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )
                }
                Text(
                    text = letter,
                    fontSize = 80.sp.computeSize(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                        )
                    ),
                    fontFamily = FontFamily.SansSerif,
                    color = DynamicTheme.colors.text1,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("$componentTag:Letter:Text")
                )
            }

            Column(
                modifier = Modifier
                    .wrapContentSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 26.sp.computeSize(),
                    lineHeight = 30.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = screenHeight * 0.032f, start = 8.dp, end = 8.dp)
                        .testTag("$componentTag:Title:Text")
                )
                Text(
                    subTitle,
                    fontSize = 18.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 22.sp.computeSize(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .heightIn(min = screenHeight * 0.07f)
                        .fillMaxWidth()
                        .padding(top = screenHeight * 0.008f, start = 8.dp, end = 8.dp)
                        .testTag("$componentTag:SubTitle:Text")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun ExperienceSelectionCompPreview(){
    ExperienceSelectionComp(
        letter = stringResource(R.string.a_uc),
        title = stringResource(R.string.title_tc),
        subTitle = "Subtitle",
        borderColor = Color.Green,
        gradientColors = listOf(Color.Blue, Color.Red),
        borderGradientColor = listOf(Color.Blue, Color.Red),
        screenHeight = 700.dp,
        screenWidth = 1200.dp,
        modifier = Modifier
    )
}