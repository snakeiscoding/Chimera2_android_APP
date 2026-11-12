package com.jht.firsttimesetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.viewmodels.CalibrationType
import com.jht.firsttimesetup.viewmodels.MachineCalibrationState
import com.jht.firsttimesetup.viewmodels.MachineCalibrationUi
import com.jht.firsttimesetup.viewmodels.MachineCalibrationViewModel
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


@Composable
fun MachineCalibrationUiView(
    vm: MachineCalibrationViewModel = hiltViewModel(),
    onNextClicked: () -> Unit,
    isEngineeringMode: Boolean
) {

    val uiState = vm.uiState.collectAsStateWithLifecycle()

    MachineCalibrationScreen(
        onStartCalibration = { vm.startCalibration() },
        onNextClicked = onNextClicked,
        onRestartDemo = { vm.onRestartDemo() },
        onToggleResult = vm::onToggleMachineCalibrationResult,
        onCompleteCalibration = { vm.triggerMachineCalibrationSuccess() },
        state = uiState.value,
        isEngineeringMode = isEngineeringMode,
        calibrationType = uiState.value.calibrationType,
    )

}

@Composable
fun MachineCalibrationScreen(
    onNextClicked: () -> Unit,
    onStartCalibration: () -> Unit,
    onRestartDemo: () -> Unit,
    onToggleResult: () -> Unit,
    onCompleteCalibration: () -> Unit,
    state: MachineCalibrationUi,
    isEngineeringMode: Boolean,
    calibrationType: CalibrationType = CalibrationType.NotConfigured
) {
    when (state.state) {
        MachineCalibrationState.START -> StartMachineCalibration(onStartCalibration, calibrationType)
        MachineCalibrationState.CALIBRATING -> CalibratingMachineUI(onCompleteCalibration, isEngineeringMode, calibrationType)
        MachineCalibrationState.SUCCESS -> CalibrationResultsUI(
            true,
            onNextClicked,
            onStartCalibration,
            onRestartDemo,
            onToggleResult,
            isEngineeringMode
        )

        MachineCalibrationState.FAIL -> CalibrationResultsUI(
            false,
            onNextClicked,
            onStartCalibration,
            onRestartDemo,
            onToggleResult,
            isEngineeringMode
        )
    }
}

@Composable
fun StartMachineCalibration(startCalibration: () -> Unit, calibrationType: CalibrationType = CalibrationType.NotConfigured) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxHeight()
    ) {
        Text(
            stringResource(R.string.machine_calibration_tc),
            color = colorResource(R.color.light_text),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("FTSU:MachineCalibration:Title:Text")
        )

        // The drawables are 527dp (treadmill) / 376dp (elliptical) tall at intrinsic size, which on
        // 1366x768 consoles left no room for anything after them. Weighting the picture measures it
        // last, so it scales down (Icon keeps the aspect ratio) to whatever the text and button leave.
        // fill = false keeps it at intrinsic size when there is room.
        MachineCalibrationPicture(calibrationType, Modifier.weight(1f, fill = false))

        // Gaps are shares of the leftover height rather than fixed dp, so they shrink with the screen.
        Spacer(Modifier.weight(.1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.machine_calibration_instruction_pg),
                color = colorResource(R.color.light_text),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("FTSU:MachineCalibration:SafetyMessage:Text")
            )
            Text(
                stringResource(R.string.machine_calibration_explanation_pg),
                color = colorResource(R.color.light_text),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag("FTSU:MachineCalibration:InformationBody:Text")
            )
        }

        Spacer(Modifier.weight(.1f))

        FTSUButton(
            label = stringResource(R.string.start_machine_calibration_tc),
            onClick = startCalibration,
            modifier = Modifier
                .testTag("FTSU:MachineCalibration:StartCalibration:Button")
        )

        Spacer(Modifier.weight(.14f))
    }
}

@Composable
fun CalibratingMachineUI(
    onCompleteCalibration: () -> Unit,
    isEngineeringMode: Boolean = false,
    calibrationType: CalibrationType = CalibrationType.NotConfigured
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.alert_circle),
            "alert circle",
            tint = colorResource(R.color.selected_color),
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Text(
            stringResource(R.string.caution_tc),
            color = colorResource(R.color.light_text),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .testTag("FTSU:MachineCalibration:CautionTitle:Text")
        )
        Text(
            stringResource(R.string.calibration_in_progress_tc),
            color = colorResource(R.color.light_text),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(bottom = 42.dp)
                .testTag("FTSU:MachineCalibration:CalibrationProgress:Text")
        )

        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.incline_tc) + " %",
                modifier = Modifier
                    .testTag("FTSU:MachineCalibration:InclinePercentage:Text")
                    .rotate(270f)
                    .padding(end = 20.dp),
                color = colorResource(R.color.med_text),
                style = MaterialTheme.typography.bodyMedium
            )

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 15 downTo 1) {
                    Text(
                        "$i",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.med_text),
                        modifier = Modifier
                            .testTag("FTSU:MachineCalibration:InclineLevel:$i:Text")
                    )
                }
            }

            Spacer(Modifier.size(30.dp))

            MachineCalibrationPicture(calibrationType)

            Spacer(Modifier.size(107.dp))
        }

        if (isEngineeringMode) {
            Button(
                onClick = onCompleteCalibration,
                modifier = Modifier
                    .testTag("FTSU:MachineCalibration:CompleteCalibration:Button")
            ) {
                Text("Complete Calibration", color = colorResource(R.color.dark_text))
            }
        }
    }
}

