package com.jht.interfaces

import android.content.Context
import com.jht.annotations.IODispatcher
import com.jht.firsttimesetup.FirstTimeSetupApi
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.FtsuClient
import com.jht.interfaces.models.MatrixFirstTimeSetupModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FtsuDiModule {

    @Singleton
    @Provides
    fun providesFirstTimeSetupModel(api: FirstTimeSetupApi, @ApplicationContext context: Context, scope: CoroutineScope): FirstTimeSetupModel =
        MatrixFirstTimeSetupModel(api, context, scope)

    @Singleton
    @Provides
    fun providesFistTimeSetupApi(client: FtsuClient): FirstTimeSetupApi {
        return FirstTimeSetupApi(client)
    }


    @Singleton
    @Provides
    fun providesApplicationScope(
        @IODispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)


}
