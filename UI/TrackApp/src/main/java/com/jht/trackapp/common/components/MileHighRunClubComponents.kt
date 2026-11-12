package com.jht.trackapp.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.trackapp.R
import com.jht.ui.computeSize

/*
    Components for the Mile High Club version of track app
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ColumnScope.MileHighInfo(
    screenWidth: Dp,
) {
    Row(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("TrackApp:MileHighClub:Bar")
            .fillMaxSize()
            .background(MILE_HIGH_BAR_BACKGROUND),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Joggers, to the left of the logo
        MileHighLevelGroup(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = stringResource(R.string.mhrc_title_joggers),
            subTitle = stringResource(R.string.mhrc_beginner_intermediate),
            speeds = listOf(
                stringResource(R.string.mhrc_jogger_level_1),
                stringResource(R.string.mhrc_jogger_level_2),
                stringResource(R.string.mhrc_jogger_level_3),
                stringResource(R.string.mhrc_jogger_level_4)
            ),
            testTagBase = "TrackApp:MileHighClub:Joggers"
        )

        // Mile High Run Club logo, centered in the bar
        Image(
            painter = painterResource(id = R.drawable.mile_high_run_club_white),
            contentDescription = "mile high run club logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(screenWidth * MILE_HIGH_LOGO_WIDTH_PERCENT)
                .aspectRatio(MILE_HIGH_LOGO_ASPECT_RATIO)
                .testTag("TrackApp:MileHighClub:Logo")
        )

        // Racers, to the right of the logo
        MileHighLevelGroup(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            title = stringResource(R.string.mhrc_title_racers),
            subTitle = stringResource(R.string.mhrc_advanced_elite),
            speeds = listOf(
                stringResource(R.string.mhrc_racers_level_1),
                stringResource(R.string.mhrc_racers_level_2),
                stringResource(R.string.mhrc_racers_level_3),
                stringResource(R.string.mhrc_racers_level_4)
            ),
            testTagBase = "TrackApp:MileHighClub:Racers"
        )
    }
}

/**
 * One half of the Mile High Run Club bar: a title block plus a LEVEL / SPEED table
 * with a divider between every speed row.
 */
@Composable
private fun MileHighLevelGroup(
    modifier: Modifier = Modifier,
    title: String,
    subTitle: String,
    speeds: List<String>,
    testTagBase: String
) {
    Row(modifier = modifier.padding(top = MILE_HIGH_GROUP_TOP_PADDING.computeSize())) {

        Spacer(modifier = Modifier.weight(MILE_HIGH_GROUP_START_WEIGHT))

        Column(modifier = Modifier.weight(MILE_HIGH_TITLE_WEIGHT)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 30.sp.computeSize(),
                fontWeight = FontWeight.Bold,
                style = DynamicTheme.typography.textStyle2,
                modifier = Modifier.testTag("$testTagBase:Title:Text")
            )
            Text(
                text = subTitle,
                color = Color.White,
                fontSize = 22.sp.computeSize(),
                style = DynamicTheme.typography.textStyle1,
                modifier = Modifier
                    .width(MILE_HIGH_INFO_WIDTH.computeSize())
                    .testTag("$testTagBase:Info:Text")
            )
        }

        Column(modifier = Modifier.weight(MILE_HIGH_TABLE_WEIGHT)) {

            MileHighLevelRow(
                level = stringResource(R.string.mhrc_level_title),
                speed = stringResource(R.string.mhrc_speed_title_mph),
                fontSize = 15.sp.computeSize(),
                fontWeight = FontWeight.Bold,
                testTagBase = "$testTagBase:Header"
            )

            speeds.forEachIndexed { index, speed ->
                MileHighLevelRow(
                    modifier = Modifier.padding(top = MILE_HIGH_ROW_SPACING.computeSize()),
                    level = "${index + 1}",
                    speed = speed,
                    fontSize = 18.sp.computeSize(),
                    fontWeight = FontWeight.Normal,
                    testTagBase = "$testTagBase:${index + 1}"
                )

                if (index != speeds.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = MILE_HIGH_ROW_SPACING.computeSize()),
                        thickness = 1.dp,
                        color = MILE_HIGH_DIVIDER_COLOR
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(MILE_HIGH_GROUP_END_WEIGHT))
    }
}

@Composable
private fun MileHighLevelRow(
    modifier: Modifier = Modifier,
    level: String,
    speed: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    testTagBase: String
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = level,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = fontWeight,
            style = DynamicTheme.typography.textStyle1,
            modifier = Modifier
                .weight(MILE_HIGH_LEVEL_COLUMN_WEIGHT)
                .testTag("$testTagBase:Level:Text")
        )
        Text(
            text = speed,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = fontWeight,
            style = DynamicTheme.typography.textStyle1,
            modifier = Modifier
                .weight(MILE_HIGH_SPEED_COLUMN_WEIGHT)
                .testTag("$testTagBase:Speed:Text")
        )
    }
}

// Mile High Run Club bar, ported from track_mile_high_run_club_layout.xml.
// The weights below reproduce the horizontal biases used by the original ConstraintLayout:
// title block at ~7%, LEVEL column at ~50%, SPEED column at ~65% and the dividers ending at ~92%
// of each half of the bar.
private val MILE_HIGH_BAR_BACKGROUND = Color(0x59000000)
private val MILE_HIGH_DIVIDER_COLOR = Color(0x33FFFFFF)
private val MILE_HIGH_GROUP_TOP_PADDING = 20.dp
private val MILE_HIGH_ROW_SPACING = 6.dp
private val MILE_HIGH_INFO_WIDTH = 130.dp
private const val MILE_HIGH_LOGO_WIDTH_PERCENT = 0.15f
private const val MILE_HIGH_LOGO_ASPECT_RATIO = 240f / 443f
private const val MILE_HIGH_GROUP_START_WEIGHT = 0.07f
private const val MILE_HIGH_TITLE_WEIGHT = 0.43f
private const val MILE_HIGH_TABLE_WEIGHT = 0.42f
private const val MILE_HIGH_GROUP_END_WEIGHT = 0.08f
private const val MILE_HIGH_LEVEL_COLUMN_WEIGHT = 0.36f
private const val MILE_HIGH_SPEED_COLUMN_WEIGHT = 0.64f
