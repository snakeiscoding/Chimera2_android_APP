package com.jht.spinbikeapps.targettraining.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BORDER_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_PRIMARY_ON_PRIMARY
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_SECONDARY_ON_SECONDARY
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_8
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TEXT_TITLE_PRIMARY
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TEXT_TITLE_SECONDARY
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_PRIMARY
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun ConfirmationPopUp(
    modifier: Modifier = Modifier,
    onClickContinue: () -> Unit = {},
    onClickEndWorkout: () -> Unit = {},
) {
    val shape = RoundedCornerShape(18.dp.computeSizeHd())
    val componentTag = "TTConfirmationPopUp"

    Box(
        modifier = modifier
            .width(750.dp.computeSizeHd())
            .height(361.dp.computeSizeHd())
            .clip(shape)
            .background(
                color = GRAY_2,
                shape = shape
            )
            .border(
                width = 1.dp.computeSizeHd(),
                color = BORDER_2,
                shape = shape
            )
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Column(
            modifier = Modifier.padding(64.dp.computeSizeHd()),
            verticalArrangement = Arrangement.spacedBy(32.dp.computeSizeHd())
        ) {
            Text(
                text = stringResource(R.string.target_reached_tc),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp.computeSize(1920f),
                    lineHeight = 40.sp.computeSize(1920f),
                    letterSpacing = 0.04.sp.computeSize(1920f),
                    color = TEXT_TITLE_PRIMARY
                )
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp.computeSizeHd(),
                color = GRAY_8
            )
            Text(
                text = stringResource(R.string.would_you_like_to_tc),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 36.sp.computeSize(1920f),
                    letterSpacing = 0.03.sp.computeSize(1920f),
                    color = TEXT_TITLE_PRIMARY
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 32.dp.computeSizeHd(),
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Button(
                    onClick = onClickContinue,
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(containerColor = TEXT_TITLE_SECONDARY),
                    contentPadding = PaddingValues(horizontal = 24.dp.computeSizeHd()),
                    modifier = Modifier
                        .height(60.dp.computeSizeHd())
                        .widthIn(min = 180.dp.computeSizeHd())
                        .testTag("$componentTag:Continue:Button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_continue_tc),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp.computeSize(1920f),
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp.computeSize(1920f),
                            letterSpacing = 0.03.sp.computeSize(1920f),
                            color = BTN_SECONDARY_ON_SECONDARY
                        )
                    )
                }
                Button(
                    onClick = onClickEndWorkout,
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(containerColor = BTN_PRIMARY),
                    contentPadding = PaddingValues(horizontal = 24.dp.computeSizeHd()),
                    modifier = Modifier
                        .height(60.dp.computeSizeHd())
                        .widthIn(min = 180.dp.computeSizeHd())
                        .testTag("$componentTag:EndWorkout:Button")
                ) {
                    Text(
                        text = stringResource(R.string.end_workout_tc),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp.computeSize(1920f),
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp.computeSize(1920f),
                            letterSpacing = 0.03.sp.computeSize(1920f),
                            color = BTN_PRIMARY_ON_PRIMARY
                        )
                    )
                }
            }
        }
    }
}

@Device22inPreviews
@Composable
private fun PreviewConfirmationPopUp() {
    ConfirmationPopUp()
}
