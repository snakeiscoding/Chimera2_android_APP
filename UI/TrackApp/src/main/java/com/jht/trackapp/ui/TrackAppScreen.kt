package com.jht.trackapp.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jht.theme.DynamicTheme
import com.jht.trackapp.R
import com.jht.trackapp.TrackAppMode
import com.jht.trackapp.common.components.BottomLapStat
import com.jht.trackapp.common.components.LapCompletePulseAnimationBox
import com.jht.trackapp.common.components.LapSelectionComponent
import com.jht.trackapp.common.components.MileHighInfo
import com.jht.trackapp.common.components.ProgressBarComps
import com.jht.trackapp.common.components.TextWithImage
import com.jht.trackapp.common.models.LapData
import com.jht.trackapp.common.models.LapDistanceEnum
import com.jht.trackapp.common.utils.Extensions.formatTime
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.ProfileGraphComponent
import com.jht.widgets.rotatePages

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TrackAppScreen(
    mode: TrackAppMode = TrackAppMode.Normal,
    currentLapDistance: LapDistanceEnum = LapDistanceEnum.DISTANCE_400,
    lapTime: Int,
    lapProgress: Float,
    laps: List<Int>,
    spm: String = "0",
    strideLength: String = "0.00",
    showCadenceStats: Boolean = false,
    onClick: (LapDistanceEnum) -> Unit = {}
) {

    // Header
    BoxWithConstraints(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .background(Color.Black)
    ) {

        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight

        //Lap Complete animation
        LapCompletePulseAnimationBox(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            lapProgress = lapProgress,
            minMaxLapAnimationOffset = getMinMaxProgressValues(currentLapDistance),
            lapsCompleted = laps.size
        )

        Column(
            modifier = Modifier
                .size(screenWidth, screenHeight)
                .padding(top = 30.dp.computeSize()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Track Components
            Box(
                modifier = Modifier
                    .width((screenWidth * 0.92f))
                    .align(Alignment.CenterHorizontally)
                    .border(
                        BorderStroke(3.dp.computeSize(), DynamicTheme.colors.gray2),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
            ) {

                Image(painter = painterResource(id = R.drawable.ic_finish), contentDescription = "finish logo", modifier = Modifier
                    .size(24.dp.computeSize())
                    .align(
                        Alignment.BottomCenter
                    )
                    .zIndex(1.0f))

                ProgressBarComps(
                    trackDimensions = DpSize(
                        width = (screenWidth * 0.92f),
                        height = (screenHeight * 0.55f)
                    ), lapProgress = lapProgress,
                    minMaxLapAnimationOffset = getMinMaxProgressValues(currentLapDistance),
                    lapDistance = currentLapDistance,
                    lapsCompleted = laps.size
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.55f)
                        .padding(20.dp.computeSize())
                        .border(
                            BorderStroke(3.dp.computeSize(), DynamicTheme.colors.gray2),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .background(Color.Black)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.SpaceAround,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Row(
                            modifier = Modifier
                                .width(screenWidth * 0.75f)
                                .fillMaxHeight(0.61f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showCadenceStats) {
                                LapCadenceStatsFrame(
                                    laps.minOrNull()?.formatTime() ?: "0:00",
                                    lapTime.formatTime(),
                                    "${laps.size}",
                                    spm,
                                    strideLength,
                                    modifier = Modifier
                                )
                            } else {
                                LapData.entries.forEachIndexed { i, entry ->
                                    TextWithImage(
                                        modifier = Modifier
                                            .weight(1f, true)
                                            .align(Alignment.CenterVertically),
                                        title = when (entry) {
                                            LapData.LAP_TIME -> lapTime.formatTime()
                                            LapData.LAPS -> "${laps.size}"
                                            LapData.BEST_LAP -> laps.minOrNull()?.formatTime() ?: "0:00"
                                        },
                                        subTitle = stringResource(entry.subTitleResource),
                                        icon = entry.res,
                                        testTagBase = "TrackApp:TrackInfo:$i"
                                    )

                                    if (i != LapData.entries.size - 1) {
                                        VerticalDivider(
                                            modifier = Modifier
                                                .fillMaxHeight(),
                                            thickness = 2.dp,
                                            color = DynamicTheme.colors.gray2
                                        )
                                    }
                                }
                            }
                        }

                        // Track Distance Component
                        LapSelectionComponent(
                            Modifier.width((screenWidth * 0.48f)),
                            itemWidth = (screenWidth.value * 0.48f) * 0.195f,
                            currentLapDistance
                        ) {
                            onClick(it)
                        }
                    }
                }
            }

            // Bottom stats
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.30f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                when (mode) {
                    TrackAppMode.Normal -> NormalBottom(
                        laps = laps,
                        screenHeight = screenHeight,
                        screenWidth = screenWidth,
                    )
                    TrackAppMode.MileHighClub -> MileHighInfo(
                        screenWidth = screenWidth,
                    )
                }
            }

        }

    }

}

