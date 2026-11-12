package com.jht.welcome.chameleon.overlays

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import com.jht.theme.DynamicTheme
import com.jht.widgets.FieldInputDefaults

// Vortex UI took over default styling, so we need to recreate original style

val fieldInputStyle @Composable get() = FieldInputDefaults.defaultStyle(
    labelStyle = DynamicTheme.typography.textStyle1.copy(color = DynamicTheme.colors.text1),
    textStyle = DynamicTheme.typography.textStyle2,
    supportTextStyle = DynamicTheme.typography.textStyle1,
    textFieldColors = DynamicTheme.textFieldColors,
    placeholderStyle = DynamicTheme.typography.textStyle2,
    borderStroke = null,
    shape = RectangleShape,
)
