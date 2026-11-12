package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FacilityInfo
import com.jht.firsttimesetup.viewmodels.FacilityAddress
import com.jht.ui.AllDevicePreviews
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConfirmFacilityScreen(
    modifier: Modifier = Modifier,
    facilityId: String,
    facilityName: String,
    facilityAddress: FacilityAddress?,
    onReEnterClicked: () -> Unit = {},
    onConfirmClicked: () -> Unit = {},
    isImport: Boolean,
    onCancelClicked: () -> Unit
) {

    Card(
        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.network_card)),
        modifier = Modifier.semantics { testTagsAsResourceId = true }
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Details scroll only when the screen is too short (e.g. the height-capped Dialog on 1366x768
            // consoles). fill = false lets the card wrap its content otherwise and keeps the buttons visible.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp.computeSize()))

                Icon(
                    painter = painterResource(R.drawable.building),
                    contentDescription = "",
                    tint = Color.White
                )

                Spacer(Modifier.height(32.dp.computeSize()))

                Text(
                    color = Color.White,
                    text = stringResource(R.string.confirm_facility_tc),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .testTag("FTSU:ConfirmFacility:Title:Text")
                )

                Spacer(Modifier.height(16.dp.computeSize()))


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Spacer(Modifier.fillMaxWidth(.1f))
                    Text(
                        color = Color.White,
                        text = stringResource(R.string.enter_facility_id_connected_pg),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth(.9f)
                            .testTag("FTSU:ConfirmFacility:Body:Text"),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.fillMaxWidth(.1f))
                }

                Spacer(Modifier.height(32.dp.computeSize()))


                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Spacer(Modifier.fillMaxWidth(.2f))

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier,
                    ) {


                        FacilityInfo(
                            testTag = "FTSU:ConfirmFacility:FacilityId",
                            label = stringResource(R.string.club_id_tc),
                            value = facilityId
                        )

                        Spacer(Modifier.height(40.dp.computeSize()))

                        FacilityInfo(
                            testTag = "FTSU:ConfirmFacility:FacilityName",
                            label = stringResource(R.string.facility_name_tc),
                            value = facilityName
                        )

                        Spacer(Modifier.height(40.dp.computeSize()))

                        facilityAddress?.longAddress?.let {
                            FacilityInfo(
                                testTag = "FTSU:ConfirmFacility:FacilityLocation",
                                label = stringResource(R.string.facility_location_tc),
                                value = it
                            )
                        }

                    }
                }

            }

            Spacer(Modifier.height(64.dp.computeSize()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.Center,
            ) {

                Spacer(Modifier.weight(1f))

                if (isImport) {
                    FTSUButton(
                        modifier = Modifier
                            .testTag("FirstTimeSetup:ConfirmFacility:Cancel:Button")
                            .padding(horizontal = 16.dp.computeSize())
                            .weight(1.4f),
                        label = stringResource(R.string.cancel_tc),
                        onClick = onCancelClicked,
                        buttonType = FTSUButtonType.Secondary
                    )
                } else {
                    FTSUButton(
                        modifier = Modifier
                            .testTag("FTSU:ConfirmFacility:ReEnter:Button")
                            .padding(horizontal = 16.dp.computeSize())
                            .weight(1.4f),
                        label = stringResource(R.string.re_enter_tc),
                        onClick = onReEnterClicked,
                        buttonType = FTSUButtonType.Secondary
                    )
                }

                FTSUButton(
                    modifier = Modifier
                        .testTag("FTSU:ConfirmFacility:Confirm:Button")
                        .padding(horizontal = 16.dp.computeSize())
                        .weight(1.4f),
                    label = stringResource(R.string.confirm_tc),
                    onClick = onConfirmClicked,
                    buttonType = FTSUButtonType.Primary
                )

                Spacer(Modifier.weight(1f))

            }

            Spacer(Modifier.height(64.dp.computeSize()))

        }
    }

}

@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun ConfirmFacilityScreenImportPreview() {
    val previewFacilityAddress = FacilityAddress(
        city = "City", street1 = "Street 1", street2 = "Street 2",
        country = "Country", countryIso = "C_ISO", countryIso3 = "C_ISO3",
        postalCode = "0", state = "State", stateIso = "S_ISO", shortAddress = "City, State",
        longAddress = "Street 1\nCity, State, Postal Code\nCountry"
    )
    ConfirmFacilityScreen(
        modifier = Modifier,
        facilityId = "123456",
        facilityName = "Planet Fitness Madison West",
        facilityAddress = previewFacilityAddress,
        isImport = true,
        onCancelClicked = {}
    )
}

@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun ConfirmFacilityScreenPreview() {
    val previewFacilityAddress = FacilityAddress(
        city = "City", street1 = "Street 1", street2 = "Street 2",
        country = "Country", countryIso = "C_ISO", countryIso3 = "C_ISO3",
        postalCode = "0", state = "State", stateIso = "S_ISO", shortAddress = "City, State",
        longAddress = "Street 1\nCity, State, Postal Code\nCountry"
    )
    ConfirmFacilityScreen(
        modifier = Modifier,
        facilityId = "123456",
        facilityName = "Planet Fitness Madison West",
        facilityAddress = previewFacilityAddress,
        isImport = false,
        onCancelClicked = {}
    )
}

@AllDevicePreviews
@Composable
private fun ConfirmFacilityScreenInDialogPreview() {
    val previewFacilityAddress = FacilityAddress(
        city = "City", street1 = "Street 1", street2 = "Street 2",
        country = "Country", countryIso = "C_ISO", countryIso3 = "C_ISO3",
        postalCode = "0", state = "State", stateIso = "S_ISO", shortAddress = "City, State",
        longAddress = "Street 1\nCity, State, Postal Code\nCountry"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ConfirmFacilityScreen(
            modifier = Modifier.fillMaxWidth(.5f),
            facilityId = "123456",
            facilityName = "Planet Fitness Madison West",
            facilityAddress = previewFacilityAddress,
            isImport = false,
            onCancelClicked = {}
        )
    }
}
