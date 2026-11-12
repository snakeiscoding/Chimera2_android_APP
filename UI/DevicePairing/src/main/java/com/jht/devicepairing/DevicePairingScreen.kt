package com.jht.devicepairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.jht.enums.DeviceState
import com.jht.interfaces.models.PairingDevice
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.ThemeSelector
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.AutoSizeText
import com.jht.widgets.MessageDialog

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DevicePairingScreen(
    theme: DynamicThemeData = defaultDynamicTheme,
    machineName: String = "CTM----",
    categories: SnapshotStateList<DeviceCategoryHandler>,
    selectedCategory: DeviceCategoryHandler? = null,
    onSelectCategory: (DeviceCategoryHandler?) -> Unit = {},
    pairingTimedOut: MutableState<Boolean> = mutableStateOf(false),
    safeRegionPx: android.graphics.Rect = android.graphics.Rect(),
    onBack: () -> Unit = {},
) {
    DynamicTheme(theme) {
        // Parent box within which to move the visible child box
        Box(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize(),
        ) {
            val safeRegionOffset = with(LocalDensity.current) {
                if (safeRegionPx.isEmpty) DpOffset.Unspecified
                else DpOffset(safeRegionPx.left.toDp(), safeRegionPx.top.toDp())
            }
            val safeRegionSize = with(LocalDensity.current) {
                if (safeRegionPx.isEmpty) DpSize.Unspecified
                else DpSize(safeRegionPx.width().toDp(), safeRegionPx.height().toDp())
            }
            // keep the UI within the safe region (avoid areas covered by system panels)
            val safeRegionModifier =
                if (safeRegionSize.isSpecified) Modifier.offset(safeRegionOffset.x, safeRegionOffset.y).size(safeRegionSize)
                else Modifier.fillMaxSize()
            // Child box holding actual visible area
            Box(
                modifier = safeRegionModifier,
            ) {
                Column {
                    // top bar container
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.1f)
                            .background(DynamicTheme.colors.onSecondaryContainer),
                    ) {
                        // Top Left Back Button
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_left_white),
                                contentDescription = null,
                                tint = DynamicTheme.colors.text1,
                                modifier = Modifier
                                    .padding(20.dp, 0.dp)
                                    .fillMaxHeight(0.5f)
                                    .aspectRatio(1f)
                            )
                        }

                        // Top Center Text - if text does not size with bar, it could look off, so let's auto size to some percentage of the height
                        AutoSizeText(
                            stringResource(R.string.device_pairing_tc),
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxHeight(0.6f),
                            DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle3
                        )
                    }
                    HorizontalDivider(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp), color = DynamicTheme.colors.gray3
                    )

                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // left tab selector
                        LazyColumn(
                            Modifier
                                .background(DynamicTheme.colors.gray1)
                                .fillMaxWidth(0.25f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(categories, key = { it.type }) { item ->
                                CategorySelector(
                                    category = item,
                                    selected = (item == selectedCategory),
                                    onClick = { onSelectCategory(item) }
                                )
                            }
                        }
                        VerticalDivider(
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp), color = DynamicTheme.colors.gray3
                        )
                        // right tab body
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(DynamicTheme.colors.onSecondaryContainer)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            selectedCategory?.let { category ->
                                if (category.type == DeviceCategory.Ftms) FtmsCategory(machineName)
                                else DeviceList(category)
                            }
                        }
                    }
                }
            }
            if (pairingTimedOut.value) MessageDialog(
                title = stringResource(R.string.device_pairing_tc),
                message = stringResource(R.string.bt_pair_timeout),
                onDismiss = { pairingTimedOut.value = false },
            )
        }
    }
}

@Composable
private fun CategorySelector(
    category: DeviceCategoryHandler,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ThemeSelector(selected = selected, testTag = category.testTag, onClick = onClick,
        modifier = Modifier.aspectRatio(3f),
        contentModifier = Modifier.padding(10.dp),
    ) {
        Column(
            Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            Icon(
                painterResource(category.iconId),
                null,
                Modifier
                    .fillMaxHeight(0.5f)
                    .aspectRatio(1f),
                tint = if (it) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
            )
            Text(stringResource(category.labelId), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
        }
    }
}

@Composable
private fun DeviceList(
    category: DeviceCategoryHandler,
) {
    val connected by remember(category) { derivedStateOf { category.devices.filter { it.connectionState.value == DeviceState.Connected } } }
    val notConnected by remember(category) { derivedStateOf { category.devices.filter { it.connectionState.value != DeviceState.Connected } } }
    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.my_devices_tc), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle2)
        Text(stringResource(R.string.note_help_pg), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
        // connected device
        if (connected.isEmpty()) NoDevices("NoDevices:Connected")
        else connected.forEach {
            DeviceItem(category, it, true) { category.onClick(it) }
        }
        Spacer(Modifier.size(20.dp))
        Text(stringResource(R.string.available_devices_tc), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle2)
        // found devices
        if (notConnected.isEmpty()) NoDevices("NoDevices:Disconnected")
        else LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(notConnected, key = {it.id}) {item->
                DeviceItem(category, item) { category.onClick(item) }
            }
        }
    }
}


