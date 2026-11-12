package com.jht.progression.ui.widgets

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.jht.progression.R
import com.jht.ui.clickableNoEffect

@Composable
fun MmDoubleArrowsRight(
    color: Color = Color.White,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Icon(
        painter = painterResource(id = R.drawable.ic_double_arrows_right),
        contentDescription = "Double Arrow With On Press Event",
        tint = if (isPressed) color.copy(alpha = 0.5f) else color,
        modifier = modifier.clickableNoEffect(interactionSource){ onClick() }
    )
}