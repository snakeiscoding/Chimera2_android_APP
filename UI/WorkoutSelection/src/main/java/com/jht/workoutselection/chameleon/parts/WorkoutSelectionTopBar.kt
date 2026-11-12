package com.jht.workoutselection.chameleon.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.jht.workoutselection.common.parts.UserButton


@Composable
internal fun WorkoutSelectionTopBar(
    onUserButton: ()->Unit,
    onLanguage: () -> Unit = {},
    onHome: () -> Unit = {},
    onPTP: () -> Unit = {},
    showPTP: Boolean = false,
    showLanguageButton: Boolean,
    showProductDashboard: MutableState<Boolean>,
    onShowDebugDialog: ()->Unit = {},
    isSecaUser: Boolean = false,
    isGuest: Boolean = false
) {

    Row(
        Modifier
            .background(DynamicTheme.colors.panelTopBackground.brush())
            .fillMaxWidth()
            .fillMaxHeight(0.1f)
            .padding(horizontal = 20.dp, vertical = 10.dp), // comp looks like it has more horz pad than vert pad?
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val buttonAspectRatio = 120/70f

        if(showLanguageButton) {
            LanguageButton(Modifier.aspectRatio(buttonAspectRatio), onLanguage)
        }
        else {
            HomeButton(Modifier.aspectRatio(buttonAspectRatio), onHome)
        }

        // If it's not a guest, it will show only one button based on an user type (SECA or PTP).
        if (!isGuest) {
            when {
                isSecaUser -> {
                    PaceLogoButton(Modifier.aspectRatio(buttonAspectRatio), onPTP)
                }

                showPTP -> {
                    PTPButton(Modifier.aspectRatio(buttonAspectRatio), onPTP)
                }
            }
        }

        ConciergeButton()

        DebugIndicator(Modifier.padding(start = 10.dp)) // using pad because we don't want extra spacer if indicator is not showing

        Spacer(modifier = Modifier.size(10.dp))

        // SPACE
        Spacer(modifier = Modifier.weight(1f))

        // WATCH BADGES and LOGO
        SmartWatchBank(
            Modifier
                .fillMaxHeight(),
            badgeModifier = Modifier.fillMaxHeight(0.5f),
            contentSpacing = 20.dp
        ) {
            BrandLogo(
                modifier = Modifier.fillMaxWidth(0.23f),
                clickModifier = Modifier.fillMaxHeight(),
                onLongPress = { showProductDashboard.value = true },
                onDebugFling = onShowDebugDialog,
            )
        }

        // SPACE
        Spacer(modifier = Modifier.weight(1f))

        // INDICATORS
        IndicatorBank(Modifier.padding(horizontal = 14.dp))

        // USER BUTTON
        UserButton(Modifier.fillMaxHeight(), onClick = onUserButton)

        // SETTINGS BUTTON
        SettingsButton(Modifier.aspectRatio(buttonAspectRatio))
    }
}
