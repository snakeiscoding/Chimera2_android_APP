package com.jht.chimera.lib.tv.tuner.lg.lg750e

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
 * Implementation of [TunerDetectionMethod] for detecting an LG750E tuner (US9/KM7/BR3/JP1).
 *
 * This class is responsible for detecting and obtaining information about an LG750E TV tuner.
 * It attempts to communicate with the LG750E tuner and fetch its model within a specified timeout period.
 * If the tuner model is successfully obtained, it returns the tuner's firmware version and its associated TV source';
 * otherwise, it throws a [TunerNotDetectedException].
 *
 * @property timeoutMillis The maximum time, in milliseconds, to wait for the tuner detection response before timing out.
 * @property context The context (typically a reference to an `Activity` or `Application`) used for accessing the tuner.
 *
 * @throws TunerNotDetectedException if the LG750E tuner cannot be detected or if any issues arise during the detection process.
 * @return [DetectedTunerInfo] containing the TV source and version of the detected LG750E tuner.
 */
class Lg750ETunerDetection(private val timeoutMillis: Long, private val context: Context): TunerDetectionMethod {
    @Throws(TunerNotDetectedException::class)
    override fun call(): DetectedTunerInfo {
        var tuner: Lg750ETunerBase? = null

        return try {
            // LG750E tuner instance used for tuner communication
            tuner = Lg750ETunerBase(
                context,
                File(context.filesDir, "settings/lg750e")
            )

            // Attempt to get LG750E tuner model and firmware version, place into a CompletableFuture<DetectedTunerInfo>
            val tunerModelFuture = tuner.model
            val tunerVersionFuture = tuner.version
            val tunerInfoFuture = CompletableFuture.allOf(tunerModelFuture, tunerVersionFuture).thenApply {
                val tunerModel = tunerModelFuture.join()
                val tunerVersion = tunerVersionFuture.join()
                return@thenApply DetectedTunerInfo(
                    TVSource.getSource(tunerModel, null) ?: throw IOException("Unrecognized LG750E tuner model: $tunerModel"),
                    tunerVersion ?: throw IOException("Invalid firmware version received for LG750E tuner")
                )
            }

            // Wait for and return detected LG tuner info or throw timeout exception
            tunerInfoFuture.get(timeoutMillis, TimeUnit.MILLISECONDS)
        }
        catch (e: Exception) {
            // Wrap any exceptions in TunerNotDetectedException
            throw TunerNotDetectedException("LG750E tuner (US9 / KM7 / BR3 / JP1) not found", e)
        }
        finally {
            // Cleanup LG750E tuner instance
            tuner?.destroy()
        }
    }
}