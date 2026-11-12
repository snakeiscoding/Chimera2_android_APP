package com.jht.chimera.lib.tv.tuner.lg.channel

import com.jht.serialport.peripheral.tuner.channel.BaseChannel
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

/**
 * Sealed class representing TV channels for LG STB devices.
 *
 * Extends [BaseChannel] to inherit UUID, name, number, and enabled fields with consistent
 * JSON/XML serialization. Subclasses add LG-specific fields via the template method hooks.
 */
sealed class LgChannel : BaseChannel() {

//##################################################################################################
// Common Properties
//##################################################################################################

    /**
     * The television channel type (ex: 'IP Class 1', 'IP Class 2', etc.).
     *
     * This gives each LgChannel subtype (IP.Class1, RF.Class1, etc.) a string name that can be used
     * to identify it to users or during serialization/deserialization.
     */
    abstract val channelType: ChannelType

    /** The broadcast type of this television channel */
    abstract val broadcastType: LgBroadcastType

    /** Returns the channel in a string representation */
    override fun channel(): String = number()

    /** Returns the display name for this channel */
    override fun displayName(noChannelInfo: String?): String = "${number()} - ${name()}"

    /** Returns whether the channel can be deleted */
    override fun canDelete(): Boolean = true

    /** Returns whether the channel can be edited */
    override fun canEdit(): Boolean = true

    /** Returns whether the channel can be disabled */
    override fun canDisable(): Boolean = true

    // LG channel lists serialize channel number as "number". BaseChannel defaults to "chnumber"
    // (Enseo legacy), so we override here.
    override val xmlNumberKey: String = "number"

//##################################################################################################
// Serialization
//##################################################################################################

    /**
     * Writes LG-common fields (channelType, broadcastType) into [json], then delegates to
     * [writeLgJsonFields] for subtype-specific fields.
     *
     * Called by [BaseChannel.toJSON] after uuid, number, name, and enabled have been written.
     */
    override fun writeJsonFields(json: JSONObject) {
        json.put("channelType", channelType.identifiers.first())
        json.put("broadcastType", broadcastType.name)
        writeLgJsonFields(json)
    }

    /**
     * Writes the subtype-specific fields of this channel into the provided [JSONObject].
     *
     * Called by [writeJsonFields] after channelType and broadcastType have been written.
     */
    protected abstract fun writeLgJsonFields(json: JSONObject)

    /**
     * Writes LG-common attributes (channelType, broadcastType) into the open XML element, then
     * delegates to [writeLgXmlAttributes] for subtype-specific attributes.
     *
     * Called by [BaseChannel.toXml] after uuid, name, number, and enabled have been written.
     */
    override fun writeXmlAttributes(serializer: XmlSerializer) {
        serializer.attribute("", "channelType", channelType.identifiers.first())
        serializer.attribute("", "broadcastType", broadcastType.name)
        writeLgXmlAttributes(serializer)
    }

    /**
     * Writes the subtype-specific XML attributes of this channel into the provided [XmlSerializer].
     *
     * Called by [writeXmlAttributes] after channelType and broadcastType have been written.
     */
    protected abstract fun writeLgXmlAttributes(serializer: XmlSerializer)

    /**
     * Handles backward compatibility for the channel number XML attribute, then delegates to
     * [readLgXmlAttributes] for subtype-specific fields.
     *
     * LG channel lists previously used "chnumber" for the channel number attribute before
     * migrating to "number". If [number] is blank after [BaseChannel.fromXML] reads "number",
     * this falls back to "chnumber" so older channel lists continue to display correctly.
     */
    final override fun readXmlAttributes(parser: XmlPullParser) {
        if (number().isBlank()) {
            val legacy = parser.getAttributeValue(null, "chnumber") ?: ""
            if (legacy.isNotBlank()) number(legacy)
        }
        readLgXmlAttributes(parser)
    }

