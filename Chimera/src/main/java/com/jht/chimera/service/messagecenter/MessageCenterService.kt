package com.jht.chimera.service.messagecenter

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.chimera.service.ApiFacilityMessage
import com.jht.chimera.service.ApiFacilityMessageType
import com.jht.chimera.service.FacilityServiceApiClient
import com.jht.datamatrixlib.DataMatrix
import com.jht.services.InlineConsoleService
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.time.Duration.Companion.seconds

private const val TAG = "MessageCenterService"
private const val DEBUG = false
private const val USE_PERIODIC_POLLING = true
private val PERIODIC_TIMER_DELAY = 20.seconds
private val PERIODIC_TIMER_RATE = 60.seconds

class MessageCenterService(context: Context): InlineConsoleService(context) {
    private val mDataMatrix = DataMatrix()
    private val syncLock = Object() // some lists are shared between threads
    private val messageCenterPreWorkoutList = mutableListOf<ApiFacilityMessage>()
    private val messageCenterPostWorkoutList = mutableListOf<ApiFacilityMessage>()
    private val messageCenterRecurringList = mutableListOf<ApiFacilityMessage>()
    private val messageCenterScheduledMap = mutableMapOf<Int, TimerTask>() // scheduled show task for message ID
    private val scheduleTimer = java.util.Timer() // for the scheduled timer tasks
    private var needsToWaitForValidMachineId = false
    private val scopeIo = CoroutineScope(Dispatchers.IO)
    private var pollJob: Job? = null
    private val facilityServiceApi = FacilityServiceApiClient()
    private var previousRunState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT)

    companion object {
        const val ID = TAG // UUID or unique friendly name for this service
    }
    override val id: String = ID // for service as a whole

    override fun start() {
        Log.d("ServiceStarter", "Starting $this")
        init()
        Log.d("ServiceStarter", "Ending $this")
    }

    override fun stop() {
        mDataMatrix.finish()
        stopRequestForMessages()
        scheduleTimer.cancel()
        scopeIo.cancel()
    }

    private fun init() {
        if (DEBUG) Log.d(TAG, "start of init")

        mDataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, { v: Int? ->
            if (v == UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY) {
                if (DEBUG) Log.d(TAG, "Workout Summary displayed: " + messageCenterPostWorkoutList.size)
                synchronized(syncLock) {
                    for (webMessage in messageCenterPostWorkoutList) {
                        if (webMessage.isAllowed()) {
                            showMessage(webMessage)
                            if (DEBUG) Log.d(TAG, "Post Workout Message: " + webMessage.message)
                        }
                    }
                }
            }
        }, false)

        // listen to run state changes to display the relevant startup messages.
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { runState: Int ->
            if (DEBUG) Log.i(TAG, "DATA_RUN_STATE_INT transitioned to $runState (RUNNING=${WorkoutKeys.Constants.RUN_STATE_RUNNING})")
            // Gating the trigger using the previous run state ensures we do not display messages again on pause/resume transitions.
            if (runState == WorkoutKeys.Constants.RUN_STATE_RUNNING && previousRunState == WorkoutKeys.Constants.RUN_STATE_STOP) {
                if (DEBUG) Log.d(TAG, "Workout entered running state: " + messageCenterPreWorkoutList.size)
                synchronized(syncLock) {
                    for (webMessage in messageCenterPreWorkoutList) {
                        if (webMessage.isAllowed()) {
                            showMessage(webMessage)
                            if (DEBUG) Log.d(TAG, "Pre Workout Message: " + webMessage.message)
                        }
                    }
                    for (webMessage in messageCenterRecurringList) {
                        if (webMessage.isAllowed()) {
                            val message = toMessageCenterMessage(webMessage)
                            showMessage(message)
                            if (DEBUG) Log.d(TAG, "Recurring Message: " + message.messageText)
                        }
                    }
                }
            }
            // Cache the new state for comparison in subsequent transitions.
                previousRunState = runState
        }, false)

        // if polling, need to kick off periodic when machine is valid (otherwise rely on push callbacks)
        if (USE_PERIODIC_POLLING) mDataMatrix.createBoolAdapter(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, { isValid: Boolean? ->
            if (isValid == true) {
                if (needsToWaitForValidMachineId) {
                    needsToWaitForValidMachineId = false
                    startRequestForMessages()
                }
            } else {
                needsToWaitForValidMachineId = true
                stopRequestForMessages()
            }
        }, true)
    }

    /** Attempts to get message list from API and update our local copies */
    private suspend fun updateMessagesTask() {
        if (DEBUG) Log.i(TAG, "Starting API fetch via facilityServiceApi.apiGetMessage()...")
        val rawData = facilityServiceApi.apiGetMessage()
        if (DEBUG) Log.i(TAG, "API response: $rawData")
        if (rawData == null) {
            if (DEBUG) Log.i(TAG, "Null response detected. Falling back to emptyList() to allow cache clearing downstream.")
        }
        val webData = rawData ?: emptyList() //use emptyList as a fallback instead of returning to ensure the rest of the function continue to execute.
        if (DEBUG) Log.d(TAG, "webData> $webData")
        val pendingPreWorkoutMessages = mutableListOf<ApiFacilityMessage>()
        val pendingPostWorkoutMessages = mutableListOf<ApiFacilityMessage>()
        val pendingRecurringMessages = mutableListOf<ApiFacilityMessage>()
        val pendingSingleMessages = mutableListOf<ApiFacilityMessage>()
        webData.forEach { webMessage ->
            when (webMessage.messageType) {
                ApiFacilityMessageType.WORKOUT_START -> {
                    pendingPreWorkoutMessages.add(webMessage)
                }
                ApiFacilityMessageType.WORKOUT_END -> {
                    pendingPostWorkoutMessages.add(webMessage)
                }
                ApiFacilityMessageType.SINGLE_MESSAGE -> {
                    pendingSingleMessages.add(webMessage)
                }
                ApiFacilityMessageType.RECURRING_MESSAGE -> {
                    pendingRecurringMessages.add(webMessage)
                }
                else -> {}
            }
        }
        // remove unneeded schedules
        val mapIterator = messageCenterScheduledMap.iterator()
        while (mapIterator.hasNext()) {
            val entry = mapIterator.next()
            if (pendingSingleMessages.find { it.id == entry.key } == null) {
                entry.value.cancel()
                mapIterator.remove()
            }
        }
        // add new schedules
        pendingSingleMessages.forEach { webMessage ->
            if (webMessage.startAt != null) {
                if (!messageCenterScheduledMap.contains(webMessage.id)) {
                    messageCenterScheduledMap[webMessage.id] = scheduleTimer.schedule(Date.from(webMessage.startAt)) {
                        // should duration be reduced by diff between now and the end time?
                        if (webMessage.isAllowed()) {
                            showMessage(webMessage)
                            if (DEBUG) Log.d(TAG, "Single Message: " + webMessage.message)
                        }
                        messageCenterScheduledMap.remove(webMessage.id)
                    }
                }
            }
        }
        synchronized(syncLock) {
            if (DEBUG) Log.i(TAG, "Updating message caches. Pre-workout count=${pendingPreWorkoutMessages.size}, Post-workout count=${pendingPostWorkoutMessages.size}, Recurring count=${pendingRecurringMessages.size}")
            // update lists in thread safe manner
            messageCenterPreWorkoutList.clear()
            messageCenterPreWorkoutList.addAll(pendingPreWorkoutMessages)
            messageCenterPostWorkoutList.clear()
            messageCenterPostWorkoutList.addAll(pendingPostWorkoutMessages)
            messageCenterRecurringList.clear()
            messageCenterRecurringList.addAll(pendingRecurringMessages)
        }
    }

    private fun startRequestForMessages(initialDelayMs: Long = 0) {
        if (DEBUG) Log.i(TAG, "startRequestForMessages called. Current pollJob: $pollJob (isActive=${pollJob?.isActive})")
        if (pollJob != null) {
            if (DEBUG) Log.w(TAG, "Request BLOCKED because pollJob is non-null!")
            return
        }
        pollJob = scopeIo.launch {
            if (DEBUG) Log.i(TAG, "Coroutine started. pollJob is now non-null.")
            try { //Wrap the execution block to guarantee the pollJob lock is cleared under any exit condition. ie network timeout preventing deadlock
                runCatchingCancellable {
                    if (initialDelayMs > 0) delay(initialDelayMs)
                    if (USE_PERIODIC_POLLING) {
                        delay(PERIODIC_TIMER_DELAY)
                        while (isActive) {
                            updateMessagesTask()
                            delay(PERIODIC_TIMER_RATE)
                        }
                    } else {
                        updateMessagesTask()
                    }
                }
            } finally {
                if (DEBUG)Log.i(TAG, "Finally block reached. Releasing lock by resetting pollJob to null.")
                pollJob = null
            }
        }
    }
    private fun stopRequestForMessages() {
        if (DEBUG) Log.i(TAG, "stopRequestForMessages called. Cancelling job: $pollJob")
        pollJob?.cancel()
        pollJob = null
    }

    /** Sends the message to the console's local message center handling UI service */
    private fun showMessage(webMessage: ApiFacilityMessage) = showMessage(toMessageCenterMessage(webMessage))
    private fun showMessage(message: MessageCenterMessage?) {
        message?.let {
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, it)
        }
    }

    override fun onWebReady(ready: Boolean) {
        if (!ready) return
        if (USE_PERIODIC_POLLING) return
        // this should handle initial start and internet recovery
        startRequestForMessages(initialDelayMs = 5_000)
    }
    override fun onWebPushData(data: Bundle) {
        if (USE_PERIODIC_POLLING) return
        val type = data.getString("type") ?: ""
        if (DEBUG) Log.d(TAG, "Any Push received: type='$type' data=$data")
        
        // there should be a type but no payload
        if (type == "fs_message") {
            // After receiving this notification, the console should call the Facility Service’s /console/facility/:id/message endpoint in order to refresh facility message data.
            if (DEBUG) Log.d(TAG, "Facility Push matched! Triggering update.")
            startRequestForMessages()
        }
    }

}

private fun toMessageCenterMessage(webMessage: ApiFacilityMessage): MessageCenterMessage {
    // at the moment the new endpoint does not send priority or colors for messages
    return MessageCenterMessage().apply {
        setId(webMessage.id) // JHTTypes.MessageIds contains internal IDs which are negative integers, web ID should be minimum of 1
        setMessageHeader(webMessage.title)
        setMessage(webMessage.message)
        setHideTime(webMessage.durationSeconds)
        webMessage.startAt?.let { setTimestamp(it) }
        webMessage.endAt?.let { setExpiration(it) }
        webMessage.callToActionURL?.let { setActionUrl(it) }
        webMessage.callToActionText?.let { setActionText(it) }
    }
}
