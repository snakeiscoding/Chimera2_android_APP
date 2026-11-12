package com.jht.workoutsummary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class WorkoutSummaryActivity2 : RestringThemedActivity() {
    override val themedViewModel: WorkoutSummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkoutSummaryRoute(themedViewModel)
        }
    }
}
