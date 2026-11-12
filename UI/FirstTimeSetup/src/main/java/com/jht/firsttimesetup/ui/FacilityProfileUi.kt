package com.jht.firsttimesetup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.DropDownMenu
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FacilityInfo
import com.jht.firsttimesetup.ui.commoncomponents.ScrollableCardContent
import com.jht.firsttimesetup.viewmodels.DigitalProduct
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.FacilityProfileViewModel
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlinx.serialization.Serializable

/**
 * Model for all UI displayed information in Facility Profile UI
 */
@Serializable
data class FacilityProfile(
    val timeZone: String = "",
    val timeFormat: String = "",
    val dateFormat: String = "",
    val unitFormat: String = "",
    val consoleMode: String = "",
    val ledColor: Int? = null,
    val ledHexString: String? = null,
    val tvConnectionType: String = "",
    val numberOfChannels: String = "",
    val defaultChannel: String = "",
    val digitalProducts: List<DigitalProduct> = emptyList(),
    val wirelessConnectionName: String = "",
    val wiredConnectionName: String = "",
    val showLedColor: Boolean = false,
)

@Composable
fun FacilityProfileUi(
    modifier: Modifier,
    vm: FacilityProfileViewModel = hiltViewModel(),
    onBackClicked: () -> Unit = {},
    onImportFacility: () -> Unit,
    onManuallyConfigureClicked: () -> Unit,
    facility: Facility?,
    isEdit: Boolean,
    onCancelClicked: () -> Unit
) {

    val state = vm.uiState.collectAsStateWithLifecycle()

    facility?.let {
        LaunchedEffect(facility) {
            vm.setFacility(facility)
        }
    }

    val profile = state.value.profile

    FacilityProfileView(
        modifier = modifier,
        profile = profile,
        onBackClicked = onBackClicked,
        onImportFacilityProfileClicked = {
            if (vm.uiState.value.hasCustomUiExperience) {
                vm.showCustomUiExperienceDialog()
            } else {
                val clubId = facility?.facilityId?.toIntOrNull()
                clubId?.let {
                    vm.setClubId(it)
                    vm.importChannelsFromCore(it)
                }
                vm.setLocalSettingsToImportedSettings()
                onImportFacility()
            }
        },
        getTimeZone = { profile.timeZone },
        onManuallyConfigureClicked = onManuallyConfigureClicked,
        isEdit = isEdit,
        onCancelClicked = onCancelClicked,
        profileHasCustomUiExperience = state.value.hasCustomUiExperience,
        showCustomUiExperienceDialog = state.value.showUiExperienceSelectionDialog,
        onShowDialog = { vm.showCustomUiExperienceDialog() },
        onDismissRequest = { vm.hideCustomUiExperienceDialog() },
        onUiExperienceSelected = { vm.setUiExperience(it) },
        overrideUuidMap = vm.getUiExperiences()
    )

}


/**
 * UI associated with facility profile detail page
 * @param [profile]
 * @param [onBackClicked] -> navigates back to facility ID search
 * @param [onImportFacilityProfileClicked] action to navigate forward
 * @param [onImportFromUsbClicked] action to import setting
 * @param [onManuallyConfigureClicked] action for manual process
 */
