package com.jht.widgets

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.jht.data.TvTunerInformation
import com.jht.enums.FrameType
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter
import com.jht.widgets.helpers.SystemStats
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.numpad.NumPadDisplay
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@AllDevicePreviews
@Composable
private fun PreviewSettingsDialog() {
    // TODO context breaks previews, but I need it to inject data into ProductDashboardData
    // TODO split out ProductDashboardData into variables for private composable
    val productDashboardData = ProductDashboardData(LocalContext.current)
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardContent(productDashboardData)
    }
}
@AllDevicePreviews
@Composable
private fun PreviewPasscodePrompt() {
    val productDashboardData = ProductDashboardData(LocalContext.current)
    Box(modifier = Modifier.fillMaxSize()) {
        DashboardPasscodePrompt(data = productDashboardData)
    }
}

// made this way for previews to easily work and for ease of use inserting into other composables

private const val TAG = "ProductDashboard"

private class ProductDashboardData(context: Context) {
    private val loginModel: LoginModel
    private val systemModel: SystemModel
    private val idleTimerModel: IdleTimerModel
    private val deviceModel: DeviceModel
    private val navigationModel: NavigationModel
    private val uiBaseModel: UIBaseModel
    private val workoutModel: WorkoutModel

    init {
        val hiltEntryPoint = getInjectionEntryPoint(context) // get arbitrary DI entry point
        deviceModel = hiltEntryPoint.devices()
        systemModel = hiltEntryPoint.system()
        idleTimerModel = hiltEntryPoint.idleTimer()
        navigationModel = hiltEntryPoint.navigation()
        loginModel = hiltEntryPoint.login()
        uiBaseModel = hiltEntryPoint.uiBase()
        workoutModel = hiltEntryPoint.workout()
    }

    val serialNumberFrame = systemModel.serialNumberFrame
    val serialNumberConsole = systemModel.serialNumberConsole
    val serialNumberBridge = systemModel.serialNumberBridge
    val facilityID = systemModel.facilityID.value
    val accumulatedDistance = systemModel.accumulatedDistanceKm
    val accumulatedFloors = systemModel.accumulatedFloors
    val accumulatedTime = systemModel.accumulatedTime
    val softwareVersion = systemModel.softwareVersion
    val lcbVersion = systemModel.lcbVersion
    val lcbType = systemModel.lcbType
    val wirelessIpAddress = systemModel.wirelessIpAddress
    val wirelessMacAddress = systemModel.wirelessMacAddress
    val wirelessSSID = systemModel.wirelessSSID
    val ethernetIpAddress = systemModel.ethernetIpAddress
    val ethernetMacAddress = systemModel.ethernetMacAddress
    val dapiEnvironment = systemModel.dapiEnvironment.value
    val hasEthernet = systemModel.hasEthernet
    val frameType = uiBaseModel.frameType
    val logsSavedListener by lazy { mutableStateOf(false) }
    val processKeyCode: (Int) -> Boolean = { code ->
       systemModel.processKeypadCode(code)
    }
    val passcodeField = systemModel.settingsPasscodeField
    val passcodeLocked = mutableStateOf(passcodeField != null)
    val saveLogs: () -> Unit = { systemModel.saveLogs() }
    val reboot: () -> Unit = { systemModel.rebootSystem("Dashboard") }
    val enterERP: () -> Unit = { systemModel.enterERP() }
    val workoutStopped = mutableStateOf(false)
    val demoMode = mutableStateOf(false)
    val demoRpm = mutableIntStateOf(0)
    val demoHr  = mutableIntStateOf(0)
    val tvTunerInfo = mutableStateOf<TvTunerInformation?>(null)
    val outOfOrder = mutableStateOf(false)
    fun start(onDismissRequest: () -> Unit) {
        passcodeLocked.value = passcodeField != null
        systemModel.demoRpm.listenAndTrigger { demoRpm.intValue = it }
        systemModel.demoHeartRate.listenAndTrigger { demoHr.intValue = it }
        systemModel.saveLogsCompleteEvent.listen {
            logsSavedListener.value = true
        }
        uiBaseModel.isDemoMode.listenAndTrigger { demoMode.value = it }
        val timeoutSeconds =
            if (systemModel.screenTimeoutSeconds > 0) systemModel.screenTimeoutSeconds else 15
        if (timeoutSeconds > 0) {
            idleTimerModel.addIdleTimer(TAG, timeoutSeconds * 1000, onDismissRequest)
            idleTimerModel.startIdleTimers()
        }
        workoutModel.isWorkoutStopped.listenAndTrigger { workoutStopped.value = it }
        systemModel.tvTunerInformation.listenAndTrigger { tvTunerInfo.value = it }
        systemModel.outOfOrder.listenAndTrigger { outOfOrder.value = it }
    }
    fun stop() {
        deviceModel.close()
        systemModel.close()
        idleTimerModel.close()
        navigationModel.close()
        loginModel.close()
        uiBaseModel.close()
        workoutModel.close()
    }
    fun setDemoRpm(value: Int) { systemModel.demoRpm.value = value }
    fun setDemoHr(value: Int) { systemModel.demoHeartRate.value = value }
    fun setOutOfOrder(enable: Boolean) {
        systemModel.outOfOrder.value = enable
        if (enable) {
            navigationModel.launchOutOfOrder()
        } else {
            navigationModel.launchSplashScreen()
        }
    }
    fun unlockViaPasscode(passcode: String): Boolean {
        val passcodeCorrect = systemModel.settingsPasscodeCheck(passcode)
        passcodeLocked.value = !passcodeCorrect
        return passcodeCorrect
    }
}

