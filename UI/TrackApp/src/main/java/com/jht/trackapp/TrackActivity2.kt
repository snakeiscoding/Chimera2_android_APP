package com.jht.trackapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.trackapp.ui.TrackAppRoute
import com.jht.widgets.RestringThemedActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackActivity2 : RestringThemedActivity() {
    override val themedViewModel: TrackAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Only a fresh launch carries a new mode. On a recreation (config change or process
        // death) the view model's saved state is newer than getIntent(), which is still the
        // original launch intent, so it must not be re-applied here.
        if (savedInstanceState == null) {
            themedViewModel.onLaunchIntent(intent)
        }

        setContent {
            TrackAppRoute(themedViewModel)
        }
    }

    // This activity is singleTask, so a re-launch reuses this instance and its view model. The
    // mode has to be re-applied from the new intent, otherwise the previous launch's mode sticks.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        themedViewModel.onLaunchIntent(intent)
    }

}
