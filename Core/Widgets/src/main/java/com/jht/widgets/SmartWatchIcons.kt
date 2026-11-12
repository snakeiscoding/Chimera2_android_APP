package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.enums.Device
import com.jht.enums.DeviceState
import com.jht.enums.RelativeLocation
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.MarketingModel
import com.jht.widgets.helpers.getInjectionEntryPoint

// We need to support different marketing levels for watch for awhile. If this becomes a thing, maybe create a class to hold strings/images and pass that?
internal fun getAppleBadgeImageId(gymKitLevel: Int): Int  = when {
    (gymKitLevel >= 2) -> R.drawable.ic_apple_watch2
    else -> R.drawable.ic_apple_watch
}
internal fun getAppleConnectTextId(gymKitLevel: Int): Int = when {
    (gymKitLevel >= 2) -> R.string.connect_watch_phone_apple_tc
    else -> R.string.connect_watch_apple_tc
}
@Composable
internal fun getAppleInstructionTextId(gymKitLevel: Int, readerLocation: RelativeLocation): String = when {
    (gymKitLevel >= 2) -> when (readerLocation) {
        RelativeLocation.TOP_RIGHT -> stringResource(R.string.apple2_help_top_right_pg)
        RelativeLocation.BOTTOM_LEFT -> stringResource(R.string.apple2_help_bottom_left_pg)
        RelativeLocation.BOTTOM_MIDDLE -> stringResource(R.string.apple2_help_bottom_center_pg)
        else -> stringResource(R.string.apple2_help_pg)
    }
    else -> stringResource(R.string.apple_help_pg)
}
@Composable
internal fun getAppleNoteTextId(gymKitLevel: Int): String = when {
    (gymKitLevel >= 2) -> stringResource(R.string.apple2_help_note_pg)
    else -> ""
}


//Apple Watch Composable
@Composable
fun AppleWatchIcon(
    appleWatchState: DeviceState,
    modifier: Modifier = Modifier,
) {
    if (appleWatchState.isConnected || appleWatchState.isTemporarilyDisconnected)
        Image(
            painter = painterResource(
                id = if (appleWatchState.isConnected) {
                    R.drawable.gymkit_connected
                } else {
                    R.drawable.gymkit_out_of_range
                }
            ),
            contentDescription = null,
            modifier
                .height(42.dp)
                .wrapContentWidth()
                .testTag("AppleWatchIcon")
        )
}

@Composable
fun AppleWatchBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gymKitLevel: Int = 0,
) {
    Image(
        painter = painterResource(id = getAppleBadgeImageId(gymKitLevel)),
        contentDescription = null,
        modifier
            //.aspectRatio(140 / 38f)
            .clickable { onClick() }
            .testTag("AppleWatchBadge")
    )
}

//Samsung Watch Composable
@Composable
fun SamsungWatchIcon(
    samsungWatchState: DeviceState,
    modifier: Modifier = Modifier,
) {
    if (samsungWatchState.isConnecting ||
        samsungWatchState.isConnected ||
        samsungWatchState.isTemporarilyDisconnected
    )
        Image(
            painter = painterResource(
                id = if (samsungWatchState.isConnecting) {
                    R.drawable.ic_icon_samsung_connecting
                } else if (samsungWatchState.isConnected) {
                    R.drawable.ic_icon_samsung_connected
                } else {
                    R.drawable.ic_icon_samsung_disconnected
                }
            ),
            contentDescription = null,
            modifier
                .height(23.dp)
                .wrapContentWidth()
                .testTag("SamsungWatchIcon")
        )
}

@Composable
fun SamsungWatchBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_samsung_watch),
        contentDescription = null,
        modifier
            //.aspectRatio(140 / 38f)
            .clickable { onClick() }
            .testTag("SamsungWatchBadge")
    )
}



