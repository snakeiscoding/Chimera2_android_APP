package com.jht.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jht.interfaces.IStartStop
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass


/**
    This file contains interfaces and classes to aid in generic service handling or service coordination.

    We have a number of "services". Some are really just background tasks usually driven by system events,
        but some are process isolated full android services. And we are trying to manage all of them from a central supervisor.
        Usually simple web endpoint "service" only need to be lightweight "service" reacting to web events from central web listener.

    Some breakdown:
    - [IConsoleService] represents a generic interface for managing a console service.
    - [InlineConsoleService] is a lightweight "service" that is basically just a class that will run where created.
    - [ChildConsoleService] is a full android service base that can emulate the behavior of a ConsoleService via a wrapper.
    - [CentralConsoleService] is a full android service meant to run and manage a collection of IConsoleService items.
        It has mechanisms to coordinate with instances of ChildConsoleService via IPC to emulate the IConsoleService interfacing.

    These exist in a shared module so that the implementation of both lightweight and full services can themselves be in modules.
    The actual implementation of [CentralConsoleService] will be in the main app.
    [ChildConsoleService] will try to locate the current [CentralConsoleService] by name in the manifest meta-data:
    <service
        android:name=".service.ConsoleServiceSupervisor"
        android:process="com.jhtp.services"
        android:exported="false">
        <meta-data
            android:name="com.jht.services.isCentralService"
            android:value="true" />
    </service>
    <meta-data
        android:name="com.jht.services.centralPackageName"
        android:value="com.jht.chimera.service" />
    <meta-data
        android:name="com.jht.services.centralClassName"
        android:value="com.jht.chimera.service.ConsoleServiceSupervisor" />
 */


/** Main states of a [InlineConsoleService] */
enum class ConsoleServiceState {
    UNKNOWN, STARTED, READY, STOPPED;
    companion object {
        fun fromString(value: String?): ConsoleServiceState? = entries.find { it.name == value }
        fun fromOrdinal(index: Int): ConsoleServiceState? = entries.getOrNull(index)
    }
}
typealias ConsoleServiceStateCallback = (id: String, state: ConsoleServiceState) -> Unit

/** Interface defining common functions for console services */
interface IConsoleService : IStartStop {
    /** Get the unique identifier for this service */
    val id: String
    /** Called when web subsystem is usable */
    fun onWebReady(ready: Boolean)
    /** Called when web push system is running, potential requiring a new initial read */
    fun onWebPushReady(ready: Boolean)
    /** Called when web push data comes in from web. Service can determine if it can use it or not. */
    fun onWebPushData(data: Bundle)
    /** Called when fleet provisioning has completed and bearer tokens are obtainable.
     * The token itself should be pulled on demand (e.g. via BearerTokenHelper) as tokens expire; this event only gates availability.
     * Default no-op so existing implementations are unaffected. */
    fun onBearerReady(ready: Boolean) {}
}


/** Base class for a managed generic console "service" or console lightweight inlined service, where a full android service is not needed */
abstract class InlineConsoleService(
    val context: Context,
    val onStateChange: ConsoleServiceStateCallback = { _, _ -> },
) : IConsoleService {
    /** internal helper to notify host of state change within this service */
    protected fun setServiceState(state: ConsoleServiceState) {
        onStateChange(id, state)
    }
    /** Called to start the service. It is not expected to resume a stopped service. */
    override fun start() {}
    /** Called to stop the service. It is not expected to resume a stopped service. */
    override fun stop() {}
    override fun onWebReady(ready: Boolean) {}
    override fun onWebPushReady(ready: Boolean) {}
    override fun onWebPushData(data: Bundle) {}
    override fun onBearerReady(ready: Boolean) {}
}



/** Class for internal IPC between [CentralConsoleService] and [ChildConsoleService] */
private sealed class ServiceEvent : Parcelable { // Parcelize is not generating CREATOR for sealed class, so we go manual
    class WebReady(val ready: Boolean) : ServiceEvent()
    class WebPushReady(val ready: Boolean) : ServiceEvent()
    class WebPush(val data: Bundle) : ServiceEvent()
    class StateUpdate(val state: ConsoleServiceState) : ServiceEvent()
    class PossibleCrash(val message: String = "") : ServiceEvent()
    class BearerReady(val ready: Boolean) : ServiceEvent()

