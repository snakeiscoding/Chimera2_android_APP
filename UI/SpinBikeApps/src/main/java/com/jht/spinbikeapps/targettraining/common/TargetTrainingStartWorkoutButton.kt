package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun TargetTrainingStartWorkoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val componentTag = "TTStartWorkoutButton"

    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp.computeSizeHd())
            .width(218.dp.computeSizeHd())
            .semantics {
                testTagsAsResourceId = true
            }.testTag("$componentTag:StartWorkout:Button"),
        shape = RoundedCornerShape(18.dp.computeSizeHd()),
        colors = ButtonDefaults.buttonColors(containerColor = TargetTrainingColors.BTN_PRIMARY),
    ) {
        Text(
            text = stringResource(id = R.string.start_workout_tc),
            color = TargetTrainingColors.BTN_PRIMARY_ON_PRIMARY,
            fontSize = 22.sp.computeSize(1920f),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Device22inPreviews
@Composable
private fun TargetTrainingStartWorkoutButtonPreview() {
    TargetTrainingStartWorkoutButton(onClick = {})
}
