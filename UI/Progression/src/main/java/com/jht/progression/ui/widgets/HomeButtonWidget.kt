package com.jht.progression.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jht.progression.R
import com.jht.ui.clickableNoEffect
import com.jht.widgets.scaleSize

@Composable
fun HomeButtonWidget(onHomeButtonPressed: () -> Unit, debounceTimeMs: Int = 1000) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val interactionSource: MutableInteractionSource =
            remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        Image(
            painter = painterResource(id = R.drawable.progression_home_icon),
            contentDescription = "Home Button",
            modifier = Modifier
                .clickableNoEffect(
                    interactionSource = interactionSource,
                    debounceTimeMs = debounceTimeMs
                ) { onHomeButtonPressed() }
                .alpha(alpha = if (isPressed) 0.5f else 1.0f)
                .padding(start = 20.dp.scaleSize())
                .size(30.dp.scaleSize())
                .testTag("home_button"),
            contentScale = ContentScale.FillBounds
        )
    }
}