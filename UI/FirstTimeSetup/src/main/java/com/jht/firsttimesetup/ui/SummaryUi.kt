package com.jht.firsttimesetup.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FacilityInfo
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.ui.commoncomponents.ScrollableCardContent
import com.jht.firsttimesetup.viewmodels.DigitalProduct
import com.jht.firsttimesetup.viewmodels.ExportProfileState
import com.jht.firsttimesetup.viewmodels.Facility
import com.jht.firsttimesetup.viewmodels.FacilityAddress
import com.jht.firsttimesetup.viewmodels.SummaryViewModel
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

internal data class Summary(
    val facilityId: String = "",
    val facilityName: String = "",
    val facilityAddress: FacilityAddress? = null,
    val networkNameWireless: String? = null,
    val networkNameWired: String? = null,
    val consoleType: String = "",
    val frame: String = "",
    val model: String = "",
    val consoleSerialNumber: String = "",
    val frameSerialNumber: String = "",
    val timeZone: String = "",
    val timeFormat: String = "",
    val dateFormat: String = "",
    val unitFormat: String = "",
    val consoleMode: String = "",
    val tvConnectionType: String = "",
    val numberOfChannels: String = "",
    val defaultChannel: String = "",
    val ledColor: Int? = null,
    val ledColorHex: String = "",
    val digitalProducts: List<DigitalProduct> = emptyList(),
    val bridgeSerialNumber: String? = null,
    val isSerialNumberTupleValid: Boolean = true,
)

internal enum class SummaryEditType {
    FacilityProfile,
    Machine,
    Network,
    GeneralSettings,
    TVSetup,
}


@Composable
internal fun SummaryUiView(
    modifier: Modifier = Modifier,
    vm: SummaryViewModel = hiltViewModel(),
    onEditClicked: (SummaryEditType) -> Unit,
    facilityProfile: Facility? = null,
    shouldInstallUpdate: Boolean,
    shouldImportFacilitySettings: Boolean
) {

    val uiState = vm.uiState.collectAsStateWithLifecycle()

    val summary = uiState.value.summary

    val lifecycleOwner = LocalLifecycleOwner.current

    val exportState = vm.exportState.collectAsStateWithLifecycle(ExportProfileState.DEFAULT, lifecycleOwner)

    /***
     * Sets facility profile for viewmodel to save settings
     */
    LaunchedEffect(facilityProfile, shouldImportFacilitySettings) {
        if (shouldImportFacilitySettings) {
            vm.setFacility(facilityProfile)
        }
    }

    LaunchedEffect(shouldInstallUpdate) {
        vm.shouldUpdate = shouldInstallUpdate
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                //used to refresh user selected option
                vm.updateUi()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (exportState.value != ExportProfileState.DEFAULT) {
        Dialog(
            onDismissRequest = { vm.onDismissExportDialog() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            )
        ) {

            Card(
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.network_card)),
                modifier = Modifier.fillMaxSize(.5F)
            ) {
                when(exportState.value) {
                    ExportProfileState.DEFAULT -> {}
                    ExportProfileState.EXPORTING -> {
                        ProgressLoader(
                            title = stringResource(R.string.export_profile_pg),
                            modifier = Modifier.padding(bottom = 20.dp.computeSize()).fillMaxSize()
                        )
                    }
                    ExportProfileState.EXPORT_FAILED -> {
                        ExportFailureView(
                            onDismiss = { vm.onDismissExportDialog() }
                        )
                    }
                    ExportProfileState.EXPORT_SUCCESS -> {
                        ExportSuccessView(
                            onDismiss = { vm.onDismissExportDialog() }
                        )
                    }
                }

            }

        }
    }

    SummaryView(
        modifier = modifier,
        summary = summary,
        onEditClicked = onEditClicked,
        onFinishSetupClicked = { vm.onFinishClicked() },
        facilityProfile = facilityProfile,
        onExportUsbClicked = { vm.onExportProfile() }
    )

}

/**
 * Confirmation view for failed profile export to usb
 */
