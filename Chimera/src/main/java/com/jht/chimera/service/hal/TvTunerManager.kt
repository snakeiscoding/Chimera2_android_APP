package com.jht.chimera.service.hal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import com.jht.androidcommonalgorithms.util.ThreadHelper
import com.jht.chimera.lib.common.SystemPropertyUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVHDMIOutputResolution
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.tv.TVControllerFactory
import com.jht.chimera.lib.tv.tuner.DetectedTunerInfo
import com.jht.chimera.lib.tv.tuner.IChimeraIPTuner
import com.jht.chimera.lib.tv.tuner.IChimeraTuner
import com.jht.chimera.lib.tv.tuner.SwiftTunerBase
import com.jht.chimera.lib.tv.tuner.SwiftTunerDetection
import com.jht.chimera.lib.tv.tuner.TunerFactory
import com.jht.chimera.lib.tv.tuner.TunerNotDetectedException
import com.jht.chimera.lib.tv.tuner.US4Tuner
import com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500TunerDetection
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EConnection
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETuner
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750ETunerDetection
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.IStartStop
import com.jht.serialport.peripheral.tuner.ITuner
import com.jht.serialport.peripheral.tuner.TunerProperties
import com.jht.serialport.peripheral.tuner.channel.IChannel
import com.jht.serialport.serialcommunication.TransactionManager
import com.jht.utility.unregisterReceiverSafe
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private const val TAG = "TvTunerManager"
private typealias TunerType = IChimeraTuner<out IChannel>

