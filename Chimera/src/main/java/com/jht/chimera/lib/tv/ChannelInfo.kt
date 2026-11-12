package com.jht.chimera.lib.tv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jht.serialport.peripheral.tuner.channel.BaseChannel
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer
import java.io.File

/** IPTV / legacy channel model. */
open class ChannelInfo : BaseChannel {

    override val jsonEnabledKey: String = ""
    override val xmlEnabledKey: String = ""

    /** Decodes a streaming URL into protocol, address, port, and path components. */
    class URLDecode(url: String?) {
        private var _protocol: String = "rtsp"
        private var _address: String = ""
        private var _port: String = ""
        private var _path: String = ""

        init {
            if (url != null) {
                val parts = url.split("://", limit = 2)
                if (parts.size == 2) {
                    _protocol = parts[0].uppercase()
                    val remainder = parts[1].removePrefix("@")
                    if (_protocol.equals("UDP", ignoreCase = true)) {
                        val udpParts = remainder.split(":", limit = 2)
                        _address = udpParts[0]
                        if (udpParts.size == 2) _port = udpParts[1]
                    } else {
                        if (!remainder.contains("/")) {
                            _address = remainder
                        } else {
                            _address = remainder.substringBefore("/")
                            _path = remainder.substringAfter("/")
                        }
                        if (_address.contains(":")) {
                            if (_address == ":") {
                                _address = ""
                                _port = ""
                            } else {
                                val addressParts = _address.split(":", limit = 2)
                                if (addressParts.size == 2) {
                                    _address = addressParts[0]
                                    _port = addressParts[1]
                                }
                            }
                        }
                    }
                }
            }
        }

        fun protocol(): String = _protocol
        fun address(): String = _address
        fun port(): String = _port
        fun path(): String = _path
    }

    enum class ChannelType {
        REGULAR_TV,
        REGULAR_TV_USER_ENTERED,
        UNENCRYPTED_IPTV,
        ENSEO_PRO_IPTV
    }

    private var channelType: ChannelType? = null
    private var shortName: String = ""
    private var description: String = ""
    private var url: String = ""
    private var logo: Bitmap? = null
    private var logoPath: String = ""
    private var sourceType: String = ""
    private var majorNumber: String = ""
    private var minorNumber: String = ""
    private var serviceType: Int = 0
    private var isFavorite: Int = 0
    private var isSkip: Int = 0
    private var tag: Any? = null
    private var _enabled: Boolean = false
    private var deinterlaced: Boolean = false
    private var numberReadOnly: Boolean = false

    class ChannelInfoSort : Comparator<ChannelInfo> {
        override fun compare(channel1: ChannelInfo, channel2: ChannelInfo): Int {
            return try {
                val number1 = channel1.number().replace("-", ".")
                val number2 = channel2.number().replace("-", ".")
                val parts1 = number1.split('.')
                val parts2 = number2.split('.')
                if (parts1.isNotEmpty() && parts2.isNotEmpty()) {
                    val major1 = parts1[0].toInt()
                    val major2 = parts2[0].toInt()
                    if (major1 == major2) {
                        when {
                            parts1.size > 1 && parts2.size > 1 -> parts1[1].toInt() - parts2[1].toInt()
                            parts1.size > 1 -> 1
                            parts2.size > 1 -> -1
                            else -> 0
                        }
                    } else major1 - major2
                } else channel1.number().compareTo(channel2.number())
            } catch (_: Exception) {
                channel1.number().compareTo(channel2.number())
            }
        }
    }

    constructor(channelType: ChannelType, json: JSONObject) : super() {
        this.channelType = channelType
        fromJSON(json)
    }

    constructor(channelType: ChannelType, logoFolder: String, parser: XmlPullParser, index: Int) : super() {
        this.channelType = channelType
        fromXML(logoFolder, parser, index)
    }

    constructor(channelType: ChannelType, number: String, name: String, tag: Any?) : super(number, name) {
        this.channelType = channelType
        shortName = name
        this.tag = tag
    }

    constructor(info: ChannelInfo) : super(info) {
        copyFrom(info)
    }

    open fun copyFrom(info: ChannelInfo) {
        super.name(info.name())
        super.number(info.number())
        channelType = info.channelType()
        shortName = info.shortName
        description = info.description()
        url = info.url()
        logo = info.logo()
        logoPath = info.logoPath()
        sourceType = info.sourceType()
        majorNumber = info.majorNumber()
        minorNumber = info.minorNumber()
        serviceType = info.serviceType()
        isFavorite = info.isFavorite()
        isSkip = info.isSkip()
        tag = info.tag()
        enabled(info.enabled())
        deinterlaced = info.deinterlaced
        numberReadOnly = info.numberReadOnly
    }

    override fun name(name: String): ChannelInfo { super.name(name); return this }
    override fun number(number: String): ChannelInfo { super.number(number); return this }

    override fun enabled(): Boolean = _enabled
    override fun enabled(enabled: Boolean): ChannelInfo { _enabled = enabled; return this }

    open fun shortName(): String = shortName
    open fun shortName(shortName: String): ChannelInfo { this.shortName = shortName; return this }

    open fun description(): String = description
    open fun description(description: String): ChannelInfo { this.description = description; return this }

    open fun url(): String = url
    open fun url(url: String): ChannelInfo { this.url = url; return this }

    open fun logo(): Bitmap? = logo
    open fun logo(logo: Bitmap?): ChannelInfo { this.logo = logo; return this }

    open fun logoPath(): String = logoPath
    open fun logoPath(logoPath: String): ChannelInfo { this.logoPath = logoPath; return this }

    open fun sourceType(): String = sourceType
    open fun sourceType(sourceType: String): ChannelInfo { this.sourceType = sourceType; return this }

