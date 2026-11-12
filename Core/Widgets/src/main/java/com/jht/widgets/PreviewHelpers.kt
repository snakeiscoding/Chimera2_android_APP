package com.jht.widgets

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

class PreviewHelpers {
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GlideAndPreviewImage(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.None,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    if (uri != Uri.EMPTY) {
        GlideImage(
            model = uri,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    } else { // in case image on sd card is corrupted or doesn't load, this preserves the image + text sizing
        Image(
            painter = ColorPainter(Color.Transparent), // should placeHolderColor be a parameter? any reason to not be transparent other than debugging?
            contentDescription = null,
            modifier = modifier,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }
}