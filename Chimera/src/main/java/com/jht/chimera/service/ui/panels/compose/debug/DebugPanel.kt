package com.jht.chimera.service.ui.panels.compose.debug

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Slider
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.androidcommonalgorithms.network.AddressHelper
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum.Companion.getFrameTypeFromString
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.chimera.service.ui.panels.compose.ComposePanel
import com.jht.datamatrixlib.DataMatrix
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import kotlin.math.roundToInt

private val debugButtonColor = ButtonColors(
    containerColor = Color(0xff4f1bfa),
    contentColor = Color.White,
    disabledContainerColor = Color(0xff4f1bfa),
    disabledContentColor = Color(0xff4f1bfa),
)

/**
 * UI panel that displays settings useful for debugging. Should only be displayed on debug builds.
 */
class DebugPanel(context: Context) : ComposePanel(context) {
    val selectedFrame = mutableStateOf("")
    val frameOptions = mutableStateOf(listOf<String>())
    val currentLcb = mutableStateOf("")
    val demoModeOn = mutableStateOf(false)
    val adbWifiIp = mutableStateOf("")

    val demoModeRpm = mutableIntStateOf(0)
    val demoModeHr = mutableIntStateOf(0)

    val showMesButton = mutableStateOf(true)

    var dataMatrix: DataMatrix? = null

    @Composable
    override fun ThemedContent() {
        DebugDialog(
            selectedFrame = selectedFrame.value,
            frameOptions = frameOptions.value,
            currentLcb = currentLcb.value,
            currentDemoMode = demoModeOn.value,
            adbWifiIp = adbWifiIp.value,
            defaultDemoRpm = demoModeRpm.intValue,
            defaultDemoHr = demoModeHr.intValue,
            showMesButton = showMesButton.value,
            onSetFrame = {
                FrameConfigManager.getInstance().setFrame(getFrameTypeFromString(it))
            },
            onSetDemoMode = { DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, it) },
            onEnableAdbWifi = { activateAdbWifi() },
            onUpdateDemoRpm = { DataMatrix.putInt(HALKeys.DATA_DEMO_RPM_INT, it) },
            onUpdateDemoHr = { DataMatrix.putInt(HALKeys.DATA_DEMO_HEART_RATE_INT, it) },
            onUpdateDemoWorkoutSpeed = {
                DataMatrix.putFloat(
                    WorkoutKeys.ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT,
                    it.toFloat()
                )
            },
            onDismiss = { dismissPanel() },
            showTreadmillReverseToggle = FrameConfigManager.getInstance().frameConfig.TM_SUPPORTS_REVERSE_BELT,
        )
    }

    override fun getPanelParams(): WindowManager.LayoutParams? {
        val viewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            ChimeraEnums.TYPE_SYSTEM_ERROR, (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH), PixelFormat.TRANSPARENT
        )
        viewParams.gravity = Gravity.NO_GRAVITY
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }

    override fun panelAdded() {
        setupDataMatrixCallbacks()
    }

    override fun panelRemoved() {
        stopDataMatrixCallbacks()
    }

    private fun stopDataMatrixCallbacks() {
        dataMatrix?.finish()
        dataMatrix = null
    }

    private fun setupDataMatrixCallbacks() {
        stopDataMatrixCallbacks()

        dataMatrix = DataMatrix()

        dataMatrix?.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, {
            selectedFrame.value = it
        }, true)
        frameOptions.value = getSupportedFrames()

        dataMatrix?.createIntAdapter(
            HALKeys.DATA_LCB_TYPE_INT,
            { value: Int? -> currentLcb.value = (String.format("0x%02x", value)) },
            true
        )

        dataMatrix?.createBoolAdapter(
            SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL,
            { value: Boolean? -> demoModeOn.value = value ?: true },
            true
        )

        demoModeRpm.intValue = DataMatrix.getInt(HALKeys.DATA_DEMO_RPM_INT, 0)
        demoModeHr.intValue = DataMatrix.getInt(HALKeys.DATA_DEMO_HEART_RATE_INT, 0)
        showMesButton.value =
            DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP
    }

    private fun dismissPanel() {
        WorkoutPanelManager.instance?.showPanel(PanelIds.DEBUG_PANEL, false)
    }

    private fun getSupportedFrames(): List<String> {
        val supportedFramesList = mutableListOf<String>()
        val supportedFramesEnum = FrameConfigEnum.getSupportedFrames()
        supportedFramesEnum.forEach { supportedFramesList.add(it.toString()) }
        return supportedFramesList
    }

    private fun activateAdbWifi() {
        DataMatrix.sendEvent(LoggerKeys.ACTION_ENABLE_ADB_WIFI)
        val wlanIp = AddressHelper.getIPAddress("wlan0")
        val shownIp = wlanIp.ifEmpty { AddressHelper.getIPAddress("eth0") }
        adbWifiIp.value = shownIp
    }
}