@Composable
fun ExportFailureView(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp.computeSize()).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        //title
        Text(
            stringResource(R.string.export_title_tc),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge
        )
        //body
        Text(stringResource(
            R.string.export_error_body_pg),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium
        )
        //dismiss button
        FTSUButton(
            label = stringResource(R.string.ok_uc),
            buttonType = FTSUButtonType.Primary,
            onClick = onDismiss,
            modifier = Modifier.widthIn(200.dp)
        )

    }
}

/**
 * Confirmation view for successful profile export to usb
 */
@Composable
fun ExportSuccessView(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(32.dp.computeSize()).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        //title
        Text(
            stringResource(R.string.export_title_tc),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge
        )
        //body
        Text(stringResource(
            R.string.export_success_body_pg),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium
        )
        //dismiss button
        FTSUButton(
            label = stringResource(R.string.ok_uc),
            buttonType = FTSUButtonType.Primary,
            onClick = onDismiss,
            modifier = Modifier.widthIn(200.dp)
        )

    }
}


/**
 * Summary view allows users to edit previously made choices in FTSU
 * @param [onExportUsbClicked] -> handles exporting setting to usb
 * @param [onFinishSetupClicked] -> for now should just navigate back to main app flow
 * @param [onEditClicked] -> navigates to detail edit flow based on parameter [SummaryEditType]
 */