@Composable
private fun NoDevices(
    testTag: String = ""
) {
    // Try to approximate the size of a DeviceItem
    Row(
        Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .padding(10.dp),
    ) {
        Box(Modifier.height(40.dp))
        Text(
            stringResource(R.string.none_uc), // no_devices_found_tc, none_uc ?
            color = DynamicTheme.colors.text1.copy(0.5f), style = DynamicTheme.typography.textStyle2
        )
    }
}

@Composable
private fun DeviceItem(
    category: DeviceCategoryHandler,
    deviceHandler: DeviceHandler,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Box(
        Modifier
            .testTag("Device:${deviceHandler.id}")
            .fillMaxWidth()
            .background(DynamicTheme.colors.gray1)
            .clickable(
                enabled = (deviceHandler.connectionState.value != DeviceState.Connected), // creative wants to try limiting disconnect to touching icon
                onClick = onClick,
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .align(Alignment.Center)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Custom image or category icon
            val iconModifier = Modifier
                .padding(10.dp)
                .size(30.dp)
            if (deviceHandler.iconId != 0) {
                Image(
                    painterResource(deviceHandler.iconId),
                    null,
                    iconModifier,
                )
            } else {
                Icon(
                    painterResource(category.iconId),
                    null,
                    iconModifier,
                    tint = if (selected) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
                )
            }
            // Name
            Text(
                deviceHandler.label,
                Modifier
                    .padding(10.dp, 0.dp)
                    .weight(1f),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle2
            )
            // Status
            when (deviceHandler.connectionState.value) {
                DeviceState.Connected -> {
                    Box( // creative wants to try limiting disconnect to touching icon
                        Modifier
                            .testTag("Device:${deviceHandler.id}:Disconnect")
                            .fillMaxHeight()
                            .clickable(onClick = onClick)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_cancelled),
                            null,
                            iconModifier,
                            tint = DynamicTheme.colors.text1,
                        )
                    }
                }
                DeviceState.Connecting -> {
                    CircularProgressIndicator(iconModifier, color = DynamicTheme.colors.text1)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FtmsCategory(
    machineName: String,
) {
    val selectMessage = stringResource(R.string.select_tc) + " \"$machineName\" " + stringResource(R.string.ftms_instruction_tc)
    Column(
        Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // image
        Image(
            painterResource(R.drawable.ic_ftms_training_app_graphic),
            null,
        )
        // message
        Text(stringResource(R.string.ftms_description_pg), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1, textAlign = TextAlign.Center)
        // machine name box
        Column(
            Modifier
                .background(DynamicTheme.colors.gray2)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.machine_name_tc), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle2)
            Text(machineName, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle3)
            Text(selectMessage, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1, textAlign = TextAlign.Center)
        }
    }
}


//region PREVIEWS
@Composable
private fun fakeData(): SnapshotStateList<DeviceCategoryHandler> {
    val fakeDevice = object : PairingDevice {
        override val deviceName: String = ""
        override val deviceId: String = ""
        override var connectionState: DeviceState = DeviceState.Disconnected
    }
    val connected = DeviceHandler(fakeDevice, "Connected Device", "0")
    connected.connectionState.value = DeviceState.Connected
    val connecting = DeviceHandler(fakeDevice, "Connecting Device", "1")
    connecting.connectionState.value = DeviceState.Connecting
    val disconnect = DeviceHandler(fakeDevice, "Disconnected Device", "2")
    val devices = listOf(connected, connecting, disconnect)
    val firstCategory = DeviceCategoryHandler(R.drawable.ic_headphones_enabled, R.string.speakers_headphones_tc, "btPairAudioOut", type = DeviceCategory.AudioOut)
    firstCategory.devices.addAll(devices)
    val categories = remember { mutableStateListOf(
        firstCategory,
        DeviceCategoryHandler(R.drawable.ic_heart_rate_monitor_enabled, R.string.heart_rate_monitors_tc, "btPairHr", type = DeviceCategory.HeartRate),
        DeviceCategoryHandler(R.drawable.ic_ftms, R.string.ftms_tc, "btPairFtms", type = DeviceCategory.Ftms),
    ) }
    return categories
}

@AllDevicePreviews
@Composable
private fun PreviewDevicePairingScreen() {
    val categories = fakeData()
    DevicePairingScreen(
        categories = categories,
        selectedCategory = categories[0],
    )
}

@AllDevicePreviews
@Composable
private fun PreviewFtms() {
    val categories = fakeData()
    DevicePairingScreen(
        categories = categories,
        selectedCategory = categories.last(),
    )
}
//endregion PREVIEWS