@Composable
private fun ColumnScope.NormalBottom(
    laps: List<Int>,
    screenWidth: Dp,
    screenHeight: Dp,
) {
    val pagerState = rememberPagerState {
        2 + (laps).size / 16
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f),
        flingBehavior = PagerDefaults.flingBehavior(state = pagerState, snapPositionalThreshold = 0.25f),
        userScrollEnabled = true,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (it == 0) {
            ProfileGraphComponent(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp.computeSize())
            )
        } else {
            BottomLapStat(
                modifier = Modifier
                    .fillMaxWidth(), width = (screenWidth * 0.2f),
                laps,
                it - 1
            )
        }
    }

    // Indicator
    Row(
        modifier = Modifier
            .clickableNoEffect { pagerState.rotatePages() }
            .testTag("TrackApp:Pager:Indicator")
            .fillMaxWidth()
            .height(50.dp.computeSize()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PagerJumpingDotIndicator(
            pagerState,
            dotSize = 14.dp.computeSize(),
            dotSpacing = 16.dp.computeSize(),
        )
    }
}

private val LapAccentColor = Color(0xFFFFCB27)
private val CadenceAccentColor = Color(0xFF00CCCA)

@Composable
private fun StatPillHeader(title: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp.computeSize()))
            .background(accentColor.copy(alpha = 0.2f))
            .padding(horizontal = 24.dp.computeSize(), vertical = 8.dp.computeSize())
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 22.sp.computeSize(),
            fontWeight = FontWeight.SemiBold,
            style = DynamicTheme.typography.textStyle2
        )
    }
}

@Composable
private fun StatColumn(
    icon: Painter,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp.computeSize())
        )
        Text(
            text = value,
            color = Color(0xFFF5F5F5),
            fontSize = 42.sp.computeSize(),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            color = Color(0xFFABABAC),
            fontSize = 16.sp.computeSize(),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Generated from Zeplin Track - Cadence screen.
 * Replaces the plain icon/value/label trio with grouped "Lap" / "Cadence" pill cards.
 */
@Composable
private fun LapCadenceStatsFrame(
    bestLapTime: String,
    lapTime: String,
    laps: String,
    spm: String,
    strideLength: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp.computeSize()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatPillHeader(title = stringResource(R.string.lap_tc), accentColor = LapAccentColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(painterResource(R.drawable.ic_best_lap_time), bestLapTime, stringResource(R.string.best_lap_tc), LapAccentColor, Modifier.weight(1f))
                StatColumn(painterResource(R.drawable.ic_lap_time), lapTime, stringResource(R.string.lap_time_tc), LapAccentColor, Modifier.weight(1f))
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp.computeSize())) {
                Icon(painterResource(R.drawable.ic_laps), contentDescription = null, tint = LapAccentColor, modifier = Modifier.size(32.dp.computeSize()))
                Text(laps, color = Color(0xFFF5F5F5), fontSize = 72.sp.computeSize(), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.laps_tc), color = Color(0xFFABABAC), fontSize = 22.sp.computeSize(), fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp.computeSize()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatPillHeader(title = stringResource(R.string.cadence_tc), accentColor = CadenceAccentColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(painterResource(R.drawable.ic_cadence), spm, stringResource(R.string.spm_uc), CadenceAccentColor, Modifier.weight(1f))
                StatColumn(painterResource(R.drawable.ic_stride_length), "$strideLength${stringResource(R.string.m_lc)}", stringResource(R.string.stride_length_tc), CadenceAccentColor, Modifier.weight(1f))
            }
        }
    }
}

// this method will give the Lap Complete Animation's start and end values,
// so even when the speed is set to high and LapDistance set to 100 will be able to
// see the Lap Complete Animation.
private fun getMinMaxProgressValues(currentLapDistance: LapDistanceEnum) = when (currentLapDistance) {
    LapDistanceEnum.DISTANCE_100 -> Pair(0.02f, 0.965f)
    LapDistanceEnum.DISTANCE_200 -> Pair(0.01f, 0.98f)
    LapDistanceEnum.DISTANCE_400 -> Pair(0.005f, 0.99f)
    LapDistanceEnum.DISTANCE_800 -> Pair(0.002f, 0.99f)
    LapDistanceEnum.DISTANCE_1600 -> Pair(0.001f, 0.995f)
}

@AllDevicePreviews
@Composable
private fun TrackAppPreview() {
    TrackAppScreen(
        lapTime = 0,
        lapProgress = 1.0f,
        laps = listOf()
    )
}

@AllDevicePreviews
@Composable
private fun TrackAppCadencePreview() {
    TrackAppScreen(
        lapTime = 75_000,
        lapProgress = 0.42f,
        laps = listOf(70_000, 68_500, 71_200),
        spm = "162",
        strideLength = "1.12",
        showCadenceStats = true
    )
}

@AllDevicePreviews
@Composable
private fun TrackAppMileHighPreview() {
    TrackAppScreen(
        mode = TrackAppMode.MileHighClub,
        lapTime = 0,
        lapProgress = 1.0f,
        laps = listOf()
    )
}
