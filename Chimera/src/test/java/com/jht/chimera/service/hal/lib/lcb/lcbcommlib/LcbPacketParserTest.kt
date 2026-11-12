package com.jht.chimera.service.hal.lib.lcb.lcbcommlib

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbPacket.Companion.toString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream

@RunWith(Parameterized::class)
class LcbPacketParserTest(
    private val label: String,
    private val usePossibleStartsInData: Boolean,
) {
    // we should test both with and without a possible start in the data
    private val verMajor: Byte = if (usePossibleStartsInData) LcbPacket.START_BYTE_RESPONSE else 0x03
    // interestingly if we make minor zero, crc shifts enough to get BadCommandThenGood to recover in time, so leaving at 2 for worse case
    private val verMinor: Byte = 0x02
    private val GET_VERSION_RESPONSE_LENGTH = 7

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}") // final name of the test will be the name of the test method, followed by the name in brackets
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Include possible starts", true),
                arrayOf("Exclude possible starts", false),
            )
        }
    }

    @Test fun testValidResponseSimple() {
        packetResponseTest("Simple") {
            insertPacketResponse(it)
        }
    }

    @Test fun testValidResponseLeadingStartBytes() {
        packetResponseTest("Leading start bytes") {
            // feed a bunch of fake start bytes
            insertNBytes(LcbPacket.START_BYTE_RESPONSE, 300, it)
            // then the response
            insertPacketResponse(it)
        }
    }

    @Test fun testBigBadDataLength() {
        packetResponseTest("Bad data length (big)", false) {
            insertPacketResponse(it, overrideDataLength = 200.toByte())
        }
    }
    @Test fun testBigBadDataLengthResetThenGood() {
        // keep same parser so we can test resetting between passed
        val parser = LcbParser()
        // If the data length is valid and beyond our actual data, we have to rely on receive timeout to reset for next attempt
        packetResponseTest("Bad data length then good (big full reset)", false, parser) {
            insertPacketResponse(it, overrideDataLength = 200.toByte())
        }
        // ASSUMED RECEIVE TIMEOUT RESET
        parser.reset()
        packetResponseTest("Bad data length then good (big)", true, parser) {
            insertPacketResponse(it)
        }
    }
    @Test fun testBigBadDataLengthRewindThenGood() {
        // keep same parser so we can test resetting between attempts
        val parser = LcbParser()
        // If the data length is valid and beyond our actual data, we have to rely on receive timeout to reset for next attempt
        packetResponseTest("Bad data length then good (big rewind)", false, parser) {
            insertPacketResponse(it, overrideDataLength = 200.toByte())
            insertPacketResponse(it)
        }
        // ASSUMED RECEIVE TIMEOUT REWIND
        rewindTestCallback(
            parser = parser,
        ) { result, packet ->
            assertTrue("Should find the packet hidden by bad data length", confirmPacketResponse(packet))
        }
    }
    @Test fun testBigBadDataLengthRewindThenGoodSplit() {
        // keep same parser so we can test resetting between attempts
        val parser = LcbParser()
        // If the data length is valid and beyond our actual data, we have to rely on receive timeout to reset for next attempt
        packetResponseTest("Bad data length then good (big rewind split)", false, parser) {
            insertPacketResponse(it, overrideDataLength = 200.toByte())
            insertPacketResponse(it, cutOffEnd = 3) // save last 3 for after resume
        }
        // ASSUMED RECEIVE TIMEOUT REWIND
        rewindTestCallback(
            parser = parser,
        ) { result, packet ->
            assertTrue("Should not find a complete packet yet, result=$result,packet=$packet", false)
        }
        packetResponseTest("Bad data length then good (big rewind split)", true, parser) {
            insertPacketResponse(it, cutOffStart = (GET_VERSION_RESPONSE_LENGTH - 3)) // save last 3 for after resume
        }
    }
    @Test fun testShortBadDataLengthButThenGood() {
        packetResponseTest("Bad data length then good (short)", true) {
            insertPacketResponse(it, overrideDataLength = 1.toByte()) // actual length is 2
            insertPacketResponse(it)
        }
    }
    @Test fun testLongBadDataLengthButThenGood() {
        packetResponseTest("Bad data length then good (long)", true) {
            insertPacketResponse(it, overrideDataLength = 3.toByte()) // actual length is 2
            insertPacketResponse(it)
        }
    }

    @Test fun testInvalid255DataLength() {
        val callbacks = parseTestCollectPackets(
            streamBuilder = {
                insertPacketResponse(it, overrideDataLength = 255.toByte())
            }
        )
        val invalidLengthDetected = callbacks.find { it.result == LcbParser.ParseResult.InvalidDataLength } != null
        assertTrue("Treat 255 data length as invalid", invalidLengthDetected)
    }


    @Test fun testBadCrc() {
        packetResponseTest("Bad CRC", false) {
            insertPacketResponse(it, overrideCrc = 23)
        }
    }
    @Test fun testBadCrcThenGood() {
        packetResponseTest("Bad CRC then good", true) {
            insertPacketResponse(it, overrideCrc = 23)
            insertPacketResponse(it)
        }
    }

    @Test fun testBadCommand() {
        packetResponseTest("Unknown command", false) {
            insertPacketResponse(it, overrideCommand = 0)
        }
    }
    @Test fun testBadCommandThenGood() {
        packetResponseTest("Unknown command then good", true) {
            insertPacketResponse(it, overrideCommand = 0)
            insertPacketResponse(it)
        }
    }

    @Test fun testMultipleBadThenGood() {
        // interesting, a seed of 3 caused a bad length that made this test fail incomplete, so timeout condition was required
        testMultipleBadThenGood(0)
        testMultipleBadThenGood(3)
    }
    fun testMultipleBadThenGood(seed: Int) {
        println("testMultipleBadThenGood seed=$seed")
        val callbacks = parseTestCollectPacketsWithTimeout {
            insertNBytes(LcbPacket.START_BYTE_RESPONSE, 200, it)
            insertRandomBytes(20, it, seed)
            insertPacketResponse(it, overrideCommand = 0)
            insertPacketResponse(it, overrideCrc = 23)
            insertNBytes(0x00, 5, it)
            insertPacketResponse(it, overrideDataLength = 3.toByte())
            insertPacketResponse(it, cutOffEnd = 3)
            insertPacketResponse(it)
        }
        val successes = callbacks.filter { it.result == LcbParser.ParseResult.Success }
        assertTrue("Trailing valid packet recovered (possibly via timeout), got ${successes.size}",
            successes.any { confirmPacketResponse(it.packet) })
    }

    @Test fun testRandomBytesThenGood() {
        // not sure if we should have a few different seeds?
        val seed = 0
        packetResponseTest("Random bytes then good", true) {
            insertRandomBytes(10..100, it, seed)
            insertPacketResponse(it)
        }
    }

    @Test fun testTwoPacketsAfterRewindRecovery() {
        val callbacks = parseTestCollectPackets {
            // First packet has bad length that covers the first packet, but not at a natural boundary
            insertPacketResponse(it, overrideDataLength = (GET_VERSION_RESPONSE_LENGTH * 2).toByte())
            // Second valid packet — should be found via rewind into the first attempt's tail
            insertPacketResponse(it, actualCommand = LcbCommands.GetLCBVersion)
            // make it so next valid byte is not immediately after
            insertNBytes(0, 1, it)
            // Third valid packet which should be found with in the rewind
            insertPacketResponse(it, actualCommand = LcbCommands.GetLCBStatus)
            insertNBytes(0, 5, it)
        }
        val success = callbacks.filter { it.result == LcbParser.ParseResult.Success }
        assertTrue("Expected 2 valid packets, got ${success.size}", success.size == 2)
        assertTrue("Packet 1 should decode as version response", confirmPacketResponse(success[0].packet, expectedCommand = LcbCommands.GetLCBVersion))
        assertTrue("Packet 2 should decode as version response", confirmPacketResponse(success[1].packet, expectedCommand = LcbCommands.GetLCBStatus))
    }

    @Test fun testRewindFindsPacketAfterFailedReplay() {
        val callbacks = parseTestCollectPackets {
            // Initial bad-length packet. Its body contains an SB byte (because verMajor == SB
            // when usePossibleStartsInData=true), which becomes our first nextPossibleStart.
            insertPacketResponse(it, overrideDataLength = (GET_VERSION_RESPONSE_LENGTH * 2).toByte())

            // Some non-SB bytes that look like a packet header when replayed from the SB-in-data
            // position, but resolve to an error (e.g., InvalidRtnCommand or InvalidCrc) before
            // any further SB shows up. The exact contents depend on what comes after the SB
            // in the bad-length packet's tail and what we add here.
            insertNBytes(0x00, 5, it)

            // The actual valid packet we want recovered. Its SB lives past the bytes the
            // failed replay would have consumed.
            insertPacketResponse(it, actualCommand = LcbCommands.GetLCBStatus)
            insertNBytes(0x00, 2, it)
        }
        val successes = callbacks.filter { it.result == LcbParser.ParseResult.Success }
        assertEquals("Expected to recover the trailing valid packet, found=${successes.size}", 1, successes.size)
        assertTrue("Recovered packet should be GetLCBStatus",
            confirmPacketResponse(successes[0].packet, expectedCommand = LcbCommands.GetLCBStatus))
    }

    /**
     * Test is trying to catch situation where a post-timeout replay ends up incomplete with more data left in the buffer
     */
    @Test fun testRewindTimeoutTriesMultipleCandidatesAfterIncomplete1() {
        // Only meaningful in the SB-in-data variant — we need the bad-length packet's body
        // to contain SBs that become candidates during the failed primary attempt.
        //if (!usePossibleStartsInData) return  // skip in the non-SB variant

        val parser = LcbParser()
        val callbacks = mutableListOf<CallbackData>()

        val buffer = ByteArrayOutputStream()
        // Bad-length packet: declares LEN large enough to swallow the bytes that follow.
        // Body will contain SB bytes (because verMajor=SB) that get captured as candidates.
        insertPacketResponse(buffer, overrideDataLength = (GET_VERSION_RESPONSE_LENGTH * 3).toByte())
        // Insert a chunk of bytes whose interpretation as a frame from the first in-data SB
        // would produce a plausible header (valid command, plausible length) but run out of
        // bytes mid-data — i.e., Incomplete on replay.
        // The trick: make the apparent LEN large enough that the available remaining bytes
        // can't satisfy it.
        val validCmd = LcbCommands.GetLCBVersion.byteValue
        buffer.write(byteArrayOf(0x00, validCmd, 30))  // STAT, CMD, LEN=30 from candidate-1's view
        insertNBytes(0x00, 5, buffer)  // partial "data" — not enough for LEN=30
        // Now the actual valid trailing packet — its SB is candidate-2.
        insertPacketResponse(buffer, actualCommand = LcbCommands.GetLCBStatus)

        val bytes = buffer.toByteArray()
        parser.parse(bytes, bytes.size) { result, packet ->
            callbacks += CallbackData(result, packet)
        }
        println("After parse: midPacket=${parser.isMidPacket()}, pending=${parser.pendingDataBytes()}")

        assertTrue("Test premise: parser should be stalled mid-data", parser.isMidPacket())
        assertTrue("Test premise: should have remaining data pending", parser.pendingDataBytes() > 0)

        // Simulate timeout — this is where the hole would manifest if it exists.
        parser.rewindTimedOutPacket { result, packet ->
            callbacks += CallbackData(result, packet)
        }

        val successes = callbacks.filter { it.result == LcbParser.ParseResult.Success }
        assertTrue("Trailing valid packet should be recovered even when first rewind candidate goes Incomplete, got ${successes.size}",
            successes.any { confirmPacketResponse(it.packet, expectedCommand = LcbCommands.GetLCBStatus) })
    }

    @Test fun testRewindTimeoutTriesMultipleCandidatesAfterIncomplete2() {
        val parser = LcbParser()
        val callbacks = mutableListOf<CallbackData>()

        val buffer = ByteArrayOutputStream()
        // First bad data length to trip original timeout
        insertPacketResponse(buffer, overrideDataLength = (GET_VERSION_RESPONSE_LENGTH * 3).toByte())
        // Second bad data length during rewind
        insertPacketResponse(buffer, actualCommand = LcbCommands.GetLCBVersion, overrideDataLength = 50)
        insertNBytes(0x00, 5, buffer)  // partial "data" — not enough for LEN=50
        // Two valid responses
        insertPacketResponse(buffer, actualCommand = LcbCommands.GetLCBStatus)
        insertNBytes(0x00, 2, buffer)
        insertPacketResponse(buffer, actualCommand = LcbCommands.GetLCBMotorRPM)

        val bytes = buffer.toByteArray()
        parser.parse(bytes, bytes.size) { result, packet ->
            callbacks += CallbackData(result, packet)
        }
        println("After parse: midPacket=${parser.isMidPacket()}, pending=${parser.pendingDataBytes()}")

        assertTrue("Test premise: parser should be stalled mid-data", parser.isMidPacket())
        assertTrue("Test premise: should have remaining data pending", parser.pendingDataBytes() > 0)

        // Simulate timeout — this is where the hole would manifest if it exists.
        parser.rewindTimedOutPacket { result, packet ->
            callbacks += CallbackData(result, packet)
        }

        val successes = callbacks.filter { it.result == LcbParser.ParseResult.Success }
        assertEquals("Expected to recover the trailing valid packet, found=${successes.size}", 2, successes.size)
        assertTrue("Recovered packet should be GetLCBStatus",
            confirmPacketResponse(successes[0].packet, expectedCommand = LcbCommands.GetLCBStatus))
        assertTrue("Recovered packet should be GetLCBMotorRPM",
            confirmPacketResponse(successes[1].packet, expectedCommand = LcbCommands.GetLCBMotorRPM))
    }


    //#region TEST SUPPORT FUNCTIONS ==========

    /** create a packet response containing at least one possible start byte in the body, with options to break command, data size, or crc */
    private fun createPacketResponse(
        actualCommand: LcbCommands = LcbCommands.GetLCBVersion,
        actualData: ByteArray = byteArrayOf(verMajor, verMinor),
        overrideCommand: Byte? = null,
        overrideDataLength: Byte? = null,
        overrideCrc: Byte? = null,
    ): ByteArray {
        val packet = LcbPacket.createResponse(
            command = actualCommand,
            data = actualData
        )
        val packetBytes = packet.buildPacket()
        overrideCommand?.let { packetBytes[2] = it }
        overrideDataLength?.let { packetBytes[3] = it }
        overrideCrc?.let { packetBytes[packetBytes.size - 1] = it }
        return packetBytes
    }
    /** insert a GetVersion response with options to break command, data size, or crc */
    private fun insertPacketResponse(
        buffer: ByteArrayOutputStream,
        actualCommand: LcbCommands = LcbCommands.GetLCBVersion,
        actualData: ByteArray = byteArrayOf(verMajor, verMinor),
        overrideCommand: Byte? = null,
        overrideDataLength: Byte? = null,
        overrideCrc: Byte? = null,
        cutOffStart: Int? = null,
        cutOffEnd: Int? = null,
        cutToSize: Int? = null
    ) {
        var packet = createPacketResponse(actualCommand, actualData, overrideCommand, overrideDataLength, overrideCrc).toMutableList()
        cutOffStart?.let { packet = packet.drop(it).toMutableList() }
        cutOffEnd?.let { packet = packet.dropLast(it).toMutableList() }
        cutToSize?.let { packet = packet.take(it).toMutableList() }
        val bytes = packet.toByteArray()
        println("buffer write packet ${toString(bytes)}")
        buffer.write(bytes)
    }
    /** confirms expected data in GetVersion response */
    private fun confirmPacketResponse(packet: LcbPacket?, expectedCommand: LcbCommands = LcbCommands.GetLCBVersion): Boolean {
        if (packet == null) return false
        if (packet.command != expectedCommand) return false
        // crc check? in theory LcbPacket CRC calc is already compared against LcbParser CRC calc in order to get a packet to check, so unless they are both wrong should be ok
        return (packet.data.getOrNull(0) == verMajor && packet.data.getOrNull(1) == verMinor)
    }

    /** Inserts [count] number of [value] bytes into stream */
    private fun insertNBytes(value: Byte, count: Int, buffer: ByteArrayOutputStream) {
        val byteArray = ByteArray(count) { value }
        println("buffer write repeat $count of [${value.toHexString()}]")
        buffer.write(byteArray)
    }

    /** Inserts [count] number of random bytes into stream */
    private fun insertRandomBytes(count: Int, buffer: ByteArrayOutputStream, seed: Int = 0) {
        val random = kotlin.random.Random(seed)
        val byteArray = ByteArray(count)
        random.nextBytes(byteArray)
        println("buffer write random $count bytes ${toString(byteArray)}")
        buffer.write(byteArray)
    }

    /** Inserts random number of random bytes into stream */
    private fun insertRandomBytes(
        range: IntRange,
        buffer: ByteArrayOutputStream,
        seed: Int = 0
    ) {
        val random = kotlin.random.Random(seed)
        val count = random.nextInt(range.first, range.last + 1)
        val byteArray = ByteArray(count)
        random.nextBytes(byteArray)
        println("buffer write random $count bytes ${toString(byteArray)}")
        buffer.write(byteArray)
    }

    /** short hand function to test various bytes streams parse response with callback */
    private fun rewindTestCallback(
        parser: LcbParser = LcbParser(),
        parseCallback: (result: LcbParser.ParseResult, packet: LcbPacket?)-> Unit,
    ) {
        parser.rewindTimedOutPacket { result, packet ->
            println("\uD83E\uDDE9 rewindTestCallback> result $result, packet=$packet")
            parseCallback(result, packet)
        }
    }

    /** short hand function to test various bytes streams parse response with callback */
    private fun parseTestCallback(
        parser: LcbParser = LcbParser(),
        streamBuilder: (ByteArrayOutputStream) -> Unit,
        parseCallback: (result: LcbParser.ParseResult, packet: LcbPacket?)-> Unit,
    ) {
        val buffer = ByteArrayOutputStream()
        streamBuilder(buffer)
        val bufferBytes = buffer.toByteArray()

        parser.parse(bufferBytes, bufferBytes.size) { result, packet ->
            println("\uD83E\uDDE9 parseTestCallback> result $result, packet=$packet")
            parseCallback(result, packet)
        }
    }

    /** Run parse and collect every parser callback into a list. */
    private fun parseTestCollectPackets(
        parser: LcbParser = LcbParser(),
        streamBuilder: (ByteArrayOutputStream) -> Unit,
    ): List<CallbackData> {
        val collected = mutableListOf<CallbackData>()
        parseTestCallback(parser = parser, streamBuilder = streamBuilder) { result, packet ->
            collected += CallbackData(result, packet)
        }
        return collected
    }
    /** Run parse and collect every parser callback into a list. It will run a rewind timeout if parser was stalled. */
    private fun parseTestCollectPacketsWithTimeout(
        parser: LcbParser = LcbParser(),
        streamBuilder: (ByteArrayOutputStream) -> Unit,
    ): List<CallbackData> {
        val collected = mutableListOf<CallbackData>()
        parseTestCallback(parser = parser, streamBuilder = streamBuilder) { result, packet ->
            collected += CallbackData(result, packet)
        }
        if (parser.isMidPacket()) {
            println("Parser was mid packet, simulate rewind timeout. Pending data bytes ${parser.pendingDataBytes()}")
            parser.rewindTimedOutPacket { result, packet ->
                collected += CallbackData(result, packet)
            }
        }
        return collected
    }
    /** could use Pair, but this is clearer and only takes up space in tests */
    private class CallbackData(
        val result: LcbParser.ParseResult,
        val packet: LcbPacket?
    )

    /** short hand function to test various bytes streams parse response returning the packet or null */
    private fun parseTestReturnPacket(
        parser: LcbParser = LcbParser(),
        streamBuilder: (ByteArrayOutputStream) -> Unit,
    ): LcbPacket? {
        var parsedPacket: LcbPacket? = null
        parseTestCallback(parser = parser, streamBuilder = streamBuilder) { result, packet ->
            println("\uD83E\uDDE9 parseTestReturnPacket> result $result, packet=$packet")
            parsedPacket = packet
        }
        return parsedPacket
    }

    /** short hand function to test various bytes streams containing a packet response */
    private fun packetResponseTest(
        label: String,
        expectSuccess: Boolean = true,
        parser: LcbParser = LcbParser(),
        streamBuilder: (ByteArrayOutputStream) -> Unit,
    ) {
        val parsedPacket = parseTestReturnPacket(parser, streamBuilder)
        if (expectSuccess) {
            assertTrue("$label: response found", parsedPacket != null)
            assertTrue("$label: response decoded", confirmPacketResponse(parsedPacket))
        } else {
            assertFalse("$label: response found", parsedPacket != null)
            assertFalse("$label: response decoded", confirmPacketResponse(parsedPacket))
        }
    }
    //#endregion TEST SUPPORT FUNCTIONS

} // END CLASS

