package com.jht.progression.interfaces

import android.content.Context
import com.jht.widgets.helpers.isContextHilt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProgressionInjectionEntryPoint {
    fun progression(): ProgressionModel
}

private object MockProgressionInjectionEntryPoint : ProgressionInjectionEntryPoint {
    override fun progression(): ProgressionModel = MockProgressionModel()
}

fun getProgressionInjectionEntryPoint(context: Context): ProgressionInjectionEntryPoint {
    return if (isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, ProgressionInjectionEntryPoint::class.java)
    } else {
        MockProgressionInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}
