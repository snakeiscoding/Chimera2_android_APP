package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.chimera.R
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.chimera.service.ui.panels.WorkoutPanelManager.AutoHideBehavior
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.HeartRateSource
import com.jht.enums.WifiState
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.ui.smallerThan16
import com.jht.widgets.AppleWatchBadge
import com.jht.widgets.AppleWatchIcon
import com.jht.widgets.BrandLogo
import com.jht.widgets.HeadphoneIndicator
import com.jht.widgets.HeartRateIndicator
import com.jht.widgets.ProductDashboard
import com.jht.widgets.SamsungWatchBadge
import com.jht.widgets.SamsungWatchIcon
import com.jht.widgets.SmartDeviceIndicator
import com.jht.widgets.SmartWatchPopup
import com.jht.widgets.WifiStateIcon
import com.jht.widgets.helpers.InjectionEntryPoint
import dagger.hilt.android.EntryPointAccessors

private const val TAG = "WorkoutTopPanel2.kt"

/**
 * Top panel during an active workout (Auto hide 2-panel POC)
 */
class WorkoutTopPanel2(context: Context) : ComposePanel(context) {
    private val appleWatchState by lazy { mutableStateOf(DeviceState.Disconnected) }
    private val samsungWatchState by lazy { mutableStateOf(DeviceState.Disconnected) }
    private val wifiState by lazy { mutableStateOf(WifiState.NO_CONNECTION) }
    private val headphoneState by lazy { mutableStateOf(DeviceState.Disconnected) }
    private val smartDeviceState by lazy { mutableStateOf(DeviceState.Disconnected) }
    private val heartRateSource by lazy { mutableStateOf(HeartRateSource.None) }

    //region auto hide
    private val isFull = mutableStateOf(false)
    override fun setFullscreen(isFull: Boolean) { this.isFull.value = isFull }
    private val hideMode = mutableStateOf(AutoHideBehavior.TOUCH)
    fun setAutoHideBehavior(behavior: AutoHideBehavior) { this.hideMode.value = behavior }
    private fun onHideMode(mode: AutoHideBehavior) { WorkoutPanelManager.instance?.setAutoHideBehavior(mode) }
    //endregion auto hide


    private val workoutModel: WorkoutModel
    private val navigationModel: NavigationModel
    private val deviceModel: DeviceModel
    private val systemModel: SystemModel

    init {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(context, InjectionEntryPoint::class.java) // get arbitrary DI entry point
        deviceModel = hiltEntryPoint.devices()
        systemModel = hiltEntryPoint.system()
        navigationModel = hiltEntryPoint.navigation()
        workoutModel = hiltEntryPoint.workout()

    }

    override fun panelAdded() {
        super.panelAdded()
        deviceModel.getDeviceState(Device.AppleWatch).listenAndTrigger { appleWatchState.value = it }
        deviceModel.getDeviceState(Device.SamsungWatch).listenAndTrigger { samsungWatchState.value = it }
        deviceModel.getDeviceState(Device.Headphones).listenAndTrigger { headphoneState.value = it }
        deviceModel.getDeviceState(Device.BtSink).listenAndTrigger { smartDeviceState.value = it }
        deviceModel.heartRateSource.listenAndTrigger { heartRateSource.value = it }
        deviceModel.wifiState.listenAndTrigger { wifiState.value = it }
    }

    override fun panelRemoved() {
        deviceModel.close()
        systemModel.close()
        workoutModel.close()
        super.panelRemoved()
    }

    @Composable
    override fun ThemedContent() {
        TopPanel(
            isFull = isFull.value,
            onUnhide = {
                if (hideMode.value == AutoHideBehavior.SWIPE)
                    WorkoutPanelManager.instance?.resetAutoHidePanels()
            },
            hideMode = hideMode.value,
            onHideMode = this::onHideMode,
            onHome = this::onHome,
            onSetting = this::onSettings,
            wifiState = wifiState.value,
            appleWatchState = appleWatchState.value, samsungWatchState = samsungWatchState.value,
            headphoneState = headphoneState.value, smartDeviceState = smartDeviceState.value,
            heartRateSource = heartRateSource.value,
            connectWatchResourceId = systemModel.connectWatchResourceId
        )
    }

    private fun onHome() {
        // Can this have different functions based on whether in a third party interface?
        // Should that logic belong to the WorkoutPanelManager?
        WorkoutPanelManager.onHomeClick(this)
    }
    private fun onSettings() {
        //WorkoutPanelManager.onMenuClick(this)
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        val topBarHeight =
            if (isFull.value) {
                if (hideMode.value == AutoHideBehavior.ICON) 0 else resources.getDimension(R.dimen.hidden_bar_height).toInt()
            }
            else resources.getDimension(R.dimen.top_bar_height).toInt()
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
} // END CLASS: WorkoutTopPanel2


@Preview(heightDp = 66, widthDp = 1024)
@Preview(heightDp = 80, widthDp = 1366)
@Preview(heightDp = 80, widthDp = 1920)
@Composable
private fun previewTopPanel() {
    TopPanel()
}


@Composable
private fun TopPanel(
    isFull: Boolean = false,
    onUnhide: () -> Unit = {},
    hideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
    onHideMode: (AutoHideBehavior) -> Unit = {},
    onHome: () -> Unit = {},
    onSetting: () -> Unit = {},
    wifiState: WifiState = WifiState.STRENGTH_100,
    appleWatchState: DeviceState = DeviceState.Disconnected,
    samsungWatchState: DeviceState = DeviceState.Disconnected,
    headphoneState: DeviceState = DeviceState.Disconnected,
    smartDeviceState: DeviceState = DeviceState.Disconnected,
    heartRateSource: HeartRateSource = HeartRateSource.None,
    connectWatchResourceId: Int = 0,
) {
    if (isFull) TopPanelHidden(onUnhide)
    else TopPanelVisible(
        hideMode, onHideMode, // auto hide items
        onHome, onSetting, wifiState, appleWatchState, samsungWatchState,
        headphoneState, smartDeviceState, heartRateSource, connectWatchResourceId
    )
}

@Composable
private fun TopPanelHidden(
    onSwipe: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, y) = dragAmount
                    when {
                        x > 0 -> {}
                        x < 0 -> {}
                    }
                    when {
                        y > 0 -> {
                            onSwipe()
                        }

                        y < 0 -> {}
                    }
                }
            }
    )
}

