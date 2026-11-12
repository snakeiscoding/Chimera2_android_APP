package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam
import com.jht.sprint8.R
import com.jht.sprint8.model.CustomStatInfo
import com.jht.sprint8.sprint8classic.ui.CustomIntervalData
import com.jht.ui.computeSize

// Colors (vertical gradients: darker at bottom, lighter at top)
val warmupBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF5d5f03), Color(0xFFcece2a)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(0f, 0f)
)

val sprintBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF985500), Color(0xFFffa600)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(0f, 0f)
)

val recoveryBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF377d17), Color(0xFF62ce30)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(0f, 0f)
)

val cooldownBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF004758), Color(0xFF019abe)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(0f, 0f)
)

/**
 * Displays a legend for a custom graph, providing information
 * about the workout data being visualized.
 *
 * @param workoutData List of [CustomIntervalData] used to populate legend entries.
 * @param statInfo Metadata describing the statistic being displayed, such as
 * units, colors, or value meaning.
 * @param modifier Modifier applied to the legend composable for layout and styling.
 */
@Composable
internal fun CustomGraphLegend(
    workoutData: List<CustomIntervalData>,
    statInfo: CustomStatInfo,
    modifier: Modifier = Modifier
) {
    val legendEntries = getLegendEntries(workoutData, statInfo)

    Row(modifier = modifier) {
        legendEntries.forEachIndexed { index, entry ->
            LegendItem(entry)
            if (legendEntries.lastIndex != index) {
                Spacer(modifier = Modifier.width(50.dp.computeSize(1920f)))
            }
        }
    }

}

