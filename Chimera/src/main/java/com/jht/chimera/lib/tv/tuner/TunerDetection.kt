package com.jht.chimera.lib.tv.tuner

import android.content.Context
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.serialport.peripheral.tuner.TunerProperties
import com.jht.serialport.peripheral.tuner.swift.GenericSwiftTuner
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Interface defining a method for TV tuner detection.
 *
 * This interface extends [Callable] and is used to implement different tuner detection strategies.
 * Each implementation of this interface should provide the logic to detect a specific type of TV tuner.
 *
 * The [call] method, when invoked, should attempt to detect a TV tuner and return information about it.
 * If the tuner is not detected, a [TunerNotDetectedException] should be thrown.
 *
 * Implementations of this interface are expected to be used in asynchronous operations, typically for auto-detecting
 * tuner hardware in TV or similar devices.
 *
 * @throws TunerNotDetectedException if the tuner cannot be detected or if any issue arises during the detection process.
 * @return [DetectedTunerInfo] containing details about the detected tuner.
 */
interface TunerDetectionMethod: Callable<DetectedTunerInfo> {
    @Throws(TunerNotDetectedException::class)
    override fun call(): DetectedTunerInfo
}


/**
 * Data class representing information about a detected TV tuner.
 *
 * @property tunerSource The [TVSource] identified during detection.
 * @property firmwareVersion The firmware version of the detected tuner.
 */
data class DetectedTunerInfo (
    val tunerSource: TVSource,
    val firmwareVersion: String = ""
)


/**
 * Exception thrown when a TV tuner cannot be detected.
 *
 * This exception is used in the context of tuner detection processes where no tuner hardware is found
 * or the tuner cannot be initialized. It extends the standard [Exception] class and provides several
 * constructors for different scenarios of exception handling.
 *
 * Constructors:
 * [TunerNotDetectedException] - Default constructor with a standard error message "No tuner detected."
 * [TunerNotDetectedException] - Constructor that allows for a custom error message.
 * [TunerNotDetectedException] - Constructor that allows for a custom error message and a throwable cause.
 *
 * @constructor Creates an instance of [TunerNotDetectedException] with various overloads for customizing the error message and cause.
 */
class TunerNotDetectedException : Exception {
    constructor(): super("No tuner detected.")
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}


/**
 * Implementation of [TunerDetectionMethod] for detecting a Swift tuner.
 *
 * This class encapsulates the logic for detecting and obtaining properties of a Swift TV tuner.
 * It tries to communicate with the tuner and fetch its properties within a specified timeout period.
 * If successful, it returns information about the detected tuner; otherwise, it throws a [TunerNotDetectedException].
 *
 * @property timeoutMillis The maximum time, in milliseconds, to wait for tuner detection before timing out.
 * @property context The context (often a reference to an `Activity` or `Application`) used for creating the tuner.
 *
 * @throws TunerNotDetectedException if the Swift tuner cannot be detected or if any issues arise during the detection process.
 * @return [DetectedTunerInfo] containing details about the detected Swift tuner.
 */
class SwiftTunerDetection(private val timeoutMillis: Long, private val context: Context): TunerDetectionMethod {
    @Throws(TunerNotDetectedException::class)
    override fun call(): DetectedTunerInfo {
        // Create generic Swift tuner
        val genericSwiftTuner = GenericSwiftTuner(context, ProductConfig.portMappings.swiftPath())
        val tunerPropertiesFuture = CompletableFuture<TunerProperties>()

        return try {
            // Attempt to get tuner properties
            genericSwiftTuner.loadProperties { value: TunerProperties -> tunerPropertiesFuture.complete(value) }
            val tunerProperties = tunerPropertiesFuture[timeoutMillis, TimeUnit.MILLISECONDS]

            // Validate & return detected tuner info
            DetectedTunerInfo(
                TVSource.getSource(tunerProperties.tunerType(), null) ?: throw IOException("Invalid tuner type received"),
                tunerProperties.version() ?: throw IOException("Invalid firmware version received")
            )
        } catch (e: java.lang.Exception) {
            // Swift tuner not found, throw exception
            throw TunerNotDetectedException("Swift tuner not found", e)
        } finally {
            // Cleanup generic swift tuner
            genericSwiftTuner.destroy()
        }
    }

}