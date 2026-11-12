package com.jht.firsttimesetup.viewmodels

import android.util.Log
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val TAG = "EditMachineInfoViewModel"

@HiltViewModel
class EditMachineInfoViewModel @Inject constructor(
    private val systemModel: SystemModel,
    private val uiBaseModel: UIBaseModel,
    private val ftsuModel: FirstTimeSetupModel,
) : ThemedViewModel() {


    private var _machineInfo = MutableStateFlow(MachineInfoUiModel())

    internal val machineInfo = _machineInfo.asStateFlow()


    init {

        _machineInfo.value = machineInfo.value.copy(
            bridgeSn = systemModel.serialNumberBridge,
            consoleSn = systemModel.serialNumberConsole,
            frameSn = systemModel.serialNumberFrame,
            frame = ftsuModel.getFrameType(systemModel.serialNumberFrame),
            console = ftsuModel.getConsoleMarketingName(),
            model = ftsuModel.getFrameMarketingName(systemModel.serialNumberFrame),
        )
    }

    fun isOnyx() : Boolean {
        return ftsuModel.isOnyx()
    }

    fun updateConsoleSn(newSn: String) {
        _machineInfo.value = _machineInfo.value.copy(
            consoleSn = newSn,
        )
    }

    fun updateFrameSn(newSn: String) {
        _machineInfo.value = _machineInfo.value.copy(
            frameSn = newSn,
            frame = ftsuModel.getFrameType(newSn),
            model = ftsuModel.getFrameMarketingName(newSn),
        )
    }

    fun autofillPrefix(newPrefix: String, oldPrefixList: List<String>) {
        val oldFrameSn = _machineInfo.value.frameSn

        var framePostfix = oldFrameSn

        oldPrefixList.forEach { prefix ->
            if(oldFrameSn.contains(prefix)) {
                framePostfix = oldFrameSn.removePrefix(prefix)
            }
        }

        updateFrameSn("$newPrefix$framePostfix")
    }

    fun updateBridgeSn(newSn: String) {
        _machineInfo.value = _machineInfo.value.copy(
            bridgeSn = newSn,
        )
    }


    fun saveChanges() {
        try {
            ftsuModel.updateConsoleSerialNumber(machineInfo.value.consoleSn)
            ftsuModel.updateFrameSerialNumber(machineInfo.value.frameSn)
            if (ftsuModel.isOnyx()) {
                ftsuModel.updateBridgeSerialNumber(machineInfo.value.bridgeSn)
            }
            ftsuModel.setFrame(machineInfo.value.frameSn)
        } catch (e: Exception) {
            // apparently setFrame can throw a null pointer if the frame cannot be found so just
            // to be safe, catch any exception and log it
            Log.e(TAG, "saveChanges error", e)
        }
    }
}


/**
 * Ui model for info related to current console and frame
 */
internal data class MachineInfoUiModel(
    val bridgeSn: String = "",
    val frame: String = "",
    val model: String = "",
    val consoleSn: String = "",
    val frameSn: String = "",
    val console: String = "",

)