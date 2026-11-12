package com.jht.switchStride.ui

import SwitchStrideScreen
import androidx.compose.runtime.Composable
import com.jht.switchStride.SwitchStrideViewModel

@Composable
internal fun SwitchStrideRoute(
    switchStrideViewModel: SwitchStrideViewModel,
) {
    SwitchStrideScreen(switchStrideViewModel)
}


