@file:JvmName("BumpSensorPathProvider")
package com.jht.bumpsensorinterface

import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.service.hal.lib.io.iocommlib.IoPassthroughPort
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughDevice
import com.jht.libbumpsensor.BumpSensorSerialPort
import com.jht.libbumpsensor.IBumpSensorPathInterface
import com.jht.serialport.CloseableSerialPort
import com.jht.serialport.SerialPortConfiguration
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToLong

/**
 * The is consumed by the Bump Sensor library and included in the library by using reflection.
 * */
@Suppress("unused")
class BumpSensorPathProvider: IBumpSensorPathInterface {
    override fun createPort(configuration: SerialPortConfiguration): BumpSensorSerialPort {
        ProductConfig.portMappings.bumpSensorPath()?.let {
            return BumpSensorSerialPort(it, configuration)
        }
        return BumpPassthroughSerialPort(configuration)
    }
}

// BumpSensor library apparently uses its own serialport rather than ISerialPort, so extended it but insert passthrough for the important bits until we can do something better
private class BumpPassthroughSerialPort(configuration: SerialPortConfiguration) : BumpSensorSerialPort(deviceName = "", configuration) {
    // IO passthrough is slow (fyi there is also a timeout in McuPacketManager writeToPassthrough)
    override val availableTimeoutMs = 2000L
    private val port = IoPassthroughPort(
        passthroughDevice =  McuPassthroughDevice.BUMP_SENSOR,
        readTimeoutMs = (availableTimeoutMs * 0.9).roundToLong(), // give some margin so we timeout before available check does
    )
    override fun open(): CloseableSerialPort {
        port.open()
        return this
    }
    override fun close() {
        port.close()
    }
    override fun flushInput(): Int = port.flushInput()
    override fun drainOutput(): Int = port.drainOutput()
    override val inputStream: InputStream
        get() = checkNotNull(port.inputStream) { "Port not open" }
    override val outputStream: OutputStream
        get() = port.outputStream
    override fun serialBreak(enabled: Boolean): Int {
        return port.serialBreak(enabled)
    }
    override fun setTraceDebug(debug: Boolean) {
        super.setTraceDebug(debug)
        port.setTraceDebug(debug)
    }
}