@Composable
fun FacilityProfileView(
    modifier: Modifier = Modifier,
    profile: FacilityProfile,
    onBackClicked: () -> Unit = {},
    onImportFromUsbClicked: () -> Unit = {},
    onManuallyConfigureClicked: () -> Unit = {},
    onImportFacilityProfileClicked: () -> Unit = {},
    getTimeZone: () -> String = { "" },
    isEdit: Boolean,
    onCancelClicked: () -> Unit = {},
    profileHasCustomUiExperience: Boolean = false,
    showCustomUiExperienceDialog: Boolean = false,
    onShowDialog: () -> Unit = {},
    onUiExperienceSelected: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    overrideUuidMap: Map<String, String> = emptyMap()
) {


    if(showCustomUiExperienceDialog) {
        UiExperienceSelectDialog(
            {
                onDismissRequest()
                onImportFacilityProfileClicked()
            },
            onUiExperienceSelected = onUiExperienceSelected,
            uuidMap = overrideUuidMap
        )
    }

    Column(
        modifier = modifier
    ) {
        Spacer(Modifier.height(45.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.clickable {
                    onBackClicked()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_white),
                    contentDescription = "Back arrow for navigation",
                    tint = Color.White
                )

                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.back_tc),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.testTag("FTSU:FacilityProfile:Back:Button")
                )
            }

            Text(
                stringResource(
                    R.string.facility_profile_tc
                ),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("FTSU:FacilityProfile:Title:Text"),
                color = Color.White
            )
        }

        Spacer(Modifier.height(36.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp.computeSize()),
            modifier = Modifier.weight(1f)
        ) {

            //Network card

            Card(
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            ) {

                ScrollableCardContent(contentPadding = PaddingValues(32.dp)) {

                    Text(
                        stringResource(R.string.network_tc),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("FTSU:FacilityProfile:NetworkTitle:Text")
                    )

                    Spacer(Modifier.height(32.dp))

                    FacilityInfo(
                        label = stringResource(R.string.wireless_tc),
                        value = profile.wirelessConnectionName,
                        testTag = "FTSU:FacilityProfile:Wireless"
                    )

                    Spacer(Modifier.height(32.dp))

                    FacilityInfo(
                        label = stringResource(R.string.wired_tc),
                        value = profile.wiredConnectionName.ifEmpty { stringResource(R.string.no_connection_tc) },
                        testTag = "FTSU:FacilityProfile:Wired"
                    )

                }

            }

            // General settings card
            Card(
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2f),
            ) {

                ScrollableCardContent(
                    contentPadding = PaddingValues(32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Text(
                        stringResource(R.string.general_settings_tc),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("FTSU:FacilityProfile:GeneralSettingsTitle:Text")
                    )

                    FacilityInfo(
                        label = stringResource(R.string.time_zone_tc),
                        value = getTimeZone(),
                        testTag = "FTSU:FacilityProfile:TimeZone"
                    )

                    FacilityInfo(
                        label = stringResource(R.string.time_format_tc),
                        value = profile.timeFormat,
                        testTag = "FTSU:FacilityProfile:TimeFormat"
                    )

                    FacilityInfo(
                        label = stringResource(R.string.dateformat_tc),
                        value = profile.dateFormat,
                        testTag = "FTSU:FacilityProfile:DateFormat"
                    )

                    FacilityInfo(
                        label = stringResource(R.string.unit_of_measurement_tc),
                        value = profile.unitFormat,
                        testTag = "FTSU:FacilityProfile:UnitMeasurement"
                    )

                    profile.ledColor?.let {
                        if (profile.showLedColor) {
                            HorizontalDivider(
                                Modifier
                                    .height(1.dp)
                                    .fillMaxWidth(),
                                DividerDefaults.Thickness,
                                color = colorResource(R.color.secondary_btn)
                            )

                            Text(
                                stringResource(R.string.appearance_tc),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.testTag("FTSU:FacilityProfile:AppearanceTitle:Text")
                            )
                            //LED color preview and hex
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {

                                Box(
                                    modifier = Modifier
                                        .padding(0.dp)
                                        .size(
                                            12.dp.computeSize()
                                        )
                                        .drawBehind {
                                            drawCircle(
                                                color = Color(it)
                                            )
                                        },
                                    contentAlignment = Alignment.Center,
                                    content = {

                                    }
                                )

                                Spacer(Modifier.width(4.dp))

                                profile.ledHexString?.let {
                                    Text(
                                        profile.ledHexString,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("FTSU:FacilityProfile:LedColor:Text")

                                    )

                                }
                            }
                        }
                    }

                }

            }

            //Tv setup
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {


                    ScrollableCardContent(contentPadding = PaddingValues(32.dp)) {

                        Text(
                            stringResource(R.string.tv_setup_tc),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("FTSU:FacilityProfile:TvSetupTitle:Text")
                        )

                        Spacer(Modifier.height(32.dp))


                        FacilityInfo(
                            label = stringResource(R.string.connection_type_tc),
                            value = profile.tvConnectionType,
                            testTag = "FTSU:FacilityProfile:ConnectionType"
                        )

                        Spacer(Modifier.height(32.dp))

                        FacilityInfo(
                            label = stringResource(R.string.number_of_channels_tc),
                            value = profile.numberOfChannels,
                            testTag = "FTSU:FacilityProfile:NumberOfChannels"
                        )

                        Spacer(Modifier.height(32.dp))

                        FacilityInfo(
                            label = stringResource(R.string.default_channel_tc),
                            value = profile.defaultChannel,
                            testTag = "FTSU:FacilityProfile:DefaultChannel"
                        )
                    }

                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {

                    ScrollableCardContent(contentPadding = PaddingValues(32.dp)) {

                        Text(
                            stringResource(R.string.digital_products_tc),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("FTSU:FacilityProfile:DigitalProductsTitle:Text")
                        )

                        Spacer(Modifier.height(32.dp))

                        profile.digitalProducts.forEachIndexed { index, product ->
                            if (product.isAvailable) {
                                Text(
                                    product.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.testTag("FTSU:FacilityProfile:DigitalProduct:$index:Text")
                                )

                                Spacer(Modifier.height(16.dp))
                            }
                        }


                    }

                }

            }

        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (!isEdit) {
                    FTSUButton(
                        modifier = Modifier.testTag("FTSU:FacilityProfile:Skip:Button"),
                        label = stringResource(R.string.manually_configure_settings_tc),
                        buttonType = FTSUButtonType.Secondary,
                        onClick = { onManuallyConfigureClicked() }
                    )

                } else {
                    FTSUButton(
                        modifier = Modifier.testTag("FTSU:FacilityProfile:Cancel:Button"),
                        label = stringResource(R.string.cancel_tc),
                        buttonType = FTSUButtonType.Secondary,
                        onClick = { onCancelClicked() }
                    )
                }

                Spacer(Modifier.width(48.dp))

                FTSUButton(
                    modifier = Modifier.testTag("FTSU:FacilityProfile:Import:Button"),
                    label = if (isEdit) {
                        stringResource(R.string.update_profile)
                    } else {
                        stringResource(R.string.import_facility_profile_tc)
                    },
                    buttonType = if (profileHasCustomUiExperience) FTSUButtonType.Primary else FTSUButtonType.Secondary,
                    onClick = {
                        if (profileHasCustomUiExperience) {
                            onShowDialog()
                        } else {
                            onImportFacilityProfileClicked()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UiExperienceSelectDialog(
    onDismissRequest: () -> Unit = {},
    onUiExperienceSelected: (String) -> Unit = {},
    uuidMap: Map<String, String> = emptyMap()
) {
    var dropdownMenuSize by remember { mutableStateOf(Size.Zero) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedOverrideKey by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ){
        Card(
            modifier = Modifier
                .padding(32.dp)
                .width(intrinsicSize = IntrinsicSize.Max),
            colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
        ) {
            Column(
                modifier = Modifier
                    .padding(64.dp)
                    .width(intrinsicSize = IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //title
                Text(
                    text = "What UI experience should be shown?",
                    color = colorResource(R.color.light_text),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("FTSU:FacilityProfile:UiExperienceDialog:Title:Text")
                )

                Spacer(Modifier.height(32.dp))

                //UI option selector
                DropDownMenu(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            dropdownMenuSize = coordinates.size.toSize()
                        },
                    title = "UI Experience",
                    menu = uuidMap.keys.toList(),
                    testTag = "FTSU:FacilityProfile:UiExperience:DropDown",
                    menuWidth = dropdownMenuSize,
                    expandMenu = isDropdownExpanded,
                    onExpandedMenuItemSelected = { index ->
                        selectedIndex = index
                        selectedOverrideKey = uuidMap.keys.toList()[index]
                        isDropdownExpanded = false
                    },
                    onExpandClicked = { isDropdownExpanded = it },
                    selectedItemIndex = selectedIndex
                )

                Spacer(Modifier.height(32.dp))

                //button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    //cancel button
                    FTSUButton(
                        modifier = Modifier.weight(1f),
                        label = "Cancel",
                        buttonType = FTSUButtonType.Secondary,
                        onClick = onDismissRequest
                    )

                    Spacer(Modifier.width(64.dp))

                    //select button
                    FTSUButton(
                        modifier = Modifier.weight(1f),
                        label = "Select",
                        onClick = {
                            val selectedOption = uuidMap[selectedOverrideKey] ?: "default"
                            onUiExperienceSelected(selectedOption)
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun FacilityProfileViewPreview() {
    val profile = FacilityProfile(
        digitalProducts = listOf(
            DigitalProduct("ENGAGE_360", true),
            DigitalProduct("ASSET_MANAGEMENT", true)
        ),
        defaultChannel = "Fox Espanol",
        numberOfChannels = "9001",
        tvConnectionType = "US5 (ProCoax)",
        ledColor = 0xFFFFFF,
        consoleMode = "Dark",
        unitFormat = "Imperial",
        timeFormat = "12 - Hour",
        timeZone = "(GMT=1:00) Belgrabe, Bratislava, Budapest, Hungary",
        dateFormat = "dd/mm/yyyy"
    )
    FacilityProfileView(
        profile = profile, modifier = Modifier.fillMaxSize(),
        isEdit = false
    )
}

@AllDevicePreviews
@Composable
private fun FacilityProfileViewEditPreview() {
    val profile = FacilityProfile(
        digitalProducts = listOf(
            DigitalProduct("ENGAGE_360", true),
            DigitalProduct("ASSET_MANAGEMENT", true)
        ),
        defaultChannel = "Fox Espanol",
        numberOfChannels = "9001",
        tvConnectionType = "US5 (ProCoax)",
        ledColor = 0xFFFFFF,
        consoleMode = "Dark",
        unitFormat = "Imperial",
        timeFormat = "12 - Hour",
        timeZone = "(GMT=1:00) Belgrabe, Bratislava, Budapest, Hungary",
        dateFormat = "dd/mm/yyyy"
    )
    FacilityProfileView(
        profile = profile, modifier = Modifier.fillMaxSize(),
        isEdit = true
    )
}

@AllDevicePreviews
@Composable
private fun CustomUiExperienceSelectionDialog() {
    UiExperienceSelectDialog()
}