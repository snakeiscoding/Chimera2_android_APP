package com.jht.applibrary.chameleon

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.applibrary.R
import com.jht.data.AppInfo
import com.jht.theme.ThemeButton
import com.jht.ui.clickableNoEffect
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.largerThan16
import com.jht.ui.smallerThan16
import com.jht.widgets.BoxGrid
import com.jht.widgets.MessageDialog
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.PopupFullscreen

/**
 * The app library is where a user launches apps. App ordering and availability can be adjusted
 * from MES. Certain apps will only be available with compatible workouts and modalities.
 *
 * Inputs:
 * @param theme This is the UI theme (light or dark mode)
 * @param appInfoList The list of apps, including title and icon
 * @param ssoPrompt The dialog to warn user that data isn't loaded yet
 *
 * Outputs:
 * @param onAppClick
 * @param onForceLaunch
 *
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppLibraryScreen(
    theme: DynamicThemeData = defaultDynamicTheme,
    appInfoList: List<AppInfo>,
    ssoPrompt: MutableState<AppInfo?> = mutableStateOf(null),
    tunerConnected: Boolean = true,
    showTunerInitializingDialog: Boolean = false,
    onDismissTunerInitializingDialog: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit = {},
    onForceLaunch: (AppInfo) -> Unit = {},
) {
    val upgradePrompt = remember(appInfoList) { mutableStateOf(false) }
    val verticalGradient = Brush.verticalGradient(
        listOf(Color(0xFF393939), Color(0xFF191919), Color(0xFF191919)),
        startY = -150.0f,
        endY = 500.0f
    )
    // For new UI, they want 7 x 3, but I am assuming 10 inch will need to be less
    val smallScreen = smallerThan16()
    val columnsPerPage = if (smallScreen) 4 else 7
    val rowsPerPage = 3
    DynamicTheme(theme) {
        Box(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(verticalGradient),
        ) {
            val appInfoPages = remember(appInfoList) { appInfoList.chunked(columnsPerPage * rowsPerPage) }
            val pagerState = rememberPagerState { appInfoPages.size }

            // The page which holds the app icons
            HorizontalPager(
                modifier = Modifier
                    .testTag("AppGrid")
                    .matchParentSize(),
                state = pagerState,
            ) { page ->
                // Grid of boxes of icons
                AppIconsGridView(
                    appInfoPage = appInfoPages[page],
                    rows = rowsPerPage,
                    columns = columnsPerPage,
                    tunerConnected = tunerConnected,
                ) {
                    if (it.locked) {
                        upgradePrompt.value = true
                    } else {
                        onAppClick(it)
                    }
                }
            }
            // overlay the dot indicator section so we can swipe there
            PagerJumpingDotIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(30.dp),
                state = pagerState,
                activeColor = DynamicTheme.colors.primary,
                dotSize = 10.dp,
            )
        }
        //This is for Single Sign On
        SsoPromptDialog(ssoPrompt, onForceLaunch)

        if (showTunerInitializingDialog) {
            PopupFullscreen(
                onDismissRequest = onDismissTunerInitializingDialog,
                properties = PopupProperties(focusable = true, clippingEnabled = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickableNoEffect(onClick = onDismissTunerInitializingDialog),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .background(DynamicTheme.colors.gray1)
                            .clickableNoEffect { }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.initializing_please_wait_pg),
                            color = DynamicTheme.colors.text1,
                        )
                        ThemeButton(
                            onClick = onDismissTunerInitializingDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DynamicTheme.colors.secondary,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = stringResource(android.R.string.ok),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        //This is for tiers, when selecting a locked app
        TiersUpgrade(upgradePrompt)
    }
}

/**
 * This is for the case where an app is still syncing to SSO. If a user launches the app, we
 * will show a dialog to give the option to launch the app anyway.
 *
 * Inputs:
 * @param ssoPrompt the dialog's current value
 *
 * Outputs:
 * @param onForceLaunch AppInfo of the app being launched
 */
