@file:OptIn(ExperimentalComposeUiApi::class)

package com.jht.firsttimesetup.ui  //todo change name of file to NetworkSelectUI.kt

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.IntentKeys
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FTSUTextField
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.viewmodels.NetworkSelectViewModel
import com.jht.firsttimesetup.viewmodels.State
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


private fun launchNetworkBroadcast(context: Context) {
    if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
        context.sendBroadcast(Intent(IntentKeys.WIFI_SETTINGS_INTENT))
    } else {
        context.sendBroadcast(Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))
    }
}

@Composable
internal fun NetworkSelectScreen(
    networkVm: NetworkSelectViewModel = hiltViewModel(),
    onNextClicked: () -> Unit = {},
    isEngineeringMode: Boolean = false,
    isEdit: Boolean = false,
    onCancelClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    onSkipFtsu: () -> Unit
) {
    val uiState = networkVm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        networkVm.onViewStarted()
        onDispose {
            networkVm.onViewDismissed()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (uiState.value.state != State.DEFAULT) Modifier.blur(30.dp) else Modifier)
    ) {

        NetworkSettingsView(
            modifier = Modifier.align(Alignment.Center),
            onImportClicked = { networkVm.importWirelessSettings() },
            onExportClicked = { networkVm.exportWirelessSettings() },
            wirelessConnectionName = uiState.value.wirelessNetworkName,
            wiredIpAddress = uiState.value.wiredIpAddress,
            onUpdateUrl = { url -> networkVm.setSpeedTestUrl(url) },
            onTestInternetClicked = { networkVm.onTestInternetClicked() },
            speedTestUrl = uiState.value.speedTestUrl,
            onFacilityIdNavigationClicked = onNextClicked,
            isEngineeringMode = isEngineeringMode,
            onSkipFtsu = onSkipFtsu,
            isEdit = isEdit,
            onCancelClicked = onCancelClicked,
            onSaveClicked = onSaveClicked
        )

        when (uiState.value.state) {
            State.DEFAULT -> {

            }

            State.LOADING -> {
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(.5f)
                            .fillMaxHeight(.8f)
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                    ) {
                        ProgressLoader(
                            modifier = Modifier.fillMaxSize(),
                            title = stringResource(R.string.loading_tc)
                        )
                    }
                }

            }

            State.EXPORT_SUCCESS -> {
                Dialog(
                    onDismissRequest = { networkVm.setState(State.DEFAULT) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {

                    Card(
                        modifier = Modifier
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(50.dp)
                        ) {

                            Text(
                                stringResource(R.string.export_settings_disk_success_pg),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            FTSUButton(
                                label = stringResource(R.string.ok_uc),
                                onClick = { networkVm.setState(State.DEFAULT) }
                            )

                        }
                    }
                }

            }

            State.EXPORT_FAILURE -> {

                Dialog(
                    onDismissRequest = { networkVm.setState(State.DEFAULT) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { testTagsAsResourceId = true },
                        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(50.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.export_settings_disk_fail_pg),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )

                            FTSUButton(
                                modifier = Modifier.testTag("FTSU:NetworkSelection:ExportFail:Button"),
                                label = stringResource(R.string.ok_uc),
                                onClick = { networkVm.setState(State.DEFAULT) }
                            )

                        }
                    }
                }
            }

            State.IMPORT_SUCCESS -> {
                launchNetworkBroadcast(context)
                Dialog(
                    onDismissRequest = { networkVm.setState(State.DEFAULT) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(50.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_settings_success_pg),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )

                            FTSUButton(
                                label = stringResource(R.string.ok_uc),
                                onClick = { networkVm.setState(State.DEFAULT) }
                            )

                        }
                    }
                }

            }

            State.IMPORT_FAILURE -> {
                Dialog(
                    onDismissRequest = { networkVm.setState(State.DEFAULT) },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { testTagsAsResourceId = true },
                        colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(50.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_settings_fail_pg),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )

                            FTSUButton(
                                modifier = Modifier.testTag("FTSU:NetworkSelection:ImportFail:Button"),
                                label = stringResource(R.string.ok_uc),
                                onClick = { networkVm.setState(State.DEFAULT) }
                            )

                        }
                    }
                }

            }

        }

    }
}

