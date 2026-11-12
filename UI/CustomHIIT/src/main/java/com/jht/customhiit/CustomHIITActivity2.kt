package com.jht.customhiit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.customhiit.ui.CustomHIITRoute
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class CustomHIITActivity2 : RestringThemedActivity() {
    override val themedViewModel: CustomHIITViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomHIITRoute(themedViewModel)
        }
    }

}
