package com.jht.progression.ui.setup.assessment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize

@Composable
fun DisclaimerScreen(
    onBackPressed: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    MmSetupFrame(onBackPressed = onBackPressed) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Disclaimer Screen"),
            color = Color.Black.copy(alpha = 0.5f)
        ) {
            Disclaimer(
                onAccept = onAccept,
                onReject = onReject
            )
        }
    }
}

@Composable
private fun Disclaimer(
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(0.15f)
                .width(1000.dp.scaleSize()),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            DisclaimerHeader()
        }

        var canAccept by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .weight(0.6f)
                .width(1000.dp.scaleSize())
                .padding(bottom = 40.dp.scaleSize())
                .background(color = Color.Black),
            contentAlignment = Alignment.TopStart
        ) {
            DisclaimerBodyContainer(onScrolledToBottom = { canAccept = it }, canAccept = canAccept)
        }

        Row(
            modifier = Modifier
                .weight(0.25f)
                .width(1000.dp.scaleSize()),
            horizontalArrangement = Arrangement.Center
        ) {
            DisclaimerButtons(onAccept = onAccept, onReject = onReject, canAccept = canAccept)
        }
    }
}

@Composable
fun DisclaimerHeader() {
    Text(
        modifier = Modifier
            .padding(start = 15.dp.scaleSize(), bottom = 10.dp.scaleSize())
            .alpha(0.9f)
            .testTag("disclaimer_header_string"),
        text = stringResource(id = R.string.disclaimer_uc),
        color = Color.White,
        fontSize = 24.sp.scaleSize(),
        style = oxaniumStyle.copy(fontWeight = FontWeight.Bold),
    )
    Canvas(// draw a line like a trapezoid shape
        modifier = Modifier
            .size(width = 1000.dp.scaleSize(), height = 2.dp.scaleSize())
            .alpha(0.5f)
            .testTag("disclaimer_header_bottom_line")
    ) {
        val inset = size.width * 0.005f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - inset, size.height)
            lineTo(inset, size.height)
            close()
        }
        drawPath(
            path = path,
            color = MmColors.futuristicBlue,
            style = Fill
        )
    }
}

@Composable
fun DisclaimerBodyContainer(
    onScrolledToBottom: (Boolean) -> Unit,
    canAccept: Boolean
) {
    val scrollState = rememberScrollState()
    val hasScrolledToBottom = remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 15.dp.scaleSize())
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 15.dp.scaleSize())
                    .testTag("disclaimer_body_message"),
                text = stringResource(R.string.mm_disclaimer_pg),
                color = Color.White,
                fontSize = 16.sp.scaleSize(),
                style = oxaniumStyle.copy(fontWeight = FontWeight.Medium),
                lineHeight = 24.sp.scaleSize()
            )
            Text(
                text = stringResource(R.string.the_matrix_team_uc),
                color = Color.White,
                modifier = Modifier
                    .padding(top = 20.dp.scaleSize())
                    .alpha(0.6f)
                    .testTag("disclaimer_footer_string"),
                fontSize = 18.sp.scaleSize(),
                style = oxaniumStyle.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        // Check if scrolled to the container bottom
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }
                .collect { scrollValue ->
                    if (!hasScrolledToBottom.value && scrollValue >= scrollState.maxValue) {
                        hasScrolledToBottom.value = true
                        onScrolledToBottom(true)
                    }
                }
        }

        DisclaimerScrollbar(
            modifier = Modifier.align(if (canAccept) Alignment.BottomEnd else Alignment.TopEnd)
        )
    }
}

@Composable
fun DisclaimerScrollbar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(4.dp.scaleSize())
            .fillMaxHeight(0.5f)
            .padding(top = 2.dp.scaleSize(), bottom = 2.dp.scaleSize())
            .background(Color.Gray.copy(alpha = 0.5f))
            .testTag("disclaimer_scrollbar")
    )
}

@Composable
fun DisclaimerButtons(
    onAccept: () -> Unit,
    onReject: () -> Unit,
    canAccept: Boolean
) {
    MmThemedButton(
        text = stringResource(id = R.string.decline_uc),
        modifier = Modifier.testTag("decline_button")
    ){
        onReject()
    }

    Spacer(modifier = Modifier.width(10.dp.scaleSize()))

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MmThemedButton(
            text = stringResource(id = R.string.accept_uc),
            color = if (canAccept) MmColors.futuristicBlue else MmColors.futuristicBlue.copy(0.5f),
            enabled = canAccept,
            modifier = Modifier.testTag("accept_button")
        ) {
            if (canAccept) {
                onAccept()
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun DisclaimerPreview() {
    MaterialTheme {
        DisclaimerScreen(onBackPressed = {}, onAccept = {}, onReject = {})
    }
}