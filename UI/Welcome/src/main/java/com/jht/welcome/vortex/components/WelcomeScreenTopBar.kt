package com.jht.welcome.vortex.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jht.concierge.ui.ConciergeButton
import com.jht.theme.DynamicTheme
import com.jht.widgets.BrandLogo
import com.jht.widgets.DebugIndicator
import com.jht.widgets.IndicatorBank
import com.jht.widgets.LanguageButton
import com.jht.widgets.SmartWatchBank
import com.jht.widgets.fillScreenHeight

@Composable
internal fun WelcomeScreenTopBar(
    modifier: Modifier = Modifier,
    onLanguage: () -> Unit = {},
    isLanguageOverlayShowing: MutableState<Boolean>,
    showLanguageButton: Boolean,
    showProductDashboard: MutableState<Boolean>,
    showDebugDialog: ()->Unit = {},
    onSmartWatchIconClick: (isShowing: Boolean) -> Unit = {},
    ) {

    Row(
        modifier
            .background(DynamicTheme.colors.panelTopBackground.brush())
            .fillMaxWidth()
            .fillScreenHeight(0.09f)
            .padding(horizontal = 20.dp, vertical = 10.dp), // comp looks like it has more horz pad than vert pad?
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val buttonAspectRatio = 120/70f
            if(showLanguageButton) {
                LanguageButton(
                    Modifier.aspectRatio(buttonAspectRatio),
                    onLanguage,
                    isLanguageOverlayShowing
                )
            }

            ConciergeButton()

            DebugIndicator(Modifier.padding(start = 10.dp))
        }

        // WATCH BADGES and LOGO
        SmartWatchBank(
            modifier = Modifier
                .fillMaxHeight(),
            badgeModifier = Modifier
                .defaultMinSize(minHeight = 40.dp) // GymKit specs require 40px minimum
                .fillMaxHeight(.5f),
            contentSpacing = 20.dp,
            onSmartWatchIconClick = onSmartWatchIconClick,
        ) {
            BrandLogo(
                modifier = Modifier.fillMaxWidth(0.23f),
                clickModifier = Modifier.fillMaxHeight(),
                onLongPress = { showProductDashboard.value = true },
                onDebugFling = showDebugDialog,
            )
        }

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) { // room for multiple Buttons at end.
            IndicatorBank(Modifier.padding(horizontal = 14.dp))
        }

    }
}