@Composable
internal fun SummaryView(
    modifier: Modifier = Modifier,
    summary: Summary,
    onExportUsbClicked: () -> Unit = {},
    onFinishSetupClicked: () -> Unit = {},
    onEditClicked: (editType: SummaryEditType) -> Unit = {},
    facilityProfile: Facility?
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            stringResource(R.string.summary_tc),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("FTSU:Summary:Title:Text"),
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp.computeSize()),
            modifier = Modifier
                .fillMaxHeight(.85f)
                .padding(bottom = 32.dp)
        ) {

            /** FacilityProfile and Machine info **/
            Column(
                modifier = Modifier.weight(1f)
            ) {
                /** Facility Profile ***/
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .weight(1f),
                ) {

                    ScrollableCardContent {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.facility_profile_tc),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.testTag("FTSU:Summary:FacilityTitle:Text")
                            )

                            Spacer(Modifier.width(6.dp))

                            Icon(
                                painter = painterResource(R.drawable.note_pencil),
                                contentDescription = "",
                                modifier = Modifier
                                    .clickable { onEditClicked(SummaryEditType.FacilityProfile) }
                                    .testTag("FTSU:Summary:EditFacility:Button"),
                                tint = Color.White,

                                )
                        }

                        Spacer(Modifier.height(16.dp.computeSize()))

                        FacilityInfo(
                            label = stringResource(R.string.club_id_tc),
                            value = facilityProfile?.facilityId ?: summary.facilityId,
                            testTag = "FTSU:Summary:FacilityProfile"
                        )

                        Spacer(Modifier.height(32.dp))

                        FacilityInfo(
                            label = stringResource(R.string.facility_name_tc),
                            value = facilityProfile?.name ?: "",
                            testTag = "FTSU:Summary:FacilityName"
                        )

                        Spacer(Modifier.height(32.dp))

                        FacilityInfo(
                            label = stringResource(R.string.facility_location_tc),
                            value = facilityProfile?.location?.shortAddress ?: "",
                            testTag = "FTSU:Summary:FacilityLocation"
                        )
                    }

                }

                Spacer(Modifier.height(16.dp))

                /** Machine card **/
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .weight(1f),
                ) {

                    ScrollableCardContent {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.machine_tc),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.testTag("FTSU:Summary:MachineTitle:Text")
                            )

                            Spacer(Modifier.width(6.dp))

                            Icon(
                                painter = painterResource(R.drawable.note_pencil),
                                contentDescription = "",
                                modifier = Modifier
                                    .clickable { onEditClicked(SummaryEditType.Machine) }
                                    .testTag("FTSU:Summary:EditFacility:Button"),
                                tint = Color.White
                            )

                            if (!summary.isSerialNumberTupleValid) {
                                Spacer(Modifier.width(32.dp))

                                Text(
                                    stringResource(R.string.invalid_serial_number_combination_pg),
                                    modifier = Modifier.testTag("FirstTimeSetup:Summary:InvalidTupleText"),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp.computeSize()))

                        Row(Modifier.fillMaxWidth()) {

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(32.dp)
                            ) {

                                FacilityInfo(
                                    label = stringResource(R.string.console_tc),
                                    value = summary.consoleType,
                                    testTag = "FTSU:Summary:Console"
                                )

                                FacilityInfo(
                                    label = stringResource(R.string.frame_tc),
                                    value = summary.frame,
                                    testTag = "FTSU:Summary:Frame"
                                )

                                FacilityInfo(
                                    label = stringResource(R.string.model_tc),
                                    value = summary.model,
                                    testTag = "FTSU:Summary:Model"
                                )

                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                FacilityInfo(
                                    label = stringResource(R.string.serial_number_console_tc),
                                    value = summary.consoleSerialNumber,
                                    testTag = "FTSU:Summary:ConsoleSerialNumber"
                                )

                                Spacer(Modifier.height(32.dp))

                                FacilityInfo(
                                    label = stringResource(R.string.serial_number_frame_tc),
                                    value = summary.frameSerialNumber,
                                    testTag = "FTSU:Summary:FrameSerialNumber"
                                )

                                summary.bridgeSerialNumber?.let {
                                    Spacer(Modifier.height(32.dp))

                                    FacilityInfo(
                                        label = stringResource(R.string.bridge_serial_num_tc),
                                        value = summary.bridgeSerialNumber,
                                        testTag = "FTSU:Summary:BridgeSerialNumber"
                                    )
                                }
                            }

                        }

                    }
                }
            }

            /** Network,Tv setup,  and digital products **/
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Network and TV setup always hold one row of fields, so they wrap their content instead
                // of taking a weighted share that can come out a few dp short and force a tiny scroll.
                // Digital products (variable length) takes the rest. Scrolling stays as a fallback.

                /** Network card **/
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {


                    ScrollableCardContent {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.network_tc),
                                Modifier
                                    .testTag("FTSU:Summary:NetworkCard:Text"),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Spacer(Modifier.width(6.dp))

                            Icon(
                                painter = painterResource(R.drawable.note_pencil),
                                contentDescription = "",
                                modifier = Modifier
                                    .testTag("FTSU:Summary:EditNetwork:Button")
                                    .clickable { onEditClicked(SummaryEditType.Network) },
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.height(16.dp.computeSize()))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {
                            FacilityInfo(
                                label = stringResource(R.string.wireless_tc),
                                value = summary.networkNameWireless
                                    ?: stringResource(R.string.no_connection_tc),
                                testTag = "FTSU:Summary:Wireless"
                            )

                            Spacer(Modifier.height(32.dp))

                            FacilityInfo(
                                label = stringResource(R.string.wired_tc),
                                value = if (summary.networkNameWired.isNullOrEmpty()) {
                                    stringResource(R.string.no_connection_tc)
                                } else {
                                    summary.networkNameWired
                                },
                                testTag = "FTSU:Summary:Wired"
                            )

                        }

                    }

                }

                /** TV setup card **/
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {


                    ScrollableCardContent {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.tv_setup_tc),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.testTag("FTSU:Summary:TvSetupTitle:Text")

                            )

                            Spacer(Modifier.width(6.dp))

                            Icon(
                                painter = painterResource(R.drawable.note_pencil),
                                contentDescription = "",
                                modifier = Modifier
                                    .testTag("FTSU:Summary:EditTvSetup:Button")
                                    .clickable { onEditClicked(SummaryEditType.TVSetup) },
                                tint = Color.White
                            )

                        }

                        Spacer(Modifier.height(16.dp.computeSize()))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {
                            Column {
                                /* Connection type doesn't make sense so hiding it for now
                                FacilityInfo(
                                    label = stringResource(R.string.connection_type_tc),
                                    value = summary.tvConnectionType,
                                    testTag = "FTSU:Summary:ConnectionType"

                                )

                                Spacer(Modifier.height(32.dp))
                                 */

                                FacilityInfo(
                                    label = stringResource(R.string.default_channel_tc),
                                    value = summary.defaultChannel,
                                   testTag = "FTSU:Summary:DefaultChannels"

                                )
                            }

                            Column {
                                FacilityInfo(
                                    label = stringResource(R.string.number_of_channels_tc),
                                    value = summary.numberOfChannels,
                                    testTag = "FTSU:Summary:NumberOfChannels"

                                )
                            }
                        }
                    }

                }


                /** Digital Products**/
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {

                    ScrollableCardContent {

                        Text(
                            stringResource(R.string.digital_products_tc),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("FTSU:Summary:DigitalProductsTitle:Text")

                        )

                        Spacer(Modifier.height(16.dp.computeSize()))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                facilityProfile?.digitalProducts?.take(facilityProfile.digitalProducts.size / 2)
                                    ?.forEachIndexed { index, product ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AvailableIcon(
                                                modifier = Modifier,
                                                isAvailable = product.isAvailable
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                product.name,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .testTag("FTSU:Summary:DigitalProducts:${index + (summary.digitalProducts.size / 2)}:Text"),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }
                            }

                            Column {
                                facilityProfile?.digitalProducts?.drop(facilityProfile.digitalProducts.size / 2)
                                    ?.forEachIndexed { index, product ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AvailableIcon(
                                                modifier = Modifier,
                                                isAvailable = product.isAvailable
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                product.name,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .testTag("FTSU:DigitalProducts:${index + (summary.digitalProducts.size / 2)}:Text"),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }
                            }
                        }
                    }
                }
            }


            /** General settings card **/
            Card(
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.language_card_color)),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            ) {


                ScrollableCardContent {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.general_settings_tc),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("FTSU:Summary:GeneralSettingsTitle:Text")

                        )

                        Spacer(Modifier.width(6.dp))

                        Icon(
                            painter = painterResource(R.drawable.note_pencil),
                            contentDescription = "",
                            modifier = Modifier
                                .testTag("FTSU:Summary:EditGeneralSettings:Button")
                                .clickable { onEditClicked(SummaryEditType.GeneralSettings) },
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.height(16.dp.computeSize()))

                    FacilityInfo(
                        label = stringResource(R.string.time_zone_tc),
                        value = summary.timeZone,
                        testTag = "FTSU:Summary:TimeZone"

                    )

                    Spacer(Modifier.height(32.dp))

                    FacilityInfo(
                        label = stringResource(R.string.time_format_tc),
                        value = summary.timeFormat,
                        testTag = "FTSU:Summary:TimeFormat"

                    )

                    Spacer(Modifier.height(32.dp))

                    FacilityInfo(
                        label = stringResource(R.string.dateformat_tc),
                        value = summary.dateFormat,
                        testTag = "FTSU:Summary:DateFormat"

                    )

                    Spacer(Modifier.height(32.dp))

                    FacilityInfo(
                        label = stringResource(R.string.unit_format_tc),
                        value = summary.unitFormat,
                        testTag = "FTSU:Summary:UnitFormat"

                    )

                    summary.ledColor?.let {
                        Spacer(Modifier.height(32.dp.computeSize()))

                        HorizontalDivider(
                            Modifier
                                .height(1.dp)
                                .fillMaxWidth(),
                            color = colorResource(R.color.text_field_background)
                        )

                        Spacer(Modifier.height(32.dp.computeSize()))

                        Text(
                            stringResource(R.string.appearance_tc),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.testTag("FTSU:Summary:AppearanceTitle:Text")

                        )

                        Spacer(Modifier.height(16.dp.computeSize()))

                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {

                            Text(
                                stringResource(R.string.led_color_tc),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.testTag("FTSU:Summary:LedColor:Text")

                            )

                            Spacer(Modifier.height(8.dp.computeSize()))


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

                                Text(
                                    summary.ledColorHex,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("FTSU:Summary:LedColor:Text")

                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }

            }

        }

        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            FTSUButton(
                label = stringResource(R.string.settings_export_btn_text_tc),
                onClick = onExportUsbClicked,
                buttonType = FTSUButtonType.Secondary,
                modifier = Modifier
                    .testTag("FTSU:Summary:ExportToUsb:Button")
                    .weight(1f),
            )

            Spacer(Modifier.width(48.dp))

            FTSUButton(
                label = stringResource(R.string.finish_setup_tc),
                onClick = onFinishSetupClicked,
                buttonType = FTSUButtonType.Primary,
                modifier = Modifier
                    .testTag("FTSU:Summary:FinishSetup:Button")
                    .weight(1f),

            )

        }

    }

}

