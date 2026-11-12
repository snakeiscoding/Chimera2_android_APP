package com.jht.chimera.service.hal.lib.io.iocommlib

import android.os.Bundle
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughDevice.Companion.fromString
import com.jht.datamatrixlib.DataMatrix
import com.jht.serialport.ByteBufferStream
import com.jht.serialport.ISerialPort
import java.io.InputStream
import java.io.OutputStream

// IO mcu comms passthrough constants for data passing
const val PASSTHROUGH_PAYLOAD_KEY = "payload" //NON-NLS
const val PASSTHROUGH_DEVICE_KEY = "device" //NON-NLS

/** Destinations reachable through the IO communication passthrough */
enum class McuPassthroughDevice {
    /** CAB tuner */
    CAB,
    /** Bump sensor */
    BUMP_SENSOR,
    ; // END LIST
    companion object {
        @JvmStatic
        fun fromString(value: String?): McuPassthroughDevice? = entries.find { it.name == value }
    }
}

/** Creates a virtual serial port to a device via IO communications passthrough */
class IoPassthroughPort @JvmOverloads constructor(
    private val passthroughDevice: McuPassthroughDevice,
    // Things like bump sensor expect a read timeout, but most devices use a blocking read thread.
    // If we took SerialPortConfiguration, maybe we could convert it from VTIME deciseconds? but better to be explicit
    private val readTimeoutMs: Long = ByteBufferStream.TIMEOUT_WAIT_FOREVER,
) : ISerialPort {
    private val dataMatrix = DataMatrix()
    override fun setTraceDebug(debug: Boolean) {}
    override fun serialBreak(enabled: Boolean): Int = 0

    @Synchronized
    override fun open() {
        if (isOpen()) return // most of our ports no-op already open
        inputStream = createInputStream() // create stream fresh to avoid any session crossing
        dataMatrix.createParcelableAdapter(HALKeys.EVENT_RECEIVE_DATA_FROM_IO_PASSTHROUGH_PARCELABLE, Bundle::class.java) { bundle: Bundle? ->
            // DM handlers fire on main thread, do not block in here!
            // While we could add coroutine/channel/etc to handle a blocking data add, the buffer itself should be handling bursts.
            // What we could do if we need back pressure is put a block in our OutputStream below if ByteBufferStream reaches a threshold of some kind?
            val device = fromString(bundle?.getString(PASSTHROUGH_DEVICE_KEY))
            if (device != passthroughDevice) return@createParcelableAdapter // ignore if not for us
            bundle?.getByteArray(PASSTHROUGH_PAYLOAD_KEY)?.let { payload ->
                if (payload.isNotEmpty()) inputStream?.addData(payload, payload.size, ByteBufferStream.TIMEOUT_NON_BLOCKING)
            }
        }
    }
    @Synchronized
    override fun close() {
        dataMatrix.finish()
        inputStream?.close()
        inputStream = null
    }
    override fun isOpen() = inputStream != null

    private fun createInputStream() = ByteBufferStream(
        // these command are at most 256 + a header, but there can be bursts of them
        maxQueueLength = 1024,
        readTimeoutMs = readTimeoutMs,
    )
    @Volatile
    private var inputStream: ByteBufferStream? = null
    override fun getInputStream(): InputStream? = inputStream
    override fun flushInput(): Int {
        inputStream?.flushInput()
        return 0 // zero for success
    }

    override fun getOutputStream(): OutputStream = outputStream
    override fun drainOutput(): Int {
        // there is no write buffer to drain
        return 0 // zero for success
    }
    private val outputStream = object : OutputStream() {
        private fun sendBytes(bytes: ByteArray?) {
            if (bytes == null || bytes.isEmpty()) return
            val bundle = Bundle().apply {
                putString(PASSTHROUGH_DEVICE_KEY, passthroughDevice.name)
                putByteArray(PASSTHROUGH_PAYLOAD_KEY, bytes)
            }
            DataMatrix.putParcelable(HALKeys.ACTION_SEND_DATA_TO_IO_PASSTHROUGH_PARCELABLE, bundle)
        }
        override fun write(i: Int) {
            sendBytes(byteArrayOf(i.toByte()))
        }
        override fun write(b: ByteArray?) {
            sendBytes(b)
        }
        override fun write(b: ByteArray, off: Int, len: Int) {
            sendBytes(b.copyOfRange(off, off + len))
        }
    }
}