@Composable
fun CalibrationResultsUI(
    succeeded: Boolean,
    onNextClicked: () -> Unit,
    onStartCalibration: () -> Unit,
    onRestartDemo: () -> Unit,
    onToggleResult: () -> Unit,
    isEngineeringMode: Boolean = false
) {
    Column (
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(
                    if (succeeded) R.drawable.check_circle
                    else R.drawable.x_circle
                ),
                tint = colorResource(
                    if (succeeded) R.color.selected_color
                    else R.color.alert_color
                ),
                contentDescription = "Calibration Result Icon",
                modifier = Modifier.size(264.dp)
            )

            ResultText(succeeded)

            if (!succeeded) {
                Spacer(Modifier.size(64.dp))

                FTSUButton(
                    label = stringResource(R.string.try_again_tc),
                    onClick = onStartCalibration,
                    modifier = Modifier
                        .testTag("FTSU:MachineCalibration:TryAgain:Button")
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row (
            Modifier
                .fillMaxWidth()
                .padding(64.dp.computeSize()),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ){


            if (isEngineeringMode) {
                Button(
                    onClick = onRestartDemo,
                    modifier = Modifier
                        .testTag("FTSU:MachineCalibration:RestartCalibration:Button")
                ) {
                    Text(stringResource(R.string.restart_calibration_tc), color = colorResource(R.color.dark_text))
                }

                Spacer(Modifier.size(64.dp))

                Button(
                    onClick = onToggleResult,
                ) {
                    Text(stringResource(R.string.toggle_success_failure_tc), color = colorResource(R.color.dark_text))
                }

                Spacer(Modifier.size(64.dp))
            }

            FTSUButton(
                modifier = Modifier
                    .testTag("FTSU:MachineCalibration:Summary:Button"),
                onClick = onNextClicked,
                label =
                    if (succeeded) stringResource(R.string.summary_tc)
                    else stringResource(R.string.skip_for_now_tc),
                buttonType =
                    if (succeeded) FTSUButtonType.Primary
                    else FTSUButtonType.Secondary,
                imageEnd = {
                    if (succeeded) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            "",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp.computeSize())
                        )
                    }
                    else {
                        Spacer(Modifier.size(30.dp.computeSize()))
                    }
                },
            )
        }
    }
}

@Composable
fun ResultText(succeeded: Boolean) {
    if (succeeded) {
        Text(
            stringResource(R.string.machine_calibration_competed_tc),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = colorResource(R.color.light_text),
            modifier = Modifier
                .testTag("FTSU:MachineCalibration:CalibrationCompleted:Text")
        )
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.error_tc),
                style = MaterialTheme.typography.headlineSmall,
                color = colorResource(R.color.light_text)
            )

            Spacer(Modifier.size(16.dp))

            Text(
                stringResource(R.string.calibration_fail_instruction_pg),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colorResource(R.color.light_text)
            )
        }
    }
}

@Composable
fun MachineCalibrationPicture(
    calibrationType: CalibrationType = CalibrationType.NotConfigured,
    modifier: Modifier = Modifier,
) {
    when(calibrationType){
        CalibrationType.Ascent ->
            Icon(
                painter = painterResource(R.drawable.elliptical),
                "Elliptical Icon",
                tint = Color.Unspecified,
                modifier = modifier.testTag("MachineCalibrationPicture: ${calibrationType.name}"),
            )

        CalibrationType.Treadmill ->
            Icon(
                painter = painterResource(R.drawable.treadmill),
                "Treadmill Icon",
                tint = Color.Unspecified,
                modifier = modifier.testTag("MachineCalibrationPicture: ${calibrationType.name}"),
            )

        CalibrationType.NotConfigured ->
            Icon(
                painter = painterResource(R.drawable.treadmill),
                "Unconfigured Treadmill Icon",
                tint = Color.Unspecified,
                modifier = modifier.testTag("MachineCalibrationPicture: ${calibrationType.name}"),
            )
    }
}


@Preview
@Composable
private fun MachinePicture() {
    MachineCalibrationPicture()
}

@AllDevicePreviews
@Composable
private fun StartCalibrationUi() {
    StartMachineCalibration(
        startCalibration = { }
    )
}

@AllDevicePreviews
@Composable
private fun CalibratingMachine() {
    CalibratingMachineUI(
        onCompleteCalibration = {}
    )
}

@AllDevicePreviews
@Composable
private fun CalibratingMachineEngineeringMode() {
    CalibratingMachineUI(
        onCompleteCalibration = {},
        isEngineeringMode = true
    )
}

@AllDevicePreviews
@Composable
private fun ResultPassExampleEngineeringMode() {
    CalibrationResultsUI(true, {}, {}, {}, {}, isEngineeringMode = true)
}

@AllDevicePreviews
@Composable
private fun ResultPassExample() {
    CalibrationResultsUI(true, {}, {}, {}, {})
}


@Preview
@Composable
private fun ResultFailExample() {
    CalibrationResultsUI(false, {}, {}, {}, {})
}

@Preview
@Composable
private fun ResultFailExampleEngineeringMode() {
    CalibrationResultsUI(false, {}, {}, {}, {}, isEngineeringMode = true)
}