    /**
     * Reads the subtype-specific XML attributes of this channel from the provided [XmlPullParser].
     *
     * Called by [BaseChannel.fromXML] after uuid, name, number, and enabled have been restored.
     * Called by [readXmlAttributes] after the "chnumber" backward-compat fallback has been applied.
     * Each subclass reads back its broadcastType and any subtype-specific attributes.
     */
    protected abstract fun readLgXmlAttributes(parser: XmlPullParser)

//##################################################################################################
// Common Methods
//##################################################################################################

    /** Creates a copy of this channel, preserving name, number, and enabled state. */
    fun createCopy(): LgChannel = when (this) {
        is IpChannel.Class1 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is IpChannel.Class2 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class1 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class2 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class3 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class4 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class5 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
        is RfChannel.Class6 -> copy().also { it.name(name()); it.number(number()); it.enabled(enabled()) }
    }

//##################################################################################################
// IP Television Channels
//##################################################################################################

    /** IP television channels supported by LG devices */
    sealed class IpChannel : LgChannel() {
        /** The IP broadcast type of this television channel */
        abstract override var broadcastType: LgBroadcastType.IpBroadcast


        /**
         * IP Class 1 Channel
         *
         * @param broadcastType the IP broadcast type of this channel
         * @param logicalNumber the logical number of this channel
         */
        data class Class1(
            override var broadcastType: LgBroadcastType.IpBroadcast = LgBroadcastType.IpBroadcast.UNKNOWN,

            /** The logical number of this television channel */
            var logicalNumber: String = ""
        ) : IpChannel() {
            override val channelType = ChannelType.IP_CLASS_1

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("logicalNumber", logicalNumber)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "logicalNumber", logicalNumber)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.IpBroadcast.fromName(parser.getAttributeValue(null, "broadcastType"))
                logicalNumber = parser.getAttributeValue(null, "logicalNumber") ?: ""
            }
        }


        /**
         * IP Class 2 Channel
         *
         * @param broadcastType the IP broadcast type of this channel
         * @param ip the IP address of this channel
         * @param port the port of this channel
         */
        data class Class2(
            override var broadcastType: LgBroadcastType.IpBroadcast = LgBroadcastType.IpBroadcast.UNKNOWN,

            /** The IP address of this television channel */
            var ip: String = "",
            /** The port of this television channel */
            var port: String = ""
        ) : IpChannel() {
            override val channelType = ChannelType.IP_CLASS_2

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("ip", ip)
                json.put("port", port)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                if (ip.isNotEmpty()) serializer.attribute("", "ip", ip)
                if (port.isNotEmpty()) serializer.attribute("", "port", port)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.IpBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "source")
                )
                ip = parser.getAttributeValue(null, "ip") ?: ""
                port = parser.getAttributeValue(null, "port") ?: ""
            }
        }
    }

