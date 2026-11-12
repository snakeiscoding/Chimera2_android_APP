package com.jht.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

// TODO This code is from Kong to test a different way of scaling screen sizes
/**
 * Helper logic to compute new size for the different screen by dividing the dp/sp by the ratio
 * of the design width by the screen width
 */
/** ------------------------------------------------------------------------------- **/
const val designWidth = 1366f

@Composable
fun Dp.scaleSize(): Dp {
    return scaleSize(designWidth)
}

@Composable
fun TextUnit.scaleSize(): TextUnit {
    return scaleSize(designWidth)
}

@Composable
fun Dp.scaleSize(designWidth: Float): Dp {
    return this / (designWidth / LocalConfiguration.current.screenWidthDp)
}

@Composable
fun TextUnit.scaleSize(designWidth: Float): TextUnit {
    return this / (designWidth / LocalConfiguration.current.screenWidthDp)
}

/** ------------------------------------------------------------------------------- **/