@Composable
fun NetworkSettingsView(
    modifier: Modifier = Modifier,
    onImportClicked: () -> Unit = {},
    onExportClicked: () -> Unit = {},
    wirelessConnectionName: String,
    wiredIpAddress: String,
    onTestInternetClicked: () -> Unit = {},
    onUpdateUrl: (newUrl: String) -> Unit = {},
    speedTestUrl: String,
    onFacilityIdNavigationClicked: () -> Unit = {},
    isEngineeringMode: Boolean,
    onSkipFtsu: () -> Unit,
    isEdit: Boolean = false,
    onCancelClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {}
) {

    val localFocusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        //Page Title
        Text(
            stringResource(R.string.network_connection_tc),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTag("FTSU:NetworkSelection:Title:Text")
        )

        // weight(fill = false): the cards keep their natural height when there is room, but are
        // capped at what the title and button row leave, so the buttons can't be squashed.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(intrinsicSize = IntrinsicSize.Max)
                .then(Modifier.width(IntrinsicSize.Min)),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1 / 12f))
            WirelessConnectionCard(
                modifier = Modifier
                    .weight(5 / 12f)
                    .fillMaxHeight(),
                onExportClicked = { onExportClicked() },
                onImportClicked = { onImportClicked() },
                connectionStatus = if (wirelessConnectionName != WifiManager.UNKNOWN_SSID) {
                    wirelessConnectionName
                } else {
                    stringResource(R.string.no_connection_tc)
                },
                isConnected = wirelessConnectionName != WifiManager.UNKNOWN_SSID
            )

            Spacer(
                Modifier.width(32.dp)
            )

            Column(modifier = Modifier.weight(5 / 12f)) {
                WiredConnectionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.4f),
                    isConnected = wiredIpAddress.isNotEmpty(),
                    connectionStatus = wiredIpAddress
                )

                Spacer(modifier = Modifier.height(34.dp))

                InternetSpeedTestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(.6f),
                    onTestInternetClicked = onTestInternetClicked,
                    onUpdateUrl = { url -> onUpdateUrl(url) },
                    url = speedTestUrl
                )
            }

            Spacer(Modifier.weight(1 / 12f))
        }

        if (isEdit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                FTSUButton(
                    label = stringResource(R.string.cancel_tc),
                    modifier = Modifier
                        .widthIn(200.dp.computeSize())
                        .testTag("FTSU:NetworkSelect:Cancel:Button"),
                    onClick = { onCancelClicked() },
                    buttonType = FTSUButtonType.Secondary
                )

                FTSUButton(
                    label = stringResource(R.string.save_tc),
                    modifier = Modifier
                        .widthIn(200.dp.computeSize())
                        .testTag("FTSU:NetworkSelect:Save:Button"),
                    onClick = { onSaveClicked() },
                    buttonType = FTSUButtonType.Primary
                )
            }

        } else {
            BottomButtonRow(
                isEngineeringMode = isEngineeringMode,
                onFacilityIdNavigationClicked = onFacilityIdNavigationClicked,
                onSkipFtsu = onSkipFtsu
            ) //todo make VM var controlled
        }
    }
}

@AllDevicePreviews
@Composable
private fun NetworkSelectScreenPreview() {
    NetworkSettingsView(
        wiredIpAddress = "123.45.678",
        wirelessConnectionName = "Don't connect to this wifi",
        speedTestUrl = "superfast.com",
        onFacilityIdNavigationClicked = {},
        isEngineeringMode = false,
        onSkipFtsu = {}
    )
}

@AllDevicePreviews
@Composable
private fun NetworkSelectScreenEditPreview() {
    NetworkSettingsView(
        isEdit = true,
        wiredIpAddress = "123.45.678",
        wirelessConnectionName = "Don't connect to this wifi",
        speedTestUrl = "superfast.com",
        isEngineeringMode = false,
        onSkipFtsu = {},
        onFacilityIdNavigationClicked = {}

    )
}

@Composable
fun WirelessConnectionCard(
    modifier: Modifier = Modifier,
    connectionStatus: String = stringResource(R.string.no_connection_tc),
    isConnected: Boolean = false,
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit
) {

    val context = LocalContext.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(colorResource(R.color.network_card))
    ) { //Wireless
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) { // Check
                Spacer(Modifier.width(32.dp))
                ConnectionIcon(
                    modifier = Modifier
                        .testTag(if (isConnected) "FTSU:NetworkSelection:WirelessConnected:Image" else "FTSU:NetworkSelection:WirelessDisconnected:Image"),
                    isConnected = isConnected
                )
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        stringResource(R.string.wireless_tc),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.testTag("FTSU:NetworkSelection:Wireless:Text")
                    )
                    Text(
                        connectionStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.testTag("FTSU:NetworkSelection:WirelessConnectionStatus:Text")
                    )
                }
            }
            //Buttons
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp).height(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                FTSUButton(
                    modifier = buttonModifier
                        .testTag("FTSU:NetworkSelection:WirelessSetup:Button"),
                    label = stringResource(R.string.wireless_setup_tc),
                    buttonType = FTSUButtonType.Primary,
                    onClick = { gotToWifiSettings(context) }
                )
                FTSUButton(
                    modifier = buttonModifier
                        .testTag("FTSU:NetworkSelection:ImportWireless:Button"),
                    label = stringResource(R.string.import_wireless_settings_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = { onImportClicked() }
                )
                FTSUButton(
                    modifier = buttonModifier
                        .testTag("FTSU:NetworkSelection:ExportWireless:Button"),
                    label = stringResource(R.string.export_wireless_settings_tc),
                    buttonType = FTSUButtonType.Secondary,
                    onClick = { onExportClicked() }
                )
            }
        }
    }
}

