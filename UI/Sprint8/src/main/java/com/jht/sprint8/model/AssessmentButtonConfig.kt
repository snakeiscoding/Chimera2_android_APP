package com.jht.sprint8.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

/**
 * Configuration data for an assessment action button.
 * @property buttonName The text label displayed on the button.
 * @property buttonIcon The drawable resource ID for the icon displayed on the button.
 * @property buttonBackgroundColor The background color of the button.
 * @property buttonTextColor The color of the button text.
 * @property onButtonClick Lambda function invoked when the button is clicked.
 */
internal data class AssessmentButtonConfig(
    val buttonName: String,
    @DrawableRes val buttonIcon: Int,
    val buttonBackgroundColor: Color,
    val buttonTextColor: Color,
    val onButtonClick: () -> Unit
)
