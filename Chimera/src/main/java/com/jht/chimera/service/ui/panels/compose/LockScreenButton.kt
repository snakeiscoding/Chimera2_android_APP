package com.jht.chimera.service.ui.panels.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.PopupProperties
import com.jht.chimera.R
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.disabledBackground
import com.jht.theme.themeButtonBorder
import com.jht.widgets.PopupFullscreen

/*
    NOTE: It is hard to get the button location in absolute screen coordinates, so if this is in a panel that is not left most you may need to pass panel screen offset?
    Maybe we can find a clever way around that one day?
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LockScreenButton(
    modifier: Modifier,
    enabled: Boolean = true,
    rootOffset: Offset = Offset.Zero, // composable can only track its position within root composable, provide a means to pass offset of root
    border: BorderStroke? = themeButtonBorder,
) {
    val locked = remember { mutableStateOf(false) }
    val positionOfButton = remember { mutableStateOf(DpOffset.Zero) }
    val sizeOfButton = remember { mutableStateOf(DpSize.Zero) }

    with(LocalDensity.current) {
        LockButton(
            onClick = { locked.value = true },
            enabled = enabled,
            border = border,
            modifier = Modifier
                .testTag(if (locked.value) "" else "LockScreenButton")
                .then(modifier)
                .onGloballyPositioned { coordinates ->
                    val sizePixels = coordinates.size
                    sizeOfButton.value = DpSize(sizePixels.width.toDp(), sizePixels.height.toDp())
                    val offsetPixels = coordinates.positionInRoot() + rootOffset
                    positionOfButton.value = DpOffset(offsetPixels.x.toDp(), offsetPixels.y.toDp())
                }
        )
    }

    if (locked.value) PopupFullscreen(
        //onDismissRequest = {},
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        Box(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .background(disabledBackground)
                .fillMaxSize()
        ) {
            //Popup(offset = IntOffset(positionOfButton.value.x.toInt(), positionOfButton.value.y.toInt())) // when window type was lower, needed a popup otherwise it was under panel
            LockButton(
                onClick = { locked.value = false },
                modifier = Modifier
                    .testTag(if (locked.value) "LockScreenButton" else "")
                    .absoluteOffset(positionOfButton.value.x, positionOfButton.value.y)
                    .size(sizeOfButton.value),
                isLocked = true,
                border = border,
            )
        }
    }
}

@Composable
private fun LockButton(
    onClick: ()-> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    isLocked: Boolean = false,
    border: BorderStroke? = themeButtonBorder,
) {
    ThemeButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        modifier = modifier,
        border = border,
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(.5f),
            painter = painterResource(id = R.drawable.ic_lock_screen),
            contentDescription = null,
            tint = if (isLocked) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
        )
    }
}
