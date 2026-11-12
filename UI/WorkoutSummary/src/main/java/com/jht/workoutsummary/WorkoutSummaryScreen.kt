package com.jht.workoutsummary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.concierge.ui.ConciergeButton
import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.interfaces.models.FitnessTestResult
import com.jht.interfaces.models.FullStatData
import com.jht.interfaces.models.QuickStatData
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.unit.UnitFormatter
import com.jht.widgets.AutoSizeText
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.HomeButton
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.WebViewWidget
import com.jht.widgets.fillScreenHeight
import com.jht.widgets.helpers.colorInvertIfDark
import com.jht.widgets.helpers.getMyZoneColor
import com.jht.widgets.helpers.stringResourceDefault

private val pageBackgroundColor @Composable get() = DynamicTheme.colors.gray1

// we really need to figure out how we want to handle text styles? maybe a base then standard sizes then weights?
private val normalText1 @Composable get() = DynamicTheme.typography.textStyle1.copy(fontWeight = FontWeight.Normal)
private val normalText2 @Composable get() = DynamicTheme.typography.textStyle2.copy(fontWeight = FontWeight.Normal)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutSummaryScreen(
    quickStats: List<QuickStatData>,
    fullStats: List<FullStatData>,
    workoutSplits: SplitTable?,
    fitnessTestResult: FitnessTestResult? = null,
    myZoneResults: MyZoneResults? = null,
    theme: DynamicThemeData = defaultDynamicTheme,
    surveyUrl: String = "",
    onHome: () -> Unit = {},
) {
    val showSurvey = remember(surveyUrl) { mutableStateOf(surveyUrl.isNotEmpty()) }

    DynamicTheme(theme) {
        Column(modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(DynamicTheme.colors.background1)
        ) {
            val pagerState = rememberPagerState { 2 }

            // we could hide top banner with rememberKeyboardState() to gain more space for keyboard?
            // TOP BANNER
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillScreenHeight(0.1f) // keyboard adjust resize will make activity smaller so use screen, should we just add panel dimensions to DynamicTheme?
                    .background(DynamicTheme.colors.panelTopBackground.brush()),
            ) {
                // Top Left Button Row
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(30.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeButton(Modifier.aspectRatio(120/70f)) {
                        if (showSurvey.value) showSurvey.value = false
                        else onHome()
                    }
                    ConciergeButton()
                }
                // Top Center Text - if text does not size with bar, it could look off, so let's auto size to some percentage of the height
                AutoSizeText(stringResource(R.string.workout_title_tc),
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight(0.6f),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle3,
                    testTag = "Summary:Title:Text"
                )
            }
            Divider(Modifier.fillMaxWidth().height(1.dp), color = DynamicTheme.colors.gray3)

            // BODY SECTION (we have a background image behind a pager for quick/full)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(pageBackgroundColor),
            ) {
                // Creative wants image for first page but not second page (and it has to be behind the dots in the first page)
                if (pagerState.currentPage == 0) SummaryBackground(Modifier.fillMaxSize())
                Column {
                    HorizontalPager(
                        pagerState,
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        when (it) {
                            0 -> QuickSummary(quickStats)
                            else -> FullSummary(fullStats, workoutSplits, fitnessTestResult, myZoneResults)
                        }
                    }
                    // Page indicator
                    PagerJumpingDotIndicator(
                        pagerState,
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxHeight(0.1f),
                        dotSpacing = 20.dp,
                    )
                }
                if (showSurvey.value) WebViewWidget(surveyUrl, Modifier.matchParentSize())
            }

        } // Column
    } // Theme
} // Screen

/** Provides optional background image for summary screen */
@Composable
private fun SummaryBackground(
    modifier: Modifier = Modifier,
) {
    // do we need a 'preset' or 'default' URI? maybe stick to external assets? JUST DO EXT?
    // maybe a 'resourceByName' scheme for internal URI so theme does not need to know package name or resource ID?
    if (DynamicTheme.images.summaryBackground.scheme == "preset") {
        Image(
            painterResource(R.drawable.background_mountains), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = 0.5f,
            colorFilter = colorInvertIfDark(DynamicTheme.colors.text1), // image is dark, so if text is dark we invert image
        )
    } else {
        GlideAndPreviewImage(
            DynamicTheme.images.summaryBackground, modifier,
            contentScale = ContentScale.FillBounds,
            alpha = 0.5f,
        )
    }
}


