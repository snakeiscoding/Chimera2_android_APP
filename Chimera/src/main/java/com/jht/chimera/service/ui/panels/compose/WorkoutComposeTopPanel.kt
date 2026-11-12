package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.chimera.R
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.lib.workoutapps.AppInfo
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.ui.LaunchAppCondition
import com.jht.chimera.service.ui.LaunchAppData
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.concierge.ui.ConciergeButton
import com.jht.datamatrixlib.DataMatrix
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.clickableNoEffect
import com.jht.ui.pxToDp
import com.jht.widgets.BrandLogo
import com.jht.widgets.IndicatorBank
import com.jht.widgets.PopupFullscreen
import com.jht.widgets.ProductDashboard
import com.jht.widgets.SettingsButton
import com.jht.widgets.SmartWatchBank

private const val TAG = "WorkoutComposeTopPanel.kt"

/**
 * Top panel during an active workout
 */
class WorkoutComposeTopPanel(context: Context) : ComposePanel(context) {
    private val showFullBar = mutableStateOf(false)
    private val useBackButton = mutableStateOf(false)
    private val showCasting = mutableStateOf(false)
    private val tunerReady = mutableStateOf(false)
    private val showInitializingDialog = mutableStateOf(false)
    private val mDataMatrix = DataMatrix()

    override fun panelAdded() {
        super.panelAdded()
        mDataMatrix.createBoolAdapter(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL) { updateThirdPartyButtons() }
        updateThirdPartyButtons()
    }

    override fun panelRemoved() {
        super.panelRemoved()
        mDataMatrix.finish()
    }

    override fun panelUpdated() {
        super.panelUpdated()
        updateThirdPartyButtons()
    }

    @Composable
    override fun ThemedContent() {
        TopPanel(
            onHome = ::onHome,
            onLaunchCasting = ::onLaunchCasting,
            showFullBar = showFullBar.value,
            useBackButton = useBackButton.value,
            showCasting = showCasting.value,
            tunerReady = tunerReady.value,
            showInitializingDialog = showInitializingDialog.value,
            onDismissInitializingDialog = { showInitializingDialog.value = false },
        )
    }

    private fun onHome() {
        // Can this have different functions based on whether in a third party interface?
        // Should that logic belong to the WorkoutPanelManager?
        if (useBackButton.value) WorkoutPanelManager.onExitClick(this)
        else LaunchAppUtil.launchSplashScreen()
    }
    private fun onLaunchCasting() {
        if (!tunerReady.value) {
            showInitializingDialog.value = true
            return
        }
        val data = LaunchAppData(AppIds.CASTING_UUID, LaunchAppCondition.None)
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_APP_PARCELABLE, data)
    }

    private fun updateThirdPartyButtons() {
        // do we even need to check if app is third party? if top bar is showing in the IDLE state, would it always be the full bar?
        val appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING)
        // if we really do need the state, we could add a method that workout panel manager can call directly
        // if panel is different depending on state, should it just have a different ID inside of panel behavior so it just gets swapped on state change?
        val panelState = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT)
        // copied from the old code, but could be re-evaluated?
        val appInfo = AppManager.getInstance().getApp(appId)
        // old code used back button for retail, but commercial had a home button that bailed to splash if there was no welcome screen?
        useBackButton.value = ProductConfig.isRetail() || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL)
        showFullBar.value = appInfo?.useThirdPartyInterface == true && panelState == UiServiceKeys.WorkoutPanelState.IDLE
        // check for casting support and NOT an integration
        val tvSource = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))
        showCasting.value = ProductConfig.supportsCasting(tvSource)
                && appInfo?.useThirdPartyInterface != true
                && appInfo?.category != AppInfo.Category.Integration
        tunerReady.value = !tvSource.isLG750E() || DataMatrix.getBoolean(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL)
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        val topBarHeight = resources.getDimension(R.dimen.top_bar_height).toInt()
        val width = RealScreen.widthPixels
        val viewParams = WindowManager.LayoutParams(
            width, topBarHeight,
            mPanelMode.windowType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
        )
        viewParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
} // END CLASS: WorkoutComposeTopPanel


