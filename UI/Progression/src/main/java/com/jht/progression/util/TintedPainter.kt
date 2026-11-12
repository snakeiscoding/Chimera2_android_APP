package com.jht.progression.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

/**
 * Creates a Painter which applies a tint to a resource
 *
 * @param resId The drawable resource
 * @param tintColor The tint color to be applied
 * @return a Painter of the drawable with the tint applied
 */
@Composable
fun createTintedPainter(
    @DrawableRes resId: Int,
    tintColor: Color
): Painter {
    val basePainter = painterResource(id = resId)
    return remember(resId, tintColor) {
        object : Painter() {
            override val intrinsicSize = basePainter.intrinsicSize

            override fun DrawScope.onDraw() {
                with(basePainter) {
                    draw(size = size, colorFilter = ColorFilter.tint(tintColor))
                }
            }
        }
    }
}
