package com.jht.chimera.service.hal.lib.lcb.lcbcommlib

import android.util.Log
import com.jht.utility.runCatchingCancellable
import java.util.function.BiConsumer

// We could make this a constructor parameter if we need this dynamic. I put it here to demonstrate how we could disable rewind.
// You can flip this false for a quick demonstration of what unit tests need this to pass.
private const val useRewind: Boolean = true

/** Parsers [LcbPacket] out of raw data bytes.
 *  In general meant to operate on a single thread. Usually the port reading thread.
 *  The [reset] method can be called across thread (it sets flag for next parse).
 * */
class LcbParser : LcbPacket() {
    companion object {
        private const val TAG = "LcbParser"
    }
    enum class ParseResult {
        Success,
        Incomplete,
        InvalidStartByte,
        InvalidRtnCommand,
        InvalidDataLength,
        InvalidCrc,
        UnknownError;
        fun isError() = this != Success && this != Incomplete
    }
    private enum class ParseState {
        START_BYTE,
        STATUS_BYTE,
        COMMAND_BYTE,
        DATA_LENGTH,
        DATA_BYTES,
        CRC_BYTE
    }
    private val crcGenerator = LcbCrc8()
    private var parseState = ParseState.START_BYTE
    private var parseDataIndex = 0
    // try a flag for async resets instead of thread sync (atomic bool was not recommend in this case)
    @Volatile private var resetRequested = false
    private val rewindBuffer = RewindBuffer()

    /** If parser is waiting for data bytes, return amount of pending bytes. For other states, returns zero. */
    fun pendingDataBytes(): Int = when (parseState) {
        ParseState.DATA_BYTES -> data.size - parseDataIndex
        else -> 0
    }
    /** returns true if parser is in the middle of packet parse */
    fun isMidPacket(): Boolean = parseState != ParseState.START_BYTE

    /** requests the parser reset before parsing the next byte (avoids corrupting any ongoing parse if called by thread other than parsing thread) */
    fun reset() {
        resetRequested = true
    }

    /** If a packet parse was cut off incomplete at timeout, replay the rewind buffer from next possible start if there was one (bad data length overshoot).
     * If replay ends with Incomplete packet, parser will run the next bytes from that point. Call [reset] if a hard restart is desired moving forward.
     * Returns true if rewind buffer was checked.
     * (If used should only be run in same thread as [parse] or with a sync lock)
     * */
    fun rewindTimedOutPacket(callback: BiConsumer<ParseResult, LcbPacket?>): Boolean {
        // if packet was in progress and it had another possible start in it, rewind the last attempt
        if (useRewind && parseState != ParseState.START_BYTE && rewindBuffer.hasPossibleStart()) {
            // Treat the stuck attempt as a synthetic error and let checkRewindBuffer resync
            resetParsing()
            // This could exit with Incomplete and data left in rewind buffer. The caller could call reset if they want a hard restart after the search.
            checkRewindBuffer(ParseResult.UnknownError, callback)
            return true
        }
        return false
    }

    /** parse given [data] buffer up to [length] of new bytes, invoke callback when something happens */
    fun parse(data: ByteArray, length: Int, callback: BiConsumer<ParseResult, LcbPacket?>) {
        runCatchingCancellable {
            for (i in 0..<length) {
                if (resetRequested) {
                    resetFullSync()
                    resetRequested = false
                }
                val parseResult = feedFreshByte(data[i])
                if (parseResult == ParseResult.Success) {
                    // if parse was success, we can callback and purge everything for the next pass
                    safeCallback(parseResult, deepCopyNoMeta(), callback)
                    resetRequested = true
                } else if (parseResult.isError()) {
                    // there was an error, we need to recheck what we read
                    resetParsing()
                    if (useRewind) checkRewindBuffer(parseResult, callback)
                    else safeCallback(parseResult, null, callback)
                }
            }
        }.onFailure {
            Log.e(TAG,"Parsing loop threw exception, doing full reset recovery", it)
            resetParsing()
            rewindBuffer.reset()
            safeCallback(ParseResult.UnknownError, null, callback)
        }
    }
    /** provides a wrapper around parser callback to avoid it crashing the parser */
    private fun safeCallback(result: ParseResult, packet: LcbPacket?, callback: BiConsumer<ParseResult, LcbPacket?>) {
        runCatchingCancellable {
            callback.accept(result, packet)
        }.onFailure {
            Log.w(TAG, "Parser callback threw exception", it)
        }
    }

