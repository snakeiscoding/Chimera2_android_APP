package com.jht.outoforder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class OutOfOrderActivity : RestringThemedActivity() {

    override val themedViewModel: OutOfOrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OutOfOrderRoute(themedViewModel)
        }
    }

}
