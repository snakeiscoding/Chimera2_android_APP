package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize
import kotlinx.coroutines.launch

@Composable
internal fun ProgressLoader(
    modifier: Modifier = Modifier,
    title: String? = null,
    centerText: String = "",
    onDismissClicked: (() -> Unit)? = null,
    footer: @Composable ((modifier: Modifier) -> Unit)? = null,
) {

    val rotation = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    tween(2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        onDismissClicked?.let {
            IconButton(
                onClick = onDismissClicked,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Exit button",
                    tint = Color.White,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.align(Alignment.Center).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {


            title?.let {
                Spacer(Modifier.height(32.dp.computeSize()))

                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.testTag("ProgressLoader:PageTitle:Text")
                )

                Spacer(Modifier.height(64.dp.computeSize()))

            }

            Box {
                Icon(
                    painter = painterResource(R.drawable.progress_circle),
                    contentDescription = "",
                    tint = colorResource(R.color.selected_color),
                    modifier = Modifier
                        .rotate(rotation.value - (rotation.value % 11.25f))
                        .align(Alignment.Center)
                )

                Text(
                    text = centerText,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("ProgressLoader:CenterText:Text"),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            footer?.let {
                Spacer(Modifier.size(64.dp))

                footer(Modifier)
            }

        }
    }

}


@Composable
@Preview
private fun ProgressLoaderPreview() {
    ProgressLoader(
        title = "Loading Facility ID Profile",
        centerText = stringResource(R.string.loading_tc)
    )
}

@Composable
@Preview
private fun ProgressLoaderNoTitlePreview() {
    ProgressLoader(
        centerText = stringResource(R.string.loading_tc)
    )
}