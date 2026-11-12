package com.jht.switchStride.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

data class ButtonConfig(
    @field:DrawableRes val buttonIcon: Int,
    @field:StringRes val buttonName: Int,
    val buttonColor: Color,
    val onClick: () -> Unit = {}
)
