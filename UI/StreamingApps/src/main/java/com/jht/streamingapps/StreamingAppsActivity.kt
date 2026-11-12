package com.jht.streamingapps

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class StreamingAppsActivity : RestringThemedActivity() {
    override val themedViewModel: StreamingAppsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture the android BACK button
        overrideBackButton(themedViewModel::onBack)

        setContent {
            StreamingAppsRoute(themedViewModel)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        themedViewModel.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // do we need to do onConfigurationChanged as well?
        try {
            val locale = getResources().configuration.locales.get(0)
            themedViewModel.onLocaleChange(locale.toLanguageTag())
        } catch (e: Exception) {
            Log.e("StreamingAppsActivity", "Failed to retrieve locale", e)
        }
    }

}