private fun gotToWifiSettings(context: Context) {
    val intent = Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
    // this will hide the Multi Panel of WiFi Settings
    intent.putExtra(":settings:show_fragment_as_subsetting", true)
    context.startActivity(intent)
}

@Composable
fun InternetSpeedTestCard(
    modifier: Modifier = Modifier,
    onUpdateUrl: (newUrl: String) -> Unit,
    onTestInternetClicked: () -> Unit,
    url: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(colorResource(R.color.network_card))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 32.dp, horizontal = 32.dp)
                .fillMaxHeight()
        ) {
            FTSUTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                label = stringResource(R.string.internet_speed_test_tc),
                testTag = "FTSU:NetworkSelection:SpeedTest",
                text = url,
                onTextChange = { url -> onUpdateUrl(url) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri)

            )
            FTSUButton(
                label = stringResource(R.string.test_internet_speed_tc),
                modifier = Modifier
                    .testTag("FTSU:NetworkSelection:TestInternetSpeed:Button")
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                buttonType = FTSUButtonType.Secondary,
                onClick = { onTestInternetClicked() }
            )
        }
    }
}

@Composable
fun WiredConnectionCard(
    modifier: Modifier = Modifier,
    connectionStatus: String = stringResource(R.string.no_connection_tc),
    isConnected: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(colorResource(R.color.network_card))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(vertical = 48.dp, horizontal = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionIcon(
                isConnected = isConnected,
                modifier = Modifier.testTag(if (isConnected) "FTSU:NetworkSelection:WiredConnected:Image" else "FTSU:NetworkSelection:WiredDisconnected:Image")
            )
            Column(modifier = Modifier.padding(horizontal = 14.7.dp)) {
                Text(
                    stringResource(R.string.wired_connection_tc),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.testTag("FTSU:NetworkSelection:WiredConnection:Text")
                )
                Text(
                    if(isConnected) connectionStatus
                    else stringResource(R.string.no_connection_tc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.testTag("FTSU:NetworkSelection:WiredConnectionStatus:Text")
                )
            }
        }
    }
}

@Composable
fun ConnectionIcon(
    modifier: Modifier = Modifier,
    isConnected: Boolean
) {
    if (isConnected) {
        Icon(
            modifier = modifier
                .width(32.dp)
                .aspectRatio(1f),
            painter = painterResource(R.drawable.check_circle),
            contentDescription = "Check Circle",
            tint = colorResource(R.color.success)
        )
    } else {
        Icon(
            modifier = modifier
                .width(32.dp)
                .aspectRatio(1f),
            painter = painterResource(R.drawable.x_circle),
            contentDescription = "X Circle",
            tint = Color.White
        )
    }
}

@Preview
@Composable
private fun WirelessConnectionCardExample() {
    WirelessConnectionCard(onImportClicked = {}, onExportClicked = {})
}

@Preview
@Composable
private fun WiredConnectionCardExample() {
    WiredConnectionCard()
}

@Composable
fun BottomButtonRow(
    isEngineeringMode: Boolean,
    onFacilityIdNavigationClicked: () -> Unit,
    onSkipFtsu: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp.computeSize(), vertical = 24.dp.computeSize()),
        horizontalArrangement = Arrangement.End
    ) {
        FTSUButton(
            modifier = Modifier
                .fillMaxWidth(2 / 12f)
                .testTag("FTSU:NetworkSelection:FacilityId:Button"),
            label = stringResource(R.string.club_id_tc),
            imageEnd = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    "",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp.computeSize())
                )
            },
            onClick = { onFacilityIdNavigationClicked() }
        )
    }
}

@AllDevicePreviews
@Composable
private fun BottomButtons() {
    BottomButtonRow(isEngineeringMode = true, onFacilityIdNavigationClicked = {})
}