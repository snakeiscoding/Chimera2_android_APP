package com.jht.spinbikeapps.targettraining.ui

import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.ui.popup.ConfirmationPopUp
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.ORANGE_1
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.DialogFullscreen

@Composable
internal fun CaloriesScreen(
    modifier: Modifier = Modifier,
    calories: Float,
    targetCalories: Float? = null,
    activeMode: WorkoutMode,
    onClickEndWorkout: () -> Unit = {},
    onClickContinue: () -> Unit = {},
    isPopUpVisible: Boolean = false,
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var videoView: VideoView? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoView = this
                    val uri = "android.resource://${context.packageName}/${R.raw.ic_fire}".toUri()
                    setVideoURI(uri)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        true
                    }
                }
            },
            modifier = Modifier
                .size(700.dp.computeSizeHd())
                .align(Alignment.Center)
        )
        if (targetCalories != null && activeMode == WorkoutMode.Calories) {
            TargetTrainingProgressBar(
                progress = targetCalories.takeIf { it > 0f }?.let {
                    (calories / it).coerceIn(0f, 1f)
                } ?: 0f,
                progressColors = listOf(
                    ORANGE_1.copy(alpha = 0.7f),
                    ORANGE_1
                ),
                trackColor = ORANGE_1.copy(alpha = 0.2f),
                trailingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_calories_crosshair),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingLabel = {
                    Row(
                        modifier = Modifier.offset(x = 15.dp.computeSize()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = targetCalories.toInt().toString(),
                            color = ORANGE_1,
                            fontSize = 20.sp.computeSize(),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            modifier = Modifier.align(Alignment.Bottom),
                            text = " " + stringResource(R.string.kcal_lc),
                            color = ORANGE_1.copy(alpha = 0.6f),
                            fontSize = 16.sp.computeSize(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
        Column(
            modifier = modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp.computeSizeHd()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                calories.toInt().toString(),
                fontSize = 160.sp.computeSize(1902f),
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                stringResource(R.string.calories_tc),
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 50.sp.computeSize(1902f),
                textAlign = TextAlign.Center,
            )
        }
        if (isPopUpVisible) {
            DialogFullscreen(onDismissRequest = {}) {
                ConfirmationPopUp(
                    onClickEndWorkout = onClickEndWorkout,
                    onClickContinue = onClickContinue,
                    modifier = modifier.align(Alignment.Center)
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {

                Lifecycle.Event.ON_RESUME -> {
                    videoView?.start()
                }


                Lifecycle.Event.ON_PAUSE -> {
                    videoView?.pause()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    videoView?.stopPlayback()
                }


                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoView?.stopPlayback()
        }
    }
}

@AllDevicePreviews
@Composable
private fun CaloriesScreenPreview(){
    CaloriesScreen(
        modifier = Modifier,
        calories  = 10f,
        targetCalories = 100f,
        activeMode = WorkoutMode.Calories
    )
}