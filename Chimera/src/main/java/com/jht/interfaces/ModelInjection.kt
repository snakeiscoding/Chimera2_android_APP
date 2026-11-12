package com.jht.interfaces

import android.content.Context
import android.net.wifi.WifiManager
import com.jht.annotations.IODispatcher
import com.jht.annotations.MainDispatcher
import com.jht.appstore.interfaces.AppStoreModel
import com.jht.chimera.service.appstore.MatrixAppStoreModel
import com.jht.clubcom.interfaces.ClubComModel
import com.jht.concierge.interfaces.ConciergeModel
import com.jht.engage360.Engage360SystemModel
import com.jht.interfaces.models.AppModel
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.MarketingModel
import com.jht.interfaces.models.MatrixAppModel
import com.jht.interfaces.models.MatrixClubComModel
import com.jht.interfaces.models.MatrixDeviceModel
import com.jht.interfaces.models.MatrixEngage360SystemModel
import com.jht.interfaces.models.MatrixIdleTimerModel
import com.jht.interfaces.models.MatrixLoginModel
import com.jht.interfaces.models.MatrixMarketingModel
import com.jht.interfaces.models.MatrixMastBoardModel
import com.jht.interfaces.models.MatrixNavigationModel
import com.jht.interfaces.models.MatrixProgressionModel
import com.jht.interfaces.models.MatrixSystemModel
import com.jht.interfaces.models.MatrixUIBaseModel
import com.jht.interfaces.models.MatrixUIServiceModel
import com.jht.interfaces.models.MatrixUserModel
import com.jht.interfaces.models.MatrixVideoInputModel
import com.jht.interfaces.models.MatrixWorkoutCatalogModel
import com.jht.interfaces.models.MatrixWorkoutLauncherModel
import com.jht.interfaces.models.MatrixWorkoutModel
import com.jht.interfaces.models.MatrixWorkoutSummaryModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.VideoInputModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.interfaces.models.experimental.MatrixConciergeModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.mastboard.interfaces.MastboardModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class, ServiceComponent::class)
internal object ModelInjection {
    @Provides
    fun provideMarketingModel() : MarketingModel = MatrixMarketingModel()

    @Provides
    fun provideEngage360SystemModel() : Engage360SystemModel = MatrixEngage360SystemModel()

    @Provides
    fun provideMastBoardModel() : MastboardModel = MatrixMastBoardModel()

    @Provides
    fun provideClubComModel() : ClubComModel = MatrixClubComModel()

    @Provides
    fun provideSystemModel() : SystemModel = MatrixSystemModel()

    @Provides
    fun provideIdleTimerModel() : IdleTimerModel = MatrixIdleTimerModel()

    @Provides
    fun provideUIServiceModel() : UIServiceModel = MatrixUIServiceModel()

    @Provides
    fun provideWorkoutModel(@ApplicationContext context: Context) : WorkoutModel = MatrixWorkoutModel(context)

    @Provides
    fun provideLoginModel() : LoginModel = MatrixLoginModel()

    @Provides
    fun provideDeviceModel() : DeviceModel = MatrixDeviceModel()

    @Provides
    fun provideUIBaseModel() : UIBaseModel = MatrixUIBaseModel()

    @Provides
    fun provideProgressionModel() : ProgressionModel = MatrixProgressionModel()

    @Provides
    fun provideUserModel() : UserModel = MatrixUserModel()

    @Provides
    fun provideWorkoutLauncherModel() : WorkoutLauncherModel = MatrixWorkoutLauncherModel()

    @Provides
    fun providesAppModel() : AppModel = MatrixAppModel()

    @Provides
    fun providesVideoInputModel() : VideoInputModel = MatrixVideoInputModel()

    @Provides
    fun provideWorkoutSummaryModel() : WorkoutSummaryModel = MatrixWorkoutSummaryModel()

    @Provides
    fun provideNavigationModel() : NavigationModel = MatrixNavigationModel()

    @Provides
    fun provideConciergeModel() : ConciergeModel = MatrixConciergeModel()

    @Provides
    fun provideWorkoutCatalogModel() : WorkoutCatalogModel = MatrixWorkoutCatalogModel()

    @IODispatcher
    @Provides
    fun providesIODispatcher() : CoroutineDispatcher {
        return Dispatchers.IO
    }

    @MainDispatcher
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    fun providesWifiManager(
        @ApplicationContext applicationContext: Context
    ) : WifiManager{
        return applicationContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @Provides
    fun provideAppStoreModel() : AppStoreModel = MatrixAppStoreModel()
}
