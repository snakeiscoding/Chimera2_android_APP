package com.jht.widgets.helpers

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import com.jht.theme.DynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
@Device15inPreviews
private fun Preview() {
    LoadingWorkoutAnimation(Modifier.fillMaxSize().background(DynamicTheme.colors.gray2), true) {
        GlideAndPreviewImage(modifier = it, uri = Uri.EMPTY)
    }
}

/**
 * Composable to wrap any data that shows on screen that will change when loading a workout.
 * The way we load workouts involves a callback to get the workout inputs, and there is a small
 * delay that results in some pop-in of text and pictures.  This is an attempt to solve that pop-in.
 * We show the loading animation over the top of the normal content for an extra .5 seconds after
 * the workout has finished loading, giving that extra bit of time for the newly gathered resources
 * to populate through recomposition.
 */
@Composable
fun LoadingWorkoutAnimation(
    modifier: Modifier,
    isLoadingWorkout: Boolean,
    content: @Composable (modifier: Modifier) -> Unit
){
    // default as true, so that the loading screen shows first.
    // If nothing to load we only delay .5 seconds before the user is shown the content,
    // which is acceptable in my opinion
    val isPreview = LocalInspectionMode.current // don't show loading animation in previews
    val showLoadingAnimation = remember { mutableStateOf(!isPreview) }

    LaunchedEffect(key1 = isLoadingWorkout) {
        if (isLoadingWorkout) {
            showLoadingAnimation.value = true
        } else {
            delay(.3.seconds)
            showLoadingAnimation.value = false
        }
    }
    // show loading animation, over the top of the content so that when the workout is done loading
    // we can delay lifting the loading veil to get rid of any pop in when changing text/pictures
    if(showLoadingAnimation.value) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            Image(modifier = Modifier, painter = painterResource(id = R.drawable.ic_logo_matrix), contentDescription = "loading_logo")
            CircularProgressIndicator(modifier = Modifier, color = DynamicTheme.colors.primary)
        }
    } else {
        // so that we don't get any errors while waiting for the workout to load
        if (!isLoadingWorkout) {
            content(modifier)
        }
    }
}