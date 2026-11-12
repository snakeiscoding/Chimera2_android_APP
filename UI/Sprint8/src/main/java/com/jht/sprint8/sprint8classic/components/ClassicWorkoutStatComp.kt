package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

/**
 * Displays a classic-style card component for showing either a heart rate (BPM)
 * or RPM value with an icon, title, and customizable visual styling.
 *
 * @param modifier Modifier applied to the root composable for layout, styling,
 * and interaction behavior.
 * @param title Title text displayed to describe the metric.
 * @param icon Drawable resource ID representing the metric or null to omit the icon.
 * @param unitLabel Optional label rendered below the value. Null omits it.
 * @param labelFontWeight Weight applied to both [title] and [unitLabel].
 * @param backgroundColor Background color of the component container.
 * @param iconColor Tint color applied to the icon.
 * @param workoutStatValue Integer value representing either heart rate (BPM), RPM, or cadence (SPM).
 * @param screenWidth Screen width used to calculate responsive dimensions.
 * @param screenHeight Screen height used to calculate responsive dimensions.
 * @param borderColors List of colors used to render the component border
 * (for gradients or multi-layer effects).
 * @param testTagPrefix Prefix for test tags to support UI testing and automation.
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ClassicWorkoutStatComp(
    modifier: Modifier,
    title: String,
    icon: Int?,
    unitLabel: String? = null,
    labelFontWeight: FontWeight = FontWeight.Bold,
    backgroundColor: Color,
    iconColor: Color,
    workoutStatValue: Int,
    screenWidth: Dp,
    screenHeight: Dp,
    borderColors: List<Color>,
    testTagPrefix: String
) {
    val borderWidth = 10.dp.computeSizeHd()
    val labelFontSize = 22.sp.computeSize(1920f) //specify designWidth to match comps
    val labelLineHeight = 30.sp.computeSize(1920f)
    val valueFontSize = 80.sp.computeSize(1920f)
    val valueLineHeight = 80.sp.computeSize(1920f)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(bottom = screenHeight * 0.024f)
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2.0f
            drawCircle(
                brush = Brush.verticalGradient(borderColors),
                radius = outerRadius,
                style = Stroke(width = borderWidth.toPx())
            )
            drawCircle(
                color = backgroundColor,
                radius = outerRadius,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(1.dp.computeSizeHd(), Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = DynamicTheme.colors.text1,
                fontSize = labelFontSize,
                fontWeight = labelFontWeight,
                lineHeight = labelLineHeight,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("$testTagPrefix:Title:Text")
            )
            Row(
                modifier = Modifier
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(0.2f)
                            .testTag("$testTagPrefix:$title:Image"),
                        colorFilter = ColorFilter.tint(iconColor)
                    )
                }
                Text(
                    text = workoutStatValue.toString().takeIf { it != "0" } ?: stringResource(R.string.placeholder_dash),
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    lineHeight = valueLineHeight,
                    color = DynamicTheme.colors.text1,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("$testTagPrefix:${title}Value:Text")
                )
            }
            if (unitLabel != null) {
                Text(
                    text = unitLabel,
                    color = DynamicTheme.colors.text1,
                    fontSize = labelFontSize,
                    fontWeight = labelFontWeight,
                    lineHeight = labelLineHeight,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("$testTagPrefix:UnitLabel:Text")
                )
            }
        }
    }
}

private val previewBgColor = Color(0xFFBB751C)
private val previewIconColor = Color(0xFFFFBC42)
private val previewBorderColors = listOf(Color(0xFFFFBB65), Color(0xFF7A4400))

@AllDevicePreviews
@Composable
private fun ClassicHeartRateCompPreview(){
    ClassicWorkoutStatComp(
        modifier = Modifier.size(300.dp.computeSizeHd()),
        title = stringResource(R.string.heart_rate_tc),
        icon = R.drawable.ic_big_heart,
        backgroundColor = previewBgColor,
        iconColor = previewIconColor,
        workoutStatValue = 120,
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        borderColors = previewBorderColors,
        testTagPrefix = ""
    )
}


@AllDevicePreviews
@Composable
private fun ClassicRpmCompPreview(){
    ClassicWorkoutStatComp(
        modifier = Modifier.size(300.dp.computeSizeHd()),
        title = stringResource(R.string.rpm_uc),
        icon = R.drawable.ic_gauge,
        backgroundColor = previewBgColor,
        iconColor = previewIconColor,
        workoutStatValue = 120,
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        borderColors = previewBorderColors,
        testTagPrefix = ""
    )
}

@AllDevicePreviews
@Composable
private fun ClassicCadenceCompPreview(){
    ClassicWorkoutStatComp(
        modifier = Modifier.size(300.dp.computeSizeHd()),
        title = stringResource(R.string.cadence_tc),
        icon = null,
        unitLabel = stringResource(R.string.spm_uc),
        backgroundColor = previewBgColor,
        iconColor = previewIconColor,
        workoutStatValue = 170,
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        borderColors = previewBorderColors,
        testTagPrefix = ""
    )
}