    /** if there is a next possible start in the rewind buffer, replay it until whole buffer has been searched for missed packets */
    private fun checkRewindBuffer(parseResult: ParseResult, callback: BiConsumer<ParseResult, LcbPacket?>): ParseResult {
        var lastParseResult = parseResult
        // possible starts loop
        while (rewindBuffer.hasPossibleStart() && lastParseResult != ParseResult.Incomplete) {
            // set read for next possible start and reset next possible start
            rewindBuffer.setReadForNextPossibleStart()
            // processing loop for current possible start
            while (rewindBuffer.isNotEmpty()) {
                if (resetRequested) return lastParseResult // full parser reset request should bail out, next incoming will have clean slate
                val currentByte = rewindBuffer.currentByte() ?: break // technically if not empty, this should not return null
                lastParseResult = feedReplayByte(currentByte)
                rewindBuffer.advanceToNextByte()
                if (lastParseResult == ParseResult.Success) {
                    safeCallback(lastParseResult, deepCopyNoMeta(), callback)
                    // in case there is another packet after this one in the rewind buffer keep scanning
                    resetParsing()
                    rewindBuffer.seekToNextPossibleStart()
                    break
                } else if (lastParseResult.isError()) {
                    // reset local parsing, move to next rewind buffer location
                    resetParsing()
                    // if there is still a viable start candidate to check, keep it, otherwise check buffer for next
                    if (!rewindBuffer.hasPossibleStart()) rewindBuffer.seekToNextPossibleStart()
                    break
                }
            }
        }
        // if nothing was found, just relay the last error (essential pass through error if we cannot avoid it)
        if (lastParseResult.isError()) safeCallback(lastParseResult, null, callback)
        // NOTE: we leave the rewindBuffer with content if we leave with an incomplete packet still in the oven
        return lastParseResult
    }

    /** resets base parser state tracking */
    private fun resetParsing() {
        crcGenerator.reset()
        parseState = ParseState.START_BYTE
        command = LcbCommands.GetLCBStatus
        data = emptyData
    }

    /** resets everything (synchronous, should be called on same thread as parser) */
    private fun resetFullSync() {
        resetParsing()
        rewindBuffer.reset()
    }

    /** feed bytes from receive buffer into parsing state machine */
    private fun feedFreshByte(currentByte: Byte): ParseResult {
        runCatchingCancellable {
            if (useRewind && parseState > ParseState.START_BYTE) {
                // if in packet, write to the rewind buffer
                if (!rewindBuffer.write(currentByte)) {
                    // This should never happen, but if it does better catch it
                    Log.w(TAG, "Rewind buffer overflow, resetting rewind")
                    rewindBuffer.reset()
                }
            }
            return feedByte(currentByte)
        }.onFailure { Log.e(TAG, "Parsing fresh data encountered unknown error", it) }
        return ParseResult.UnknownError
    }

    /** feed bytes from rewind buffer into parsing state machine */
    private fun feedReplayByte(currentByte: Byte): ParseResult {
        runCatchingCancellable {
            if (parseState > ParseState.START_BYTE) {
                // queue up next possible start if one is not already
                rewindBuffer.checkCurrentByteForPossibleStart()
            }
            return feedByte(currentByte)
        }.onFailure { Log.e(TAG, "Parsing rewind data encountered unknown error", it) }
        return ParseResult.UnknownError
    }

