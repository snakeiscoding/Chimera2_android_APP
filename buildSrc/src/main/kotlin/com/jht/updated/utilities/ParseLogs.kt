package com.jht.updated.utilities

import com.jht.updated.closeStreams
import java.io.File

class ParseLogs {
    val parsers = arrayListOf<LogParser>()

    fun addParser(parser: LogParser) {
        parsers.add(parser)
    }

    fun parseLogs(inputPath: File, outputPath: File) {
        for (f in inputPath.listFiles() ?: emptyArray()) {
            if (f.isFile && f.name.contains("log") && f.name.contains(".txt")) {
                parseLog(f, File(outputPath, f.name))
            }
            else if (f.isDirectory) {
                parseLogs(f, File(outputPath, f.name))
            }
        }

        val processOutput = StringBuilder()
        val p = "adb logcat -d".execute()
        val outThread = Thread {
            processOutput.append(p.getText())
        }.also { it.start() }
        val errorThread = Thread {
            p.errorStream.bufferedReader().readText()
        }.also { it.start() }
        outThread.join(5000)
        errorThread.join(5000)

        for (parser in parsers) {
            for (line in processOutput.toString().split("\n")) {
                parser.parseLine(line, System.out)
            }
        }
    }

    fun parseLog(logFile: File, outputFile: File) {
        outputFile.outputStream().use { output ->
            logFile.reader().use { input ->
                input.readLines().forEach { line ->
                    for (parser in parsers) {
                        parser.parseLine(line, output)
                    }
                }
            }
        }
    }
}

/**
 * Executes the command specified by this [String] as a command-line process.
 *
 * For more control over Process construction you can use [java.lang.ProcessBuilder].
 *
 * @return the [Process] which has just started for this command-line representation
 * @throws java.io.IOException if an IOException occurs.
 */
fun String.execute(): Process = Runtime.getRuntime().exec(this)

/**
 * Read the text of the output stream of this [Process]. Closes all streams associated with this
 * process after retrieving the text.
 *
 * @return the text of the output
 * @throws java.io.IOException if an IOException occurs.
 */
fun Process.getText(): String {
    val text = inputStream.bufferedReader().readText()
    closeStreams()
    return text
}