@Composable
private fun LegendItem(legendEntry: LegendEntry, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        // color swatch
        Box(
            modifier = Modifier
                .size(25.dp.computeSize(1920f))
                .background(
                    brush = legendEntry.color,
                    shape = RoundedCornerShape(8.dp.computeSize(1920f))
                )
        )

        Spacer(modifier = Modifier.width(10.dp.computeSize(1920f)))

        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = legendEntry.title,
                fontSize = 16.sp.computeSize(1920f),
                color = Color.White,
                modifier = Modifier.padding(bottom = 3.dp.computeSize(1920f)),
            )

            Text(
                text = legendEntry.param1Info,
                fontSize = 16.sp.computeSize(1920f),
                color = Color.White.copy(alpha = 0.8f)
            )

            Text(
                text = legendEntry.param2Info,
                fontSize = 16.sp.computeSize(1920f),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private data class LegendEntry(
    val color: Brush,
    val title: String,
    val param1Info: String,
    val param2Info: String,
)


@Composable
private fun getLegendEntries(
    workoutData: List<CustomIntervalData>,
    statInfo: CustomStatInfo,
): List<LegendEntry> {
    if (workoutData.isEmpty()) return emptyList()

    val unitPrimary = statInfo.primaryStat.second.unitString
    val precisionPrimary = statInfo.primaryStat.second.precision
    val typePrimary = statInfo.primaryStat.first

    val hasSecondary = statInfo.secondaryStat != null
    val unitSecondary = statInfo.secondaryStat?.second?.unitString.orEmpty()
    val precisionSecondary = statInfo.secondaryStat?.second?.precision ?: 0
    val typeSecondary = statInfo.secondaryStat?.first

    fun Float.format(decimals: Int): String =
        if (decimals <= 0) this.toInt().toString() else "%.${decimals}f".format(this)

    // formats a range of values to show as expected in the legend
    @Composable
    fun formatRange(values: List<Float>, unit: String, precision: Int, paramType: MinMaxParam?): String {
        if (values.isEmpty()) return ""
        val min = values.min()
        val max = values.max()

        val addedString = when (paramType) {
            MinMaxParam.Speed -> " $unit"
            MinMaxParam.Incline -> "$unit ${stringResource(R.string.incline_tc)}"
            MinMaxParam.Resistance -> " ${stringResource(R.string.resistance_tc)}"
            else -> null
        }

        val builder = StringBuilder()
        if (min != max) {
            builder.append("${min.format(precision)} - ${max.format(precision)}")
        } else {
            builder.append(max.format(precision))
        }
        if (addedString != null) {
            builder.append(addedString)
        }

        return builder.toString()
    }

    val lastIndex = workoutData.lastIndex
    val warmupIndex = 0
    val cooldownIndex = lastIndex

    val sprints = (1..lastIndex - 1 step 2).toList()
    val recoveries = (2..lastIndex - 1 step 2).toList()

    fun collectPrimary(indices: List<Int>): List<Float> =
        indices.mapNotNull { workoutData.getOrNull(it)?.primaryValue?.value }

    fun collectSecondary(indices: List<Int>): List<Float> =
        indices.mapNotNull { workoutData.getOrNull(it)?.secondaryValue?.value }

    // Warmup
    val warmupPrimary = listOfNotNull(workoutData.getOrNull(warmupIndex)?.primaryValue?.value)
    val warmupSecondary = if (hasSecondary)
        listOfNotNull(workoutData.getOrNull(warmupIndex)?.secondaryValue?.value) else emptyList()

    // Sprints
    val sprintPrimary = collectPrimary(sprints)
    val sprintSecondary = if (hasSecondary) collectSecondary(sprints) else emptyList()

    // Recoveries
    val includeRecovery = sprints.size >= 2 && recoveries.isNotEmpty()
    val recoveryPrimary = if (includeRecovery) collectPrimary(recoveries) else emptyList()
    val recoverySecondary =
        if (includeRecovery && hasSecondary) collectSecondary(recoveries) else emptyList()

    // Cooldown
    val cooldownPrimary = listOfNotNull(workoutData.getOrNull(cooldownIndex)?.primaryValue?.value)
    val cooldownSecondary =
        if (hasSecondary) listOfNotNull(workoutData.getOrNull(cooldownIndex)?.secondaryValue?.value) else emptyList()

    val entries = mutableListOf<LegendEntry>()

    // Warmup
    entries += LegendEntry(
        color = warmupBrush,
        title = stringResource(R.string.warmup_tc),
        param1Info = formatRange(warmupPrimary, unitPrimary, precisionPrimary, typePrimary),
        param2Info = if (hasSecondary) formatRange(
            warmupSecondary,
            unitSecondary,
            precisionSecondary,
            typeSecondary,
        ) else ""
    )

    // Sprint
    entries += LegendEntry(
        color = sprintBrush,
        title = stringResource(R.string.sprint_intervals_tc),
        param1Info = formatRange(sprintPrimary, unitPrimary, precisionPrimary, typePrimary),
        param2Info = if (hasSecondary) formatRange(
            sprintSecondary,
            unitSecondary,
            precisionSecondary,
            typeSecondary,
        ) else ""
    )

    // Recovery (omit if there is only one sprint; then recovery == cooldown)
    if (includeRecovery) {
        entries += LegendEntry(
            color = recoveryBrush,
            title = stringResource(R.string.active_recover_tc),
            param1Info = formatRange(recoveryPrimary, unitPrimary, precisionPrimary, typePrimary),
            param2Info = if (hasSecondary) formatRange(
                recoverySecondary,
                unitSecondary,
                precisionSecondary,
                typeSecondary,
            ) else ""
        )
    }

    // Cooldown
    entries += LegendEntry(
        color = cooldownBrush,
        title = stringResource(R.string.cooldown_tc),
        param1Info = formatRange(cooldownPrimary, unitPrimary, precisionPrimary, typePrimary),
        param2Info = if (hasSecondary) formatRange(
            cooldownSecondary,
            unitSecondary,
            precisionSecondary,
            typeSecondary,
        ) else ""
    )

    return entries
}

@Preview(widthDp = 800, heightDp = 75)
@Composable
private fun LegendPreview() {
    val testData = getDefaultWorkoutWithSecondaryData()
    val statInfo = CustomStatInfo(
        primaryStat = Pair(
            MinMaxParam.Speed, MinMaxInfo(
                min = 0f,
                max = 12.5f,
                defaultValue = 5f,
                stepSize = 0.1f,
                precision = 1,
                captionText = stringResource(R.string.speed_tc),
                unitString = stringResource(R.string.mph_uc),
                measurement = null
            )
        ),
        secondaryStat = Pair(
            MinMaxParam.Incline, MinMaxInfo(
                min = 0f,
                max = 12.5f,
                defaultValue = 5f,
                stepSize = 0.5f,
                precision = 1,
                captionText = stringResource(R.string.incline_tc),
                unitString = stringResource(R.string.percent_lc),
                measurement = null
            )
        ),
    )
    CustomGraphLegend(
        workoutData = testData,
        statInfo = statInfo,
        modifier = Modifier,
    )
}

@Preview(widthDp = 800, heightDp = 75)
@Composable
private fun LegendPreviewNoSecondary() {
    val testData = getDefaultWorkout()
    val statInfo = CustomStatInfo(
        primaryStat = Pair(
            MinMaxParam.Speed, MinMaxInfo(
                min = 0f,
                max = 12.5f,
                defaultValue = 5f,
                stepSize = 0.1f,
                precision = 1,
                captionText = stringResource(R.string.speed_tc),
                unitString = stringResource(R.string.mph_uc),
                measurement = null
            )
        ),
        secondaryStat = null,
    )
    CustomGraphLegend(
        workoutData = testData,
        statInfo = statInfo,
        modifier = Modifier,
    )
}

private fun getDefaultWorkout(): List<CustomIntervalData> {
    return mutableListOf(
        CustomIntervalData(mutableStateOf(1f)), // warmup
        CustomIntervalData(mutableStateOf(5f)), // sprint1
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(6f)), // sprint2
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint3
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint4
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint5
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint6
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint7
        CustomIntervalData(mutableStateOf(2f)),
        CustomIntervalData(mutableStateOf(5f)), // sprint8
        CustomIntervalData(mutableStateOf(1.5f)), // cooldown
    )
}

private fun getDefaultWorkoutWithSecondaryData(): List<CustomIntervalData> {
    return mutableListOf(
        CustomIntervalData(mutableStateOf(1f), mutableStateOf(1.5f)), // warmup
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint1
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint2
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint3
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint4
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint5
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint6
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint7
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)),
        CustomIntervalData(mutableStateOf(5f), mutableStateOf(5.5f)), // sprint8
        CustomIntervalData(mutableStateOf(2f), mutableStateOf(2.5f)), // cooldown
    )
}
