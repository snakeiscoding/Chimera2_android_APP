package com.jht.sprint8

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.sprint8.utils.Sprint8TypeData
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

/**
 * Activity that displays the Sprint8 workout screen.
 **/
@AndroidEntryPoint
class Sprint8Activity : RestringThemedActivity() {

    override val themedViewModel: Sprint8ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sprint8Type = Sprint8TypeData.selectedMode
            Sprint8Route(themedViewModel, sprint8Type)
        }
    }
}