@Composable
private fun SsoPromptDialog(
    ssoPrompt: MutableState<AppInfo?>,
    onForceLaunch: (AppInfo) -> Unit
){
    ssoPrompt.value?.let { app->
        // If user denies, we continue syncing SSO data. If user clicks again before data has
        // loaded, we display the dialog again.
        MessageDialog(
            title = "",
            message = stringResource(R.string.sync_pg).replace("app_name", app.title),
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .aspectRatio(1.8f),
            okText = stringResource(R.string.yes_tc),
            cancelText = stringResource(R.string.no_tc),
        ) { confirm->
            ssoPrompt.value = null
            if (confirm) {
                onForceLaunch(app)
            }
        }
    }
}

/**
 * This is for tiers to provide a prompt when an application requires an upgrade in membership
 *
 * Inputs:
 * @param upgradePrompt Boolean state whether prompt is shown or hidden
 */
@Composable
private fun TiersUpgrade(upgradePrompt: MutableState<Boolean>) {
    if (upgradePrompt.value) MessageDialog(
        title = stringResource(R.string.upgrade_unavailable_tc),
        message = stringResource(R.string.upgrade_unavailable_pg),
        modifier = Modifier
            .fillMaxWidth(0.33f)
            .aspectRatio(1.8f),
        onDismiss = { upgradePrompt.value = false },
    )
}

/**
 * The grid which holds all of the app icons and titles and adds the spacing between each icon.
 *
 * Inputs:
 * @param appInfoPage list of currently displayed apps
 * @param rows number of rows to display
 * @param columns number of columns to display
 *
 * Outputs:
 * @param onAppClick AppInfo to return when app is clicked
 */
@Composable
fun AppIconsGridView(
    appInfoPage: List<AppInfo>,
    rows: Int,
    columns: Int,
    tunerConnected: Boolean = true,
    onAppClick: (AppInfo) -> Unit
) {
    // do an adjusted pad or use a max size?
    val iconPad = if (largerThan16()) 35.dp else 15.dp
    BoxGrid(
        rows = rows,
        columns = columns,
        items = appInfoPage,
        modifier = Modifier.padding(30.dp),
    ) { cell ->
        AppInfoGridItem(
            Modifier
                .aspectRatio(1f)
                .padding(iconPad),
            cell.item,
            tunerConnected = tunerConnected,
            onAppClick,
        )
    }
}

/**
 * Holds icon, text, and spacing for an individual app displayed on the screen.
 *
 * Inputs:
 * @param modifier visual details, the padding and aspect ratio
 * @param gridItem AppInfo properties such as title, icon and locked status which are displayed,
 * and uuid which is not
 *
 * Outputs:
 * @param onAppClick AppInfo to return when clicked
 */
@Composable
fun AppInfoGridItem(
    modifier: Modifier,
    gridItem: AppInfo,
    tunerConnected: Boolean = true,
    onAppClick: (AppInfo) -> Unit
) {
    val isTvNotReady = gridItem.tunerDependent && !tunerConnected
    AppButton(
        gridItem.title,
        gridItem.icon,
        modifier.alpha(if (isTvNotReady) 0.4f else 1f),
        gridItem.uuid,
        gridItem.locked,
    ) {
        onAppClick(gridItem)
    }

}



//region PREVIEWS
@Composable
private fun dummyData() = List(25) {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    AppInfo(
        title = "Twitter",
        uuid = "",
        icon = icon!!,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewAppLibraryScreenDark() {
    AppLibraryScreen(
        theme = darkDynamicTheme,
        dummyData(),
    )
}

@AllDevicePreviews
@Composable
private fun PreviewAppLibraryScreenLight() {
    AppLibraryScreen(
        theme = lightDynamicTheme,
        dummyData(),
    )
}
//endregion PREVIEWS