/** Self contained widget for Smart watch badges. Includes the necessary popups. If a [middleSlot] is defined, it is always shown. */
@Composable
fun SmartWatchBank(
    modifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal, // do we pass orientation, or just create 2 composables?
    contentSpacing: Dp = 10.dp,
    onSmartWatchIconClick: (isShowing: Boolean) -> Unit = {},
    keepBadgeSpacing: Boolean = true,
    middleSlot: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val state = remember { SmartWatchBankState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    val usingSamsung = !state.samsungWatchState.value.isDisconnected && state.samsungWatchState.value.isSupported
    val usingApple = !state.appleWatchState.value.isDisconnected && state.appleWatchState.value.isSupported
    val showSamsung = !usingApple && state.samsungWatchState.value.isSupported && state.samsungWatchState.value.isDisconnected
    val showApple = !usingSamsung && state.appleWatchState.value.isSupported && state.appleWatchState.value.isDisconnected
    // alpha of zero will make element invisible, but it may still be clickable so block click callbacks
    val alphaSamsung = if (showSamsung) 1f else 0f
    val alphaApple = if (showApple) 1f else 0f
    val showAppleWatchPopup = remember(showApple, showSamsung) {
        onSmartWatchIconClick(false)
        mutableStateOf(false)
    }
    val showSamsungWatchPopup = remember(showApple, showSamsung) {
        onSmartWatchIconClick(false)
        mutableStateOf(false)
    }

    val badgeContent: @Composable () -> Unit = {
        if (keepBadgeSpacing) {
            AppleWatchBadge(onClick = { if (showApple) showAppleWatchPopup.value = true }, modifier = badgeModifier.alpha(alphaApple), gymKitLevel = state.gymKitLevel.intValue)
            middleSlot()
            SamsungWatchBadge(onClick = { if (showSamsung) showSamsungWatchPopup.value = true }, modifier = badgeModifier.alpha(alphaSamsung))
        } else {
            if (showApple) AppleWatchBadge(onClick = { showAppleWatchPopup.value = true }, modifier = badgeModifier.alpha(alphaApple), gymKitLevel = state.gymKitLevel.intValue)
            middleSlot()
            if (showSamsung) SamsungWatchBadge(onClick = { showSamsungWatchPopup.value = true }, modifier = badgeModifier.alpha(alphaSamsung))
        }
    }

    if (orientation == Orientation.Horizontal) {
        Row(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(contentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            badgeContent()
        }
    } else {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            badgeContent()
        }
    }

    if (showAppleWatchPopup.value || showSamsungWatchPopup.value) {
        onSmartWatchIconClick(true)
        SmartWatchPopup(
            isApple = showAppleWatchPopup.value,
            onClose = {
                showAppleWatchPopup.value = false
                showSamsungWatchPopup.value = false
                onSmartWatchIconClick(false)
            },
            resourceId = state.connectWatchResourceId,
            timeoutMilliseconds = 10_000,
            readerLocation = state.rfidReaderLocation,
            gymKitLevel = state.gymKitLevel.intValue,
        )
    }
}

class SmartWatchBankState(context: Context) {
    // defaulting to visible states for previews, on device the model will update them with actual states
    val gymKitLevel = mutableIntStateOf(0)
    val appleWatchState = mutableStateOf(DeviceState.Disconnected)
    val samsungWatchState = mutableStateOf(DeviceState.Disconnected)
    val rfidReaderLocation: RelativeLocation
    val connectWatchResourceId: Int
    private val deviceModel: DeviceModel
    private val marketingModel: MarketingModel

    init {
        val injection = getInjectionEntryPoint(context)
        marketingModel = injection.marketing()
        deviceModel = injection.devices()
        connectWatchResourceId = injection.system().connectWatchResourceId
        rfidReaderLocation = deviceModel.rfidReaderLocation
    }

    fun start() {
        deviceModel.getDeviceState(Device.AppleWatch).listenAndTrigger { appleWatchState.value = it }
        deviceModel.getDeviceState(Device.SamsungWatch).listenAndTrigger { samsungWatchState.value = it }
        marketingModel.gymKitLevel.listenAndTrigger { gymKitLevel.intValue = it }
    }
    fun stop() {
        deviceModel.close()
        marketingModel.close()
    }
}
