package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize

/**
 * Generic error view
 */
@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    errorIcon: @Composable () -> Unit = {
        Icon(
            modifier = Modifier.fillMaxSize(.35f),
            painter = painterResource(R.drawable.x_circle),
            contentDescription = "error icon",
            tint = Color.White
        )
    },
    errorMessage: String = stringResource(R.string.error_tc),
    actionButton: @Composable () -> Unit = {}
) {

    Box(modifier = modifier) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.size(52.dp.computeSize()))

            errorIcon()

            Spacer(Modifier.size(52.dp.computeSize()))

            Text(
               errorMessage,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.size(64.dp.computeSize()))

            actionButton()

        }
    }

}