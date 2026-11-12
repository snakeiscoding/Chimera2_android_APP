package com.jht.spinbikeapps.targettraining.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Creates an Android Paint configured to render a blurred glow effect.
 *
 * @param color The color of the glow.
 * @param blurRadius The radius of the blur applied to the glow.
 * @param strokeWidth Optional stroke width. When provided, the paint is configured
 *                    to draw a stroked shape.
 */
fun glowPaint(
    paintColor: Color,
    blurRadius: Float,
    strokeWidth: Float? = null,
    alpha: Float = 0.8f
) = android.graphics.Paint().apply {
    isAntiAlias = true
    this.color = paintColor.copy(alpha = alpha).toArgb()
    maskFilter = android.graphics.BlurMaskFilter(
        blurRadius,
        android.graphics.BlurMaskFilter.Blur.NORMAL
    )
    strokeWidth?.let {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = it
    }
}