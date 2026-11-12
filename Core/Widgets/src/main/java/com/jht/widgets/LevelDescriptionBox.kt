package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.WorkoutInputLevelInfo
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.ui.computeSize


@Composable
@Device15inPreviews
private fun Preview() {
    val mockWorkoutCatalogModel = MockWorkoutCatalogModel()
    LevelDescriptionBox(
        modifier = Modifier.width(200.dp.computeSize()),
        level = 4f,
        workoutInputLevelInfoList =  mockWorkoutCatalogModel.getWorkoutInputLevelInfo(4),
    )
}

@Composable
fun LevelDescriptionBox(
    modifier: Modifier,
    level:Float,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
){
    Column(modifier
        .clip(RoundedCornerShape(10.dp))
        .background(color = DynamicTheme.colors.gray2, shape = RoundedCornerShape(10.dp))
        .padding(horizontal = 20.dp.computeSize(), vertical = 10.dp.computeSize())
    ) {
        Text(
            text = String.format(stringResource(R.string.concat_2_space),stringResource(R.string.level_tc),level.toInt()),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle1,
            textAlign = TextAlign.Start,
        )
        for (i in workoutInputLevelInfoList.indices) {
            LevelInfoBoxHelper(
                String.format(stringResource(R.string.concat_2_space),stringResource(R.string.average_tc),workoutInputLevelInfoList[i].name),
                workoutInputLevelInfoList[i].average
            )
            LevelInfoBoxHelper(
                String.format(stringResource(R.string.concat_2_space),stringResource(R.string.max_tc),workoutInputLevelInfoList[i].name),
                workoutInputLevelInfoList[i].max
            )
            if (i<1 && workoutInputLevelInfoList.size > 1) {
                Divider(
                    modifier = Modifier
                        .padding(vertical = 4.dp.computeSize())
                        .height(1.dp)
                        .background(DynamicTheme.colors.gray3)

                )
            }
        }
    }
}

@Composable
private fun LevelInfoBoxHelper(
    name: String,
    avgOrMax: String,
) {
    Text(
        text = name,
        color = DynamicTheme.colors.text1,
        style = DynamicTheme.typography.textStyle1,
        textAlign = TextAlign.Start,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 2.dp.computeSize())
    )
    Text(
        text = avgOrMax,
        color = DynamicTheme.colors.text1,
        style = DynamicTheme.typography.textStyle2,
        textAlign = TextAlign.Start,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(vertical = 2.dp.computeSize())
    )
}