    fun dispatch(service: IConsoleService) {
        when (this) {
            is WebReady -> service.onWebReady(ready)
            is WebPushReady -> service.onWebPushReady(ready)
            is WebPush -> service.onWebPushData(data)
            is BearerReady -> service.onBearerReady(ready)
            else -> {} // for central, not for IConsoleService
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        when (this) {
            is WebReady       -> { parcel.writeInt(0); parcel.writeInt(if (ready) 1 else 0) }
            is WebPushReady   -> { parcel.writeInt(1); parcel.writeInt(if (ready) 1 else 0) }
            is WebPush        -> { parcel.writeInt(2); parcel.writeBundle(data) }
            is StateUpdate    -> { parcel.writeInt(3); parcel.writeString(state.name) } // more robust if we separate apk
            //is StateUpdate    -> { parcel.writeInt(3); parcel.writeInt(state.ordinal) } // more efficient if built as one apk
            is PossibleCrash  -> { parcel.writeInt(4); parcel.writeString(message) }
            is BearerReady    -> { parcel.writeInt(5); parcel.writeInt(if (ready) 1 else 0) }
        }
    }
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ServiceEvent> = object : Parcelable.Creator<ServiceEvent> {
            override fun createFromParcel(parcel: Parcel): ServiceEvent = when (parcel.readInt()) {
                0 -> WebReady(parcel.readInt() != 0)
                1 -> WebPushReady(parcel.readInt() != 0)
                2 -> WebPush(parcel.readBundle(ServiceEvent::class.java.classLoader) ?: Bundle())
                3 -> StateUpdate(ConsoleServiceState.fromString(parcel.readString()) ?: ConsoleServiceState.UNKNOWN) // more robust if we separate apk
                //3 -> StateUpdate(ConsoleServiceState.fromOrdinal(parcel.readInt()) ?: ConsoleServiceState.UNKNOWN) // more efficient if built as one apk
                4 -> PossibleCrash(parcel.readString() ?: "")
                5 -> BearerReady(parcel.readInt() != 0)
                else -> error("Unknown ServiceEvent tag")
            }
            override fun newArray(size: Int): Array<ServiceEvent?> = arrayOfNulls(size)
        }
    }
}


/**
 * ServiceEvent queue with fixed capacity, overwrites the oldest event
 * Other possibility is coroutine Channel, but its use case is slightly different.
 * */
private class ServiceEventBuffer(private val capacity: Int) {
    private val deque = ArrayDeque<ServiceEvent>(capacity)
    /** Adds event to buffer, replaces oldest if capacity is reached */
    fun add(item: ServiceEvent) {
        // should we considered deduplicating event types? or refreshing existing event types? or are there enough to matter?
        synchronized(deque) {
            if (deque.size == capacity) {
                deque.removeFirst()
            }
            deque.addLast(item)
        }
    }
    /** removes all events and returns them as list */
    fun removeAll(): List<ServiceEvent> {
        synchronized(deque) {
            val list = deque.toList()
            deque.clear()
            return list
        }
    }
}


// keys for finding/passing central service names to child services for binding
internal const val INTENT_KEY_CENTRAL_PACKAGE_NAME = "com.jht.services.centralPackageName"
internal const val INTENT_KEY_CENTRAL_CLASS_NAME = "com.jht.services.centralClassName"
// could use as meta-data tag in specific service tag in manifest and then search for it? but storing the name direct might be faster
internal const val SERVICE_MANIFEST_IS_CENTRAL = "com.jht.services.isCentralService"
/** searches for first service level entry in android manifest that has the meta-data marker for being the central service */
internal fun findCentralServiceViaMetaData(context: Context): ComponentName? {
    val packManager = context.packageManager
    val packageInfo = packManager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES or PackageManager.GET_META_DATA)
    val serviceInfo = packageInfo.services?.find { it?.metaData?.getBoolean(SERVICE_MANIFEST_IS_CENTRAL, false) == true }
    return serviceInfo?.let { ComponentName(it.packageName, it.name) }
}
/** extracts application level meta-data for central service names if it exists, returns null if both are not present */
internal fun readCentralServiceFromAppMetaData(context: Context): ComponentName? {
    val ourApkInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    val meta: Bundle? = ourApkInfo.metaData // this Bundle can actually be null
    val packageName = meta?.getString(INTENT_KEY_CENTRAL_PACKAGE_NAME)
    val className = meta?.getString(INTENT_KEY_CENTRAL_CLASS_NAME)
    return if (packageName?.isNotEmpty() == true && className?.isNotEmpty() == true) ComponentName(packageName, className) else null
}

