package com.jht.outoforder

import androidx.compose.runtime.mutableIntStateOf
import com.jht.enums.UiEvent
import com.jht.interfaces.models.SystemModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class OutOfOrderViewModel @Inject constructor(
    private val systemModel: SystemModel,
) : ThemedViewModel() {

    val errorCode by lazy { mutableIntStateOf(0) }


    override fun start() {
        systemModel.sendUiEvent(UiEvent.OUT_OF_ORDER_SCREEN) // don't think the grips show anything for ooo, but here nonetheless
        systemModel.errorCode.listenAndTrigger {
            errorCode.intValue = it
        }
    }

    override fun stop() {
        systemModel.close()
    }

}