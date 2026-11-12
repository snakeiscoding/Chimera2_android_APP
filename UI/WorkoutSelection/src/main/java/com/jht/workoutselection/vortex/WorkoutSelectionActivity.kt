package com.jht.workoutselection.vortex

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.workoutselection.WorkoutSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class WorkoutSelectionActivity : RestringThemedActivity() {
    override val themedViewModel: WorkoutSelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkoutSelectionRoute(themedViewModel)
        }
    }
}
