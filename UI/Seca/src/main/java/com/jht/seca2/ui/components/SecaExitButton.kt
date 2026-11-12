@file:OptIn(ExperimentalComposeUiApi::class)

package com.jht.seca2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.seca.R
import com.jht.seca2.ui.SecaColors
import com.jht.ui.AllSecaDevicePreviews

@Composable
internal fun SecaExitButton(modifier: Modifier = Modifier,
                   isPressedOverride: Boolean = false, //probably just for previews
                   onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = animateColorAsState(targetValue = if (isPressed || isPressedOverride) SecaColors.primaryRed else SecaColors.unpressedButton)
    val borderColor = animateColorAsState(targetValue = if (isPressed || isPressedOverride) SecaColors.primaryRed else SecaColors.unpressedButtonBorder)
    val cornerRadius = 10.dp

    Button(
        modifier = modifier
            .size(60.dp)
            .border(width = 2.dp, color = borderColor.value, shape = RoundedCornerShape(cornerRadius))
            .testTag("seca:ExitButton:Button"),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp), //remove default padding from buttons
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor.value),
        shape = RoundedCornerShape(cornerRadius),
    ){
        Image(
            modifier = Modifier
                .size(25.dp)
                .testTag("seca:ExitButton:Image"),
            contentScale = ContentScale.FillBounds,
            painter = painterResource(if (isPressed || isPressedOverride) R.drawable.ic_door_back_pressed else R.drawable.ic_door_back),
            contentDescription = "Exit button door icon"
        )
    }
}

//region PREVIEWS
@AllSecaDevicePreviews
@Composable
private fun SecaExitButtonPreview(){
    SecaExitButton {  }
}

@AllSecaDevicePreviews
@Composable
private fun SecaExitButtonPressedPreview(){
    SecaExitButton(isPressedOverride = true) {  }
}