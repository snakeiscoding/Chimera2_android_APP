package com.jht.chimera.lib.tv.tuner.lg.lg750e

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.ERROR_UNSUPPORTED_COMMAND
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.ERROR_UNSUPPORTED_VERSION
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.TYPE_COMMAND
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.TYPE_ERROR
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.TYPE_HELLO
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.TYPE_ITEM
import com.jht.chimera.lib.tv.tuner.lg.lg750e.Lg750EMessage.Companion.TYPE_REPLY
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * The protocol state of one WebSocket connection to the LG750E tuner. A reconnect gets a new
 * instance, so nothing negotiated or pending can leak across connections.
 *
 * Commands are classic until the tuner's hello arrives; a tuner app from before the JSON protocol
 * never sends one. After that, each command uses the highest version both sides list, or classic
 * if there is none.
 *
 * Thread-safe: messages arrive on the WebSocket thread, commands on a DataMatrix callback, and
 * [expire] on a timer.
 *
 * @param publish Must notify even for an unchanged value (DataMatrix `put`), because callers wait
 *   for a reply to each request.
 */
internal class Lg750ESession(
    private val consoleHello: Lg750EConsoleHello,
    private val sendText: (String) -> Unit,
    private val publish: (TVKeys, String) -> Unit,
    private val now: () -> Long,
    private val onTunerMacAp: (String) -> Unit,
    private val log: (String) -> Unit,
) {
    private val classicRouter = Lg750EResponseRouter(publish, log)

    /** Null until the tuner hello arrives. */
    private var tunerCommands: Map<String, List<Int>>? = null
    private var nextId = 1L
    private val pending = mutableMapOf<Long, Pending>()

    private class Pending(val command: Lg750ECommand, val ver: Int, var lastActivity: Long) {
        val items = mutableListOf<JsonObject>()
    }

    @Synchronized
    fun onOpen() {
        sendText(Lg750ELegacyMessage(consoleInfo = Lg750ELegacyMessage.ConsoleInfo(consoleHello.consoleType)).encode())
        val data = lg750eJson.encodeToJsonElement(consoleHello).jsonObject
        sendText(Lg750EMessage(type = TYPE_HELLO, name = Lg750EMessage.HELLO_CONSOLE, data = data).encode())
    }

    /** The session is not used again. */
    @Synchronized
    fun onClose() {
        pending.values.forEach { fail(it, "tuner disconnected") }
        pending.clear()
    }

    @Synchronized
    fun send(command: Lg750ECommand) {
        val ver = negotiatedVersion(command)
        if (ver != null) sendJson(command, ver) else sendClassic(command)
    }

    @Synchronized
    fun onMessage(text: String) {
        val message = Lg750EMessage.decode(text)
        if (message != null) {
            onJson(message)
            return
        }
        val legacy = Lg750ELegacyMessage.decode(text)
        if (legacy == null) {
            log("unrecognized message, ignored: $text")
            return
        }
        legacy.tunerInfo?.let { info ->
            log("tuner info> $info")
            onTunerMacAp(info.tunerMacAp.orEmpty())
        }
        legacy.response?.let(classicRouter::route)
    }

    /**
     * Removes and [fail]s each pending command with no activity (its send, or its latest item)
     * for [REPLY_TIMEOUT_MS], or [STREAM_TIMEOUT_MS] for a command whose reply is streamed. A
     * reply that arrives later is ignored.
     */
    @Synchronized
    fun expire() {
        val time = now()
        val expired = pending.filterValues { time - it.lastActivity >= timeoutFor(it.command) }
        expired.forEach { (id, request) ->
            pending.remove(id)
            fail(request, "timed out")
        }
    }

    /**
     * Picks the JSON version to send [command] in, or returns null to send it as classic.
     *
     * 1. Looks up [command]'s name in [tunerCommands], the map from the tuner hello (e.g.
     * `"setVolume" -> [1, 2]`). Null if no hello has arrived or the name isn't listed.
     * 2. Takes the versions the console can send for this command ([Lg750ECommand.versions],
     * `[1]` unless the command overrides it) and keeps only those the tuner also listed.
     * 3. Returns the highest of those, or null if none are shared. For example, console
     * `[1, 2]` and tuner `[1]` gives 1; console `[2]` and tuner `[1]` gives null, so the
     * command goes classic.
     */
    private fun negotiatedVersion(command: Lg750ECommand): Int? {
        val tuner = tunerCommands?.get(command.name) ?: return null
        return command.versions.filter { it in tuner }.maxOrNull()
    }

    private fun sendJson(command: Lg750ECommand, ver: Int) {
        val id = nextId++
        pending[id] = Pending(command, ver, now())
        sendText(Lg750EMessage(type = TYPE_COMMAND, id = id, name = command.name, ver = ver, data = command.data(ver)).encode())
    }

    private fun sendClassic(command: Lg750ECommand) {
        if (command is Lg750ECommand.GetChannelList) classicRouter.startChannelImport()
        command.classic().forEach { sendText(Lg750ELegacyMessage(command = it).encode()) }
    }

    private fun onJson(message: Lg750EMessage) {
        when (message.type) {
            TYPE_HELLO -> onHello(message)
            TYPE_REPLY -> onReply(message)
            TYPE_ITEM -> onItem(message)
            TYPE_ERROR -> onError(message)
            else -> log("message type ${message.type} ignored")
        }
    }

    private fun onHello(message: Lg750EMessage) {
        if (message.name != Lg750EMessage.HELLO_TUNER) return
        val hello = try {
            lg750eJson.decodeFromJsonElement<Lg750ETunerHello>(message.data)
        } catch (e: IllegalArgumentException) {
            log("tuner hello unreadable, staying classic: ${e.message}")
            return
        }
        log("tuner hello> ${message.data}")
        tunerCommands = hello.commands
    }

    private fun onReply(message: Lg750EMessage) {
        val request = takePending(message) ?: return
        when (val command = request.command) {
            is Lg750ECommand.Query -> command.publishReply(request.ver, message.data, publish)
            is Lg750ECommand.GetChannelList -> finishChannelList(request, message.data)
            else -> Unit // acknowledgement only
        }
    }

    private fun onItem(message: Lg750EMessage) {
        val id = message.id ?: return
        val request = pending[id]
        if (request == null) {
            log("item for unknown request $id ignored")
            return
        }
        if (message.seq != request.items.size) {
            pending.remove(id)
            fail(request, "item ${message.seq} arrived when ${request.items.size} was expected")
            return
        }
        request.items.add(message.data)
        request.lastActivity = now()
    }

    private fun onError(message: Lg750EMessage) {
        val request = takePending(message) ?: return
        val code = (message.data["code"] as? JsonPrimitive)?.contentOrNull
        if (code == ERROR_UNSUPPORTED_COMMAND || code == ERROR_UNSUPPORTED_VERSION) {
            // Nothing was executed, so resending as classic is safe.
            log("${request.command.name} v${request.ver} rejected as $code, falling back to classic")
            tunerCommands = tunerCommands?.minus(request.command.name)
            sendClassic(request.command)
            return
        }
        fail(request, "tuner error: ${message.data}")
    }

    private fun finishChannelList(request: Pending, data: JsonObject) {
        val count = (data["count"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        if (count != request.items.size) {
            fail(request, "reply count $count does not match ${request.items.size} items received")
            return
        }
        publish(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_RESULT_STRING, JsonArray(request.items).toString())
    }

    /**
     * Removes and returns the pending command whose `id` matches [message], or logs and returns
     * null if none is pending (a late reply after a timeout, or a duplicate).
     *
     * Called only for a final reply or error, since removing the command ends it. Items read
     * `pending[id]` instead, leaving the command pending for the rest of the stream.
     */
    private fun takePending(message: Lg750EMessage): Pending? {
        val id = message.id
        val request = id?.let(pending::remove)
        if (request == null) log("${message.type} for unknown request $id (${message.name}) ignored")
        return request
    }

    /** Queries have no failure key, so their callers time out, as with classic. */
    private fun fail(request: Pending, reason: String) {
        log("${request.command.name} v${request.ver} failed: $reason")
        if (request.command is Lg750ECommand.GetChannelList) {
            publish(TVKeys.EVENT_LG750E_CHANNEL_IMPORT_ERROR_STRING, reason)
        }
    }

    private fun timeoutFor(command: Lg750ECommand) =
        if (command is Lg750ECommand.GetChannelList) STREAM_TIMEOUT_MS else REPLY_TIMEOUT_MS

    private companion object {
        const val REPLY_TIMEOUT_MS = 5_000L

        /** Measured from the latest item, not from the command. */
        const val STREAM_TIMEOUT_MS = 30_000L
    }
}
