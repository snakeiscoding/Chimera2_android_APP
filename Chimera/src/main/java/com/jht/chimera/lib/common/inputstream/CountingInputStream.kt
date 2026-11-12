package com.jht.chimera.lib.common.inputstream

import java.io.FilterInputStream
import java.io.InputStream

/**
 * A [FilterInputStream] that counts the number of bytes read from the underlying stream.
 *
 * @constructor Creates a new [CountingInputStream] wrapping the given [InputStream].
 * @param input the underlying input stream.
 */
class CountingInputStream(input: InputStream): FilterInputStream(input) {
    /**
     * The total number of bytes that have been read from the stream.
     */
    var bytesRead: Long = 0
        private set

    /**
     * Reads a single byte from the underlying stream.
     *
     * @return the byte read as an integer in the range 0 to 255, or -1 if the end of the stream is reached.
     */
    override fun read(): Int {
        val result = super.read()
        if (result != -1) {
            bytesRead++
        }
        return result
    }

    /**
     * Reads up to [len] bytes of data from the underlying stream into an array of bytes.
     *
     * Increments [bytesRead] by the number of bytes actually read.
     *
     * @param b the buffer into which the data is read.
     * @param off the start offset in array [b] at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = super.read(b, off, len)
        if (count != -1) {
            bytesRead += count
        }
        return count
    }
}