/**
 * Extension of full standard android [Service] that takes care of [IConsoleService] event passing.
 * This can be used as a base in place of the standard [Service] base to automatically get connectivity to central service.
 * */
abstract class ChildConsoleService : LifecycleService(), IConsoleService {
    companion object {
        private const val TAG = "ChildConsoleService"
    }
    /** Using full service name as ID since service names in manifest cannot be obscured (and its easier) */
    override val id: String = this::class.java.canonicalName ?: ""
    private var myState = ConsoleServiceState.UNKNOWN
    private val pendingEvents = ServiceEventBuffer(10)
    override fun start() {
        // if implementer wants to follow a start/stop pattern like the inline services
    }
    override fun stop() {
        // if implementer wants to follow a start/stop pattern like the inline services
    }
    override fun onWebReady(ready: Boolean) {
        // let implementer override this if needed
    }
    override fun onWebPushReady(ready: Boolean) {
        // let implementer override this if needed
    }
    override fun onWebPushData(data: Bundle) {
        // let implementer override this if needed
    }
    override fun onBearerReady(ready: Boolean) {
        // let implementer override this if needed
    }
    protected open fun onPossibleCrash() {
        // let implementer override this if needed
    }
    /** sends state update to central service */
    protected fun updateMyServiceState(state: ConsoleServiceState) {
        myState = state
        sendEventToCentral(ServiceEvent.StateUpdate(myState))
    }
    // child service binds to an AIDL central service to both send/receive service events
    private var hub: IServiceEventHub? = null
    private var bindRequested = false // prevent multiple binds in progress
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            // NOTE: these connection events marshall to the main thread, but binder operates on background threads
            Log.d(TAG, "onServiceConnected $id $name")
            hub = IServiceEventHub.Stub.asInterface(binder)
            bindRequested = true // keep true since requested, hub being set indicates we are connected
            hub?.registerCallback(id, callback)
            // replay queued events
            sendEventsToCentralFromQueue()
            lifecycleScope.launch(Dispatchers.IO + CoroutineName("ChildStart")) { start() }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected $id $name")
            hub = null
            bindRequested = false
        }
    }
    private val callback = object : IServiceEventCallback.Stub() {
        override fun onEvent(event: ServiceEvent) {
            // any need to track last received event and filter any events that were timestamp before it?
            event.dispatch(this@ChildConsoleService)
        }
    }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        // Get central names via manifest meta-data instead of relying on intents (can operate earlier and on crash restarts)
        // We can either get the exact name in application level meta-data OR use a meta-data marker in the service block to find its ServiceInfo inside our PackageInfo.
        var component = readCentralServiceFromAppMetaData(applicationContext)
        if (component != null) {
            bindToCentral(component)
        } else {
            // if top level meta data is missing, try searching for the service with the meta-data tag
            Log.d(TAG, "Could not find application level meta-data, searching for service level meta-data")
            component = findCentralServiceViaMetaData(applicationContext)
            if (component != null) {
                bindToCentral(component)
            } else {
                Log.e(TAG, "Cannot bind without central names from manifest $id")
                // should we throw an error to force developer to address issue rather than see the console hangs and wonder why?
                // otherwise the onStart will bind and failure will be quiet, leaving an issue if service crashes without knowing where to rebind
                throw RuntimeException("FATAL DEV ERROR: Cannot bind child services to central service without manifest meta-data")
            }
        }
    }
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand $id ${intent?.extras}")
        var foundInfo = false
        // check if central names were in the intent (central adds them when it starts the service)
        intent?.let {
            val packageName = it.getStringExtra(INTENT_KEY_CENTRAL_PACKAGE_NAME)
            val className = it.getStringExtra(INTENT_KEY_CENTRAL_CLASS_NAME)
            if (packageName?.isNotEmpty() == true && className?.isNotEmpty() == true) {
                foundInfo = true
                // re-check if we need binding in case central crashed
                val component = ComponentName(packageName, className)
                bindToCentral(component)
            }
        }
        // if no intent was passed, we did not start this service, likely android restarted it
        if (!foundInfo) {
            onPossibleCrash()
            sendEventToCentral(ServiceEvent.PossibleCrash("onStartCommand without expected intent extras"))
        }
        // if we used START_REDELIVER_INTENT, then intent would be replayed; if START_STICKY theoretically our extras won't be replayed
        return START_STICKY
    }
    @CallSuper
    override fun onDestroy() {
        stop()
        runCatching { hub?.unregisterCallback(id, callback) }
        runCatching { unbindService(connection) }
        super.onDestroy()
    }
    /** will attempt to bind back to central, provided there is no active connection or active request */
    private fun bindToCentral(component: ComponentName) {
        if (hub != null || bindRequested) return // only bind once
        bindRequested = true
        val intent = Intent().apply { this.component = component }
        bindService(intent, connection, BIND_AUTO_CREATE)
    }
    /** attempts to send event, returns success */
    private fun internalSendEventToCentral(event: ServiceEvent): Boolean {
        try {
            hub?.let {
                it.sendToCentral(id, event)
                return true
            }
        } catch (_: RemoteException) {
            // wait for central to issue new start to trigger rebind
        }
        return false
    }
    /** attempts to send event or queues for sending later, returns success */
    private fun sendEventToCentral(event: ServiceEvent): Boolean {
        val success = internalSendEventToCentral(event)
        if (!success) pendingEvents.add(event)
        return success
    }
    /** grabs copy of current pending queue, clears it, re-attempts to send events (always include a state update */
    private fun sendEventsToCentralFromQueue() {
        // if we are sending because of a crash, need to make sure state is always sent
        var sentState = false
        pendingEvents.removeAll().forEach {
            if (it is ServiceEvent.StateUpdate) sentState = true
            internalSendEventToCentral(it)
        }
        if (!sentState) internalSendEventToCentral(ServiceEvent.StateUpdate(myState))
    }
} // END CHILD


