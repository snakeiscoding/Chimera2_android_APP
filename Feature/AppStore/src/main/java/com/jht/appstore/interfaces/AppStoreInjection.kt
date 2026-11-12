package com.jht.appstore.interfaces

import android.content.Context
import com.jht.widgets.helpers.isContextHilt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppStoreInjectionEntryPoint {
    fun appStore(): AppStoreModel
}

private object MockAppStoreInjectionEntryPoint : AppStoreInjectionEntryPoint {
    override fun appStore(): AppStoreModel = MockAppStoreModel()
}

fun getAppStoreInjectionEntryPoint(context: Context): AppStoreInjectionEntryPoint {
    return if (isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, AppStoreInjectionEntryPoint::class.java)
    } else {
        MockAppStoreInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}