@Composable
private fun AvailableIcon(
    modifier: Modifier = Modifier,
    isAvailable: Boolean
) {
    if (isAvailable) {
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
            tint = colorResource(R.color.alert_color)
        )
    }
}

@Composable
@AllDevicePreviews
private fun SummaryViewOnyxPreview() {
    val previewFacilityAddress = FacilityAddress(
        city = "City", street1 = "Street 1", street2 = "Street 2",
        country = "Country", countryIso = "C_ISO", countryIso3 = "C_ISO3",
        postalCode = "0", state = "State", stateIso = "S_ISO", shortAddress = "City, State",
        longAddress = "Street 1\nCity, State, Postal Code\nCountry"
    )
    val fakeSummary = Summary(
        facilityAddress = previewFacilityAddress,
        facilityName = "Big Gainzzz Incorporated",
        facilityId = "Over 9000",
        digitalProducts = listOf(
            DigitalProduct("Engage 360", true),
            DigitalProduct("Asset Management", true)
        ),
        defaultChannel = "Fox Espanol",
        numberOfChannels = "9001",
        tvConnectionType = "US5 (ProCoax)",
        consoleMode = "Dark",
        unitFormat = "Imperial",
        timeFormat = "12 - Hour",
        timeZone = "(GMT=1:00) Belgrabe, Bratislava, Budapest, Hungary",
        frame = "Treadmill",
        model = "Performance Plus",
        frameSerialNumber = "31415926535897",
        consoleSerialNumber = "G3TF1TP@L",
        consoleType = "Touch XL",
        ledColor = 0xff00000,
        ledColorHex = "#360180",
        dateFormat = "mm/dd/yyyy",
        bridgeSerialNumber = "CE89373812"
    )
    SummaryView(summary = fakeSummary, modifier = Modifier.fillMaxSize(), facilityProfile = null)
}