//##################################################################################################
// RF Television Channels
//##################################################################################################

    /** RF television channels supported by LG devices */
    sealed class RfChannel : LgChannel() {
        /** The RF broadcast type of this television channel */
        abstract override var broadcastType: LgBroadcastType.RfBroadcast


        /**
         * RF Class 1 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param logicalNumber the logical number of this channel
         */
        data class Class1(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The logical number of this television channel */
            var logicalNumber: String = ""
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_1

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("logicalNumber", logicalNumber)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "logicalNumber", logicalNumber)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType") // legacy attribute name
                )
                logicalNumber = parser.getAttributeValue(null, "logicalNumber") ?: ""
            }
        }


        /**
         * RF Class 2 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param frequency the frequency of this channel
         * @param programNumber the program number of this channel
         */
        data class Class2(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The frequency of this television channel */
            var frequency: String = "",
            /** The program number of this television channel */
            var programNumber: String = ""
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_2

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("frequency", frequency)
                json.put("programNumber", programNumber)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "frequency", frequency)
                serializer.attribute("", "programNumber", programNumber)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType")
                )
                frequency = parser.getAttributeValue(null, "frequency") ?: ""
                programNumber = parser.getAttributeValue(null, "programNumber") ?: ""
            }
        }


        /**
         * RF Class 3 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param major the major number of this channel
         * @param minor the minor number of this channel
         */
        data class Class3(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The major number of this television channel */
            var major: String = "",
            /** The minor number of this television channel */
            var minor: String = ""
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_3

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("major", major)
                json.put("minor", minor)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "major", major)
                serializer.attribute("", "minor", minor)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType")
                )
                major = parser.getAttributeValue(null, "major") ?: ""
                minor = parser.getAttributeValue(null, "minor") ?: ""
            }
        }


        /**
         * RF Class 4 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param frequency the frequency of this channel
         * @param programNumber the program number of this channel
         * @param satelliteId the satellite ID of this channel
         * @param polarization the polarization of this channel
         */
        data class Class4(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The frequency of this television channel */
            var frequency: String = "",
            /** The program number of this television channel */
            var programNumber: String = "",
            /** The satellite ID of this television channel */
            var satelliteId: String = "",
            /** The polarization of this television channel */
            var polarization: LgPolarization = LgPolarization.UNKNOWN
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_4

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("frequency", frequency)
                json.put("programNumber", programNumber)
                json.put("satelliteId", satelliteId)
                json.put("polarization", polarization.name)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "frequency", frequency)
                serializer.attribute("", "programNumber", programNumber)
                serializer.attribute("", "satelliteId", satelliteId)
                serializer.attribute("", "polarization", polarization.name)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType")
                )
                frequency = parser.getAttributeValue(null, "frequency") ?: ""
                programNumber = parser.getAttributeValue(null, "programNumber") ?: ""
                satelliteId = parser.getAttributeValue(null, "satelliteId") ?: ""
                polarization = LgPolarization.fromName(parser.getAttributeValue(null, "polarization"))
            }
        }


        /**
         * RF Class 5 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param frequency the frequency of this channel
         * @param programNumber the program number of this channel
         * @param plpId the plpId of this channel
         */
        data class Class5(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The frequency of this television channel */
            var frequency: String = "",
            /** The program number of this television channel */
            var programNumber: String = "",
            /** The PLP ID of this television channel */
            var plpId: String = ""
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_5

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("frequency", frequency)
                json.put("programNumber", programNumber)
                json.put("plpId", plpId)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "frequency", frequency)
                serializer.attribute("", "programNumber", programNumber)
                serializer.attribute("", "plpId", plpId)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType")
                )
                frequency = parser.getAttributeValue(null, "frequency") ?: ""
                programNumber = parser.getAttributeValue(null, "programNumber") ?: ""
                plpId = parser.getAttributeValue(null, "plpId") ?: ""
            }
        }


        /**
         * RF Class 6 Channel
         *
         * @param broadcastType the RF broadcast type of this channel
         * @param major the major number of this channel
         * @param minor the minor number of this channel
         * @param plpId the PLP ID of this channel
         */
        data class Class6(
            override var broadcastType: LgBroadcastType.RfBroadcast = LgBroadcastType.RfBroadcast.UNKNOWN,

            /** The major number of this television channel */
            var major: String = "",
            /** The minor number of this television channel */
            var minor: String = "",
            /** The PLP ID of this television channel */
            var plpId: String = ""
        ) : RfChannel() {
            override val channelType = ChannelType.RF_CLASS_6

            override fun writeLgJsonFields(json: JSONObject) {
                json.put("major", major)
                json.put("minor", minor)
                json.put("plpId", plpId)
            }

            override fun writeLgXmlAttributes(serializer: XmlSerializer) {
                serializer.attribute("", "major", major)
                serializer.attribute("", "minor", minor)
                serializer.attribute("", "plpId", plpId)
            }

            override fun readLgXmlAttributes(parser: XmlPullParser) {
                broadcastType = LgBroadcastType.RfBroadcast.fromName(
                    parser.getAttributeValue(null, "broadcastType") ?: parser.getAttributeValue(null, "rfBroadcastType")
                )
                major = parser.getAttributeValue(null, "major") ?: ""
                minor = parser.getAttributeValue(null, "minor") ?: ""
                plpId = parser.getAttributeValue(null, "plpId") ?: ""
            }
        }
    }

