package com.jht.outoforder

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.CountDownTimer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.theme.DynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.DrawableUtil
import com.jht.ui.computeSize
import com.jht.widgets.Blur
import com.jht.widgets.ProductDashboard
import com.jht.widgets.helpers.isLiveApplication
import kotlin.math.roundToInt


@Device15inPreviews
@Device22inPreviews
@Composable
fun PreviewOutOfOrderScreen() {
    val mockSystemModel = MockSystemModel()
    val mockWorkoutModel = MockWorkoutModel()
    val mockNavigationModel = MockNavigationModel()
    OutOfOrderScreen(0)
}


// TODO - MAKE THIS STOP THE BELT, AND NOT ALLOW WORKOUTS TO START
// currently just a page to show to people
// also need to link background to whatever is the homepage for the welcome screen
// possibly we might just want to attach this composable to the welcome screen itself?

@OptIn(ExperimentalComposeUiApi::class, ExperimentalStdlibApi::class)
@Composable
fun OutOfOrderScreen(
    errorCode: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val interactionSource2 = remember { MutableInteractionSource() }
    val isPressed2 by interactionSource2.collectIsPressedAsState()

    val interactionSource3 = remember { MutableInteractionSource() }
    val isPressed3 by interactionSource3.collectIsPressedAsState()

    val interactionSource4 = remember { MutableInteractionSource() }
    val isPressed4 by interactionSource4.collectIsPressedAsState()

    val showProductDash = remember { mutableStateOf(false) }
    val timer = object: CountDownTimer(5000, 1000) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            showProductDash.value = true
        }
    }
    if (
        isPressed && isPressed2 ||
        isPressed && isPressed3 ||
        isPressed && isPressed4 ||
        isPressed2 && isPressed3 ||
        isPressed2 && isPressed4 ||
        isPressed3 && isPressed4
    ) {
        timer.start()
    } else {
        timer.cancel()
    }

    // UI Region start

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // blur
        if (!isLiveApplication()) {
            // this blur only works on android 12 or higher (and in previews)
            Image(
                painter = painterResource(R.drawable.new_welcome_background),
                contentDescription = "background",
                modifier = Modifier
                    .blur(25.dp)
                    .fillMaxSize())
        } else {
            val blurCache = remember{HashMap<Uri, Bitmap>()}
            with (LocalDensity.current) {
                val screenWidth = maxWidth.toPx().roundToInt()
                val screenHeight = maxHeight.toPx().roundToInt()
                Blur(
                    backgroundUri = DrawableUtil.getUri(
                        LocalContext.current,
                        R.drawable.new_welcome_background
                    ),
                    rect = Rect(0, 0, screenWidth, screenHeight),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    backgroundBlurCache = blurCache
                )
            }
        }

        // gradient
        Box(modifier = Modifier.fillMaxSize().background(DynamicTheme.colors.gradient8.vertical()))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning_circle),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(5.dp)
                    .testTag("warning_icon"),
                tint = Color.Red
            )
            Text(
                modifier = Modifier
                    .padding(5.dp),
                text = stringResource(R.string.out_of_order_tc),
                fontSize = 40.sp.computeSize(),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .padding(5.dp),
                text = stringResource(R.string.out_of_order_message_pg),
                fontSize = 40.sp.computeSize(),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Center
            )
        }


        val buttonSize = .2f

        Button(
            onClick = { /*TODO*/ },
            interactionSource = interactionSource,
            modifier = Modifier
                .align(Alignment.TopStart)
                .testTag("HiddenBtnTopLeft")
                .fillMaxSize(buttonSize)
                .alpha(0f) // hides the button
                .border(BorderStroke(2.dp, Color.Green))
        ) {}

        Button(
            onClick = { /*TODO*/ },
            interactionSource = interactionSource2,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("HiddenBtnTopRight")
                .fillMaxSize(buttonSize)
                .alpha(0f) // hides the button
                .border(BorderStroke(2.dp, Color.Red))
        ) {}

        Button(
            onClick = { /*TODO*/ },
            interactionSource = interactionSource3,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .testTag("HiddenBtnBottomLeft")
                .fillMaxSize(buttonSize)
                .alpha(0f) // hides the button
                .border(BorderStroke(2.dp, Color.Red))
        ) {}

        Button(
            onClick = { /*TODO*/ },
            interactionSource = interactionSource4,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("HiddenBtnBottomRight")
                .fillMaxSize(buttonSize)
                .alpha(0f) // hides the button
                .border(BorderStroke(2.dp, Color.Red))
        ) {}

        ProductDashboard(showProductDash)
    }
}