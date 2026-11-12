package com.jht.devicepairing

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

@AndroidEntryPoint
class DevicePairingActivity: RestringThemedActivity() {
    override val themedViewModel: DevicePairingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DevicePairingRoute(themedViewModel)
        }
    }

}
