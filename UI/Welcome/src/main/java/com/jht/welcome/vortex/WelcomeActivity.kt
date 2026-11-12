package com.jht.welcome.vortex

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.jht.data.UserFieldFunction
import com.jht.welcome.WelcomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class WelcomeActivity : RestringThemedActivity() {
    override val themedViewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WelcomeRoute(themedViewModel)
        }
    }

    // do we need to check intents for initial login type and RFID to associate?

}