    /** feeds byte to the base parser raw */
    private fun feedByte(currentByte: Byte): ParseResult {
        when (parseState) {
            ParseState.START_BYTE -> if (currentByte == START_BYTE_RESPONSE) {
                parseState = ParseState.STATUS_BYTE
                isCommand = false
                crcGenerator.reset()
                crcGenerator.feedByte(currentByte)
            } else {
                return ParseResult.InvalidStartByte
            }

            ParseState.STATUS_BYTE -> {
                crcGenerator.feedByte(currentByte)
                addressOrStatus = currentByte
                parseState = ParseState.COMMAND_BYTE
            }

            ParseState.COMMAND_BYTE -> {
                crcGenerator.feedByte(currentByte)
                val rawCommand = DataUtils.unsignedByteToInt(currentByte)
                val readCommand = LcbCommands.fromInt(rawCommand)
                if (readCommand == null) {
                    // This applies because we only expect commands back that we sent.
                    // If a device can send commands we might not know about, this may need to be changed.
                    parseState = ParseState.START_BYTE
                    return ParseResult.InvalidRtnCommand
                }
                command = readCommand
                parseState = ParseState.DATA_LENGTH
            }

            ParseState.DATA_LENGTH -> {
                crcGenerator.feedByte(currentByte)
                val dataLength = DataUtils.unsignedByteToInt(currentByte)
                if (dataLength > PACKET_MAX_DATA_LENGTH) {
                    parseState = ParseState.START_BYTE
                    return ParseResult.InvalidDataLength
                } else if (dataLength == 0) {
                    data = emptyData
                    parseState = ParseState.CRC_BYTE
                } else {
                    data = ByteArray(dataLength)
                    parseState = ParseState.DATA_BYTES
                }
                parseDataIndex = 0
            }

            ParseState.DATA_BYTES -> {
                crcGenerator.feedByte(currentByte)
                data[parseDataIndex] = currentByte
                if (++parseDataIndex == data.size) {
                    parseState = ParseState.CRC_BYTE
                }
            }

            ParseState.CRC_BYTE -> {
                parseState = ParseState.START_BYTE
                val calculatedCrc = crcGenerator.crc
                if (currentByte != calculatedCrc) {
                    //Log.e(TAG, String.format("Invalid LcbCRC8! Parsed=0x%02X, Calculated=0x%02X", currentByte, calculatedCrc))
                    return ParseResult.InvalidCrc
                }
                return ParseResult.Success
            }
        }
        return ParseResult.Incomplete
    }


    /** Buffer the current packet being parsed in case in fails and we need to rewind.
     *  Written to only when we are collecting fresh bytes for a possible packet.
     *  Read only when replaying bytes back through the parser to find missed starts.
     *  Each parse attempt sets read cursor to next possible start if there is one.
     *  Should not need more than max enforced packet size.
     * */
    private class RewindBuffer {
        private val buffer = ByteArray(PACKET_MAX_SIZE + 1) // add one for tracking full (prevent read==write)
        private var readIndex = 0
        private var writeIndex = 0
        private var nextPossibleStart = -1

        /** full reset of buffer to empty state and no possible starts */
        fun reset() {
            readIndex = 0
            writeIndex = 0
            nextPossibleStart = -1
        }
        fun isNotEmpty(): Boolean = readIndex != writeIndex
        /** add byte to the buffer, return false if full (checks and sets next possible start if needed) */
        fun write(byte: Byte): Boolean {
            val nextIndex = (writeIndex + 1) % buffer.size
            if (nextIndex == readIndex) {
                // buffer full
                return false
            }
            // If possible start set, it could become the readIndex, so preserve it as though it could be the read index.
            // If you are adding fresh bytes to an incomplete packet, it is possible next start is before current read index.
            if (nextPossibleStart >= 0 && nextPossibleStart == nextIndex) {
                return false
            }
            buffer[writeIndex] = byte
            if (nextPossibleStart < 0 && byte == START_BYTE_RESPONSE) nextPossibleStart = writeIndex
            writeIndex = nextIndex
            return true
        }
        /** returns the byte at current read location or null */
        fun currentByte(): Byte? {
            if (readIndex == writeIndex) return null
            return buffer[readIndex]
        }
        /** advances read cursor, return false if nothing left */
        fun advanceToNextByte(): Boolean {
            if (readIndex == writeIndex) return false
            readIndex = (readIndex + 1) % buffer.size
            return true
        }
        /** if there is a next possible start, reset the read cursor and clear old possible start, otherwise return false */
        fun setReadForNextPossibleStart(): Boolean {
            if (nextPossibleStart < 0) {
                // if there is no other attempt, do we flush remaining since there is nothing useful there?
                reset()
                return false
            }
            readIndex = nextPossibleStart
            nextPossibleStart = -1
            return true
        }
        /** Returns true if there is a next start being tracked */
        fun hasPossibleStart(): Boolean = nextPossibleStart >= 0
        /** called during rewind parsing to set the next possible start within the current rewind if one is not set */
        fun checkCurrentByteForPossibleStart() {
            val replayByte = currentByte()
            if (nextPossibleStart < 0 && replayByte == START_BYTE_RESPONSE) nextPossibleStart = readIndex
        }
        /** scan buffer from current read position for next possible start, if end of buffer reached just reset next possible start, true if found */
        fun seekToNextPossibleStart(): Boolean {
            while (readIndex != writeIndex) {
                if (buffer[readIndex] == START_BYTE_RESPONSE) {
                    nextPossibleStart = readIndex
                    return true
                }
                readIndex = (readIndex + 1) % buffer.size
            }
            nextPossibleStart = -1
            return false
        }
    }
}

