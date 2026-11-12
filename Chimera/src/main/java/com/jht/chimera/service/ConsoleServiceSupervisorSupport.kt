package com.jht.chimera.service

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys
import com.jht.chimera.lib.fleet.BearerTokenHelper
import com.jht.chimera.lib.fleet.createFleetOAuthConfig
import com.jht.chimera.service.fleet.FleetProvision
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.IStartStop
import com.jht.oauthlib.auth.ClientCredentialsFlow
import com.jht.oauthlib.auth.ConsoleScope
import com.jht.oauthlib.integration.oauth.createOAuthConfig
import com.jht.oauthlib.microservices.NotificationService.getBaseUrl
import com.jht.oauthlib.microservices.NotificationService.getChannelName
import com.jht.oauthlib.microservices.NotificationService.getScopes
import com.jht.oauthlib.microservices.NotificationService.getWebSocketUrl
import com.jht.oauthlib.push.WebPushHandler
import com.jht.services.CentralConsoleService
import com.jht.services.ConsoleServiceState
import com.jht.services.IConsoleService
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/*
    This file contains the backend plumbing for the central supervisor service.
    This way we can keep the list of services and their conditions separate of the details.
 */


/** Internals for central supervisor service managing our console services. Allows us to separate child service list from backing code details. */
abstract class ConsoleServiceSupervisorInternals : CentralConsoleService() {
    companion object {
        protected const val TAG = "CentralConsoleService"
    }
    protected val dataMatrix = DataMatrix()
    private val servicesMap = ConcurrentHashMap<String, ServiceTrackingBlock>() // ConcurrentHashMap ?
    private var myState = ConsoleServiceState.UNKNOWN
    private lateinit var fleetProvision : FleetProvision
    private val webPushScope = CoroutineScope(Dispatchers.IO)
    private var webPushReceiver: WebPushHandler? = null
    private var currentEnvironment = -1
    private var currentMachineUUID = ""
    // state tracking for web events
    private var hasValidatedMachineUUID = false // store a local state to handle weird callback edge case
    private var isRfidReady = false
    private var isSystemSetup = false
    private var isNetworkAvailable = false
    private var wasWebReady = false
    private var wasWebPushReady = false
    private var wasBearerReady = false
    /** single-flight guard so concurrent triggers (club ID change, web ready, env/UUID change) serialize provisioning */
    private val provisionMutex = Mutex()

