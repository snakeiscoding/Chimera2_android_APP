package com.jht.concierge.service

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import com.jht.androidcommonalgorithms.timer.PeriodicTimer
import com.jht.concierge.R
import com.jht.concierge.interfaces.ConciergeModel
import com.jht.concierge.interfaces.FacilityConfigResponse
import com.jht.concierge.interfaces.TimeOpenAndCloseModel
import com.jht.concierge.interfaces.getConciergeInjectionEntryPoint
import com.jht.enums.FrameType
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.services.InlineConsoleService
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val OPTIONS_SCHEDULE_TIME_MILLISECONDS = 86_400_000L // 24 hours
private const val TAG = "ConciergeService"

class ConciergeConsoleService(context: Context): InlineConsoleService(context) {
    companion object {
        const val ID = TAG // UUID or unique friendly name or just use the log tag?
    }
    override val id: String = ID // do we need instance to be able to return its ID?

    private val systemModel: SystemModel
    private val uiBaseModel: UIBaseModel
    private val workoutModel: WorkoutModel
    private val conciergeModel: ConciergeModel = getConciergeInjectionEntryPoint(context).concierge()

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val mutex: Mutex = Mutex()
    private val pollForUpdates: PeriodicTimer = PeriodicTimer(5000, 5000) {runBlocking{mutex.withLock{updateRequest()}}}
    private var isPollingStarted: Boolean = false
    private var api: ConciergeApi
    private var facilityId: Int
    private var frameSerial: String
    private var machineModality: FrameType
    private var machineUUID: String
    private var conciergeRequest: ConciergeRequest = ConciergeRequest()
    private var autoImportFacilityRequestOptionsTask: ScheduledFuture<*>? = null
    private val facilityRequestOptionsScheduler = Executors.newSingleThreadScheduledExecutor()
    private var lastFacilityOptionsTimeMs = 0L
    private val started = AtomicBoolean(false) // should we roll a standard-ish running state?

    init {
        val injection = getInjectionEntryPoint(context) // TODO - any nice annotations we could use? would class need to be a context wrapper?
        systemModel = injection.system()
        uiBaseModel = injection.uiBase()
        workoutModel = injection.workout()
        facilityId = systemModel.facilityID.value
        frameSerial = systemModel.serialNumberFrame
        machineUUID = systemModel.machineUUID.value
        machineModality = uiBaseModel.frameType
        api = ConciergeApi(systemModel.dapiEnvironment.value, context.applicationContext)
    }

    override fun start() {
        if (started.getAndSet(true)) {
            Log.d(TAG, "Already started, ignoring start call")
            return
        }
        Log.d(TAG, "Service Started")
        setupListeners()
        // get facility config response to fetch facility request options to show to user on service start, with 1 minute delay so we have internet, runs every 24 hours [TODO: Might need to refactor this logic?]
        autoImportFacilityRequestOptionsTask = facilityRequestOptionsScheduler.scheduleWithFixedDelay({
            if (lastFacilityOptionsTimeMs == 0L || (SystemClock.elapsedRealtime() - lastFacilityOptionsTimeMs) > OPTIONS_SCHEDULE_TIME_MILLISECONDS) {
                Log.d(TAG, "Concierge scheduler running . . .")
                if (workoutModel.isWorkoutStopped.value) {
                    runBlocking {
                        mutex.withLock {
                            checkScheduledAvailability(getFacilityConfigRequest())
                        }
                    }
                }
            }
        }, 10, 3600, TimeUnit.SECONDS) // service may start before splash, so do not rely on catching splash event for initial read (do we need any protection against double read on first boot?)
    }

    override fun stop() {
        serviceScope.cancel() // once this is called, we cannot resume operations
        systemModel.close()
        uiBaseModel.close()
        workoutModel.close()
        conciergeModel.close()
        stopPollingForUpdates()
        autoImportFacilityRequestOptionsTask?.cancel(true)
        Log.d(TAG, "Service Stopped")
        started.set(false)
    }

