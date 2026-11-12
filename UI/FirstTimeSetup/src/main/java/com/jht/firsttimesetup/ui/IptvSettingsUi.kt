package com.jht.firsttimesetup.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FTSUTextField
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.ScrollHintOverlay

/**
 * Ui model for iptv dialog
 */
data class IptvSettings(
    val version: String,
    val tunerMacAddress: String,
    val tunerIpAddress: String,
    val subnetMask: String,
    val dns1: String,
    val dns2: String,
    val gateway: String
)

/**
 * @param [onRebootClicked] -> TBD
 * @param [onExitClicked] -> exits the dialog
 * @param [onSaveClicked] -> Saves input settings
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IptvSettingsView(
    modifier: Modifier = Modifier,
    onExitClicked: () -> Unit = {},
    onRebootClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
) {

    var dhcpSelected by remember { mutableStateOf(false) }

    var tunerIpAddress by remember { mutableStateOf("0.0.0.0") }

    var subnetMask by remember { mutableStateOf("0.0.0.0") }

    var dns1 by remember { mutableStateOf("0.0.0.0") }

    var dns2 by remember { mutableStateOf("0.0.0.0") }

    var gateway by remember { mutableStateOf("0.0.0.0") }

    val iptvSettings = IptvSettings(
        version = "N/A",
        tunerMacAddress = "64.e4.a5.30.44.a3",
        tunerIpAddress = "64.e4.a5.30.44.a3",
        subnetMask = "64.e4.a5.30.44.a3",
        dns1 = "0.0.0.0",
        dns2 = "0.0.0.0",
        gateway = "0.0.0.0"
    )

    val localFocusManager = LocalFocusManager.current


    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
        modifier = modifier.padding(16.dp).pointerInput(Unit) {
            detectTapGestures(onTap = {
                localFocusManager.clearFocus()
            })
        }.semantics { testTagsAsResourceId = true },

    ) {
        val scrollState = rememberScrollState()
        ScrollHintOverlay(
            state = scrollState,
            scrimColor = colorResource(R.color.language_card_color),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                    enabled = true,
                    state = scrollState
                )
            ) {

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onExitClicked) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Exit dialog button",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.iptv_channel_setup_tc),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.testTag("FTSU:Iptv:Title:Text")
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.iptv_function_explained_pg),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("FTSU:Iptv:Body:Text")
                    )

                    Spacer(Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsNameAndDisplay(
                            modifier = Modifier.weight(1f).testTag("FTSU:Iptv:Version:Text"),
                            setting = stringResource(R.string.version_tc),
                            value = iptvSettings.version
                        )
                        SettingsNameAndDisplay(
                            modifier = Modifier.weight(1f).testTag("FTSU:Iptv:TunerMacAddress:Text"),
                            setting = stringResource(R.string.tuner_mac_address_tc),
                            value = iptvSettings.tunerMacAddress
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FTSUTextField(
                            modifier = Modifier.weight(1f).testTag("FTSU:Iptv:TunerIpAddress:Button"),
                            label = stringResource(R.string.tuner_ip_address_tc),
                            text = tunerIpAddress,
                            onTextChange = {
                                tunerIpAddress = it
                            },
                            testTag = "FTSU:Iptv:TunerIpAddress"
                        )

                        FTSUTextField(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.subnet_mask_tc),
                            text = subnetMask,
                            onTextChange = {
                                subnetMask = it
                            },
                            testTag = "FTSU:Iptv:TunerSubnet"
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FTSUTextField(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.dns1_uc),
                            text = dns1,
                            onTextChange = {
                                dns1 = it
                            },
                            testTag = "FTSU:Iptv:Dns1"
                        )

                        FTSUTextField(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.dns2_uc),
                            text = iptvSettings.dns2,
                            onTextChange = {
                                dns2 = it
                            },
                            testTag = "FTSU:Iptv:Dns2"
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FTSUTextField(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.gateway_tc),
                            text = gateway,
                            onTextChange = {
                                gateway = it
                            },
                            testTag = "FTSU:Iptv:Gateway"
                        )

                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(50.dp))

                    //DHCP check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            modifier = Modifier.testTag("FTSU:Iptv:dhcpSelected:Button").size(48.dp),
                            onCheckedChange = { isSelected -> dhcpSelected = isSelected },
                            checked = dhcpSelected,
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = Color.Black,
                                checkedColor = Color.White,
                                uncheckedColor = Color.White
                            )
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            modifier = Modifier.testTag("FTSU:Iptv:DHCP:Text"),
                            text = stringResource(R.string.dhcp1_ac),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(Modifier.height(50.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        FTSUButton(
                            onClick = onRebootClicked,
                            buttonType = FTSUButtonType.Secondary,
                            label = stringResource(R.string.reboot_stb_tc),
                            modifier = Modifier
                                .testTag("FTSU:Iptv:RebootStb:Button").widthIn(200.dp)
                        )

                        Spacer(Modifier.width(48.dp))

                        FTSUButton(
                            onClick = onSaveClicked,
                            buttonType = FTSUButtonType.Primary,
                            label = stringResource(R.string.save_tc),
                            modifier = Modifier
                                .testTag("FTSU:Iptv:Savebutton:Button").widthIn(200.dp)
                        )

                    }
                }
            }
        }
    }

}

@AllDevicePreviews
@Composable
private fun IptvSettingsViewPreview(){
    IptvSettingsView(
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun SettingsNameAndDisplay(
    modifier: Modifier = Modifier,
    setting: String,
    value: String,
    settingColor: Color = Color.White,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(setting, color = settingColor, style = MaterialTheme.typography.labelSmall)
        Text(value, color = valueColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
@Preview
private fun SettingsNameAndDisplayPreview() {
    SettingsNameAndDisplay(
        setting = "Version",
        value = "N/A"
    )
}