    open fun majorNumber(): String = majorNumber
    open fun majorNumber(majorNumber: String): ChannelInfo { this.majorNumber = majorNumber; return this }

    open fun minorNumber(): String = minorNumber
    open fun minorNumber(minorNumber: String): ChannelInfo { this.minorNumber = minorNumber; return this }

    open fun serviceType(): Int = serviceType
    open fun serviceType(serviceType: Int): ChannelInfo { this.serviceType = serviceType; return this }

    open fun isFavorite(): Int = isFavorite
    open fun isFavorite(isFavorite: Int): ChannelInfo { this.isFavorite = isFavorite; return this }

    open fun isSkip(): Int = isSkip
    open fun isSkip(isSkip: Int): ChannelInfo { this.isSkip = isSkip; return this }

    open fun deinterlaced(): Boolean = deinterlaced
    open fun deinterlaced(deinterlaced: Boolean): ChannelInfo { this.deinterlaced = deinterlaced; return this }

    open fun tag(): Any? = tag
    open fun tag(tag: Any?): ChannelInfo { this.tag = tag; return this }

    open fun numberReadOnly(): Boolean = numberReadOnly
    open fun numberReadOnly(numberReadOnly: Boolean): ChannelInfo { this.numberReadOnly = numberReadOnly; return this }

    override fun channel(): String = number()

    override fun displayName(noChannelInfo: String?): String =
        if (name().isEmpty()) noChannelInfo ?: "" else name()

    override fun canDelete(): Boolean = true
    override fun canEdit(): Boolean = true
    override fun canDisable(): Boolean = true

    open fun channelType(): ChannelType? = channelType

    override fun writeXmlAttributes(serializer: XmlSerializer) {
        runLogging { serializer.attribute("", "ip", url) }
        runLogging { serializer.attribute("", "description", description) }
        runLogging { serializer.attribute("", "icon", logoPath) }
        runLogging { serializer.attribute("", "enabled", if (enabled()) "true" else "false") }
        runLogging { serializer.attribute("", "deinterlace", if (deinterlaced) "true" else "false") }
    }

    override fun writeJsonFields(json: JSONObject) {
        runLogging { json.put("sourceType", sourceType()) }
        runLogging { json.put("majorNumber", majorNumber()) }
        runLogging { json.put("minorNumber", minorNumber()) }
        runLogging { json.put("serviceType", serviceType()) }
        runLogging { json.put("isFavorite", isFavorite()) }
        runLogging { json.put("isSkip", isSkip()) }
    }

    override fun readJsonFields(json: JSONObject) {
        shortName = name()
        sourceType = json.optString("sourceType", "")
        majorNumber = json.optString("majorNumber", "")
        minorNumber = json.optString("minorNumber", "")
        serviceType = json.optInt("serviceType", 0)
        isFavorite = json.optInt("isFavorite", 0)
        isSkip = json.optInt("isSkip", 0)
    }

    override fun readXmlAttributes(parser: XmlPullParser) {
        shortName = name()
        url = parser.getAttributeValue(null, "ip") ?: ""
        description = parser.getAttributeValue(null, "description") ?: ""
        logoPath = parser.getAttributeValue(null, "icon") ?: ""
        val enabledValue = parser.getAttributeValue(null, "enabled")
        enabled(enabledValue == null || "true".equals(enabledValue, ignoreCase = true))
        val deinterlaceValue = parser.getAttributeValue(null, "deinterlace")
        deinterlaced = deinterlaceValue == null || "true".equals(deinterlaceValue, ignoreCase = true)
    }

    open fun fromXML(logoFolder: String, parser: XmlPullParser, index: Int) {
        fromXML(parser)
        if (number().isNullOrEmpty() || number() == "0") {
            number("$index")
        }
        // this is file IO, we should be careful about which thread runs this?
        if (logoPath.isNotEmpty()) {
            val file = File(logoFolder, logoPath)
            if (file.exists()) {
                logo = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    override fun toString(): String =
        "$channelType ${number()} ${name()} $shortName $description $url $logoPath $sourceType " +
                "$majorNumber $minorNumber $serviceType $isFavorite $isSkip ${enabled()} $deinterlaced $numberReadOnly"

    override fun equals(other: Any?): Boolean {
        if (other is ChannelInfo) {
            var result = true
            if (channelType != null && other.channelType != null) result = result && channelType == other.channelType
            if (number().isNotEmpty() && other.number().isNotEmpty()) result = result && number() == other.number()
            if (name().isNotEmpty() && other.name().isNotEmpty()) result = result && name() == other.name()
            if (shortName.isNotEmpty() && other.shortName.isNotEmpty()) result = result && shortName == other.shortName
            if (description.isNotEmpty() && other.description.isNotEmpty()) result = result && description == other.description
            if (url.isNotEmpty() && other.url.isNotEmpty()) result = result && url == other.url
            if (logoPath.isNotEmpty() && other.logoPath.isNotEmpty()) result = result && logoPath == other.logoPath
            if (sourceType.isNotEmpty() && other.sourceType.isNotEmpty()) result = result && sourceType == other.sourceType
            if (majorNumber.isNotEmpty() && other.majorNumber.isNotEmpty()) result = result && majorNumber == other.majorNumber
            if (minorNumber.isNotEmpty() && other.minorNumber.isNotEmpty()) result = result && minorNumber == other.minorNumber
            result = result && serviceType == other.serviceType
            result = result && isFavorite == other.isFavorite
            result = result && isSkip == other.isSkip
            result = result && enabled() == other.enabled()
            result = result && deinterlaced == other.deinterlaced
            return result
        }
        return false
    }
}
