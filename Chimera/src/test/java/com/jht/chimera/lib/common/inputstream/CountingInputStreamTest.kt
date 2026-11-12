package com.jht.chimera.lib.common.inputstream

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class CountingInputStreamTest {

    @Test
    fun `test single byte reads increment counter`() {
        val data = byteArrayOf(10, 20, 30)
        val inputStream = ByteArrayInputStream(data)
        val countingStream = CountingInputStream(inputStream)

        // Initially, no bytes have been read.
        assertEquals(0, countingStream.bytesRead)

        // Read one byte at a time.
        assertEquals(10, countingStream.read())
        assertEquals(1, countingStream.bytesRead)

        assertEquals(20, countingStream.read())
        assertEquals(2, countingStream.bytesRead)

        assertEquals(30, countingStream.read())
        assertEquals(3, countingStream.bytesRead)

        // Reading past the end should return -1 and not increment the counter.
        assertEquals(-1, countingStream.read())
        assertEquals(3, countingStream.bytesRead)
    }

    @Test
    fun `test byte array read increments counter`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val inputStream = ByteArrayInputStream(data)
        val countingStream = CountingInputStream(inputStream)
        val buffer = ByteArray(3)

        // Read first three bytes.
        val bytesRead = countingStream.read(buffer, 0, buffer.size)
        assertEquals(3, bytesRead)
        assertEquals(3, countingStream.bytesRead)
        assertArrayEquals(byteArrayOf(1, 2, 3), buffer)

        // Read the remaining bytes.
        val bytesRead2 = countingStream.read(buffer, 0, buffer.size)
        // Only two bytes remain.
        assertEquals(2, bytesRead2)
        assertEquals(5, countingStream.bytesRead)
        // Validate the beginning of the buffer with the remaining bytes.
        assertEquals(4, buffer[0].toInt())
        assertEquals(5, buffer[1].toInt())

        // Further reads should indicate end-of-stream.
        val bytesRead3 = countingStream.read(buffer, 0, buffer.size)
        assertEquals(-1, bytesRead3)
        assertEquals(5, countingStream.bytesRead)
    }

    @Test
    fun `test mixed read operations`() {
        val data = byteArrayOf(100, 101, 102, 103)
        val inputStream = ByteArrayInputStream(data)
        val countingStream = CountingInputStream(inputStream)

        // Read one byte.
        val firstByte = countingStream.read()
        assertEquals(100, firstByte)
        assertEquals(1, countingStream.bytesRead)

        // Read remaining bytes into a buffer.
        val buffer = ByteArray(10)
        val count = countingStream.read(buffer, 0, buffer.size)
        // Should read the remaining 3 bytes.
        assertEquals(3, count)
        assertEquals(4, countingStream.bytesRead)
    }

    @Test
    fun `test no reads does not change counter`() {
        val data = byteArrayOf(1, 2, 3)
        val inputStream = ByteArrayInputStream(data)
        val countingStream = CountingInputStream(inputStream)

        // Without any read operations, bytesRead should be zero.
        assertEquals(0, countingStream.bytesRead)
    }
}
