package com.jht.workoutselection.vortex.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jht.concierge.ui.ConciergeButton
import com.jht.theme.DynamicTheme
import com.jht.widgets.BrandLogo
import com.jht.widgets.DebugIndicator
import com.jht.widgets.HomeButton
import com.jht.widgets.IndicatorBank
import com.jht.widgets.LanguageButton
import com.jht.widgets.PTPButton
import com.jht.widgets.PaceLogoButton
import com.jht.widgets.SettingsButton
import com.jht.widgets.SmartWatchBank
import com.jht.workoutselection.common.parts.UserButtonWithMenu

/**
 * Top bar that handles home, Engage360, seca, login, and settings buttons
 * If [isHomeScreen] the home button will be replaced with the language button
 */
@Composable
internal fun WorkoutSelectionTopBar(
    onHome: () -> Unit = {},
    showE360Button: Boolean = false,
    showPaceButton: Boolean = false,
    onPTP: () -> Unit = {},
    onUserButton: ()->Unit,
    isHomeScreen: Boolean,
    onLanguage: () -> Unit = {},
    showProductDashboard: MutableState<Boolean>,
    onShowDebugDialog: ()->Unit = {},
) {
    Column {
        Row(
            Modifier
                .background(DynamicTheme.colors.panelTopBackground.brush())
                .fillMaxWidth()
                .fillMaxHeight(0.09f)
                .padding(horizontal = 20.dp, vertical = 10.dp), // comp looks like it has more horz pad than vert pad?
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val buttonAspectRatio = 120 / 70f

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                //Home button and language are mutually exclusive depending on if it is the home screen
                if(isHomeScreen){
                    LanguageButton(Modifier.aspectRatio(buttonAspectRatio), onLanguage)
                }
                else{
                    HomeButton(Modifier.aspectRatio(buttonAspectRatio), onHome)
                }

                // Show only one button based on priority: SECA first, then PTP
                when {
                    showPaceButton -> {
                        PaceLogoButton(Modifier.aspectRatio(buttonAspectRatio), onPTP)
                    }
                    showE360Button -> {
                        PTPButton(Modifier.aspectRatio(buttonAspectRatio), onPTP)
                    }
                }

                //Concierge only shows
                ConciergeButton()

                DebugIndicator() // using pad because we don't want extra spacer if indicator is not showing
            }

            // WATCH BADGES and LOGO
            SmartWatchBank(
                modifier = Modifier
                    .fillMaxHeight(),
                badgeModifier = Modifier
                    .defaultMinSize(minHeight = 40.dp) // GymKit specs require 40px minimum
                    .fillMaxHeight(.5f),
                contentSpacing = 20.dp
            ) {
                BrandLogo(
                    modifier = Modifier.fillMaxWidth(0.23f),
                    clickModifier = Modifier.fillMaxHeight(),
                    onLongPress = { showProductDashboard.value = true },
                    onDebugFling = onShowDebugDialog,
                )
            }

            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { // room for multiple Buttons at end.

                // INDICATORS
                IndicatorBank(Modifier)

                // USER BUTTON
                UserButtonWithMenu(Modifier.fillMaxHeight(), onGuestClick = onUserButton)

                // SETTINGS BUTTON
                SettingsButton(Modifier.aspectRatio(buttonAspectRatio))
            }
        }

        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .height(1.dp),
            color = DynamicTheme.colors.gray3
        )
    }
}