@Composable
fun ProductDashboard(
    openDialog: MutableState<Boolean>,
) {
    if (openDialog.value) {
        val context = LocalContext.current
        val data = remember { ProductDashboardData(context) }
        DisposableEffect(data) {
            data.start { openDialog.value = false }
            onDispose {
                data.stop()
            }
        }

        PopupFullscreen(
            properties = PopupProperties(focusable = true),
            panelOperationMode = PanelOperationMode.Inplace, // apparently the chimera2 keyboard dismisses itself without this
            onDismissRequest = {
                openDialog.value = false
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoEffect(
                        onClick = { openDialog.value = false },
                    ),
                contentAlignment = Alignment.Center
            ) {
                DashboardContentProtected(
                    data = data,
                    onDismiss = {openDialog.value = false},
                )
            }
        }
    }
}


/** Standalone dialog version of [ProductDashboard] */
class ProductDashboardDialog(context: Context) : ComposeDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // old SettingsDlg set type to avoid keyboard rendering under the dialog
        setWindowType(PanelOperationMode.Inplace)
        makeFullScreen()
        /* Support for cancel on touch outside is being removed to prevent accident closing that has been observed on multiple occasions. */
        setCancelable(false)
        setCanceledOnTouchOutside(false)
    }
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val data = remember { ProductDashboardData(context) }
        DisposableEffect(data) {
            data.start(::dismiss)
            onDispose { data.stop() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoEffect(onClick = ::dismiss),
            contentAlignment = Alignment.Center,
        ) {
            DashboardContentProtected(
                data = data,
                onDismiss = ::dismiss,
            )
        }
    }
}


/** The main dashboard content secured by passcode prompt */
@Composable
private fun BoxScope.DashboardContentProtected(
    data: ProductDashboardData,
    onDismiss: () -> Unit = {},
) {
    if (data.passcodeLocked.value) {
        DashboardPasscodePrompt(
            data = data,
            modifier = Modifier.align(Alignment.TopCenter),
            onDismiss = onDismiss,
        )
    } else {
        DashboardContent(
            data = data,
            onDismiss = onDismiss,
        )
    }
}

