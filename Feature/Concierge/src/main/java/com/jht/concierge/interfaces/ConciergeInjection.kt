package com.jht.concierge.interfaces

import android.content.Context
import com.jht.widgets.helpers.isContextHilt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConciergeInjectionEntryPoint {
    fun concierge(): ConciergeModel
}

private object MockConciergeInjectionEntryPoint : ConciergeInjectionEntryPoint {
    override fun concierge(): ConciergeModel = MockConciergeModel()
}

fun getConciergeInjectionEntryPoint(context: Context): ConciergeInjectionEntryPoint {
    return if (isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, ConciergeInjectionEntryPoint::class.java)
    } else {
        MockConciergeInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}
