package com.jht.updated.utilities

import java.io.OutputStream

class AppUpdateParser: LogParser() {
    companion object {
        private const val TOKEN = "PackageManager: Update package"
    }

    override fun parseLine(line: String, stream: OutputStream) {
        if (line.contains(TOKEN)) {
            val event = line.substring(line.indexOf(TOKEN))
            val result = getTimeStamp(line) + " " + event + "\n"
            stream.write(result.toByteArray())
        }
    }
}