package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BG_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BORDER_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_4
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun CurrentTargetComp(
    modifier: Modifier = Modifier,
    color: Color = BLUE_1,
    onClick: () -> Unit,
    target: String = "- -",
    subtitle: String = "",
    subtitleValue: String = ""
) {
    val componentTag = "CurrentTargetComp"
    val subtitleText = subtitle + subtitleValue
    Box(
        modifier = modifier
            .padding(start = 32.dp.computeSize(1920f), bottom = 284.dp.computeSize(1920f))
            .width(
                280.dp.computeSize(1920f),
            )
            .wrapContentHeight()
            .border(
                width = 1.dp.computeSize(1920f),
                color = BORDER_1,
                shape = RoundedCornerShape(32.dp.computeSize(1920f))
            )
            .background(
                BG_2, shape = RoundedCornerShape(32.dp.computeSize(1920f))
            ).semantics {
                testTagsAsResourceId = true
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = modifier
                .wrapContentSize()
                .padding(
                    horizontal = 24.dp.computeSize(1920f),
                    vertical = 20.dp.computeSize(1920f)
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.current_target_tc),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 20.sp.computeSize(1920f),
                ),
                color = DynamicTheme.colors.gray18,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 8.dp.computeSizeHd())
            )

            Text(
                target,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 42.sp.computeSize(1920f),
                ),
                color = DynamicTheme.colors.gray18,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 8.dp.computeSizeHd())
            )

            if (subtitleText.isNotBlank()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 16.sp.computeSize(1920f),
                        fontWeight = FontWeight.Medium
                    ),
                    color = DynamicTheme.colors.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = modifier.padding(bottom = 8.dp.computeSizeHd())
                )
            }
            Button(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag("$componentTag:ChangeTarget:Button"),
                onClick = { onClick() },
                shape = RoundedCornerShape(18.dp.computeSize(1920f)),
                border = BorderStroke(width = 1.dp.computeSize(1920f), color = color),
                colors = ButtonDefaults.buttonColors(containerColor = GRAY_4)
            ) {
                Text(
                    text = if (target != "- -") {
                        stringResource(R.string.change_tc)
                    } else {
                        stringResource(R.string.set_tc)
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp.computeSize(1920f),
                    ),
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun CurrentTargetCompPreview() {
    CurrentTargetComp(
        modifier = Modifier,
        color = Color.Red,
        onClick = {},
        target = "100"
    )
}