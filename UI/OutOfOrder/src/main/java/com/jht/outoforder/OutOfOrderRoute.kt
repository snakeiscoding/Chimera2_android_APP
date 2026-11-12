package com.jht.outoforder

import androidx.compose.runtime.Composable

@Composable
fun OutOfOrderRoute(
    oooViewModel: OutOfOrderViewModel,
) {
    OutOfOrderScreen(oooViewModel.errorCode.value)
}