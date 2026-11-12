package com.jht.streamingapps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun StreamingAppsRoute(
    streamingAppsViewModel: StreamingAppsViewModel,
) {
    val theme = remember { streamingAppsViewModel.theme }
    val videoInputType = remember { streamingAppsViewModel.videoInput }
    val showOverlay = remember { streamingAppsViewModel.showOverlay }

    StreamingAppsScreen(
        theme = theme.value,
        videoInput = videoInputType.value,
        onTapUp = streamingAppsViewModel::onTouchTapUp,
        onTouchDown = streamingAppsViewModel::onTouchDown,
        onDrag = streamingAppsViewModel::onDrag,
        onHScroll = { streamingAppsViewModel.onTouchScroll(it, 0f) },
        onVScroll = { streamingAppsViewModel.onTouchScroll(0f, it) },
        onBackButton = streamingAppsViewModel::onBack,
        showingOverlay = showOverlay.value,
        onShowOverlay = streamingAppsViewModel::onShowOverlay,
        onCloseOverlay = streamingAppsViewModel::onCloseOverlay,
        safeRegionPx = streamingAppsViewModel.safeRegionPx.value,
        noSignal = streamingAppsViewModel.noSignal,
    )
}
