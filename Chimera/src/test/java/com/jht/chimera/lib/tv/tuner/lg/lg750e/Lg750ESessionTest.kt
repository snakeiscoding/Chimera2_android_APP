package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class Lg750ESessionTest {
    private val sent = mutableListOf<String>()
    private val published = mutableListOf<Pair<TVKeys, String>>()
    private var time = 0L

    private val session = Lg750ESession(
        consoleHello = Lg750EConsoleHello("treadmill", "5.13.1.0", Lg750ECommand.SUPPORTED),
        sendText = { sent += it },
        publish = { key, value -> published += key to value },
        now = { time },
        onTunerMacAp = {},
        log = {},
    )

    /** [commands] is the hello's `commands` map as raw JSON, e.g. `{"setVolume":[1]}`. */
    private fun tunerHello(commands: String) =
        """{"tunerApi":"json","type":"hello","name":"tuner","data":{"commands":$commands}}"""

    private fun tunerError(id: Long, name: String, code: String) =
        """{"tunerApi":"json","type":"error","id":$id,"name":"$name","data":{"code":"$code"}}"""

    /** [data] is the reply's `data` as raw JSON, e.g. `{"count":0}`. */
    private fun tunerReply(id: Long, name: String, data: String) =
        """{"tunerApi":"json","type":"reply","id":$id,"name":"$name","data":$data}"""

    /** A classic `response` message carrying [response]; built as JSON so its quotes are escaped. */
    private fun classicResponse(response: String) =
        buildJsonObject {
            put("tunerApi", "classic")
            put("response", response)
        }.toString()

    private fun channelEntry(name: String) = """{"type":"channellist_entry","name":"$name","rfClass":"RF1"}"""
    private val channelEnd = """{"type":"channellist_end"}"""

    /** The `id` of the JSON command at [index] in [sent], as the tuner would echo it. */
    private fun sentId(index: Int): Long =
        checkNotNull(Lg750EMessage.decode(sent[index])?.id) { "not a JSON command: ${sent[index]}" }

    /** Sends [Lg750ECommand.GetChannelList] as JSON at [START] and returns its `id`. */
    private fun sendJsonChannelList(): Long {
        time = START
        session.onMessage(tunerHello("""{"getChannelList":[1]}"""))
        session.send(Lg750ECommand.GetChannelList)
        return sentId(0)
    }

    /** Sends [Lg750ECommand.GetModel] as JSON at [START] and returns its `id`. */
    private fun sendJsonGetModel(): Long {
        time = START
        session.onMessage(tunerHello("""{"getModel":[1]}"""))
        session.send(Lg750ECommand.GetModel)
        return sentId(0)
    }

    /** [data] is the item's `data` as raw JSON. */
    private fun tunerItem(id: Long, seq: Int, data: String) =
        """{"tunerApi":"json","type":"item","id":$id,"name":"getChannelList","seq":$seq,"data":$data}"""

    /** One channel as a JSON item carries it. */
    private fun channelData(name: String) = """{"name":"$name","rfClass":"RF1"}"""

    /** Compared as parsed JSON, so key order and whitespace in the published array do not matter. */
    private fun assertPublishedChannels(vararg names: String) {
        val (key, value) = published.single()
        assertEquals(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING, key)
        val expected = names.joinToString(",", "[", "]") { channelData(it) }
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(value))
    }

    // region version negotiation

    @Test
    fun `commands are sent as classic before the tuner hello arrives`() {
        session.send(Lg750ECommand.SetVolume(30))

        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent.single())?.command)
    }

    @Test
    fun `a command is sent as JSON once the tuner hello lists a version the console supports`() {
        session.onMessage(tunerHello("""{"setVolume":[1]}"""))

        session.send(Lg750ECommand.SetVolume(30))

        val message = checkNotNull(Lg750EMessage.decode(sent.single())) { "not a JSON message: ${sent.single()}" }
        assertEquals("command", message.type)
        assertEquals("setVolume", message.name)
        assertEquals(1, message.ver)
        assertEquals(Json.parseToJsonElement("""{"level":30}"""), message.data)
    }

    @Test
    fun `the highest version both sides list is sent`() {
        // The console lists only v1 of setVolume, so the tuner's higher versions are not shared.
        session.onMessage(tunerHello("""{"setVolume":[1,2,3]}"""))

        session.send(Lg750ECommand.SetVolume(30))

        val message = checkNotNull(Lg750EMessage.decode(sent.single())) { "not a JSON message: ${sent.single()}" }
        assertEquals(1, message.ver)
    }

    @Test
    fun `a command missing from the tuner hello stays classic`() {
        session.onMessage(tunerHello("""{"reboot":[1]}"""))

        session.send(Lg750ECommand.SetVolume(30))
        session.send(Lg750ECommand.Reboot)

        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent[0])?.command)
        // Reboot going JSON shows the hello was accepted, so setVolume was classic only for being unlisted.
        assertEquals("reboot", Lg750EMessage.decode(sent[1])?.name)
        assertEquals(2, sent.size)
    }

    @Test
    fun `a command with no version shared with the tuner stays classic`() {
        session.onMessage(tunerHello("""{"reboot":[2],"setVolume":[1]}"""))

        session.send(Lg750ECommand.Reboot)
        session.send(Lg750ECommand.SetVolume(30))

        assertEquals("00\n", Lg750ELegacyMessage.decode(sent[0])?.command)
        // setVolume going JSON shows the hello was accepted, so reboot was classic only for the version mismatch.
        assertEquals("setVolume", Lg750EMessage.decode(sent[1])?.name)
        assertEquals(2, sent.size)
    }

    @Test
    fun `an unreadable tuner hello leaves commands classic`() {
        // A valid envelope whose data is not a tuner hello, so it reaches the hello parsing.
        val hello = tunerHello("""{"setVolume":"one"}""")
        assertNotNull(Lg750EMessage.decode(hello))
        session.onMessage(hello)

        session.send(Lg750ECommand.SetVolume(30))

        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent.single())?.command)
    }

    @Test
    fun `a hello not named as the tuner hello is ignored`() {
        // A readable tuner hello in every way except its name.
        session.onMessage("""{"tunerApi":"json","type":"hello","name":"console","data":{"commands":{"setVolume":[1]}}}""")

        session.send(Lg750ECommand.SetVolume(30))

        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent.single())?.command)
    }

    // endregion

    // region classic fallback

    @Test
    fun `an unsupported_command error resends the command as classic`() {
        session.onMessage(tunerHello("""{"setVolume":[1]}"""))
        session.send(Lg750ECommand.SetVolume(30))

        session.onMessage(tunerError(sentId(0), "setVolume", "unsupported_command"))

        assertEquals(2, sent.size)
        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent[1])?.command)
    }

    @Test
    fun `an unsupported_version error resends the command as classic`() {
        session.onMessage(tunerHello("""{"setVolume":[1]}"""))
        session.send(Lg750ECommand.SetVolume(30))

        session.onMessage(tunerError(sentId(0), "setVolume", "unsupported_version"))

        assertEquals(2, sent.size)
        assertEquals("02:30\n", Lg750ELegacyMessage.decode(sent[1])?.command)
    }

    @Test
    fun `after an unsupported error later sends of that command stay classic`() {
        session.onMessage(tunerHello("""{"setVolume":[1]}"""))
        session.send(Lg750ECommand.SetVolume(30))
        session.onMessage(tunerError(sentId(0), "setVolume", "unsupported_command"))

        // A different level, so this send cannot be mistaken for the fallback resend.
        session.send(Lg750ECommand.SetVolume(40))

        assertEquals(3, sent.size)
        assertEquals("02:40\n", Lg750ELegacyMessage.decode(sent[2])?.command)
    }

    @Test
    fun `an unsupported error leaves other commands on JSON`() {
        session.onMessage(tunerHello("""{"setVolume":[1],"reboot":[1]}"""))
        session.send(Lg750ECommand.SetVolume(30))
        session.onMessage(tunerError(sentId(0), "setVolume", "unsupported_command"))

        session.send(Lg750ECommand.Reboot)

        assertEquals(3, sent.size)
        assertEquals("reboot", Lg750EMessage.decode(sent[2])?.name)
    }

    @Test
    fun `a GetChannelList resent as classic starts a classic channel import`() {
        session.onMessage(tunerHello("""{"getChannelList":[1]}"""))
        // Left over from an earlier classic import; only starting a new import discards it.
        session.onMessage(classicResponse(channelEntry("stale")))
        session.send(Lg750ECommand.GetChannelList)
        session.onMessage(tunerError(sentId(0), "getChannelList", "unsupported_command"))

        session.onMessage(classicResponse(channelEntry("A")))
        session.onMessage(classicResponse(channelEnd))

        assertEquals(2, sent.size)
        assertEquals("41\n", Lg750ELegacyMessage.decode(sent[1])?.command)
        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[${channelEntry("A")}]"), published)
    }

    @Test
    fun `any other error code fails the command without resending it`() {
        // A channel list, because it is the command whose failure is published.
        session.onMessage(tunerHello("""{"getChannelList":[1]}"""))
        session.send(Lg750ECommand.GetChannelList)
        val id = sentId(0)

        session.onMessage(tunerError(id, "getChannelList", "busy"))
        // Ignored if the error ended the command, so no result follows the import error.
        session.onMessage(tunerReply(id, "getChannelList", """{"count":0}"""))

        assertEquals(1, sent.size)
        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `an error for an unknown request id is ignored`() {
        session.onMessage(tunerHello("""{"getChannelList":[1]}"""))
        session.send(Lg750ECommand.GetChannelList)
        val id = sentId(0)

        // unsupported_command, so wrongly matching it to the pending command would show as a resend.
        session.onMessage(tunerError(id + 1, "getChannelList", "unsupported_command"))

        assertEquals(1, sent.size)
        assertEquals(emptyList<Pair<TVKeys, String>>(), published)

        // The pending command is untouched and still completes.
        session.onMessage(tunerReply(id, "getChannelList", """{"count":0}"""))
        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING to "[]"), published)
    }

    // endregion

    // region channel list

    @Test
    fun `items in order followed by a matching count publish the channel list`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 1, channelData("B")))
        session.onMessage(tunerItem(id, 2, channelData("C")))
        session.onMessage(tunerReply(id, "getChannelList", """{"count":3}"""))

        assertPublishedChannels("A", "B", "C")
    }

    @Test
    fun `an empty channel list with count 0 publishes an empty array`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerReply(id, "getChannelList", """{"count":0}"""))

        assertPublishedChannels()
    }

    @Test
    fun `an item arriving ahead of its seq fails the channel import`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 2, channelData("C")))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a repeated item seq fails the channel import`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 0, channelData("A")))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a reply count lower than the items received fails the channel import`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 1, channelData("B")))
        session.onMessage(tunerReply(id, "getChannelList", """{"count":1}"""))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a reply count higher than the items received fails the channel import`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 1, channelData("B")))
        session.onMessage(tunerReply(id, "getChannelList", """{"count":3}"""))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a reply with no count fails the channel import`() {
        val id = sendJsonChannelList()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerReply(id, "getChannelList", "{}"))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `items and the reply after a failed channel import are ignored`() {
        val id = sendJsonChannelList()
        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerItem(id, 2, channelData("C")))

        // The rest of the stream as the tuner sent it, with a count that matches what it sent.
        session.onMessage(tunerItem(id, 1, channelData("B")))
        session.onMessage(tunerReply(id, "getChannelList", """{"count":3}"""))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    // endregion

    // region timeouts

    @Test
    fun `a command is still pending 1 ms before the reply timeout`() {
        val id = sendJsonGetModel()

        time = START + REPLY_TIMEOUT_MS - 1
        session.expire()
        session.onMessage(tunerReply(id, "getModel", """{"model":"STB-750E-UA"}"""))

        assertEquals(listOf(TVKeys.EVENT_TUNER_MODEL_STRING to "STB-750E-UA"), published)
    }

    @Test
    fun `a command times out exactly at the reply timeout`() {
        val id = sendJsonGetModel()

        time = START + REPLY_TIMEOUT_MS
        session.expire()
        // A query's timeout publishes nothing, so the ignored reply is what shows it timed out.
        session.onMessage(tunerReply(id, "getModel", """{"model":"STB-750E-UA"}"""))

        assertEquals(emptyList<Pair<TVKeys, String>>(), published)
    }

    @Test
    fun `a channel list is still pending 1 ms before the stream timeout`() {
        val id = sendJsonChannelList()

        time = START + STREAM_TIMEOUT_MS - 1
        session.expire()
        session.onMessage(tunerReply(id, "getChannelList", """{"count":0}"""))

        assertPublishedChannels()
    }

    @Test
    fun `a channel list with no items times out exactly at the stream timeout`() {
        sendJsonChannelList()

        time = START + STREAM_TIMEOUT_MS
        session.expire()

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `each item restarts the stream timeout`() {
        val id = sendJsonChannelList()
        time = START + 20_000
        session.onMessage(tunerItem(id, 0, channelData("A")))
        time = START + 40_000
        session.onMessage(tunerItem(id, 1, channelData("B")))

        // Past the stream timeout from the send and from the first item, but not from the second.
        time = START + 40_000 + STREAM_TIMEOUT_MS - 1
        session.expire()
        session.onMessage(tunerReply(id, "getChannelList", """{"count":2}"""))

        assertPublishedChannels("A", "B")
    }

    @Test
    fun `a channel list times out a stream timeout after its latest item`() {
        val id = sendJsonChannelList()
        time = START + 20_000
        session.onMessage(tunerItem(id, 0, channelData("A")))

        time = START + 20_000 + STREAM_TIMEOUT_MS
        session.expire()

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a timed-out channel list publishes the import error once`() {
        sendJsonChannelList()

        // The timer keeps firing after the command has expired.
        time = START + STREAM_TIMEOUT_MS
        session.expire()
        time += STREAM_TIMEOUT_MS
        session.expire()

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `a reply after a timeout is ignored`() {
        val id = sendJsonChannelList()
        time = START + STREAM_TIMEOUT_MS
        session.expire()

        session.onMessage(tunerItem(id, 0, channelData("A")))
        session.onMessage(tunerReply(id, "getChannelList", """{"count":1}"""))

        assertEquals(listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING), published.map { it.first })
    }

    @Test
    fun `closing the session fails every pending command`() {
        session.onMessage(tunerHello("""{"getChannelList":[1]}"""))
        session.send(Lg750ECommand.GetChannelList)
        session.send(Lg750ECommand.GetChannelList)

        session.onClose()

        assertEquals(
            listOf(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING, TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING),
            published.map { it.first },
        )
    }

    // endregion

    private companion object {
        /** Copies of the session's private timeouts. */
        const val REPLY_TIMEOUT_MS = 5_000L
        const val STREAM_TIMEOUT_MS = 30_000L

        /** Not 0, so a timeout measured from the epoch instead of from the send fails. */
        const val START = 1_000L
    }
}
