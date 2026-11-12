package com.jht.sprint8.sprint8workoutsummary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.sprint8.sprint8workoutsummary.ui.Sprint8WorkoutSummaryRoute
import com.jht.sprint8.sprint8workoutsummary.ui.Sprint8WorkoutSummaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

/**
 * Activity that displays the Sprint8 workout summary screen.
 **/
@AndroidEntryPoint
class Sprint8WorkoutSummaryActivity : RestringThemedActivity() {

    override val themedViewModel: Sprint8WorkoutSummaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Sprint8WorkoutSummaryRoute(themedViewModel)
        }
    }

}