    // for delayed stuff?
    protected fun runDelayed(delayMS: Long = 0, block: ()->Unit) {
        lifecycleScope.launch(CoroutineName("DelayService") + Dispatchers.IO) {
            // kotlin delay returns immediately for non-positive values already
            delay(delayMS)
            block()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        // if this service already registered as ready, this is unexpected restart
        val alreadyRan = DataMatrix.getBoolean(HALKeys.DATA_CONSOLE_SERVICES_INIT_BOOL)
        if (alreadyRan) {
            Log.w(TAG, "Possible crash detected")
        }
        // should we move this whole block to IO thread?
        lifecycleScope.launch(CoroutineName("SupervisorCreate") + Dispatchers.IO) {
            fleetProvision = FleetProvision(applicationContext)
            // setup the managed "services" before any events can fire
            setupServices()
            // in case there are no services to wait for, run a check at the end of setup
            checkBlockingServices()
            // setup push last in case services are looking for callbacks (or should we wait for services to be ready first?)
            setupWebPushNotifications()
        }
    }
    //override fun onBind(intent: Intent?): IBinder? { return null }
    override fun onDestroy() {
        // stop DM first to keep listeners from restarting anything
        dataMatrix.finish()
        // be sure to stop push receiving before cleaning up services
        webPushScope.cancel()
        webPushReceiver?.close()
        cleanupServices()
        Log.d(TAG, "Service destroyed")
        updateMyState(ConsoleServiceState.STOPPED)
        super.onDestroy()
    }

    /** Starts a full android service without any tracking or event passing. */
    protected fun startFullServiceUntracked(serviceClass: Class<*>, asForeground: Boolean = false) {
        val intent = Intent(applicationContext, serviceClass)
        internalStartService(intent, asForeground)
    }
    /** Starts a full android service without any tracking or event passing. */
    protected fun startFullServiceUntracked(packageName: String, className: String, asForeground: Boolean = false) {
        val intent = Intent().setClassName(packageName, className)
        internalStartService(intent, asForeground)
    }
    /** Wrapper function for the raw starting of an android service by intent. Meant for internal use. */
    protected fun internalStartService(intent: Intent, asForeground: Boolean = false) {
        runCatchingCancellable {
            if (asForeground) applicationContext.startForegroundService(intent)
            else applicationContext.startService(intent)
        }.onFailure {
            Log.e(TAG, "Failed to start service ${intent.component}, foreground=$asForeground", it)
        }
    }


    /** Starts a full android service and adds it to tracking list.
     * If it is a [com.jht.services.ChildConsoleService] events should work. A regular service will start/stop but no events.
     * If [blockUntilReady] is true, the system should wait for service to be ready. */
    protected fun startFullService(serviceClass: Class<*>, blockUntilReady: Boolean = false) {
        // service names are safe from proguard as it has keep rule to preserve for android manifest
        startService(serviceClass.canonicalName ?: "", blockUntilReady) { ConsoleServiceWrapper(serviceClass) }
    }
    /** Starts a full android service and adds it to tracking list.
     * If it is a [com.jht.services.ChildConsoleService] events should work. A regular service will start/stop but no events.
     * If [blockUntilReady] is true, the system should wait for service to be ready. */
    protected fun startFullService(serviceClassName: String, blockUntilReady: Boolean = false) {
        startService(serviceClassName, blockUntilReady) { ConsoleServiceWrapper(serviceClassName) }
    }

    /** If given ID is not being tracked, will use given constructor to create, track, and start the [IConsoleService].
     * If [blockUntilReady] is true, the system should wait for service to be ready. */
    protected fun startService(id: String, blockUntilReady: Boolean = false, consoleServiceConstruction: () -> IConsoleService) {
        if (id.isEmpty()) {
            Log.e(TAG, "Cannot add empty service ID")
            return
        }
        // do we need lock on "services" map for safety or totally unnecessary?
        if (!servicesMap.containsKey(id)) {
            runCatchingCancellable { // probably doesn't need to be cancellable?
                // could start inside a coroutine, but do not know unique needs of each of these, so they should probably handle that individually
                val newBlock = ServiceTrackingBlock(consoleServiceConstruction(), blockUntilReady)
                // make sure we added block before calling start
                if (servicesMap.putIfAbsent(id, newBlock) == null) {
                    newBlock.start()
                    // since some services are conditionally started later, they might miss events they would receive normally after starting
                    // could use replayActiveEventsForConsoleService, but we already have these tracked locally, so this is cheaper
                    newBlock.consoleService.onWebReady(wasWebReady)
                    newBlock.consoleService.onWebPushReady(wasWebPushReady)
                    newBlock.consoleService.onBearerReady(wasBearerReady)
                }
            }.onFailure {
                Log.w(TAG, "Failed to start $id", it)
                stopService(id)
            }
        }
    }
    /** If give ID is being tracked, will remove and stop the associated [IConsoleService] */
    protected fun stopService(id: String) {
        // do we need lock on "services" map for safety or totally unnecessary?
        runCatchingCancellable {
            servicesMap.remove(id)?.stop()
        }.onFailure {
            Log.w(TAG, "Failed to stop $id", it)
        }
    }

    /** this is the method where actual service list will be built */
    abstract fun setupServices()
    /** stops all tracked [IConsoleService] and clears tracking list */
    private fun cleanupServices() {
        servicesMap.forEach {
            runCatchingCancellable { it.value.stop() }
        }
        servicesMap.clear()
    }

    /** Sends web ready event to all tracked [IConsoleService], both lightweight and full services */
    override fun sendWebReady(ready: Boolean) {
        wasWebReady = ready
        // get the full services
        super.sendWebReady(ready)
        // get the micro services
        servicesMap.forEach { it.value.consoleService.onWebReady(ready) }
    }
    /** Sends web push ready event to all tracked [IConsoleService], both lightweight and full services */
    override fun sendWebPushReady(ready: Boolean) {
        wasWebPushReady = ready
        // get the full services
        super.sendWebPushReady(ready)
        // get the micro services
        servicesMap.forEach { it.value.consoleService.onWebPushReady(ready) }
    }
    /** Sends bearer ready (fleet provisioning complete) event to all tracked [IConsoleService], both lightweight and full services */
    override fun sendBearerReady(ready: Boolean) {
        wasBearerReady = ready
        // get the full services
        super.sendBearerReady(ready)
        // get the micro services
        servicesMap.forEach { it.value.consoleService.onBearerReady(ready) }
    }
    /** Sends web push event to all tracked [IConsoleService], both lightweight and full services */
    override fun sendWebPush(data: Bundle) {
        // get the full services
        super.sendWebPush(data)
        // get the micro services
        servicesMap.forEach { it.value.consoleService.onWebPushData(data) }
    }

    /** updates state for a tracked [IConsoleService], will recheck status of blocked services if needed */
    override fun onChildStateChange(id: String, state: ConsoleServiceState) {
        Log.d(TAG, "Service='$id' State=$state")
        servicesMap[id]?.state = state
        if (state == ConsoleServiceState.READY) checkBlockingServices()
    }
    override fun onChildRegister(id: String) {
        Log.w(TAG, "Register from $id")
    }
    override fun onChildUnregister(id: String) {
        Log.w(TAG, "Unregister from $id")
    }
    override fun onChildCommunicationLoss(id: String) {
        Log.w(TAG, "Lost contact with $id")
    }
    override fun onChildPossibleCrash(id: String, message: String) {
        Log.w(TAG, "Possible crash from $id $message")
    }

    /** checks if all blocking services are READY, if so updates our state to READY */
    private fun checkBlockingServices() {
        val blockPending = servicesMap.values.filter { it.blockUntilReady && it.state != ConsoleServiceState.READY }
        if (blockPending.isEmpty()) {
            updateMyState(ConsoleServiceState.READY)
        } else {
            // we could in theory time the waiting and take some kind of timeout actions?
            Log.d(TAG, "Waiting for ${blockPending.joinToString { it.consoleService.id }}")
        }
    }
    /** update our internal state and the global console services ready flag */
    private fun updateMyState(state: ConsoleServiceState) {
        myState = state
        DataMatrix.updateBoolean(HALKeys.DATA_CONSOLE_SERVICES_INIT_BOOL, state == ConsoleServiceState.READY)
    }

    //#region WEB PUSH NOTIFICATION
    /** Setup listeners to create/update web push handler */
    private fun setupWebPushNotifications() {

        dataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, {
            webPushScope.launch(CoroutineName("ClubReFleet")) {
                runCatchingCancellable {
                    // call provision to update the wallet keys
                    performFleetProvision()
                }.onFailure { Log.e(TAG, "performFleetProvision FAILED", it) }
            }
        }, false)

        // setup web ready and push handling
        dataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, {
            // changing dapi env will trigger machine UUID logic but state can be wrong so update local state first
            hasValidatedMachineUUID = false
            updateWebPushReceiver(newEnvironment = it)
        }, false)
        dataMatrix.createStringAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING, {
            updateWebPushReceiver(newMachineUUID = it)
        }, false)
        dataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, {
            isNetworkAvailable = it
            checkIfWebReady()
        }, false)
        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, {
            isSystemSetup = it
            checkIfWebReady()
        }, false)
        dataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, {
            hasValidatedMachineUUID = it
            checkIfWebReady()
        }, false)
        dataMatrix.createBoolAdapter(HALKeys.DATA_RFID_FINDER_FIRST_TRY_COMPLETE_BOOL, {
            isRfidReady = it
            checkIfWebReady()
        }, false)
        checkIfWebReady(true)
        // FOR TESTING WEB PUSH without web
        dataMatrix.createStringAdapter("ACTION_TEST_PUSH_TYPE_STRING") { type ->
            Log.w(TAG, "TESTING WEB PUSH type='$type'")
            val data = Bundle().apply {
                putString("type", type)
                putString("payload", "")
                putString("timestamp", Instant.now().toString())
            }
            sendWebPush(data)
        }
    }

    /** perform web ready checking, trigger check for web push if needed */
    private fun checkIfWebReady(forceFreshValueReads: Boolean = false) {
        if (forceFreshValueReads) {
            isNetworkAvailable = DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL)
            isSystemSetup = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)
            hasValidatedMachineUUID = DataMatrix.getBoolean(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL)
            currentEnvironment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT)
            currentMachineUUID = DataMatrix.getString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING)
            isRfidReady = DataMatrix.getBoolean(HALKeys.DATA_RFID_FINDER_FIRST_TRY_COMPLETE_BOOL)
            if (!ProductConfig.hasRFID()) isRfidReady = true // set this to true if console doesn't support RFID
        }
        val webReady = isNetworkAvailable && isSystemSetup && hasValidatedMachineUUID && isRfidReady
        if (wasWebReady != webReady || forceFreshValueReads) {
            sendWebReady(webReady)
            // technically the push receiver has connection retries, but we may want to bypass waiting for the retry timer?
            updateWebPushReceiver(force = true)
        }
    }



    /** creates or updates instance of web push handler, if necessary */
    private fun updateWebPushReceiver(newEnvironment: Int = currentEnvironment, newMachineUUID: String = currentMachineUUID, force: Boolean = false) {
        Log.d(TAG, "Check push receiver, environment=$newEnvironment, UUID=$newMachineUUID")
        if (currentEnvironment == newEnvironment && currentMachineUUID == newMachineUUID && !force) return // if different or forced
        // stop the current push handler (should we attempt to join the old job first?)
        if (wasWebPushReady) sendWebPushReady(false)
        if (wasBearerReady) sendBearerReady(false)
        Log.d(TAG, "Stop old push receiver, environment=$currentEnvironment, UUID=$currentMachineUUID")
        webPushScope.coroutineContext.cancelChildren()
        webPushReceiver?.close()
        // update console info
        currentEnvironment = newEnvironment
        currentMachineUUID = newMachineUUID
        // check if we can start new push handler
        if (currentEnvironment < 0 || currentMachineUUID.isEmpty()) return // if valid environment and machine
        if (!wasWebReady) {
            Log.w(TAG, "Machine is not ready for web end points: isNetworkAvailable=$isNetworkAvailable, isSystemSetup=$isSystemSetup, hasValidatedMachineUUID=$hasValidatedMachineUUID, isRfidReady=$isRfidReady")
            return
        }
        webPushScope.launch(CoroutineName("WebPushStart")) {
            runCatchingCancellable {
                Log.d(TAG, "--> WebPushStart performFleetProvision")
                performFleetProvision()
                // provisioning succeeded, bearer tokens are now obtainable via fleet
                sendBearerReady(true)

                // https://jhtnaee.atlassian.net/wiki/spaces/SA/pages/4905992193/Push+Solution+-+AWS+AppSync
                // https://jhtnaee.atlassian.net/wiki/spaces/SA/pages/5658673359/Console+Implementation
                Log.d(TAG, "Restart push receiver, environment=$currentEnvironment, UUID=$currentMachineUUID")
                val dapiEnvironment = currentEnvironment
                val machineUuid = currentMachineUUID.ifEmpty { "00000000-0000-0000-0000-000000000000" }
                val channel = getChannelName(machineUuid, "machine") // original poc was "console"
                val scopes = ConsoleScope.compose(getScopes(channel))

                // try to get token via fleet first
                var bearerToken: String? = getFleetOidcBearerToken()
                // fallback to get token
                if (bearerToken == null) {
                    Log.d(TAG, "Failed to acquire bearer token via fleet")
                    bearerToken = getOidcBearerToken(dapiEnvironment, scopes)
                }
                if (bearerToken == null) {
                    Log.e(TAG, "Failed to acquire bearer token, cannot setup WebPush notifications")
                    return@launch
                }
                val baseUrl = getBaseUrl(dapiEnvironment)
                val wsUrl = getWebSocketUrl(dapiEnvironment)
                //Log.d(TAG, "Push urls $baseUrl $wsUrl on ch $channel")
                webPushReceiver = WebPushHandler(bearerToken, channel, "$baseUrl/graphql", wsUrl, onPushStartup = {
                    // TODO - web push receiver should provide callback for up and down statuses
                    sendWebPushReady(true)
                }, onPushData = { data ->
                    Log.d(TAG, "Push received $data")
                    sendWebPush(data)
                })
            }.onFailure { Log.e(TAG, "updateWebPushReceiver FAILED", it) }
        }
    }

    /** provision fleet device */
    suspend fun performFleetProvision() {
        provisionMutex.withLock {
            fleetProvision.provision()
        }
    }

    private suspend fun getFleetOidcBearerToken(): String? {
        return runCatchingCancellable {
            withContext(Dispatchers.IO) {
                BearerTokenHelper.getAccessToken(applicationContext).getOrNull()
            }
        }.onFailure {e ->
            Log.e(TAG, "Failed to acquire OAuth token", e)
        }.getOrNull()
    }

    /** Gets token for web push receiver (copied with modification from artifact example service) */
    private suspend fun getOidcBearerToken(
        deployment: Int,
        scopes: String
    ): String? {
        return runCatchingCancellable {
            val config = createOAuthConfig(deployment)
            val clientCredentials = ClientCredentialsFlow(config)
            val result = clientCredentials.authenticate(deployment, scopes)
            result.getOrNull()?.accessToken
        }.onFailure {e ->
            Log.e(TAG, "Failed to acquire OAuth token", e)
        }.getOrNull()
    }
    //#endregion WEB PUSH NOTIFICATION
} // END MAIN SERVICE



/** Wraps [IConsoleService] with extra tracking meta data */
private class ServiceTrackingBlock(
    /** [IConsoleService] being tracked */
    val consoleService: IConsoleService,
    /** True if system should wait for it to be ready */
    var blockUntilReady: Boolean = false,
) : IStartStop {
    /** Tracking if service is ready (important if it is a blocking service) */
    var state = ConsoleServiceState.UNKNOWN // default unknown in case some wrapped services do not provide state
    override fun start() { consoleService.start() }
    override fun stop() { consoleService.stop() }
    override fun toString(): String = "(ID=${consoleService.id} STATE=$state Blocking=$blockUntilReady)"
    // should we have a list of IDs required to be ready before this service can run? a sort of dependency tree?
}
