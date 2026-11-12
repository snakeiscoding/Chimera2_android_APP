package com.jht.switchStride.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.switchStride.components.ReverseBeltDialogContent
import com.jht.switchStride.utils.backgroundBlack
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Reverse Belt and Belt stopping dialog UI.
 *
 * Variables:
 * - isBeltInReverse: Indicates current belt direction (used for UI state only).
 *
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ReverseBeltScreen(
    isBeltInReverse: Boolean,
    onCancel: () -> Unit,
    onReverse: () -> Unit
) {
    val testTagPrefix = "SwitchStride"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .semantics { testTagsAsResourceId = true },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .offset(y = (-30).dp.computeSize())
                .clip(RoundedCornerShape(16.dp.computeSize()))
                .background(backgroundBlack)
                .padding(20.dp.computeSize())
        ) {
            ReverseBeltDialogContent(
                isBeltInReverse,
                onCancel = onCancel,
                onReverse = onReverse,
                testTagPrefix = testTagPrefix
            )
        }
    }
}


@AllDevicePreviews
@Composable
private fun ReverseBeltScreenPreview() {
    ReverseBeltScreen(onCancel = {}, onReverse = {}, isBeltInReverse = true)
}