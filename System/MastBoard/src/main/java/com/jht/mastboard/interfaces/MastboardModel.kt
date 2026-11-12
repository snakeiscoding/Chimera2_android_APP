package com.jht.mastboard.interfaces

import android.content.Context
import com.jht.concierge.service.ConciergeEvent
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataListener
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.EventListener
import com.jht.mastboard.data.LEDItem
import com.jht.mastboard.data.LEDItem.LEDGroup
import com.jht.mastboard.data.LEDStyle
import com.jht.mastboard.lib.HapticMotorEvent
import com.jht.mock.interfaces.MockDataListener
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockEventListener
import com.jht.widgets.helpers.isContextHilt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/*
    We should try to use as much standard interfaces models as possible.
    Collecting things that are needed but not in the standard interfaces models yet.
    We could probably use a control model of some kind for workout controls?
    We could probably also convert some of this to compose.
 */

abstract class MastboardModel : BaseModel {
    override fun close() {}

    open val isGoAllowed = true

    open val ledDefaultColor: DataValueListener<Int> = MockDataValue(0)
    open val ledWorkoutRunningColor: DataValueListener<Int> = MockDataValue(0)
    open val ledWorkoutPausedColor: DataValueListener<Int> = MockDataValue(0)
    open val ledWorkoutSummaryColor: DataValueListener<Int> = MockDataValue(0)
    open val ledConciergeColor: DataValueListener<Int> = MockDataValue(0)
    open val userLedStyle: DataValueListener<Int> = MockDataValue(LEDStyle.NONE.value)
    open val ledOverride: DataValueListener<Boolean> = MockDataValue(false)

    open val uiEvent: DataValueListener<String> = MockDataValue("")
    open val conciergeEvent: DataValueListener<ConciergeEvent> = MockDataValue(ConciergeEvent.RequestClosed)

    open fun setLedColor(ledItem: LEDItem) {}
    open val onLedColor: DataListener<LEDItem> = MockDataValue(LEDItem(LEDGroup.Default))
    open val ledBrightness: DataValueListener<Int> = MockDataValue(100)
    open val lcdBrightness: DataValueListener<Int> = MockDataValue(100)
    open fun onScreenActivity() {}
    open fun sendHapticEvent(event: HapticMotorEvent) {}
    open val onHapticEvent: DataListener<HapticMotorEvent> = MockDataListener()
    open val hapticMotorEffect: DataValueListener<Int> = MockDataValue(80)
    open val userHapticsEnabled: DataValueListener<Boolean> = MockDataValue(true)
    open val systemHapticsEnabled: DataValueListener<Boolean> = MockDataValue(true)
    open val onLcdDebugLayoutEvent: EventListener = MockEventListener()
    open val lcdDebugPanels: DataValueListener<Boolean> = MockDataValue(false)

    // These could be replaced by passing a control model like compose panels use?
    open fun inclineIncrement(touched: Boolean) {}
    open fun inclineDecrement(touched: Boolean) {}
    open fun speedIncrement(touched: Boolean) {}
    open fun speedDecrement(touched: Boolean) {}
    open fun resistanceIncrement(touched: Boolean) {}
    open fun resistanceDecrement(touched: Boolean) {}
    open fun emulateGoPress() {}
}


class MockMastBoardModel : MastboardModel() {
    // nothing
}


@EntryPoint
@InstallIn(SingletonComponent::class)
interface MastBoardInjectionEntryPoint {
    fun mastboard(): MastboardModel
}
private object MockConciergeInjectionEntryPoint : MastBoardInjectionEntryPoint {
    override fun mastboard(): MastboardModel = MockMastBoardModel()
}
fun getMastBoardInjectionEntryPoint(context: Context?): MastBoardInjectionEntryPoint {
    return if (context != null && isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, MastBoardInjectionEntryPoint::class.java)
    } else {
        MockConciergeInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}
