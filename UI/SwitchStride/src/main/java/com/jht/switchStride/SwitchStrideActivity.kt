package com.jht.switchStride

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.switchStride.ui.SwitchStrideRoute
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class SwitchStrideActivity : RestringThemedActivity() {
    override val themedViewModel: SwitchStrideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwitchStrideRoute(themedViewModel)
        }
    }

}
