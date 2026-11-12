package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.data.SoftwareUpdateState
import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.HeartRateSource
import com.jht.enums.WifiState
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.SystemModel
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSizeHd
import com.jht.widgets.helpers.getInjectionEntryPoint
import java.util.EnumSet

/*
    The current directive is that icons will show when connected and be absent when disconnected.
    This is a change from the past behavior where we had icons showing for both states.
 */

@Composable
private fun ConnectionIndicator(
    connectedIconId: Int,
    //disconnectedIconId: Int,
    modifier: Modifier = Modifier,
    deviceState: DeviceState = DeviceState.Disconnected,
) {
    //val iconId = if (deviceState.isConnected) connectedIconId else disconnectedIconId
    if (deviceState.isConnected) {
        Icon(
            painter = painterResource(id = connectedIconId),
            contentDescription = null,
            tint = DynamicTheme.colors.text1,
            modifier = modifier,
        )
    }
}

@Composable
fun HeadphoneIndicator(
    deviceState: DeviceState,
    modifier: Modifier = Modifier,
) {
    ConnectionIndicator(
        R.drawable.ic_new_headphones,
        //R.drawable.ic_headphones_disabled,
        modifier.size(20.dp).testTag("HeadphoneIndicator"),
        deviceState,
    )
}

@Composable
fun SmartDeviceIndicator(
    deviceState: DeviceState,
    modifier: Modifier = Modifier,
) {
    ConnectionIndicator(
        R.drawable.ic_new_smart_device_enabled,
        //R.drawable.ic_smart_device_disabled,
        modifier
            .testTag("SmartDeviceIndicator")
                .size(20.dp),
        deviceState,
    )
}

@Composable
fun HeartRateIndicator(
    heartRateSource: HeartRateSource,
    modifier: Modifier = Modifier,
    excludeSource: EnumSet<HeartRateSource> = EnumSet.of(HeartRateSource.Watch),
) {
    if (heartRateSource != HeartRateSource.None && !excludeSource.contains(heartRateSource)) {
        if (heartRateSource == HeartRateSource.MyZone) { // not sure what tint would do with MyZone icon?
            Image(
                modifier = modifier.size(20.dp).testTag("HeartRateIndicator"),
                painter = painterResource(id = R.drawable.logo_myzone),
                contentDescription = null
            )
        } else {
            ConnectionIndicator(
                R.drawable.ic_new_heart_rate_enabled,
                //R.drawable.ic_heart_rate_monitor_disabled,
                modifier.size(20.dp).testTag("HeartRateIndicator"),
                DeviceState.fromBool(true),
            )
        }
    }
}


@Composable
fun WifiStateIcon(
    wifiState: WifiState,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id =
        when (wifiState) {
            WifiState.NO_CONNECTION ->
                R.drawable.topbar_wifi_noconnector

            WifiState.STRENGTH_25 ->
                R.drawable.topbar_wifi_25

            WifiState.STRENGTH_50 ->
                R.drawable.topbar_wifi_50

            WifiState.STRENGTH_75 ->
                R.drawable.topbar_wifi_75

            WifiState.STRENGTH_100 ->
                R.drawable.topbar_wifi_100

            WifiState.ETHERNET ->
                R.drawable.topbar_ethernet
        }),
        contentDescription = null,
        tint = DynamicTheme.colors.text1,
        modifier = modifier
            .width(20.dp)
            .aspectRatio(1f)
            .testTag("WifiStateIcon")
    )
}

@Composable
private fun SoftwareUpdateIndicator(
    updateState: SoftwareUpdateState,
    modifier: Modifier = Modifier,
) {
    val iconId = when (updateState) {
        SoftwareUpdateState.Download -> R.drawable.download_arrow
        SoftwareUpdateState.Ready -> R.drawable.ic_software_update
        else -> 0
    }
    if (iconId != 0) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            modifier = modifier.size(20.dp).testTag("SoftwareUpdateIndicator"),
        )
    }
}

/** A self contained widget of the commonly grouped indicators */
@Composable
fun IndicatorBank(
    modifier: Modifier = Modifier,
    contentSpacing: Dp = 14.dp,
) {
    val context = LocalContext.current
    val state = remember { IndicatorBankState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoftwareUpdateIndicator(state.updateState.value)
        HeadphoneIndicator(state.headphoneState.value)
        SmartDeviceIndicator(state.smartDeviceState.value)
        HeartRateIndicator(state.heartRateSource.value)
        AppleWatchIcon(state.appleWatchState.value)
        SamsungWatchIcon(state.samsungWatchState.value)
        WifiStateIcon(state.wifiState.value)
    }
}
private class IndicatorBankState(context: Context) {
    // defaulting to visible states for previews, on device the model will update them with actual states
    val appleWatchState = mutableStateOf(DeviceState.Disconnected)
    val samsungWatchState = mutableStateOf(DeviceState.Disconnected)
    val wifiState = mutableStateOf(WifiState.NO_CONNECTION)
    val headphoneState = mutableStateOf(DeviceState.Connected)
    val smartDeviceState = mutableStateOf(DeviceState.Connected)
    val heartRateSource = mutableStateOf(HeartRateSource.BLE)
    val updateState = mutableStateOf(SoftwareUpdateState.Ready)
    val deviceModel: DeviceModel
    val systemModel: SystemModel

    init {
        val injection = getInjectionEntryPoint(context)
        deviceModel = injection.devices()
        systemModel = injection.system()
    }

    fun start() {
        deviceModel.getDeviceState(Device.AppleWatch).listenAndTrigger { appleWatchState.value = it }
        deviceModel.getDeviceState(Device.SamsungWatch).listenAndTrigger { samsungWatchState.value = it }
        deviceModel.wifiState.listenAndTrigger { wifiState.value = it }
        deviceModel.getDeviceState(Device.Headphones).listenAndTrigger { headphoneState.value = it }
        deviceModel.getDeviceState(Device.BtSink).listenAndTrigger { smartDeviceState.value = it }
        deviceModel.heartRateSource.listenAndTrigger { heartRateSource.value = it }
        systemModel.setSoftwareUpdateCallback { state, _ -> updateState.value = state }
    }

    fun stop() {
        systemModel.close()
        deviceModel.close()
    }
}

@AllDevicePreviews
@Composable
private fun PreviewIndicatorBank() {
    Column(
        Modifier.fillMaxSize()
    ) {
        IndicatorBank(Modifier.fillMaxWidth().fillMaxHeight(0.1f))
    }
}

@AllDevicePreviews
@Composable
fun RFIDIndicator(modifier: Modifier = Modifier, isVisible: Boolean = false) {
    if (isVisible) {
        Box(
            modifier = modifier
                .size(72.dp.computeSizeHd())
                .clip(CircleShape)
                .background(Color(0xFF393939)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_rfid),
                contentDescription = "rfid",
                modifier = Modifier.size(38.4.dp.computeSizeHd())
            )
        }
    }
}
