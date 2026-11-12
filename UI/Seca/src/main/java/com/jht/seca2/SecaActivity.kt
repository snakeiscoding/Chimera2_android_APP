package com.jht.seca2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.jht.engage360.data.ColorBrand
import com.jht.engage360.theme.Engage360Theme
import com.jht.engage360.theme.Engage360ThemeProvider
import com.jht.seca2.ui.SecaNav
import com.jht.theme.DynamicTheme
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

private const val TAG = "SecaActivity"

/**
 * Activity for seca survey
 */
@AndroidEntryPoint
class SecaActivity : RestringThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val secaSummaryViewModel: SecaViewModel = viewModel()
            val navController = rememberNavController()
            DynamicTheme(paceUiTheme) {
                SecaNav(
                    navController = navController,
                    secaViewModel = secaSummaryViewModel,
                    onExit = { secaSummaryViewModel.navigateToSplash() },
                )
            }
        }
    }
}