/** Provides central management of the TV Tuner. Only one instance should be created by a background service. */
class TvTunerManager(
    private val context: Context
) : IStartStop {
    private val mDataMatrix = DataMatrix()
    // TODO - would a coroutine replace these handler/thread items?
    private var tvThread: HandlerThread? = null
    private var tvHandler: Handler? = null
    // TODO - keep and use one tuner instance?
    //private var tvTuner: TunerType? = null
    //private var tvSource: TVSource? = null
    private var lg750EConnection: Lg750EConnection? = null

    private val updateTunerFirmware: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val tunerType = try {
                TunerProperties.TUNER_TYPE.valueOf(SystemPropertyUtil.get("persist.jht.tuner.detect"))
            } catch (e: Exception) {
                updateTunerFirmwareResult(100, true, "Invalid Tuner Type Provided")
                return
            }
            val tvSource = TVSource.getSource(tunerType)
            val tuner = TunerFactory.getTuner(context, tvSource)
            if (tuner is SwiftTunerBase<*>) {
                val firmwarePath = intent.getStringExtra("path")
                if (!firmwarePath.isNullOrEmpty()) {
                    val firmwarePathFile = File(firmwarePath)
                    if (firmwarePathFile.exists()) {
                        tuner.upgradeTuner(null, firmwarePathFile) { progress: Int, finished: Boolean, error: Any? ->
                            updateTunerFirmwareResult(progress, finished, error?.toString() ?: "")
                        }
                        return
                    }
                }
                updateTunerFirmwareResult(100, true, "Error finding tuner firmware")
            } else {
                updateTunerFirmwareResult(100, true, "Tuner does not support remote updates")
            }
        }
    }
    private fun updateTunerFirmwareResult(progress: Int, finished: Boolean, result: String) {
        val intent = Intent("com.jht.updatetuner.result")
        intent.putExtra("progress", progress)
        intent.putExtra("finished", finished)
        intent.putExtra("result", result)
        context.sendBroadcast(intent)
    }

    override fun start() {
        tvThread = HandlerThread("HALTVThread")
        tvThread?.let {
            it.start()
            tvHandler = Handler(it.looper)
        }
        context.registerReceiver(updateTunerFirmware, IntentFilter("com.jht.hal.update.tuner"))

        mDataMatrix.createEventAdapter(HALKeys.EVENT_MACHINE_SETUP_COMPLETE) {
            tvHandler?.post {
                autoDetectTVTuner().whenComplete { tunerInfo: DetectedTunerInfo, error: Throwable? ->
                    if (error == null) {
                        DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, tunerInfo.tunerSource.name)
                        DataMatrix.putString(TVKeys.DATA_TUNER_VERSION_STRING, tunerInfo.firmwareVersion)
                    } else {
                        Log.w(TAG, "Warning: " + error.message)
                    }
                }
            }
        }
        mDataMatrix.createStringAdapter(SettingKeys.TV.DATA_HDMI_OUTPUT_RESOLUTION_STRING) { resolutionName ->
            val selectedResolution = getSelectedTvHdmiResolution()
            if (selectedResolution.name == resolutionName) {
                tvHandler?.post { setTvSourceHDMIResolution(hdmiResolution = selectedResolution) }
            } else {
                DataMatrix.putString(SettingKeys.TV.DATA_HDMI_OUTPUT_RESOLUTION_STRING, selectedResolution.name)
            }
        }
        mDataMatrix.createEventAdapter(TVKeys.ACTION_GET_TUNER_NETWORK_DETAILS, {
            tvHandler?.post(this::getTunerNetworkDetails)
        }, false)
        mDataMatrix.createStringAdapter(SettingKeys.TV.DATA_TV_SOURCE_STRING, { source: String? ->
            DataMatrix.putString(TVKeys.EVENT_TUNER_IP_ADDRESS_STRING, "")
            DataMatrix.putString(TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING, "")
            tvHandler?.post(this::getTunerNetworkDetails)
        }, false)
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET) {
            tvHandler?.post { resetTV(false, clearSession = true) }
        }
        mDataMatrix.createEventAdapter(TVKeys.EVENT_KM4_ISC_START) { runKm4IscProcess() }

        tvHandler?.post(this::initializeTV)
        // only spin up websocket on certain consoles (or tuners)?
        if (ProductConfig.hasInternalEthernetNetwork()) try {
            // should we auto port and send that to tuner to avoid "already-in-use" errors?
            // should we be sure to bind only to internal network interface?
            lg750EConnection = Lg750EConnection(
                getConsoleType = { ProductConfig.getConsoleType().toString() },
            ).also { it.start() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start web socket server", e)
        }

        if (ProductConfig.getAvailableTuners().any { it.isLG750E }) {
            tvHandler?.post { startProcentricProxy() }
        }
    }

    override fun stop() {
        mDataMatrix.finish()
        context.unregisterReceiverSafe(updateTunerFirmware)
        tvThread?.quit()
        lg750EConnection?.stop()
        lg750EConnection = null
    }

    fun onRebooting() {
        // We could probably do this in "stop" and just call stop before rebooting the console?
        // Do we need to marshall this onto the tv thread? but need to make the caller wait for it?
        // old code> if (tvHandler != null) tvHandler.post(()-> rebootConsole(reason));
        rebootTV(true)
    }


    private var tvInitialized = false
    private fun initializeTV() {
        // Ensure TV source setting is valid for platform
        ensureTVSettingIsValidForPlatform(TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)))

        // Get the active TV source setting from MES
        val source = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name))

        // Check for old US4 tuner settings file and convert to new location if needed
        if (source == TVSource.US4) {
            US4Tuner.checkSettingsFile(context)
        }

        // Attempt to detect the actively installed tuner. Note that this runs asynchronously and
        // only blocks the TV thread when tuner communication is actively occurring. Timeouts between
        // tuner detection attempts are non-blocking.
        Log.d(TAG, "initializeTV: Starting auto-detection for source=$source")
        autoDetectTVTuner().also { future ->
            // If detection hasn't succeeded after 90s, attempt setting resolution anyway
            tvHandler?.postDelayed({
                if (!future.isDone) {
                    Log.d(TAG, "initializeTV: Auto-detection still in progress after 90s, setting resolution with fallback source")
                    setTvSourceHDMIResolution()
                } else {
                    Log.d(TAG, "initializeTV: 90s timer fired but auto-detection already completed, skipping early resolution")
                }
            }, 90_000)
        }.whenComplete { tunerInfo: DetectedTunerInfo?, error: Throwable? ->
            if (error != null) {
                Log.e(TAG, "initializeTV: Auto-detection failed: ${error.message}")
            } else if (tunerInfo != null) {
                Log.d(TAG, "initializeTV: Auto-detection succeeded: source=${tunerInfo.tunerSource}, firmware=${tunerInfo.firmwareVersion}")
                Log.d(TAG, "initializeTV: Setting resolution with detected source=${tunerInfo.tunerSource}")
                setTvSourceHDMIResolution(tvSource = tunerInfo.tunerSource)
            }
            if (source.followsSwiftProtocol()) {
                var i = 0
                while (i < 5 && !ensureTunerMatchesDetectedHardware(source, i == 4)) {
                    ThreadHelper.sleepIgnoreInterruption(4000)
                    i++
                }
            }
            if (source == TVSource.US4) {
                val elapsedTime = SystemClock.elapsedRealtime()
                if (elapsedTime < 72000) {
                    ThreadHelper.sleepIgnoreInterruption(72000 - elapsedTime.toInt())
                }
            }
            getTunerNetworkDetails(60000)
            resetTV(true)
            tvInitialized = true
        }

        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_ISC_AT_BOOT_BOOL)) {
            Log.d(TAG, "[km4] initializeTV: DATA_ENABLE_ISC_AT_BOOT_BOOL == true, call runKm4IscProcess()")
            runKm4IscProcess()
        }
    }

    private fun ensureTVSettingIsValidForPlatform(source: TVSource) {
        val sources = ProductConfig.getAvailableTuners()
        if (!sources.contains(source)) {
            for (s in sources) {
                DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, s.toString())
                break
            }
        }
    }

    private fun ensureTunerMatchesDetectedHardware(source: TVSource, force: Boolean): Boolean {
        var sourceDetected = false
        val tuner = TunerFactory.getTuner(context, TVSource.US6)
        if (tuner != null) {
            sourceDetected = tuner.ensureTunerMatchesDetectedHardware(source, force)
            tuner.destroy()
        }
        return sourceDetected
    }

    /**
     * Attempts to retrieve tuner network details (IP and MAC address) within a specified timeout period.
     * If the initial attempt to retrieve the network details fails, the method will schedule retries every 10 seconds
     * until the operation is successful or the specified timeout period elapses.
     *
     *
     * This method utilizes a [Runnable] to perform tuner communication, and schedules execution
     * and retries using a [Handler] to avoid blocking the calling thread.
     *
     * @param timeoutMillis The maximum amount of time, in milliseconds, to attempt retrieving tuner network details.
     *
     *
     */
    private fun getTunerNetworkDetails(timeoutMillis: Long) {
        Log.d(TAG, "getTunerNetworkDetails: Starting with timeout=${timeoutMillis}ms")
        val startTime = System.currentTimeMillis()
        val runnable: Runnable = object : Runnable {
            var attempt = 0
            override fun run() {
                attempt++
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "getTunerNetworkDetails: Attempt $attempt (elapsed=${elapsed}ms/${timeoutMillis}ms)")
                if (getTunerNetworkDetails()) {
                    Log.d(TAG, "getTunerNetworkDetails: Succeeded on attempt $attempt after ${System.currentTimeMillis() - startTime}ms")
                } else if (elapsed < timeoutMillis) {
                    Log.d(TAG, "getTunerNetworkDetails: Failed on attempt $attempt, retrying in 10s")
                    tvHandler?.postDelayed(this, 10000)
                } else {
                    Log.w(TAG, "getTunerNetworkDetails: Timed out after ${timeoutMillis}ms ($attempt attempts)")
                }
            }
        }
        tvHandler?.post(runnable)
    }

    /**
     * Attempts to retrieve network details (IP address and MAC address) for the currently selected tv source.
     * The retrieved network details will be accessible via the following DataMatrix keys:
     * [TVKeys.EVENT_TUNER_MAC_ADDRESS_STRING]
     * [TVKeys.EVENT_TUNER_IP_ADDRESS_STRING]
     *
     *
     * The method determines the type of tuner based on the current TV source setting and proceeds only
     * if it is an IP tuner.
     *
     *
     *
     * @return `true` if the network details were successfully retrieved or if no retrieval was necessary
     * (e.g., non-IP tuner), `false` otherwise.
     */
    private fun getTunerNetworkDetails(): Boolean {
        val tvSource = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))
        val tuner = TunerFactory.getTuner(context, tvSource)
        try {
            if (tuner !is IChimeraIPTuner<*>) {
                Log.d(TAG, "getTunerNetworkDetails: source=$tvSource is not an IP tuner, skipping")
                return true
            }

            Log.d(TAG, "getTunerNetworkDetails: Requesting IP and MAC from source=$tvSource")
            // Request IP & MAC address from tuner
            val ipTuner = tuner
            val ipAddressFuture = ipTuner.ipAddress
            val macAddressFuture = ipTuner.macAddress

            // Wait for tuner IP & MAC address response to ensure tuner is not destroyed too early
            CompletableFuture.allOf(ipAddressFuture, macAddressFuture)[5, TimeUnit.SECONDS]
            Log.d(TAG, "getTunerNetworkDetails: Obtained ip=${ipAddressFuture.get()}, mac=${macAddressFuture.get()}")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "getTunerNetworkDetails: Failed for source=$tvSource: ${e.javaClass.simpleName}: ${e.message}")
            return false
        } finally {
            if (tuner != null) {
                // Sleep for short period of time to prevent issues with tuner being constructed and immediately destroyed
                SystemClock.sleep(300)
                tuner.destroy()
            }
        }
    }

    private fun autoDetectTVTuner(): CompletableFuture<DetectedTunerInfo> {
        return autoDetectTVTuner(5000L, 10, 10000L)
    }

    /**
     * Asynchronously auto-detects TV tuner hardware.
     *
     *
     * This method attempts to detect the type of TV tuner present using a set of predefined detection methods.
     * It retries detection based on the specified maximum number of retries and delays between each retry.
     * If a tuner is detected, the future is completed with the detected tuner info. If no tuner is detected
     * after all retries, the future is completed exceptionally with a `TunerNotDetectedException`.
     *
     * @param timeoutMillis The maximum time in milliseconds to wait for each tuner detection attempt.
     * @param maxRetries The maximum number of detection attempts to make.
     * @param retryDelayMillis The delay in milliseconds between each detection attempt.
     * @return A [CompletableFuture] that, when completed, provides the detected [DetectedTunerInfo],
     * or is completed exceptionally if no tuner is detected.
     */
    private fun autoDetectTVTuner(
        timeoutMillis: Long,
        maxRetries: Int,
        retryDelayMillis: Long
    ): CompletableFuture<DetectedTunerInfo> {
        val currentTvSource = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))
        val tunerInfoFuture = CompletableFuture<DetectedTunerInfo>()
        val possibleTuners = ProductConfig.getAvailableTuners()
        val possibleSwiftTuner = possibleTuners.any { it.followsSwiftProtocol() }
        val possibleLG6500 = possibleTuners.any { it.isLG6500 }
        val possibleLG750E = possibleTuners.any { it.isLG750E }
        val tunerDetectionMethods = listOfNotNull(
            SwiftTunerDetection(timeoutMillis, context).takeIf { possibleSwiftTuner },
            LG6500TunerDetection(timeoutMillis, context).takeIf { possibleLG6500 },
            Lg750ETunerDetection(timeoutMillis, context).takeIf { possibleLG750E },
        )

        if (tunerDetectionMethods.isEmpty()) {
            Log.d(TAG, "autoDetectTVTuner: No auto-detectable tuners for this product, skipping detection")
            tunerInfoFuture.completeExceptionally(TunerNotDetectedException())
            return tunerInfoFuture
        }

        Log.d(TAG, "autoDetectTVTuner: Starting detection with ${tunerDetectionMethods.size} method(s), maxRetries=$maxRetries, timeout=${timeoutMillis}ms, retryDelay=${retryDelayMillis}ms")
        Log.d(TAG, "autoDetectTVTuner: Possible tuners: swift=$possibleSwiftTuner, LG6500=$possibleLG6500, LG750E=$possibleLG750E")

        // Ensure tuner port is being provided power. This may be disabled on boot if the active tv source is US4
        DataMatrix.putBoolean(HALKeys.ACTION_SET_TV_IO_POWER_STATE_BOOL, true)

        val detectionRunnable: Runnable = object : Runnable {
            var attempts: Int = 0
            override fun run() {
                if (attempts < maxRetries) {
                    Log.d(TAG, "autoDetectTVTuner: Attempt ${attempts + 1}/$maxRetries")
                    for (tunerDetectionMethod in tunerDetectionMethods) {
                        try {
                            val detectedInfo = tunerDetectionMethod.call()
                            Log.d(TAG, "autoDetectTVTuner: Detected tuner via ${tunerDetectionMethod.javaClass.simpleName}: source=${detectedInfo.tunerSource}, firmware=${detectedInfo.firmwareVersion}")
                            persistDetectedTunerInfo(detectedInfo)
                            tunerInfoFuture.complete(detectedInfo)
                            break
                        } catch (e: TunerNotDetectedException) {
                            Log.d(TAG, "autoDetectTVTuner: ${tunerDetectionMethod.javaClass.simpleName} did not detect tuner: ${e.message}")
                        }
                    }
                    if (!tunerInfoFuture.isDone) {
                        attempts++
                        Log.d(TAG, "autoDetectTVTuner: No tuner detected on attempt $attempts, retrying in ${retryDelayMillis}ms")
                        tvHandler?.postDelayed(this, retryDelayMillis)
                    }
                } else {
                    Log.w(TAG, "autoDetectTVTuner: All $maxRetries attempts exhausted, no tuner detected")
                    tunerInfoFuture.completeExceptionally(TunerNotDetectedException())
                    // Disable power for tuner port if tuner is not detected and US4 is the active
                    // tv source. This is due to US4 using a different port for communication.
                    if (currentTvSource == TVSource.US4) {
                        Log.d(TAG, "autoDetectTVTuner: Disabling tuner IO power for US4 source after failed detection")
                        DataMatrix.putBoolean(HALKeys.ACTION_SET_TV_IO_POWER_STATE_BOOL, false)
                    }
                }
            }
        }
        tvHandler?.post(detectionRunnable)
        return tunerInfoFuture
    }

    private fun persistDetectedTunerInfo(info: DetectedTunerInfo) {
        SystemPropertyUtil.set("persist.jht.tuner.detect", info.tunerSource.name)
        SystemPropertyUtil.set("persist.jht.tuner.version", info.firmwareVersion)
    }

    private fun getDetectedTvSource(): TVSource? {
        return TVSource.fromName(
            SystemPropertyUtil.get("persist.jht.tuner.detect"),
            null
        )
    }

    private fun getSelectedTvSource(): TVSource {
        return TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING))
    }

    private fun getSelectedTvHdmiResolution(
        defaultIfInvalid: TVHDMIOutputResolution = ProductConfig.getHDMIResolutionConfig().defaultResolution
    ): TVHDMIOutputResolution {
        val resolutionConfig = ProductConfig.getHDMIResolutionConfig()
        val selectedResolutionName = DataMatrix.getString(SettingKeys.TV.DATA_HDMI_OUTPUT_RESOLUTION_STRING)
        return if (resolutionConfig.isValidResolution(selectedResolutionName)) resolutionConfig.fromName(selectedResolutionName)
        else defaultIfInvalid
    }

    /*
     * TODO: As it stands currently, we always ship consoles with tuners attached that support the
     *  same resolution as the console. Sometimes the tuner even supports higher resolutions than the
     *  console it is attached to. This is okay because we have the console dictate what resolution
     *  the tuner outputs at (the console's maximum). Should the day come, however, that we wish to
     *  ship older tuner with newer consoles (ex: Swift with Onyx Vortex) then this logic will need
     *  to be updated so the console and tuner agree on a mutually supported max/min resolution.
     */
    private fun setTvSourceHDMIResolution(
        tvSource: TVSource = getDetectedTvSource() ?: getSelectedTvSource(),
        hdmiResolution: TVHDMIOutputResolution = getSelectedTvHdmiResolution()
    ) {
        Log.d(TAG, "setTvSourceHDMIResolution: source=$tvSource, resolution=$hdmiResolution (detected=${getDetectedTvSource()}, selected=${getSelectedTvSource()})")
        val tuner = TunerFactory.getTuner(context, tvSource)
        if (tuner != null) {
            tuner.setTunerOutputResolution(hdmiResolution.mapToTunerOutputResolution())
            Log.d(TAG, "setTvSourceHDMIResolution: Resolution set successfully for source=$tvSource")
            tuner.destroy()
        } else {
            Log.w(TAG, "setTvSourceHDMIResolution: TunerFactory returned null for source=$tvSource, cannot set resolution")
        }
    }

    @Synchronized
    private fun resetTV(force: Boolean, clearSession: Boolean = false) {
        if (tvInitialized || force) {
            // Reset the TV here.  We need to wait a little bit to make sure it is fully reset.
            val source = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name))

            if (source.useNewLib()) {
                resetTunerNewLib(source, clearSession)
            } else {
                resetTunerOldLib(source)
            }
            DataMatrix.putInt(TVKeys.DATA_REMOTE_TV_CURRENT_CHANNEL_INT, DataMatrix.getInt(SettingKeys.TV.DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT))
        }
    }

    @Synchronized
    private fun rebootTV(skipClose: Boolean) { // if console is going to reboot as well, might as well leave ports open
        try {
            val source = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name))
            if (source.useNewLib()) {
                // Early return if tuner is null
                val tuner = TunerFactory.getTuner(context, source) ?: return
                if (tuner is Lg750ETuner) {
                    tuner.clearUserSession()
                }
                val transactionManager = tuner.tuner().startTransaction()
                tuner.reboot()
                waitForTransaction(transactionManager, 1000)
                if (!skipClose) {
                    tuner.destroy()
                }
            } else {
                val tvController = TVControllerFactory.getTVController(context, source)
                tvController.open()
                // Delays help allow the serial port send the commands and get responses. (but need to be quick about it)
                ThreadHelper.sleepIgnoreInterruption(500)
                tvController.reboot()
                ThreadHelper.sleepIgnoreInterruption(500)
                if (!skipClose) {
                    tvController.destroy()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reboot TV tuner", e)
        }
    }

    private fun resetTunerOldLib(source: TVSource) {
        val tvController = TVControllerFactory.getTVController(context, source)
        tvController.open()
        // Delays help allow the serial port send the commands and get responses.
        ThreadHelper.sleepIgnoreInterruption(3000)
        tvController.reset()
        ThreadHelper.sleepIgnoreInterruption(2000)
        tvController.destroy()
    }

    private fun resetTunerNewLib(source: TVSource, clearSession: Boolean = false) {
        // Early return if tuner is null
        val tuner = TunerFactory.getTuner(context, source) ?: return

        // Reset the primary tuner
        val transactionManager = tuner.tuner().startTransaction()
        tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.MUTE_OFF)
        tuner.resetTunerVolume(ProductConfig.getTunerDefaultVolume(source))
        tuner.reset()
        if (clearSession && tuner is Lg750ETuner) {
            tuner.clearUserSession()
        }

        // If source is CAB and secondary tuner is installed, reset secondary tuner
        if (source == TVSource.CAB) {
            val secondarySource = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_CAB_TV_SOURCE_STRING)) //NON-NLS
            val availableTuners = ProductConfig.getAvailableTuners()
            if (availableTuners.contains(secondarySource)) {
                val secondaryTuner = TunerFactory.getTuner(context, secondarySource)
                if (secondarySource != TVSource.CAB && secondaryTuner != null) {
                    secondaryTuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.MUTE_OFF)
                    secondaryTuner.resetTunerVolume(ProductConfig.getTunerDefaultVolume(secondarySource))
                    secondaryTuner.reset()
                    secondaryTuner.destroy()
                }
            }
        }
        waitForTransaction(transactionManager, 15000)
        tuner.destroy()
    }

    /**
     * Waits for [transactionManager] to finish before the caller destroys the tuner. Tuners without
     * transaction support (LG750E) return null, so this sleeps for the full timeout instead: the
     * default-channel change from `reset()` runs asynchronously and is dropped if the tuner is
     * destroyed first.
     */
    private fun waitForTransaction(transactionManager: TransactionManager?, timeoutMs: Int) {
        if (transactionManager != null) {
            transactionManager.waitForTransactionComplete(timeoutMs)
        } else {
            ThreadHelper.sleepIgnoreInterruption(timeoutMs)
        }
    }

    @Synchronized
    private fun runKm4IscProcess() {
        tvHandler?.post {
            val source = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name))
            if (source == TVSource.KM4) {
                Log.d(TAG, "[km4] HalService runKm4IscProcess: Begin")
                val tvController = TVControllerFactory.getTVController(context, source)
                tvController.open()
                // Delays help allow the serial port send the commands and get responses.
                ThreadHelper.sleepIgnoreInterruption(3000)
                tvController.executeIscProcess()
                Log.d(TAG, "[km4] HalService runKm4IscProcess: End")
                tvController.destroy()
            }
        }
    }

    private fun destroyTvController() {
        val source = TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, ProductConfig.getDefaultTuner().name))
        if (source == TVSource.KM4) {
            val tvController = TVControllerFactory.getTVController(context, source)
            Log.d(TAG, "[km4] HalService destroyTvController: Begin")
            tvController.destroy()
            Log.d(TAG, "[km4] HalService destroyTvController: End")
        }
    }

}
