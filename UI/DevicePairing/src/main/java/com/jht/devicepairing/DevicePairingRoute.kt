package com.jht.devicepairing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


@Composable
fun DevicePairingRoute(
    devicePairingViewModel: DevicePairingViewModel,
) {
    val theme = remember { devicePairingViewModel.theme }

    DevicePairingScreen(
        theme = theme.value,
        machineName = devicePairingViewModel.machineName,
        categories = devicePairingViewModel.categories,
        selectedCategory = devicePairingViewModel.selectedCategory.value,
        onSelectCategory = devicePairingViewModel::onSelect,
        pairingTimedOut = devicePairingViewModel.pairingTimedOut,
        safeRegionPx = devicePairingViewModel.safeRegionPx.value,
        onBack = devicePairingViewModel::onBack,
    )
}
