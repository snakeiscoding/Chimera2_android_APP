package com.jht.trackapp.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.trackapp.common.utils.Extensions.formatTime
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BottomLapStat(modifier: Modifier, width: Dp = 0.dp, lapData: List<Int>, currentPage: Int) {
    LazyHorizontalGrid(rows = GridCells.Fixed(4),
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        items(16) {
            val index = (currentPage * 16) + it
            Column(
                modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("TrackApp:CurrentPage: $index").width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.width(width),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (index < lapData.size) "Lap ${index + 1}" else "", color = Color.White, textAlign = TextAlign.Left, modifier = Modifier
                        .weight(1f, false).testTag("TrackApp:LapGrid:Row:$it:Lap:Text")
                        .padding(10.dp.computeSize()), style = DynamicTheme.typography.textStyle1, fontSize = 14.sp.computeSize(), fontWeight = FontWeight.Medium)
                    Text(text = if (index < lapData.size) lapData[index].formatTime() else "", color = Color.White, textAlign = TextAlign.Right, modifier = Modifier
                        .weight(1f, false).testTag("TrackApp:LapGrid:Row:$it:Time:Text")
                        .padding(10.dp.computeSize()), style = DynamicTheme.typography.textStyle1, fontSize = 14.sp.computeSize(), fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(Modifier.width(width), color = Color(0x4DFFFFFF))
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun BottomStatPreview() {
    Box(
       modifier = Modifier.fillMaxSize()
    ) {
       BottomLapStat(modifier = Modifier.fillMaxWidth(), lapData = listOf(0, 100, 200, 3, 4, 34,3, 3,4, 233, 423,4, 234,23432, 23,4, 234,23, 4,23432, 34 ,34,23,4,234, 2,34, 32,4, 32,4, 324,324, 32,423 ,423) , currentPage = 1)
    }
}
