package com.jht.chimera.lib.tv.tuner.lg.lg750e

import android.os.SystemClock
import android.util.Log
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.SystemPropertyUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.IStartStop
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val TAG = "Lg750EConnection"

/** The port the tuner's web app connects to. */
private const val PORT = 9311

/** How often each session's [Lg750ESession.expire] runs, which sets how precisely timeouts fire. */
private const val EXPIRE_INTERVAL_MS = 1_000L

// There is no verbose log setting that covers TV. This logs every WebSocket message in addition to
// the less frequent connection events.
private const val traceDebug = false // BuildConfig.DEBUG

/**
 * Owns the WebSocket server the LG750E tuner connects to, and is the only code that exchanges
 * messages with it. Commands arrive from any process through [Lg750ECommandSender]; each
 * connection's protocol is handled by its own [Lg750ESession].
 *
 * Runs in the HAL service. Create one instance; the tuner connects to a single fixed port.
 *
 * @param address Defaults to the address the tuner's web app is built to connect to.
 */
class Lg750EConnection(
    private val getConsoleType: () -> String,
    private val address: InetSocketAddress = InetSocketAddress(ProductConfig.VORTEX_INTERNAL_IP, PORT),
) : IStartStop {
    private val dataMatrix = DataMatrix()
    private val sessions = ConcurrentHashMap<WebSocket, Lg750ESession>()
    private var server: Lg750EWebSocketServer? = null
    private var expiry: ScheduledExecutorService? = null

    override fun start() {
        server = Lg750EWebSocketServer(address, ::onOpen, ::onClose) { conn, text ->
            if (traceDebug) Log.d(TAG, "websocket message ${conn.remoteSocketAddress}> $text")
            sessions[conn]?.onMessage(text)
        }.also { it.start() }

        expiry = Executors.newSingleThreadScheduledExecutor().also {
            it.scheduleWithFixedDelay({ sessions.values.forEach(Lg750ESession::expire) }, EXPIRE_INTERVAL_MS, EXPIRE_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }

        dataMatrix.createStringAdapter(TVKeys.ACTION_SEND_TUNER_COMMAND_STRING, { relay: String? ->
            val command = relay?.let(Lg750ECommand::fromRelay)
            if (command == null) Log.w(TAG, "unreadable command relay, dropped: $relay") else send(command)
        }, false)
        dataMatrix.createEventAdapter(TVKeys.ACTION_LG750E_CHANNEL_IMPORT_START) {
            Log.d(TAG, "ACTION_LG750E_CHANNEL_IMPORT_START: requesting channel list")
            send(Lg750ECommand.GetChannelList)
        }
    }

    override fun stop() {
        dataMatrix.finish()
        expiry?.shutdownNow()
        expiry = null
        server?.stop(100)
        server = null
        sessions.values.forEach(Lg750ESession::onClose)
        sessions.clear()
        DataMatrix.putBoolean(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL, false)
    }

    private fun send(command: Lg750ECommand) {
        if (sessions.isEmpty()) {
            Log.w(TAG, "send> no tuner connected, command dropped: $command")
            return
        }
        sessions.values.forEach { it.send(command) }
    }

    private fun onOpen(conn: WebSocket) {
        val session = Lg750ESession(
            consoleHello = Lg750EConsoleHello(getConsoleType(), BuildConfig.VERSION_NAME, Lg750ECommand.SUPPORTED),
            sendText = { text ->
                if (traceDebug) Log.d(TAG, "websocket send> $text")
                conn.send(text)
            },
            publish = { key, value -> DataMatrix.putString(key, value) },
            now = SystemClock::elapsedRealtime,
            onTunerMacAp = { SystemPropertyUtil.set("persist.jht.tuner.ap_mac", it) },
            log = { Log.d(TAG, it) },
        )
        // Sent before the session can receive commands, so the console info and hello go out first.
        session.onOpen()
        sessions[conn] = session
        DataMatrix.putBoolean(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL, true)
    }

    private fun onClose(conn: WebSocket) {
        sessions.remove(conn)?.onClose()
        DataMatrix.putBoolean(TVKeys.DATA_LG750E_TUNER_CONNECTED_BOOL, sessions.isNotEmpty())
    }
}

private class Lg750EWebSocketServer(
    address: InetSocketAddress,
    private val openHandler: (WebSocket) -> Unit,
    private val closeHandler: (WebSocket) -> Unit,
    private val messageHandler: (WebSocket, String) -> Unit,
) : org.java_websocket.server.WebSocketServer(address) {
    init {
        isReuseAddr = true // allows rebinding while old socket is in TIME_WAIT
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        Log.d(TAG, "websocket open ${conn?.remoteSocketAddress} on ${conn?.localSocketAddress}")
        conn?.let(openHandler)
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "websocket close ${conn?.remoteSocketAddress}")
        conn?.let(closeHandler)
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (conn != null && message != null) messageHandler(conn, message)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.d(TAG, "websocket error ${conn?.remoteSocketAddress}", ex)
    }

    override fun onStart() {
        Log.d(TAG, "websocket start")
    }
}
