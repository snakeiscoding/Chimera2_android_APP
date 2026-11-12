package com.jht.test.util

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.TimeUnit

class SystemCommandHelper {

    fun execute(command: String, timeout: Long = 0): SystemCommandResult {
        return execute(listOf(command), timeout)
    }

    fun execute(commands: List<String>, timeout: Long = 0): SystemCommandResult {
        try {
            if (commands.isNotEmpty()) {
                val process = ProcessBuilder(exe()).start()
                return runCommands(commands, process, timeout)
            }
        } catch (ex: Exception) {
            println("Error executing internal operation")
            ex.printStackTrace()
            return SystemCommandResult(ex = ex)
        }
        return SystemCommandResult(ex = InvalidParameterException("No commands to execute"))
    }


    @Throws(IOException::class, InterruptedException::class)
    private fun runCommands(commands: List<String>, process: Process, timeout: Long): SystemCommandResult {

        return runBlocking {
            val outputDeferred = async { BufferedReader(InputStreamReader(process.inputStream)).readText() }
            val errorDeffered = async { BufferedReader(InputStreamReader(process.errorStream)).readText() }

            DataOutputStream(process.outputStream).use {
                for (currCommand in commands) {
                    println(currCommand)
                    it.writeBytes("$currCommand\n")
                    it.flush()
                }
                it.writeBytes("exit\n")
                it.flush()
            }

            if(timeout > 0) {
                if(process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
                    SystemCommandResult(process.exitValue(), outputDeferred.await(), errorDeffered.await())
                }
                else {
                    SystemCommandResult(-1, "", "command timed out")
                }
            }
            else {
                SystemCommandResult(
                    process.waitFor(),
                    outputDeferred.await(),
                    errorDeffered.await()
                )
            }
        }
    }

    private val exe: () -> String
        get() = {
            val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
            when {
                os.contains("win") -> {
                    "cmd.exe"
                }
                else -> "sh"
            }
        }

}