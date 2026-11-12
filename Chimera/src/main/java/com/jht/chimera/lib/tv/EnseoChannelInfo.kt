package com.jht.chimera.lib.tv

import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlSerializer

/** Enseo IPTV channel model extending [ChannelInfo] with source-based URL construction. */
open class EnseoChannelInfo : ChannelInfo {

    constructor(channelType: ChannelType, json: JSONObject) : super(channelType, json)

    constructor(channelType: ChannelType, logoFolder: String, parser: XmlPullParser, index: Int)
        : super(channelType, logoFolder, parser, index)

    constructor(channelType: ChannelType, number: String, name: String, tag: Any?)
        : super(channelType, number, name, tag)

    constructor(info: EnseoChannelInfo) : super(info)

    override fun readXmlAttributes(parser: XmlPullParser) {
        var source = parser.getAttributeValue(null, "source")?.lowercase()
        val port = parser.getAttributeValue(null, "port")
        val ip = parser.getAttributeValue(null, "ip")
        enabled("true".equals(parser.getAttributeValue(null, "enabled"), ignoreCase = true))

        if (source != null && ip != null && port != null) {
            url("$source://$ip:$port")
        }
    }

    override fun fromXML(logoFolder: String, parser: XmlPullParser, index: Int) {
        fromXML(parser)
    }

    override fun writeXmlAttributes(serializer: XmlSerializer) {
        val decode = URLDecode(url())
        runLogging { serializer.attribute("", "ip", decode.address()) }
        runLogging { serializer.attribute("", "source", decode.protocol().uppercase()) }
        runLogging { serializer.attribute("", "port", decode.port()) }
        runLogging { serializer.attribute("", "enabled", if (enabled()) "true" else "false") }
        runLogging { serializer.attribute("", "default", "false") }
    }
}