/** The main dashboard content */
@Composable
private fun DashboardContent(
    data: ProductDashboardData,
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .background(DynamicTheme.colors.gray1)
            .clickableNoEffect(
                onClick = {/* do nothing, so clicking inside the settings window doesn't dismiss it */ },
            )
            .fillMaxSize(.9f),
        contentAlignment = Alignment.TopCenter
    ) {
        val context = LocalContext.current
        val bandwidthTestVisible = remember { mutableStateOf(false) }
        val numPadVisible = remember(data.workoutStopped.value) { mutableStateOf(false) }
        val systemStats = remember { SystemStats(context) }
        DisposableEffect(systemStats) {
            systemStats.start()
            onDispose { systemStats.stop() }
        }

        if (numPadVisible.value) NumPadEntryPopup(
            numPadValue = -1f,
            displayNumberWithoutUserInput = false,
            displayType = NumPadDisplay.Passcode,
            onCommit = {
                if (data.processKeyCode(it.roundToInt())) {
                    onDismiss()
                }
                numPadVisible.value = false
            },
            onDismiss = { numPadVisible.value = false },
        )

        BandWidthTestPopup(bandwidthTestVisible)


        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
        ) {
            // LEFT SIDE (Buttons)
            val buttonModifier = Modifier
                .height(85.dp.computeSize())
                .fillMaxWidth(0.9f)
                .padding(10.dp, 10.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(.3f)
                    .background(DynamicTheme.colors.gray3)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.8f)
                        .padding(0.dp, 10.dp)
                ) {
                    if (data.workoutStopped.value) DashBoardButton(
                        text = stringResource(R.string.engineering_mode_tc),
                        color = DynamicTheme.colors.secondary,
                        modifier = buttonModifier,
                        onClick = { numPadVisible.value = true },
                    )
                    //CGC-19826
                    /* Force users into MES to toggle out-of-order
                    if (data.workoutStopped.value) {
                        val text: String
                        val onClick: () -> Unit
                        if (data.outOfOrder.value) { // out of order
                            text = stringResource(R.string.disable_out_of_order)
                            onClick = { data.setOutOfOrder(false) }
                        } else {
                            text = stringResource(R.string.enable_out_of_order)
                            onClick = { data.setOutOfOrder(true) }
                        }
                        DashBoardButton(
                            text = text,
                            color = DynamicTheme.colors.secondary,
                            onClick = onClick,
                            data = data
                        )
                    }*/
                    DashBoardButton(
                        text = stringResource(R.string.save_logs_tc),
                        color = DynamicTheme.colors.secondary,
                        modifier = buttonModifier,
                        onClick = { data.saveLogs() },
                        showSpinnerOnClick = true,
                        completionListener = data.logsSavedListener,
                    )

                    if (data.workoutStopped.value) DashBoardButton(
                        text = stringResource(R.string.erp_tc),
                        color = DynamicTheme.colors.secondary,
                        modifier = buttonModifier,
                        onClick = { data.enterERP() },
                    )
                    DashBoardButton(
                        text = stringResource(R.string.bandwidth_test_tc),
                        modifier = buttonModifier,
                        color = DynamicTheme.colors.secondary,
                        onClick = { bandwidthTestVisible.value = true },
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f)
                        .wrapContentHeight()
                ) {
                    DashBoardButton(
                        text = stringResource(R.string.reboot_tc),
                        color = Color(0XFFFF432E),
                        modifier = buttonModifier,
                        onClick = { data.reboot() },
                        showSpinnerOnClick = true,
                    )
                }
            }
            // RIGHT SIDE (text information)
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.9f)
                    .padding(20.dp, 16.dp)
                    .background(DynamicTheme.colors.gray1)
            ) {
                val fontSize = 18.sp.computeSize()
                Text(
                    text = "PRODUCT DASHBOARD",
                    modifier = Modifier.padding(0.dp, 5.dp),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle3,
                )
                Text(
                    fontSize = fontSize, color = DynamicTheme.colors.text1,
                    modifier = Modifier.padding(0.dp, 5.dp),
                    text = data.frameType.name,
                    style = DynamicTheme.typography.textStyle2
                )
                val dashboardTextDataList =
                    arrayListOf(
                        Pair(
                            stringResource(R.string.serial_number_frame_tc),
                            data.serialNumberFrame
                        ),
                        Pair(
                            stringResource(R.string.serial_number_console_tc),
                            data.serialNumberConsole
                        ),
                        Pair(
                            stringResource(R.string.club_id_tc),
                            data.facilityID.toString() + " - " + data.dapiEnvironment
                        ),

                        when(data.frameType){
                            FrameType.Climbmill,
                            FrameType.Stepper ->
                                //Climbmills and steppers show accumulated floors instead of distance
                                Pair(stringResource(R.string.floor_climbed_tc),
                                    String.format(Locale.ENGLISH, "%.0f",data.accumulatedFloors))

                            else -> {
                                //Other frames show accumulated distance
                                Pair(
                                    stringResource(R.string.accumulated_distance_tc),
                                    UnitFormatter.formatDistance(data.accumulatedDistance, true, 1) +
                                            " " + stringResource(R.string.km_lc) +
                                            " / " + UnitFormatter.formatDistance(data.accumulatedDistance, false, 1) +
                                            " " + stringResource(R.string.miles_lc)
                                )
                            }
                        },
                        Pair(
                            stringResource(R.string.accumulated_time_tc), String.format(
                                Locale.ENGLISH, "%01d:%02d %s",
                                data.accumulatedTime / 60 / 60,
                                data.accumulatedTime / 60 % 60,
                                stringResource(R.string.hours_lc)
                            )
                        ),

                        Pair(
                            stringResource(R.string.software_version_dashboard_tc),
                            data.softwareVersion
                        ),

                        Pair(
                            stringResource(R.string.lcb_uc), String.format(
                                Locale.ENGLISH, "Type: 0x%02X, Version: %s",
                                data.lcbType,
                                data.lcbVersion,
                            )
                        ),

                        Pair(
                            stringResource(R.string.wifi_ip_address_tc),
                            data.wirelessIpAddress
                        ),
                        Pair(
                            stringResource(R.string.wifi_mac_address_tc),
                            data.wirelessMacAddress
                        ),
                        Pair(
                            stringResource(R.string.wireless_ssid_tc),
                            data.wirelessSSID
                        )
                    )
                if (data.hasEthernet) {
                    dashboardTextDataList.add(
                        Pair(
                            stringResource(R.string.ethernet_ip_address_tc),
                            data.ethernetIpAddress
                        )
                    )
                    dashboardTextDataList.add(
                        Pair(
                            stringResource(R.string.ethernet_mac_address_tc),
                            data.ethernetMacAddress
                        )
                    )
                }
                if (data.serialNumberBridge.isNotEmpty()) {
                    dashboardTextDataList.add(
                        Pair(
                            stringResource(R.string.serial_number_bridge_tc),
                            data.serialNumberBridge
                        )
                    )
                }
                data.tvTunerInfo.value?.let { tunerData ->
                    if (tunerData.hasTuner) {
                        if (tunerData.ip.isNotEmpty()) dashboardTextDataList.add(
                            Pair(
                                stringResource(R.string.tuner_ip_address_tc),
                                tunerData.ip
                            )
                        )
                        if (tunerData.mac.isNotEmpty()) dashboardTextDataList.add(
                            Pair(
                                stringResource(R.string.tuner_mac_address_tc),
                                tunerData.mac
                            )
                        )
                    }
                }

                ProductInfoGridView(dashboardTextData = dashboardTextDataList)


                // Line separator
                HorizontalDivider(modifier = Modifier.padding(0.dp, 16.dp), thickness = 1.dp, color = Color.White)

                Text(
                    text = stringResource(R.string.resource_monitor_tc),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle2
                )
                val dashboardResourcesTextDataList =
                    arrayListOf(
                        Pair(stringResource(R.string.memory_tc), systemStats.memoryUsage.collectAsState().value),
                        Pair(stringResource(R.string.cpu_uc), systemStats.cpuUsage.collectAsState().value),
                        Pair(stringResource(R.string.sdcard_tc), systemStats.sdCardUsage.collectAsState().value),
                        Pair(stringResource(R.string.storage_tc), systemStats.storageUsage.collectAsState().value),
                        Pair(stringResource(R.string.cpu_temp_uc), systemStats.cpuTemp.collectAsState().value),
                        Pair(stringResource(R.string.wifi_signal_strength_tc), systemStats.wifiSignal.collectAsState().value),
                    )
                Row(){
                    ProductInfoGridView(dashboardTextData = dashboardResourcesTextDataList)
                }
                if (data.demoMode.value) {
                    Row {
                        SliderBox(
                            icon = R.drawable.icon_heart_rate_white,
                            valueRange = 0f..200f, // heart rate range 0-200bpm
                            value = data.demoHr.intValue,
                            onValueChange = data::setDemoHr,
                            modifier = Modifier.weight(1f),
                            testTag = "demo_hr",
                        )
                        if (!data.frameType.isTreadmill) SliderBox(
                            icon = R.drawable.ic_rpm_white,
                            valueRange = 0f..140f, // old demo RPM slider was 0-140 rpm
                            value = data.demoRpm.intValue,
                            onValueChange = data::setDemoRpm,
                            modifier = Modifier.weight(1f),
                            testTag = "demo_rpm",
                        )
                    }
                } // END DEMO MODE

            }
        }
    }
}

