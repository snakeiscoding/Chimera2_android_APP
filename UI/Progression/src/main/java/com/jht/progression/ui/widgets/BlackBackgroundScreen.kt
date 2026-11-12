package com.jht.progression.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * This is a full size of a black background screen using for the transition between two screens
 * */
@Composable
fun BlackBackgroundScreen(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}