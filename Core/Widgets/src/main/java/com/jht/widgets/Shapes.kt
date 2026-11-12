package com.jht.widgets

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/*
    Widgets and things for shapes.
    Also look at androidx.graphics graphics-shapes-android for more shape options. https://developer.android.com/develop/ui/compose/graphics/draw/shapes
 */

/** Converts a string of SVG path data to compose path */
fun String?.toPath(size: Size?, pathDestination: Path? = null): Path? {
    // https://medium.com/@taharjeridi4/android-svg-path-to-jetpack-compose-shape-2eba4cb0339e
    this?.let {
        size?.let {
            if (!TextUtils.isEmpty(this)) {
                val pathDestinationResult = pathDestination ?: kotlin.run { Path() }
                val scaleMatrix = Matrix()
                val rectF = RectF()
                val path = PathParser().parsePathString(this).toPath(pathDestinationResult)
                val bounds = path.getBounds()
                val rectPath = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
                val scaleXFactor = size.width / rectPath.width().toFloat()
                val scaleYFactor = size.height / rectPath.height().toFloat()
                val androidPath = path.asAndroidPath()
                scaleMatrix.setScale(scaleXFactor, scaleYFactor, rectF.centerX(), rectF.centerY())
                androidPath.computeBounds(rectF, true)
                androidPath.transform(scaleMatrix)
                return androidPath.asComposePath()
            }
        }
    }
    return null
}

/** Creates a compose shape based on SVG path string */
class ShapePath(private val pathData: String): Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = Outline.Generic(
        Path().apply {
            reset()
            pathData.toPath(size, this)
            close()
        }
    )
}