private enum class ButtonActionState {
    Idle,
    InProgress,
    Complete;
}

@Composable
private fun DashBoardButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    showSpinnerOnClick: Boolean = false,
    completionListener: MutableState<Boolean>? = null,
) {

    val textSize = 18.sp.computeSize()

    // only need a completionListener for the 'save logs' button since it has datamatrix listeners
    // and an animation that plays. default is fine for others since they won't be used
    val actionState = remember { mutableStateOf(ButtonActionState.Idle) }
    // block to update spinning progress indicator, based on state of boolean that is listening for
    // the datamatrix key that fires an event when saving logs has completed

    if (completionListener != null) {
        LaunchedEffect(completionListener.value) {
            if (completionListener.value) {
                actionState.value = ButtonActionState.Complete // show checkmark
                delay(2.seconds)
                actionState.value = ButtonActionState.Idle // back to idle text
                completionListener.value = false // reset listener
            }
        }
    }

    ThemeButton(
        onClick = {
            if (showSpinnerOnClick) {
                // only allow click if not already saving logs
                if (actionState.value == ButtonActionState.Idle) {
                    actionState.value = ButtonActionState.InProgress
                    onClick()
                }
            } else {
                onClick()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
    ) {
        if (showSpinnerOnClick) {
            when (actionState.value) {
                ButtonActionState.Idle -> {
                    Text(
                        text = text,
                        color = DynamicTheme.colors.text2,
                        fontSize = textSize,
                        style = DynamicTheme.typography.textStyle1,
                    )
                }
                ButtonActionState.InProgress -> {
                    CircularProgressIndicator(color = DynamicTheme.colors.gray1)
                }
                ButtonActionState.Complete -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = "done",
                        tint = DynamicTheme.colors.gray1
                    )
                }
            }
        } else {
            Text(
                text = text,
                color = DynamicTheme.colors.text2,
                fontSize = textSize,
                style = DynamicTheme.typography.textStyle1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashBoardText(label: String, text: String) {

    val fontSize = 16.sp.computeSize()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp, 10.dp)
    ) {
        Text(
            text = label, color = DynamicTheme.colors.text1, modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.Start)
                , fontSize = fontSize, textAlign = TextAlign.Start
        )
        Text(
            text = text, color = DynamicTheme.colors.text1, modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.Start)
            , style = TextStyle(fontWeight = FontWeight.Bold)
            , fontSize = fontSize, textAlign = TextAlign.Start
        )
    }
}


