package com.jht.widgets.helpers

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/** Provides a version of [stringResource] that returns a given string if resource is not found */
@Composable
fun stringResourceDefault(@StringRes id: Int, default: String = ""): String {
    if (id == 0) return default // noticed previews are showing a string for zero which should be "Resources.ID_NULL" ?
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return try {
        resources.getString(id)
    } catch (_:Exception) {
        default
    }
}
