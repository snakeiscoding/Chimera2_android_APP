package com.jht.appstore.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class AppStoreActivity : RestringThemedActivity() {
    override val themedViewModel: AppStoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If we don't handle this then the activity will exit and go back to unwanted screen.
        overrideBackButton()

        setContent {
            AppStoreRoute(themedViewModel)
        }
    }

}