//##################################################################################################
// Channel Type
//##################################################################################################

    /**
     * Identifies the type of [LgChannel].
     *
     * This is primarily used to identify [LgChannel] types prior to deserializing them. It also
     * is used to allow mapping of supported [ChannelType] per tuner variant, etc.
     *
     * @property displayName human-readable name shown in UI (e.g. "IP Class 1", "RF Class 2")
     */
    enum class ChannelType(
        /** The display name of a channel type (displayed to users) */
        val displayName: String,

        /**
         * The identifiers for a channel type (used for serialization & deserialization).
         *
         * The **first** entry is the canonical identifier used for serialization. Any additional
         * entries are legacy aliases accepted during deserialization only (for backwards
         * compatibility with EnseoChannel XML files).
         */
        val identifiers: List<String>
    ) {
        IP_CLASS_1("IP - Logical", listOf("IP1")),
        IP_CLASS_2("IP - Address/Port", listOf("IP2", "UDP", "RTP")), // backward compatible with EnseoUdpChannel & EnseoRtpChannel
        RF_CLASS_1("RF - Logical", listOf("RF1")),
        RF_CLASS_2("RF - Frequency", listOf("RF2")),
        RF_CLASS_3("RF - Major/Minor", listOf("RF3")),
        RF_CLASS_4("RF - Satellite", listOf("RF4")),
        RF_CLASS_5("RF - Frequency (T2)", listOf("RF5")),
        RF_CLASS_6("RF - Major/Minor (T2)", listOf("RF6"))
    }

//##################################################################################################
// Deserialization
//##################################################################################################

    companion object {
        /**
         * Creates an [LgChannel] from the XML at the current position of [parser].
         *
         * Supports both the native LgChannel format (`channelType` attribute) and the legacy
         * Enseo format (`source` attribute) by resolving the type via [ChannelType.identifiers].
         * Delegates all field population to [BaseChannel.fromXML].
         */
        /** Creates a default [LgChannel] of the given [channelType] with all fields at their defaults. */
        @JvmStatic
        fun createDefault(channelType: ChannelType): LgChannel = when (channelType) {
            ChannelType.IP_CLASS_1 -> IpChannel.Class1()
            ChannelType.IP_CLASS_2 -> IpChannel.Class2()
            ChannelType.RF_CLASS_1 -> RfChannel.Class1()
            ChannelType.RF_CLASS_2 -> RfChannel.Class2()
            ChannelType.RF_CLASS_3 -> RfChannel.Class3()
            ChannelType.RF_CLASS_4 -> RfChannel.Class4()
            ChannelType.RF_CLASS_5 -> RfChannel.Class5()
            ChannelType.RF_CLASS_6 -> RfChannel.Class6()
        }

        @JvmStatic
        fun fromXml(parser: XmlPullParser): LgChannel {
            val channelType = (parser.getAttributeValue(null, "channelType")
                ?: parser.getAttributeValue(null, "source")) // backward compatibility with EnseoChannel
                ?.let { value -> ChannelType.entries.firstOrNull { value in it.identifiers } }
                ?: ChannelType.IP_CLASS_2

            return when (channelType) {
                ChannelType.IP_CLASS_1 -> IpChannel.Class1()
                ChannelType.IP_CLASS_2 -> IpChannel.Class2()
                ChannelType.RF_CLASS_1 -> RfChannel.Class1()
                ChannelType.RF_CLASS_2 -> RfChannel.Class2()
                ChannelType.RF_CLASS_3 -> RfChannel.Class3()
                ChannelType.RF_CLASS_4 -> RfChannel.Class4()
                ChannelType.RF_CLASS_5 -> RfChannel.Class5()
                ChannelType.RF_CLASS_6 -> RfChannel.Class6()
            }.also { it.fromXML(parser) }
        }
    }
}
