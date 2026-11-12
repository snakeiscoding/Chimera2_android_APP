@file:OptIn(ExperimentalComposeUiApi::class)

package com.jht.seca2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.seca.R
import com.jht.seca2.ui.SecaColors
import com.jht.seca2.secaDesignWidth
import com.jht.seca2.secaUiTheme
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllSecaDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.HomeButton

@Composable
internal fun SecaTopBar(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    onClickLogo: () -> Unit = {},
    showExitDialog: Boolean = true,
) {
    var exitDialogShown by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            //dimension required for top bar height because no matching spacing tokens
            .height(dimensionResource(R.dimen.topBarHeight))
            .background(color = SecaColors.white)
            .border(width = 1.dp.computeSize(secaDesignWidth), color = SecaColors.borderGray)
    ) {
        //Seca Logo
        //this is the same DP on both screen sizes, so we can hardcode the dimensions
        Image(
            painter = painterResource(R.drawable.ic_logo_seca),
            contentDescription = "Seca Logo",
            modifier = Modifier
                .testTag("seca:SecaTopBar:Image")
                .align(Alignment.Center)
                .width(124.dp)
                .height(40.dp)
                //todo make this touch and hold CGC-20893
                .clickableNoEffect { onClickLogo() })

        //Exit Button
        //This is also the same DP on both screen sizes, same with the padding on the right
        SecaExitButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp)
                .testTag("seca:SecaTopBar:SecaExitButton")
        ) {
            if (showExitDialog) {
                exitDialogShown = true
            } else {
                onExit()
            }
        }
    }

    ExitDialog(
        shown = exitDialogShown,
        onCancel = { exitDialogShown = false },
        onExit = onExit
    )
}



/** seca themed HOME button */
@Composable
fun SecaHomeButton(
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ThemeButton(
        modifier = Modifier
            .testTag("HomeButton")
            .padding(5.dp)
            .background(color = SecaColors.white)
            .then(modifier),
        colors = ButtonColors(SecaColors.neutralTertiary, SecaColors.neutralTertiary, SecaColors.neutralTertiary, SecaColors.neutralTertiary),
        border = BorderStroke(1.dp, SecaColors.unpressedButtonBorder),
        contentPadding = PaddingValues(),
        onClick = onClick
    ) {
        Icon(
            painterResource(id = com.jht.widgets.R.drawable.ic_home),
            null,
            Modifier.fillMaxSize(0.5f),
            tint = SecaColors.white,
        )
    }
}

//region PREVIEWS
@AllSecaDevicePreviews
@Composable
private fun SecaTopBarPreview() {
    DynamicTheme(secaUiTheme) {
        SecaTopBar(
            onClickLogo = {},
            onExit = {}
        )
    }
}

@AllSecaDevicePreviews
@Composable
private fun SecaTopBarPreviewHome() {
    DynamicTheme(secaUiTheme) {
        SecaTopBar(
            onClickLogo = {},
            onExit = {},
        )
    }
}