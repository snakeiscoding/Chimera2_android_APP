package com.jht.simpleapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class SimpleRunActivity2 : RestringThemedActivity() {
    override val themedViewModel: SimpleAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleAppRoute(themedViewModel)
        }
    }

}