    /**
     * Check if concierge service is available based on the schedule key response. If schedule is empty then concierge service is available uninterrupted.
     * Depending on the schedule received in response body, this function checks and further toggles the service if it's falling in between the current timeframe and week's day.
     * Side effects: Updates the global availability state
     * @return true if determined services are available, false otherwise
     */
    private fun checkScheduledAvailability(): Boolean {
        val responseString = conciergeModel.facilityConfiguration
        if (responseString.isNotEmpty()) {
            try {
                val conciergeSchedule = Json.decodeFromString<FacilityConfigResponse>(responseString)
                return checkScheduledAvailability(conciergeSchedule)
            } catch (exception: JSONException) {
                Log.w(TAG, "Unable to parse response body due to ${exception.message}")
            }
        }
        updateConciergeAvailability(false)
        return false
    }
    private fun checkScheduledAvailability(conciergeSchedule: FacilityConfigResponse?): Boolean {
        var isAvailable = false

        // if not active, disregard schedule
        if (conciergeSchedule?.active == false) {
            updateConciergeAvailability(false)
            return false
        }

        // if no schedule, always available
        if (conciergeSchedule?.availabilitySchedule == null || conciergeSchedule.availabilitySchedule?.isEmpty() == true) {
            updateConciergeAvailability(true)
            return true
        }

        // In case if schedule field is null or empty, this code won't do any changes to existing availability of the service.
        conciergeSchedule.availabilitySchedule?.let { schedule ->
            if (schedule.isNotEmpty()) {
                // Get current instance of day, time [HH:MM]
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                for ((k, v) in schedule) {
                    // Check if key's day is same to today's day
                    if (k.calendarDoW == dayOfWeek) {
                        isAvailable = isConciergeAvailable(
                            schedule = v,
                            currentHour = hourOfDay,
                            currentMinute = minute
                        )
                        if (isAvailable) break
                    }
                }
            }
        }
        updateConciergeAvailability(isAvailable)
        return isAvailable
    }

    /**
     * This method takes schedule, currentHour & currentMinute and based on the available schedule, it toggles Concierge button
     */
    private fun isConciergeAvailable(schedule: List<TimeOpenAndCloseModel>, currentHour: Int, currentMinute: Int): Boolean {
        var isAvailable = false
        for (time in schedule) {
            try {
                // Validate if both the time provided in schedule is accurate or malformed
                if (time.open.contains(":") && time.open.length in 4..5 && time.close.contains(":") && time.close.length in 4..5) {
                    val openTime = time.open.split(":")
                    val closeTime = time.close.split(":")
                    val startHour = openTime[0].toInt()
                    val startMinute = openTime[1].toInt()
                    val endHour = closeTime[0].toInt()
                    val endMinute = closeTime[1].toInt()

                    // Check if currentHour and currentMinute is falling in given range
                    if (currentHour in startHour..endHour) {
                        if (currentHour == startHour) {
                            isAvailable = currentMinute >= startMinute
                            break
                        }
                        if (currentHour == endHour) {
                            isAvailable = currentMinute <= endMinute
                            break
                        }
                        isAvailable = true
                        break
                    }
                }
            } catch(exception: Exception) {
                Log.d(TAG, "time parse error: Unable to parse time from provided string, due to ${exception.message}")
            }
        }

        return isAvailable
    }

    /**
     * Call this  method to update visibility of Concierge button
     */
    private fun updateConciergeAvailability(enable: Boolean) {
        conciergeModel.isConciergeAvailable.value = enable
    }

    /**
     * This method starts the polling process which checks the server for concierge updates
     */
    private fun startPollingForUpdates() {
        if (!isPollingStarted) {
            isPollingStarted = true
            pollForUpdates.start()
            Log.d(TAG, "Polling started")
        }
    }

    /**
     * This method stops the polling process which checks the server for concierge updates
     */
    private fun stopPollingForUpdates() {
        if (isPollingStarted) {
            pollForUpdates.stop()
            isPollingStarted = false
            Log.d(TAG, "Polling stopped")
        }
    }

