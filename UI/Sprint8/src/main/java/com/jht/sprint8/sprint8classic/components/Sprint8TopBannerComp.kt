package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.Sprint8ViewModel
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.HomeButton
import com.jht.widgets.fillScreenHeight

/**
 * Displays the top banner for the Sprint8 screen, optionally showing an info icon.
 * @param modifier Modifier applied to the banner for layout, styling, and interaction.
 * @param isInfoIconVisible Boolean indicating whether the info icon should be visible. Defaults to true.
 * @param navController Optional [NavController] for handling navigation events.
 * @param sprint8ViewModel The [Sprint8ViewModel] providing data and state for the banner. Defaults to a Hilt-injected ViewModel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@AllDevicePreviews
@Composable
internal fun Sprint8TopBannerComp(
    modifier: Modifier = Modifier,
    isInfoIconVisible: Boolean = true,
    navController: NavController? = null,
    sprint8ViewModel: Sprint8ViewModel = hiltViewModel()
) {
    val componentTag = "Sprint8TopBannerComp"
    Box(
        Modifier
            .fillMaxWidth()
            .fillScreenHeight(0.1f)
            .background(DynamicTheme.colors.panelTopBackground.brush())
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        println("Tapped outside")
                    }
                )
            }
            .semantics {
                testTagsAsResourceId = true
            },
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(30.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeButton(Modifier.aspectRatio(120 / 70f)) {
                sprint8ViewModel.onHome()
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_sprint_8_top_banner),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .testTag("$componentTag:TopBannerImageSprint8:Image")
        )
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(30.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isInfoIconVisible) {
                Sprint8TopBannerInfoButton(
                    Modifier.aspectRatio(120 / 70f),
                    onClick = { navController?.navigate(Screen.Sprint8InformationScreen.route) },
                    isEnabled = navController?.currentDestination?.route != Screen.Sprint8InformationScreen.route
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            thickness = 1.dp.computeSize(),
            color = Color.DarkGray
        )
    }
}