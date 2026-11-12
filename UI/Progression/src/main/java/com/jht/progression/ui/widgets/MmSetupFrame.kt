package com.jht.progression.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.progression.R
import com.jht.progression.ui.MmColors
import com.jht.ui.clickableNoEffect
import com.jht.widgets.scaleSize

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MmSetupFrame(
    topBarColor: Color = MmColors.deepBlue,
    backgroundId: Int = R.drawable.mm_squares_bg,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    onBackPressed: () -> Unit,
    debounceTimeMs: Int = 1000,
    content: @Composable BoxScope.() -> Unit
) {
    MmBackground(
        modifier = Modifier.fillMaxSize(),
        backgroundId = backgroundId
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        MmTopBar(
            modifier = Modifier.height(78.dp.scaleSize()),
            backgroundColor = topBarColor,
            backPressed = onBackPressed,
            backIcon = backIcon,
            debounceTimeMs = debounceTimeMs
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun MmTopBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    backPressed: () -> Unit,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    debounceTimeMs: Int
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        Icon(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .aspectRatio(1f)
                .clickableNoEffect(
                    interactionSource = interactionSource,
                    debounceTimeMs = debounceTimeMs
                ) { backPressed() }
                .padding(all = 20.dp.scaleSize())
                .testTag(if (backIcon == Icons.AutoMirrored.Filled.ArrowBack) "top_bar_back_button" else "top_bar_home_button"),
            imageVector = backIcon,
            contentDescription = "Back",
            tint = if (isPressed) Color.White.copy(alpha = 0.5f) else Color.White
        )

        Image(
            painter = painterResource(id = R.drawable.ic_method_logo),
            contentDescription = "Matrix Method screen header ",
            modifier = Modifier
                .size(206.dp.scaleSize(), 30.dp.scaleSize())
                .align(Alignment.Center)
                .testTag("method_logo"),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(color = Color.White)
        )
    }
}

@Composable
private fun MmBackground(modifier: Modifier = Modifier, backgroundId: Int) {
    Image(
        modifier = modifier.fillMaxSize(),
        painter = painterResource(id = backgroundId),
        contentDescription = "background",
        contentScale = ContentScale.FillBounds
    )
}