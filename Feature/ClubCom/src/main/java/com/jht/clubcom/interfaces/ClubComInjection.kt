package com.jht.clubcom.interfaces

import android.content.Context
import com.jht.widgets.helpers.isContextHilt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClubComInjectionEntryPoint {
    fun clubCom(): ClubComModel
}

private object MockClubComInjectionEntryPoint : ClubComInjectionEntryPoint {
    override fun clubCom(): ClubComModel = MockClubComModel()
}

fun getClubComInjectionEntryPoint(context: Context): ClubComInjectionEntryPoint {
    return if (isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, ClubComInjectionEntryPoint::class.java)
    } else {
        MockClubComInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}