@Composable
private fun QuickSummary(
    quickStats: List<QuickStatData>,
) {
    Box(Modifier.fillMaxSize()) {
        Row (
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            quickStats.forEachIndexed { index, stat ->
                QuickStat(stat, index)
            }
        }
    }
}

@Composable
private fun QuickStat(
    stat: QuickStatData,
    index: Int
) {
    val nonValueColor = DynamicTheme.colors.text1.copy(0.75f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val labelText = if (stat.labelId == 0) "" else stringResource(stat.labelId)
        val unitText = if (stat.unitId == 0) "" else stringResource(stat.unitId)
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(stat.iconId), null,
                Modifier.size(30.dp), nonValueColor,
            )
            Spacer(Modifier.size(10.dp))
            Text(labelText, Modifier.testTag("Summary:QuickStat:Title:$index"), color = nonValueColor, style = normalText2)
        }
        Text(stat.value, Modifier.testTag("Summary:QuickStat:Value:$index"), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle3.copy(fontSize = 48.sp))
        Spacer(Modifier.size(10.dp))
        Text(unitText, Modifier.testTag("Summary:QuickStat:Unit:$index"), color = nonValueColor, style = normalText1)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullSummary(
    fullStats: List<FullStatData>,
    workoutSplits: SplitTable?,
    fitnessTestResult: FitnessTestResult?,
    myZoneResults: MyZoneResults?,
) {
    val cellModifier = Modifier.padding(20.dp)
    LazyColumn(Modifier.fillMaxSize()) {
        // FITNESS TEST RESULTS
        fitnessTestResult?.let {
            stickyHeader {
                HeaderRow {
                    HeaderText(stringResource(R.string.fitness_test_result_uc), cellModifier.weight(1f).testTag("SummaryScreen:FitnessTestTitle:Text"))
                }
            }
            item {
                CellText(fitnessTestResult.getReport(), cellModifier.testTag("SummaryScreen:FitnessTestResult:Text"), first = true, total = true)
            }
        }

        // MYZONE STATS
        myZoneResults?.let {
            stickyHeader {
                HeaderRow {
                    HeaderText(stringResource(R.string.myzone_summary_mc), cellModifier.weight(1f))
                }
            }
            item { MyZoneReport(myZoneResults, cellModifier) }
        }

        // first cell is basically 2x a normal cell and does not have a divider after it
        // GENERAL STAT TABLE
        stickyHeader {
            HeaderRow {
                HeaderText("", cellModifier.weight(2f), true)
                TableDivider(false)
                HeaderText(stringResource(R.string.warmup_tc), cellModifier.weight(1f))
                TableDivider(false)
                HeaderText(stringResource(R.string.workout_tc), cellModifier.weight(1f))
                TableDivider(false)
                HeaderText(stringResource(R.string.cool_down_tc), cellModifier.weight(1f))
                TableDivider(false)
                HeaderText(stringResource(R.string.total_tc), cellModifier.weight(1f), total = true)
            }
        }
        for (statRow in fullStats) {
            item {
                val label = stringResourceDefault(statRow.labelId)
                val unit = stringResourceDefault(statRow.unitId)
                val rowLabel = label + if (unit.isEmpty()) "" else " ($unit)"
                CellRow {
                    CellText(rowLabel, cellModifier.weight(2f), true)
                    TableDivider(false)
                    CellText(statRow.warmupText, cellModifier.weight(1f))
                    TableDivider()
                    CellText(statRow.workoutText, cellModifier.weight(1f))
                    TableDivider()
                    CellText(statRow.cooldownText, cellModifier.weight(1f))
                    TableDivider()
                    CellText(statRow.totalText, cellModifier.weight(1f), total = true)
                }
                TableDivider(horizontal = true)
            } // END ITEM
        } // END EACH DATA ROW

        // SPLITS
        if (workoutSplits?.hasSplits() == true) {
            stickyHeader {
                val distanceLabel = "${stringResource(R.string.summary_split_distance_uc)} (${stringResourceDefault(workoutSplits.distanceUnitId)})"
                HeaderRow {
                    HeaderText(stringResource(R.string.summary_number_split_uc), cellModifier.weight(1f))
                    TableDivider(false)
                    HeaderText(stringResource(R.string.summary_split_time_uc), cellModifier.weight(1f))
                    TableDivider(false)
                    HeaderText(distanceLabel, cellModifier.weight(1f))
                }
            }
            items(workoutSplits.splits, key = {it.number}) {split ->
                CellRow {
                    CellText(split.number, cellModifier.weight(1f))
                    TableDivider()
                    CellText(split.time, cellModifier.weight(1f))
                    TableDivider()
                    CellText(split.distance, cellModifier.weight(1f))
                }
                TableDivider(horizontal = true)
            }
        } // END split info
        // LAPS
        workoutSplits?.let {
            // if no splits, provide a different header to separate lap information from the big table
            if (!workoutSplits.hasSplits()) stickyHeader {
                HeaderRow {
                    HeaderText(stringResource(id = R.string.laps_uc), cellModifier.weight(1f))
                }
            }
            item {
                // Put lap summary stats on one row
                CellRow {
                    CellText(stringResource(R.string.summary_total_laps_uc), cellModifier.weight(1f))
                    TableDivider(false)
                    CellText(workoutSplits.laps, cellModifier.weight(1f))
                    TableDivider()
                    CellText(stringResource(R.string.best_lap_uc), cellModifier.weight(1f))
                    TableDivider(false)
                    CellText(workoutSplits.lapBestTime, cellModifier.weight(1f))
                }
                TableDivider(horizontal = true)
            }
        } // END lap info

    } // END LAZY COLUMN
}

