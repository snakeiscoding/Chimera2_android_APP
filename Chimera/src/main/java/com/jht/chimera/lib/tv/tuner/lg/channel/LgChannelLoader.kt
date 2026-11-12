package com.jht.chimera.lib.tv.tuner.lg.channel

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter

/**
 * Channel loader for LG 750e tuner channels.
 *
 * Parses and saves [LgChannel] instances. Loading supports both the legacy `<IPTVChannels>`
 * wrapper (from EnseoChannelLoader) and the new `<LgChannels>` wrapper — the base class
 * only looks for `<Channel>` tags so the outer wrapper is irrelevant during parsing.
 *
 * Saving always writes the new `<LgChannels>` wrapper.
 */
class LgChannelLoader : com.jht.serialport.peripheral.tuner.channel.ChannelLoader<LgChannel>(false) {
    override fun parseChannel(parser: XmlPullParser, index: Int): LgChannel {
        return LgChannel.fromXml(parser)
    }

    override fun saveList(file: File, list: MutableList<LgChannel>) {
        val serializer = Xml.newSerializer() ?: return
        val writer = StringWriter()
        try {
            FileOutputStream(file).use { fout ->
                serializer.setOutput(writer)
                serializer.startDocument("UTF-8", true)
                serializer.startTag("", "LgChannels")
                serializer.startTag("", "Channels")
                for (channel in list) {
                    channel.toXml(serializer)
                }
                serializer.endTag("", "Channels")
                serializer.endTag("", "LgChannels")
                serializer.endDocument()
                fout.write(writer.toString().toByteArray())
                fout.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
