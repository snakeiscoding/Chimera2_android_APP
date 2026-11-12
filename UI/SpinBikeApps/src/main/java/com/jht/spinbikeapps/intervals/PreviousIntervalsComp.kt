package com.jht.spinbikeapps.intervals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.ui.pxToDp

@Composable
fun IntervalsDataTable(
    intervalsData: List<IntervalData>,
    totalDistanceText: String = "0.00",
    totalTimeText: String = "0:00",
    isMetric: Boolean = true,
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        intervalsData.forEachIndexed { index, data ->
            // Current is at top (index 0). Labels should count from 1 to N for Work intervals.
            val labelIndex = (intervalsData.size - index + 1) / 2
            IntervalsDataTableRow(
                dataIndex = labelIndex,
                intervalTime = data.intervalTime,
                intervalDistance = data.intervalDistance,
                intervalPhaseIs = data.intervalType,
                isMetric = isMetric
            )
        }
        IntervalsDataTotalRow(
            totalDistanceText = totalDistanceText,
            totalTimeText = totalTimeText,
        )
    }
}

/**
 * Fill each row of the Previous Intervals table with the proper data
 */
@Composable
private fun IntervalsDataTableRow(
    dataIndex: Int = 0,
    intervalTime: String,
    intervalDistance: String,
    intervalPhaseIs: IntervalPhase = IntervalPhase.Other,
    isMetric: Boolean = true,
) {
    val phaseColor = if (intervalPhaseIs == IntervalPhase.Work) {
        TargetTrainingColors.WORK_ROW_BACKGROUND
    } else {
        TargetTrainingColors.REST_ROW_BACKGROUND
    }
    val intervalUnit = if (isMetric) {
        stringResource(R.string.km_lc)
    } else {
        stringResource(R.string.mi_lc)
    }
    Row(
        modifier = Modifier
            .height(intervalRowHeight)
            .width(intervalRowWidth)
            .clip(shape = intervalRowCorner)
            .background(color = phaseColor)
            .border(
                shape = intervalRowCorner,
                width = 1.dp,
                color = phaseColor,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (intervalPhaseIs == IntervalPhase.Rest) { //Rest doesn't have a label number
                ""
            } else {
                "$dataIndex"
            },
            style = previousIntervalsTableDataStyle,
            modifier = Modifier
                .width(rowNumberWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = intervalTime,
            style = previousIntervalsTableDataStyle.copy(fontSize = 20.sp),
            modifier = Modifier
                .width(rowTimeWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = "$intervalDistance $intervalUnit",
            style = previousIntervalsTableDataStyle.copy(fontSize = 20.sp),
            modifier = Modifier
                .width(rowTimeWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = if (intervalPhaseIs == IntervalPhase.Work) {
                stringResource(R.string.work_tc)
            } else {
                stringResource(R.string.rest_tc)
            },
            style = previousIntervalsTableDataStyle.copy(fontSize = 20.sp),
            modifier = Modifier
                .width(rowTimeWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.pxToDp())
            .background(color = Color.Black)
    )
}

/**
 * Fill each row of the Previous Intervals table with the proper data
 */
@Composable
private fun IntervalsDataTotalRow(
    totalDistanceText: String,
    totalTimeText: String,
) {
    Row(
        modifier = Modifier
            .height(intervalRowHeight)
            .width(intervalRowWidth)
            .clip(shape = intervalRowCorner)
            .background(color = Color.Black)
            .border(
                shape = intervalRowCorner,
                width = 1.dp,
                color = Color.Black,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(R.string.total_tc)}: ",
            style = previousIntervalsTableDataStyle,
            modifier = Modifier
                .width(rowNumberWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = totalTimeText,
            style = previousIntervalsTableDataStyle.copy(fontSize = 20.sp),
            modifier = Modifier
                .width(rowTimeWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Text(
            text = totalDistanceText,
            style = previousIntervalsTableDataStyle.copy(fontSize = 20.sp),
            modifier = Modifier
                .width(rowTimeWidth)
                .height(rowNumberHeight),
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(rowNumberWidth))
    }
}