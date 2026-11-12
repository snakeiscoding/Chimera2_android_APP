package com.jht.workoutselection.chameleon.parts


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jht.theme.DynamicTheme
import com.jht.widgets.helpers.TabItem
import com.jht.workoutselection.R
import com.jht.workoutselection.TabAppsKey
import com.jht.workoutselection.common.parts.TabBase

// Pass any data needed through the create function to feed into composable?
@Composable
internal fun createTabApps(): TabItem {
    return TabItem(
        { selected, onSelect ->
            TabBase(R.drawable.ic_setup_apps, stringResource(R.string.apps_tc), selected, "tab$TabAppsKey", onSelect)
        },
        {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "APPS BODY",
                    Modifier.align(Alignment.Center),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle2
                )
            }
        },
        TabAppsKey
    )
}