@Composable
private fun DebugDialog(
    selectedFrame: String,
    frameOptions: List<String>,
    currentLcb: String,
    currentDemoMode: Boolean,
    adbWifiIp: String,
    defaultDemoRpm: Int,
    defaultDemoHr: Int,
    showMesButton: Boolean,
    onSetFrame: (String) -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onEnableAdbWifi: () -> Unit,
    onUpdateDemoRpm: (Int) -> Unit,
    onUpdateDemoHr: (Int) -> Unit,
    onUpdateDemoWorkoutSpeed: (Int) -> Unit,
    onDismiss: () -> Unit,
    showTreadmillReverseToggle: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickableNoEffect(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(Color.White.copy(alpha = 0.9f))
                .padding(15.dp)
        ) {
            Text(
                text = "Quick Debug Options",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Black)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // frame updates
            Row(modifier = Modifier.padding(bottom = 5.dp)) {
                FrameDropdown(
                    options = frameOptions,
                    onOptionSelected = { onSetFrame(it) },
                )

                if (showMesButton) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { LaunchAppUtil.launchSystemSetup(); onDismiss() },
                        colors = debugButtonColor
                    ) {
                        Text(
                            text = "Enter MES",
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { LaunchAppUtil.launchAndroidLauncher(); onDismiss() },
                    colors = debugButtonColor
                ) {
                    Text(
                        text = "Android Launcher",
                        color = Color.White
                    )
                }

                if (showTreadmillReverseToggle) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT)
                            val isReverse = DataMatrix.getBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL)
                            DataMatrix.putBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL, !isReverse)
                        },
                        colors = debugButtonColor
                    ) {
                        Text(
                            text = "Toggle Belt",
                            color = Color.White
                        )
                    }
                }
            }

            Text(
                text = "Current Frame: $selectedFrame",
                color = Color.Black,
                modifier = Modifier.padding(bottom = 5.dp),
            )

            Text(
                text = "LCB: $currentLcb",
                color = Color.Black,
                modifier = Modifier.padding(bottom = 5.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Demo Mode",
                    color = Color.Black,
                    modifier = Modifier.padding(end = 10.dp)
                )
                // demo mode switch
                Switch(
                    currentDemoMode,
                    onCheckedChange = onSetDemoMode,
                )
            }

            // adb wifi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 5.dp)
            ) {
                Button(
                    onClick = onEnableAdbWifi,
                    modifier = Modifier.padding(end = 10.dp),
                    colors = debugButtonColor
                ) {
                    Text(
                        text = "Enable ADB Wifi",
                        color = Color.White
                    )
                }

                Text(
                    text = adbWifiIp,
                    color = Color.Black
                )
            }

            DemoValueSlider(
                title = "Demo RPM",
                minValue = 0,
                maxValue = 300,
                defaultValue = defaultDemoRpm,
                onValueUpdated = onUpdateDemoRpm
            )

            DemoValueSlider(
                title = "Demo HR",
                minValue = 0,
                maxValue = 300,
                defaultValue = defaultDemoHr,
                onValueUpdated = onUpdateDemoHr
            )

            DemoValueSlider(
                title = "Workout Speed Multiplier",
                minValue = 1,
                maxValue = 30,
                defaultValue = 1,
                onValueUpdated = onUpdateDemoWorkoutSpeed
            )
        }
    }
}

@Composable
private fun FrameDropdown(
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            colors = debugButtonColor
        ) {
            Text(
                text = "Select new Frame",
                color = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DemoValueSlider(
    title: String,
    minValue: Int,
    maxValue: Int,
    defaultValue: Int,
    onValueUpdated: (Int) -> Unit
) {
    val sliderValue = remember { mutableIntStateOf(defaultValue) }
    Column {

        Text(
            text = "$title: ${sliderValue.intValue}",
            color = Color.Black
        )

        Slider(
            value = sliderValue.intValue.toFloat(),
            onValueChange = {
                sliderValue.intValue = it.roundToInt()
                onValueUpdated(sliderValue.intValue)
            },
            steps = 0,
            valueRange = minValue.toFloat()..maxValue.toFloat()
        )
    }
}

@AllDevicePreviews
@Composable
private fun DebugPanelPreview() {
    DebugDialog(
        selectedFrame = "test_frame",
        frameOptions = listOf("test_frame", "test_frame_2"),
        currentLcb = "lcb_type",
        currentDemoMode = true,
        adbWifiIp = "000.000.000",
        showMesButton = true,
        onSetFrame = {},
        onSetDemoMode = {},
        onEnableAdbWifi = {},
        onUpdateDemoRpm = {},
        onUpdateDemoHr = {},
        onUpdateDemoWorkoutSpeed = {},
        onDismiss = {},
        defaultDemoRpm = 30,
        defaultDemoHr = 30,
        showTreadmillReverseToggle = false,
    )
}