/**
 * Basis for a central service that can manage multiple [InlineConsoleService] while also supporting full android services implementing [ChildConsoleService]
 * There is only meant to be one instance of a central service per application.
 *
 * Couple of ideas were considered before going with AIDL. Though ordered sure delivery and ability to track crashes vary.
 * 1. just use startService to pass intents to children and a targeted intent broadcast for child state updates back
 * 2. try to inject some IPC mechanism from app like DataMatrix
 * 3. use Messenger with binding
 * 4. ultimately just using AIDL on the central service with calls to send event and subscribe to event
 * 5. "Fast Message Queue with AIDL" is interesting but Android 12+
 * */
abstract class CentralConsoleService() : LifecycleService() {
    companion object {
        private const val TAG = "CentralConsoleService"
    }
    /** stores active events in order to replay them for child services that may not have been ready for them */
    private val activeEvents = LinkedHashMap<KClass<out ServiceEvent>, ServiceEvent>() // LinkedHashMap - replay order is insertion order
    private val activeEventsLock = Object()
    /** sets an active event for later replay and sends event on to current children */
    private fun setActiveEvent(event: ServiceEvent) {
        val targets: List<Pair<String, IServiceEventCallback>>
        synchronized(activeEventsLock) {
            activeEvents[event::class] = event
            targets = callbacksById.entries.map { it.key to it.value }
        }
        targets.forEach { (id, cb) ->
            if (!sendEventToCallback(cb, event)) onChildCommunicationLoss(id)
        }
    }
    /** removes an active event from our replay list */
    private fun clearActiveEvent(event: KClass<out ServiceEvent>) {
        synchronized(activeEventsLock) {
            activeEvents.remove(event)
        }
    }
    /** if the managed lightweight [InlineConsoleService] needs event replay, those rely on direct event calls (this skips wrappers cause they update on register) */
    protected fun replayActiveEventsForConsoleService(service: IConsoleService) {
        // this mechanism is only applicable to the lightweight services that are called directly and not via AIDL
        if (service !is InlineConsoleService) return
        val replay: List<ServiceEvent>
        synchronized(activeEventsLock) {
            replay = activeEvents.values.toList()
        }
        // Replay active events missed while binding
        replay.forEach { event->
            event.dispatch(service)
        }
    }

