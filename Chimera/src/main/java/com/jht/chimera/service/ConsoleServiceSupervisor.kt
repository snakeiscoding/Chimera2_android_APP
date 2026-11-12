package com.jht.chimera.service

import android.app.Service
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.jht.androidcommonalgorithms.events.JHTNotification
import com.jht.androidcommonalgorithms.util.SystemDetection
import com.jht.appstore.service.AppStoreService
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.chimera.lib.common.usb.StorageUtils
import com.jht.chimera.service.android.AndroidConsoleService
import com.jht.chimera.service.consoleconfig.ConsoleConfigService
import com.jht.chimera.service.dapi.DAPIService
import com.jht.chimera.service.gattserver2.GattServerServiceConsole
import com.jht.chimera.service.hal.HALService
import com.jht.chimera.service.hal.IOService
import com.jht.chimera.service.hal.LCBService
import com.jht.chimera.service.johnsonwebendpoints.TranslationsWeb
import com.jht.chimera.service.logger.LoggerService
import com.jht.chimera.service.matrixcomm.MatrixCommService
import com.jht.chimera.service.messagecenter.MessageCenterService
import com.jht.chimera.service.sso.SingleSignOnService
import com.jht.chimera.service.systemsettings.SystemSettings
import com.jht.chimera.service.tiers.Tiers
import com.jht.chimera.service.tiers.TiersJump
import com.jht.chimera.service.ui.UiService
import com.jht.chimera.service.usbmedia.UsbMediaService
import com.jht.chimera.service.workout.WorkoutControllerService
import com.jht.chimera.service.workout.WorkoutSaverService
import com.jht.chimera.ui.mainapp.workouttracking.WorkoutTrackingService
import com.jht.clubcom.service.ClubComConsoleService
import com.jht.concierge.service.ConciergeConsoleService
import com.jht.datamatrixlib.DataMatrix
import com.jht.engage360.Engage360Service
import com.jht.util.ContextHelper
import com.jht.utility.runCatchingCancellable
import com.jht.virtualactive.service.VAService
import java.io.File


/*
    This file contains only the setting up of services.
    The support file contains the detailed plumbing.
    This allows us to separate maintenance of our service list and their conditionals vs the actual mechanism that manages it.
 */

/**
 * The central service management for the console. This is a full android [Service] and requires manifest entry.
 * An individual [com.jht.services.IConsoleService] represents a generic background "service" that manages some feature, but might not require a full android [Service].
 * A "lightweight service" or "micro service" or inlined service could just be a class extending [com.jht.services.InlineConsoleService] that we can start/stop.
 * However a full android service that implements [com.jht.services.ChildConsoleService] can be added as well and will be wrapped.
 * A full android service [Service] can also be wrapped and tracked, but will not support event passing provided by [com.jht.services.ChildConsoleService].
 * It is also possible to start regular android service as untracked and unwrapped as well.
 * So a breakdown of possible "services"
 *  - Lightweight or inlined service class, extends the abstract [com.jht.services.InlineConsoleService]
 *  - Full android service with event support, extends [com.jht.services.ChildConsoleService]
 *  - Full android service without event support, extends android stock service classes
 *  Full android services can also be started without tracking if desired. An inline service is always tracked so we can stop it.
 *
 * NOTE: See [ConsoleServiceSupervisorInternals] for implementation details. This class isolates the actual services we setup for clarity.
 * */
class ConsoleServiceSupervisor : ConsoleServiceSupervisorInternals() {
    /** Setup our console services. Some may start/stop conditionally based on a setting. */
    override fun setupServices() {
        // original code would start hardware services on emulator, but not wait for them
        val waitForHardware = !SystemDetection.isEmulator()

        startUpdateService()

        // blocking services are important, and should probably be fully tracked
        startFullService(HALService::class.java, blockUntilReady = true)
        startFullService(IOService::class.java, blockUntilReady = waitForHardware)
        startFullService(LCBService::class.java, blockUntilReady = waitForHardware)
        startFullService(WorkoutControllerService::class.java, blockUntilReady = true)
        startFullService(DAPIService::class.java, blockUntilReady = true)
        startFullService(UiService::class.java, blockUntilReady = true)

        // are the non-blocking full android services worth tracking? some these could be made into lightweight services possibly?
        startFullServiceUntracked(SystemSettings::class.java)
        startFullServiceUntracked(UsbMediaService::class.java)
        startFullServiceUntracked(LoggerService::class.java)
        startFullServiceUntracked(MatrixCommService::class.java)
        startFullServiceUntracked(WorkoutTrackingService::class.java)
        startFullServiceUntracked(IPGeolocationService::class.java)

        startService(ConsoleConfigService.ID, blockUntilReady = true) { ConsoleConfigService(context = applicationContext, onStateChange = ::onChildStateChange) }
        startService(AndroidConsoleService.ID){ AndroidConsoleService(applicationContext) }

        // at this point I think everything has SSO, do we need this config anymore ?
        if (BuildConfig.SUPPORTS_SSO) startFullServiceUntracked(SingleSignOnService::class.java)

        //region MARKET CHECKING
        if (ProductConfig.isRetail()) {
            // retail only
            startFullServiceUntracked(WorkoutSaverService::class.java)
        } else if (ProductConfig.isCommercial()) {
            // commercial only
            dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL, {
                if (it) startService(ConciergeConsoleService.ID) { ConciergeConsoleService(applicationContext) }
                else stopService(ConciergeConsoleService.ID)
            }, true)

            dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL, {
                if (it) startService(ClubComConsoleService.ID) { ClubComConsoleService(applicationContext) }
                else stopService(ClubComConsoleService.ID)
            }, true)

