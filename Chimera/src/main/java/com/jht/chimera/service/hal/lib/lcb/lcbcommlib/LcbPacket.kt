package com.jht.chimera.service.hal.lib.lcb.lcbcommlib

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 *  LCB Packet - Default constructor is a command packet, see static helper for creating response packet
 *  - Start byte: 0 Command, 1 Response
 *  - Address byte for command / Status byte for response
 *  - Command byte
 *  - Data Length byte
 *  - Data N bytes
 *  - CRC byte
 */
open class LcbPacket(
    initialCommand: LcbCommands = LcbCommands.GetLCBStatus,
) {
    companion object {
        // packet constants
        protected const val PACKET_HEADER_SIZE = 4 // start + address/status + command + dataLength
        protected const val PACKET_CRC_SIZE = 1
        // technically I don't think we have packets the use the full limit of data size
        // should not allow 0xFF due to chance of occurring naturally
        // we could have an expected max per command if we want to be really rigid
        protected const val PACKET_MAX_DATA_LENGTH = 250
        protected const val PACKET_MIN_SIZE = PACKET_HEADER_SIZE + PACKET_CRC_SIZE
        protected const val PACKET_MAX_SIZE = PACKET_MIN_SIZE + PACKET_MAX_DATA_LENGTH
        protected const val PACKET_DEFAULT_ADDRESS = 0xFF.toByte() //Address 0xFF used for only one receiver condition
        protected const val PACKET_IR_ADDRESS = 0x26.toByte() //Address 0x26 used for IR address

        // meta data priority
        const val PRIORITY_HIGHEST = 1
        const val PRIORITY_HIGH = 2
        const val PRIORITY_NORMAL = 3
        const val PRIORITY_LOW = 4
        const val PRIORITY_LOWEST = 5

        // meta data special operations
        const val SPECIAL_NONE = 0
        const val SPECIAL_400MS_PULSE = 1 // ERP wake up pulse must be issued before packet is sent

        // constants that might be useful elsewhere?
        const val START_BYTE_COMMAND = 0x00.toByte()
        const val START_BYTE_RESPONSE = 0x01.toByte()

        /** creates a response packet (default construction is a command packet) */
        fun createResponse(
            status: Byte = 0,
            command: LcbCommands = LcbCommands.GetLCBStatus,
            data: ByteArray = emptyData
        ) = LcbPacket().apply {
            isCommand = false
            this.command = command
            addressOrStatus = status
            this.data = data
        }
        private val sequenceCounter = java.util.concurrent.atomic.AtomicLong(0)
        /** Using single instance of empty array to avoid re-allocations, empty array is inherently immutable */
        val emptyData = byteArrayOf()
        /** formats byte array into a string of hexadecimal inside of square brackets for human readable uses */
        fun toString(byteArray: ByteArray) = "[" + byteArray.toHexString(
            HexFormat {
                bytes { byteSeparator = " " }
                upperCase = true
            }) + "]"
    }

    //meta data
    /** packet queuing priority, when set also sets [sequenceId], sorting is based on priority first then sequence (setting this also assigns [sequenceId]) */
    var priority: Int = PRIORITY_NORMAL
        set(value) {
            field = value
            // old code establishes order at moment of priority assignment
            // (feels a little awkward, maybe we can review if other code is reworked)
            sequenceId = sequenceCounter.incrementAndGet()
        }
    // sequenceId keeps track of ordering, considered using an actual timestamp, but possibility exists to get same stamp if going fast enough?
    var sequenceId: Long = 0
        private set
    var specialOperation = SPECIAL_NONE

    // packet structure (default to command packet)
    var isCommand = true
    var addressOrStatus: Byte = PACKET_DEFAULT_ADDRESS
    var command: LcbCommands = initialCommand
    var data: ByteArray = emptyData

    // Tx/Rx specific wrappers
    fun getRxLcbStatus() = addressOrStatus
    fun setRxLcbStatus(status: Byte) { addressOrStatus = status }
    fun getTxAddress() = addressOrStatus
    fun setTxAddress(address: Byte) { addressOrStatus = address }

    override fun toString(): String {
        return toString(buildPacket())
    }

    /** Deep copy of packet without meta-data */
    fun deepCopyNoMeta(): LcbPacket {
        return LcbPacket().also {
            it.isCommand = this.isCommand
            it.addressOrStatus = this.addressOrStatus
            it.command = this.command
            if (this.data.isNotEmpty()) it.data = this.data.copyOf() // deep copy
        }
    }

    /** Shallow copy of packet without meta-data */
    fun shallowCopyNoMeta(): LcbPacket {
        return LcbPacket().also {
            it.isCommand = this.isCommand
            it.addressOrStatus = this.addressOrStatus
            it.command = this.command
            it.data = this.data
        }
    }

    /** Builds byte array representation of the packet */
    fun buildPacket(): ByteArray {
        val byteArray = ByteArray(PACKET_MIN_SIZE + data.size)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        byteBuffer.order(ByteOrder.BIG_ENDIAN)
        //Start byte
        byteBuffer.put(if (isCommand) START_BYTE_COMMAND else START_BYTE_RESPONSE)
        //For commands, this byte is address, for responses, this byte is Status
        byteBuffer.put(addressOrStatus)
        //Message ID same command
        byteBuffer.put(command.Value.toByte())
        // data length
        byteBuffer.put(data.size.toByte())
        // data
        byteBuffer.put(data)
        // CRC
        byteBuffer.put(LcbCrc8.doLcbCrc8(byteArray, byteArray.size - 1))
        return byteArray
    }


    /** priority queue returns smaller number first
     *    returns a positive integer if the first packet has lower priority (higher #) or occurred after
     *    returns a negative integer if the first packet has higher priority (lower #) or occurred first
     */
    class LcbPacketSort : Comparator<LcbPacket> {
        override fun compare(p1: LcbPacket, p2: LcbPacket): Int {
            return compareValuesBy(p1, p2,
                { it.priority },
                { it.sequenceId }
            )
        }
    }
}