@Composable
private fun HeaderRow(
    content: @Composable RowScope.() -> Unit
) {
    val headerColor = DynamicTheme.colors.gray2.copy(0.5f)
    Row(
        Modifier
            .background(pageBackgroundColor) // we need solid background color or you will see the cells behind it,
            .background(headerColor),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content
    )
}
@Composable
private fun CellRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content
    )
}

@Composable
private fun HeaderText(
    text: String,
    modifier: Modifier,
    first: Boolean = false,
    total: Boolean = false,
) {
    val align = if (first) TextAlign.Start else TextAlign.Center
    val color = if (total) DynamicTheme.colors.text1 else DynamicTheme.colors.text1.copy(0.6f)
    Text(text, modifier, color, style = DynamicTheme.typography.textStyle2, textAlign = align)
}
@Composable
private fun CellText(
    text: String,
    modifier: Modifier,
    first: Boolean = false,
    total: Boolean = false,
) {
    val align = if (first) TextAlign.Start else TextAlign.Center
    val color = if (first || total) DynamicTheme.colors.text1 else DynamicTheme.colors.text1.copy(0.6f)
    val style = if (total) DynamicTheme.typography.textStyle2 else if (first) normalText2 else normalText1
    Text(text, modifier, color, style = style, textAlign = align)
}

@Composable
private fun TableDivider(
    show: Boolean = true,
    horizontal: Boolean = false,
) {
    val borderColor = DynamicTheme.colors.gray3
    val borderThickness = 2.dp
    if (show) {
        val modifier = if (horizontal) Modifier
            .fillMaxWidth()
            .height(borderThickness)
        else Modifier
            .fillMaxHeight()
            .width(borderThickness)
        Divider(modifier, color = borderColor)
    } else {
        Spacer(Modifier.size(borderThickness))
    }
}


