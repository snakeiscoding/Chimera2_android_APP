package com.jht.chimera.lib.tv.tuner.lg.lg6500

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.tv.tuner.DetectedTunerInfo
import com.jht.chimera.lib.tv.tuner.TunerDetectionMethod
import com.jht.chimera.lib.tv.tuner.TunerNotDetectedException
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


/**
 * Implementation of [TunerDetectionMethod] for detecting a LG6500 tuner (US8/KM6).
 *
 * This class is responsible for detecting and obtaining information about a LG6500 TV tuner.
 * It attempts to communicate with the LG6500 tuner and fetch its model within a specified timeout period.
 * If the tuner model is successfully obtained, it returns the tuner's firmware version and its associated TV source';
 * otherwise, it throws a [TunerNotDetectedException].
 *
 * @property timeoutMillis The maximum time, in milliseconds, to wait for the tuner detection response before timing out.
 * @property context The context (typically a reference to an `Activity` or `Application`) used for accessing the tuner.
 *
 * @throws TunerNotDetectedException if the LG6500 tuner cannot be detected or if any issues arise during the detection process.
 * @return [DetectedTunerInfo] containing the TV source and version of the detected LG6500 tuner.
 */
class LG6500TunerDetection(private val timeoutMillis: Long, private val context: Context): TunerDetectionMethod {
    @Throws(TunerNotDetectedException::class)
    override fun call(): DetectedTunerInfo {
        var tuner: LG6500TunerBase? = null

        return try {
            // LG6500 tuner instance used for tuner communication
            tuner = LG6500TunerBase(
                context,
                File(context.filesDir, "settings/lg6500")
            )

            // Must open in try-catch in case system is emulator and cannot open serial port.
            LG6500SerialPort.getInstance().open()
            tuner.restart(LG6500SerialPort.getInstance().inputStream, LG6500SerialPort.getInstance().outputStream, null);

            // Attempt to get LG6500 tuner model and firmware version, place into a CompletableFuture<DetectedTunerInfo>
            val tunerModelFuture = tuner.model
            val tunerVersionFuture = tuner.version
            val tunerInfoFuture = CompletableFuture.allOf(tunerModelFuture, tunerVersionFuture).thenApply {
                val tunerModel = tunerModelFuture.join()
                val tunerVersion = tunerVersionFuture.join()
                return@thenApply DetectedTunerInfo(
                    TVSource.getSource(tunerModel, null) ?: throw IOException("Unrecognized LG6500 tuner model: $tunerModel"),
                    tunerVersion ?: throw IOException("Invalid firmware version received for LG6500 tuner")
                )
            }

            // Wait for and return detected LG tuner info or throw timeout exception
            tunerInfoFuture.get(timeoutMillis, TimeUnit.MILLISECONDS)
        }
        catch (e: Exception) {
            // Wrap any exceptions in TunerNotDetectedException
            throw TunerNotDetectedException("LG6500 tuner (US8 / KM6) not found", e)
        }
        finally {
            // Cleanup LG6500 tuner instance & associated serial port
            tuner?.destroy()
            try { LG6500SerialPort.getInstance().close() }
            catch (_: Exception) {}
        }
    }
}