@Composable
private fun SliderBox(
    testTag: String,
    icon: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontSize = 18.sp.computeSize()
    val heartSize = 25.dp.computeSize()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = testTag + "_icon",
                tint = if (value > 0) Color.Red else Color.LightGray,
                modifier = Modifier.size(heartSize)
            )
            Text(
                text = value.toString(),
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = fontSize,
                style = DynamicTheme.typography.textStyle1,
                modifier = Modifier
                    .testTag(testTag + "_value_text")
                    .padding(10.dp, 5.dp)
            )

            // Wrap the Slider with a container that has background and rounded corners
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .background(DynamicTheme.colors.gray4)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Slider(
                    value = value.toFloat(),
                    onValueChange = {
                        onValueChange(it.toInt())
                    },
                    valueRange = valueRange,
                    colors = SliderDefaults.colors(thumbColor = DynamicTheme.colors.secondary),
                    modifier = Modifier
                        .padding(10.dp, 3.dp)
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
        }
    }

}

@Composable
private fun ProductInfoGridView(dashboardTextData: ArrayList<Pair<String,String>>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        content = {
            items(dashboardTextData.size) {

                DashboardTextGridItem(gridItem = dashboardTextData.get(it))
            }
        }
    )
}

@Composable
private fun DashboardTextGridItem(
    gridItem: Pair<String, String>
) {
    Row(
        modifier = Modifier
    ) {

            DashBoardText(
                gridItem.first,
                gridItem.second
            )
        }
}