//region MyZone
@Composable
private fun MyZoneReport(
    myZoneResults: MyZoneResults,
    modifier: Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
    ) {
        MyZoneDetails(myZoneResults, Modifier.weight(1f))
        Spacer(Modifier.weight(0.1f))
        MyZoneTimeInZones(myZoneResults, Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.MyZoneDetails(
    myZoneResults: MyZoneResults,
    modifier: Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MyZoneLabel(R.string.move_details_uc, Modifier.padding(5.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(Modifier.weight(1f)) {
                MyZoneLabel(R.string.duration_tc)
                MyZoneLabel(R.string.effort_tc)
                MyZoneLabel(R.string.meps_tc)
            }
            Column(Modifier.weight(1f)) {
                MyZoneValue(myZoneResults.duration + " " + stringResource(R.string.min_abbrev_lc))
                MyZoneValue(myZoneResults.effort + " %")
                MyZoneValue(myZoneResults.meps)
            }
            Column(Modifier.weight(1f)) {
                MyZoneLabel(R.string.calories_tc)
                MyZoneLabel(R.string.average_hr_tc)
                MyZoneLabel(R.string.peak_hr_tc)
            }
            Column(Modifier.weight(1f)) {
                MyZoneValue(myZoneResults.calories)
                MyZoneValue(myZoneResults.averageHr)
                MyZoneValue(myZoneResults.peakHr)
            }
        }
    }
}

@Composable
private fun RowScope.MyZoneTimeInZones(
    myZoneResults: MyZoneResults,
    modifier: Modifier,
) {
    // looking at the old code, it seems to be implied that there are 6 zones
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MyZoneLabel(R.string.time_in_zones_tc, Modifier.padding(5.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (i in 0..2) {
                    MyZonePerZone(myZoneResults, i)
                }
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (i in 3..5) {
                    MyZonePerZone(myZoneResults, i)
                }
            }
        }
    }
}
@Composable
private fun MyZonePerZone(
    myZoneResults: MyZoneResults,
    index: Int,
) {
    val zone = myZoneResults.zone(index)
    val color = getMyZoneColor(index)
    if (zone == null) {
        // nothing
    } else {
        val value = UnitFormatter.formatTimeSeconds(zone.seconds) +
                " - " + UnitFormatter.formatInt(zone.meps) +
                " " + stringResource(R.string.meps_tc)
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(20.dp, 10.dp)
                    .background(color)
                    .border(1.dp, Color.Black) // adding a known border in case background color blends
            )
            Spacer(Modifier.size(5.dp))
            MyZoneValue(value)
        }
    }
}

@Composable
private fun MyZoneLabel(
    stringResId: Int,
    modifier: Modifier = Modifier,
) {
    Text(stringResource(stringResId), modifier, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
}
@Composable
private fun MyZoneValue(
    value: String,
    modifier: Modifier = Modifier,
) {
    Text(value, modifier, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
}
//endregion MyZone


//region Previews
private fun createDummyStats() = listOf(
    QuickStatData(R.drawable.ic_time_total_white, R.string.total_duration_tc, "300", R.string.minutes_lc),
    QuickStatData(R.drawable.ic_distance_white, R.string.total_distance_tc, "200", R.string.km_lc)
)
private fun createDummyTable() = listOf(
    FullStatData(R.string.time_elapsed_tc, R.string.minutes_lc,
        "1",
        "2",
        "3",
        "4"
    ),
    FullStatData(R.string.distance_tc, R.string.km_lc,
        "1",
        "2",
        "3",
        "4"
    )
)

@AllDevicePreviews
@Composable
private fun PreviewWorkoutSummaryScreenLight() {
    WorkoutSummaryScreen(
        quickStats = createDummyStats(),
        fullStats = createDummyTable(),
        workoutSplits = SplitTable(),
        theme = lightDynamicTheme,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewWorkoutSummaryScreenDark() {
    WorkoutSummaryScreen(
        quickStats = createDummyStats(),
        fullStats = createDummyTable(),
        workoutSplits = SplitTable(),
        theme = darkDynamicTheme,
    )
}

//endregion Previews

