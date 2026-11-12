package com.jht.updated.utilities

import java.io.OutputStream

class UserClicksParser: LogParser() {
    override fun parseLine(line: String, stream: OutputStream) {
        if (line.contains("CLICK:")) {
            val button = line.substring(line.indexOf("CLICK:", + 7))
            val result = getTimeStamp(line) + " User clicked on " + button + "\n"
            stream.write(result.toByteArray())
        }
    }
}