@Preview(heightDp = 66, widthDp = 1024)
@Preview(heightDp = 80, widthDp = 1366)
@Preview(heightDp = 80, widthDp = 1920)
@Composable
private fun PreviewTopPanel() {
    TopPanel()
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TopPanel(
    onHome: () -> Unit = {},
    showFullBar: Boolean = false, // in case we need 3rd party buttons here or maybe we just create a different top bar for that?
    useBackButton: Boolean = false,
    showCasting: Boolean = false,
    onLaunchCasting: () -> Unit = {},
    tunerReady: Boolean = true,
    showInitializingDialog: Boolean = false,
    onDismissInitializingDialog: () -> Unit = {},
) {
    //Can't call the RealScreen application context in previews
    val sideMargin: Dp = if(!LocalInspectionMode.current) {
        (RealScreen.widthPixels.pxToDp() - dimensionResource(R.dimen.app_container_width)) / 2
    }
    else{
        //Android Studio preview margin
        10.dp
    }
    val showProductDashboard = remember { mutableStateOf(false) }
    ProductDashboard(showProductDashboard)

    if (showInitializingDialog) {
        PopupFullscreen(
            onDismissRequest = onDismissInitializingDialog,
            properties = PopupProperties(focusable = true, clippingEnabled = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoEffect(onClick = onDismissInitializingDialog),
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
                        onClick = onDismissInitializingDialog,
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

    Column(
        Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.top_bar_height))
            .background(DynamicTheme.colors.panelTopBackground.brush())
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // LEFT SECTION
            Row(
                Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showFullBar) {
                    HomeAppsButton(Modifier.panelIconButton(), onHome, useBackButton)
                } else {
                    Spacer(Modifier.size(sideMargin))
                }
                ConciergeButton(Modifier.fillMaxHeight(0.7f))
                if (showCasting) {
                    CastingButton(
                        Modifier
                            .fillMaxHeight(0.7f)
                            .alpha(if (tunerReady) 1f else 0.4f),
                        onClick = onLaunchCasting,
                    )
                }
            }

            // CENTER SECTION
            SmartWatchBank(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(),
                badgeModifier = Modifier.fillMaxHeight(0.5f),
                contentSpacing = 20.dp
            ) {
                BrandLogo(
                    Modifier.fillMaxWidth(0.23f),
                    onLongPress = {showProductDashboard.value = true},
                    onDebugFling = { DataMatrix.sendEvent(UiServiceKeys.ACTION_SHOW_DEBUG_PANEL_EVENT) }
                )
            }

            // RIGHT SECTION
            Row(
                Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // INDICATOR SECTION
                IndicatorBank(
                    Modifier.height(60.dp),
                    contentSpacing = 10.dp,
                )
                // BUTTON SECTION
                if (showFullBar) {
                    SettingsButton(
                        modifier = Modifier.panelIconButton(),
                    )
                } else {
                    Spacer(Modifier.size(sideMargin))
                }
            }
        }
        HorizontalDivider(Modifier.fillMaxWidth(), 1.dp, panelBorderColor)
    }
}


@Composable
fun HomeAppsButton(
    modifier: Modifier,
    onClick: () -> Unit,
    useBackIcon: Boolean = false,
) {
    ThemeButton(
        modifier = Modifier
            .testTag("HomeButton")
            .then(modifier),
        contentPadding = PaddingValues(),
        onClick = onClick,
    ) {
        Icon(
            painterResource(if (useBackIcon) R.drawable.ic_back_button_white else R.drawable.ic_home),
            null,
            Modifier.fillMaxSize(0.5f),
            DynamicTheme.colors.text1,
        )
    }
}

@Composable
private fun CastingButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    ThemeButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(),
        modifier = modifier
            .aspectRatio(120 / 70f) // copied from ConciergeButton
            .testTag("CastingButton")
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(.5f),
            painter = painterResource(id = R.drawable.ic_casting),
            contentDescription = null,
            tint = DynamicTheme.colors.text1,
        )
    }
}

