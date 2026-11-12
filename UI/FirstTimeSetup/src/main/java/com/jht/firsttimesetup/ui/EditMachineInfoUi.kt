package com.jht.firsttimesetup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FTSUTextField
import com.jht.firsttimesetup.viewmodels.EditMachineInfoViewModel
import com.jht.firsttimesetup.viewmodels.FirstTimeSetupViewModel
import com.jht.ui.AllDevicePreviews


private object SerialNumberEntryUtils {
    const val SERIAL_MIN_LENGTH = 14
    const val SERIAL_MAX_LENGTH = 32
    fun filterSerialInput(input: String): String =
        input
            .uppercase()
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .take(SERIAL_MAX_LENGTH)

    fun isValid(input: String): Boolean =
        input.length >= SERIAL_MIN_LENGTH
}

@Composable
private fun FrameDropdown(
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FTSUButton(
            label = "Frame Prefix List - Select Frame",
            modifier = modifier,
            onClick = { expanded = true },
        )

        DropdownMenu(
            expanded = expanded,
            modifier = modifier,
            onDismissRequest = { expanded = false },
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
fun EditMachineInfoView(
    vm: EditMachineInfoViewModel = hiltViewModel(),
    ftsuViewModel: FirstTimeSetupViewModel,
    onDiscard: () -> Unit = {},
    isEdit: Boolean = false,
) {

    val uiState = vm.machineInfo.collectAsStateWithLifecycle()

    EditMachineInfo(
        ftsuViewModel = ftsuViewModel,
        console = uiState.value.console,
        frame = uiState.value.frame,
        model = uiState.value.model,
        consoleSN =  uiState.value.consoleSn,
        frameSN = uiState.value.frameSn,
        bridgeSN = uiState.value.bridgeSn,
        onUpdateConsoleSN = vm::updateConsoleSn,
        onUpdateFrameSN = vm::updateFrameSn,
        onUpdateBridgeSN = vm::updateBridgeSn,
        onDiscard = onDiscard,
        onSave = { vm.saveChanges(); onDiscard() },
        hasBridgeSN = vm.isOnyx(), // Onyx has the bridge serial number
        isEdit = isEdit,
        onAutofillPrefix = vm::autofillPrefix
    )

}

@Composable
fun EditMachineInfo(
    ftsuViewModel: FirstTimeSetupViewModel = hiltViewModel(),
    console: String,
    frame: String,
    model: String,
    consoleSN: String,
    frameSN: String,
    bridgeSN: String,
    onUpdateConsoleSN: (String) -> Unit = {},
    onUpdateFrameSN: (String) -> Unit = {},
    onUpdateBridgeSN: (String) -> Unit = {},
    onDiscard: () -> Unit = {},
    onSave: () -> Unit = {},
    onAutofillPrefix: (String, List<String>) -> Unit = fun(_, _) {},
    hasBridgeSN: Boolean = false,
    isEdit: Boolean = false,
){
    val localFocusManager = LocalFocusManager.current
    var showHint by remember { mutableStateOf(ftsuViewModel.isDemoMode()) }
    var clickCount by remember { mutableIntStateOf(0) }
    var selectedFrame by remember { mutableStateOf(ftsuViewModel.getFrameTypeFromSerial()) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            }
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.65f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            //Title
            Text(
                stringResource(R.string.edit_machine_info_tc),
                style = MaterialTheme.typography.headlineLarge,
                color = colorResource(R.color.light_text),
                modifier = Modifier
                    .clickable {
                        clickCount++
                        if (clickCount >= 3) { // click 3x to show hints
                            showHint = true
                        }
                    }
                    .testTag("FTSU:EditMachine:Title:Text")
            )

            Spacer(Modifier.weight(1.24f))

            // hints will show a frame selection dropdown and valid prefixes for the frame selected
            if (showHint) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.padding(bottom = 5.dp)) {
                    FrameDropdown(
                        options = ftsuViewModel.getSupportedFrames(),
                        onOptionSelected = { selFrame ->
                            val oldPrefixList = ftsuViewModel.getFramePrefixes(selectedFrame)
                            selectedFrame = selFrame
                            val newPrefix = ftsuViewModel.getFramePrefixes(selFrame).first()
                            onAutofillPrefix(newPrefix, oldPrefixList)
                        },
                        modifier = Modifier.width(600.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Valid frame prefixes for [${selectedFrame.ifEmpty { "None" }}]: ${
                        ftsuViewModel.getFramePrefixes(
                            selectedFrame
                        )
                    }"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row {
                //Console
                MachineInfo(
                    stringResource(R.string.console_tc),
                    console,
                    testTag = "Console",
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.size(48.dp))

                //Frame
                MachineInfo(
                    stringResource(R.string.frame_tc),
                    frame,
                    testTag = "Frame",
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.size(48.dp))

                //Model
                MachineInfo(
                    stringResource(R.string.model_tc),
                    model,
                    testTag = "Model",
                    modifier = Modifier.weight(1f),
                )

            }

            Spacer(Modifier.weight(.48f))

            Row {
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                val hideKeyboardOnDone = remember {
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        })
                }

                //CSN
                FTSUTextField(
                    label = stringResource(R.string.serial_number_console_tc),
                    text = consoleSN,
                    testTag = "EditMachineSettings:ConsoleSerialNumber",
                    onTextChange = {
                        val consoleSerialNumber = SerialNumberEntryUtils.filterSerialInput(it)
                        onUpdateConsoleSN(consoleSerialNumber)
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = hideKeyboardOnDone,
                )
                Spacer(Modifier.size(48.dp))

                //FSN
                FTSUTextField(
                    label = stringResource(R.string.serial_number_frame_tc),
                    text = frameSN,
                    testTag = "EditMachineSettings:FrameSerialNumber",
                    onTextChange = {
                        val frameSerialNumber = SerialNumberEntryUtils.filterSerialInput(it)
                        onUpdateFrameSN(frameSerialNumber)
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = hideKeyboardOnDone,
                )

                if (hasBridgeSN) {
                    Spacer(Modifier.size(48.dp))

                    //BSN
                    FTSUTextField(
                        label = stringResource(R.string.bridge_serial_num_tc),
                        text = bridgeSN,
                        testTag = "FTSU:EditMachine:BridgeSerialNumber",
                        onTextChange = {
                            val bridgeSerialNumber = SerialNumberEntryUtils.filterSerialInput(it)
                            onUpdateBridgeSN(bridgeSerialNumber)
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = hideKeyboardOnDone,

                    )
                }
            }

            Spacer(Modifier.weight(1.1f))

            Row {
                if (isEdit) {
                    FTSUButton(
                        label = stringResource(R.string.discard_tc),
                        modifier = Modifier
                            .widthIn(max = 250.dp)
                            .fillMaxWidth(),

                        buttonType = FTSUButtonType.Primary,
                        onClick = { onDiscard() }
                    )

                    Spacer(Modifier.size(48.dp))
                }

                FTSUButton(
                    label = stringResource(R.string.save_tc),
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .fillMaxWidth(),
                    onClick = { onSave() },
                    buttonType = if (SerialNumberEntryUtils.isValid(consoleSN) && SerialNumberEntryUtils.isValid(frameSN) && frame.isNotBlank())
                        FTSUButtonType.Primary else FTSUButtonType.Disabled,
                )

                if (ftsuViewModel.isDemoMode()){
                    Spacer(Modifier.size(48.dp))

                    FTSUButton(
                        label = "Save and Exit FTSU",
                        onClick = {
                            onSave()
                            ftsuViewModel.onSkipFtsu()
                        },
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }

        Text(
            text = "LCB: 0x${ftsuViewModel.getLcbType().toString(16).uppercase().padStart(2, '0')}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun MachineInfo(
    title: String,
    info: String,
    modifier: Modifier = Modifier,
    testTag: String
){
    val isValid = info.isNotBlank()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            title,
            color = colorResource(R.color.light_text),
            style = MaterialTheme.typography.bodyLarge
        )

        if (isValid) {
            Text(
                info,
                color = colorResource(R.color.light_text),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("FTSU:EditMachine:$testTag:Text")
            )
        } else {
            Text(
                text = stringResource(R.string.invalid_tc),
                color = MaterialTheme.colorScheme.error,       // red from theme
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("FTSU:EditMachine:$testTag:InvalidText")
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun Preview(){
    EditMachineInfo(
        console = "Touch Xl",
        frame = "Treadmill",
        model = "Performance Plus",
        consoleSN = "CF1583345618",
        frameSN = "FTM2826127",
        bridgeSN = "B2397203821",
    )
}