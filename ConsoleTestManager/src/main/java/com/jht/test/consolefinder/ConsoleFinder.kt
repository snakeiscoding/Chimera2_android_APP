package com.jht.test.consolefinder

import com.jht.test.util.catchAll
import com.jht.test.util.getIPAddress
import com.jht.test.util.simpleSleep
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.util.*

class ConsoleFinder(private val consoleName: String) {
    private val consoles: MutableList<String> = ArrayList()
    private var serverSocket: ServerSocket? = null
    fun findConsoles(): List<String> {
        consoles.clear()
        runBlocking {
            val poloJob = async(Dispatchers.Default) { polo(this) }
            withContext(Dispatchers.Default) { marco() }
            serverSocket?.close()
            poloJob.cancelAndJoin()
        }
        catchAll { serverSocket?.close() }
        return consoles
    }

    private fun marco() {
        val xml = getBroadcastXML()
        catchAll {
            DatagramSocket().use { ds ->
                ds.broadcast = true
                val dp = DatagramPacket(xml.toByteArray(), xml.length, InetAddress.getByName("255.255.255.255"),1600 )
                for (i in 0..4) {
                    ds.send(dp)
                    simpleSleep(2000)
                }
            }
        }
    }

    private fun getBroadcastXML(): String {
        val random = Random()
        return """<?xml version="1.0" encoding="utf-8"?>
<SettingsAvailable>
	<Nonce>${random.nextInt(2000)}</Nonce>
	<Console>$consoleName</Console>
	<APIVersion>2</APIVersion>
	<SettingsVersion>1</SettingsVersion>
	<Host>${getIPAddress()}</Host>
	<Port>1600</Port>
	<SettingsPort>1602</SettingsPort>
	<ConsoleSerialNumber>console_finder</ConsoleSerialNumber>
	<FrameSerialNumber>console_finder</FrameSerialNumber>
</SettingsAvailable>"""
    }

    private fun polo(job: CoroutineScope) {
        catchAll {
            serverSocket = ServerSocket(1602)
            while (job.isActive) {
                serverSocket!!.accept().use { consoleSocket ->
                    consoles.add(consoleSocket.inetAddress.hostAddress)
                }
            }
        }
        catchAll { serverSocket?.close() }
    }

}