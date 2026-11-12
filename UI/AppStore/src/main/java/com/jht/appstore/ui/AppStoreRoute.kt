package com.jht.appstore.ui

import androidx.compose.runtime.Composable

@Composable
fun AppStoreRoute(
    appStoreViewModel: AppStoreViewModel,
) {
    AppStoreScreen(
        viewModel = appStoreViewModel
    )
}
