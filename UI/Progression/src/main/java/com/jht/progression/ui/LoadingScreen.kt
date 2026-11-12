package com.jht.progression.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jht.ui.AllDevicePreviews

@Composable
fun LoadingScreen() {
    val strokeWidth = 5.dp

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .drawBehind {
                    drawCircle(
                        color = Color.DarkGray,
                        radius = size.width / 2 - strokeWidth.toPx() / 2,
                        style = Stroke(strokeWidth.toPx())
                    )
                },
            color = Color.LightGray,
            strokeWidth = strokeWidth
        )
    }
}

@AllDevicePreviews
@Composable
private fun MmLoadingScreenPreview(){
    LoadingScreen()
}