    /** stores event callbacks from children, uses the child ID as cookie to track association, RemoteCallbackList manages dead connections */
    private val callbacks = object : RemoteCallbackList<IServiceEventCallback>() {
        // universal catch for any callback death
        override fun onCallbackDied(callback: IServiceEventCallback, cookie: Any?) {
            (cookie as? String)?.let { id ->
                callbacksById.remove(id, callback)
                onChildCommunicationLoss(id)
            }
        }
    }
    /** stores event callbacks from children by child ID, without access limitations of RemoteCallbackList */
    private val callbacksById = ConcurrentHashMap<String, IServiceEventCallback>()
    private val binder = object : IServiceEventHub.Stub() {
        /** process events sent from children */
        override fun sendToCentral(id: String, e: ServiceEvent) {
            Log.d(TAG, "sendToCentral $id $e")
            when (e) {
                is ServiceEvent.PossibleCrash -> onChildPossibleCrash(id, e.message)
                is ServiceEvent.StateUpdate -> onChildStateChange(id, e.state)
                else -> { /* not expected from children */ }
            }
        }
        /** children registering for events */
        override fun registerCallback(id: String, cb: IServiceEventCallback) {
            Log.d(TAG, "registerCallback $id $cb")
            val replay: List<ServiceEvent>
            synchronized(activeEventsLock) {
                callbacks.register(cb, id)
                callbacksById[id] = cb
                replay = activeEvents.values.toList()
            }
            // Replay active events missed while binding
            replay.forEach { sendEventToCallback(cb, it) }
            onChildRegister(id)
        }
        /** children unregistering for events */
        override fun unregisterCallback(id: String, cb: IServiceEventCallback) {
            Log.d(TAG, "unregisterCallback $id $cb")
            callbacks.unregister(cb)
            callbacksById.remove(id, cb)
            onChildUnregister(id)
        }
    }
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    /** central sending event to children */
    private fun sendEventToChildren(e: ServiceEvent) {
        Log.d(TAG, "sendEventToChildren $e")
        callbacksById.forEach { (id, callback) ->
            // onCallbackDied should technically handle onChildCommunicationLoss, but this might provide an earlier warning
            if (!sendEventToCallback(callback, e)) onChildCommunicationLoss(id)
        }
    }
    /** send an event to a specific child's callback and return success */
    private fun sendEventToCallback(cb: IServiceEventCallback, e: ServiceEvent): Boolean {
        try {
            cb.onEvent(e)
            return true
        }
        catch (_ : RemoteException) {} // RemoteCallbackList handles any removals
        return false
    }

    /** this is called when a child service reports a state change */
    abstract fun onChildStateChange(id: String, state: ConsoleServiceState)
    /** this is called when a child service registers */
    abstract fun onChildRegister(id: String)
    /** this is called when a child service unregisters */
    abstract fun onChildUnregister(id: String)
    /** this is called when a send to child service fails */
    abstract fun onChildCommunicationLoss(id: String)
    /** this is called when a send to child service restarts unexpectedly */
    abstract fun onChildPossibleCrash(id: String, message: String)
    /** this is sends web ready event to child services */
    @CallSuper
    protected open fun sendWebReady(ready: Boolean) = setActiveEvent(ServiceEvent.WebReady(ready))
    /** this is sends web push ready event to child services */
    @CallSuper
    protected open fun sendWebPushReady(ready: Boolean) = setActiveEvent(ServiceEvent.WebPushReady(ready))
    /** this sends bearer ready (fleet provisioning complete) event to child services */
    @CallSuper
    protected open fun sendBearerReady(ready: Boolean) = setActiveEvent(ServiceEvent.BearerReady(ready))
    /** this is sends web push event to child services */
    @CallSuper
    protected open fun sendWebPush(data: Bundle) = sendEventToChildren(ServiceEvent.WebPush(data))


    /** inner class for wrapping full child android services */
    protected inner class ConsoleServiceWrapper(
        private val serviceClass: Class<*>,
    ) : IConsoleService {
        /** Constructor to look up service class by canonical name (note this can throw if cannot find it) */
        constructor(serviceClassName: String) : this(Class.forName(serviceClassName))
        override val id: String = serviceClass.canonicalName ?: "" // for wrapped full service, use full name as ID - service names are safe from proguard
        override fun start() {
            val context = this@CentralConsoleService.applicationContext
            val intent = Intent(context, serviceClass).apply {
                putExtra(INTENT_KEY_CENTRAL_PACKAGE_NAME, this@CentralConsoleService.packageName)
                putExtra(INTENT_KEY_CENTRAL_CLASS_NAME, this@CentralConsoleService.javaClass.canonicalName)
            }
            context.startService(intent)
        }
        override fun stop() {
            val context = this@CentralConsoleService.applicationContext
            context.stopService(Intent(context, serviceClass))
        }
        override fun onWebReady(ready: Boolean) {
            // use send functions to notify the full services to avoid duplicate events
        }
        override fun onWebPushReady(ready: Boolean) {
            // use send functions to notify the full services to avoid duplicate events
        }
        override fun onWebPushData(data: Bundle) {
            // use send functions to notify the full services to avoid duplicate events
        }
        override fun onBearerReady(ready: Boolean) {
            // use send functions to notify the full services to avoid duplicate events
        }
    }
} // END CENTRAL
