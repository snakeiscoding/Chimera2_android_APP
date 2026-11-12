package com.jht.switchStride.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.switchStride.R
import com.jht.switchStride.utils.backgroundGrey
import com.jht.switchStride.utils.buttonYellow
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
internal fun ReverseBeltDialogContent(
    isBeltInReverse: Boolean,
    onCancel: () -> Unit,
    onReverse: () -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 20.dp.computeSize(),
                bottom = 10.dp.computeSize(),
                start = 20.dp.computeSize(),
                end = 20.dp.computeSize()
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isBeltInReverse) {
                stringResource(R.string.reverse_to_forward_belt_tc)
            } else {
                stringResource(
                    R.string.reverse_to_backward_belt_tc
                )
            },
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp.computeSize()))

        Text(
            text = if (isBeltInReverse) {
                stringResource(R.string.forward_belt_dialog_message_pg)
            } else {
                stringResource(
                    R.string.reverse_belt_dialog_message_pg
                )
            },
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp.computeSize()))

        Image(
            modifier = Modifier,
            painter = painterResource(id = if (isBeltInReverse) R.drawable.forwardbelt else R.drawable.reversebelt),
            contentDescription = null
        )

        Spacer(Modifier.height(16.dp.computeSize()))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp.computeSize())
        ) {
            Button(
                onClick = { onCancel() },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .testTag("$testTagPrefix:Cancel:Button"),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = backgroundGrey
                ),
                shape = RoundedCornerShape(18.dp.computeSize())
            ) {
                Text(
                    text = stringResource(R.string.cancel_tc),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.weight(0.1f))

            Button(
                onClick = { onReverse() },
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .testTag("$testTagPrefix:ReverseBelt:Button"),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = buttonYellow
                ),
                shape = RoundedCornerShape(18.dp.computeSize())
            ) {
                Text(
                    text = stringResource(R.string.reverse_tc),
                    color = Color.Black,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun ReverseBeltDialogContentPreview(){
    ReverseBeltDialogContent(isBeltInReverse = false, onCancel = {}, onReverse = {}, testTagPrefix = "")
}