            // DAPI fallback changes need a reboot, so do not bother with live changes
            if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_DAPI_FALLBACK_BOOL)) {
                startFullServiceUntracked(Tiers::class.java)
            } else {
                startService(TiersJump.ID) { TiersJump(applicationContext) }
            }

            startService(MessageCenterService.ID) { MessageCenterService(applicationContext) }
        }
        //endregion MARKET CHECKING

        startService(TranslationsWeb.ID) { TranslationsWeb(applicationContext) }

        if (ProductConfig.supportsPtp() && ProductConfig.hasPremiumUi()) {
            startFullServiceUntracked(Engage360Service::class.java)
        }
        if (ProductConfig.isProjectOne()) {
            startFullService("com.jht.mastboard.MastBoardCommService", blockUntilReady = true)
        }
        if (ProductConfig.hasAppStore()) {
            dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL, {
                ConsoleEvent.logEvent("AppStore callback enable=$it with filter name=${ProductConfig.getConsoleType().appStoreFilterName}")
                if (it) startService(AppStoreService.ID) { AppStoreService(applicationContext) }
                else stopService(AppStoreService.ID)
            }, true)
        }

        /*
            do these really need delays? if so, should we build that into our startService methods?
         */
        // Some services like GATT we need to start later as we are seeing ANR's in the system UI on boot.
        runDelayed(10_000) { startFullServiceUntracked(GattServerServiceConsole::class.java) }
        // Need to delay VA on Chimera 2 to wait for SD card to mount.
        runDelayed(500) { startFullServiceUntracked(VAService::class.java) }
    }


    /** Starts our update service by name either as foreground or not depending on certain factors */
    private fun startUpdateService() {
        // NOTE: This is duplicated between chimera ServiceStarter and UpdateService ServiceStarter
        // Disabling foreground service because it sometimes throws java.lang.SecurityException: Permission Denial: startForeground from pid=3312, uid=10069 requires android.permission.FOREGROUND_SERVICE
        val startAsForeground = BuildConfig.FLAVOR_platform.equals("chimera") || BuildConfig.FLAVOR_platform.equals("lcgui")
        val updatePackage = "com.jht.updateservice"
        val updateClass = "com.jht.updateservice.UpdateService"
        Log.d(TAG, "Starting $updateClass")
        startFullServiceUntracked(updatePackage, updateClass, startAsForeground)
    }
} // END SERVICE



/**
 * Helper to start/stop/wait for services (Replaces old ServiceStarter)
 * Most functions are static methods except for waiting which requires an instance for state tracking.
 * (Keep in mind this could be used in various processes separate from the service)
 * If you need an instance try to create on main thread. DataMatrix instance prefers it.
 * */
class ConsoleServicesHelper : AutoCloseable {
    companion object {
        private const val TAG = "ConsoleServicesHelper"
        /** Start console services */
        @JvmStatic fun startServices() {
            if (File(StorageUtils.getUSBPath(), "jht_disable_chimera.txt").exists()) return
            runCatchingCancellable {
                ContextHelper.context?.let { context ->
                    context.startService(Intent(context, ConsoleServiceSupervisor::class.java))
                }
            }.onFailure {
                Log.e(TAG, "Failed to start services supervisor", it)
            }
        }
        /** Stop console services */
        @JvmStatic fun stopServices() {
            runCatchingCancellable {
                ContextHelper.context?.let { context ->
                    context.stopService(Intent(context, ConsoleServiceSupervisor::class.java))
                }
            }.onFailure {
                Log.e(TAG, "Failed to stop services supervisor", it)
            }
        }
        /** Returns if console services are ready */
        @JvmStatic fun servicesReady(): Boolean = DataMatrix.getBoolean(HALKeys.DATA_CONSOLE_SERVICES_INIT_BOOL)
        /** Stops services that cause issue during factory reset (Should review this at some point? Could service do this based on a trigger?) */
        @JvmStatic fun stopServicesForReset() {
            // when doing things like a factory reset, these services can cause issues
            // caller might already be calling from a new thread, so don't create another one here
            ContextHelper.context?.let { context ->
                listOf(
                    WorkoutSaverService::class.java,
                    IPGeolocationService::class.java,
                    GattServerServiceConsole::class.java,
                ).forEach { serviceClass ->
                    val intent = Intent(context, serviceClass)
                    runCatchingCancellable {
                        context.stopService(intent)
                    }.onFailure {
                        Log.w(TAG, "stopServicesForReset failed for ${intent.component}")
                    }
                }
            }
        }
    }

    // waiting will require some form of state (could convert to coroutine suspend function when more code uses coroutines)
    private val dataMatrix = DataMatrix()
    private val servicesStarted: JHTNotification = JHTNotification()
    override fun close() = dataMatrix.finish()
    init {
        // this will listen for the global ready status of the console services
        dataMatrix.createBoolAdapter(HALKeys.DATA_CONSOLE_SERVICES_INIT_BOOL, { ready ->
            if (ready) servicesStarted.setNotification()
        }, true)
    }

    /** Wait for essential console services to report ready status (splash screen needs a minimum wait time) */
    fun waitForServices(minimumWaitTimeMS: Int = 0) {
        val startTimeMS = SystemClock.elapsedRealtime()
        servicesStarted.waitForNotification()
        if (minimumWaitTimeMS > 0) {
            val timeElapsed = SystemClock.elapsedRealtime() - startTimeMS
            val timeRemaining = minimumWaitTimeMS - timeElapsed
            if (timeRemaining > 1000) {
                runCatchingCancellable { Thread.sleep(timeRemaining) }
            }
        }
    }
} // END ConsoleServiceHelper