    /**
     * @deprecated
     * This method pings the concierge endpoint to return a list of Facility Request Options to
     * display to the user on the concierge dialog
     */
    @Deprecated(message = "use getFacilityConfigRequest()") // Kept it as deprecated for now, will remove this block of code post confirmation
    private suspend fun getFacilityRequestOptions() {
        Log.d(TAG, "Getting concierge request options...")
        val result = attempt(3) {
            api.getFacilityRequestType(
                totp = api.authenticate(),
                facilityId = facilityId.toString(),
            )
        }
        if (result is Result.Success) {
            Log.d(TAG, "New facility request options: ${result.value}")
            updateLedColorSetting(result.value.firstOrNull()?.ledColor)
            conciergeModel.facilityConfiguration = Json.encodeToString(result.value)
        } else {
            Log.d(TAG, "No facility request options found")
            // clear any data from differing clubs/environments
            conciergeModel.facilityConfiguration = ""
        }
    }

    /**
     * This method returns facility configuration which includes the status of the concierge service alongside it's scheduled availability,
     * it also return a list of Facility Request Options which shows available options on the concierge dialog.
     */
    private suspend fun getFacilityConfigRequest(): FacilityConfigResponse? {
        Log.d(TAG, "Getting facility configuration for concierge...")
        val result = attempt(3) {
            api.getFacilityConfigRequest(
                totp = api.authenticate(),
                facilityId = facilityId.toString(),
            )
        }
        if (result is Result.Success) {
            Log.d(TAG, "Facility configuration response: ${result.value}")
            // Update LED color if needed
            updateLedColorSetting(result.value.color)
            // Store facility configuration as a string
            lastFacilityOptionsTimeMs = SystemClock.elapsedRealtime()
            conciergeModel.facilityConfiguration = Json.encodeToString(result.value)
            return result.value
        } else {
            Log.d(TAG, "No facility configuration found")
            conciergeModel.facilityConfiguration = ""
            return null
        }
    }

    /** if a new LED color is given, attempt to parse it, update setting if different than current setting */
    private fun updateLedColorSetting(colorHexString: String?) {
        colorHexString?.takeIf { it.isNotEmpty() }?.let {
            try {
                val androidColorHex = it.replace("0x", "#")
                val value = Color.parseColor(androidColorHex)
                Log.d(TAG, "Updating LED color $androidColorHex")
                conciergeModel.ledColor = value
            } catch (e: Exception) { // make sure this doesn't crash
                Log.e(TAG, "updateLedColorSetting exception", e)
            }
        }
    }

    /**
     * This method creates a new concierge request with the server (if one is not already in progress)
     */
    private suspend fun createRequest(requestId: String) {
        if (requestInProgress()) return
        // TODO - Does UI need to call a preflight check to do this before user makes request? Maybe we should have a not-available event for UI
        // TODO - does not seem efficient to have to do this transaction every time, probably need to re-examine later
        if (!checkScheduledAvailability(getFacilityConfigRequest())) {
            Log.d(TAG, "createRequest called, but service is not available")
            // this is cheesy but gets the job done for now
            //conciergeModel.conciergeEvent = ConciergeEvent.Error(ConciergeEvent.RequestClosed)
            conciergeModel.conciergeEvent = ConciergeEvent.RequestCancelled
            conciergeModel.conciergeEvent = ConciergeEvent.RequestClosed
            return
        }
        Log.d(TAG, "Creating concierge request...")
        val previousState = conciergeModel.conciergeEvent ?: ConciergeEvent.RequestClosed
        conciergeModel.conciergeEvent = ConciergeEvent.Loading(context.getString(R.string.requesting_help_tc))
        val result = attempt(3) {
            api.createConciergeRequest(
                totp = api.authenticate(),
                facilityId = facilityId,
                frameSerial = frameSerial,
                machineModality = machineModality.name,
                machineUID = machineUUID,
                facilityRequestTypeId = requestId
            )
        }
        val event = if (result is Result.Success) {
            conciergeRequest = result.value
            startPollingForUpdates()
            Log.d(TAG, "New concierge request: ${result.value}")
            ConciergeEvent.RequestOpened
        } else {
            ConciergeEvent.Error(previousState = previousState)
        }
        conciergeModel.conciergeEvent = event
    }