@Composable
private fun TopPanelVisible(
    hideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
    onHideMode: (AutoHideBehavior) -> Unit = {},
    onHome: () -> Unit = {},
    onSetting: () -> Unit = {},
    wifiState: WifiState = WifiState.STRENGTH_100,
    appleWatchState: DeviceState = DeviceState.Disconnected,
    samsungWatchState: DeviceState = DeviceState.Disconnected,
    headphoneState: DeviceState = DeviceState.Disconnected,
    smartDeviceState: DeviceState = DeviceState.Disconnected,
    heartRateSource: HeartRateSource = HeartRateSource.None,
    connectWatchResourceId: Int = 0,
) {
    val showproductDashboard = remember { mutableStateOf(false) }
    val showAppleWatchPopup = remember { mutableStateOf(false) }
    val showSamsungWatchPopup = remember { mutableStateOf(false) }
    if (showAppleWatchPopup.value || showSamsungWatchPopup.value) {
        SmartWatchPopup(showAppleWatchPopup.value, {
            showAppleWatchPopup.value = false
            showSamsungWatchPopup.value = false
        }, connectWatchResourceId)
    }
    ProductDashboard(showproductDashboard)

    BoxWithConstraints {
        val smallScreen = smallerThan16(this.maxWidth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.top_bar_height))
                .background(panelBackgroundColor),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT SECTION
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PanelIconButton(
                        iconId = R.drawable.ic_home,
                        onClick = onHome
                    )
                    if (appleWatchState.isDisconnected && samsungWatchState.isDisconnected) {
                        val badgeModifier = Modifier
                            .fillMaxHeight(0.6f)
                            .padding(4.dp)
                        if (appleWatchState.isSupported) AppleWatchBadge({showAppleWatchPopup.value = true}, badgeModifier)
                        if (samsungWatchState.isSupported) SamsungWatchBadge({showSamsungWatchPopup.value = true}, badgeModifier)
                    }
                }
            }
            // CENTER SECTION
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandLogo(
                    Modifier.fillMaxWidth(0.23f),
                    onLongPress = {showproductDashboard.value = true},
                    onDebugFling = { DataMatrix.sendEvent(UiServiceKeys.ACTION_SHOW_DEBUG_PANEL_EVENT) },
                )
            }
            // RIGHT SECTION
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeMenu(hideMode, onHideMode)
                    // INDICATOR SECTION
                    Row(
                        modifier = Modifier
                            .height(60.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val indicatorModifier = Modifier.padding(end = 14.dp)
                        AppleWatchIcon(appleWatchState = appleWatchState, indicatorModifier)
                        SamsungWatchIcon(samsungWatchState = samsungWatchState, indicatorModifier)
                        HeadphoneIndicator(deviceState = headphoneState, indicatorModifier)
                        if (smartDeviceState != DeviceState.NotSupported) {
                            SmartDeviceIndicator(deviceState = smartDeviceState, indicatorModifier)
                        }
                        HeartRateIndicator(heartRateSource = heartRateSource, indicatorModifier)
                        WifiStateIcon(wifiState = wifiState)
                    }
                    // BUTTON SECTION
                    PanelIconButton(
                        iconId = R.drawable.ic_setting_gray,
                        onClick = onSetting
                    )
                }
            }
        }
    }
}


//region Auto Hide Mode Menu
@Composable
private fun ModeMenu(
    hideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
    onHideMode: (AutoHideBehavior) -> Unit = {},
) {
    val menuShown = remember { mutableStateOf(false) }

    PanelCommandButton(
        onClick = { menuShown.value = !menuShown.value },
        containerColor = Color.Blue
    ) {
        Text("POC Autohide")
    }
    DropdownMenu(
        expanded = menuShown.value,
        onDismissRequest = { menuShown.value = false }
    ) {
        ModeMenuItem(hideMode, AutoHideBehavior.TOUCH) { onHideMode(it); menuShown.value = false }
        ModeMenuItem(hideMode, AutoHideBehavior.SWIPE) { onHideMode(it); menuShown.value = false }
        ModeMenuItem(hideMode, AutoHideBehavior.ICON) { onHideMode(it); menuShown.value = false }
    }
}

@Composable
private fun ModeMenuItem(
    currentHideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
    myHideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
    onClick: (AutoHideBehavior) -> Unit = {},
) {
    DropdownMenuItem(
        text = {
            val isSelected = currentHideMode == myHideMode
            Text(
                text = myHideMode.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.Blue else Color.Black,
            )
        },
        onClick = { onClick(myHideMode) }
    )
}
//endregion Auto Hide Mode Menu
