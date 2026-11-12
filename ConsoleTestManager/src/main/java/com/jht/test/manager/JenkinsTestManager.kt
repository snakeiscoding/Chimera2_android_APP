package com.jht.test.manager

import com.jht.test.jenkins.Jenkins
import com.jht.test.testfinder.InstrumentationTest
import com.jht.test.util.getIPAddress
import com.jht.test.util.simpleSleep
import java.io.IOException
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class JenkinsTestManager (private val args: RunTestsOnJenkinsArgs,
                          private val consoles: List<String>,
                          private val testCycle: String,
                          private val tests: List<InstrumentationTest>,
                          private val version: String){
    private val consolesAvailable = LinkedBlockingQueue<String>()
    private lateinit var serverSocket: ServerSocket
    private var numConsolesUpdated = 0

    @Volatile
    var needMoreConsoles = true


    fun runTests() {
        thread { listen() }
        thread { updateConsoles() }
        simpleSleep(5 * 60 * 1000 + 10000)
        val jenkins = Jenkins(args)
        val jobParams = HashMap<String, String>()
        jobParams["TestCycle"] = testCycle
        for (i in tests.indices) {
            println("Run test ${i+1}")
            jobParams["Console"] = consolesAvailable.take()
            jobParams["Manager"] = getIPAddress()
            jobParams["TestIndex"] = i.toString()
            jenkins.executeTask(jobParams)
        }
        for(i in 1 .. numConsolesUpdated) {
            val console = consolesAvailable.take()
            println("Update $console to latest")
            jobParams["Console"] = console
            jobParams["Manager"] = getIPAddress()
            jobParams["TestIndex"] = "-1"
            jenkins.executeTask(jobParams)
        }
        needMoreConsoles = false
        serverSocket.close()

    }

    private fun listen() {
        serverSocket = ServerSocket(5000)
        while(needMoreConsoles) {
            try {
                val client = serverSocket.accept()
                println("Got release notification or shutdown.")
                if(!needMoreConsoles) return
                val reader = Scanner(client.getInputStream())
                reader.use {
                    val console = reader.nextLine()
                    println("$console released")
                    consolesAvailable.offer(console)
                }
                client.close()
            } catch (ex: IOException) {
                return
            }
        }
    }

    private fun updateConsolesUpdateManager(consoles: List<String>) : List<String> {
        val consolesUpdated: MutableList<String> = ArrayList()
        consoles.forEach {
            if(Console(it).updateUpdateManager()) {
                consolesUpdated.add(it)
            }
        }
        simpleSleep(120000)
        return consolesUpdated
    }

    private fun updateConsoles() {
        var consolesUpdated: MutableList<String> = ArrayList()
        updateConsolesUpdateManager(consoles).forEach {
            if(Console(it).update()) {
                consolesUpdated.add(it)
            }
        }
        simpleSleep(5 * 60 * 1000)
        while(needMoreConsoles && consolesUpdated.size > 0) {
            simpleSleep(10000)
            consolesUpdated = checkUpdate(consolesUpdated)
        }
    }

    private fun checkUpdate(consolesToCheck: List<String>) : MutableList<String> {
        val consolesNotReady: MutableList<String> = ArrayList()
        for(console in consolesToCheck) {
            val consoleObj = Console(console)
            if(consoleObj.updateComplete()) {
                println("$console is ready.")
                if(consoleObj.checkChimeraVersion(version)) {
                    println("$console is correct version")
                    numConsolesUpdated++
                    consolesAvailable.offer(console)
                }
                else {
                    println("$console is not at correct version")
                }
            }
            else {
                consolesNotReady.add(console)
            }
            consoleObj.disconnect()
        }
        return consolesNotReady
    }
}