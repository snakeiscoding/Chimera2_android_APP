package com.jht.widgets.helpers

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.MarketingModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockIdleTimerModel
import com.jht.mock.interfaces.models.MockLoginModel
import com.jht.mock.interfaces.models.MockMarketingModel
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockUserModel
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.mock.interfaces.models.MockWorkoutLauncherModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.mock.interfaces.models.MockWorkoutSummaryModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager

/**
 * Provides a means to do arbitrary injection of models from an application context.
 *  View and ViewModel injection tends to require Activity or Fragment context.
 *  This provides a more manual alternative method.
 *
 * Example usage from an arbitrary class:
 * ```
 * val hiltEntryPoint = getInjectionEntryPoint(context)
 * deviceModel = hiltEntryPoint.devices()
 * systemModel = hiltEntryPoint.system()
 * ```
 *
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface InjectionEntryPoint {
    fun marketing(): MarketingModel
    fun devices(): DeviceModel
    fun login(): LoginModel
    fun user(): UserModel
    fun navigation(): NavigationModel
    fun system(): SystemModel
    fun idleTimer(): IdleTimerModel
    fun ui(): UIServiceModel
    fun uiBase(): UIBaseModel
    fun workoutLauncher(): WorkoutLauncherModel
    fun workoutCatalog(): WorkoutCatalogModel
    fun workout(): WorkoutModel
    fun workoutSummary(): WorkoutSummaryModel
}

/** Generic mock version of InjectionEntryPoint, primarily for compose previews */
private object MockInjectionEntryPoint : InjectionEntryPoint {
    override fun marketing() = MockMarketingModel()
    override fun devices(): DeviceModel = MockDeviceModel()
    override fun login(): LoginModel = MockLoginModel()
    override fun user(): UserModel = MockUserModel()
    override fun navigation(): NavigationModel = MockNavigationModel()
    override fun system(): SystemModel = MockSystemModel()
    override fun idleTimer(): IdleTimerModel = MockIdleTimerModel()
    override fun ui(): UIServiceModel = MockUIServiceModel()
    override fun uiBase(): UIBaseModel = MockUIBaseModel()
    override fun workoutLauncher(): WorkoutLauncherModel = MockWorkoutLauncherModel()
    override fun workoutCatalog(): WorkoutCatalogModel = MockWorkoutCatalogModel()
    // previews are the case that wants moving stats
    override fun workout(): WorkoutModel = MockWorkoutModel().apply { startDistanceSimulation() }
    override fun workoutSummary(): WorkoutSummaryModel = MockWorkoutSummaryModel()
}

/** Attempts to retrieve InjectionEntryPoint from context, if not available provides generic mocks */
fun getInjectionEntryPoint(context: Context): InjectionEntryPoint {
    return if (isContextHilt(context)) {
        EntryPointAccessors.fromApplication(context, InjectionEntryPoint::class.java)
    } else {
        MockInjectionEntryPoint // this case is most likely compose previews, live app and testing should have injection from HILT
    }
}

/**
 * Provide method for detecting if HILT is running.
 *  LocalInspectionMode does not catch previewing in emulator or hardware, this can.
 */
fun isContextHilt(context: Context) : Boolean {
    // this check is based on what HILT checks for inside of EntryPointAccessors.fromApplication()
    val app = getApplicationFromContext(context.applicationContext)
    return app is GeneratedComponent || app is GeneratedComponentManager<*>
}

@Composable
fun isLiveApplication() : Boolean = isContextHilt(LocalContext.current)

@Composable
fun LiveApplicationOnly(
    content: @Composable () -> Unit
) {
    if (isContextHilt(LocalContext.current)) {
        content()
    }
}

/** This is non-exception version of the HILT helper Contexts.getApplication() to extract Application from context. Returns null if it cannot. */
fun getApplicationFromContext(context: Context): Application? {
    if (context is Application) {
        return context
    }
    var unwrapContext: Context? = context
    while (unwrapContext is ContextWrapper) {
        unwrapContext = unwrapContext.baseContext
        if (unwrapContext is Application) {
            return unwrapContext
        }
    }
    return null
}