/** Form for passcode entry */
@Composable
private fun DashboardPasscodePrompt(
    data: ProductDashboardData,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    var passcode by remember { mutableStateOf(TextFieldValue("")) }
    var hideValue by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    val fieldDetails = remember(data) { data.passcodeField }

    val attemptUnlock: () -> Unit = {
        // on success passcodeLocked flips false and the parent swaps in DashboardContent
        if (!data.unlockViaPasscode(passcode.text)) {
            error = true
            passcode = TextFieldValue("")
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(DynamicTheme.colors.gray1)
            .border(2.dp, DynamicTheme.colors.gray2)
            .clickableNoEffect { /* swallow, don't dismiss */ }
            .padding(32.dp)
            .fillMaxWidth(0.5f)
            .wrapContentHeight(),
    ) {
        BaseInput(
            value = passcode,
            onValueChange = {
                passcode = it
                error = false
            },
            modifier = Modifier
                .testTag("passcode_field")
                .fillMaxWidth(0.5f),
            label = fieldDetails?.label.orEmpty().ifEmpty { stringResource(R.string.passcode_tc) },
            trailingIcon = { InputHideToggle(hideValue, { hideValue = !hideValue }) },
            isError = error,
            errorText = stringResource(R.string.passcode_error_pg),
            visualTransformation = if (hideValue) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrect = false,
            ),
            keyboardActions = KeyboardActions(onDone = { attemptUnlock() }),
            maxCharacters = fieldDetails?.maxLength ?: Int.MAX_VALUE,
        )

        Row {
            val buttonModifier = Modifier
                .height(65.dp.computeSize())
                .weight(1f)
                .padding(10.dp)
            DashBoardButton(
                text = stringResource(R.string.unlock_tc),
                color = DynamicTheme.colors.primary,
                modifier = buttonModifier,
                onClick = attemptUnlock,
            )
            DashBoardButton(
                text = stringResource(R.string.cancel_tc),
                color = DynamicTheme.colors.secondary,
                modifier = buttonModifier,
                onClick = onDismiss,
            )
        }
    }
}