    /**
     * This method updates the current concierge request (if one is in progress)
     */
    private suspend fun updateRequest() {
        if (!requestInProgress() || !isPollingStarted) return
        Log.d(TAG, "Checking for concierge request update...")
        val result = attempt(1){
            api.getConciergeRequest(totp = api.authenticate(), requestId = conciergeRequest.id)
        }
        if (result is Result.Success && result.value != conciergeRequest) {
            conciergeRequest = result.value
            Log.d(TAG, "Concierge update found: ${result.value}")
            if (requestInProgress()) startPollingForUpdates() else stopPollingForUpdates()
            val event = when (result.value.state) {
                ConciergeRequestState.CLOSED -> ConciergeEvent.RequestCompleted
                ConciergeRequestState.OPEN -> ConciergeEvent.RequestOpened
                ConciergeRequestState.ACKNOWLEDGED -> ConciergeEvent.RequestAcknowledged
            }
            conciergeModel.conciergeEvent = event
        }
    }

    /**
     * This method closes the current concierge request (if one is in progress)
     */
    private suspend fun closeRequest() {
        if (!requestInProgress()) return
        Log.d(TAG, "Closing concierge request...")
        val result = attempt(3) {
            api.closeConciergeRequest(
                totp = api.authenticate(),
                requestId = conciergeRequest.id,
                facilityId = conciergeRequest.facilityId,
                machineUID = conciergeRequest.machineUid
            )
        }
        stopPollingForUpdates()
        conciergeRequest = ConciergeRequest()
        if (result is Result.Success) Log.d(TAG, "Closed concierge request successfully")
        else Log.d(TAG, "Network error while closing concierge request")
    }

    /**
     * Initializes all the event listeners necessary for this service
     */
    private fun setupListeners() {

        systemModel.onSplashReset.listen {
            Log.d(TAG, "Splash event action")
            serviceScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    //closeRequest() // do not close at splash
//                        getFacilityRequestOptions() // get new facility options on environment change
                    checkScheduledAvailability(getFacilityConfigRequest())
                }
            }
        }


        // Cancel request in progress if club ID changes
        systemModel.facilityID.listen {
            serviceScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    closeRequest()
                    facilityId = it
//                        getFacilityRequestOptions() // get new facility options on club change
                    checkScheduledAvailability(getFacilityConfigRequest())
                }
            }
        }

        // Cancel request in progress if frame serial number changes
        systemModel.onFrameChange.listen {
            serviceScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    closeRequest()
                    frameSerial = systemModel.serialNumberFrame
                    machineModality = uiBaseModel.frameType
                }
            }
        }

        // Cancel request in progress if machine UUID changes
        systemModel.machineUUID.listen {
            serviceScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    closeRequest()
                    machineUUID = it
                }
            }
        }

        // Cancel request in progress if DAPI environment changes
        // Update concierge api to use new environment
        systemModel.dapiEnvironment.listen {
            serviceScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    closeRequest()
                    api = ConciergeApi(it, context.applicationContext)
//                        getFacilityRequestOptions() // get new facility options on environment change
                    checkScheduledAvailability(getFacilityConfigRequest())
                }
            }
        }

        // Listen for requests to create a new concierge request
        conciergeModel.onCreateRequest.listen {
            serviceScope.launch(Dispatchers.IO) { mutex.withLock { createRequest(it) } }
        }

        // Listen for requests to cancel a concierge request
        conciergeModel.onCancelRequest.listen {
            serviceScope.launch(Dispatchers.IO) {
            mutex.withLock { closeRequest() } }
        }
    }

    /**
     * Helper method to check if a concierge request is in progress
     * In progress means the status is either Open or Acknowledged
     */
    private fun requestInProgress(): Boolean {
        return conciergeRequest.state != ConciergeRequestState.CLOSED
    }

    /**
     * Utility method which attempts to execute a block. Function will only re-attempt execution of
     * the code block if an exception occurs.
     * @param maxAttempts the maximum attempts to execute the block of code.
     * @param function the block of code to attempt to execute.
     * @return Result<T> where T is the return type of function.
     */
    private suspend fun <T> attempt(maxAttempts: Int, function: suspend () -> T): Result<T> {
        var attempts = 0
        while (true) {
            try {
                return Result.Success(function.invoke())
            } catch (e: Exception) {
                attempts++
                Log.w(TAG, e.toString())
                if (attempts >= maxAttempts) return Result.Failure(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Returned from the utility attempt method. Can be of type Success or Failure which indicates whether the
     * attempted function ever completed successfully.
     */
    private sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val message: String) : Result<Nothing>()
    }
}