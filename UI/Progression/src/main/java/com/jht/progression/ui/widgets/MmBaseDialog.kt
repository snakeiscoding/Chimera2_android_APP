package com.jht.progression.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.progression.R
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.dpToPx
import com.jht.widgets.scaleSize


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MmBaseDialog(
    open: Boolean,
    width: Dp = 800.dp.scaleSize(),
    height: Dp = 438.dp.scaleSize(),
    dismissOnOutsideTap: Boolean = true,
    onDismiss: () -> Unit,
    content: (@Composable () -> Unit)
) {
    if (!open) return
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2f)
            .background(Color(0xCC000000))
            .clickableNoEffect { if (dismissOnOutsideTap) onDismiss() }
            .semantics { testTagsAsResourceId = true },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dialog_box"),
                painter = painterResource(id = R.drawable.mm_dialog_bg),
                contentDescription = "dialog background",
                contentScale = ContentScale.FillBounds
            )

            // Ensure inner content does not escape bounds of dialog
            SlicedCornerBox (
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoEffect(enabled = false) {}
                    .padding(
                        start = 12.dp.scaleSize(),
                        top = 20.dp.scaleSize(),
                        end = 12.dp.scaleSize(),
                        bottom = 3.dp.scaleSize()
                    )
            ) { content() }
        }
    }
}


@Composable
private fun SlicedCornerBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val slicedCornerPadding = 43.dp.scaleSize().dpToPx()
    val slicedCornerShape = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height - slicedCornerPadding)
        lineTo(size.width - slicedCornerPadding, size.height)
        lineTo(0f, size.height)
        close()
    }
    Box(modifier = Modifier
        .clip(slicedCornerShape)
        .then(modifier)
    ) { content() }
}


@AllDevicePreviews
@Composable
private fun Preview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        MmBaseDialog(
            open = true,
            onDismiss = {},
        ) {}
    }
}