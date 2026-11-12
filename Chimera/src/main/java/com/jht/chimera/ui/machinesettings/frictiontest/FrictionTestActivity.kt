package com.jht.chimera.ui.machinesettings.frictiontest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.jht.chimera.lib.widget.activity.RestringActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.ui.Device15inPreviews
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize

class FrictionTestActivity : RestringActivity() {
    private val viewModel: FrictionTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.frictionTestState.collectAsState()

            FrictionTestScreen(
                testState = state.testStage,
                targetSpeedKph = state.currentSpeedKph,
                targetIncline = state.currentIncline,
                instructionText = state.instructionText,
                onStartButtonPressed = { viewModel.startButtonPressed() },
                onStopButtonPressed = { viewModel.stopButtonPressed() },
                onBackButtonPressed = { finish() },
            )
        }

        viewModel.registerLifecycleObserver(this)
    }
}

@Composable
private fun FrictionTestScreen(
    testState: FrictionTestStage,
    targetSpeedKph: String,
    targetIncline: String,
    instructionText: String,
    onStartButtonPressed: () -> Unit,
    onStopButtonPressed: () -> Unit,
    onBackButtonPressed: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.background_black),
            contentScale = ContentScale.FillBounds,
            contentDescription = ""
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp.scaleSize())
                .background(Color.Black)
        ) {

            // Back button
            Image(
                modifier = Modifier
                    .size(40.dp.scaleSize())
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBackButtonPressed),
                painter = painterResource(id = R.drawable.back_button),
                contentDescription = "",
                colorFilter = ColorFilter.tint(Color.White)
            )

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Treadmill Friction Test",
                fontSize = 38.sp.scaleSize(),
                color = Color.White
            )

        }

        // Start button
        Button(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .size(width = 200.dp.scaleSize(), height = 120.dp.scaleSize())
                .padding(15.dp.scaleSize()),
            onClick = onStartButtonPressed
        ) {
            AutoSizeText(
                modifier = Modifier.fillMaxSize(),
                text = "START TEST",
                color = Color.White
            )
        }

        // Stop button
        Button(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .size(width = 200.dp.scaleSize(), height = 120.dp.scaleSize())
                .padding(15.dp.scaleSize()),
            onClick = onStopButtonPressed
        ) {
            AutoSizeText(
                modifier = Modifier.fillMaxSize(),
                text = "STOP BELT",
                color = Color.White
            )
        }

        // Speed value box
        ValueBox(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp.scaleSize()),
            value = targetSpeedKph,
            units = "Speed KPH"
        )


        // Incline value box
        ValueBox(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp.scaleSize()),
            value = targetIncline,
            units = "Incline"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 190.dp.scaleSize(),
                    end = 190.dp.scaleSize(),
                    top = 100.dp.scaleSize(),
                    bottom = 140.dp.scaleSize()
                )
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AutoSizeText(
                modifier = Modifier
                    .padding(top = 10.dp.scaleSize())
                    .fillMaxWidth()
                    .height(40.dp.scaleSize()),
                text = "Instructions",
                color = Color.Black
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight()
                    .padding(top = 15.dp.scaleSize()),
            )
            {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    text = instructionText,
                    color = Color.Black,
                    fontSize = 50.sp.scaleSize(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ValueBox(modifier: Modifier = Modifier, value: String, units: String) {
    Column(
        modifier = modifier
            .size(100.dp.scaleSize())
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 30.sp.scaleSize(),
            color = Color.Black
        )

        Text(
            modifier = Modifier.padding(top = 5.dp.scaleSize()),
            text = units,
            fontSize = 18.sp.scaleSize(),
            color = Color.Black
        )
    }
}

@Device15inPreviews
@Composable
private fun Preview() {
    FrictionTestScreen(
        testState = FrictionTestStage.NotStarted,
        targetSpeedKph = "5.0",
        targetIncline = "0.0",
        instructionText = "This is preview instruction text.",
        onStartButtonPressed = {},
        onStopButtonPressed = {},
        onBackButtonPressed = {},
    )
}


