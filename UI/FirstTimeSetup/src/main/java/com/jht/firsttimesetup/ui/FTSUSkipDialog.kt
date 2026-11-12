package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.ui.computeSize


@Preview
@Composable
private fun SkipFtsuViewPreview() {
    SkipFtsuView()
}

/**
 * Dialog view that allows users to skip FTSU
 * @param modifier
 * @param [onCancelClicked] -> Action to be performed on cancel clicked
 * @param [onConfirmClicked] -> Action to be performed on confirm clicked
 */
@Composable
fun SkipFtsuView(
    modifier: Modifier = Modifier,
    onCancelClicked: () -> Unit = {},
    onConfirmClicked: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(IntrinsicSize.Max),
        colors = CardDefaults.cardColors().copy(containerColor = colorResource(R.color.grey_5))
    ) {

        Column(
            modifier = Modifier
                .padding(64.dp)
                .fillMaxSize(),
        ) {
            Text(
                text = stringResource(R.string.skip_ftsu_tc),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("FTSU:Skip:Header:Text"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.size(16.dp.computeSize()))

            Text(
                text = stringResource(R.string.skip_ftsu_pg),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("FTSU:Skip:Body:Text"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge

            )

            Spacer(Modifier.size(94.dp.computeSize()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceAround
            ) {

                FTSUButton(
                    label = stringResource(R.string.cancel_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = onCancelClicked,
                    modifier = Modifier
                        .defaultMinSize(200.dp.computeSize())
                        .testTag("FTSU:Skip:Cancel:Button")
                )

                FTSUButton(
                    label = stringResource(R.string.confirm_tc),
                    onClick = onConfirmClicked,
                    modifier = Modifier
                        .defaultMinSize(200.dp.computeSize())
                        .testTag("FTSU:Skip:Confirm:Button")
                )
            }
        }

    }
}