@Composable
@AllDevicePreviews
private fun SummaryViewTouchXLPreview() {
    val previewFacilityAddress = FacilityAddress(
        city = "City", street1 = "Street 1", street2 = "Street 2",
        country = "Country", countryIso = "C_ISO", countryIso3 = "C_ISO3",
        postalCode = "0", state = "State", stateIso = "S_ISO", shortAddress = "City, State",
        longAddress = "Street 1\nCity, State, Postal Code\nCountry"
    )
    val fakeSummary = Summary(
        facilityAddress = previewFacilityAddress,
        facilityName = "Big Gainzzz Incorporated",
        facilityId = "Over 9000",
        digitalProducts = listOf(
            DigitalProduct("Engage 360", true),
            DigitalProduct("Asset Management", true)
        ),
        defaultChannel = "Fox Espanol",
        numberOfChannels = "9001",
        tvConnectionType = "US5 (ProCoax)",
        consoleMode = "Dark",
        unitFormat = "Imperial",
        timeFormat = "12 - Hour",
        timeZone = "(GMT=1:00) Belgrabe, Bratislava, Budapest, Hungary",
        frame = "Treadmill",
        model = "Performance Plus",
        frameSerialNumber = "31415926535897",
        consoleSerialNumber = "G3TF1TP@L",
        consoleType = "Touch XL",
        ledColor = null,
        ledColorHex = "#360180",
        dateFormat = "mm/dd/yyyy",
        bridgeSerialNumber = null
    )
    SummaryView(summary = fakeSummary, modifier = Modifier.fillMaxSize(), facilityProfile = null)
}