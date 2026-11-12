package com.jht.landmarks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class LandmarksActivity : RestringThemedActivity() {

    override val themedViewModel: LandmarksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LandmarksRoute(themedViewModel)
        }
    }
}
