package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize
import com.jht.widgets.ScrollHintOverlay

/**
 * Scrollable body for an FTSU info card, with [ScrollHintOverlay] fades and chevron so overflowing
 * content (e.g. on 1366x768 consoles) is discoverable.
 *
 * [contentPadding] sits inside the scroll so content scrolls under the fade to the card edge.
 * [scrimColor] should match the card's container color so the fade blends in.
 */
@Composable
internal fun ScrollableCardContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp.computeSize()),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrimColor: Color = colorResource(R.color.language_card_color),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    ScrollHintOverlay(
        state = scrollState,
        modifier = modifier,
        scrimColor = scrimColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
